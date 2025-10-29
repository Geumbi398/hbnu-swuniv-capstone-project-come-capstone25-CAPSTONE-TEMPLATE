package com.example.myapplication_github.ui.home;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class HomeViewModel extends ViewModel {

    private final MutableLiveData<String> mText;

    public HomeViewModel() {
        mText = new MutableLiveData<>();
        mText.setValue("안녕하세요,\nAI Holmes\n입니다."); //This is home fragment
    }

    public LiveData<String> getText() {
        return mText;
    }
}