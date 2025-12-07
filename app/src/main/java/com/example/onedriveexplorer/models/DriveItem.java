package com.example.onedriveexplorer.models;

import com.google.gson.annotations.SerializedName;

public class DriveItem {
    @SerializedName("id")
    public String id;

    @SerializedName("name")
    public String name;

    @SerializedName("folder")
    public Folder folder;

    @SerializedName("file")
    public File file;

    @SerializedName("@microsoft.graph.downloadUrl")
    public String downloadUrl;

    public static class Folder {
        @SerializedName("childCount")
        public int childCount;
    }

    public static class File {
        @SerializedName("mimeType")
        public String mimeType;
    }
}
