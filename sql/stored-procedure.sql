-- PostgreSQL replacement for the old MySQL add_movie stored procedure.
-- The Java app now inserts movies directly, but this function is available
-- if you want to call the same operation from pgAdmin.

DROP FUNCTION IF EXISTS add_movie(VARCHAR, INT, VARCHAR, VARCHAR, VARCHAR);

CREATE OR REPLACE FUNCTION add_movie(
    p_title VARCHAR(100),
    p_year INT,
    p_director VARCHAR(100),
    p_star_name VARCHAR(100),
    p_genre_name VARCHAR(32)
)
RETURNS TABLE (
    movie_id VARCHAR(10),
    star_id VARCHAR(10),
    genre_id INT,
    already_exists BOOLEAN
)
LANGUAGE plpgsql
AS $$
DECLARE
    max_movie_id INT;
    max_star_id INT;
BEGIN
    SELECT m.id INTO movie_id
    FROM movies m
    WHERE m.title = p_title
      AND m.year = p_year
      AND m.director = p_director
    LIMIT 1;

    IF movie_id IS NOT NULL THEN
        already_exists := TRUE;
        star_id := NULL;
        genre_id := NULL;
        RETURN NEXT;
        RETURN;
    END IF;

    SELECT COALESCE(MAX(CAST(SUBSTRING(id FROM 2) AS INT)), 0) + 1
    INTO max_movie_id
    FROM movies
    WHERE id LIKE 'K%';

    movie_id := CONCAT('K', max_movie_id);

    INSERT INTO movies (id, title, year, director)
    VALUES (movie_id, p_title, p_year, p_director);

    INSERT INTO ratings (movieId, rating, numVotes)
    VALUES (movie_id, ROUND((random() * 10)::numeric, 1), 0);

    SELECT s.id INTO star_id
    FROM stars s
    WHERE s.name = p_star_name
    LIMIT 1;

    IF star_id IS NULL THEN
        SELECT COALESCE(MAX(CAST(SUBSTRING(id FROM 2) AS INT)), 0) + 1
        INTO max_star_id
        FROM stars
        WHERE id LIKE 'S%';

        star_id := CONCAT('S', max_star_id);

        INSERT INTO stars (id, name)
        VALUES (star_id, p_star_name);
    END IF;

    SELECT g.id INTO genre_id
    FROM genres g
    WHERE g.name = p_genre_name
    LIMIT 1;

    IF genre_id IS NULL THEN
        INSERT INTO genres (name)
        VALUES (p_genre_name)
        RETURNING id INTO genre_id;
    END IF;

    INSERT INTO stars_in_movies (starId, movieId)
    VALUES (star_id, movie_id)
    ON CONFLICT DO NOTHING;

    INSERT INTO genres_in_movies (genreId, movieId)
    VALUES (genre_id, movie_id)
    ON CONFLICT DO NOTHING;

    already_exists := FALSE;
    RETURN NEXT;
END;
$$;
