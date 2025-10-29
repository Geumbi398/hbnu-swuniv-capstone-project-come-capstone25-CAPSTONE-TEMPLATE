package com.example.myapplication_github.database;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface SavedResultDao {
    @Insert
    long insert(SavedResult savedResult);

    @Update
    void update(SavedResult savedResult);

    @Delete
    void delete(SavedResult savedResult);

    @Query("UPDATE saved_result SET isDelete = 1 WHERE id = :id")
    void softDelete(int id);

    @Query("SELECT * FROM saved_result WHERE userId = :userId AND isDelete = 0 ORDER BY savedDate DESC")
    LiveData<List<SavedResult>> getAllSavedResultByUser(String userId);

    @Query("DELETE FROM saved_result WHERE isDelete = 1 AND isUpload = 1")
    void DeleteResult();

    @Query("SELECT * FROM saved_result WHERE isUpload = 0 AND isDelete = 0")
    List<SavedResult> getUnuploadResult();

    @Query("SELECT * FROM saved_result WHERE serverId = :serverId")
    SavedResult getByServerId(String serverId);

    @Query("SELECT * FROM saved_result WHERE isDelete = 0 ORDER BY savedDate DESC")
    LiveData<List<SavedResult>> getAllSavedResult();
}
