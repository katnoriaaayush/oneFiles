package com.example.onedriveexplorer;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;
import androidx.cardview.widget.CardView;

public class MenuPopupHelper {

    private final Context context;
    private final PopupWindow popupWindow;
    private final LinearLayout itemContainer; // Holds the text options

    // Config
    private float defaultTextSizeSp = 16f;
    private int defaultPaddingDp = 12;

    public MenuPopupHelper(Context context) {
        this.context = context;

        // 1. Create CardView (The visual container with Shadow & Corners)
        CardView cardView = new CardView(context);
        
        // crucial: Layout params for the Card inside the Popup
        cardView.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));
        
        cardView.setCardBackgroundColor(Color.WHITE);
        cardView.setRadius(dpToPx(12)); // Rounded Corners
        cardView.setCardElevation(dpToPx(8)); // Shadow Depth
        
        // This ensures space is reserved for the shadow so it doesn't get cut off
        cardView.setUseCompatPadding(true); 
        // OR manually: cardView.setContentPadding(0,0,0,0);

        // 2. Create Linear Layout to hold the items
        itemContainer = new LinearLayout(context);
        itemContainer.setOrientation(LinearLayout.VERTICAL);
        
        // Add the Linear Layout into the CardView
        cardView.addView(itemContainer);

        // 3. Setup PopupWindow
        popupWindow = new PopupWindow(context);
        popupWindow.setContentView(cardView);
        popupWindow.setFocusable(true);
        popupWindow.setWidth(ViewGroup.LayoutParams.WRAP_CONTENT);
        popupWindow.setHeight(ViewGroup.LayoutParams.WRAP_CONTENT);
        
        // IMPORTANT: Set transparent background so only the Card is visible
        popupWindow.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        
        // Remove window elevation (CardView handles it now)
        popupWindow.setElevation(0); 
    }

    // --- Helper to convert DP to PX ---
    private float dpToPx(int dp) {
        return TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, 
                dp, 
                context.getResources().getDisplayMetrics()
        );
    }

    // --- Add Options ---
    public void addOption(String text, int textColor, View.OnClickListener listener) {
        TextView textView = new TextView(context);
        textView.setText(text);
        textView.setTextColor(textColor);
        textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, defaultTextSizeSp);
        
        int padding = (int) dpToPx(defaultPaddingDp);
        textView.setPadding(padding, padding, padding, padding);

        // Standard Ripple Effect
        TypedValue outValue = new TypedValue();
        context.getTheme().resolveAttribute(android.R.attr.selectableItemBackground, outValue, true);
        textView.setBackgroundResource(outValue.resourceId);

        textView.setOnClickListener(v -> {
            if (listener != null) listener.onClick(v);
            popupWindow.dismiss();
        });

        itemContainer.addView(textView);
    }
    
    // Overload for default black text
    public void addOption(String text, View.OnClickListener listener) {
        addOption(text, Color.BLACK, listener);
    }

    // --- Show ---
    public void show(View anchorView, int xOffset, int yOffset) {
        if (!popupWindow.isShowing()) {
            // Adjust yOffset slightly if shadow makes it look too far or too close
            popupWindow.showAsDropDown(anchorView, xOffset, yOffset);
        }
    }
}
