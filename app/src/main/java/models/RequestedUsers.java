package models;

import java.util.List;

/**
 * Created by progamer on 01/05/17.
 */

public class RequestedUsers {

    private String requestId;
    private List<String> usersTokens;

    public RequestedUsers() {}

    public RequestedUsers(String reqId, List<String> users) {
        requestId = reqId;
        usersTokens = users;
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public List<String> getUsersTokens() {
        return usersTokens;
    }

    public void setUsersTokens(List<String> usersTokens) {
        this.usersTokens = usersTokens;
    }
}
