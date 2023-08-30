/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2014 Zillow
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is furnished
 * to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.documentscanner.pdfscanner365.activity;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import timber.log.Timber;

import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;

import org.parceler.Parcels;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import com.documentscanner.pdfscanner365.BuildConfig;
import com.documentscanner.pdfscanner365.R;
import com.documentscanner.pdfscanner365.db.DBManager;
import com.documentscanner.pdfscanner365.db.PDFScannerDatabase;
import com.documentscanner.pdfscanner365.db.models.NoteGroup;

public abstract class BaseActivity extends AppCompatActivity {

    public static final int REQUEST_ID_MULTIPLE_PERMISSIONS = 101;

    private static final int SELECT_PHOTO = 0x201;
    private NoteGroup noteGroup;
    public static final int REQUEST_IMAGE_CAPTURE = 156;

    private File capturedImage;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    protected void hideKeyboard() {
        InputMethodManager inputManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        View view = this.getCurrentFocus();
        if (view != null) {
            if (inputManager != null) {
                inputManager.hideSoftInputFromWindow(view.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
            }
        }
    }


    protected void showActionBar() {
        if (getActionBar() != null) {
            getActionBar().show();
        }
    }

    protected void hideActionBar() {
        if (getActionBar() != null) {
            getActionBar().hide();
        }
    }

    public void showBack() {
//        getActionBar().setDisplayShowHomeEnabled(false);
//        getActionBar().setDisplayHomeAsUpEnabled(true);
    }

    public void hideBack() {
        getActionBar().setDisplayShowHomeEnabled(true);
        getActionBar().setDisplayHomeAsUpEnabled(false);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        switch (menuItem.getItemId()) {
            case android.R.id.home:
                super.onBackPressed();
                return true;
        }
        return super.onOptionsItemSelected(menuItem);
    }

    protected void selectImageFromGallery(NoteGroup noteGroup)
    {
        this.noteGroup = noteGroup;
        Intent photoPickerIntent = new Intent(
                Intent.ACTION_PICK,
                android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(photoPickerIntent, SELECT_PHOTO);
    }

    protected void openCamera(NoteGroup noteGroup){
        // camera
        this.noteGroup = noteGroup;
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, FileProvider.getUriForFile(this,
                BuildConfig.APPLICATION_ID + ".provider",
                createImageFile()));
        try {
            startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
        } catch (ActivityNotFoundException e) {
            // display error state to the user
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch(requestCode) {
            case SELECT_PHOTO:
                if(resultCode == RESULT_OK){
                    Uri selectedImage = data.getData();

                    /*
                    String[] filePathColumn = { MediaStore.Images.Media.DATA };

                    Cursor cursor = getContentResolver().query(selectedImage,
                            filePathColumn, null, null, null);
                    cursor.moveToFirst();

                    int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
                    String picturePath = cursor.getString(columnIndex);

                    Timber.d(picturePath);

                    File file = new File(picturePath);

                    cursor.close();
                    openScannerActivity(picturePath, file.getName(), noteGroup);*/
                    OutputStream fos;
                    try {
                        Bitmap bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), selectedImage);
                        String imagesDir = getCacheDir().getPath() + File.separator + "/temporary_image/";
                        File file = new File(imagesDir);

                        if (!file.exists()) {
                            file.mkdir();
                        }

                        File image = new File(imagesDir, "tmp" + Calendar.getInstance().getTimeInMillis() + ".jpg");
                        fos = new FileOutputStream(image);
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos);

                        fos.flush();
                        fos.close();
                        Timber.d("mSelected %s", image.getPath());
                        openScannerActivity(image.getAbsolutePath(), image.getName(), noteGroup);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    break;
                }
            case REQUEST_IMAGE_CAPTURE:
                if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
                    //Bundle extras = data.getExtras();
                    openScannerActivity(capturedImage.getAbsolutePath(), capturedImage.getName(), noteGroup);
                }
                break;
        }
    }

    public File createImageFile() {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = null;
        try {
            image = File.createTempFile(
                    imageFileName,
                    ".jpg",
                    storageDir
            );
        } catch (IOException e) {
            e.printStackTrace();
        }
        capturedImage = image;
        return image;
    }

    private void openScannerActivity(String picturePath, String name, NoteGroup noteGroup) {
        Intent intent = new Intent(this, ScannerActivity.class);
        intent.putExtra(BaseScannerActivity.EXTRAS.PATH, picturePath);
        intent.putExtra(BaseScannerActivity.EXTRAS.NAME, name);
        intent.putExtra(BaseScannerActivity.EXTRAS.FROM_CAMERA, false);
        if(noteGroup!=null)
            intent.putExtra(NoteGroup.class.getSimpleName(), Parcels.wrap(noteGroup));

        startActivityForResult(intent, BaseScannerActivity.EXTRAS.REQUEST_PHOTO_EDIT);
        overridePendingTransition(R.anim.slide_left_in, R.anim.slide_left_out);
    }
}
