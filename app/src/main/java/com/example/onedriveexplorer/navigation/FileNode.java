package com.example.onedriveexplorer.navigation;

import com.example.onedriveexplorer.models.DriveItem;
import java.util.ArrayList;
import java.util.List;

public class FileNode {
    public DriveItem fileItem;
    public FileNode parentNode;
    public List<DriveItem> cachedItems;  // null = not fetched, empty = empty folder
    
    public FileNode(DriveItem fileItem, FileNode parent) {
        this.fileItem = fileItem;
        this.parentNode = parent;
        this.cachedItems = null;
    }
    
    public boolean isCached() {
        return cachedItems != null;
    }
    
    public void cache(List<DriveItem> items) {
        this.cachedItems = items != null ? new ArrayList<>(items) : new ArrayList<>();
    }
    
    public void clearCache() {
        this.cachedItems = null;
    }
}
