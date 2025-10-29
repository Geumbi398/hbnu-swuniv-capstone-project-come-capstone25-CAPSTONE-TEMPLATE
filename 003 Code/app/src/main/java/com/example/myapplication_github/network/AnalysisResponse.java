package com.example.myapplication_github.network;

import com.google.gson.annotations.SerializedName;

import java.util.List;

import javax.xml.transform.Result;

public class AnalysisResponse {
    @SerializedName("message")
    private String message;

    @SerializedName("file_path")
    private String filePath;

    @SerializedName("result")
    private String result;

    public String getMessage() {
        return message;
    }

    public String getFilePath() {
        return filePath;
    }

    public String getResult() {
        return result;
    }

    public boolean isFake() {
        if (result != null) {
            return result.contains("[Fake]");
        }
        return false;
    }

    public boolean isReal() {
        if (result != null) {
            return result.contains("[Real]");
        }
        return false;
    }

    public boolean isNoFace() {
        if (result != null) {
            return result.contains("[NoFace]");
        }
        return false;
    }
}
