package com.example.onedriveexplorer;

import android.content.Context;
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
import java.util.List;

public class FolderAdapter extends RecyclerView.Adapter<FolderAdapter.ViewHolder> {

    private List<File> folders = new ArrayList<>();
    private final Context context;
    private final OnFolderClickListener listener;
    private int selectedPosition = -1;

    public interface OnFolderClickListener {
        void onFolderClick(File folder);
    }

    public FolderAdapter(Context context, OnFolderClickListener listener) {
        this.context = context;
        this.listener = listener;
    }

    public void setFiles(List<File> allFiles) {
        this.folders = new ArrayList<>();
        if (allFiles != null) {
            for (File f : allFiles) {
                if (f.isDirectory()) {
                    this.folders.add(f);
                }
            }
        }
        
        Collections.sort(this.folders, (f1, f2) -> f1.getName().compareToIgnoreCase(f2.getName()));
        notifyDataSetChanged();
    }
    
    public void setSelectedPosition(int position) {
        int previous = selectedPosition;
        selectedPosition = position;
        notifyItemChanged(previous);
        notifyItemChanged(selectedPosition);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Reusing the same layout item_local_file as it works fine
        View view = LayoutInflater.from(context).inflate(R.layout.item_local_file, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        File folder = folders.get(position);
        holder.bind(folder, position == selectedPosition);
    }

    @Override
    public int getItemCount() {
        return folders.size();
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

        void bind(File folder, boolean isSelected) {
            name.setText(folder.getName());
            details.setText("Folder");
            icon.setImageResource(R.drawable.ic_launcher_foreground); 
            
            itemView.setBackgroundColor(isSelected ? 0xFFE0E0E0 : 0x00000000); // Simple highlight

            itemView.setOnClickListener(v -> {
                setSelectedPosition(getAdapterPosition());
                listener.onFolderClick(folder);
            });
        }
    }
}
