package com.example.myapplication_github.network;

public class RegisterRequest {
    private String username;
    private String password;
    private String email;

    public RegisterRequest(String username, String password, String email){
        this.username = username;
        this.password = password;
        this.email = email;
    }

    public String getId() {
        return username;
    }

    public void setId(String username) {
        this.username = username;
    }
}
