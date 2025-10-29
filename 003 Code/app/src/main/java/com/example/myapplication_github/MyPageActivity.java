package com.example.myapplication_github;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.myapplication_github.databinding.ActivityMyPageBinding;
import com.example.myapplication_github.network.ChangePasswordRequest;
import com.example.myapplication_github.network.ChangePasswordResponse;
import com.example.myapplication_github.network.RetrofitClient;
import com.example.myapplication_github.network.UserInfoResponse;

import org.json.JSONObject;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MyPageActivity extends AppCompatActivity {

    private ActivityMyPageBinding binding;
    private SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMyPageBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        sharedPreferences = getSharedPreferences("UserPref", MODE_PRIVATE);

        TextView tvUserName = findViewById(R.id.mp_username);
        TextView tvUserEmail = findViewById(R.id.mp_email);
        TextView tvChangePassword = findViewById(R.id.change_password);

        //토큰 만료되면 로그아웃, 로그인 화면 전환
        String token = sharedPreferences.getString("token", null);
        if (token == null || token.isEmpty()){
            Toast.makeText(this, "로그인하세요.", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(this, LoginActivity.class);
            startActivity(intent);
            finish();
            return;
        }

        if (!isTokenValid(token)){
            Toast.makeText(this, "로그인이 만료되었습니다.", Toast.LENGTH_SHORT).show();
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.remove("token");
            editor.remove("isLogin");
            editor.apply();

            Intent intent = new Intent(this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
            return;
        }

        RetrofitClient.getApiService().getUserInfo("Bearer " + token).enqueue(new Callback<UserInfoResponse>() {
            @Override
            public void onResponse(Call<UserInfoResponse> call, Response<UserInfoResponse> response) {
                if (response.isSuccessful() && response.body() != null){
                    UserInfoResponse userInfoResponse = response.body();
                    tvUserName.setText(userInfoResponse.getUsername());
                    tvUserEmail.setText(userInfoResponse.getEmail());
                } else if (response.code() == 401) {//로그인 만료
                    Toast.makeText(MyPageActivity.this, "로그인이 만료되었습니다.", Toast.LENGTH_SHORT).show();
                    SharedPreferences.Editor editor = sharedPreferences.edit();
                    editor.remove("token");
                    editor.remove("isLogin");
                    editor.apply();

                    Intent intent = new Intent(MyPageActivity.this, LoginActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                }else {
                    Toast.makeText(MyPageActivity.this, "사용자 정보를 불러오지 못했습니다.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<UserInfoResponse> call, Throwable t) {
                Toast.makeText(MyPageActivity.this, "네트워크 오류", Toast.LENGTH_SHORT).show();
            }
        });

        tvChangePassword.setOnClickListener(view -> {
            changeDialog();
        });
    }

    //비밀번호 변경
    private void changeDialog() {
        Log.d("MyPageActivity", "비밀번호 변경 다이얼로그 호출");

        View dialogView = getLayoutInflater().inflate(R.layout.dialog_change_password, null);

        EditText etCurrentPassword = dialogView.findViewById(R.id.dialog_current_password);
        EditText etNewPassword = dialogView.findViewById(R.id.dialog_new_password);
        EditText etNewPassword2 = dialogView.findViewById(R.id.dialog_new_password2);

        AlertDialog alertDialog = new AlertDialog.Builder(MyPageActivity.this)
                .setTitle("비밀번호 변경")
                .setView(dialogView)
                .setPositiveButton("변경", null)
                .setNegativeButton("취소", null)
                .create();

        alertDialog.setOnShowListener(dialogInterface -> {
            Button positiveButton = alertDialog.getButton(AlertDialog.BUTTON_POSITIVE);
            positiveButton.setOnClickListener(v -> {
                String currentPassword = etCurrentPassword.getText().toString().trim();
                String newPassword = etNewPassword.getText().toString().trim();
                String newPassword2 = etNewPassword2.getText().toString().trim();

                if (validatePassword(etCurrentPassword, etNewPassword, etNewPassword2, currentPassword, newPassword, newPassword2)) {
                    String token = sharedPreferences.getString("token", null);
                    if (token == null || token.isEmpty()) {
                        Toast.makeText(MyPageActivity.this, "로그인이 필요합니다.", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (!isTokenValid(token)){
                        Toast.makeText(MyPageActivity.this, "로그인이 만료되었습니다. 다시 로그인하세요", Toast.LENGTH_SHORT).show();

                        SharedPreferences.Editor editor = sharedPreferences.edit();
                        editor.remove("token");
                        editor.remove("isLogin");
                        editor.apply();

                        Intent intent = new Intent(MyPageActivity.this, LoginActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        finish();
                        return;
                    }

                    Log.d("MyPageActivity", "=== 비밀번호 변경 요청 시작 ===");
                    Log.d("MyPageActivity", "토큰 존재: " + (token != null));
                    Log.d("MyPageActivity", "토큰 길이: " + (token != null ? token.length() : 0));
                    Log.d("MyPageActivity", "현재 비밀번호 길이: " + currentPassword.length());
                    Log.d("MyPageActivity", "새 비밀번호 길이: " + newPassword.length());

                    ProgressDialog progressDialog = new ProgressDialog(MyPageActivity.this);
                    progressDialog.setMessage("비밀번호 변경 중");
                    progressDialog.setCancelable(false);
                    progressDialog.show();

                    ChangePasswordRequest request = new ChangePasswordRequest(currentPassword, newPassword, newPassword2);

                    String authHeader = "Bearer " + token;
                    Log.d("MyPageActivity", "Authorization 헤더: " + authHeader.substring(0, Math.min(authHeader.length(), 30)) + "...");

                    RetrofitClient.getApiService().changePassword(authHeader, request)
                            .enqueue(new Callback<ChangePasswordResponse>() {
                                @Override
                                public void onResponse(Call<ChangePasswordResponse> call, Response<ChangePasswordResponse> response) {
                                    progressDialog.dismiss();

                                    Log.d("MyPageActivity", "=== 응답 받음 ===");
                                    Log.d("MyPageActivity", "응답 코드: " + response.code());
                                    Log.d("MyPageActivity", "응답 메시지: " + response.message());
                                    Log.d("MyPageActivity", "요청 URL: " + call.request().url());
                                    Log.d("MyPageActivity", "응답 성공 여부: " + response.isSuccessful());

                                    if (response.isSuccessful() && response.body() != null) {
                                        Toast.makeText(MyPageActivity.this, "비밀번호가 변경되었습니다.", Toast.LENGTH_SHORT).show();
                                    } else if (response.code() == 401) {
                                        SharedPreferences.Editor editor = sharedPreferences.edit();
                                        editor.remove("token");
                                        editor.remove("isLogin");
                                        editor.apply();

                                        Toast.makeText(MyPageActivity.this, "로그인이 만료되었습니다. 다시 로그인하세요.", Toast.LENGTH_SHORT).show();

                                        Intent intent = new Intent(MyPageActivity.this, LoginActivity.class);
                                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                        startActivity(intent);
                                        finish();
                                    } else {
                                        Toast.makeText(MyPageActivity.this, "비밀번호 변경 실패했습니다.", Toast.LENGTH_SHORT).show();
                                    }
                                    alertDialog.dismiss();

                                }

                                @Override
                                public void onFailure(Call<ChangePasswordResponse> call, Throwable t) {
                                    progressDialog.dismiss();

                                    alertDialog.dismiss();
                                    Toast.makeText(MyPageActivity.this, "비밀번호 변경 실패했습니다.", Toast.LENGTH_SHORT).show();
                                }
                            });
                }
            });
        });
        alertDialog.show();
    }

    private boolean validatePassword(EditText current, EditText new1, EditText new2, String currentPassword, String newPassword, String newPassword2){
        if (currentPassword.isEmpty()){
            current.setError("현재 비밀번호를 입력하세요.");
            current.requestFocus();
            return false;
        }
        if (newPassword.isEmpty()){
            new1.setError("새 비밀번호를 입력하세요.");
            new1.requestFocus();
            return false;
        }
        if (!newPassword.equals(newPassword2)){
            new2.setError("새 비밀번호와 일치하지 않습니다.");
            new2.requestFocus();
            return false;
        }
        return true;
    }

    private boolean isTokenValid(String token){

        Log.d("TOKEN_DEBUG", "토큰 존재 여부: " + (token != null ? "존재함" : "null"));
        Log.d("TOKEN_DEBUG", "Fragment - 저장된 토큰: " + (token != null ? "존재함" : "null"));
        Log.d("TOKEN_DEBUG", "Fragment - 토큰 길이: " + (token != null ? token.length() : 0));

        try {
            String[] part = token.split("\\.");
            if (part.length != 3)
                return false;
            String load = new String(Base64.decode(part[1], Base64.DEFAULT));
            Log.d("MyPageActivity", "Token payload: " + load);

            JSONObject jsonObject = new JSONObject(load);
            long exp = jsonObject.getLong("exp");
            long currentTime = System.currentTimeMillis() / 1000;

            if (currentTime >= exp) {
                Log.d("TOKEN_DEBUG", "토큰 만료됨");
                return false;
            }

            return true;
        }catch (Exception e){
            Log.d("MyPageActivity", "Token error" + e.getMessage());
            return false;
        }
    }
}
