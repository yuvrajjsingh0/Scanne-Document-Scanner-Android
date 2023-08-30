package com.documentscanner.pdfscanner365.activity;

import android.Manifest;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;

import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.view.ActionMode;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.GridLayoutManager;

import android.os.Environment;
import android.os.PersistableBundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.documentscanner.pdfscanner365.BuildConfig;
import com.documentscanner.pdfscanner365.LanguageDownloaderService;
import com.documentscanner.pdfscanner365.activity.adapters.NoteAdapter;
import com.documentscanner.pdfscanner365.fragment.ImageSelectorBottomSheetFragment;
import com.documentscanner.pdfscanner365.main.App;
import com.documentscanner.pdfscanner365.utils.PhotoUtil;
import com.documentscanner.pdfscanner365.utils.Themes;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;

import org.jetbrains.annotations.NotNull;
import org.parceler.Parcels;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.concurrent.Executor;

import butterknife.ButterKnife;
import timber.log.Timber;

import com.documentscanner.pdfscanner365.R;
import com.documentscanner.pdfscanner365.activity.adapters.MultiSelector;
import com.documentscanner.pdfscanner365.activity.adapters.NoteGroupAdapter;
import com.documentscanner.pdfscanner365.activity.adapters.ParcelableSparseBooleanArray;
import com.documentscanner.pdfscanner365.activity.callbacks.HomeView;
import com.documentscanner.pdfscanner365.databinding.ActivityMainBinding;
import com.documentscanner.pdfscanner365.db.models.NoteGroup;
import com.documentscanner.pdfscanner365.main.Const;
import com.documentscanner.pdfscanner365.presenters.HomePresenter;
import com.documentscanner.pdfscanner365.utils.AppUtility;
import com.documentscanner.pdfscanner365.utils.ItemOffsetDecoration;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.play.core.review.ReviewInfo;
import com.google.android.play.core.review.ReviewManager;
import com.google.android.play.core.review.ReviewManagerFactory;
import com.google.android.play.core.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.googlecode.tesseract.android.TessBaseAPI;
import com.googlecode.tesseract.android.TessPdfRenderer;
import com.sanojpunchihewa.updatemanager.UpdateManager;
import com.sanojpunchihewa.updatemanager.UpdateManagerConstant;

public class HomeActivity extends BaseActivity implements HomeView, ImageSelectorBottomSheetFragment.OnItemClickListener, MultiSelector.OnItemSelectedListener{

    HomePresenter homePresenter;

    public static final String IS_IN_ACTION_MODE = "IS_IN_ACTION_MODE";
    private MultiSelector multiSelector;
    private ActionMode actionMode;
    ActivityMainBinding binding;
    private UpdateManager mUpdateManager;
    FirebaseAuth mAuth;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this /* Activity context */);

        String newValue = sharedPreferences.getString("theme_sel", "Default");

        String[] themes = new String[3];
        themes[0] = "Default";
        themes[1] = "Light";
        themes[2] = "Dark";

        if(Themes.getTheme(newValue) != AppCompatDelegate.getDefaultNightMode()){
            if(newValue.equals(themes[0])){
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
            }else if(newValue.equals(themes[1])){
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
            }else if(newValue.equals(themes[2])){
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
            }

        }

        mAuth = FirebaseAuth.getInstance();

        FirebaseUser currentUser = mAuth.getCurrentUser();

        if(currentUser == null){
            mAuth.signInAnonymously()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            // Sign in success, update UI with the signed-in user's information
                            Timber.d("createUserWithEmail:success");
                            FirebaseUser user = mAuth.getCurrentUser();
                        } else {
                            // If sign in fails, display a message to the user.
                            Timber.w(task.getException(), "createUserWithEmail:failure");
                        }
                    });

        }

        /*mUpdateManager = UpdateManager.Builder(this).mode(UpdateManagerConstant.FLEXIBLE);
        mUpdateManager.start();*/

        if(App.isAds){
            AdView adView = findViewById(R.id.adView);
            adView.loadAd(new AdRequest.Builder().build());
        }

        ButterKnife.bind(this);

        if(!checkAndRequestPermissions(HomeActivity.this)){
            Toast.makeText(this, getString(R.string.req_storage_cam_access_message), Toast.LENGTH_LONG).show();
        }

        Intent intent = getIntent();
        String action = intent.getAction();
        String type = intent.getType();

        /*
        if (Intent.ACTION_SEND.equals(action) && type != null) {
            if (type.startsWith("image/")) {
                ArrayList<Uri> images = new ArrayList<>();
                Uri imageUri = (Uri) intent.getParcelableExtra(Intent.EXTRA_STREAM);
                images.add(Uri.parse(getRealPathFromURI(this, imageUri)));
                Timber.d("Selected Image: %s", images.get(0).getPath());
                onImagesPicked(images);
            }
        }*/

        init();

        if (savedInstanceState != null) {
            restoreSavedState(savedInstanceState);
        }
    }

    public String getRealPathFromURI(Context context, Uri contentUri) {
        Cursor cursor = null;
        try {
            String[] proj = { MediaStore.Images.Media.DATA };
            cursor = context.getContentResolver().query(contentUri,  proj, null, null, null);
            int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
            cursor.moveToFirst();
            return cursor.getString(column_index);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    public static boolean checkAndRequestPermissions(final Activity context) {
        int extstorePermission = ContextCompat.checkSelfPermission(context,
                Manifest.permission.READ_EXTERNAL_STORAGE);
        int cameraPermission = ContextCompat.checkSelfPermission(context,
                Manifest.permission.CAMERA);
        List<String> listPermissionsNeeded = new ArrayList<>();
        if (cameraPermission != PackageManager.PERMISSION_GRANTED) {
            listPermissionsNeeded.add(Manifest.permission.CAMERA);
        }

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            int locPermission = ContextCompat.checkSelfPermission(context,
                    Manifest.permission.ACCESS_MEDIA_LOCATION);
            if (locPermission != PackageManager.PERMISSION_GRANTED) {
                listPermissionsNeeded.add(Manifest.permission.ACCESS_MEDIA_LOCATION);
            }
        }else{
            if (extstorePermission != PackageManager.PERMISSION_GRANTED) {
                listPermissionsNeeded
                        .add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
            }
        }
        if (!listPermissionsNeeded.isEmpty()) {
            ActivityCompat.requestPermissions(context, listPermissionsNeeded
                            .toArray(new String[listPermissionsNeeded.size()]),
                    REQUEST_ID_MULTIPLE_PERMISSIONS);
            return false;
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,String[] permissions, int[] grantResults) {
        if (requestCode == REQUEST_ID_MULTIPLE_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(HomeActivity.this,
                    Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(getApplicationContext(),
                        getString(R.string.req_cam_access_message), Toast.LENGTH_SHORT)
                        .show();
                //finish();
            } else if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.Q && ContextCompat.checkSelfPermission(HomeActivity.this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(getApplicationContext(),
                        getString(R.string.req_storage_access_message),
                        Toast.LENGTH_SHORT).show();
                //finish();
            } else if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q && ContextCompat.checkSelfPermission(HomeActivity.this,
                    Manifest.permission.ACCESS_MEDIA_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(getApplicationContext(),
                        getString(R.string.req_media_access_message),
                        Toast.LENGTH_SHORT).show();
                //finish();
            }
        }
    }

    private void restoreSavedState(Bundle savedInstanceState) {
        // Restores the checked states
        multiSelector.onRestoreInstanceState(savedInstanceState);

        // Restore the action mode
        boolean isInActionMode = savedInstanceState.getBoolean(IS_IN_ACTION_MODE);
        if (isInActionMode) {
            startActionMode();
            updateActionModeTitle();
        }
    }

    @Override
    protected void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        restoreSavedState(savedInstanceState);
    }

    private void updateActionModeTitle() {
        if(multiSelector.getCount() == 0){
            actionMode.finish();
        }else {
            actionMode.setTitle(String.valueOf(multiSelector.getCount()));
        }
    }

    private void startActionMode() {
        actionMode = startSupportActionMode(actionModeCallback);
    }

    @Override
    public void onSaveInstanceState(@NotNull Bundle outState) {
        super.onSaveInstanceState(outState);
        multiSelector.onSaveInstanceState(outState);
        outState.putBoolean(IS_IN_ACTION_MODE, actionMode != null);
    }

    private void init() {
        homePresenter = new HomePresenter();
        homePresenter.attachView(this);

        setUpNoteGroupList();


        homePresenter.loadNoteGroups();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.home_activity_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    private void setUpNoteGroupList() {
        multiSelector = new MultiSelector(binding.noteGroupRv, this);

        binding.noteGroupRv.setHasFixedSize(true);
        GridLayoutManager gridLayoutManager = new GridLayoutManager(this, 2);
        binding.noteGroupRv.setLayoutManager(gridLayoutManager);
        ItemOffsetDecoration itemDecoration = new ItemOffsetDecoration(this, R.dimen.item_offset);
        binding.noteGroupRv.addItemDecoration(itemDecoration);

        NoteGroupAdapter adapter = new NoteGroupAdapter(this, multiSelector);
        adapter.setCallback(new NoteGroupAdapter.Callback() {
            @Override
            public void onItemClick(View view, int position, NoteGroup noteGroup) {
                if (isMultiSelectionEnabled()) {
                    multiSelector.checkView(view, position);
                    updateActionModeTitle();
                    showEditActionMode(multiSelector.getCount() <= 1);
                }
                else
                    openNoteGroupActivity(noteGroup);
            }

            @Override
            public void onItemLongClick(View view, int position) {
                if (!isMultiSelectionEnabled()) {
                    multiSelector.clearAll();
                    startActionMode();
                }
                multiSelector.checkView(view, position);
                updateActionModeTitle();
            }
        });
        binding.noteGroupRv.setAdapter(adapter);
    }

    private void showEditActionMode(boolean b) {
        if(actionMode!=null)
        {
            MenuItem menuItem = actionMode.getMenu().findItem(R.id.edit);
            if(menuItem!=null)
                menuItem.setVisible(b);
        }
    }


    private boolean isMultiSelectionEnabled() {
        return actionMode != null;
    }

    private ActionMode.Callback actionModeCallback = new ActionMode.Callback() {
        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            mode.getMenuInflater().inflate(R.menu.context_menu, menu);
            return true;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            return false;
        }

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            switch (item.getItemId()) {
                case R.id.delete:
                    onDeleteOptionClicked();
                    mode.finish();
                    return true;
                case R.id.share:
                    onShareOptionClicked();
                    mode.finish();
                    return true;
                case R.id.edit:
                    onEditOptionClicked();
                    mode.finish();
                    return true;

                default:
                    return false;
            }

        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {
            actionMode = null;
            multiSelector.clearAll();

            NoteGroupAdapter adapter = (NoteGroupAdapter) binding.noteGroupRv.getAdapter();
            if(adapter!=null)
                adapter.setNormalChoiceMode();
        }
    };

    private void onEditOptionClicked() {
        NoteGroupAdapter adapter = (NoteGroupAdapter) binding.noteGroupRv.getAdapter();
        if(adapter!=null)
        {
            NoteGroup noteGroup  = adapter.getCheckedNoteGroup();
            if(noteGroup!=null) {
                homePresenter.showRenameDialog(noteGroup, noteGroup.name);
            }
        }
    }

    private void onShareOptionClicked() {
        NoteGroupAdapter adapter = (NoteGroupAdapter) binding.noteGroupRv.getAdapter();
        if(adapter!=null)
        {
            AppUtility.shareDocuments(this, adapter.getCheckedNoteGroups());
        }
    }

    private void onDeleteOptionClicked() {
        final ParcelableSparseBooleanArray checkItems = multiSelector.getCheckedItems();
        AppUtility.askAlertDialog(this, Const.DELETE_ALERT_TITLE, Const.DELETE_ALERT_MESSAGE, (dialog, which) -> {

            NoteGroupAdapter adapter = (NoteGroupAdapter) binding.noteGroupRv.getAdapter();
            if (adapter != null) {
                adapter.deleteItems();
                homePresenter.loadNoteGroups();
            }
            multiSelector.clearAll();
        },
                (dialog, which) -> dialog.dismiss());
    }

    private void openNoteGroupActivity(NoteGroup noteGroup) {
        Intent intent = new Intent(this, NoteGroupActivity.class);
        intent.putExtra(NoteGroup.class.getSimpleName(), Parcels.wrap(noteGroup));
        startActivity(intent);
        overridePendingTransition(R.anim.slide_left_in, R.anim.slide_left_out);
    }

    @Override
    protected void onDestroy() {
        homePresenter.detachView();
        super.onDestroy();
    }

    public void onCameraClicked(View view) {
        ImageSelectorBottomSheetFragment fragment = new ImageSelectorBottomSheetFragment();
        getSupportFragmentManager().beginTransaction().add(fragment, "imageselector").commitNow();
    }

    NoteGroup mNoteGroup = null;

    @Override
    public void loadNoteGroups(List<NoteGroup> noteGroups) {
        NoteGroupAdapter adapter = (NoteGroupAdapter) binding.noteGroupRv.getAdapter();
        adapter.setNoteGroups(noteGroups);
        adapter.notifyDataSetChanged();
        binding.noteGroupRv.requestFocus();

        binding.noteGroupRv.setVisibility(View.VISIBLE);
        binding.emptyViewContainer.setVisibility(View.GONE);
        binding.progress.setVisibility(View.GONE);
    }

    @Override
    public void showEmptyMessage() {
        binding.noteGroupRv.setVisibility(View.GONE);
        binding.emptyViewContainer.setVisibility(View.VISIBLE);
        binding.progress.setVisibility(View.GONE);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(homePresenter!=null)
            homePresenter.loadNoteGroups();
    }

    @Override
    public Context getContext() {
        return this;
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.context_menu, menu);
        super.onCreateContextMenu(menu, v, menuInfo);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        return super.onContextItemSelected(item);
    }

    public void onImportGalleryClicked(MenuItem item) {
        selectImageFromGallery(null);
    }

    public void onRateUsClicked(MenuItem item) {
        //AppUtility.rateOnPlayStore(this);
        if(android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP){
            ReviewManager manager = ReviewManagerFactory.create(this);
            Task<ReviewInfo> request = manager.requestReviewFlow();
            request.addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    // We can get the ReviewInfo object
                    ReviewInfo reviewInfo = task.getResult();
                } else {
                    AppUtility.rateOnPlayStore(this);
                }
            });
        }else{
            AppUtility.rateOnPlayStore(this);
        }
    }

    public void onCloud(MenuItem item){
        startActivity(new Intent(this, SyncActivity.class));
    }

    public void onSettingsClicked(MenuItem item){
        startActivity(new Intent(this, SettingsActivity.class));
    }

    public void onUpgrade(MenuItem item){
        if(App.isAds){
            startActivity(new Intent(this, UpgradePro.class));
        }else{
            Toast.makeText(this, getString(R.string.pro_member_message), Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent imageReturnedIntent) {
        super.onActivityResult(requestCode, resultCode, imageReturnedIntent);
        if (requestCode == BaseScannerActivity.EXTRAS.REQUEST_PHOTO_EDIT) {
            switch (resultCode) {
                case BaseScannerActivity.EXTRAS.RESULT_DELETED:
                    String path = imageReturnedIntent.getStringExtra(BaseScannerActivity.EXTRAS.PATH);
                    PhotoUtil.deletePhoto(path);
                    break;
                case RESULT_OK:
                    mNoteGroup = Parcels.unwrap(imageReturnedIntent.getParcelableExtra(NoteGroup.class.getSimpleName()));
                    if (mNoteGroup != null) {
                        Intent intent = new Intent();
                        intent.putExtra(NoteGroup.class.getSimpleName(), Parcels.wrap(mNoteGroup));
                        setResult(RESULT_OK, intent);
                        //finish();
                    }
                    break;

            }
        }
    }

    @Override
    public void onBackPressed() {
        AppUtility.askAlertDialog(this, getString(R.string.rate_us_txt), getString(R.string.rate_us_desc),
                (dialog, which) -> {
                    setResult(Activity.RESULT_CANCELED);
                    dialog.dismiss();
                    AppUtility.rateOnPlayStore(HomeActivity.this);
                },
                (dialog, which) -> {
                    setResult(Activity.RESULT_CANCELED);
                    dialog.dismiss();
                    finishAffinity();
                });
    }

    @Override
    public void onClick(String i) {
        if(i.equals(ImageSelectorBottomSheetFragment.CAMERA)){
            openCamera(null);
        }else if(i.equals(ImageSelectorBottomSheetFragment.GALLERY)){
            selectImageFromGallery(null);
        }
    }

    @Override
    public void onChecked(View view, int position, boolean checked) {

        Timber.d("position %s%s", position, checked);

        /*
        NoteGroupAdapter.NoteGroupViewHolder noteViewHolder = null;

        if(view != null){
           noteViewHolder = new NoteGroupAdapter.NoteGroupViewHolder(view);
        }
        
        /*NoteGroupAdapter.NoteGroupViewHolder noteViewHolder = (NoteGroupAdapter.NoteGroupViewHolder)
                binding.noteGroupRv.findViewHolderForPosition(position);*/

        //NoteGroupAdapter adapter = (NoteGroupAdapter) binding.noteGroupRv.getAdapter();

        /*if(noteViewHolder != null){
            if(checked){
                noteViewHolder.checkBox.setVisibility(View.VISIBLE);
                noteViewHolder.checkBox.setChecked(true);
                Timber.d("position1 %s", noteViewHolder.checkBox.isChecked());
            }else{
                noteViewHolder.checkBox.setVisibility(View.INVISIBLE);
                noteViewHolder.checkBox.setChecked(false);
            }
        }else{
            Timber.d("ViewHolder was null");
        }*/
    }
}
