package com.example.myapplication_github;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.myapplication_github.network.ApiService;
import com.example.myapplication_github.network.LoginRequest;
import com.example.myapplication_github.network.LoginResponse;
import com.example.myapplication_github.network.RetrofitClient;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        EditText editId = findViewById(R.id.editId);
        EditText editPassword = findViewById(R.id.editPassword);
        Button btnLogin = findViewById(R.id.btnLogin);
        TextView textRegister = findViewById(R.id.txtRegister);

        ApiService apiService = RetrofitClient.getApiService();
        SharedPreferences sharedPreferences = getSharedPreferences("UserPref", MODE_PRIVATE);

        if (checkLogin()) {
            Intent intent = new Intent(LoginActivity.this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        }

        btnLogin.setOnClickListener(v -> {
            String username = editId.getText().toString().trim();
            String password = editPassword.getText().toString().trim();

            if (TextUtils.isEmpty(username)){
                editId.setError("아이디를 입력하세요.");
                editId.requestFocus();
                return;
            }

            if (TextUtils.isEmpty(password)){
                editPassword.setError("비밀번호를 입력하세요.");
                editPassword.requestFocus();
                return;
            }

            btnLogin.setEnabled(false);
            btnLogin.setText("로그인 중");

            LoginRequest loginRequest = new LoginRequest(username, password);

            Call<LoginResponse> call = apiService.loginUser(loginRequest);
            call.enqueue(new Callback<LoginResponse>() {
                @Override
                public void onResponse(Call<LoginResponse> call, Response<LoginResponse> response) {
                    btnLogin.setEnabled(true);
                    btnLogin.setText("로그인");

                    if (response.isSuccessful() && response.body() != null){
                        LoginResponse loginResponse = response.body();

                        String token = loginResponse.getToken();
                        if (token != null && !token.isEmpty()){
                            SharedPreferences.Editor editor = sharedPreferences.edit();
                            editor.putString("token", token);
                            editor.putString("username", username);
                            editor.putBoolean("isLogin", true);
                            editor.apply();
                            Toast.makeText(LoginActivity.this, "로그인 성공", Toast.LENGTH_SHORT).show();
                            Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            startActivity(intent);
                            finish();

                            String savedToken = sharedPreferences.getString("token", null);
                            Log.d("TOKEN_SAVE", "저장된 토큰" + savedToken);
                        } else {
                            String errorMessage = loginResponse.getMessage();
                            if (errorMessage == null || errorMessage.isEmpty())
                                errorMessage = "아이디 또는 비밀번호가 올바르지 않습니다.";
                            Toast.makeText(LoginActivity.this, loginResponse.getMessage() != null ? loginResponse.getMessage() : "로그인에 실패했습니다.", Toast.LENGTH_SHORT).show();
                        }
                    }else {
                        String errorMessage;
                        if (response.code() == 401){
                            errorMessage = "아이디 또는 비밀번호가 올바르지 않습니다.";
                        } else if (response.code() == 500) {
                            errorMessage = "서버 내부 오류가 발생했습니다.";
                        }else {
                            errorMessage = "로그인에 실패헸습니다.";
                        }
                        Toast.makeText(LoginActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
                        Log.e("LoginActivity", "Login fail" + response.code());
                    }
                }

                @Override
                public void onFailure(Call<LoginResponse> call, Throwable t) {

                }
            });
        });

        textRegister.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
            startActivity(intent);
        });
    }

    private boolean checkLogin(){
        SharedPreferences sharedPreferences = getSharedPreferences("UserPref", MODE_PRIVATE);
        return sharedPreferences.getBoolean("isLogin", false);
    }
}
