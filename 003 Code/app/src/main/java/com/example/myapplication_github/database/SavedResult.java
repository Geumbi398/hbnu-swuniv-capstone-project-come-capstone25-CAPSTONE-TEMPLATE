package com.example.myapplication_github.database;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

@Entity(tableName = "saved_result")
public class SavedResult {
    @PrimaryKey(autoGenerate = true)
    private int id;
    private String serverId;
    private String userId;
    private String imagePath;
    private String serverImageUrl;
    private String result;
    private long savedDate;
    private String title;
    private boolean isUpload;
    private boolean isDelete;

    public SavedResult(String userId, String imagePath, String serverImageUrl, String result, long savedDate, String title){
        this.imagePath = imagePath;
        this.result = result;
        this.savedDate = savedDate;
        this.title = title;
        this.userId = userId;
        this.serverImageUrl = serverImageUrl;
        this.isUpload = false;
        this.isDelete = false;
    }

    public int getId(){return id;}
    public void setId(int id) {
        this.id = id;
    }

    public String getImagePath() {
        return imagePath;
    }

    public void setImagePath(String imagePath) {
        this.imagePath = imagePath;
    }

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }

    public long getSavedDate() {
        return savedDate;
    }

    public void setSavedDate(long savedDate) {
        this.savedDate = savedDate;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public boolean isDelete() {
        return isDelete;
    }

    public void setDelete(boolean delete) {
        isDelete = delete;
    }

    public boolean isUpload() {
        return isUpload;
    }

    public void setUpload(boolean upload) {
        isUpload = upload;
    }

    public String getServerId() {
        return serverId;
    }

    public void setServerId(String serverId) {
        this.serverId = serverId;
    }

    public String getServerImageUrl() {
        return serverImageUrl;
    }

    public void setServerImageUrl(String serverImageUrl) {
        this.serverImageUrl = serverImageUrl;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public double getConfidence(){
        try{
            if (result != null && result.contains("score: ")){
                String[] part = result.split("score: ");
                if (part.length > 1){
                    String score = part[1].split("\\)")[0];
                    return Double.parseDouble(score);
                }
            }
        }catch (Exception e){
            e.printStackTrace();
        }
        return 0.0;
    }

    public String getFormatDate(){
        return new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date(savedDate));
    }

    public boolean isReal(){
        return result != null && result.contains("[Real]");
    }

    public boolean isFake(){
        return result != null && result.contains("[Fake]");
    }

    public boolean isNoFake(){
        return result != null && result.contains("[NoFace]");
    }
}
