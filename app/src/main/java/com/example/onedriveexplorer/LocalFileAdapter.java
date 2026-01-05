package com.example.onedriveexplorer;

import android.content.Context;
import android.text.format.Formatter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

public class LocalFileAdapter extends RecyclerView.Adapter<LocalFileAdapter.ViewHolder> {

    private List<File> files = new ArrayList<>();
    private final Context context;
    private final OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(File file);
    }

    public LocalFileAdapter(Context context, OnItemClickListener listener) {
        this.context = context;
        this.listener = listener;
    }

    public void setFiles(List<File> newFiles) {
        this.files = new ArrayList<>(newFiles);
        // Sort: Folders first, then files
        Collections.sort(this.files, (f1, f2) -> {
            if (f1.isDirectory() && !f2.isDirectory()) return -1;
            if (!f1.isDirectory() && f2.isDirectory()) return 1;
            return f1.getName().compareToIgnoreCase(f2.getName());
        });
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
       // View view = LayoutInflater.from(context).inflate(R.layout.item_local_file.layout, parent, false);
        // Note: Layout inflater might fail if suffix is part of name in previous tool, assuming I named it item_local_file.xml
        // Correcting to standard resource ID access: R.layout.item_local_file
        View view = LayoutInflater.from(context).inflate(R.layout.item_local_file, parent, false);
        return new ViewHolder(view);
    }

    private java.util.HashSet<File> selectedFiles = new java.util.HashSet<>();
    private boolean isSelectionMode = false;
    private OnItemLongClickListener longClickListener;

    public interface OnItemLongClickListener {
        void onItemLongClick(File file);
    }

    public void setOnItemLongClickListener(OnItemLongClickListener listener) {
        this.longClickListener = listener;
    }

    public void toggleSelection(File file) {
        if (selectedFiles.contains(file)) {
            selectedFiles.remove(file);
        } else {
            selectedFiles.add(file);
        }
        notifyDataSetChanged();
    }

    public void clearSelection() {
        selectedFiles.clear();
        isSelectionMode = false;
        notifyDataSetChanged();
    }
    
    public void setSelectionMode(boolean active) {
        isSelectionMode = active;
        if (!active) selectedFiles.clear();
        notifyDataSetChanged();
    }
    
    public int getSelectedCount() {
        return selectedFiles.size();
    }
    
    public boolean isSelected(File file) {
        return selectedFiles.contains(file);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        File file = files.get(position);
        holder.bind(file);
    }

    @Override
    public int getItemCount() {
        return files.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        ImageView icon;
        TextView name;
        TextView details;

        ViewHolder(View itemView) {
            super(itemView);
            icon = itemView.findViewById(R.id.file_icon);
            name = itemView.findViewById(R.id.file_name);
            details = itemView.findViewById(R.id.file_details);
        }

        void bind(File file) {
            name.setText(file.getName());
            
            if (file.isDirectory()) {
                icon.setImageResource(R.drawable.ic_launcher_foreground); 
                details.setText("Folder");
            } else {
                icon.setImageResource(R.drawable.ic_launcher_foreground);
                String size = Formatter.formatFileSize(context, file.length());
                String date = java.text.DateFormat.getDateTimeInstance().format(new Date(file.lastModified()));
                details.setText(size + " | " + date);
            }
            
            // Visual Highlight
            if (selectedFiles.contains(file)) {
                itemView.setBackgroundColor(0xFFE0E0E0); // Light Gray highlight
            } else {
                itemView.setBackgroundColor(android.graphics.Color.TRANSPARENT);
            }

            itemView.setOnClickListener(v -> listener.onItemClick(file));
            itemView.setOnLongClickListener(v -> {
                if (longClickListener != null) {
                    longClickListener.onItemLongClick(file);
                    return true;
                }
                return false;
            });
        }
    }
}
