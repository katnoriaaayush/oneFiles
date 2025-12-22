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
        clearSelection(); // Clear selection when switching views
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
            boolean isSelected = selectedItemIds.contains(item.id);
            ((GridViewHolder) holder).bind(item, listener, isSelectionMode, isSelected, this::toggleSelection);
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
        ImageView selectionCheckbox; // Add checkbox

        public GridViewHolder(@NonNull View itemView) {
            super(itemView);
            iconView = itemView.findViewById(R.id.fileIconGrid);
            nameView = itemView.findViewById(R.id.fileNameGrid);
            sizeView = itemView.findViewById(R.id.fileSizeGrid);
            selectionCheckbox = itemView.findViewById(R.id.selectionCheckbox);
        }

        public void bind(final DriveItem item, final OnItemClickListener listener) {
            // Bind existing data (moved to adapter to access state)
        }
        
        // Helper to bind with adapter state
        public void bind(final DriveItem item, final OnItemClickListener listener, 
                         boolean isSelectionMode, boolean isSelected, 
                         java.util.function.Consumer<DriveItem> onToggle) {
                         
            nameView.setText(item.name);
            sizeView.setText(item.getFormattedSize());
            iconView.setImageResource(getIconResId(item));

            // Selection Logic
            if (isSelectionMode) {
                selectionCheckbox.setVisibility(View.VISIBLE);
                selectionCheckbox.setImageResource(isSelected ? R.drawable.ic_check_circle_filled : R.drawable.ic_circle_outline);
                itemView.setActivated(isSelected); 
                
                // Set Border
                if (itemView instanceof com.google.android.material.card.MaterialCardView) {
                    int strokeWidth = (int) (3 * itemView.getResources().getDisplayMetrics().density);
                    ((com.google.android.material.card.MaterialCardView) itemView).setStrokeWidth(isSelected ? strokeWidth : 0);
                }
            } else {
                selectionCheckbox.setVisibility(View.GONE);
                itemView.setActivated(false);
                if (itemView instanceof com.google.android.material.card.MaterialCardView) {
                     ((com.google.android.material.card.MaterialCardView) itemView).setStrokeWidth(0);
                }
            }

            itemView.setOnClickListener(v -> {
                if (isSelectionMode) {
                    onToggle.accept(item);
                } else {
                    listener.onItemClick(item);
                }
            });

            itemView.setOnLongClickListener(v -> {
                if (!isSelectionMode) {
                    onToggle.accept(item); // Enter selection mode and select this item
                    return true;
                }
                return false;
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

    // Selection Handling
    private final java.util.Set<String> selectedItemIds = new java.util.HashSet<>();
    private boolean isSelectionMode = false;
    private OnSelectionChangeListener selectionChangeListener;

    public interface OnSelectionChangeListener {
        void onSelectionChanged(int count);
    }

    public void setOnSelectionChangeListener(OnSelectionChangeListener listener) {
        this.selectionChangeListener = listener;
    }

    private void toggleSelection(DriveItem item) {
        if (selectedItemIds.contains(item.id)) {
            selectedItemIds.remove(item.id);
        } else {
            selectedItemIds.add(item.id);
        }
        
        if (selectedItemIds.isEmpty()) {
            isSelectionMode = false;
        } else {
            isSelectionMode = true;
        }
        
        notifyDataSetChanged();
        
        if (selectionChangeListener != null) {
            selectionChangeListener.onSelectionChanged(selectedItemIds.size());
        }
    }

    public void clearSelection() {
        selectedItemIds.clear();
        isSelectionMode = false;
        notifyDataSetChanged();
        if (selectionChangeListener != null) {
            selectionChangeListener.onSelectionChanged(0);
        }
    }
}
