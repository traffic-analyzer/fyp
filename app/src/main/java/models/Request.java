package models;

import com.google.android.gms.maps.model.LatLng;

import java.util.List;

/**
 * Created by progamer on 27/04/17.
 */

public class Request {

    private List<LatLng> route;
    private String userId;
    private long timestamp;

    public Request() {}

    public Request(List<LatLng> route, String userId, long timestamp) {
        this.route = route;
        this.userId = userId;
        this.timestamp = timestamp;
    }

    public List<LatLng> getRoute() {
        return route;
    }

    public void setRoute(List<LatLng> route) {
        this.route = route;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}