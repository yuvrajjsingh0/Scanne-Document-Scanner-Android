package com.documentscanner.pdfscanner365.fragment;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.SuppressLint;
import android.app.Dialog;
import androidx.annotation.NonNull;

import com.documentscanner.pdfscanner365.utils.TessLang;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import androidx.annotation.Nullable;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.app.UiAutomation;
import android.os.Bundle;
import android.text.InputType;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.Spinner;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.documentscanner.pdfscanner365.R;
import com.documentscanner.pdfscanner365.fragment.adapters.ItemAdapter;
import com.documentscanner.pdfscanner365.utils.BottomSheetModel;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

public class ShareDialogFragment extends BottomSheetDialogFragment {

    private ShareDialogListener shareDialogListener;
    private AutoCompleteTextView spinner;

    public interface ShareDialogListener{
        void sharePDF();
        void shareImage();
        void sharePDFOCR(TessLang lang);
    }

    private final BottomSheetBehavior.BottomSheetCallback mBottomSheetBehaviorCallback = new BottomSheetBehavior.BottomSheetCallback() {

        @Override
        public void onStateChanged(@NonNull View bottomSheet, int newState) {
            if (newState == BottomSheetBehavior.STATE_HIDDEN) {
                dismiss();
            }

        }

        @Override
        public void onSlide(@NonNull View bottomSheet, float slideOffset) {
        }
    };

    public ShareDialogFragment()
    {

    }

    public static ShareDialogFragment newInstance(ShareDialogListener pickerDialogListener) {
        ShareDialogFragment fragment = new ShareDialogFragment();
        fragment.shareDialogListener = pickerDialogListener;
        return fragment;
    }

    @SuppressLint("RestrictedApi")
    @Override
    public void setupDialog(Dialog dialog, int style) {
        super.setupDialog(dialog, style);
        View contentView = View.inflate(getContext(), R.layout.share_dialog_layout, null);
        dialog.setContentView(contentView);
 
        CoordinatorLayout.LayoutParams params = (CoordinatorLayout.LayoutParams) ((View) contentView.getParent()).getLayoutParams();
        CoordinatorLayout.Behavior behavior = params.getBehavior();
 
        if( behavior != null && behavior instanceof BottomSheetBehavior) {
            ((BottomSheetBehavior) behavior).setBottomSheetCallback(mBottomSheetBehaviorCallback);
        }

        setUpView(contentView);
    }

    private void setUpView(View contentView) {
        RecyclerView recyclerView = contentView.findViewById(R.id.recyclerView);
        spinner = contentView.findViewById(R.id.language_selector);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        recyclerView.setAdapter(new ItemAdapter(createItems(), item -> {
            if(shareDialogListener!=null) {
                if (item.title.equals(getResources().getString(R.string.share_pdf))) {
                    shareDialogListener.sharePDF();
                    dismiss();
                } else if (item.title.equals(getResources().getString(R.string.share_images))) {
                    shareDialogListener.shareImage();
                    dismiss();
                } else if (item.title.equals(getResources().getString(R.string.share_pdf_ocr))){

                    recyclerView.animate()
                            .translationX(recyclerView.getWidth())
                            .setDuration(300)
                            .setListener(new AnimatorListenerAdapter() {
                                @Override
                                public void onAnimationEnd(Animator animation) {
                                    super.onAnimationEnd(animation);
                                    recyclerView.setVisibility(View.GONE);
                                    contentView.findViewById(R.id.txt_input).setVisibility(View.VISIBLE);
                                }
                            }).start();
                    spinner.setInputType(InputType.TYPE_NULL);

                    String json = loadJSONFromAsset("langs");
                    Gson gson = new Gson();
                    Type type = new TypeToken<List<TessLang>>() {}.getType();

                    List<TessLang> langList = gson.fromJson(json, type);
                    ArrayList<String> arrayList1 = new ArrayList<String>();

                    //Set<String> ocrLangs = PreferenceManager.getDefaultSharedPreferences(getContext()).getStringSet("ocr_lang", new HashSet<>());
                    for(int i = 0; i < langList.size(); i++){
                        arrayList1.add(langList.get(i).getEnglishName());
                    }

                    ArrayAdapter<String> adp = new ArrayAdapter<String> (getContext(), R.layout.dropdown_menu_popup_item, arrayList1);

                    spinner.setAdapter(adp);

                    spinner.setOnItemClickListener((adapterView, view, i, l) -> ((Button)contentView.findViewById(R.id.done_butt)).setEnabled(true));
                    contentView.findViewById(R.id.done_butt).setOnClickListener(view -> {
                        TessLang tessLang = null;
                        for(int i = 0; i < langList.size(); i++){
                            if(spinner.getText().toString().equals(langList.get(i).getEnglishName())){
                                tessLang = langList.get(i);
                            }
                        }
                        if(tessLang != null){
                            shareDialogListener.sharePDFOCR(tessLang);
                            dismiss();
                        }
                    });
                    //contentView.findViewById(R.id.txt_input).setVisibility(View.VISIBLE);
                    //recyclerView.setVisibility(View.GONE);


                }
            }
        }));
    }

    public String loadJSONFromAsset(String file) {
        String json;
        try {
            InputStream is = getActivity().getAssets().open(file + ".json");
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();
            json = new String(buffer, StandardCharsets.UTF_8);
        } catch (IOException ex) {
            ex.printStackTrace();
            return null;
        }
        return json;
    }

    public List<BottomSheetModel> createItems() {

        ArrayList<BottomSheetModel> items = new ArrayList<>();
        items.add(new BottomSheetModel(R.drawable.pdf_blue, getResources().getString(R.string.share_pdf)));
        items.add(new BottomSheetModel(R.drawable.pdf_blue, getResources().getString(R.string.share_pdf_ocr)));
        items.add(new BottomSheetModel(R.drawable.image_blue, getResources().getString(R.string.share_images)));

        return items;
    }

}