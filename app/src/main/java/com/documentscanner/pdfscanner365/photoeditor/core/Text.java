package com.documentscanner.pdfscanner365.photoeditor.core;

import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.documentscanner.pdfscanner365.R;
import com.documentscanner.pdfscanner365.photoeditor.core.Graphic;
import com.documentscanner.pdfscanner365.photoeditor.core.GraphicManager;
import com.documentscanner.pdfscanner365.photoeditor.core.MultiTouchListener;
import com.documentscanner.pdfscanner365.photoeditor.core.OnPhotoEditorListener;
import com.documentscanner.pdfscanner365.photoeditor.core.PhotoEditorViewState;
import com.documentscanner.pdfscanner365.photoeditor.core.TextStyleBuilder;
import com.documentscanner.pdfscanner365.photoeditor.core.ViewType;

/**
 * Created by Burhanuddin Rashid on 14/05/21.
 *
 * @author <https://github.com/burhanrashid52>
 */
class Text extends com.documentscanner.pdfscanner365.photoeditor.core.Graphic {

    private final com.documentscanner.pdfscanner365.photoeditor.core.MultiTouchListener mMultiTouchListener;
    private final Typeface mDefaultTextTypeface;
    private final com.documentscanner.pdfscanner365.photoeditor.core.GraphicManager mGraphicManager;
    private final ViewGroup mPhotoEditorView;
    private final com.documentscanner.pdfscanner365.photoeditor.core.PhotoEditorViewState mViewState;
    private TextView mTextView;

    public Text(ViewGroup photoEditorView,
                com.documentscanner.pdfscanner365.photoeditor.core.MultiTouchListener multiTouchListener,
                com.documentscanner.pdfscanner365.photoeditor.core.PhotoEditorViewState viewState,
                Typeface defaultTextTypeface,
                com.documentscanner.pdfscanner365.photoeditor.core.GraphicManager graphicManager
    ) {
        super(photoEditorView.getContext(), graphicManager);
        mPhotoEditorView = photoEditorView;
        mViewState = viewState;
        mMultiTouchListener = multiTouchListener;
        mDefaultTextTypeface = defaultTextTypeface;
        mGraphicManager = graphicManager;
        setupGesture();
    }

    void buildView(String text, TextStyleBuilder styleBuilder) {
        mTextView.setText(text);
        if (styleBuilder != null)
            styleBuilder.applyStyle(mTextView);
    }

    private void setupGesture() {
        com.documentscanner.pdfscanner365.photoeditor.core.MultiTouchListener.OnGestureControl onGestureControl = buildGestureController(mPhotoEditorView, mViewState);
        mMultiTouchListener.setOnGestureControl(onGestureControl);
        View rootView = getRootView();
        rootView.setOnTouchListener(mMultiTouchListener);
    }


    @Override
    ViewType getViewType() {
        return ViewType.TEXT;
    }

    @Override
    int getLayoutId() {
        return R.layout.view_photo_editor_text;
    }

    @Override
    void setupView(View rootView) {
        mTextView = rootView.findViewById(R.id.tvPhotoEditorText);
        if (mTextView != null && mDefaultTextTypeface != null) {
            mTextView.setGravity(Gravity.CENTER);
            mTextView.setTypeface(mDefaultTextTypeface);
        }
    }

    @Override
    void updateView(View view) {
        String textInput = mTextView.getText().toString();
        int currentTextColor = mTextView.getCurrentTextColor();
        OnPhotoEditorListener photoEditorListener = mGraphicManager.getOnPhotoEditorListener();
        if (photoEditorListener != null) {
            photoEditorListener.onEditTextChangeListener(view, textInput, currentTextColor);
        }
    }
}
