package com.example.onedriveexplorer;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.graphics.Color;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView; // Keep if still used, or remove
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class LocalFilesActivity extends AppCompatActivity implements LocalFileAdapter.OnItemClickListener {

    private RecyclerView recyclerView;
    private LocalFileAdapter adapter;
    private File currentDir;
    private ProgressBar progressBar;
    private TextView emptyView, searchStatusText;
    private RecursiveFileSearcher searcher;
    private Handler searchHandler = new Handler(Looper.getMainLooper());
    private Runnable searchRunnable;

    // Custom Header & Search
    private LinearLayout searchContainer;
    private EditText searchEditText;
    
    // Selection Mode
    private View selectionCard;
    private TextView selectionCountText;
    private ImageView selectionCloseBtn;
    private boolean isSelectionMode = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_local_files);

        // Custom Header Views
        LinearLayout breadcrumbContainer = findViewById(R.id.breadcrumb_container_main);
        ImageView menuBtn = findViewById(R.id.menu_button);
        menuBtn.setImageResource(android.R.drawable.ic_menu_more); 
        menuBtn.setColorFilter(Color.BLACK); 
        
        // Selection Views
        selectionCard = findViewById(R.id.selection_card);
        selectionCountText = findViewById(R.id.selection_count);
        selectionCloseBtn = findViewById(R.id.selection_close);
        
        selectionCloseBtn.setOnClickListener(v -> endSelectionMode());

        menuBtn.setOnClickListener(v -> {
            if (isSelectionMode) return; 
            
            MenuPopupHelper menuHelper = new MenuPopupHelper(this);
            menuHelper.addOption("Search", Color.BLACK, view -> {
                 if (searchStatusText.getVisibility() == View.VISIBLE) {
                     stopSearchUI();
                 } else {
                     showSearchInput();
                 }
            });
            menuHelper.addOption("Select Folder", Color.BLACK, view -> {
                FolderPickerDialog dialog = new FolderPickerDialog();
                dialog.setListener(folder -> {
                    Toast.makeText(this, "Selected: " + folder.getAbsolutePath(), Toast.LENGTH_LONG).show();
                });
                dialog.show(getSupportFragmentManager(), "FolderPicker");
            });
            menuHelper.show(menuBtn, 0, 0);
        });

        recyclerView = findViewById(R.id.recycler_view_local);
        progressBar = findViewById(R.id.progress_bar_local);
        emptyView = findViewById(R.id.empty_view_local);
        searchStatusText = findViewById(R.id.search_status_text);
        
        // Search Input
        searchContainer = findViewById(R.id.search_container);
        searchEditText = findViewById(R.id.search_edit_text);
        ImageView closeSearch = findViewById(R.id.close_search);
        
        closeSearch.setOnClickListener(v -> stopSearchUI());
        
        searchEditText.addTextChangedListener(new android.text.TextWatcher() {
             @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
             @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                 if (searchRunnable != null) searchHandler.removeCallbacks(searchRunnable);
                 searchRunnable = () -> startSearch(s.toString());
                 searchHandler.postDelayed(searchRunnable, 300);
             }
             @Override public void afterTextChanged(android.text.Editable s) {}
        });

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new LocalFileAdapter(this, this);
        
        // Add Long Click Listener
        adapter.setOnItemLongClickListener(file -> {
            if (!isSelectionMode) {
                StartSelectionMode(file);
            }
        });
        
        recyclerView.setAdapter(adapter);
        
        searcher = new RecursiveFileSearcher();
        searcher.getSearchResults().observe(this, files -> {
             if (searchStatusText.getVisibility() == View.VISIBLE) {
                 adapter.setFiles(files);
                 searchStatusText.setText("Search Algorithm: Recursive DFS | Found: " + files.size());
             }
        });
        
        searcher.getIsSearching().observe(this, isBusy -> {
            if (isBusy) {
                 progressBar.setVisibility(View.VISIBLE);
            } else {
                progressBar.setVisibility(View.GONE);
                if (adapter.getItemCount() == 0 && searchStatusText.getVisibility() == View.VISIBLE) {
                    emptyView.setVisibility(View.VISIBLE);
                    emptyView.setText("No results found.");
                }
            }
        });

        if (checkPermission()) {
            loadFiles(Environment.getExternalStorageDirectory());
        } else {
            requestPermission();
        }
    }
    
    // --- Selection Mode Logic ---
    
    @Override
    public void onItemClick(File file) {
        if (isSelectionMode) {
             toggleSelection(file);
        } else {
            if (file.isDirectory()) {
                loadFiles(file);
            } else {
                Toast.makeText(this, "File: " + file.getName(), Toast.LENGTH_SHORT).show();
            }
        }
    }
    
    private void StartSelectionMode(File file) {
        isSelectionMode = true;
        adapter.setSelectionMode(true);
        selectionCard.setVisibility(View.VISIBLE);
        toggleSelection(file);
    }
    
    private void toggleSelection(File file) {
        adapter.toggleSelection(file);
        int count = adapter.getSelectedCount();
        selectionCountText.setText(String.valueOf(count));
        
        if (count == 0) {
            endSelectionMode();
        }
    }
    
    private void endSelectionMode() {
        isSelectionMode = false;
        adapter.clearSelection();
        selectionCard.setVisibility(View.GONE);
    }
    
    // --- Search & Navigation Logic ---

    private void showSearchInput() {
        searchContainer.setVisibility(View.VISIBLE);
        searchEditText.requestFocus();
    }
    
    private void stopSearchUI() {
        searchContainer.setVisibility(View.GONE);
        searchEditText.setText("");
        searcher.stopSearch();
        searchStatusText.setVisibility(View.GONE);
        progressBar.setVisibility(View.GONE);
        loadFiles(currentDir != null ? currentDir : Environment.getExternalStorageDirectory());
    }

    private boolean checkPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            return Environment.isExternalStorageManager();
        } else {
            return ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
        }
    }

    private void requestPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                intent.addCategory("android.intent.category.DEFAULT");
                intent.setData(Uri.parse(String.format("package:%s", getApplicationContext().getPackageName())));
                startActivityForResult(intent, 2296);
            } catch (Exception e) {
                Intent intent = new Intent();
                intent.setAction(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
                startActivityForResult(intent, 2296);
            }
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 100);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (checkPermission() && currentDir == null) {
            loadFiles(Environment.getExternalStorageDirectory());
        }
    }

    private void loadFiles(File dir) {
        currentDir = dir;
        updateMainBreadcrumbs(dir); 
        
        searchStatusText.setVisibility(View.GONE);
        
        File[] files = dir.listFiles();
        List<File> fileList = new ArrayList<>();
        if (files != null) {
            for (File file : files) {
                if (!file.isHidden()) {
                    fileList.add(file);
                }
            }
        }
        
        adapter.setFiles(fileList);
        emptyView.setVisibility(fileList.isEmpty() ? View.VISIBLE : View.GONE);
    }
    
    private void updateMainBreadcrumbs(File dir) {
        LinearLayout container = findViewById(R.id.breadcrumb_container_main);
        if (container == null) return;
        container.removeAllViews();
        
        File root = Environment.getExternalStorageDirectory();
        List<File> pathChain = new ArrayList<>();
        File temp = dir;
        
        while (temp != null && temp.getAbsolutePath().startsWith(root.getAbsolutePath())) {
            pathChain.add(0, temp);
            if (temp.equals(root)) break;
            temp = temp.getParentFile();
        }
        
        if (pathChain.isEmpty()) pathChain.add(dir);

        for (int i = 0; i < pathChain.size(); i++) {
            File path = pathChain.get(i);
            TextView tv = new TextView(this);
            
            if (path.equals(root)) {
                tv.setText("Internal Storage");
            } else {
                tv.setText(path.getName());
            }
            
            tv.setTextSize(16f);
            tv.setPadding(8, 8, 8, 8);
            
            if (i == pathChain.size() - 1) {
                tv.setTypeface(null, android.graphics.Typeface.BOLD);
                tv.setTextColor(Color.BLACK);
            } else {
                tv.setTextColor(Color.DKGRAY);
                tv.setOnClickListener(v -> loadFiles(path));
            }

            container.addView(tv);

            if (i < pathChain.size() - 1) {
                TextView arrow = new TextView(this);
                arrow.setText(">");
                arrow.setTextColor(Color.GRAY);
                arrow.setPadding(4, 8, 4, 8);
                container.addView(arrow);
            }
        }
    }
    
    private void startSearch(String query) {
        if (query.isEmpty()) return;
        progressBar.setVisibility(View.VISIBLE);
        searchStatusText.setVisibility(View.VISIBLE);
        searchStatusText.setText("Searching: " + query);
        emptyView.setVisibility(View.GONE);
        searcher.search(Environment.getExternalStorageDirectory(), query);
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return false; 
    }
    
    @Override
    public boolean onSupportNavigateUp() {
        if (currentDir != null && !currentDir.equals(Environment.getExternalStorageDirectory())) {
            loadFiles(currentDir.getParentFile());
            return true;
        }
        onBackPressed();
        return true;
    }

    @Override
    public void onBackPressed() {
        if (isSelectionMode) {
            endSelectionMode();
            return;
        }
        if (searchContainer.getVisibility() == View.VISIBLE) {
            stopSearchUI();
            return;
        }
        if (currentDir != null && !currentDir.equals(Environment.getExternalStorageDirectory())) {
            loadFiles(currentDir.getParentFile());
        } else {
            super.onBackPressed();
        }
    }
}
