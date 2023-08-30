package com.documentscanner.pdfscanner365.fragment;

import android.animation.ObjectAnimator;
import android.animation.TimeInterpolator;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;

import com.documentscanner.pdfscanner365.activity.AddSignatureActivity;
import com.documentscanner.pdfscanner365.main.App;
import com.documentscanner.pdfscanner365.utils.SavingBitmapTask;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.InterstitialAd;
import com.squareup.picasso.Picasso;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import butterknife.ButterKnife;
import butterknife.OnClick;
import timber.log.Timber;

import com.documentscanner.pdfscanner365.R;
import com.documentscanner.pdfscanner365.activity.OCRActivity;
import com.documentscanner.pdfscanner365.activity.PreviewActivity;
import com.documentscanner.pdfscanner365.databinding.FragmentImage2Binding;
import com.documentscanner.pdfscanner365.db.models.Note;
import com.documentscanner.pdfscanner365.db.models.NoteGroup;
import com.documentscanner.pdfscanner365.interfaces.PhotoSavedListener;
import com.documentscanner.pdfscanner365.main.Const;
import com.documentscanner.pdfscanner365.main.FileUtil;
import com.documentscanner.pdfscanner365.manager.NotificationManager;
import com.documentscanner.pdfscanner365.utils.AppUtility;
import com.documentscanner.pdfscanner365.utils.RotatePhotoTask;

import static android.app.Activity.RESULT_OK;

/**
 * A simple {@link Fragment} subclass.
 */
public class ImageFragment extends Fragment {

    private static final long ANIM_DURATION = 600;
    private Note note;
    private NoteGroup notegroup;

    private int thumbnailTop;
    private int thumbnailLeft;
    private int thumbnailWidth;
    private int thumbnailHeight;
    private int mLeftDelta;
    private int mTopDelta;
    private float mWidthScale;
    private float mHeightScale;
    private FragmentImage2Binding binding;

    private final int REQUEST_IMG_SIGN = 121;

    public ImageFragment() {
        // Required empty public constructor
    }

    public static ImageFragment newInstance(Note note, NoteGroup noteGroup) {
        ImageFragment fragment = new ImageFragment();
        fragment.note = note;
        fragment.notegroup = noteGroup;

        return fragment;
    }


    @Override
    public View onCreateView(@NotNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        binding = FragmentImage2Binding.inflate(inflater, container, false);
        ButterKnife.bind(this, binding.getRoot());

        if(App.isAds){
            InterstitialAd mInterstitialAd = new InterstitialAd(getContext());
            mInterstitialAd.setAdUnitId(getString(R.string.admob_interestitial_id));
            mInterstitialAd.loadAd(new AdRequest.Builder().build());
            mInterstitialAd.setAdListener(new AdListener(){
                @Override
                public void onAdLoaded() {
                    super.onAdLoaded();
                    mInterstitialAd.show();
                }
            });
        }

        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NotNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Bundle bundle = getActivity().getIntent().getExtras();
        thumbnailTop = bundle.getInt(PreviewActivity.TOP);
        thumbnailLeft = bundle.getInt(PreviewActivity.LEFT);
        thumbnailWidth = bundle.getInt(PreviewActivity.WIDTH);
        thumbnailHeight = bundle.getInt(PreviewActivity.HEIGHT);

        init();
    }

    @OnClick(R.id.share_ib)
    public void onShareButtonClicked()
    {
        AppUtility.shareDocument(getActivity(), note.getImagePath());
    }

    @OnClick(R.id.delete_ib)
    public void onDeleteButtonClicked()
    {
        AppUtility.askAlertDialog(getActivity(), Const.DELETE_ALERT_TITLE, Const.DELETE_ALERT_MESSAGE, (dialog, which) -> {
            NotificationManager.getInstance().raiseNotification(getActivity(), Const.NotificationConst.DELETE_DOCUMENT, note, null);
            getActivity().finish();
        }, (dialog, which) -> dialog.dismiss());
    }

    @OnClick(R.id.ocr_butt)
    public void ocr() {
        try {
            startActivity(new Intent(getActivity(), OCRActivity.class)
                    .putExtra("image", FileUtil.from(getContext(), note.getImagePath()).getPath()));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @OnClick(R.id.sign_butt)
    public void sign(){
        startActivityForResult(new Intent(getActivity(), AddSignatureActivity.class)
            .setData(note.getImagePath()), REQUEST_IMG_SIGN);
    }

    @OnClick(R.id.rotate_right_ib)
    public void onRotateRightButtonClicked()
    {
        rotatePhoto(90);
    }

    @OnClick(R.id.rotate_left_ib)
    public void onRotateLeftButtonClicked()
    {
        rotatePhoto(-90);
    }

    private void rotatePhoto(float angle)
    {
        binding.progress.setVisibility(View.VISIBLE);

        try {
            InputStream inputStream = getActivity().getContentResolver().openInputStream(note.getImagePath());
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        try {
            new RotatePhotoTask(FileUtil.from(getContext(), note.getImagePath()).getPath(), angle, new PhotoSavedListener() {
                @Override
                public void photoSaved(String path, String name) {
                    Bitmap bitmap = BitmapFactory.decodeFile(path);
                    if(bitmap!=null) {
                        binding.progress.setVisibility(View.GONE);
                        binding.previewIv.setImageBitmap(bitmap);
                    }
                }

                @Override
                public void onNoteGroupSaved(NoteGroup noteGroup) {

                }
            }).execute();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String getPath (Uri uri)
    {
        String[] projection = {MediaStore.Images.Media.DATA};
        Cursor cursor = getActivity().getContentResolver().query(uri,
                projection,
                null,
                null,
                null);
        if (cursor == null)
            return null;
        int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
        cursor.moveToFirst();
        String s = cursor.getString(column_index);
        cursor.close();
        return s;
    }

    boolean isShowingBars = true;

    private void init() {
        Picasso.with(getActivity()).load(note.getImagePath()).into(binding.previewIv);

        binding.previewIv.setOnClickListener(v -> {
            if(isShowingBars){
                binding.bottomBar.setVisibility(View.GONE);
                binding.topBar.setVisibility(View.GONE);
                isShowingBars = false;
            }else{
                binding.bottomBar.setVisibility(View.VISIBLE);
                binding.topBar.setVisibility(View.VISIBLE);
                isShowingBars = true;
            }
        });

        ViewTreeObserver observer = binding.previewIv.getViewTreeObserver();
        observer.addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {

            @Override
            public boolean onPreDraw() {
                binding.previewIv.getViewTreeObserver().removeOnPreDrawListener(this);

                // Figure out where the thumbnail and full size versions are, relative
                // to the screen and each other
                int[] screenLocation = new int[2];
                binding.previewIv.getLocationOnScreen(screenLocation);
                mLeftDelta = thumbnailLeft - screenLocation[0];
                mTopDelta = thumbnailTop - screenLocation[1];

                // Scale factors to make the large version the same size as the thumbnail
                mWidthScale = (float) thumbnailWidth / binding.previewIv.getWidth();
                mHeightScale = (float) thumbnailHeight / binding.previewIv.getHeight();

                enterAnimation();

                return true;
            }
        });
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if(requestCode == REQUEST_IMG_SIGN && resultCode == RESULT_OK && data != null){
            String editedFilePath = data.getStringExtra("result");
            Timber.d("EditedFilePath: %s", editedFilePath);
            File outputFile = AppUtility.getOutputMediaFile(Const.FOLDERS.CROP_IMAGE_PATH, System.currentTimeMillis() + ".jpg");

            if(outputFile != null){
                new SavingBitmapTask(notegroup, BitmapFactory.decodeFile(editedFilePath), outputFile.getAbsolutePath(), new PhotoSavedListener() {
                    @Override
                    public void photoSaved(String path, String name) {
                        Timber.d("PhotoSaved");
                    }

                    @Override
                    public void onNoteGroupSaved(NoteGroup noteGroup) {
                        ImageFragment.this.notegroup = noteGroup;
                        ImageFragment.this.note = noteGroup.getNotes().get(noteGroup.getNotes().size() - 1);
                        Picasso.with(getActivity()).load(note.getImagePath()).into(binding.previewIv);
                    }

                }).execute();
            }
        }
    }

    /**
     * The enter animation scales the picture in from its previous thumbnail
     * size/location.
     */
    public void enterAnimation() {

        // Set starting values for properties we're going to animate. These
        // values scale and position the full size version down to the thumbnail
        // size/location, from which we'll animate it back up
        binding.previewIv.setPivotX(0);
        binding.previewIv.setPivotY(0);
        binding.previewIv.setScaleX(mWidthScale);
        binding.previewIv.setScaleY(mHeightScale);
        binding.previewIv.setTranslationX(mLeftDelta);
        binding.previewIv.setTranslationY(mTopDelta);

        // interpolator where the rate of change starts out quickly and then decelerates.
        TimeInterpolator sDecelerator = new DecelerateInterpolator();

        // Animate scale and translation to go from thumbnail to full size
        binding.previewIv.animate().setDuration(ANIM_DURATION).scaleX(1).scaleY(1).
                translationX(0).translationY(0).setInterpolator(sDecelerator);

        // Fade in the black background
        ObjectAnimator bgAnim = ObjectAnimator.ofInt(new ColorDrawable(Color.BLACK), "alpha", 0, 255);
        bgAnim.setDuration(ANIM_DURATION);
        bgAnim.start();

    }

    /**
     * The exit animation is basically a reverse of the enter animation.
     * This Animate image back to thumbnail size/location as relieved from bundle.
     *
     * @param endAction This action gets run after the animation completes (this is
     *                  when we actually switch activities)
     */
    public void exitAnimation(final Runnable endAction) {

        TimeInterpolator sInterpolator = new AccelerateInterpolator();
        binding.previewIv.animate().setDuration(ANIM_DURATION).scaleX(mWidthScale).scaleY(mHeightScale).
                translationX(mLeftDelta).translationY(mTopDelta)
                .setInterpolator(sInterpolator).withEndAction(endAction);

        // Fade out background
        ObjectAnimator bgAnim = ObjectAnimator.ofInt(new ColorDrawable(Color.WHITE), "alpha", 0);
        bgAnim.setDuration(ANIM_DURATION);
        bgAnim.start();
    }

    public void onBackPressed() {
        exitAnimation(() -> getActivity().finish());
    }
}
