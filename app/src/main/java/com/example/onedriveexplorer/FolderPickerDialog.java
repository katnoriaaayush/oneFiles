package com.example.onedriveexplorer;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class FolderPickerDialog extends DialogFragment implements FolderAdapter.OnFolderClickListener {

    private File currentDir;
    private FolderAdapter adapter;
    private android.widget.LinearLayout breadcrumbContainer;
    private OnFolderSelectedListener listener;
    private File selectedFolder;

    public interface OnFolderSelectedListener {
        void onFolderSelected(File folder);
    }

    public void setListener(OnFolderSelectedListener listener) {
        this.listener = listener;
    }

    private List<File> customFiles;

    public void setCustomFiles(List<File> files) {
        this.customFiles = files;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(DialogFragment.STYLE_NORMAL, android.R.style.Theme_Material_Light_Dialog_MinWidth);
        // Start at root logic: if custom files provided, start at null (virtual root).
        // Otherwise start at ExternalStorage.
        // But we can't check customFiles here efficiently if set after fragment create but before show?
        // Usually set before show.
        // Let's resolve start dir in onCreateView.
    }

    @Override
    public void onStart() {
        super.onStart();
        Dialog dialog = getDialog();
        if (dialog != null && dialog.getWindow() != null) {
            // Convert 500dp to pixels
            int heightPixels = (int) (500 * getResources().getDisplayMetrics().density);
            dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, heightPixels);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.dialog_folder_picker, container, false);

        breadcrumbContainer = view.findViewById(R.id.breadcrumb_container);
        RecyclerView recyclerView = view.findViewById(R.id.recycler_view_folders);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        
        adapter = new FolderAdapter(getContext(), this);
        recyclerView.setAdapter(adapter);

        view.findViewById(R.id.btn_cancel).setOnClickListener(v -> dismiss());
        view.findViewById(R.id.btn_move_here).setOnClickListener(v -> {
            if (listener != null) {
                // If currentDir is null (Virtual Root) and nothing selected, maybe return null?
                // Or if user selected a folder in the list?
                File result = selectedFolder;
                if (result == null) {
                    result = currentDir;
                }
                
                // If allow returning null for "no selection"? Or just ignore?
                // For now, if result is null (Virtual Root and no selection), we do nothing or dismiss?
                if (result != null) {
                     listener.onFolderSelected(result);
                }
            }
            dismiss();
        });
        
        // Back navigation within dialog
        view.findViewById(R.id.dialog_title).setOnClickListener(v -> {
            if (currentDir != null) {
                File parent = currentDir.getParentFile();
                // If parent is null or we are at the top of real FS, do we go to Virtual Root?
                // Check if we started with custom files
                if (customFiles != null && (parent == null || !parent.getAbsolutePath().startsWith(Environment.getExternalStorageDirectory().getAbsolutePath()))) {
                    // Go to virtual root
                    loadFolders(null);
                } else if (!currentDir.equals(Environment.getExternalStorageDirectory())) {
                     loadFolders(parent);
                }
            }
        });

        // Initial Load
        if (customFiles != null && currentDir == null) {
            loadFolders(null);
        } else {
            loadFolders(Environment.getExternalStorageDirectory());
        }

        return view;
    }

    private void loadFolders(File dir) {
        currentDir = dir;
        updateBreadcrumbs(dir);
        selectedFolder = null; 
        adapter.setSelectedPosition(-1);
        
        if (dir == null) {
            // Virtual Root
            if (customFiles != null) {
                adapter.setFiles(customFiles);
            } else {
                adapter.setFiles(new ArrayList<>());
            }
            return;
        }
        
        File[] files = dir.listFiles();
        List<File> fileList = new ArrayList<>();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory() && !file.isHidden()) {
                    fileList.add(file);
                }
            }
        }
        adapter.setFiles(fileList);
    }

    private void updateBreadcrumbs(File dir) {
        breadcrumbContainer.removeAllViews();
        Context context = getContext();
        if (context == null) return;
        
        if (dir == null) {
            TextView tv = new TextView(context);
            tv.setText("Roots");
            tv.setTextSize(14f);
            tv.setPadding(8, 8, 8, 8);
            tv.setTypeface(null, android.graphics.Typeface.BOLD);
            tv.setTextColor(context.getResources().getColor(android.R.color.black, null));
            breadcrumbContainer.addView(tv);
            return;
        }

        File root = Environment.getExternalStorageDirectory();
        List<File> pathChain = new ArrayList<>();
        File temp = dir;
        
        // Build chain up to root
        while (temp != null && temp.getAbsolutePath().startsWith(root.getAbsolutePath())) {
            pathChain.add(0, temp);
            if (temp.equals(root)) break;
            temp = temp.getParentFile();
        }
        
        // If we went above root or something weird, just show current
        if (pathChain.isEmpty()) pathChain.add(dir);
        
        // Add "Roots" at the start if custom files exist
        if (customFiles != null) {
            TextView rootTv = new TextView(context);
            rootTv.setText("Roots");
            rootTv.setTextSize(14f);
            rootTv.setPadding(8, 8, 8, 8);
            rootTv.setTextColor(context.getResources().getColor(android.R.color.darker_gray, null));
            rootTv.setOnClickListener(v -> loadFolders(null));
            breadcrumbContainer.addView(rootTv);
            
            TextView arrow = new TextView(context);
            arrow.setText(">");
            arrow.setTextColor(context.getResources().getColor(android.R.color.darker_gray, null));
            arrow.setPadding(4, 8, 4, 8);
            breadcrumbContainer.addView(arrow);
        }

        for (int i = 0; i < pathChain.size(); i++) {
            File path = pathChain.get(i);
            TextView tv = new TextView(context);
            
            if (path.equals(root)) {
                tv.setText("Internal Storage");
            } else {
                tv.setText(path.getName());
            }
            
            tv.setTextSize(14f);
            tv.setPadding(8, 8, 8, 8);
            
            if (i == pathChain.size() - 1) {
                tv.setTypeface(null, android.graphics.Typeface.BOLD);
                tv.setTextColor(context.getResources().getColor(android.R.color.black, null));
            } else {
                tv.setTextColor(context.getResources().getColor(android.R.color.darker_gray, null));
                tv.setOnClickListener(v -> loadFolders(path));
            }

            breadcrumbContainer.addView(tv);

            if (i < pathChain.size() - 1) {
                TextView arrow = new TextView(context);
                arrow.setText(">");
                arrow.setTextColor(context.getResources().getColor(android.R.color.darker_gray, null));
                arrow.setPadding(4, 8, 4, 8);
                breadcrumbContainer.addView(arrow);
            }
        }
    }

    @Override
    public void onFolderClick(File folder) {
        // Single click selects it
         selectedFolder = folder;
         // Double click enters it? Or separate 'open' button?
         // For now, let's say simply clicking selects it content-wise. 
         // But if we want to navigate INTO it, we need a way.
         // Let's assume onFolderClick navigates into it for browsing?
         // But user might want to select the folder itself.
         // Standard picker behavior:
         // List shows folders. Click -> Highlight. Double Click or Arrow -> Enter.
         // Let's implement: Click selects, but treating it as "Navigation" might be unexpected if "Move Here" means "Move to selected".
         // Let's just navigate on click for simplicity since it's "Explorer".
         // And "Move Here" returns currentDir.
         // Wait, user said "navigate in the popup to a folder... and move here which returns the path of the current folder".
         // So yes, click should navigate.
         loadFolders(folder);
    }
}
