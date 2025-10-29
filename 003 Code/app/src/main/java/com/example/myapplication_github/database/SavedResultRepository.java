package com.example.myapplication_github.database;

import android.app.Application;

import androidx.lifecycle.LiveData;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SavedResultRepository {
    private SavedResultDao savedResultDao;
    private LiveData<List<SavedResult>> allSavedResults;
    private ExecutorService executorService;

    public SavedResultRepository(Application application) {
        AppDatabase database = AppDatabase.getDatabase(application);
        savedResultDao = database.savedResultDao();
        allSavedResults = savedResultDao.getAllSavedResult();
        executorService = Executors.newFixedThreadPool(2);
    }

    public LiveData<List<SavedResult>> getAllSavedResults() {
        return allSavedResults;
    }

    public void insert(SavedResult savedResult) {
        executorService.execute(()->savedResultDao.insert(savedResult));
    }

    public void delete(SavedResult savedResult){
        executorService.execute(()->savedResultDao.delete(savedResult));
    }

    public void update(SavedResult savedResult){
        executorService.execute(()-> savedResultDao.update(savedResult));
    }

    public LiveData<List<SavedResult>> getAllSavedResult(String userId){
        return savedResultDao.getAllSavedResultByUser(userId);
    }

    public void cleanup(){
        if (executorService != null && executorService.isShutdown()){
            executorService.shutdown();
        }
    }
}
