package com.documentscanner.pdfscanner365.main;

import android.os.Environment;

import com.documentscanner.pdfscanner365.R;

import java.io.File;

public final class Const {

    public static final boolean DEBUG = true;
    public static final String DELETE_ALERT_TITLE = App.context.getString(R.string.delete_alert_title);
    public static final String DELETE_ALERT_MESSAGE = App.context.getString(R.string.delete_alert_message);

    public static final class FOLDERS {

        private static final String ROOT = File.separator + ".DocScanner";

        private static final String CROP = ROOT + File.separator + "CroppedImages";

        private static final String SD_CARD_PATH = App.context.getExternalFilesDir(null).getPath();

        public static final String PATH = SD_CARD_PATH + ROOT;

        public static final String CROP_IMAGE_PATH = SD_CARD_PATH + CROP;


    }

    public static final int IMAGE_SELECTOR_REQUEST_CODE = 444;

    public static final class NotificationConst {

        public static final String DELETE_DOCUMENT = "DELETE_DOCUMENT";
    }

}
