// --- Upgraded SAXParserSIM.java ---

import org.xml.sax.Attributes;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.*;
import java.sql.*;
import java.util.*;

public class SAXParserSIM extends DefaultHandler {
    private Connection connection;
    private BufferedWriter errorLogWriter;
    private ArrayList<String[]> starsInMovies = new ArrayList<>();
    private HashSet<String> existingMovieIds = new HashSet<>();
    private HashMap<String, Integer> movieStarCounts = new HashMap<>();

    private String tempVal;
    private String currentMovieId;
    private String currentStarName;

    private static final int MAX_STARS_PER_MOVIE = 6;

    public static void main(String[] args) {
        SAXParserSIM parser = new SAXParserSIM();
        parser.run();
    }

    public void run() {
        try {
            errorLogWriter = new BufferedWriter(new FileWriter("error_report.txt", true));
            connectToDatabase();
            loadExistingMovies();
            parseDocument();
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
        Class.forName("com.mysql.cj.jdbc.Driver");
        connection = DriverManager.getConnection("jdbc:mysql://localhost:3306/moviedb", "mytestuser", "My6$Password");
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
            saxParser.parse(new File("xml/casts124.xml"), this);
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
    }

    @Override
    public void characters(char[] ch, int start, int length) {
        tempVal += new String(ch, start, length);
    }

    @Override
    public void endElement(String uri, String localName, String qName) {
        try {
            switch (qName.toLowerCase()) {
                case "f":
                    currentMovieId = tempVal.trim();
                    break;
                case "a":
                    currentStarName = tempVal.trim();
                    break;
                case "m":
                    if (currentMovieId == null || currentMovieId.isEmpty()) {
                        logError("Missing movie ID in cast entry");
                        break;
                    }
                    if (currentStarName == null || currentStarName.isEmpty()) {
                        logError("Missing star name in cast entry for movie: " + currentMovieId);
                        break;
                    }
                    if (existingMovieIds.contains(currentMovieId)) {
                        String cleanedStarName = cleanStarName(currentStarName);
                        if (cleanedStarName != null) {
                            int starCount = movieStarCounts.getOrDefault(currentMovieId, 0);
                            if (starCount < MAX_STARS_PER_MOVIE) {
                                starsInMovies.add(new String[]{cleanedStarName, currentMovieId});
                                movieStarCounts.put(currentMovieId, starCount + 1);
                            } else {
                                logError("Too many stars for movie: " + currentMovieId);
                            }
                        } else {
                            logError("Invalid cleaned star name for movie: " + currentMovieId);
                        }
                    } else {
                        logError("Movie ID not found in database: " + currentMovieId);
                    }
                    currentMovieId = null;
                    currentStarName = null;
                    break;
            }
        } catch (Exception e) {
            logError("Exception in endElement: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void insertData() {
        try {
            PreparedStatement ps = connection.prepareStatement("INSERT IGNORE INTO stars_in_movies (starId, movieId) SELECT s.id, ? FROM stars s WHERE s.name = ?");

            for (String[] sim : starsInMovies) {
                ps.setString(1, sim[1]);
                ps.setString(2, sim[0]);
                ps.addBatch();
            }
            ps.executeBatch();
            ps.close();
        } catch (Exception e) {
            logError("Exception during DB insert: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private String cleanStarName(String rawName) {
        String cleaned = rawName.trim().replaceAll("[^a-zA-Z\\s]", "").replaceAll("\\s+", " ").trim();
        if (cleaned.isEmpty() || cleaned.length() < 5 || cleaned.length() > 40) return null;
        if (cleaned.split("\\s+").length > 3) return null;
        return capitalizeWords(cleaned);
    }

    private String capitalizeWords(String str) {
        String[] words = str.split(" ");
        StringBuilder sb = new StringBuilder();
        for (String word : words) {
            if (!word.isEmpty()) {
                sb.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1).toLowerCase()).append(" ");
            }
        }
        return sb.toString().trim();
    }

    private void loadExistingMovies() throws SQLException {
        Statement statement = connection.createStatement();
        ResultSet rs = statement.executeQuery("SELECT id FROM movies");

        while (rs.next()) {
            existingMovieIds.add(rs.getString("id"));
        }

        rs.close();
        statement.close();
    }
}
