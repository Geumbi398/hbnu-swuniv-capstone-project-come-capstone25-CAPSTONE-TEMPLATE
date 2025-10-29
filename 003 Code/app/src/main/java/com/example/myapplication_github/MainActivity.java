package com.example.myapplication_github;


import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ContentResolver;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.provider.OpenableColumns;

import com.example.myapplication_github.network.AnalysisResponse;
import com.google.android.material.navigation.NavigationView;

import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.core.view.GravityCompat;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.appcompat.app.AppCompatActivity;

import com.example.myapplication_github.databinding.ActivityMainBinding;

// 2025_04_07
import android.net.Uri;
import android.util.Log;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;

import android.database.Cursor;
import android.provider.MediaStore;
import android.widget.Button;

import com.example.myapplication_github.network.RetrofitClient;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import com.google.gson.Gson;

public class MainActivity extends AppCompatActivity {

    private AppBarConfiguration mAppBarConfiguration;
    private ActivityMainBinding binding;
    private SharedPreferences sharedPreferences;
    private Uri imageUri;
    private boolean isFromShareIntent = false;
    private ProgressDialog progressDialog;
    private ExecutorService executorService;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        executorService = Executors.newSingleThreadExecutor();

        sharedPreferences = getSharedPreferences("UserPref", MODE_PRIVATE);

        String action = getIntent().getAction();
        String type = getIntent().getType();

        if (Intent.ACTION_SEND.equals(action) && type != null) {
            singleShare(getIntent());
        } else if (Intent.ACTION_SEND_MULTIPLE.equals(action)) {
            multipleShare(getIntent());
        }

        if (!sharedPreferences.getBoolean("isLogin", false)) {
            if (isFromShareIntent) {
                new AlertDialog.Builder(this).setTitle("로그인 필요")
                        .setMessage("파일을 분석하려면 로그인을 해주세요.")
                        .setIcon(android.R.drawable.ic_dialog_info)
                        .setPositiveButton("로그인", (dialogInterface, i) -> {
                            Intent intent = new Intent(this, LoginActivity.class);
                            intent.putExtra("from_share", true);
                            startActivity(intent);
                        }).setNegativeButton("취소", (dialogInterface, i) -> finish())
                        .setCancelable(false).show();
            } else {
                Intent intent = new Intent(MainActivity.this, LoginActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            }
            return;
        }

        //setContentView(R.layout.activity_main);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.appBarMain.toolbar);

        DrawerLayout drawer = binding.drawerLayout;
        NavigationView navigationView = binding.navView;

        View headerView = navigationView.getHeaderView(0);

        TextView textView = headerView.findViewById(R.id.textView);
        Button btnLogout = headerView.findViewById(R.id.btnLogout);
        Button btnMyPage = headerView.findViewById(R.id.btnMyPage);

        String userId = sharedPreferences.getString("username", "user");

        if (textView != null) {
            textView.setText(userId);
        }

        btnLogout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new AlertDialog.Builder(MainActivity.this)
                        .setTitle("로그아웃")
                        .setMessage("로그아웃을 하시겠습니까?")
                        .setPositiveButton("확인", (dialog, which) -> setLogout())
                        .setNegativeButton("취소", null)
                        .show();
            }
        });

        btnMyPage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, MyPageActivity.class);
                String username = sharedPreferences.getString("username", "user");
                intent.putExtra("userId", username);
                startActivity(intent);
                drawer.closeDrawer(GravityCompat.START);
            }
        });

        // Passing each menu ID as a set of IDs because each
        // menu should be considered as a top-level destination.
        mAppBarConfiguration = new AppBarConfiguration.Builder(
                R.id.nav_home, R.id.nav_gallery, R.id.nav_slideshow)
                .setOpenableLayout(drawer)
                .build();
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        NavigationUI.setupActionBarWithNavController(this, navController, mAppBarConfiguration);
        NavigationUI.setupWithNavController(navigationView, navController);


        // ✅ 툴바 제목 제어 로직 추가
        navController.addOnDestinationChangedListener((controller, destination, arguments) -> {
            if (destination.getId() == R.id.nav_home) {
                getSupportActionBar().setTitle("AI Holmes");
                binding.appBarMain.toolbar.setTitleTextColor(Color.parseColor("#275317"));
            } else {
                getSupportActionBar().setTitle(destination.getLabel());
            }
        });

        NavigationUI.setupActionBarWithNavController(this, navController, mAppBarConfiguration);
        NavigationUI.setupWithNavController(navigationView, navController);

        // ✅ Navigation Drawer "홈" 버튼 명시적으로 처리
        navigationView.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();

            if (id == R.id.nav_home) {
                navController.popBackStack(R.id.nav_home, false);
                navController.navigate(R.id.nav_home);
            } else {
                NavigationUI.onNavDestinationSelected(item, navController);
            }

            drawer.closeDrawer(GravityCompat.START);
            return true;
        });

        // 🔹 햄버거 버튼 아이콘 색 변경
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, binding.appBarMain.toolbar,
                R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        toggle.getDrawerArrowDrawable().setColor(Color.parseColor("#275317")); // 햄버거 버튼 색 변경
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        // 🔹 오른쪽 상단 메뉴 버튼 색 변경
        if (binding.appBarMain.toolbar.getOverflowIcon() != null) {
            binding.appBarMain.toolbar.getOverflowIcon().setTint(Color.parseColor("#275317"));
        }

        // 2025_04_07
        Button btnFile2 = findViewById(R.id.btn_file_2);

        if (isFromShareIntent && imageUri != null)
            processSharedFile();
    }

    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);

        String action = getIntent().getAction();
        String type = getIntent().getType();

        if (Intent.ACTION_SEND.equals(action) && type != null) {
            singleShare(intent);
        } else if (Intent.ACTION_SEND_MULTIPLE.equals(action)) {
            multipleShare(intent);
        }

        if (isFromShareIntent && imageUri != null && sharedPreferences.getBoolean("isLogin", false)) {
            processSharedFile();
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        return NavigationUI.navigateUp(navController, mAppBarConfiguration)
                || super.onSupportNavigateUp();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        try {
            if (progressDialog != null && progressDialog.isShowing()) {
                progressDialog.dismiss();
                progressDialog = null;
            }

            if (executorService != null && !executorService.isShutdown())
                executorService.shutdown();

            imageUri = null;
        } catch (Exception e) {
            Log.e("SCREENSHOT", "onDestroy 오류: " + e.getMessage());
        }
    }

    //파일 전송 함수 2025_04_07
    private void sendMediaToServer(File mediaFile, String mimeType) {
        if (mediaFile == null) {
            if (progressDialog != null && progressDialog.isShowing()) {
                progressDialog.dismiss();
            }
            Toast.makeText(this, "파일을 찾을 수 없습니다.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!mimeType.startsWith("image/")) {
            if (progressDialog != null && progressDialog.isShowing()) {
                progressDialog.dismiss();
            }
            Toast.makeText(this, "현재 이미지 파일만 분석 가능합니다.", Toast.LENGTH_SHORT).show();
            return;
        }
        String token = sharedPreferences.getString("token", null);

        Log.d("TOKEN_DEBUG", "저장된 토큰: " + (token != null ? "존재함" : "null"));
        Log.d("TOKEN_DEBUG", "토큰 길이: " + (token != null ? token.length() : 0));
        Log.d("TOKEN_DEBUG", "토큰 앞 20자: " + (token != null && token.length() > 20 ? token.substring(0, 20) : token));

        if (token == null || token.isEmpty()) {
            Toast.makeText(this, "로그인이 필요합니다.", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
            return;
        }
        RequestBody requestFile = RequestBody.create(MediaType.parse(mimeType), mediaFile);
        MultipartBody.Part body = MultipartBody.Part.createFormData("image", mediaFile.getName(), requestFile);

        uploadWithBearer(token, body, mediaFile, mimeType);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 100 && resultCode == RESULT_OK && data != null) {
            Uri selectedUri = data.getData();

            imageUri = selectedUri;

            if (selectedUri != null) {
                File mediaFile = uriToFile(this, selectedUri);

                // 🔍 MIME 타입 추출
                String mimeType = getContentResolver().getType(selectedUri);
                if (mimeType != null) {
                    sendMediaToServer(mediaFile, mimeType);  // 이미지든 영상이든 전송
                } else {
                    Toast.makeText(this, "파일 타입을 확인할 수 없습니다.", Toast.LENGTH_SHORT).show();
                }

            } else {
                Toast.makeText(this, "파일 경로를 가져올 수 없습니다.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private File uriToFile(Context context, Uri uri) {
        ContentResolver contentResolver = context.getContentResolver();
        String mimeType = contentResolver.getType(uri);

        String extension = ".jpeg";

        if (mimeType != null) {
            String ext = MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType);
            if (ext != null) {
                extension = "." + ext;
            }
        }

        try (InputStream inputStream = contentResolver.openInputStream(uri)) {
            if (inputStream == null) {
                return null;
            }

            File file = File.createTempFile("temp_", extension, context.getCacheDir());
            file.deleteOnExit();

            try (OutputStream outputStream = new FileOutputStream(file)) {
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }
            }

            return file;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    private void setLogout() {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.clear();
        editor.apply();

        Toast.makeText(MainActivity.this, "로그아웃됐습니다.", Toast.LENGTH_SHORT).show();

        Intent intent = new Intent(MainActivity.this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void uploadWithBearer(String token, MultipartBody.Part body, File mediaFile, String mimeType) {
        Log.d("UPLOAD", "Bearer token (Header)");
        RetrofitClient.getApiService().uploadFile("Bearer " + token, body)
                .enqueue(new Callback<AnalysisResponse>() {
                    @Override
                    public void onResponse(Call<AnalysisResponse> call, Response<AnalysisResponse> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            successfulResponse(response.body());
                        } else if (response.code() == 401) {
                            Log.w("UPLOAD", "Token expires");

                            SharedPreferences.Editor editor = sharedPreferences.edit();
                            editor.remove("token");
                            editor.remove("isLogin");
                            editor.apply();

                            Toast.makeText(MainActivity.this, "로그인이 만료되었습니다. 다시 로그인하세요.", Toast.LENGTH_SHORT).show();
                            Intent intent = new Intent(MainActivity.this, LoginActivity.class);
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            startActivity(intent);
                            finish();
                        } else {
                            Log.e("API_ERROR", "Bearer token 실패" + response.code());
                            //uploadWithQuery(token, body, mediaFile, mimeType);
                            Toast.makeText(MainActivity.this, getErrorMessage(response.code()), Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<AnalysisResponse> call, Throwable t) {
                        if (progressDialog != null && progressDialog.isShowing()) {
                            progressDialog.dismiss();
                        }
                        Toast.makeText(MainActivity.this, "네트워크 오류 발생", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private String getErrorMessage(int code) {
        switch (code) {
            case 400:
                return "잘못된 요청입니다.";
            case 413:
                return "파일 크기가 큽니다.";
            case 415:
                return "지원하지 않는 파일 형식입니다.";
            default:
                return "알 수 없는 오류가 발생했습니다.";
        }
    }

    private void successfulResponse(AnalysisResponse analysisResponse) {
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }

        Log.e("SERVER_RESPONSE", analysisResponse.toString());
        Log.d("API_RESPONSE", new Gson().toJson(analysisResponse));

        String result = analysisResponse.getResult();
        String filePath = analysisResponse.getFilePath();
        String message = analysisResponse.getMessage();

        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        Bundle bundle = new Bundle();
        bundle.putString("result", result);
        bundle.putString("filePath", filePath);
        bundle.putString("message", message);
        bundle.putBoolean("isAutoUpload", false);

        if (imageUri != null) {
            try {
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), imageUri);
                if (bitmap != null) {
                    Log.d("MainActivity", "Bitmap 생성 성공: " + bitmap.getWidth() + "x" + bitmap.getHeight());
                    bundle.putParcelable("imageBitmap", bitmap);
                } else {
                    Log.e("MainActivity", "Bitmap 생성 실패, URI fallback 사용");
                    bundle.putString("imageUri", imageUri.toString());
                }
            } catch (Exception e) {
                e.printStackTrace();
                bundle.putString("imageUri", imageUri.toString());
            }

            imageUri = null;
        }
        navController.navigate(R.id.action_homeFragment_to_resultFragment, bundle);
    }

    private void processSharedFile() {
        if (imageUri == null) return;

        String mimeType = getContentResolver().getType(imageUri);
        String fileName = getFileName(imageUri);

        if (mimeType == null || !mimeType.startsWith("image/")) {
            Toast.makeText(this, "지원하지 않는 파일 형식입니다.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!isSupportedExtension(imageUri)) {
            return;
        }

        if (!isValidFileSize(imageUri)) {
            return;
        }

        new AlertDialog.Builder(this).setTitle("AIHolmes")
                .setMessage("파일을 분석하시겠습니까?\n\n파일: " + fileName)
                .setIcon(android.R.drawable.ic_menu_search)
                .setPositiveButton("분석", (dialogInterface, i) -> {
                    progressDialog = new ProgressDialog(this);
                    progressDialog.setTitle("분석 중");
                    progressDialog.setMessage("파일을 분석하고 있습니다.");
                    progressDialog.setIndeterminate(true);
                    progressDialog.setCancelable(false);
                    progressDialog.show();

                    File mediaFile = uriToFile(this, imageUri);
                    if (mediaFile != null && mimeType != null) {
                        sendMediaToServer(mediaFile, mimeType);
                    } else {
                        if (progressDialog != null && progressDialog.isShowing()) {
                            progressDialog.dismiss();
                        }
                        Toast.makeText(this, "파일을 처리할 수 없습니다.", Toast.LENGTH_SHORT).show();
                    }
                }).setNegativeButton("취소", (dialogInterface, i) -> {
                }).show();
    }

    private boolean isSupportedExtension(Uri uri) {
        String fileName = getFileName(uri);
        if (fileName == null) return true;

        String[] extension = {".jpg", ".jpeg", ".png"};

        String lowerFileName = fileName.toLowerCase();
        for (String ext : extension) {
            if (lowerFileName.endsWith(ext)) {
                return true;
            }
        }

        Toast.makeText(this, "지원하지 않는 파일 형식입니다.", Toast.LENGTH_SHORT).show();
        return false;
    }

    private String getFileName(Uri uri) {
        if (uri == null)
            return null;

        String fileName = null;
        Cursor cursor = null;

        if ("content".equals(uri.getScheme())) {
            try {
                cursor = getContentResolver().query(uri, null, null, null, null);
                if (cursor != null && cursor.moveToFirst()) {
                    int idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    if (idx >= 0) {
                        fileName = cursor.getString(idx);
                    }
                }
            } catch (Exception e) {
                Log.e("MainActivity", "getFileName 오류: " + e.getMessage());
            } finally {
                if (cursor != null && !cursor.isClosed()) {
                    cursor.close();
                }
            }
        }

        if (fileName == null) {
            fileName = imageUri.getPath();
            if (fileName != null) {
                int c = fileName.lastIndexOf("/");
                if (c != -1) {
                    fileName = fileName.substring(c + 1);
                }
            }
        }

        return fileName;
    }

    private boolean isValidFileSize(Uri uri) {
        try {
            InputStream inputStream = getContentResolver().openInputStream(uri);
            if (inputStream != null) {
                int fileSize = inputStream.available();
                inputStream.close();

                long fileSizeMB = fileSize / (1024 * 1024);

                if (fileSizeMB > 100) {
                    Toast.makeText(this, "파일 크기가 100MB를 초과합니다.", Toast.LENGTH_SHORT).show();
                    return false;
                }
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private void singleShare(Intent intent) {
        String type = intent.getType();

        if (type != null) {
            if (type.startsWith("image/")) {
                Uri shareUri = intent.getParcelableExtra(Intent.EXTRA_STREAM);
                if (shareUri != null) {
                    imageUri = shareUri;
                    isFromShareIntent = true;
                }
            } else if (type.startsWith("text/") || type.startsWith("*/*")) {
                Uri shareUri = intent.getParcelableExtra(Intent.EXTRA_STREAM);
                if (shareUri != null) {
                    String mimetype = getContentResolver().getType(shareUri);
                    if (mimetype != null && mimetype.startsWith("image/")) {
                        imageUri = shareUri;
                        isFromShareIntent = true;
                        Log.d("SHARE_INTENT", "이미지 추출: " + shareUri.toString());
                    } else {
                        Log.d("SHARE_INTENT", "이미지가 포함되지 않은 컨텐츠");
                        Toast.makeText(this, "이미지가 포함된 게시물만 분석 가능합니다.", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    String string = intent.getStringExtra(Intent.EXTRA_TEXT);
                    if (string != null) {
                        Log.d("SHARE_INTENT", "텍스트만 공유됨: " + string);
                        Toast.makeText(this, "이미지가 포함된 게시물만 분석 가능합니다.", Toast.LENGTH_SHORT).show();
                    }
                }
            } else {
                Toast.makeText(this, "이미지 파일만 분석 가능합니다.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void multipleShare(Intent intent) {
        ArrayList<Uri> uris = intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM);
        if (uris != null && !uris.isEmpty()) {
            for (Uri uri : uris) {
                String mimeType = getContentResolver().getType(uri);
                if (mimeType != null && mimeType.startsWith("image/")) {
                    imageUri = uri;
                    isFromShareIntent = true;
                    Log.d("SHARE_INTENT", "첫번째 이미지 선택: " + uri.toString());
                    Log.d("SHARE_INTENT", "총 " + uris.size() + "개 파일 중 이미지 선택됨");
                    return;
                }
            }
            Toast.makeText(this, "이미지가 포함된 게시물만 분석 가능합니다.", Toast.LENGTH_SHORT).show();
            Log.d("SHARE_INTENT", "이미지 없음");
        } else {
            Toast.makeText(this, "공유된 컨텐츠를 찾을 수 없습니다.", Toast.LENGTH_SHORT).show();
        }
    }
}