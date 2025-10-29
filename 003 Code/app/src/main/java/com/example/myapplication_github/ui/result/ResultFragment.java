
package com.example.myapplication_github.ui.result;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;

import com.example.myapplication_github.R;
import com.example.myapplication_github.database.SavedResult;
import com.example.myapplication_github.network.AnalysisResponse;
import com.example.myapplication_github.ui.gallery.GalleryViewModel;

import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Set;

public class ResultFragment extends Fragment {
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_result, container, false);
        ImageView imageView = root.findViewById(R.id.imageViewResult);
        TextView resultText = root.findViewById(R.id.textViewResult);
        Button reaultbtn = root.findViewById(R.id.resultDownload);

        Bundle bundle = getArguments();
        if (bundle != null) {
            // 📌 Bundle에 들어있는 모든 키 확인
            Set<String> keys = bundle.keySet();
            Log.d("BundleDebug", "Bundle keys: " + keys.toString());

            for (String key : keys) {
                Object value = bundle.get(key);
                Log.d("BundleDebug", key + ": " + value);
            }

            String result = bundle.getString("result", "DEFAULT_RESULT");
            String message = bundle.getString("message", "DEFAULT_MESSAGE");
            String filePath = bundle.getString("filePath", "DEFAULT_FILEPATH");

            Log.d("ResultDebug", "result: " + result);
            Log.d("ResultDebug", "message: " + message);
            Log.d("ResultDebug", "filePath: " + filePath);

            // 📌 HomeFragment → 이미지 URI로 전달받은 경우
            String imageUri = bundle.getString("imageUri");
            if (imageUri != null && !imageUri.isEmpty()) {
                imageView.setImageURI(Uri.parse(imageUri));
            }

            // 📷 CameraFragment → Bitmap으로 전달받은 경우
            Bitmap bitmap = bundle.getParcelable("imageBitmap");
            if (bitmap != null) {
                imageView.setImageBitmap(bitmap);
            }

            Log.d("ResultDebug", "result: " + result);

            if (result.contains("[NoFace]")) {
                resultText.setText("얼굴을 인식할 수 없습니다.");
                resultText.setBackground(ContextCompat.getDrawable(getContext(), R.drawable.false_button));
            } else if (result.contains("[Real]")) {
                double confidence = setConfidence(result);
                String confidenceText = String.format("실제 얼굴 판별 확률: %.2f%%", confidence * 100);
                resultText.setText("[Real]: 딥페이크가 아닙니다. \n" + confidenceText);

                resultText.setBackground(ContextCompat.getDrawable(getContext(), R.drawable.true_button));
            } else if (result.contains("[Fake]")) {
                double confidence = setConfidence(result);
                String confidenceText = String.format("실제 얼굴 판별 확률: %.2f%%", confidence * 100);

                resultText.setText("[Fake]: 딥페이크입니다. \n" + confidenceText);
                resultText.setBackground(ContextCompat.getDrawable(getContext(), R.drawable.false_button));
            } else {
                resultText.setText("분석 오류");
                resultText.setBackground(ContextCompat.getDrawable(getContext(), R.drawable.false_button));
            }

            reaultbtn.setOnClickListener(v -> {
                GalleryViewModel viewModel = new ViewModelProvider(requireActivity()).get(GalleryViewModel.class);

                String userId = "currentUserId";
                long saveDate = System.currentTimeMillis();
                SavedResult savedResult = new SavedResult(userId, filePath, "", result, saveDate, "저장된 결과");

                viewModel.insert(savedResult);
                Toast.makeText(getContext(), "결과가 저장되었습니다.", Toast.LENGTH_SHORT).show();
                NavHostFragment.findNavController(this).navigate(R.id.action_resultFragment_to_homeFragment);
            });
        }

        return root;
    }

    private double setConfidence(String result) {
        try {
            if (result.contains("score: ")) {
                String[] part = result.split("score: ");
                if (part.length > 1) {
                    String scorePart = part[1].split("\\)")[0];
                    return Double.parseDouble(scorePart);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0.0;
    }
}
