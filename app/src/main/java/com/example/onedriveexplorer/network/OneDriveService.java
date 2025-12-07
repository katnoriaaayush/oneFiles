package com.example.onedriveexplorer.network;

import com.example.onedriveexplorer.models.DriveItem;
import com.example.onedriveexplorer.models.DriveItemResponse;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.PATCH;
import retrofit2.http.Path;

public interface OneDriveService {
    @GET("/v1.0/me/drive/items/{itemId}/children")
    Call<DriveItemResponse> listChildren(@Path("itemId") String itemId);

    @GET("/v1.0/me/drive/root/children")
    Call<DriveItemResponse> listRootChildren();

    @DELETE("/v1.0/me/drive/items/{itemId}")
    Call<Void> deleteItem(@Path("itemId") String itemId);

    @POST("/v1.0/me/drive/items/{itemId}/children")
    Call<DriveItem> createFolder(@Path("itemId") String itemId, @Body DriveItem folder);

    @PATCH("/v1.0/me/drive/items/{itemId}")
    Call<DriveItem> renameItem(@Path("itemId") String itemId, @Body DriveItem item);
    
    // For download, we usually get a URL from the item metadata, but sometimes we might want to download content directly.
    // However, the requirement says "File Download", usually implies getting the content.
    // The @microsoft.graph.downloadUrl property in DriveItem is the best way.
    // We can use a simple GET to that URL or use DownloadManager.
}
