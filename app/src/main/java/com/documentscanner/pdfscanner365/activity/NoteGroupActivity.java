package com.documentscanner.pdfscanner365.activity;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;

import android.graphics.BitmapFactory;
import android.os.Bundle;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.view.ActionMode;
import androidx.recyclerview.widget.GridLayoutManager;
import timber.log.Timber;

import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import com.documentscanner.pdfscanner365.fragment.ImageSelectorBottomSheetFragment;
import com.documentscanner.pdfscanner365.interfaces.PhotoSavedListener;
import com.documentscanner.pdfscanner365.main.App;
import com.documentscanner.pdfscanner365.tesseract.Tess;
import com.documentscanner.pdfscanner365.utils.SavingBitmapTask;
import com.documentscanner.pdfscanner365.utils.TessLang;
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;

import org.parceler.Parcels;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Observable;
import java.util.concurrent.atomic.AtomicBoolean;

import com.documentscanner.pdfscanner365.imagestopdf.CreatePDFListener;
import com.documentscanner.pdfscanner365.imagestopdf.PDFEngine;
import com.documentscanner.pdfscanner365.R;
import com.documentscanner.pdfscanner365.activity.adapters.MultiSelector;
import com.documentscanner.pdfscanner365.activity.adapters.NoteAdapter;
import com.documentscanner.pdfscanner365.activity.adapters.ParcelableSparseBooleanArray;
import com.documentscanner.pdfscanner365.databinding.ActivityNoteGroupBinding;
import com.documentscanner.pdfscanner365.db.DBManager;
import com.documentscanner.pdfscanner365.db.models.Note;
import com.documentscanner.pdfscanner365.db.models.NoteGroup;
import com.documentscanner.pdfscanner365.fragment.ShareDialogFragment;
import com.documentscanner.pdfscanner365.fragment.ShareDialogFragment.ShareDialogListener;
import com.documentscanner.pdfscanner365.main.Const;
import com.documentscanner.pdfscanner365.manager.NotificationManager;
import com.documentscanner.pdfscanner365.manager.NotificationModel;
import com.documentscanner.pdfscanner365.manager.NotificationObserver;
import com.documentscanner.pdfscanner365.utils.AppUtility;
import com.documentscanner.pdfscanner365.utils.ItemOffsetDecoration;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.rewarded.RewardItem;
import com.google.android.gms.ads.rewarded.RewardedAd;
import com.google.android.gms.ads.rewarded.RewardedAdCallback;
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback;
import com.google.android.gms.ads.rewardedinterstitial.RewardedInterstitialAd;
import com.google.android.gms.ads.rewardedinterstitial.RewardedInterstitialAdLoadCallback;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public class NoteGroupActivity extends BaseActivity implements NotificationObserver, ShareDialogListener, CreatePDFListener,
        ImageSelectorBottomSheetFragment.OnItemClickListener, MultiSelector.OnItemSelectedListener {

    private NoteGroup mNoteGroup;
    private MultiSelector multiSelector;

    public static final String IS_IN_ACTION_MODE = "IS_IN_ACTION_MODE";
    private ActionMode actionMode;
    private boolean isShareClicked;

    int EDIT_IMG_REQ_CODE = 789;

    private Tess tess;

    ActivityNoteGroupBinding binding;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityNoteGroupBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());


        if(App.isAds){
            AdView adView = findViewById(R.id.adView);
            adView.loadAd(new AdRequest.Builder().build());
        }

        init();
        if (savedInstanceState != null) {
            restoreSavedState(savedInstanceState);
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

    private void init() {
        registerNotifications();
        mNoteGroup = Parcels.unwrap(getIntent().getParcelableExtra(NoteGroup.class.getSimpleName()));

        multiSelector = new MultiSelector(binding.noteGroupRv, this);
        if(mNoteGroup!=null && mNoteGroup.notes.size()>0) {
            setUpNoteList(mNoteGroup.notes);
            setToolbar(mNoteGroup);
        }else
            finish();
    }

    private void updateActionModeTitle() {
        if(multiSelector.getCount() == 0){
            actionMode.finish();
        }else {
            actionMode.setTitle(String.valueOf(multiSelector.getCount()));
        }
    }

    private void startActionMode() {
//        toolbar.setVisibility(View.GONE);
        actionMode = startSupportActionMode(actionModeCallback);
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
//            toolbar.setVisibility(View.VISIBLE);
            actionMode = null;
            multiSelector.clearAll();

            NoteAdapter adapter = (NoteAdapter) binding.noteGroupRv.getAdapter();
            if(adapter!=null)
                adapter.setNormalChoiceMode();
        }
    };

    private void onShareOptionClicked() {

        NoteAdapter adapter = (NoteAdapter) binding.noteGroupRv.getAdapter();
        if(adapter!=null)
        {
            AppUtility.shareDocuments(this,adapter.getCheckedNotes());
        }
    }

    private void onEditOptionClicked() {
        NoteAdapter adapter = (NoteAdapter) binding.noteGroupRv.getAdapter();
        if(adapter!=null)
        {
            try {
                Timber.tag("Kutta").d(adapter.getCheckedNotes().get(0).getPath());

                startActivityForResult(new Intent(this, ImageEditorActivity.class)
                        .putExtra("imgURI", adapter.getCheckedNotes().get(0).getPath()).setData(adapter.getCheckedNotes().get(0)), EDIT_IMG_REQ_CODE);

            } catch (Exception e) {
                Timber.tag("Demo App").e(e); // This could throw if either `sourcePath` or `outputPath` is blank or Null
            }
        }
    }

    private void onDeleteOptionClicked() {
        AppUtility.askAlertDialog(this, Const.DELETE_ALERT_TITLE, Const.DELETE_ALERT_MESSAGE, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                ParcelableSparseBooleanArray checkItems = multiSelector.getCheckedItems();
                NoteAdapter adapter = (NoteAdapter) binding.noteGroupRv.getAdapter();
                if (adapter != null) {
                    adapter.deleteItems();
                }

                if(mNoteGroup.getNotes().size()==0) {
                    DBManager.getInstance().deleteNoteGroup(mNoteGroup.id);
                    finish();
                }

                multiSelector.clearAll();
            }
        }, (dialog, which) -> dialog.dismiss());
    }

    private void setToolbar(NoteGroup mNoteGroup) {
        // Get a support ActionBar corresponding to this toolbar
        ActionBar ab = getSupportActionBar();

        // Enable the Up button
        ab.setDisplayHomeAsUpEnabled(true);
        ab.setTitle(mNoteGroup.name);
    }

    private void setUpNoteList(List<Note> notes) {
        binding.noteGroupRv.setHasFixedSize(true);
        GridLayoutManager gridLayoutManager = new GridLayoutManager(this, 2);
        binding.noteGroupRv.setLayoutManager(gridLayoutManager);
        ItemOffsetDecoration itemDecoration = new ItemOffsetDecoration(this, R.dimen.item_offset);
        binding.noteGroupRv.addItemDecoration(itemDecoration);

        NoteAdapter adapter = new NoteAdapter(notes, multiSelector);
        adapter.setCallback(new NoteAdapter.Callback() {
            @Override
            public void onItemClick(View view, int position, Note note) {
                if (isMultiSelectionEnabled()) {
                    multiSelector.checkView(view, position);
                    actionMode.getMenu().findItem(R.id.edit).setVisible(multiSelector.getCount() <= 1);
                    updateActionModeTitle();
                }
                else
                    openPreviewActivity(view, mNoteGroup, position);
            }

            @Override
            public void onItemLongClick(View view, int position) {
                if (!isMultiSelectionEnabled()) {
                    multiSelector.clearAll();
                    startActionMode();
                }
                multiSelector.checkView(view, position);
                actionMode.getMenu().findItem(R.id.edit).setVisible(multiSelector.getCount() <= 1);
                updateActionModeTitle();
            }
        });
        binding.noteGroupRv.setAdapter(adapter);

    }

    private void openPreviewActivity(View view, NoteGroup mNoteGroup, int position) {
        int[] screenLocation = new int[2];
        ImageView imageView = (ImageView) view.findViewById(R.id.note_iv);
        imageView.getLocationOnScreen(screenLocation);
        PreviewActivity.startPreviewActivity(mNoteGroup, position, this, screenLocation, imageView.getWidth(), imageView.getHeight());
        overridePendingTransition(0, 0);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(R.anim.slide_right_in, R.anim.slide_right_out);
    }

    @Override
    public void registerNotifications() {
        NotificationManager.getInstance().registerNotification(Const.NotificationConst.DELETE_DOCUMENT, this);
    }

    @Override
    public void deRegisterNotifications() {
        NotificationManager.getInstance().deRegisterNotification(Const.NotificationConst.DELETE_DOCUMENT, this);
    }

    @Override
    public void update(Observable observable, Object data) {
        NotificationModel notificationModel = (NotificationModel) data;
        switch (notificationModel.notificationName)
        {
            case Const.NotificationConst.DELETE_DOCUMENT:
                onDeleteDocument(notificationModel);
                break;
        }
    }

    private void onDeleteDocument(NotificationModel notificationModel) {
        Note note = (Note) notificationModel.request;
        if(note!=null)
        {
            NoteAdapter adapter = (NoteAdapter) binding.noteGroupRv.getAdapter();
            if(adapter!=null)
            {
                adapter.deleteItem(note);
            }
        }
    }

    public void onCameraClicked(View view) {

        ImageSelectorBottomSheetFragment fragment = new ImageSelectorBottomSheetFragment();
        getSupportFragmentManager().beginTransaction().add(fragment, "imageselector1").commitNow();

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.note_group_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    public void onGeneratePDFClicked(MenuItem item) {
        ArrayList<File> files = getFilesFromNoteGroup();
        if(mNoteGroup.pdfPath!=null && PDFEngine.getInstance().checkIfPDFExists(files, new File(mNoteGroup.pdfPath).getName()))
        {
            PDFEngine.getInstance().openPDF(NoteGroupActivity.this, new File(mNoteGroup.pdfPath));
        }
        else {
            PDFEngine.getInstance().createPDF(this, files, this);
        }
    }

    public void onImportGalleryClicked(MenuItem item) {
       selectImageFromGallery(mNoteGroup);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(requestCode == BaseScannerActivity.EXTRAS.REQUEST_PHOTO_EDIT ||
                requestCode == CameraActivity.CAMERA_REQUEST_CODE) {
            if (resultCode == RESULT_OK && data != null) {
                mNoteGroup = Parcels.unwrap(data.getParcelableExtra(NoteGroup.class.getSimpleName()));
                if (mNoteGroup != null) {
                    updateView(mNoteGroup);
                }
            }
        }else if(requestCode == EDIT_IMG_REQ_CODE){
            if (resultCode == RESULT_OK && data != null) {
                String editedFilePath = data.getStringExtra("result");
                Timber.d("EditedFilePath: %s", editedFilePath);
                File outputFile = AppUtility.getOutputMediaFile(Const.FOLDERS.CROP_IMAGE_PATH, System.currentTimeMillis() + ".jpg");

                if(outputFile != null){
                    new SavingBitmapTask(mNoteGroup, BitmapFactory.decodeFile(editedFilePath), outputFile.getAbsolutePath(), new PhotoSavedListener() {
                        @Override
                        public void photoSaved(String path, String name) {
                            Timber.d("PhotoSaved");
                        }

                        @Override
                        public void onNoteGroupSaved(NoteGroup noteGroup) {
                            mNoteGroup = noteGroup;
                            updateView(noteGroup);
                        }

                    }).execute();
                }
            }
        }
    }

    private void updateView(NoteGroup mNoteGroup) {
        NoteAdapter noteAdapter = (NoteAdapter) binding.noteGroupRv.getAdapter();
        if(noteAdapter!=null)
        {
            noteAdapter.setNotes(mNoteGroup.notes);
            noteAdapter.notifyDataSetChanged();
        }
    }

    /*
    private ArrayList<String> getFilesFromNoteGroup()
    {
        ArrayList<String> files = new ArrayList<>();
        for(int index=0;index<mNoteGroup.getNotes().size();index++)
        {
            /*File file = new File(mNoteGroup.getNotes().get(index).getImagePath().getPath());
            Log.d("gh", file.getAbsolutePath());
            files.add(mNoteGroup.getNotes().get(index).getImagePath().getPath());
            /*if(file.exists())
                files.add(file);
        }
        return files;
    }
    */

    private ArrayList<File> getFilesFromNoteGroup()
    {
        ArrayList<File> files = new ArrayList<>();
        for(int index=0;index<mNoteGroup.getNotes().size();index++)
        {
            File file = new File(mNoteGroup.getNotes().get(index).getImagePath().getPath());
            Timber.tag("gh").d(file.getAbsolutePath());
            files.add(file);
            /*if(file.exists())
                files.add(file);*/
        }
        return files;
    }

    public void onShareButtonClicked(MenuItem item) {
        ShareDialogFragment bottomSheetDialogFragment = ShareDialogFragment.newInstance(this);
        bottomSheetDialogFragment.show(getSupportFragmentManager(), bottomSheetDialogFragment.getTag());
    }

    @Override
    public void sharePDF() {
        ArrayList<File> files = getFilesFromNoteGroup();
        if(mNoteGroup.pdfPath!=null && PDFEngine.getInstance().checkIfPDFExists(files, new File(mNoteGroup.pdfPath).getName()))
        {
            PDFEngine.getInstance().sharePDF(NoteGroupActivity.this, new File(mNoteGroup.pdfPath));
        } else {
            isShareClicked = true;
            PDFEngine.getInstance().createPDF(this, files, this);
        }
    }


    RewardedInterstitialAd mRewardedInterstitialAd;
    @Override
    public void sharePDFOCR(TessLang lang) {
        if(!App.isAds){
            shareOCR(lang);
        }else{
            // Watch ad to proceed
            MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this, R.style.Theme_MaterialComponents_DayNight_Dialog_Bridge);
            builder.setMessage(getString(R.string.watch_ads_message))
                    .setTitle(getString(R.string.premium_feature));

            builder.setPositiveButton(R.string.ok, (dialog, id) -> {
                // User clicked OK button
                AdRequest adRequest = new AdRequest.Builder().build();
                RewardedAd rewardedAd = new RewardedAd(this, getString(R.string.admob_rewarded_id));
                rewardedAd.loadAd(adRequest, new RewardedAdLoadCallback(){
                    @Override
                    public void onRewardedAdFailedToLoad(LoadAdError loadAdError) {
                        super.onRewardedAdFailedToLoad(loadAdError);
                        RewardedInterstitialAd.load(NoteGroupActivity.this, getString(R.string.admob_rewarded_int_id), adRequest, new RewardedInterstitialAdLoadCallback(){
                            @Override
                            public void onRewardedInterstitialAdLoaded(@NonNull RewardedInterstitialAd rewardedInterstitialAd) {
                                super.onRewardedInterstitialAdLoaded(rewardedInterstitialAd);
                                mRewardedInterstitialAd = rewardedInterstitialAd;
                                AtomicBoolean earned = new AtomicBoolean(false);
                                mRewardedInterstitialAd.setFullScreenContentCallback(new FullScreenContentCallback() {
                                    /** Called when the ad failed to show full screen content. */
                                    @Override
                                    public void onAdFailedToShowFullScreenContent(AdError adError) {
                                        Timber.i("onAdFailedToShowFullScreenContent");
                                    }

                                    /** Called when ad showed the full screen content. */
                                    @Override
                                    public void onAdShowedFullScreenContent() {
                                        Timber.i("onAdShowedFullScreenContent");
                                    }

                                    /** Called when full screen content is dismissed. */
                                    @Override
                                    public void onAdDismissedFullScreenContent() {
                                        Timber.i("onAdDismissedFullScreenContent");
                                        if(earned.get()){
                                            shareOCR(lang);
                                        }
                                    }
                                });
                                mRewardedInterstitialAd.show(NoteGroupActivity.this, rewardItem -> {
                                    // Rewarded
                                    earned.set(true);
                                });
                            }

                            @Override
                            public void onRewardedInterstitialAdFailedToLoad(LoadAdError loadAdError) {
                                super.onRewardedInterstitialAdFailedToLoad(loadAdError);
                                Toast.makeText(NoteGroupActivity.this, getString(R.string.upgrade_pro_prompt), Toast.LENGTH_LONG).show();
                            }
                        });


                    }

                    @Override
                    public void onRewardedAdLoaded() {
                        super.onRewardedAdLoaded();
                        AtomicBoolean earned = new AtomicBoolean(false);
                        rewardedAd.show(NoteGroupActivity.this, new RewardedAdCallback() {
                            @Override
                            public void onUserEarnedReward(@NonNull RewardItem rewardItem) {
                                earned.set(true);
                            }

                            @Override
                            public void onRewardedAdClosed() {
                                super.onRewardedAdClosed();
                                if(earned.get()){
                                    shareOCR(lang);
                                }
                            }
                        });
                    }
                });
            });
            builder.setNegativeButton(R.string.lbl_cancel, (dialog, id) -> {
                // User cancelled the dialog
                dialog.dismiss();
            });
            AlertDialog dialog = builder.create();
            dialog.show();
        }
    }

    private void shareOCR(TessLang lang){
        tess = new Tess(this, lang.getThreeLetterLang());
        tess.onInitializedOCR = () -> {
            File outputFile = com.documentscanner.pdfscanner365.imagestopdf.Utils.getOutputMediaFile(com.documentscanner.pdfscanner365.imagestopdf.Utils.PDF_OCR_PATH,
                    com.documentscanner.pdfscanner365.imagestopdf.Utils.getPDFName(getFilesFromNoteGroup()));
            if(!outputFile.exists()){
                ProgressDialog dialog = new ProgressDialog(NoteGroupActivity.this);
                dialog.setCancelable(false);
                tess.createPdf(getFilesFromNoteGroup(), new Tess.OnOcrPdfGeneratorResult() {
                    @Override
                    public void onStarted() {
                        Timber.d("Started");
                        dialog.setMessage(getString(R.string.please_wait));
                        dialog.show();
                    }

                    @Override
                    public void onSuccess(int accuracy) {
                        Timber.d("Created");
                        Toast.makeText(NoteGroupActivity.this, String.format(getString(R.string.created_success_accuracy),
                                String.valueOf(accuracy)), Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                        PDFEngine.getInstance().sharePDF(NoteGroupActivity.this, outputFile);
                        if(tess != null){
                            tess.onDestroy();
                        }
                    }

                    @Override
                    public void onFailure() {
                        Timber.d("Failed");
                        dialog.dismiss();
                        Toast.makeText(NoteGroupActivity.this, getString(R.string.error_occurred), Toast.LENGTH_SHORT).show();
                        if(tess != null){
                            tess.onDestroy();
                        }
                    }
                });
            }else{
                PDFEngine.getInstance().sharePDF(NoteGroupActivity.this, outputFile);
            }
        };
        tess.initializeOCR();
    }

    @Override
    public void shareImage() {
        AppUtility.shareDocuments(this, AppUtility.getUrisFromNotes(mNoteGroup.getNotes()));
    }

    @Override
    public void onPDFGenerated(File pdfFile, int numOfImages) {
        if(pdfFile != null) {
            this.mNoteGroup.pdfPath = pdfFile.getPath();
            if (pdfFile.exists()) {
                if(!isShareClicked)
                    PDFEngine.getInstance().openPDF(NoteGroupActivity.this, pdfFile);
                else
                    PDFEngine.getInstance().sharePDF(NoteGroupActivity.this, pdfFile);

                DBManager.getInstance().updateNoteGroupPDFInfo(mNoteGroup.id, pdfFile.getPath(), numOfImages);
            }
            isShareClicked = false;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(tess != null){
            tess.onDestroy();
        }
    }

    @Override
    public void onClick(String i) {
        if(i.equals(ImageSelectorBottomSheetFragment.CAMERA)){
            openCamera(mNoteGroup);
        }else if(i.equals(ImageSelectorBottomSheetFragment.GALLERY)){
            selectImageFromGallery(mNoteGroup);
        }
    }

    @Override
    public void onChecked(View view, int position, boolean checked) {
        Timber.d("position %s%s", position, checked);
        /*NoteAdapter.NoteViewHolder noteViewHolder = (NoteAdapter.NoteViewHolder)
                binding.noteGroupRv.findViewHolderForAdapterPosition(position);
        if(noteViewHolder != null){
            if(checked){
                noteViewHolder.checkBox.setVisibility(View.VISIBLE);
                noteViewHolder.checkBox.setChecked(true);
                Timber.d("position1 %s", noteViewHolder.checkBox.isChecked());
            }else{
                noteViewHolder.checkBox.setVisibility(View.INVISIBLE);
                noteViewHolder.checkBox.setChecked(false);
            }
        }*/
    }
}
