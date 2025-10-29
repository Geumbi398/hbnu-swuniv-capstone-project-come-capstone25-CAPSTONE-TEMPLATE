package com.example.myapplication_github;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.myapplication_github.network.ApiService;
import com.example.myapplication_github.network.RegisterRequest;
import com.example.myapplication_github.network.RegisterResponse;
import com.example.myapplication_github.network.RetrofitClient;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RegisterActivity extends AppCompatActivity {
    private ApiService apiService;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        EditText editIdR = findViewById(R.id.editIdRegister);
        EditText editPasswordR = findViewById(R.id.editPasswordRegister);
        EditText editEmailR = findViewById(R.id.editEmailRegister);
        Button btnRegister = findViewById(R.id.btnRegister);

        apiService = RetrofitClient.getApiService();

        btnRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String id = editIdR.getText().toString().trim();
                String password = editPasswordR.getText().toString().trim();
                String email = editEmailR.getText().toString().trim();

                if (id.isEmpty()) {
                    editIdR.setError("아이디를 입력하세요.");
                    editIdR.requestFocus();
                    return;
                }

                if (password.isEmpty()) {
                    editPasswordR.setError("비밀번호를 입력하세요.");
                    editPasswordR.requestFocus();
                    return;
                }

                if (email.isEmpty()) {
                    editEmailR.setError("이메일을 입력하세요.");
                    editEmailR.requestFocus();
                    return;
                }

                btnRegister.setEnabled(false);
                btnRegister.setText("가입 중");

                RegisterRequest registerRequest = new RegisterRequest(id, password, email);

                Call<RegisterResponse> call = apiService.registerUser(registerRequest);
                call.enqueue(new Callback<RegisterResponse>() {
                    @Override
                    public void onResponse(Call<RegisterResponse> call, Response<RegisterResponse> response) {
                        btnRegister.setEnabled(true);
                        btnRegister.setText("회원가입");
                        if (response.isSuccessful() && response.body() != null) {
                            RegisterResponse registerResponse = response.body();

                            String message = registerResponse.getMessage();
                            if (message != null && message.contains("successfully")) {
                                Toast.makeText(RegisterActivity.this, "회원가입 완료", Toast.LENGTH_SHORT).show();

                                Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
                                intent.putExtra("username", id);
                                startActivity(intent);
                                finish();
                            } else if (registerResponse.isSuccess()) {
                                Toast.makeText(RegisterActivity.this, "회원가입 성공", Toast.LENGTH_SHORT).show();
                                ;

                                Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
                                intent.putExtra("username", id);
                                startActivity(intent);
                                finish();
                            } else {
                                String errorMessage = registerResponse.getMessage();
                                if (errorMessage != null && (errorMessage.contains("already exists"))) {
                                    editIdR.setError("이미 사용 중인 아이디입니다.");
                                    editIdR.requestFocus();
                                } else {
                                    Toast.makeText(RegisterActivity.this, errorMessage != null ? errorMessage : "회원가입 실패", Toast.LENGTH_SHORT).show();

                                }
                            }
                        } else {
                            if (response.code() == 400) {
                                try {
                                    String errorBody = response.errorBody().string();

                                    if (errorBody.contains("Username already exists")){
                                        editIdR.setError("이미 사용 중인 아이디 입니다.");
                                        editIdR.requestFocus();
                                    }
                                } catch (Exception e) {
                                    Toast.makeText(RegisterActivity.this, "입력을 확인해주세요.", Toast.LENGTH_SHORT).show();
                                }
                            } else {
                                Toast.makeText(RegisterActivity.this, "서버 오류", Toast.LENGTH_SHORT).show();
                                Log.e("RegisterActivity", "Register fail: " + response.code());
                            }
                        }
                    }

                    @Override
                    public void onFailure(Call<RegisterResponse> call, Throwable t) {
                        btnRegister.setEnabled(true);
                        btnRegister.setText("회원가입");
                        Toast.makeText(RegisterActivity.this, "네트워크 오류", Toast.LENGTH_SHORT).show();
                        Log.e("RegisterActivity", "Network error: " + t.getMessage());
                    }
                });
            }
        });

    }
}
