package com.example.myapplication_github.network;

import android.os.Build;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class RetrofitClient {
    private static Retrofit retrofit = null;

    private static final String BASE_URL = "https://aiholmez.com";

    public static ApiService getApiService() {
        if (retrofit == null) {
            OkHttpClient client = new OkHttpClient.Builder()
                    .addInterceptor(new HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BODY))
                    .connectTimeout(60, TimeUnit.SECONDS)  // 연결 타임아웃
                    .readTimeout(60, TimeUnit.SECONDS)     // 읽기 타임아웃
                    .writeTimeout(60, TimeUnit.SECONDS)    // 쓰기 타임아웃
                    .build();
            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .client(client)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return retrofit.create(ApiService.class);
    }
}
//base URL만 넘기면 Retrofit 인스턴스가 만들어짐