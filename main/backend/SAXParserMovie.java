// --- Upgraded SAXParserMovie.java ---

import org.xml.sax.Attributes;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.*;
import java.sql.*;
import java.util.*;

public class SAXParserMovie extends DefaultHandler {
    private Connection connection;
    private BufferedWriter errorLogWriter;
    private ArrayList<String[]> movies = new ArrayList<>();
    private ArrayList<String[]> genresInMovies = new ArrayList<>();
    private Set<String> cleanedGenres = new HashSet<>();
    private Map<String, Integer> genreCounts = new HashMap<>();
    private HashMap<String, HashSet<String>> movieGenres = new HashMap<>();

    private String tempVal;
    private String directorName;
    private String movieTitle;
    private String movieYear;
    private String movieId;

    private static final int MIN_GENRE_FREQUENCY = 10;

    public static void main(String[] args) {
        SAXParserMovie parser = new SAXParserMovie();
        parser.run();
    }

    public void run() {
        try {
            errorLogWriter = new BufferedWriter(new FileWriter("error_report.txt", true));
            connectToDatabase();
            parseDocument();
            applyMinimumFrequencyFilter();
            insertData();
        } catch (Exception e) {
            logError("Exception during run: " + e.getMessage());
            e.printStackTrace();
        } finally {
            try {
                if (errorLogWriter != null) errorLogWriter.close();
                if (connection != null) disconnectDatabase();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void connectToDatabase() throws Exception {
        Class.forName("org.postgresql.Driver");
        connection = DriverManager.getConnection(
                System.getenv().getOrDefault("DB_URL", "jdbc:postgresql://localhost:5432/moviedb"),
                System.getenv().getOrDefault("DB_USER", "postgres"),
                System.getenv("DB_PASSWORD"));
        connection.setAutoCommit(false);
    }

    private void disconnectDatabase() throws Exception {
        connection.commit();
        connection.close();
    }

    private void parseDocument() {
        try {
            SAXParserFactory factory = SAXParserFactory.newInstance();
            SAXParser saxParser = factory.newSAXParser();
            saxParser.parse(new File("xml/mains243.xml"), this);
        } catch (Exception e) {
            logError("Exception during XML parsing: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void logError(String message) {
        try {
            errorLogWriter.write("[ERROR] " + message + "\n");
            errorLogWriter.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes) {
        tempVal = "";
        if (qName.equalsIgnoreCase("directorfilms")) {
            directorName = "";
        } else if (qName.equalsIgnoreCase("film")) {
            movieId = "";
            movieTitle = "";
            movieYear = "";
        }
    }

    @Override
    public void characters(char[] ch, int start, int length) {
        tempVal += new String(ch, start, length);
    }

    @Override
    public void endElement(String uri, String localName, String qName) {
        try {
            switch (qName.toLowerCase()) {
                case "dirname":
                    directorName = tempVal.trim();
                    break;
                case "fid":
                    movieId = tempVal.trim();
                    break;
                case "t":
                    movieTitle = tempVal.trim();
                    break;
                case "year":
                    movieYear = tempVal.trim();
                    break;
                case "cat":
                    if (!movieId.isEmpty() && !tempVal.trim().isEmpty()) {
                        String cleanedGenre = cleanGenre(tempVal);
                        if (cleanedGenre != null) {
                            movieGenres.computeIfAbsent(movieId, k -> new HashSet<>()).add(cleanedGenre);
                            cleanedGenres.add(cleanedGenre);
                            genreCounts.put(cleanedGenre, genreCounts.getOrDefault(cleanedGenre, 0) + 1);
                        }
                    }
                    break;
                case "film":
                    if (movieId.isEmpty() || movieTitle.isEmpty() || movieYear.isEmpty() || directorName.isEmpty()) {
                        logError("Missing fields in movie entry: " + movieId);
                        break;
                    }
                    if (movieGenres.containsKey(movieId)) {
                        if (movieGenres.get(movieId).size() > 10) {
                            logError("Skipped movie (too many genres): " + movieId);
                        } else {
                            movies.add(new String[]{movieId, movieTitle, movieYear, directorName});
                        }
                    }
                    break;
            }
        } catch (Exception e) {
            logError("Exception in endElement: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private String cleanGenre(String rawGenre) {
        String cleaned = rawGenre.trim().toLowerCase().replaceAll("[^a-z\\s]", "").replaceAll("\\s+", " ").trim();
        if (cleaned.isEmpty() || cleaned.length() > 20 || cleaned.length() < 3) return null;
        if (cleaned.matches(".*\\d.*")) return null;
        return cleaned;
    }

    private void applyMinimumFrequencyFilter() {
        Set<String> frequentGenres = new HashSet<>();
        for (String genre : cleanedGenres) {
            if (genreCounts.getOrDefault(genre, 0) >= MIN_GENRE_FREQUENCY) {
                frequentGenres.add(genre);
            }
        }
        cleanedGenres = frequentGenres;

        for (String[] movie : movies) {
            String id = movie[0];
            if (movieGenres.containsKey(id)) {
                for (String genre : movieGenres.get(id)) {
                    if (cleanedGenres.contains(genre)) {
                        genresInMovies.add(new String[]{id, genre});
                    }
                }
            }
        }
    }

    private void insertData() {
        try {
            PreparedStatement movieStmt = connection.prepareStatement("INSERT IGNORE INTO movies (id, title, year, director) VALUES (?, ?, ?, ?)");
            PreparedStatement ratingStmt = connection.prepareStatement("INSERT IGNORE INTO ratings (movieId, rating, numVotes) VALUES (?, ?, 0)");
            PreparedStatement genreStmt = connection.prepareStatement("INSERT IGNORE INTO genres (name) VALUES (?)");
            PreparedStatement genreInMovieStmt = connection.prepareStatement("INSERT IGNORE INTO genres_in_movies (genreId, movieId) SELECT g.id, ? FROM genres g WHERE g.name = ?");

            for (String[] movie : movies) {
                movieStmt.setString(1, movie[0]);
                movieStmt.setString(2, movie[1]);
                try {
                    movieStmt.setInt(3, Integer.parseInt(movie[2].replaceAll("[^0-9]", "")));
                } catch (NumberFormatException e) {
                    movieStmt.setInt(3, 0);
                    logError("Invalid year format for movie: " + movie[0]);
                }
                movieStmt.setString(4, movie[3]);
                movieStmt.addBatch();
                float randomRating = Math.round((1.0 + Math.random() * 9.0) * 10.0) / 10.0f;
                ratingStmt.setString(1, movie[0]);
                ratingStmt.setFloat(2, randomRating);
                ratingStmt.addBatch();
            }
            movieStmt.executeBatch();
            ratingStmt.executeBatch();

            for (String genreName : cleanedGenres) {
                genreStmt.setString(1, capitalizeFirstLetter(genreName));
                genreStmt.addBatch();
            }
            genreStmt.executeBatch();

            for (String[] gim : genresInMovies) {
                genreInMovieStmt.setString(1, gim[0]);
                genreInMovieStmt.setString(2, gim[1]);
                genreInMovieStmt.addBatch();
            }
            genreInMovieStmt.executeBatch();
        } catch (Exception e) {
            logError("Exception during DB insert: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private String capitalizeFirstLetter(String str) {
        if (str.isEmpty()) return str;
        return Character.toUpperCase(str.charAt(0)) + str.substring(1);
    }
}
