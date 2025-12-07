package com.example.onedriveexplorer.network;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class OneDriveClient {
    private static final String BASE_URL = "https://graph.microsoft.com/";
    private static Retrofit retrofit = null;
    private static String accessToken = "";

    public static void setAccessToken(String token) {
        accessToken = token;
    }

    public static OneDriveService getService() {
        if (retrofit == null) {
            OkHttpClient client = new OkHttpClient.Builder()
                .addInterceptor(chain -> {
                    Request original = chain.request();
                    Request.Builder builder = original.newBuilder()
                            .header("Authorization", "Bearer " + accessToken)
                            .method(original.method(), original.body());
                    return chain.proceed(builder.build());
                })
                .build();

            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .client(client)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return retrofit.create(OneDriveService.class);
    }
}
