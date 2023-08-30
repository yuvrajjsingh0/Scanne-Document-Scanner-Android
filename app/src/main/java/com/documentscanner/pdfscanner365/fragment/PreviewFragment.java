package com.documentscanner.pdfscanner365.fragment;

import android.app.Activity;
import android.os.Bundle;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.jetbrains.annotations.NotNull;

import butterknife.ButterKnife;
import com.documentscanner.pdfscanner365.R;
import com.documentscanner.pdfscanner365.databinding.FragmentPreviewBinding;
import com.documentscanner.pdfscanner365.db.models.NoteGroup;
import com.documentscanner.pdfscanner365.fragment.adapters.NotesPagerAdapter;

public class PreviewFragment extends BaseFragment {

    private NoteGroup noteGroup;
    private int position;
    private FragmentPreviewBinding binding;

    public static PreviewFragment newInstance(NoteGroup noteGroup, int position) {

        PreviewFragment fragment = new PreviewFragment();
        fragment.noteGroup = noteGroup;
        fragment.position = position;

        return fragment;
    }

    @Override
    public View onCreateView(@NotNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentPreviewBinding.inflate(inflater, container, false);
        ButterKnife.bind(this, binding.getRoot());
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NotNull final View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        init();
    }

    private void init() {
        NotesPagerAdapter notesPagerAdapter = new NotesPagerAdapter(getChildFragmentManager(), noteGroup.notes, noteGroup);
        binding.photoVp.setAdapter(notesPagerAdapter);
        binding.photoVp.setCurrentItem(position);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

    }

    public void setNoteGroup(NoteGroup mNoteGroup, int position) {
        this.noteGroup = mNoteGroup;
        this.position = position;
    }

    public void onBackPressed() {
        Fragment page = getChildFragmentManager().findFragmentByTag("android:switcher:" + R.id.photo_vp + ":" + binding.photoVp.getCurrentItem());
        // based on the current position you can then cast the page to the correct
        // class and call the method:
        if (page != null) {
            ((ImageFragment)page).onBackPressed();
        }
    }
}
