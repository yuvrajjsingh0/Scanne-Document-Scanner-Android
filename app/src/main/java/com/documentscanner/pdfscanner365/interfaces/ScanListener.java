package com.documentscanner.pdfscanner365.interfaces;

import android.graphics.Bitmap;

/**
 * Created by droidNinja on 15/04/16.
 */
public interface ScanListener {
    void onRotateLeftClicked();

    void onRotateRightClicked();

    void onBackClicked();

    void onOkButtonClicked(Bitmap bitmap);
}
