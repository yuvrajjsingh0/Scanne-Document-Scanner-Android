package com.documentscanner.pdfscanner365.activity;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;


import org.parceler.Parcels;

import java.io.File;

import com.documentscanner.pdfscanner365.db.models.NoteGroup;
import com.documentscanner.pdfscanner365.fragment.FilterFragment;
import com.documentscanner.pdfscanner365.R;
import com.documentscanner.pdfscanner365.interfaces.PhotoSavedListener;
import com.documentscanner.pdfscanner365.interfaces.ScanListener;
import com.documentscanner.pdfscanner365.main.Const;
import com.documentscanner.pdfscanner365.utils.AppUtility;
import com.documentscanner.pdfscanner365.utils.SavingBitmapTask;

public class FilterActivity extends BaseScannerActivity implements ScanListener{

    private FilterFragment previewFragment;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getIntent().getBooleanExtra(EXTRAS.FROM_CAMERA, false)) {
            setTitle(R.string.lbl_take_another);
        }
    }

    @Override
    protected void showPhoto(Bitmap bitmap) {
        if (previewFragment == null) {
            previewFragment = FilterFragment.newInstance(bitmap);
            setFragment(previewFragment, FilterFragment.class.getSimpleName());
        } else {
            previewFragment.setBitmap(bitmap);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == EXTRAS.REQUEST_PHOTO_EDIT) {
            if (resultCode == EXTRAS.RESULT_EDITED) {
                setResult(EXTRAS.RESULT_EDITED, setIntentData());
                loadPhoto();
            }
        }
    }

    @Override
    public void onRotateLeftClicked() {

    }

    @Override
    public void onRotateRightClicked() {

    }

    @Override
    public void onBackClicked() {
        onBackPressed();
    }

    @Override
    public void onOkButtonClicked(Bitmap bitmap) {
        File outputFile = AppUtility.getOutputMediaFile(Const.FOLDERS.CROP_IMAGE_PATH, System.currentTimeMillis() + ".jpg");
        if(outputFile!=null)
            new SavingBitmapTask(getNoteGroupFromIntent(), bitmap, outputFile.getAbsolutePath(), new PhotoSavedListener() {
                @Override
                public void photoSaved(String path, String name) {
                    if(previewFragment!=null)
                        previewFragment.hideProgressBar();
                }

                @Override
                public void onNoteGroupSaved(NoteGroup noteGroup) {
//                    openNoteGroupActivity(noteGroup);
                    setResult(noteGroup);
                    finish();
                }

            }).execute();
    }

    private void openNoteGroupActivity(NoteGroup noteGroup) {
        Intent intent = new Intent(this, NoteGroupActivity.class);
        intent.putExtra(NoteGroup.class.getSimpleName(), Parcels.wrap(noteGroup));
        startActivity(intent);
        finish();
    }
}
