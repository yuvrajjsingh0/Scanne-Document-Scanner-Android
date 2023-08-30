package com.documentscanner.pdfscanner365.utils;

import android.os.AsyncTask;

import com.documentscanner.pdfscanner365.db.models.NoteGroup;

public class DeletePhotoTask extends AsyncTask<Void, Void, Void> {

    private final NoteGroup noteGroup;

    public DeletePhotoTask(NoteGroup noteGroup) {
        this.noteGroup = noteGroup;
    }

    @Override
    protected Void doInBackground(Void... params) {
        PhotoUtil.deleteNoteGroup(noteGroup);
        return null;
    }

    @Override
    protected void onPostExecute(Void aVoid) {
        super.onPostExecute(aVoid);
    }
}
