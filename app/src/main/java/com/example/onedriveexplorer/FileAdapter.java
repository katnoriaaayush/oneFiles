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

public class FileAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private List<DriveItem> items = new ArrayList<>();
    private final OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(DriveItem item);
        void onItemLongClick(DriveItem item, View view);
    }

    public FileAdapter(OnItemClickListener listener) {
        this.listener = listener;
    }

    private boolean isGridView = false;
    private static final int VIEW_TYPE_LIST = 0;
    private static final int VIEW_TYPE_GRID = 1;

    public void setItems(List<DriveItem> items) {
        this.items = items;
        notifyDataSetChanged();
    }

    public void setGridView(boolean isGridView) {
        this.isGridView = isGridView;
        notifyDataSetChanged();
    }

    public boolean isGridView() {
        return isGridView;
    }

    @Override
    public int getItemViewType(int position) {
        return isGridView ? VIEW_TYPE_GRID : VIEW_TYPE_LIST;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == VIEW_TYPE_GRID) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_file_grid, parent, false);
            return new GridViewHolder(view);
        } else {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_file_list, parent, false);
            return new ListViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        DriveItem item = items.get(position);
        if (holder instanceof GridViewHolder) {
            ((GridViewHolder) holder).bind(item, listener);
        } else {
            ((ListViewHolder) holder).bind(item, listener);
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ListViewHolder extends RecyclerView.ViewHolder {
        ImageView iconView;
        TextView nameView;
        TextView sizeView;
        TextView dateView;
        ImageView moreOptionsView;

        public ListViewHolder(@NonNull View itemView) {
            super(itemView);
            iconView = itemView.findViewById(R.id.fileIcon);
            nameView = itemView.findViewById(R.id.fileName);
            sizeView = itemView.findViewById(R.id.fileSize);
            dateView = itemView.findViewById(R.id.fileDate);
            moreOptionsView = itemView.findViewById(R.id.fileMoreOptions);
        }

        public void bind(final DriveItem item, final OnItemClickListener listener) {
            nameView.setText(item.name);
            sizeView.setText(item.getFormattedSize());
            // dateView.setText(item.getFormattedDate()); // Need to implement date
            iconView.setImageResource(getIconResId(item));

            itemView.setOnClickListener(v -> listener.onItemClick(item));
            if (moreOptionsView != null) {
                moreOptionsView.setOnClickListener(v -> listener.onItemLongClick(item, v));
            }
        }
    }

    static class GridViewHolder extends RecyclerView.ViewHolder {
        ImageView iconView;
        TextView nameView;
        TextView sizeView;

        public GridViewHolder(@NonNull View itemView) {
            super(itemView);
            iconView = itemView.findViewById(R.id.fileIconGrid);
            nameView = itemView.findViewById(R.id.fileNameGrid);
            sizeView = itemView.findViewById(R.id.fileSizeGrid);
        }

        public void bind(final DriveItem item, final OnItemClickListener listener) {
            nameView.setText(item.name);
            sizeView.setText(item.getFormattedSize());
            iconView.setImageResource(getIconResId(item));

            itemView.setOnClickListener(v -> listener.onItemClick(item));
            itemView.setOnLongClickListener(v -> {
                listener.onItemLongClick(item, v);
                return true;
            });
        }
    }

    private static int getIconResId(DriveItem item) {
        if (item.isDirectory()) {
            return R.drawable.ic_folder_colored;
        }
        if (item.file != null && item.file.mimeType != null) {
            String mime = item.file.mimeType;
            if (mime.startsWith("image/")) {
                return R.drawable.ic_file_image;
            } else if (mime.equals("application/pdf")) {
                return R.drawable.ic_file_pdf;
            } else if (mime.contains("word") || mime.contains("document")) {
                return R.drawable.ic_file_doc;
            }
        }
        return R.drawable.ic_file_generic;
    }
}
