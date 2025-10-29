package com.example.myapplication_github.network;

import com.example.myapplication_github.database.SavedResult;

import java.util.List;

import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Part;
import retrofit2.http.Query;

public interface ApiService {

    //파일 업로드
    @Multipart
    @POST("/api/upload")
    Call<AnalysisResponse> uploadFile(
            @Header("Authorization") String authorization,
            @Part MultipartBody.Part image);

    //로그인
    @POST("/api/login")
    Call<LoginResponse> loginUser(
            @Body LoginRequest loginRequest
    );

    //회원가입
    @POST("/api/register")
    Call<RegisterResponse> registerUser(
            @Body RegisterRequest registerRequest
    );

    //비밀번호 변경
    @POST("/api/change_password")
    Call<ChangePasswordResponse> changePassword(
            @Header("Authorization") String authorization,
            @Body ChangePasswordRequest request
    );

    //마이페이지
    @GET("/api/mypage")
    Call<UserInfoResponse> getUserInfo(@Header("Authorization")String token);
}