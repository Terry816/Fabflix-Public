# Fabflix

Fabflix has been migrated from a Tomcat servlet webapp to:

- Spring Boot API in `cs122b-project1-api-example-main/src/main/java`
- React + TypeScript frontend in `cs122b-project1-api-example-main/frontend`
- PostgreSQL schema/data scripts in `sql`

## Local Development

Backend:

```bash
cd cs122b-project1-api-example-main
export SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/moviedb
export SPRING_DATASOURCE_USERNAME=postgres
export SPRING_DATASOURCE_PASSWORD=your-password
export JWT_SECRET=change-me-to-at-least-32-characters
mvn spring-boot:run
```

Frontend:

```bash
cd cs122b-project1-api-example-main/frontend
npm install
npm run dev
```

The Vite dev server proxies `/api` requests to `http://localhost:8080`.

## Build

Frontend only:

```bash
cd cs122b-project1-api-example-main/frontend
npm run build
```

## Tests

Frontend unit tests:

```bash
cd cs122b-project1-api-example-main/frontend
npm test
```

End-to-end browser tests:

```bash
cd cs122b-project1-api-example-main/frontend
npx playwright install chromium
npm run test:e2e
```

Backend build:

```bash
cd cs122b-project1-api-example-main
mvn -DskipTests package
```

GitHub Actions runs the frontend Jest suite, frontend build, backend Maven package, and Playwright E2E suite against PostgreSQL.

## Data Refresh

The base school dataset still lives in `sql/movie-data.sql`. Modern movie data is generated from IMDb's non-commercial TSV datasets into `sql/modern-movie-data.sql`.

Regenerate the supplemental modern seed:

```bash
MOVIE_LIMIT=3000 MIN_YEAR=2010 MAX_YEAR=2025 MIN_VOTES=5000 node scripts/generate-modern-movie-data.mjs
```

Load a fresh PostgreSQL database in this order:

```bash
psql -d moviedb -f sql/create_table.sql
psql -d moviedb -f sql/movie-data.sql
psql -d moviedb -f sql/modern-movie-data.sql
```

## Movie Posters

The UI is poster-ready and falls back to generated poster art when a movie does not have a downloaded image.

To download actual poster files by IMDb ID:

```bash
cd cs122b-project1-api-example-main/frontend
POSTER_SOURCE=tmdb TMDB_BEARER_TOKEN=your-read-token POSTER_LIMIT=200 PROFILE_LIMIT=300 TRAILER_LIMIT=120 npm run posters:fetch
# or
POSTER_SOURCE=omdb OMDB_API_KEY=your-api-key POSTER_LIMIT=200 npm run posters:fetch
# no key required, but sparse because it only uses Wikimedia Commons images linked from Wikidata
POSTER_SOURCE=wikidata POSTER_LIMIT=200 npm run posters:fetch
```

The script stores poster images in `public/posters`, TMDb person images in `public/people`, and updates the generated poster/profile/trailer manifests that the React cards, movie detail page, star detail page, and hero backdrops read automatically. PosterLens and MM-IMDb are also poster datasets worth using for research/private projects, but both are distributed through Kaggle and should be reviewed for license/redistribution terms before committing images to a public repository.

Full container:

```bash
cd cs122b-project1-api-example-main
docker build -t fabflix .
docker run --env-file .env -p 8080:8080 fabflix
```

## Default Seed Accounts

The PostgreSQL schema includes these sample accounts:

- Customer: `terry@test.com` / `password`
- Employee: `admin@fabflix.com` / `admin`
