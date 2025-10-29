package com.example.myapplication_github.ui.gallery;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.myapplication_github.database.SavedResult;
import com.example.myapplication_github.database.SavedResultRepository;

import java.util.List;

public class GalleryViewModel extends AndroidViewModel {
    private SavedResultRepository resultRepository;
    private LiveData<List<SavedResult>> allSavedResult;
    private final MutableLiveData<String> mText;

    // 기본 생성자
    public GalleryViewModel(@NonNull Application application) {
        super(application);
        resultRepository = new SavedResultRepository(application);
        allSavedResult = resultRepository.getAllSavedResults();
        mText = new MutableLiveData<>();
        mText.setValue(""); // 기본 값 설정 This is home fragment
    }

    public LiveData<String> getText() {
        return mText;
    }
    public LiveData<List<SavedResult>>getAllSavedResult(){
        return allSavedResult;
    }
    public void insert(SavedResult savedResult){
        resultRepository.insert(savedResult);
    }
    public void delete(SavedResult savedResult){
        resultRepository.delete(savedResult);
    }
}
