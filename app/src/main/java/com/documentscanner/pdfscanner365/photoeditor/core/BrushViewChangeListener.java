package com.documentscanner.pdfscanner365.photoeditor.core;

import com.documentscanner.pdfscanner365.photoeditor.core.BrushDrawingView;

/**
 * Created on 1/17/2018.
 * @author <a href="https://github.com/burhanrashid52">Burhanuddin Rashid</a>
 * <p></p>
 */

interface BrushViewChangeListener {
    void onViewAdd(com.documentscanner.pdfscanner365.photoeditor.core.BrushDrawingView brushDrawingView);

    void onViewRemoved(BrushDrawingView brushDrawingView);

    void onStartDrawing();

    void onStopDrawing();
}
