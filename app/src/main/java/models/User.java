package models;

public class User {

    private String name, email, username, token /*password*/;
    private double latitude, longitude;

    public User() {

    }

    public User(String name, String username) {
        this.name = name.trim();
        this.username = username.trim();
    }

    public User(String name, String email, String username) {
        this.name = name.trim();
        this.email = email.trim();
        this.username = username.trim();
    }

    public User(String name, String username, String email, double latitude, double longitude) {
        this.name = name.trim();
        this.username = username.trim();
        this.email = email.trim();
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public User(String name, String email, String username, String token, double latitude,
                double longitude) {
        this.name = name;
        this.email = email;
        this.username = username;
        this.token = token;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    /*public User(String name, String email, String username, String password, double latitude,
                double longitude) {
        this.name = name;
        this.email = email;
        this.username = username;
        this.password = password;
        this.latitude = latitude;
        this.longitude = longitude;
    }*/

    public String getName() {
        return name.trim();
    }

    public void setName(String name) {
        this.name = name.trim();
    }

    public String getEmail() {
        return email.trim();
    }

    public void setEmail(String email) {
        this.email = email.trim();
    }

    public String getUsername() {
        return username.trim();
    }

    public void setUsername(String username) {
        this.username = username.trim();
    }

    /*public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }*/

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }
}