package com.example.myapplication_github.ui.gallery;

import android.database.Cursor;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.myapplication_github.R;
import com.example.myapplication_github.database.SavedResult;
import com.example.myapplication_github.databinding.FragmentGalleryBinding;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class GalleryFragment extends Fragment {

    private FragmentGalleryBinding binding;
    private GalleryViewModel galleryViewModel; // ViewModel 선언
    private RecyclerView recyclerView;
    private GalleryAdapter adapter;
    private TextView textView;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentGalleryBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        // ✅ ViewModel 초기화 (Factory 없이 직접 생성)
        galleryViewModel = new ViewModelProvider(this).get(GalleryViewModel.class);

        // ✅ UI 업데이트
        textView = binding.textGallery;
        galleryViewModel.getText().observe(getViewLifecycleOwner(), textView::setText);

        recyclerView = binding.recycleView;
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
//        swipeRefreshLayout = root.findViewById(R.id.swipeRefresh);

        adapter = new GalleryAdapter(getContext(), this::onDeleteClick);
        recyclerView.setAdapter(adapter);

        galleryViewModel.getAllSavedResult().observe(getViewLifecycleOwner(), savedResults -> {
            if (savedResults == null || savedResults.isEmpty()){
                recyclerView.setVisibility(View.GONE);
                textView.setVisibility(View.VISIBLE);
                textView.setText("저장된 결과가 없습니다.");
            }else {
                recyclerView.setVisibility(View.VISIBLE);
                textView.setVisibility(View.GONE);
                adapter.updateImage(savedResults);
            }
        });
        return root;
    }

    //목록 삭제
    private void onDeleteClick(SavedResult savedResult, int position){
        if (getContext() == null)
            return;

        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("삭제");
        builder.setMessage("해당 목록을 삭제 하시겠습니까?");
        builder.setPositiveButton("삭제", (dialog, which) -> {
            galleryViewModel.delete(savedResult);
            deleteImageFile(savedResult.getImagePath());
            Toast.makeText(getContext(), "삭제되었습니다.", Toast.LENGTH_SHORT).show();
        });
        builder.setNegativeButton("취소", null);
        builder.show();
    }

    //삭제 여부 확인, 로그 출력
    private void deleteImageFile(String imagePath){
        if (imagePath == null || imagePath.isEmpty()) return;

        try {
            File file = new File(imagePath);
            if (file.exists()){
                boolean deleted = file.delete();
                if (!deleted)
                    Log.w("GalleryFragment", "Failed to delete file: " + imagePath);
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}