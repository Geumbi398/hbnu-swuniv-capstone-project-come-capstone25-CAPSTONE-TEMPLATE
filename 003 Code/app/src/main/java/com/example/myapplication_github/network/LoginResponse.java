package com.example.myapplication_github.network;

public class LoginResponse {
    private boolean success;
    private String message;
    private String token;
    private String username;

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getToken() {
        return token;
    }
}
