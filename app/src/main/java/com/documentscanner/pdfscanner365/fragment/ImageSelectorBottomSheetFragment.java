package com.documentscanner.pdfscanner365.fragment;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.documentscanner.pdfscanner365.R;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class ImageSelectorBottomSheetFragment extends BottomSheetDialogFragment {

    private OnItemClickListener listener;
    public static final String CAMERA = "cam";
    public static final String GALLERY = "gallery";

    public interface OnItemClickListener{
        void onClick(String i);
    }

    public ImageSelectorBottomSheetFragment(){

    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.image_selector_bottom_sheet, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        init(view);
    }

    private void init(View view){
        view.findViewById(R.id.txt_camera).setOnClickListener(view1 -> listener.onClick(CAMERA));
        view.findViewById(R.id.txt_gallery).setOnClickListener(view1 -> listener.onClick(GALLERY));
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if(context instanceof OnItemClickListener){
            listener = (OnItemClickListener) context;
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        listener = null;
    }
}
