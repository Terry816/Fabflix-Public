public class User {
    private final int id;
    private final String email;
    private final String firstName;

    public User(int id, String email, String firstName) {
        this.id = id;
        this.email = email;
        this.firstName = firstName;
    }

    public int getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getFirstName() {
        return firstName;
    }
}
