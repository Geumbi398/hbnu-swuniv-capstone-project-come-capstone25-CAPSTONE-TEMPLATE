package com.example.myapplication_github.ui.gallery;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

public class GalleryViewModelFactory implements ViewModelProvider.Factory {
    private Application application;

    public GalleryViewModelFactory(Application application) {
        this.application = application;
    }

    @NonNull
    //@Override
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        if (modelClass.isAssignableFrom(GalleryViewModel.class)) {
            return (T) new GalleryViewModel(application);  // 기본 생성자 사용
        }
        throw new IllegalArgumentException("Unknown ViewModel class");
    }
}
