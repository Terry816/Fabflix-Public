import { mkdir, readFile, readdir, writeFile } from 'node:fs/promises';
import { extname } from 'node:path';
import { fileURLToPath } from 'node:url';

const posterSource = process.env.POSTER_SOURCE ?? (process.env.TMDB_API_KEY || process.env.TMDB_BEARER_TOKEN ? 'tmdb' : process.env.OMDB_API_KEY ? 'omdb' : 'wikidata');
const limit = Number(process.env.POSTER_LIMIT ?? 40);
const profileLimit = Number(process.env.PROFILE_LIMIT ?? 120);
const trailerLimit = Number(process.env.TRAILER_LIMIT ?? limit);
const requestDelayMs = Number(process.env.TMDB_REQUEST_DELAY_MS ?? 150);
const scriptRoot = new URL('.', import.meta.url);
const movieDataPaths = [new URL('../../../sql/movie-data.sql', scriptRoot), new URL('../../../sql/modern-movie-data.sql', scriptRoot)];
const posterDir = new URL('../public/posters/', scriptRoot);
const profileDir = new URL('../public/people/', scriptRoot);
const manifestPath = new URL('../src/posters/generatedPosterManifest.ts', scriptRoot);
const profileManifestPath = new URL('../src/posters/generatedProfileManifest.ts', scriptRoot);
const trailerManifestPath = new URL('../src/posters/generatedTrailerManifest.ts', scriptRoot);
const tmdbFindCache = new Map();

if (posterSource === 'omdb' && !process.env.OMDB_API_KEY) {
  console.error('Set OMDB_API_KEY before running OMDb poster fetches.');
  process.exit(1);
}

if (posterSource === 'tmdb' && !process.env.TMDB_API_KEY && !process.env.TMDB_BEARER_TOKEN) {
  console.error('Set TMDB_API_KEY or TMDB_BEARER_TOKEN before running TMDb poster fetches.');
  process.exit(1);
}

async function readSql(path) {
  try {
    return await readFile(path, 'utf8');
  } catch {
    return '';
  }
}

function extractMovies(sql) {
  return [...sql.matchAll(/INSERT INTO movies(?:\s*\([^)]+\))?\s+VALUES\s*\('([^']+)'\s*,\s*'((?:''|[^'])+)'\s*,\s*(\d+)/g)].map(
    (match) => ({
      id: match[1],
      title: match[2].replaceAll("''", "'"),
      year: Number(match[3]),
      rating: 0,
      numVotes: 0
    })
  );
}

function extractRatings(sql) {
  return new Map(
    [...sql.matchAll(/INSERT INTO ratings\s*\([^)]+\)\s+VALUES\s*\('([^']+)'\s*,\s*([\d.]+)\s*,\s*(\d+)/g)].map(
      (match) => [match[1], { rating: Number(match[2]), numVotes: Number(match[3]) }]
    )
  );
}

function extractStars(sql) {
  return new Map(
    [...sql.matchAll(/INSERT INTO stars\s*\([^)]+\)\s+VALUES\s*\('([^']+)'\s*,\s*'((?:''|[^'])+)'/g)].map((match) => [
      match[1],
      { id: match[1], name: match[2].replaceAll("''", "'") }
    ])
  );
}

function extractStarRelations(sql) {
  const relations = new Map();
  for (const match of sql.matchAll(/INSERT INTO stars_in_movies\s*\([^)]+\)\s+VALUES\s*\('([^']+)'\s*,\s*'([^']+)'\)/g)) {
    const starId = match[1];
    const movieId = match[2];
    if (!relations.has(movieId)) {
      relations.set(movieId, []);
    }
    relations.get(movieId).push(starId);
  }
  return relations;
}

function scoreMovie(movie) {
  return movie.rating * Math.log10(movie.numVotes + 10) + movie.year * 0.35;
}

function wait(ms) {
  return new Promise((resolve) => setTimeout(resolve, ms));
}

async function tmdbJson(url) {
  await wait(requestDelayMs);
  const response = await fetch(url, {
    headers: process.env.TMDB_BEARER_TOKEN ? { Authorization: `Bearer ${process.env.TMDB_BEARER_TOKEN}` } : {}
  });

  if (response.status === 429) {
    const retryAfter = Number(response.headers.get('retry-after') ?? 1);
    await wait(Math.max(1, retryAfter) * 1000);
    return tmdbJson(url);
  }

  if (!response.ok) {
    throw new Error(`TMDb returned ${response.status}`);
  }

  return response.json();
}

async function tmdbFindByImdbId(imdbId) {
  if (tmdbFindCache.has(imdbId)) {
    return tmdbFindCache.get(imdbId);
  }

  const metadataUrl = new URL(`https://api.themoviedb.org/3/find/${imdbId}`);
  metadataUrl.searchParams.set('external_source', 'imdb_id');
  if (process.env.TMDB_API_KEY) {
    metadataUrl.searchParams.set('api_key', process.env.TMDB_API_KEY);
  }

  const metadata = await tmdbJson(metadataUrl);
  tmdbFindCache.set(imdbId, metadata);
  return metadata;
}

async function posterFromOmdb(movie) {
  const metadataUrl = new URL('https://www.omdbapi.com/');
  metadataUrl.searchParams.set('apikey', process.env.OMDB_API_KEY);
  metadataUrl.searchParams.set('i', movie.id);
  metadataUrl.searchParams.set('type', 'movie');

  const metadataResponse = await fetch(metadataUrl);
  if (!metadataResponse.ok) {
    throw new Error(`OMDb returned ${metadataResponse.status}`);
  }

  const metadata = await metadataResponse.json();
  return metadata.Poster && metadata.Poster !== 'N/A' ? metadata.Poster : null;
}

async function posterFromTmdb(movie) {
  const metadata = await tmdbFindByImdbId(movie.id);
  const posterPath = metadata.movie_results?.[0]?.poster_path;
  return posterPath ? `https://image.tmdb.org/t/p/w500${posterPath}` : null;
}

async function profileFromTmdb(star) {
  const metadata = await tmdbFindByImdbId(star.id);
  const profilePath = metadata.person_results?.[0]?.profile_path;
  return profilePath ? `https://image.tmdb.org/t/p/w185${profilePath}` : null;
}

async function trailerFromTmdb(movie) {
  const metadata = await tmdbFindByImdbId(movie.id);
  const tmdbId = metadata.movie_results?.[0]?.id;
  if (!tmdbId) {
    return null;
  }

  const videosUrl = new URL(`https://api.themoviedb.org/3/movie/${tmdbId}/videos`);
  videosUrl.searchParams.set('language', 'en-US');
  if (process.env.TMDB_API_KEY) {
    videosUrl.searchParams.set('api_key', process.env.TMDB_API_KEY);
  }

  const videos = await tmdbJson(videosUrl);
  const candidates = (videos.results ?? [])
    .filter((video) => video.site === 'YouTube' && (video.type === 'Trailer' || video.type === 'Teaser') && video.key)
    .sort((left, right) => {
      if (left.official !== right.official) {
        return left.official ? -1 : 1;
      }
      if (left.type !== right.type) {
        return left.type === 'Trailer' ? -1 : 1;
      }
      return new Date(right.published_at ?? 0).getTime() - new Date(left.published_at ?? 0).getTime();
    });

  return candidates[0]?.key ?? null;
}

async function posterFromWikidata(movie) {
  const query = `
    SELECT ?image WHERE {
      ?film wdt:P345 "${movie.id}";
            wdt:P18 ?image.
    }
    LIMIT 1
  `;
  const metadataUrl = new URL('https://query.wikidata.org/sparql');
  metadataUrl.searchParams.set('query', query);

  const metadataResponse = await fetch(metadataUrl, {
    headers: {
      Accept: 'application/sparql-results+json',
      'User-Agent': 'FabflixPosterFetcher/1.0 (local development)'
    }
  });
  if (!metadataResponse.ok) {
    throw new Error(`Wikidata returned ${metadataResponse.status}`);
  }

  const metadata = await metadataResponse.json();
  return metadata.results?.bindings?.[0]?.image?.value ?? null;
}

async function resolvePosterUrl(movie) {
  if (posterSource === 'tmdb') {
    return posterFromTmdb(movie);
  }
  if (posterSource === 'omdb') {
    return posterFromOmdb(movie);
  }
  if (posterSource === 'wikidata') {
    return posterFromWikidata(movie);
  }
  throw new Error(`Unsupported POSTER_SOURCE: ${posterSource}`);
}

function extensionFor(url, contentType) {
  if (contentType?.includes('png')) return '.png';
  if (contentType?.includes('webp')) return '.webp';
  const guessedExtension = extname(new URL(url).pathname).toLowerCase();
  return guessedExtension && guessedExtension.length <= 5 ? guessedExtension : '.jpg';
}

function imageDimensions(buffer) {
  if (buffer[0] === 0x89 && buffer.toString('ascii', 1, 4) === 'PNG') {
    return { width: buffer.readUInt32BE(16), height: buffer.readUInt32BE(20) };
  }

  if (buffer[0] === 0xff && buffer[1] === 0xd8) {
    let offset = 2;
    while (offset < buffer.length) {
      if (buffer[offset] !== 0xff) {
        break;
      }
      const marker = buffer[offset + 1];
      const length = buffer.readUInt16BE(offset + 2);
      if (marker >= 0xc0 && marker <= 0xc3) {
        return { height: buffer.readUInt16BE(offset + 5), width: buffer.readUInt16BE(offset + 7) };
      }
      offset += 2 + length;
    }
  }

  return null;
}

function isPosterShaped(buffer) {
  const dimensions = imageDimensions(buffer);
  if (!dimensions) {
    return true;
  }
  const ratio = dimensions.width / dimensions.height;
  return ratio >= 0.55 && ratio <= 0.85;
}

const moviesById = new Map();
const starsById = new Map();
const starIdsByMovieId = new Map();
for (const path of movieDataPaths) {
  const sql = await readSql(path);
  const ratings = extractRatings(sql);
  for (const movie of extractMovies(sql)) {
    const rating = ratings.get(movie.id);
    if (rating) {
      movie.rating = rating.rating;
      movie.numVotes = rating.numVotes;
    }
    moviesById.set(movie.id, movie);
  }
  for (const [starId, star] of extractStars(sql)) {
    starsById.set(starId, star);
  }
  for (const [movieId, starIds] of extractStarRelations(sql)) {
    starIdsByMovieId.set(movieId, starIds);
  }
}

const movies = [...moviesById.values()].sort(
  (left, right) => scoreMovie(right) - scoreMovie(left) || right.year - left.year || left.title.localeCompare(right.title)
);

await mkdir(posterDir, { recursive: true });
console.log(`Fetching posters from ${posterSource} for up to ${limit} movies.`);

for (const movie of movies.slice(0, limit)) {
  let posterUrl;
  try {
    posterUrl = await resolvePosterUrl(movie);
  } catch (error) {
    console.warn(`Skipped ${movie.id}: ${error instanceof Error ? error.message : String(error)}`);
    continue;
  }

  if (!posterUrl) {
    console.warn(`Skipped ${movie.title}: no poster returned`);
    continue;
  }

  const imageResponse = await fetch(posterUrl, {
    headers: { 'User-Agent': 'FabflixPosterFetcher/1.0 (local development)' }
  });
  if (!imageResponse.ok) {
    console.warn(`Skipped ${movie.title}: poster returned ${imageResponse.status}`);
    continue;
  }

  const imageBuffer = Buffer.from(await imageResponse.arrayBuffer());
  if (!isPosterShaped(imageBuffer)) {
    console.warn(`Skipped ${movie.title}: image is not poster-shaped`);
    continue;
  }

  const extension = extensionFor(posterUrl, imageResponse.headers.get('content-type') ?? '');
  const posterPath = new URL(`../public/posters/${movie.id}${extension}`, scriptRoot);
  await writeFile(posterPath, imageBuffer);
  console.log(`Saved ${fileURLToPath(posterPath)}`);
}

const files = await readdir(posterDir);
const manifest = files
  .filter((file) => /^tt\d+\.(jpe?g|png|webp)$/i.test(file))
  .sort()
  .map((file) => {
    const id = file.replace(/\.(jpe?g|png|webp)$/i, '');
    return `  ${JSON.stringify(id)}: ${JSON.stringify(`/posters/${file}`)}`;
  });

const contents = `export const generatedPosterManifest: Record<string, string> = {\n${manifest.join(',\n')}\n};\n`;
await writeFile(manifestPath, contents);
console.log(`Updated ${fileURLToPath(manifestPath)}`);

const trailerEntries = [];
if (posterSource === 'tmdb' && trailerLimit > 0) {
  console.log(`Fetching TMDb trailer metadata for up to ${trailerLimit} movies.`);
  for (const movie of movies.slice(0, trailerLimit)) {
    let trailerKey;
    try {
      trailerKey = await trailerFromTmdb(movie);
    } catch (error) {
      console.warn(`Skipped ${movie.id}: ${error instanceof Error ? error.message : String(error)}`);
      continue;
    }

    if (!trailerKey) {
      console.warn(`Skipped ${movie.title}: no YouTube trailer returned`);
      continue;
    }

    trailerEntries.push(`  ${JSON.stringify(movie.id)}: ${JSON.stringify(trailerKey)}`);
  }
}

const trailerContents = `export const generatedTrailerManifest: Record<string, string> = {\n${trailerEntries.sort().join(',\n')}\n};\n`;
await writeFile(trailerManifestPath, trailerContents);
console.log(`Updated ${fileURLToPath(trailerManifestPath)}`);

if (posterSource === 'tmdb' && profileLimit > 0) {
  await mkdir(profileDir, { recursive: true });
  const starIds = [];
  for (const movie of movies.slice(0, limit)) {
    for (const starId of starIdsByMovieId.get(movie.id) ?? []) {
      if (!starIds.includes(starId) && starsById.has(starId)) {
        starIds.push(starId);
      }
    }
  }

  console.log(`Fetching TMDb profile images for up to ${profileLimit} stars.`);
  for (const starId of starIds.slice(0, profileLimit)) {
    const star = starsById.get(starId);
    let profileUrl;
    try {
      profileUrl = await profileFromTmdb(star);
    } catch (error) {
      console.warn(`Skipped ${starId}: ${error instanceof Error ? error.message : String(error)}`);
      continue;
    }

    if (!profileUrl) {
      console.warn(`Skipped ${star.name}: no profile image returned`);
      continue;
    }

    const imageResponse = await fetch(profileUrl, {
      headers: { 'User-Agent': 'FabflixPosterFetcher/1.0 (local development)' }
    });
    if (!imageResponse.ok) {
      console.warn(`Skipped ${star.name}: profile image returned ${imageResponse.status}`);
      continue;
    }

    const extension = extensionFor(profileUrl, imageResponse.headers.get('content-type') ?? '');
    const profilePath = new URL(`../public/people/${star.id}${extension}`, scriptRoot);
    await writeFile(profilePath, Buffer.from(await imageResponse.arrayBuffer()));
    console.log(`Saved ${fileURLToPath(profilePath)}`);
  }
}

const profileFiles = await readdir(profileDir).catch(() => []);
const profileManifest = profileFiles
  .filter((file) => /^nm\d+\.(jpe?g|png|webp)$/i.test(file))
  .sort()
  .map((file) => {
    const id = file.replace(/\.(jpe?g|png|webp)$/i, '');
    return `  ${JSON.stringify(id)}: ${JSON.stringify(`/people/${file}`)}`;
  });

const profileContents = `export const generatedProfileManifest: Record<string, string> = {\n${profileManifest.join(',\n')}\n};\n`;
await writeFile(profileManifestPath, profileContents);
console.log(`Updated ${fileURLToPath(profileManifestPath)}`);
