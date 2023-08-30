package com.documentscanner.pdfscanner365.photoeditor.core;

import android.graphics.Bitmap;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.documentscanner.pdfscanner365.R;
import com.documentscanner.pdfscanner365.photoeditor.core.Graphic;
import com.documentscanner.pdfscanner365.photoeditor.core.GraphicManager;
import com.documentscanner.pdfscanner365.photoeditor.core.MultiTouchListener;
import com.documentscanner.pdfscanner365.photoeditor.core.PhotoEditorViewState;
import com.documentscanner.pdfscanner365.photoeditor.core.ViewType;

/**
 * Created by Burhanuddin Rashid on 14/05/21.
 *
 * @author <https://github.com/burhanrashid52>
 */
class Sticker extends com.documentscanner.pdfscanner365.photoeditor.core.Graphic {

    private final com.documentscanner.pdfscanner365.photoeditor.core.MultiTouchListener mMultiTouchListener;
    private final ViewGroup mPhotoEditorView;
    private final com.documentscanner.pdfscanner365.photoeditor.core.PhotoEditorViewState mViewState;
    private ImageView imageView;

    public Sticker(ViewGroup photoEditorView,
                   com.documentscanner.pdfscanner365.photoeditor.core.MultiTouchListener multiTouchListener,
                   com.documentscanner.pdfscanner365.photoeditor.core.PhotoEditorViewState viewState,
                   com.documentscanner.pdfscanner365.photoeditor.core.GraphicManager graphicManager
    ) {
        super(photoEditorView.getContext(), graphicManager);
        mPhotoEditorView = photoEditorView;
        mViewState = viewState;
        mMultiTouchListener = multiTouchListener;
        setupGesture();
    }

    void buildView(Bitmap desiredImage) {
        imageView.setImageBitmap(desiredImage);
    }

    private void setupGesture() {
        com.documentscanner.pdfscanner365.photoeditor.core.MultiTouchListener.OnGestureControl onGestureControl = buildGestureController(mPhotoEditorView, mViewState);
        mMultiTouchListener.setOnGestureControl(onGestureControl);
        View rootView = getRootView();
        rootView.setOnTouchListener(mMultiTouchListener);
    }


    @Override
    ViewType getViewType() {
        return ViewType.IMAGE;
    }

    @Override
    int getLayoutId() {
        return R.layout.view_photo_editor_image;
    }

    @Override
    void setupView(View rootView) {
        imageView = rootView.findViewById(R.id.imgPhotoEditorImage);
    }
}
