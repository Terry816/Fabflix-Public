DROP PROCEDURE IF EXISTS add_movie;

DELIMITER $$

CREATE PROCEDURE add_movie(
    IN p_title VARCHAR(100),
    IN p_year INT,
    IN p_director VARCHAR(100),
    IN p_star_name VARCHAR(100),
    IN p_genre_name VARCHAR(32),
    OUT p_movie_id VARCHAR(10),
    OUT p_star_id VARCHAR(10),
    OUT p_genre_id INT,
    OUT p_already_exists BOOLEAN
)
BEGIN
    DECLARE max_movie_id INT;
    DECLARE max_star_id INT;

    DECLARE exit handler for SQLEXCEPTION
    BEGIN
        ROLLBACK;
        SET p_already_exists = TRUE;
    END;

    START TRANSACTION;

    -- Assume new by default
    SET p_already_exists = FALSE;

    -- Check if movie already exists
    SELECT m.id INTO p_movie_id
    FROM movies m
    WHERE m.title = p_title AND m.year = p_year AND m.director = p_director
    LIMIT 1;

    IF p_movie_id IS NOT NULL THEN
        -- Movie already exists
        SET p_already_exists = TRUE;
        ROLLBACK;
    ELSE
        -- Create new Movie ID
        SELECT IFNULL(MAX(CAST(SUBSTRING(id, 2) AS UNSIGNED)), 0) + 1 INTO max_movie_id FROM movies;
        SET p_movie_id = CONCAT('K', max_movie_id);

        -- INSERT IGNORE to prevent crashing replication if ID already exists
        INSERT IGNORE INTO movies (id, title, year, director)
        VALUES (p_movie_id, p_title, p_year, p_director);

        INSERT IGNORE INTO ratings (movieId, rating, numVotes)
        VALUES (p_movie_id, ROUND(RAND() * 10, 1), 0);

        -- Handle star
        SELECT s.id INTO p_star_id
        FROM stars s
        WHERE s.name = p_star_name
        LIMIT 1;

        IF p_star_id IS NULL THEN
            SELECT IFNULL(MAX(CAST(SUBSTRING(id, 2) AS UNSIGNED)), 0) + 1 INTO max_star_id FROM stars;
            SET p_star_id = CONCAT('S', max_star_id);

            INSERT IGNORE INTO stars (id, name)
            VALUES (p_star_id, p_star_name);
        END IF;

        -- Handle genre
        SELECT id INTO p_genre_id
        FROM genres
        WHERE name = p_genre_name
        LIMIT 1;

        IF p_genre_id IS NULL THEN
            INSERT IGNORE INTO genres (name)
            VALUES (p_genre_name);

            SET p_genre_id = LAST_INSERT_ID();
        END IF;

        -- Link star and movie
        INSERT IGNORE INTO stars_in_movies (starId, movieId)
        VALUES (p_star_id, p_movie_id);

        -- Link genre and movie
        INSERT IGNORE INTO genres_in_movies (genreId, movieId)
        VALUES (p_genre_id, p_movie_id);

        COMMIT;
    END IF;
END$$

DELIMITER ;
