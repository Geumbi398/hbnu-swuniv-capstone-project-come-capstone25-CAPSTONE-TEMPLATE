package com.example.myapplication_github.network;

public class LoginRequest {
    private String username;
    private String password;

    public LoginRequest(String username, String password){
        this.username = username;
        this.password = password;
    }

    public String getId() {
        return username;
    }


    public void setId(String username) {
        this.username = username;
    }

}
