package com.documentscanner.pdfscanner365.fragment.adapters;


import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;

import java.util.List;

import com.documentscanner.pdfscanner365.db.models.Note;
import com.documentscanner.pdfscanner365.db.models.NoteGroup;
import com.documentscanner.pdfscanner365.fragment.ImageFragment;

public class NotesPagerAdapter extends FragmentPagerAdapter {

    private final List<Note> notes;
    private final NoteGroup noteGroup;

    public NotesPagerAdapter(FragmentManager fragmentManager, List<Note> notes, NoteGroup noteGroup) {
            super(fragmentManager);
            this.notes = notes;
            this.noteGroup = noteGroup;
        }

        // Returns total number of pages
        @Override
        public int getCount() {
            return notes.size();
        }

        // Returns the fragment to display for that page
        @Override
        public Fragment getItem(int position) {
            return ImageFragment.newInstance(notes.get(position), noteGroup);
        }

        // Returns the page title for the top indicator
        @Override
        public CharSequence getPageTitle(int position) {
            return "Scan " + (position+1);
        }

    }