package com.example.onedriveexplorer;

import android.app.DownloadManager;
import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.PopupMenu;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.onedriveexplorer.models.DriveItem;
import com.example.onedriveexplorer.models.DriveItemResponse;
import com.example.onedriveexplorer.network.OneDriveClient;
import com.example.onedriveexplorer.network.OneDriveService;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import java.util.Stack;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity implements FileAdapter.OnItemClickListener {

    private RecyclerView recyclerView;
    private FileAdapter adapter;
    private OneDriveService service;
    private Stack<String> folderStack = new Stack<>();
    private String currentFolderId = "root";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Prompt for Access Token
        showTokenDialog();

        recyclerView = findViewById(R.id.recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new FileAdapter(this);
        recyclerView.setAdapter(adapter);

        FloatingActionButton fab = findViewById(R.id.fab_add_folder);
        fab.setOnClickListener(v -> showCreateFolderDialog());
    }

    private void showTokenDialog() {
        EditText input = new EditText(this);
        input.setHint("Paste Access Token Here");
        new AlertDialog.Builder(this)
                .setTitle("Enter OneDrive Access Token")
                .setView(input)
                .setPositiveButton("OK", (dialog, which) -> {
                    String token = input.getText().toString();
                    if (!token.isEmpty()) {
                        OneDriveClient.setAccessToken(token);
                        service = OneDriveClient.getService();
                        loadDirectory(currentFolderId);
                    }
                })
                .setCancelable(false)
                .show();
    }

    private void loadDirectory(String folderId) {
        Call<DriveItemResponse> call;
        if (folderId.equals("root")) {
            call = service.listRootChildren();
        } else {
            call = service.listChildren(folderId);
        }

        call.enqueue(new Callback<DriveItemResponse>() {
            @Override
            public void onResponse(Call<DriveItemResponse> call, Response<DriveItemResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    adapter.setItems(response.body().value);
                } else {
                    Toast.makeText(MainActivity.this, "Failed to load: " + response.code(), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<DriveItemResponse> call, Throwable t) {
                Toast.makeText(MainActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onItemClick(DriveItem item) {
        if (item.folder != null) {
            folderStack.push(currentFolderId);
            currentFolderId = item.id;
            loadDirectory(currentFolderId);
        } else {
            // It's a file, maybe show details or download prompt
            Toast.makeText(this, "File: " + item.name, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onItemLongClick(DriveItem item, View view) {
        PopupMenu popup = new PopupMenu(this, view);
        popup.getMenu().add("Download");
        popup.getMenu().add("Rename");
        popup.getMenu().add("Delete");
        popup.setOnMenuItemClickListener(menuItem -> {
            switch (menuItem.getTitle().toString()) {
                case "Download":
                    downloadFile(item);
                    return true;
                case "Rename":
                    showRenameDialog(item);
                    return true;
                case "Delete":
                    deleteItem(item);
                    return true;
            }
            return false;
        });
        popup.show();
    }

    private void downloadFile(DriveItem item) {
        if (item.folder != null) {
            Toast.makeText(this, "Cannot download folders yet", Toast.LENGTH_SHORT).show();
            return;
        }
        if (item.downloadUrl == null) {
            Toast.makeText(this, "No download URL found", Toast.LENGTH_SHORT).show();
            return;
        }

        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(item.downloadUrl));
        request.setTitle(item.name);
        request.setDescription("Downloading file...");
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
        request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, item.name);

        DownloadManager manager = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
        manager.enqueue(request);
        Toast.makeText(this, "Download started", Toast.LENGTH_SHORT).show();
    }

    private void deleteItem(DriveItem item) {
        service.deleteItem(item.id).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(MainActivity.this, "Deleted", Toast.LENGTH_SHORT).show();
                    loadDirectory(currentFolderId);
                } else {
                    Toast.makeText(MainActivity.this, "Delete failed", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                Toast.makeText(MainActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showRenameDialog(DriveItem item) {
        EditText input = new EditText(this);
        input.setText(item.name);
        new AlertDialog.Builder(this)
                .setTitle("Rename")
                .setView(input)
                .setPositiveButton("Rename", (dialog, which) -> {
                    String newName = input.getText().toString();
                    if (!newName.isEmpty()) {
                        renameItem(item, newName);
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void renameItem(DriveItem item, String newName) {
        DriveItem update = new DriveItem();
        update.name = newName;
        service.renameItem(item.id, update).enqueue(new Callback<DriveItem>() {
            @Override
            public void onResponse(Call<DriveItem> call, Response<DriveItem> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(MainActivity.this, "Renamed", Toast.LENGTH_SHORT).show();
                    loadDirectory(currentFolderId);
                } else {
                    Toast.makeText(MainActivity.this, "Rename failed", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<DriveItem> call, Throwable t) {
                Toast.makeText(MainActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showCreateFolderDialog() {
        EditText input = new EditText(this);
        input.setHint("Folder Name");
        new AlertDialog.Builder(this)
                .setTitle("New Folder")
                .setView(input)
                .setPositiveButton("Create", (dialog, which) -> {
                    String name = input.getText().toString();
                    if (!name.isEmpty()) {
                        createFolder(name);
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void createFolder(String name) {
        DriveItem folder = new DriveItem();
        folder.name = name;
        folder.folder = new DriveItem.Folder(); // Indicates it's a folder
        folder.folder.childCount = 0; // Optional, but good for structure

        service.createFolder(currentFolderId, folder).enqueue(new Callback<DriveItem>() {
            @Override
            public void onResponse(Call<DriveItem> call, Response<DriveItem> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(MainActivity.this, "Folder created", Toast.LENGTH_SHORT).show();
                    loadDirectory(currentFolderId);
                } else {
                    Toast.makeText(MainActivity.this, "Create failed: " + response.code(), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<DriveItem> call, Throwable t) {
                Toast.makeText(MainActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onBackPressed() {
        if (!folderStack.isEmpty()) {
            currentFolderId = folderStack.pop();
            loadDirectory(currentFolderId);
        } else {
            super.onBackPressed();
        }
    }
}
