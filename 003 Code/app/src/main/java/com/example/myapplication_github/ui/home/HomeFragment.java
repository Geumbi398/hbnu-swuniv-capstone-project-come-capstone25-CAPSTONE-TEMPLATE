package com.example.myapplication_github.ui.home;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.MimeTypeMap;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.example.myapplication_github.LoginActivity;
import com.example.myapplication_github.R;
import com.example.myapplication_github.databinding.FragmentHomeBinding;
import com.example.myapplication_github.network.AnalysisResponse;
import com.example.myapplication_github.network.RetrofitClient;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class HomeFragment extends Fragment {

    private FragmentHomeBinding binding;
    private ActivityResultLauncher<Intent> galleryLauncher;
    private SharedPreferences sharedPreferences;

    public void onCreate(@NonNull Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        sharedPreferences = requireContext().getSharedPreferences("UserPref", Context.MODE_PRIVATE);

        galleryLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    Log.d("ACTIVITY_RESULT", "ActivityResultLauncher called, resultCode=" + result.getResultCode());

                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        Uri selectedImage = result.getData().getData();
                        if (selectedImage != null) {
                            Log.d("ACTIVITY_RESULT", "Selected URI: " + selectedImage.toString());
                            sendImageFileToServer(selectedImage);
                        } else {
                            Log.e("ACTIVITY_RESULT", "Selected URI is null");
                            Toast.makeText(requireContext(), "파일 선택에 실패했습니다", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Log.d("ACTIVITY_RESULT", "Result cancelled or data is null");
                    }
                }
        );
    }

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        //수정한코드
        HomeViewModel homeViewModel = new ViewModelProvider(requireActivity()).get(HomeViewModel.class);


        binding = FragmentHomeBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        final TextView textView = binding.textHome;
        homeViewModel.getText().observe(getViewLifecycleOwner(), textView::setText);

        //버튼 클릭 시 카메라 열기
        Button btnCamera = binding.btnFile1;
        btnCamera.setOnClickListener(v -> {
            NavController navController = Navigation.findNavController(v);
            navController.navigate(R.id.action_homeFragment_to_cameraFragment);
        });

        // 파일 버튼 클릭 시 갤러리 열기
        Button btnFile = binding.btnFile2;
        btnFile.setOnClickListener(v -> openGallery());

        return root;
    }

    // 📂 갤러리에서 이미지 선택
    private void openGallery() {
        Log.d("GALLERY", "권한 확인 완료, 갤러리 열기 시작");

        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");
        String[] type = {"image/*", "video/*", "audio/*"};
        intent.putExtra(Intent.EXTRA_MIME_TYPES, type);
//        startActivityForResult(intent, REQUEST_CODE);
        Log.d("GALLERY", "Intent 생성 완료, launcher 호출");
        galleryLauncher.launch(intent);
    }

    private void sendImageFileToServer(Uri imageUri) {
        Log.d("SERVER_CALL", "sendImageFileToServer 호출됨");
        Toast.makeText(requireContext(), "파일 처리 중...", Toast.LENGTH_SHORT).show();

        if (imageUri == null) {
            Toast.makeText(requireContext(), "이미지 URI가 유효하지 않습니다", Toast.LENGTH_SHORT).show();
            return;
        }

        String token = sharedPreferences.getString("token", null);

        Log.d("TOKEN_DEBUG", "토큰 존재 여부: " + (token != null ? "존재함" : "null"));
        Log.d("TOKEN_DEBUG", "Fragment - 저장된 토큰: " + (token != null ? "존재함" : "null"));
        Log.d("TOKEN_DEBUG", "Fragment - 토큰 길이: " + (token != null ? token.length() : 0));

        if (token == null || token.isEmpty()) {
            Toast.makeText(requireContext(), "로그인이 필요합니다.", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(requireContext(), LoginActivity.class);
            startActivity(intent);
            return;
        }

        File mediaFile = uriToFile(imageUri);

        if (mediaFile == null || !mediaFile.exists()) {
            Toast.makeText(requireContext(), "파일 생성에 실패했습니다", Toast.LENGTH_SHORT).show();
            return;
        }

        ContentResolver contentResolver = requireContext().getContentResolver();
        String tempMimeType = contentResolver.getType(imageUri);

        ProgressDialog progressDialog = new ProgressDialog(requireContext());
        progressDialog.setMessage("이미지 분석 중");
        progressDialog.setCancelable(false);
        progressDialog.show();

        RequestBody requestFile = RequestBody.create(MediaType.parse(tempMimeType), mediaFile);
        MultipartBody.Part body = MultipartBody.Part.createFormData("image", mediaFile.getName(), requestFile);

        Log.d("URI_DEBUG", "Uri: " + imageUri.toString());
        Log.d("URI_DEBUG", "File exists: " + mediaFile.exists());
        Log.d("URI_DEBUG", "File path: " + mediaFile.getAbsolutePath());
        Log.d("URI_DEBUG", "File length: " + mediaFile.length());

        Log.d("FILE_UPLOAD", "File name: " + mediaFile.getName());
        Log.d("FILE_UPLOAD", "File size: " + mediaFile.length());
        Log.d("FILE_UPLOAD", "MIME type: " + tempMimeType);
        Log.d("FILE_UPLOAD", "File exists: " + mediaFile.exists());

        uploadWithBearer(token, body, imageUri, tempMimeType, progressDialog);

    }

    private String getFileName(Context context, Uri uri) {
        String result = null;
        if (uri.getScheme() != null && uri.getScheme().equals("content")) {
            try (Cursor cursor = context.getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    if (nameIndex >= 0) {
                        result = cursor.getString(nameIndex);
                    }
                }
            } catch (Exception e) {
                Log.e("FILE_ERROR", "파일명 가져오기 오류: " + e.getMessage());
            }
        }

        if (result == null && uri.getPath() != null) {
            result = uri.getPath();
            int cut = result.lastIndexOf('/');
            if (cut != -1) {
                result = result.substring(cut + 1);
            }
        }

        if (result == null) {
            result = "unknown_file_" + System.currentTimeMillis() + ".jpg";
        }

        return result;
    }

    private File uriToFile(Uri uri) {
        ContentResolver contentResolver = requireContext().getContentResolver();
        String fileName = getFileName(requireContext(), uri);

        String mimeType = contentResolver.getType(uri);
        String extension = "";

        if (mimeType != null) {
            String ex = MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType);
            if (ex != null) {
                extension = "." + ex;
            }
        }


        Log.d("getUri", "파일 이름: " + fileName);
        File tempFile = new File(requireContext().getCacheDir(), fileName);

        try (InputStream inputStream = contentResolver.openInputStream(uri);
             OutputStream outputStream = new FileOutputStream(tempFile)) {
            byte[] buffer = new byte[4096];
            int len;
            while ((len = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, len);
            }
            Log.d("getUri", "파일 복사 완료: " + tempFile.getAbsolutePath());
        } catch (Exception e) {
            e.printStackTrace();
            Log.e("getUri", "파일 변환 오류: " + e.getMessage());
            return null;
        }
        return tempFile;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    private void uploadWithBearer(String token, MultipartBody.Part body, Uri imageUri, String mimeType, ProgressDialog progressDialog) {
        Log.d("UPLOAD", "Bearer token (Header)");
        Log.d("TOKEN_DEBUG", "사용할 토큰: " + (token != null ? token.substring(0, Math.min(20, token.length())) + "..." : "null"));

        String header = "Bearer " + token;
        Log.d("TOKEN_DEBUG", "Authorization 헤더: " + header.substring(0, Math.min(30, header.length())) + "...");

        RetrofitClient.getApiService().uploadFile(header, body)
                .enqueue(new Callback<AnalysisResponse>() {
                    @Override
                    public void onResponse(Call<AnalysisResponse> call, Response<AnalysisResponse> response) {
                        Log.d("API_RESPONSE", "Bearer 응답 코드: " + response.code());
                        Log.d("API_RESPONSE", "Bearer 응답 메시지: " + response.message());

                        progressDialog.dismiss();
                        if (response.isSuccessful() && response.body() != null) {
                            successfulResponse(response.body(), imageUri, mimeType, progressDialog);
                        } else {
                            Log.e("API_ERROR", "Bearer token 실패" + response.code());

                            SharedPreferences.Editor editor = sharedPreferences.edit();
                            editor.remove("token");
                            editor.remove("isLogin");
                            editor.apply();

                            Toast.makeText(requireContext(), "로그인이 만료되었습니다. 다시 로그인하세요.", Toast.LENGTH_SHORT).show();

                            Intent intent = new Intent(requireContext(), LoginActivity.class);
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            startActivity(intent);
                            requireActivity().finish();
                        }
                    }

                    @Override
                    public void onFailure(Call<AnalysisResponse> call, Throwable t) {
                        progressDialog.dismiss();
                        //uploadWithHeader(token, body, imageUri, mimeType, progressDialog);
                    }
                });
    }

    private void successfulResponse(AnalysisResponse analysisResponse, Uri imageUri, String mimeType, ProgressDialog progressDialog) {
        progressDialog.dismiss();

        String result = analysisResponse.getResult();
        String filePath = analysisResponse.getFilePath();
        String message = analysisResponse.getMessage();

        try {
            // Bundle 생성 후 결과 담기
            Bundle bundle = new Bundle();

            bundle.putString("result", result);
            bundle.putString("filePath", filePath);
            bundle.putString("message", message);
            bundle.putString("mimeType", mimeType);

            if (mimeType.startsWith("image/")) {
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(requireContext().getContentResolver(), imageUri);
                if (bitmap != null)
                {bundle.putParcelable("imageBitmap", bitmap);
                    Log.e("DEBUG_IMAGE", "Bitmap이 생성");}
                else {
                    Log.e("DEBUG_IMAGE", "Bitmap이 null입니다!");
                    bundle.putString("imageUri", imageUri.toString());}

                Log.d("BUNDLE", "Uri 전달");
            } else if (mimeType.startsWith("video/")) {
                bundle.putString("videoUri", imageUri.toString());
            }

            // 결과 화면으로 이동
            NavController navController = Navigation.findNavController(requireView());
            navController.navigate(R.id.action_homeFragment_to_resultFragment, bundle);

        } catch (Exception e) {
            Log.e("BITMAP_ERROR", "Bitmap 변환 오류: " + e.getMessage());
            Toast.makeText(requireContext(), "이미지 처리 중 오류가 발생했습니다",
                    Toast.LENGTH_SHORT).show();
        }
    }
}