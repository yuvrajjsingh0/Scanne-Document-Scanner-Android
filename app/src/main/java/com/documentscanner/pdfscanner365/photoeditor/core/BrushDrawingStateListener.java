package com.documentscanner.pdfscanner365.photoeditor.core;

import android.view.View;

import androidx.annotation.Nullable;
import com.documentscanner.pdfscanner365.photoeditor.core.BrushDrawingView;
import com.documentscanner.pdfscanner365.photoeditor.core.BrushViewChangeListener;
import com.documentscanner.pdfscanner365.photoeditor.core.OnPhotoEditorListener;
import com.documentscanner.pdfscanner365.photoeditor.core.PhotoEditorView;
import com.documentscanner.pdfscanner365.photoeditor.core.PhotoEditorViewState;
import com.documentscanner.pdfscanner365.photoeditor.core.ViewType;

/**
 * Created by Burhanuddin Rashid on 17/05/21.
 *
 * @author <https://github.com/burhanrashid52>
 */
public class BrushDrawingStateListener implements BrushViewChangeListener {
    private final PhotoEditorView mPhotoEditorView;
    private final PhotoEditorViewState mViewState;
    private @Nullable
    OnPhotoEditorListener mOnPhotoEditorListener;

    BrushDrawingStateListener(PhotoEditorView photoEditorView,
                              PhotoEditorViewState viewState) {
        mPhotoEditorView = photoEditorView;
        mViewState = viewState;
    }

    public void setOnPhotoEditorListener(@Nullable OnPhotoEditorListener onPhotoEditorListener) {
        mOnPhotoEditorListener = onPhotoEditorListener;
    }

    @Override
    public void onViewAdd(BrushDrawingView brushDrawingView) {
        if (mViewState.getRedoViewsCount() > 0) {
            mViewState.popRedoView();
        }
        mViewState.addAddedView(brushDrawingView);
        if (mOnPhotoEditorListener != null) {
            mOnPhotoEditorListener.onAddViewListener(
                    ViewType.BRUSH_DRAWING,
                    mViewState.getAddedViewsCount()
            );
        }
    }

    @Override
    public void onViewRemoved(BrushDrawingView brushDrawingView) {
        if (mViewState.getAddedViewsCount() > 0) {
            View removeView = mViewState.removeAddedView(
                    mViewState.getAddedViewsCount() - 1
            );
            if (!(removeView instanceof BrushDrawingView)) {
                mPhotoEditorView.removeView(removeView);
            }
            mViewState.pushRedoView(removeView);
        }

        if (mOnPhotoEditorListener != null) {
            mOnPhotoEditorListener.onRemoveViewListener(
                    ViewType.BRUSH_DRAWING,
                    mViewState.getAddedViewsCount()
            );
        }
    }

    @Override
    public void onStartDrawing() {
        if (mOnPhotoEditorListener != null) {
            mOnPhotoEditorListener.onStartViewChangeListener(ViewType.BRUSH_DRAWING);
        }
    }

    @Override
    public void onStopDrawing() {
        if (mOnPhotoEditorListener != null) {
            mOnPhotoEditorListener.onStopViewChangeListener(ViewType.BRUSH_DRAWING);
        }
    }
}
