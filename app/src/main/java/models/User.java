package models;

public class User {

    private String name, email, username /*password*/;
    private double latitude, longitude;

    public User() {

    }

    public User(String name, String username) {
        this.name = name;
        this.username = username;
    }

    public User(String name, String email, String username) {
        this.name = name;
        this.email = email;
        this.username = username;
    }

    public User(String name, String username, String email, double latitude, double longitude) {
        this.name = name;
        this.username = username;
        this.email = email;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    /*public User(String name, String email, String username, String password) {
        this.name = name;
        this.email = email;
        this.username = username;
        this.password = password;
    }

    public User(String name, String email, String username, String password, double latitude,
                double longitude) {
        this.name = name;
        this.email = email;
        this.username = username;
        this.password = password;
        this.latitude = latitude;
        this.longitude = longitude;
    }*/

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
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
}