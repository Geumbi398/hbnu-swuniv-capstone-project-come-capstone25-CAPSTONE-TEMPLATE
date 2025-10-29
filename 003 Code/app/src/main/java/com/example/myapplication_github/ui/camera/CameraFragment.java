package com.example.myapplication_github.ui.camera;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.example.myapplication_github.LoginActivity;
import com.example.myapplication_github.R;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.example.myapplication_github.databinding.FragmentCameraBinding;


import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import com.example.myapplication_github.network.AnalysisResponse;
import com.example.myapplication_github.network.ApiService;
import com.example.myapplication_github.network.RetrofitClient;
import com.example.myapplication_github.ui.result.ResultFragment;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import org.json.JSONException;
import org.json.JSONObject;


public class CameraFragment extends Fragment {
    private Uri photoUri;
    private static final int CAMERA_REQUEST_CODE = 11;

    private ImageCapture imageCapture;
    private ProcessCameraProvider cameraProvider;
    private FragmentCameraBinding binding;

    private SharedPreferences sharedPreferences;


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentCameraBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        sharedPreferences = requireContext().getSharedPreferences("UserPref", Context.MODE_PRIVATE);

        //버튼 클릭 시 사진 촬영
        binding.btnCapture.setOnClickListener(v -> capture());

        //카메라 권한 요청
        if (ContextCompat.checkSelfPermission(requireContext(), android.Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(requireActivity(),
                    new String[]{Manifest.permission.CAMERA}, CAMERA_REQUEST_CODE);
        } else {
            startCamera();
        }
    }

    //카메라 설정
    private void startCamera() {
        //후면 카메라 사용
        CameraSelector cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                .build();
        Preview preview = new Preview.Builder().build();

        imageCapture = new ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .setTargetRotation(Surface.ROTATION_0)
                .build();

        ProcessCameraProvider.getInstance(requireContext()).addListener(() -> {
            try {
                cameraProvider = ProcessCameraProvider.getInstance(requireContext()).get();

                // 카메라 객체
                Camera camera = cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture);

                // 카메라 previewView 표시
                preview.setSurfaceProvider(binding.previewView.createSurfaceProvider(camera.getCameraInfo()));

            } catch (ExecutionException | InterruptedException e) {
                e.printStackTrace();
            }
        }, ContextCompat.getMainExecutor(requireContext())); //Executor 반환
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startCamera();
            } else {
                requireActivity().finish();
            }
        }
    }

    //사진 촬영
    private void capture() {
        File photoFile = null;

        //이미지 저장 파일 생성
        try {
            photoFile = createImageFile();
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        if (photoFile != null) {
            final File capturedPhotoFile = photoFile;  // final로 할당
            photoUri = FileProvider.getUriForFile(requireContext(), "com.example.myapplication_github.fileprovider", photoFile);
            int rotation = requireActivity().getWindowManager().getDefaultDisplay().getRotation();

            ImageCapture.OutputFileOptions options = new ImageCapture.OutputFileOptions.Builder(photoFile).build();
            imageCapture.setTargetRotation(rotation);
            //사진 촬영
            imageCapture.takePicture(options, ContextCompat.getMainExecutor(requireContext()), new ImageCapture.OnImageSavedCallback() {
                @Override
                public void onImageSaved(@NonNull ImageCapture.OutputFileResults outputFileResults) {
                    if (cameraProvider != null) {
                        cameraProvider.unbindAll();
                    }


                    // 촬영된 이미지 → 서버 전송 (이 안에서 navigate 처리)
                    sendImageToServer(capturedPhotoFile);  // ← 여기에 전송 함수 호출
                }

                @Override
                public void onError(@NonNull ImageCaptureException exception) {
                    exception.printStackTrace();
                }
            });
        }

    }

    //이미지 파일 생성
    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format((new Date()));
        String imageFileName = "AIHolmes_" + timeStamp + "_";
        File storageDir = requireContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES);

        if (!storageDir.exists()) {
            storageDir.mkdirs();
        }

        //jpg파일 생성
        File image = File.createTempFile(imageFileName, ".jpg", storageDir);
        return image;
    }

    // 2025_04_07
    private void sendImageToServer(File imageFile) {


        String token = sharedPreferences.getString("token", null);
        if (token == null || token.isEmpty()){
            Toast.makeText(requireContext(), "로그인이 필요합니다", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(requireContext(), LoginActivity.class);
            startActivity(intent);
            return;
        }

        ProgressDialog progressDialog = new ProgressDialog(requireContext());
        progressDialog.setMessage("이미지 분석 중");
        progressDialog.setCancelable(false);
        progressDialog.show();

        RequestBody requestFile = RequestBody.create(MediaType.parse("image/*"), imageFile);
        MultipartBody.Part body = MultipartBody.Part.createFormData("image", imageFile.getName(), requestFile);

        //POST 이미지 업로드
        RetrofitClient.getApiService().uploadFile("Bearer " + token, body).enqueue(new Callback<AnalysisResponse>() {
            @Override
            public void onResponse(Call<AnalysisResponse> call, Response<AnalysisResponse> response) {
                progressDialog.dismiss();
                if (response.isSuccessful()&&response.body() != null) {
                    try {
                        AnalysisResponse analysisResponse = response.body();

                        String result = analysisResponse.getResult();
                        String filePath = analysisResponse.getFilePath();
                        String message = analysisResponse.getMessage();

                        Log.d("API_DATA", "Result: " + result);
                        Log.d("API_DATA", "FilePath: " + filePath);
                        Log.d("API_DATA", "Message: " + message);

                        // Bitmap 생성
                        Bitmap bitmap = MediaStore.Images.Media.getBitmap(requireContext().getContentResolver(), Uri.fromFile(imageFile));

                        // Bundle 생성 후 결과 담기
                        Bundle bundle = new Bundle();
                        bundle.putString("result", result);
                        bundle.putString("filePath", filePath);
                        bundle.putString("message", message);
                        bundle.putParcelable("imageBitmap", bitmap);

                        ResultFragment resultFragment = new ResultFragment();
                        resultFragment.setArguments(bundle);

                        // 서버 응답 처리
                        Navigation.findNavController(requireView()).navigate(R.id.action_cameraFragment_to_resultFragment, bundle);

                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } else {
                    Toast.makeText(requireContext(), "서버 오류", Toast.LENGTH_SHORT).show();
                }

                //오류 확인
                if (response.isSuccessful()) {
                    Log.d("API_SUCCESS", "Response: " + new Gson().toJson(response.body()));
                } else {
                    Log.e("API_ERROR", "Response code: " + response.code());
                    progressDialog.dismiss();
                    try {
                        Log.e("API_ERROR_BODY", response.errorBody().string());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }

            @Override
            public void onFailure(Call<AnalysisResponse> call, Throwable t) {
                progressDialog.dismiss();
                t.printStackTrace();
                Log.e("API_ERROR", "통신 실패: " + t.getMessage(), t);
                Toast.makeText(requireContext(), "통신 실패", Toast.LENGTH_SHORT).show();
            }
        });
    }
}