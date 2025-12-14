package com.example.onedriveexplorer;

import android.app.DownloadManager;
import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
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
import com.example.onedriveexplorer.navigation.NavigationManager;
import com.example.onedriveexplorer.network.OneDriveClient;
import com.example.onedriveexplorer.network.OneDriveService;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity implements FileAdapter.OnItemClickListener {

    private RecyclerView recyclerView;
    private FileAdapter adapter;
    private OneDriveService service;
    private NavigationManager nav;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Prompt for Access Token
        // Fetch Access Token
        fetchToken();

        recyclerView = findViewById(R.id.recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new FileAdapter(this);
        recyclerView.setAdapter(adapter);

        FloatingActionButton fab = findViewById(R.id.fab_add_folder);
        fab.setOnClickListener(v -> showCreateFolderDialog());
    }

    private void fetchToken() {
        String ip = "192.168.29.30";
        String baseUrl = "http://" + ip + ":8010/";
        retrofit2.Retrofit retrofit = new retrofit2.Retrofit.Builder()
                .baseUrl(baseUrl)
                .addConverterFactory(retrofit2.converter.gson.GsonConverterFactory.create())
                .build();

        com.example.onedriveexplorer.network.TokenService tokenService = retrofit.create(com.example.onedriveexplorer.network.TokenService.class);
        tokenService.getToken().enqueue(new Callback<com.example.onedriveexplorer.models.TokenResponse>() {
            @Override
            public void onResponse(Call<com.example.onedriveexplorer.models.TokenResponse> call, Response<com.example.onedriveexplorer.models.TokenResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    String token = response.body().accessToken;
                    OneDriveClient.setAccessToken(token);
                    service = OneDriveClient.getService();
                    initNavigation();
                    Toast.makeText(MainActivity.this, "Token fetched successfully", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(MainActivity.this, "Failed to fetch token: " + response.code(), Toast.LENGTH_LONG).show();
                    // Optional: Retry logic could go here
                }
            }

            @Override
            public void onFailure(Call<com.example.onedriveexplorer.models.TokenResponse> call, Throwable t) {
                Toast.makeText(MainActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_LONG).show();
                // Optional: Retry logic could go here
            }
        });
    }



    private void initNavigation() {
        DriveItem rootItem = new DriveItem();
        rootItem.id = "root";
        rootItem.name = "OneDrive";
        rootItem.folder = new DriveItem.Folder();

        nav = new NavigationManager(rootItem, service);
        androidx.swiperefreshlayout.widget.SwipeRefreshLayout swipeRefresh = findViewById(R.id.swipe_refresh_layout);
        swipeRefresh.setOnRefreshListener(() -> nav.refresh());

        nav.setCallback(new NavigationManager.Callback() {
            @Override
            public void onSuccess(List<DriveItem> items, boolean fromCache) {
                swipeRefresh.setRefreshing(false);
                findViewById(R.id.progress_bar).setVisibility(View.GONE);
                adapter.setItems(items);
                setTitle(nav.getPath());
                // Toast removed as per user request
                // String source = fromCache ? "from cache" : "fetched from network";
                // Toast.makeText(MainActivity.this, "Loaded " + source, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onError(String message) {
                swipeRefresh.setRefreshing(false);
                findViewById(R.id.progress_bar).setVisibility(View.GONE);
                Toast.makeText(MainActivity.this, message, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onLoading() {
                if (!swipeRefresh.isRefreshing()) {
                    findViewById(R.id.progress_bar).setVisibility(View.VISIBLE);
                }
            }
        });
        nav.init();
    }

    @Override
    public boolean onCreateOptionsMenu(android.view.Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_toggle_view) {
            toggleView();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void toggleView() {
        boolean isGrid = !adapter.isGridView();
        adapter.setGridView(isGrid);
        if (isGrid) {
            recyclerView.setLayoutManager(new androidx.recyclerview.widget.GridLayoutManager(this, 2));
        } else {
            recyclerView.setLayoutManager(new LinearLayoutManager(this));
        }
    }

    @Override
    public void onItemClick(DriveItem item) {
        if (item.isDirectory()) {
            nav.navigateTo(item);
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
        if (item.isDirectory()) {
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
                    nav.refresh();
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
                    nav.refresh();
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
        if (nav == null || nav.getCurrentNode() == null) return;
        
        DriveItem folder = new DriveItem();
        folder.name = name;
        folder.folder = new DriveItem.Folder(); // Indicates it's a folder
        folder.folder.childCount = 0; // Optional, but good for structure

        service.createFolder(nav.getCurrentNode().fileItem.id, folder).enqueue(new Callback<DriveItem>() {
            @Override
            public void onResponse(Call<DriveItem> call, Response<DriveItem> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(MainActivity.this, "Folder created", Toast.LENGTH_SHORT).show();
                    nav.refresh();
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
        if (nav != null && nav.canGoBack()) {
            nav.goBack();
        } else {
            super.onBackPressed();
        }
    }
}
