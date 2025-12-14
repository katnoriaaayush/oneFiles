package com.example.onedriveexplorer.network;

import com.example.onedriveexplorer.models.TokenResponse;
import retrofit2.Call;
import retrofit2.http.GET;

public interface TokenService {
    @GET("/token")
    Call<TokenResponse> getToken();
}
