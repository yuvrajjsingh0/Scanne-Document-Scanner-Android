package com.documentscanner.pdfscanner365.activity.adapters;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import timber.log.Timber;

import com.documentscanner.pdfscanner365.R;
import com.documentscanner.pdfscanner365.db.DBManager;
import com.documentscanner.pdfscanner365.db.models.Note;
import com.documentscanner.pdfscanner365.manager.ImageManager;
import com.documentscanner.pdfscanner365.utils.PhotoUtil;

/**
 * Created by droidNinja on 20/04/16.
 */
public class NoteAdapter extends RecyclerView.Adapter<NoteAdapter.NoteViewHolder>{

    private final MultiSelector multiSelector;
    private List<Note> notes;
    private Callback callback;
    private boolean isMultipleChoiceMode;
    Context context;
    private boolean isDeleted = false;

    public interface Callback {
        void onItemClick(View view, int position, Note note);
        void onItemLongClick(View view, int position);
    }

    public NoteAdapter(List<Note> notes, MultiSelector multiSelector)
    {
        this.notes = notes;
        this.multiSelector = multiSelector;
    }

    public void deleteItems() {
        ParcelableSparseBooleanArray checkItems = multiSelector.getCheckedItemsBeforeClearing();
        for(int x = notes.size() - 1; x >= 0; x--)
        {
            if(checkItems.get(x, false)) {
                DBManager.getInstance().deleteNote(notes.get(x).id);
                PhotoUtil.deletePhoto(notes.get(x).getImagePath().getPath());
                notes.remove(x);
            }
        }
        isDeleted = true;
        notifyDataSetChanged();
    }

    public void deleteItem(Note note)
    {
        for(int x = notes.size() - 1; x >= 0; x--)
        {
            if(notes.get(x).id==note.id) {
                DBManager.getInstance().deleteNote(notes.get(x).id);
                PhotoUtil.deletePhoto(notes.get(x).getImagePath().getPath());
                notes.remove(x);
            }
        }
        notifyDataSetChanged();
    }

    public ArrayList<Uri> getCheckedNotes() {
        ArrayList<Uri> checkedUriItems = new ArrayList<>();
        ParcelableSparseBooleanArray checkItems = multiSelector.getCheckedItems();
        for(int x = notes.size() - 1; x >= 0; x--)
        {
            if(checkItems.get(x, false)) {
                    checkedUriItems.add(notes.get(x).getImagePath());
            }
        }
        return checkedUriItems;
    }

    public void setNormalChoiceMode() {
        this.isMultipleChoiceMode = false;
        notifyDataSetChanged();
    }

    @NotNull
    @Override
    public NoteViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View layoutView = LayoutInflater.from(parent.getContext()).inflate(R.layout.note_layout, parent, false);
        context = parent.getContext();
        return new NoteViewHolder(layoutView);
    }

    @Override
    public void onBindViewHolder(final NoteViewHolder holder, final int position) {
        final Note note = notes.get(position);
        holder.NoteName.setText(String.format(context.getString(R.string.scan_count), String.valueOf(position + 1)));

        holder.rootView.setOnClickListener(v -> {
            if (callback != null) {
                callback.onItemClick(v, holder.getAdapterPosition(), note);
                //notifyDataSetChanged();
            }

            if (isMultipleChoiceMode) {
                check(position, holder);
            }
            //check(position, holder);
        });

        holder.rootView.setLongClickable(true);
        holder.rootView.setOnLongClickListener(v -> {
            if(callback!=null) {
                callback.onItemLongClick(v, holder.getAdapterPosition());
                isMultipleChoiceMode = true;
                Timber.d("choicemade %s", isMultipleChoiceMode);
                check(position, holder);
                //notifyDataSetChanged();
            }
            return true;
        });

        if(isDeleted){
            holder.checkBox.setVisibility(View.GONE);
            isDeleted = false;
        }

        try{
            Picasso.with(context).load(note.getImagePath()).into(holder.imageView);
            holder.itemView.setAlpha(1.0f);
        }catch (Exception e){
            e.printStackTrace();
        }
        //setImageView(holder.imageView,note);
    }

    public void check(int position, NoteViewHolder holder){
        //holder.itemView.setActivated(multiSelector.isChecked(position));

        if(isMultipleChoiceMode) {
            //holder.checkBox.setVisibility(View.VISIBLE);
            //holder.checkBox.setChecked(multiSelector.isChecked(position));
            if(multiSelector.isChecked(position)){
                holder.itemView.setAlpha(0.3f);
            }else{
                holder.itemView.setAlpha(1.0f);
            }
            Timber.d("checkbox checked %s", isMultipleChoiceMode);
        }
        else {
            holder.itemView.setAlpha(1.0f);
            //holder.checkBox.setVisibility(View.INVISIBLE);
            Timber.d("checkbox not checked %s", isMultipleChoiceMode);
        }
    }

    private void setImageView(final ImageView imageView, Note note) {
        Target loadingTarget = new Target() {

            @Override
            public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
                imageView.setImageBitmap(bitmap);
            }

            @Override
            public void onBitmapFailed(Drawable errorDrawable) {

            }

            @Override
            public void onPrepareLoad(Drawable placeHolderDrawable) {
                imageView.setImageResource(R.drawable.placeholder);
            }

        };

        ImageManager.i.loadPhoto(note.getImagePath().getPath(), 400, 600, loadingTarget);
    }

    public void setNotes(List<Note> Notes) {
        this.notes = Notes;
    }

    public void setCallback(Callback callback) {
        this.callback = callback;
    }

    @Override
    public int getItemCount() {
        return notes.size();
    }

    public static class NoteViewHolder extends RecyclerView.ViewHolder
    {
        @BindView(R.id.note_iv)
        public ImageView imageView;

        @BindView(R.id.noteName_tv)
        public TextView NoteName;

        @BindView(R.id.root_layout)
        public View rootView;

        @BindView(R.id.isSelected_cb)
        public CheckBox checkBox;

        public NoteViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
            NoteName = itemView.findViewById(R.id.noteName_tv);
            imageView = itemView.findViewById(R.id.note_iv);
            rootView = itemView.findViewById(R.id.root_layout);
            checkBox = itemView.findViewById(R.id.isSelected_cb);

            checkBox.setVisibility(View.INVISIBLE);
        }
    }
}
