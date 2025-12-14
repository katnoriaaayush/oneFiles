package com.example.onedriveexplorer.navigation;

import com.example.onedriveexplorer.models.DriveItem;
import com.example.onedriveexplorer.models.DriveItemResponse;
import com.example.onedriveexplorer.network.OneDriveService;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import retrofit2.Call;
import retrofit2.Response;

public class NavigationManager {
    private FileNode currentNode;
    private FileNode rootNode;
    private Map<String, FileNode> nodeMap = new HashMap<>();
    private OneDriveService cloudService;
    private Callback callback;
    
    public interface Callback {
        void onSuccess(List<DriveItem> items, boolean fromCache);
        void onError(String message);
        void onLoading();
    }
    
    public NavigationManager(DriveItem rootItem, OneDriveService service) {
        this.cloudService = service;
        this.rootNode = new FileNode(rootItem, null);
        this.currentNode = rootNode;
        this.nodeMap.put(rootItem.id, rootNode);
    }
    
    /**
     * Main navigation method
     * User clicks folder -> pass DriveItem here
     */
    public void navigateTo(DriveItem fileItem) {
        // Check if node already exists
        FileNode node = nodeMap.get(fileItem.id);
        
        if (node == null) {
            // New folder - create node with current as parent
            node = new FileNode(fileItem, currentNode);
            nodeMap.put(fileItem.id, node);
        }
        
        currentNode = node;
        
        // Check cache
        if (node.isCached()) {
            if (callback != null) callback.onSuccess(node.cachedItems, true);
        } else {
            fetchFolder(node);
        }
    }
    
    /**
     * Navigate back
     * Press back button -> call this
     */
    public void goBack() {
        if (currentNode.parentNode != null) {
            currentNode = currentNode.parentNode;
            if (callback != null) {
                if (currentNode.isCached()) {
                    callback.onSuccess(currentNode.cachedItems, true);
                } else {
                    fetchFolder(currentNode);
                }
            }
        }
    }
    
    /**
     * Fetch and cache folder contents
     */
    private void fetchFolder(FileNode node) {
        if (callback != null) callback.onLoading();
        
        Call<DriveItemResponse> call;
        if ("root".equals(node.fileItem.id)) {
            call = cloudService.listRootChildren();
        } else {
            call = cloudService.listChildren(node.fileItem.id);
        }

        call.enqueue(new retrofit2.Callback<DriveItemResponse>() {
            @Override
            public void onResponse(Call<DriveItemResponse> call, Response<DriveItemResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<DriveItem> items = response.body().value;
                    node.cache(items);
                    // Create nodes for all items (optional, but good for pre-population if needed)
                    // For now, we just cache the list in the node
                    if (callback != null) callback.onSuccess(items, false);
                } else {
                    if (callback != null) callback.onError("Failed to load: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<DriveItemResponse> call, Throwable t) {
                if (callback != null) callback.onError(t.getMessage());
            }
        });
    }
    
    // ===== Getters =====
    
    public FileNode getCurrentNode() {
        return currentNode;
    }
    
    public void setCallback(Callback cb) {
        this.callback = cb;
    }
    
    /**
     * Refresh current folder
     */
    public void refresh() {
        currentNode.clearCache();
        fetchFolder(currentNode);
    }
    
    /**
     * Get current path
     */
    public String getPath() {
        StringBuilder sb = new StringBuilder();
        FileNode node = currentNode;
        while (node != null) {
            sb.insert(0, node.fileItem.name);
            if (node.parentNode != null) sb.insert(0, "/");
            node = node.parentNode;
        }
        return sb.toString();
    }
    
    /**
     * Initialize (load root)
     */
    public void init() {
        fetchFolder(rootNode);
    }
    
    public boolean canGoBack() {
        return currentNode.parentNode != null;
    }
}
