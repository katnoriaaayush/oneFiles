package com.example.onedriveexplorer;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.onedriveexplorer.models.DriveItem;
import java.util.ArrayList;
import java.util.List;

public class FileAdapter extends RecyclerView.Adapter<FileAdapter.FileViewHolder> {

    private List<DriveItem> items = new ArrayList<>();
    private final OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(DriveItem item);
        void onItemLongClick(DriveItem item, View view);
    }

    public FileAdapter(OnItemClickListener listener) {
        this.listener = listener;
    }

    public void setItems(List<DriveItem> items) {
        this.items = items;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public FileViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_file, parent, false);
        return new FileViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FileViewHolder holder, int position) {
        DriveItem item = items.get(position);
        holder.bind(item, listener);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class FileViewHolder extends RecyclerView.ViewHolder {
        ImageView iconView;
        TextView nameView;

        public FileViewHolder(@NonNull View itemView) {
            super(itemView);
            iconView = itemView.findViewById(R.id.icon_view);
            nameView = itemView.findViewById(R.id.name_view);
        }

        public void bind(final DriveItem item, final OnItemClickListener listener) {
            nameView.setText(item.name);
            if (item.folder != null) {
                iconView.setImageResource(android.R.drawable.ic_menu_view); // Generic folder icon
            } else {
                iconView.setImageResource(android.R.drawable.ic_menu_gallery); // Generic file icon
            }

            itemView.setOnClickListener(v -> listener.onItemClick(item));
            itemView.setOnLongClickListener(v -> {
                listener.onItemLongClick(item, v);
                return true;
            });
        }
    }
}
