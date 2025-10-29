package com.example.myapplication_github.ui.gallery;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.health.connect.TimeInstantRangeFilter;
import android.net.Uri;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.myapplication_github.R;
import com.example.myapplication_github.database.SavedResult;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class GalleryAdapter extends RecyclerView.Adapter<GalleryAdapter.ViewHolder> {
    private List<SavedResult> savedResults;
    public Context context;
    private OnDeleteClickListener deleteClickListener;

    public interface OnDeleteClickListener {
        void onDeleteClick(SavedResult savedResult, int position);
    }

    public GalleryAdapter(Context context, OnDeleteClickListener deleteClickListener) {
        this.context = context;
        this.deleteClickListener = deleteClickListener;
        this.savedResults = new ArrayList<>();
    }

    public void updateImage(List<SavedResult> newResults) {
        this.savedResults = newResults;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_gallery, parent, false);

        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        SavedResult result = savedResults.get(position);
        Log.d("GalleryAdapter", "Image path: " + result.getImagePath());

        File file = new File(result.getImagePath());
        Log.d("GalleryAdapter", "File exists: " + file.exists());
        Log.d("GalleryAdapter", "File can read: " + file.canRead());

        if (result.getImagePath().startsWith("http://") || result.getImagePath().startsWith("https://")) {
            Log.d("GalleryAdapter", "Loading from HTTP URL: " + result.getImagePath());
            Glide.with(context).load(result.getImagePath()).override(120, 120).centerCrop().
                    placeholder(R.drawable.ic_launcher_background).error(R.drawable.ic_launcher_foreground).into(holder.imageView);
        } else {
            File file1 = new File(result.getImagePath());

            Log.d("GalleryAdapter", "Loading from local file: " + result.getImagePath());
            Log.d("GalleryAdapter", "File exists: " + file.exists());
            Log.d("GalleryAdapter", "File can read: " + file.canRead());

            if (file1.exists()) {
                Glide.with(context).load(file1).override(120, 120).centerCrop().into(holder.imageView);
            } else {
                holder.imageView.setImageResource(R.drawable.ic_launcher_foreground);
            }
        }

        String resultText = getResult(result);
        holder.imageText.setText(resultText);

        if (result.getResult().contains("[Real]")) {
            holder.linearLayout.setBackground(ContextCompat.getDrawable(context, R.drawable.true_button));
        } else {
            holder.linearLayout.setBackground(ContextCompat.getDrawable(context, R.drawable.false_button));
        }

        holder.deleteButton.setOnClickListener(view -> {
            if (deleteClickListener != null) {
                int adapterPosition = holder.getAdapterPosition();
                deleteClickListener.onDeleteClick(result, adapterPosition);
            }
        });
    }

    private String getResult(SavedResult savedResult) {
        String analysis = savedResult.getResult();

        double confidence = getConfidence(analysis);

        if (analysis.contains("[NoFace]")) {
            return String.format("얼굴을 인식 할 수 없습니다.");
        } else if (analysis.contains("[Real]")) {
            return String.format("[Real]\n딥페이크 판별 확률: %.2f%%", confidence * 100);
        } else if (analysis.contains("[Fake]")) {
            return String.format("[Fake]\n딥페이크 판별 확률: %.2f%%", confidence * 100);
        } else {
            return "분석 오류";
        }
    }

    public int getItemCount() {
        return savedResults != null ? savedResults.size() : 0;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;
        TextView imageText;
        Button deleteButton;
        LinearLayout linearLayout;

        ViewHolder(@NonNull View view) {
            super(view);
            imageView = view.findViewById(R.id.imageView);
            imageText = view.findViewById(R.id.imageText);
            deleteButton = view.findViewById(R.id.deleteButton);
            linearLayout = view.findViewById(R.id.linearLayout);
        }
    }

    private double getConfidence(String result) {
        if (result.contains("score: ")) {
            String[] part = result.split("score: ");
            if (part.length > 1) {
                String score = part[1].split("\\)")[0].trim();
                return Double.parseDouble(score);
            }
        }
        return 0.0;
    }
}