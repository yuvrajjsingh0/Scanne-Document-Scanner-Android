package com.documentscanner.pdfscanner365.activity;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.documentscanner.pdfscanner365.photoeditor.core.OnPhotoEditorListener;
import com.documentscanner.pdfscanner365.photoeditor.core.PhotoEditor;
import com.documentscanner.pdfscanner365.photoeditor.core.PhotoEditorView;
import com.documentscanner.pdfscanner365.photoeditor.core.SaveSettings;
import com.documentscanner.pdfscanner365.photoeditor.core.ViewType;
import timber.log.Timber;

import android.Manifest;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.documentscanner.pdfscanner365.BuildConfig;
import com.documentscanner.pdfscanner365.R;
import com.documentscanner.pdfscanner365.activity.adapters.SignaturesAdapter;
import com.documentscanner.pdfscanner365.fragment.ImageSelectorBottomSheetFragment;
import com.documentscanner.pdfscanner365.photoeditor.FileSaveHelper;
import com.google.android.material.snackbar.Snackbar;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import static android.content.pm.PackageManager.PERMISSION_GRANTED;
import static com.documentscanner.pdfscanner365.photoeditor.FileSaveHelper.isSdkHigherThan28;

public class AddSignatureActivity extends AppCompatActivity implements OnPhotoEditorListener, ImageSelectorBottomSheetFragment.OnItemClickListener {

    PhotoEditor mPhotoEditor;
    FileSaveHelper mSaveFileHelper;

    LinearLayout addSignLayout;

    RecyclerView signContainer;

    SignaturesAdapter adapter;

    ArrayList<File> signs;

    private File capturedImage;

    private final int CAPTURE_PHOTO = 1;
    private final int SELECT_PHOTO = 2;

    private ProgressDialog mProgressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_signature);

        PhotoEditorView mPhotoEditorView = findViewById(R.id.photoEditorView);

        mPhotoEditor = new PhotoEditor.Builder(this, mPhotoEditorView)
                .setPinchTextScalable(true)
                .build(); // build photo editor sdk

        mPhotoEditor.setOnPhotoEditorListener(this);

        handleIntentImage(mPhotoEditorView.getSource());

        mSaveFileHelper = new FileSaveHelper(this);

        addSignLayout = findViewById(R.id.add_sign);
        signContainer = findViewById(R.id.sign_container);

        addSignLayout.setOnClickListener(v -> openSignSelector());

        File[] signatures = getExternalFilesDir("signatures").listFiles();

        if(signatures == null || signatures.length == 0){
            addSignLayout.setVisibility(View.VISIBLE);
            signContainer.setVisibility(View.GONE);
        }else{
            addSignLayout.setVisibility(View.GONE);
            signContainer.setVisibility(View.VISIBLE);
        }

        signs = new ArrayList<>();
        signs.add(null);
        if(signatures != null){
            signs.addAll(Arrays.asList(signatures));
        }
        Timber.d("total sign: %s %s", signs.size(), signs);

        adapter = new SignaturesAdapter(this, signs, new SignaturesAdapter.OnItemClickListener() {
            @Override
            public void onClick(int position) {
                if(position == 0){
                    openSignSelector();
                }else{
                    mPhotoEditor.undo();
                    mPhotoEditor.addImage(BitmapFactory.decodeFile(signs.get(position).getPath()));
                }
            }

            @Override
            public boolean onLongClick(int position) {
                return false;
            }
        });

        signContainer.setLayoutManager(new LinearLayoutManager(this, RecyclerView.HORIZONTAL, false));

        signContainer.setAdapter(adapter);
    }

    private void handleIntentImage(ImageView source){
        Intent intent = getIntent();
        if(intent != null && intent.getData() != null){
            source.setImageURI(intent.getData());
        }else{
            finish();
        }
    }

    private void openSignSelector(){
        ImageSelectorBottomSheetFragment fragment = new ImageSelectorBottomSheetFragment();
        getSupportFragmentManager().beginTransaction().add(fragment, "imageselector1").commitNow();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_add_sign, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.done) {
            saveWithSign();
        }
        return super.onOptionsItemSelected(item);
    }

    private void saveWithSign(){
        final boolean hasStoragePermission =
                ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PERMISSION_GRANTED;
        if (hasStoragePermission || isSdkHigherThan28()) {
            final String fileName = System.currentTimeMillis() + ".png";
            File file = new File(getCacheDir() + "/" + File.separator + fileName);
            showLoading(getString(R.string.please_wait));
            try {
                if (file.createNewFile()) {
                    SaveSettings saveSettings = new SaveSettings.Builder()
                            .setClearViewsEnabled(true)
                            .setTransparencyEnabled(true)
                            .build();

                    mPhotoEditor.saveAsFile(file.getPath(), saveSettings, new PhotoEditor.OnSaveListener() {
                        @Override
                        public void onSuccess(@NonNull String imagePath) {
                            mSaveFileHelper.notifyThatFileIsNowPubliclyAvailable(getContentResolver());
                            hideLoading();
                            showSnackbar("Image Saved Successfully");
                            Timber.d("Saved Successfully at: %s", file.getPath());

                            Intent returnIntent = new Intent();
                            returnIntent.putExtra("result", file.getPath());
                            setResult(Activity.RESULT_OK, returnIntent);
                            finish();
                        }

                        @Override
                        public void onFailure(@NonNull Exception exception) {
                            hideLoading();
                            exception.printStackTrace();
                            showSnackbar("Failed to save Image");

                            Intent returnIntent = new Intent();
                            setResult(Activity.RESULT_CANCELED, returnIntent);
                            finish();
                        }
                    });
                }
            } catch (IOException e) {
                e.printStackTrace();
                hideLoading();
                showSnackbar("Failed to save Image");

                Intent returnIntent = new Intent();
                setResult(Activity.RESULT_CANCELED, returnIntent);
                finish();
            }
        }
    }



    @Override
    public void onEditTextChangeListener(View rootView, String text, int colorCode) {

    }

    @Override
    public void onAddViewListener(ViewType viewType, int numberOfAddedViews) {

    }

    @Override
    public void onRemoveViewListener(ViewType viewType, int numberOfAddedViews) {

    }

    @Override
    public void onStartViewChangeListener(ViewType viewType) {

    }

    @Override
    public void onStopViewChangeListener(ViewType viewType) {

    }

    protected void showLoading(@NonNull String message) {
        mProgressDialog = new ProgressDialog(this);
        mProgressDialog.setMessage(message);
        mProgressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        mProgressDialog.setCancelable(false);
        mProgressDialog.show();
    }

    protected void hideLoading() {
        if (mProgressDialog != null) {
            mProgressDialog.dismiss();
        }
    }

    protected void showSnackbar(@NonNull String message) {
        View view = findViewById(android.R.id.content);
        if (view != null) {
            Snackbar.make(view, message, Snackbar.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onClick(String i) {
        if(i.equals(ImageSelectorBottomSheetFragment.CAMERA)){
            openCamera();
        }else if(i.equals(ImageSelectorBottomSheetFragment.GALLERY)){
            selectImageFromGallery();
        }
    }

    private void selectImageFromGallery()
    {
        //this.noteGroup = noteGroup;
        Intent photoPickerIntent = new Intent(
                Intent.ACTION_PICK,
                android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(photoPickerIntent, SELECT_PHOTO);
    }

    private void openCamera(){
        // camera
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, FileProvider.getUriForFile(this,
                BuildConfig.APPLICATION_ID + ".provider",
                createSignatureFile()));
        try {
            startActivityForResult(takePictureIntent, CAPTURE_PHOTO);
        } catch (ActivityNotFoundException e) {
            e.printStackTrace();
        }
    }

    public File createSignatureFile() {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
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

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(resultCode == RESULT_OK){
            switch (requestCode){
                case SELECT_PHOTO:
                    Uri selectedImage = data.getData();
                    OutputStream fos;
                    try {
                        Bitmap bitmap = getResizedBitmap(MediaStore.Images.Media.getBitmap(this.getContentResolver(), selectedImage), 240);
                        File file = getExternalFilesDir("signatures");
                        String imagesDir = file.getPath();

                        if (!file.exists()) {
                            file.mkdir();
                        }

                        File image = new File(imagesDir, "tmp" + Calendar.getInstance().getTimeInMillis() + ".jpg");
                        fos = new FileOutputStream(image);
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos);

                        fos.flush();
                        fos.close();
                        updateView(image);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    break;
                case CAPTURE_PHOTO:
                    try {
                        Bitmap bitmap = getResizedBitmap(BitmapFactory.decodeFile(capturedImage.getPath()), 240);
                        File file = getExternalFilesDir("signatures");
                        String imagesDir = file.getPath();

                        if (!file.exists()) {
                            file.mkdir();
                        }

                        File image = new File(imagesDir, "tmp" + Calendar.getInstance().getTimeInMillis() + ".jpg");
                        fos = new FileOutputStream(image);
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos);

                        fos.flush();
                        fos.close();
                        updateView(image);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    break;
            }
        }
    }

    public Bitmap getResizedBitmap(Bitmap image, int maxSize) {
        int width = image.getWidth();
        int height = image.getHeight();

        float bitmapRatio = (float) width / (float) height;
        if (bitmapRatio > 1) {
            width = maxSize;
            height = (int) (width / bitmapRatio);
        } else {
            height = maxSize;
            width = (int) (height * bitmapRatio);
        }

        return Bitmap.createScaledBitmap(image, width, height, true);
    }

    private void updateView(File image){
        addSignLayout.setVisibility(View.GONE);
        signContainer.setVisibility(View.VISIBLE);
        signs.add(image);
        adapter.refresh(signs);
    }
}