package com.documentscanner.pdfscanner365.imagestopdf;

import java.io.File;

/**
 * Created by droidNinja on 25/07/16.
 */
public interface CreatePDFListener {
    void onPDFGenerated(File pdfFile, int numOfImages);
}
