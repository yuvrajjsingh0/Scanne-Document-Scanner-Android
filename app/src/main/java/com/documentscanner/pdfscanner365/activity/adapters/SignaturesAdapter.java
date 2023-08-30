package com.documentscanner.pdfscanner365.activity.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.documentscanner.pdfscanner365.R;
import com.squareup.picasso.Picasso;

import java.io.File;
import java.util.ArrayList;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

public class SignaturesAdapter extends RecyclerView.Adapter<SignaturesAdapter.SignaturesViewHolder> {

    ArrayList<File> files;
    Context context;

    int VIEW_TYPE_ADD_NEW = 0;
    int VIEW_TYPE_NORMAL = 1;

    OnItemClickListener clickListener;

    public SignaturesAdapter(Context context, ArrayList<File> files, OnItemClickListener onItemClickListener){
        this.files = files;
        this.context = context;
        this.clickListener = onItemClickListener;
    }

    public void refresh(ArrayList<File> files){
        this.files = files;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public SignaturesViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view;
        if(viewType == VIEW_TYPE_ADD_NEW){
            view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_add_sign, parent, false);
        }else{
            view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_sign, parent, false);
        }
        return new SignaturesViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SignaturesViewHolder holder, int position) {
        holder.bind(position);
    }

    @Override
    public int getItemCount() {
        return files.size();
    }

    @Override
    public int getItemViewType(int position) {
        if(position == 0){
            return VIEW_TYPE_ADD_NEW;
        }else{
            return VIEW_TYPE_NORMAL;
        }
    }

    class SignaturesViewHolder extends RecyclerView.ViewHolder{

        ImageView signIV;
        TextView signTV;

        public SignaturesViewHolder(@NonNull View itemView) {
            super(itemView);
            signIV = itemView.findViewById(R.id.note_iv);
            signTV = itemView.findViewById(R.id.noteName_tv);
        }

        public void bind(int position){
            if(position != 0){
                signTV.setText(files.get(position).getName());
                Picasso.with(context).load(files.get(position)).into(signIV);
            }
            itemView.setOnClickListener(v -> clickListener.onClick(position));
            itemView.setOnLongClickListener(v -> clickListener.onLongClick(position));
        }
    }

    public interface OnItemClickListener{
        void onClick(int position);
        boolean onLongClick(int position);
    }
}
