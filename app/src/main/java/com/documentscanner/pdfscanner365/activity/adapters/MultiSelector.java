package com.documentscanner.pdfscanner365.activity.adapters;

import android.os.Bundle;
import androidx.recyclerview.widget.RecyclerView;
import timber.log.Timber;

import android.view.View;
import android.widget.CheckBox;

import com.documentscanner.pdfscanner365.R;

/**
 * Helper class to keep track of the checked items.
 */
public class MultiSelector {
    private static final String CHECKED_STATES = "checked_states";
    private RecyclerView recyclerView;
    private OnItemSelectedListener listener;

    public MultiSelector(RecyclerView recyclerView, OnItemSelectedListener listener) {
        this.recyclerView = recyclerView;
        this.listener = listener;
    }

    private ParcelableSparseBooleanArray checkedItems = new ParcelableSparseBooleanArray();
    private ParcelableSparseBooleanArray checkedItemsBeforeClearing = new ParcelableSparseBooleanArray();

    public interface OnItemSelectedListener{
        void onChecked(View view, int position, boolean checked);
    }

    public void onSaveInstanceState(Bundle state) {
        if (state != null) {
            state.putParcelable(CHECKED_STATES, checkedItems);
        }
    }

    public void onRestoreInstanceState(Bundle state) {
        if (state != null) {
            checkedItems = state.getParcelable(CHECKED_STATES);
        }
    }

    public void checkView(View view, int position) {
        boolean isChecked = isChecked(position);
        view.setActivated(!isChecked);
        Timber.tag("view").d(String.valueOf(view));
        onChecked(view, position, !isChecked);
    }

    public boolean isChecked(int position) {
        if(checkedItems != null){
            return checkedItems.get(position, false);
        }else{
            return false;
        }
    }

    private void onChecked(View v, int position, boolean isChecked) {
        listener.onChecked(v, position, isChecked);
        /*if(isChecked){
            checkedItems.put(position, isChecked);
        }else{
            checkedItems.delete(position);
        }*/
        checkedItems.put(position, isChecked);
    }

    public ParcelableSparseBooleanArray getCheckedItems()
    {
        return checkedItems;
    }

    public int getCount() {
        int count = 0;
        for(int i = 0; i< checkedItems.size(); i++){
            if(checkedItems.get(i, false)){
                count = count + 1;
            }
        }
        return count;
    }

    public ParcelableSparseBooleanArray getCheckedItemsBeforeClearing() {
        return checkedItemsBeforeClearing;
    }

    public void clearAll() {
        for (int i = 0; i < checkedItems.size(); i++) {
            int position = checkedItems.keyAt(i);
            checkedItemsBeforeClearing.put(position, checkedItems.get(position));
            /*
            RecyclerView.ViewHolder viewHolder = recyclerView.findViewHolderForAdapterPosition(position);
            if (viewHolder != null) {
                viewHolder.itemView.setActivated(false);
                CheckBox checkBox = viewHolder.itemView.findViewById(R.id.isSelected_cb);
                checkBox.setChecked(false);
                checkBox.setVisibility(View.INVISIBLE);
            }*/
            onChecked(null, i, false);
        }
        //checkedItems.clear();
    }
}