package com.example.onedriveexplorer.models;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class DriveItemResponse {
    @SerializedName("value")
    public List<DriveItem> value;
}
