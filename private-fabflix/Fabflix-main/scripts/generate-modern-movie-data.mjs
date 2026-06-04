import { createReadStream, createWriteStream } from 'node:fs';
import { mkdir, stat, writeFile } from 'node:fs/promises';
import { dirname, join } from 'node:path';
import { createInterface } from 'node:readline';
import { Readable } from 'node:stream';
import { pipeline } from 'node:stream/promises';
import { createGunzip } from 'node:zlib';

const root = new URL('..', import.meta.url).pathname;
const cacheDir = join(root, '.cache', 'imdb');
const outputPath = process.env.OUTPUT_PATH ?? join(root, 'sql', 'modern-movie-data.sql');
const movieLimit = Number(process.env.MOVIE_LIMIT ?? 3000);
const minYear = Number(process.env.MIN_YEAR ?? 2010);
const maxYear = Number(process.env.MAX_YEAR ?? 2025);
const minVotes = Number(process.env.MIN_VOTES ?? 5000);
const includeStars = process.env.INCLUDE_STARS !== 'false';

const datasets = {
  basics: 'https://datasets.imdbws.com/title.basics.tsv.gz',
  ratings: 'https://datasets.imdbws.com/title.ratings.tsv.gz',
  crew: 'https://datasets.imdbws.com/title.crew.tsv.gz',
  names: 'https://datasets.imdbws.com/name.basics.tsv.gz',
  principals: 'https://datasets.imdbws.com/title.principals.tsv.gz'
};

async function exists(path) {
  try {
    await stat(path);
    return true;
  } catch {
    return false;
  }
}

async function download(name, url) {
  await mkdir(cacheDir, { recursive: true });
  const destination = join(cacheDir, `${name}.tsv.gz`);
  if (await exists(destination)) {
    return destination;
  }

  console.log(`Downloading ${url}`);
  const response = await fetch(url, {
    headers: { 'User-Agent': 'FabflixDataSeeder/1.0 (local development)' }
  });
  if (!response.ok || !response.body) {
    throw new Error(`Failed to download ${url}: ${response.status} ${response.statusText}`);
  }
  await pipeline(Readable.fromWeb(response.body), createWriteStream(destination));
  return destination;
}

async function readTsvGz(path, onRow) {
  const input = createReadStream(path).pipe(createGunzip());
  const lines = createInterface({ input, crlfDelay: Infinity });
  let headers = [];
  let index = 0;

  for await (const line of lines) {
    const fields = line.split('\t');
    if (index === 0) {
      headers = fields;
      index += 1;
      continue;
    }
    const row = Object.fromEntries(headers.map((header, fieldIndex) => [header, fields[fieldIndex] ?? '']));
    await onRow(row);
    index += 1;
  }
}

function parseNumber(value) {
  if (!value || value === '\\N') {
    return null;
  }
  const parsed = Number(value);
  return Number.isFinite(parsed) ? parsed : null;
}

function sqlString(value, maxLength = 255) {
  const cleaned = String(value ?? '')
    .replace(/\s+/g, ' ')
    .trim()
    .slice(0, maxLength);
  return `'${cleaned.replaceAll("'", "''")}'`;
}

function sqlNumber(value) {
  return Number.isFinite(value) ? String(value) : 'NULL';
}

function scoreMovie(candidate) {
  return candidate.rating * Math.log10(candidate.numVotes + 10) + (candidate.year - minYear) * 0.025;
}

const [basicsPath, ratingsPath, crewPath, namesPath, principalsPath] = await Promise.all([
  download('title.basics', datasets.basics),
  download('title.ratings', datasets.ratings),
  download('title.crew', datasets.crew),
  download('name.basics', datasets.names),
  includeStars ? download('title.principals', datasets.principals) : Promise.resolve(null)
]);

const ratings = new Map();
await readTsvGz(ratingsPath, (row) => {
  const averageRating = parseNumber(row.averageRating);
  const numVotes = parseNumber(row.numVotes);
  if (averageRating !== null && numVotes !== null && numVotes >= minVotes) {
    ratings.set(row.tconst, { rating: averageRating, numVotes });
  }
});

const candidates = [];
await readTsvGz(basicsPath, (row) => {
  if (row.titleType !== 'movie' || row.isAdult !== '0') {
    return;
  }
  const year = parseNumber(row.startYear);
  const rating = ratings.get(row.tconst);
  if (year === null || year < minYear || year > maxYear || !rating || !row.genres || row.genres === '\\N') {
    return;
  }
  const genres = row.genres.split(',').filter(Boolean);
  const candidate = {
    id: row.tconst,
    title: row.primaryTitle === '\\N' ? row.originalTitle : row.primaryTitle,
    year,
    genres,
    rating: rating.rating,
    numVotes: rating.numVotes,
    score: 0,
    directorIds: [],
    directors: [],
    starIds: [],
    stars: []
  };
  candidate.score = scoreMovie(candidate);
  candidates.push(candidate);
});

candidates.sort((left, right) => right.score - left.score || right.numVotes - left.numVotes);
const movies = candidates.slice(0, movieLimit);
const movieById = new Map(movies.map((movie) => [movie.id, movie]));

await readTsvGz(crewPath, (row) => {
  const movie = movieById.get(row.tconst);
  if (!movie || !row.directors || row.directors === '\\N') {
    return;
  }
  movie.directorIds = row.directors.split(',').filter(Boolean).slice(0, 3);
});

const neededDirectorIds = new Set(movies.flatMap((movie) => movie.directorIds));
const neededStarIds = new Set();
const starsById = new Map();

if (includeStars && principalsPath) {
  await readTsvGz(principalsPath, (row) => {
    const movie = movieById.get(row.tconst);
    if (!movie || movie.starIds.length >= 5 || (row.category !== 'actor' && row.category !== 'actress')) {
      return;
    }
    movie.starIds.push(row.nconst);
    neededStarIds.add(row.nconst);
  });
}

await readTsvGz(namesPath, (row) => {
  const name = row.primaryName;
  if (!name || name === '\\N') {
    return;
  }

  if (neededDirectorIds.has(row.nconst)) {
    for (const movie of movies) {
      if (movie.directorIds.includes(row.nconst)) {
        movie.directors.push(name);
      }
    }
  }

  if (neededStarIds.has(row.nconst)) {
    const birthYear = parseNumber(row.birthYear);
    const star = { id: row.nconst, name, birthYear };
    starsById.set(row.nconst, star);
  }
});

for (const movie of movies) {
  movie.stars = movie.starIds.map((starId) => starsById.get(starId)).filter(Boolean);
}

function movieIdList() {
  return movies.map((movie) => sqlString(movie.id, 10)).join(', ');
}

const allGenres = [...new Set(movies.flatMap((movie) => movie.genres))].sort();
const lines = [
  '-- Supplemental modern movie seed data for Fabflix.',
  '-- Source: IMDb non-commercial datasets from https://datasets.imdbws.com/.',
  `-- Generated: ${new Date().toISOString()}`,
  `-- Selection: ${movies.length} movies, years ${minYear}-${maxYear}, minimum ${minVotes} votes.`,
  '',
  "SELECT setval(pg_get_serial_sequence('genres', 'id'), COALESCE((SELECT MAX(id) FROM genres), 1));",
  `DELETE FROM stars_in_movies WHERE movieId IN (${movieIdList()});`,
  `DELETE FROM genres_in_movies WHERE movieId IN (${movieIdList()});`,
  ''
];

for (const genre of allGenres) {
  lines.push(`INSERT INTO genres (name) VALUES (${sqlString(genre, 32)}) ON CONFLICT (name) DO NOTHING;`);
}

lines.push('');

for (const movie of movies) {
  const director = movie.directors.length > 0 ? movie.directors.join(', ') : 'Unknown';
  lines.push(
    `INSERT INTO movies (id, title, year, director) VALUES (${sqlString(movie.id, 10)}, ${sqlString(movie.title)}, ${movie.year}, ${sqlString(director)}) ON CONFLICT (id) DO UPDATE SET title = EXCLUDED.title, year = EXCLUDED.year, director = EXCLUDED.director;`
  );
}

lines.push('');

for (const star of [...starsById.values()].sort((left, right) => left.id.localeCompare(right.id))) {
  lines.push(
    `INSERT INTO stars (id, name, birthYear) VALUES (${sqlString(star.id, 10)}, ${sqlString(star.name)}, ${sqlNumber(star.birthYear)}) ON CONFLICT (id) DO NOTHING;`
  );
}

lines.push('');

for (const movie of movies) {
  lines.push(
    `INSERT INTO ratings (movieId, rating, numVotes) VALUES (${sqlString(movie.id, 10)}, ${movie.rating}, ${movie.numVotes}) ON CONFLICT (movieId) DO UPDATE SET rating = EXCLUDED.rating, numVotes = EXCLUDED.numVotes;`
  );
}

lines.push('');

for (const movie of movies) {
  for (const genre of movie.genres) {
    lines.push(
      `INSERT INTO genres_in_movies (genreId, movieId) SELECT id, ${sqlString(movie.id, 10)} FROM genres WHERE name = ${sqlString(genre, 32)} ON CONFLICT DO NOTHING;`
    );
  }
}

lines.push('');

for (const movie of movies) {
  for (const star of movie.stars) {
    lines.push(
      `INSERT INTO stars_in_movies (starId, movieId) VALUES (${sqlString(star.id, 10)}, ${sqlString(movie.id, 10)}) ON CONFLICT DO NOTHING;`
    );
  }
}

lines.push('');

await mkdir(dirname(outputPath), { recursive: true });
await writeFile(outputPath, `${lines.join('\n')}\n`);
console.log(`Wrote ${movies.length} movies to ${outputPath}`);
