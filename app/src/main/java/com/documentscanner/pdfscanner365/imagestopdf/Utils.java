package com.documentscanner.pdfscanner365.imagestopdf;

import android.os.Environment;
import android.util.Log;

import com.documentscanner.pdfscanner365.main.App;

import java.io.File;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;

import timber.log.Timber;

/**
 * Created by droidNinja on 25/07/16.
 */
public class Utils {
    private static final String IMAGE_TO_PDF_MODULE = "IMAGE_TO_PDF_MODULE";

    public static final String PDFS_PATH = App.context.getExternalFilesDir(null).getPath() + File.separator + ".DocScanner" + File.separator + "PDFs";
    public static final String PDF_OCR_PATH = App.context.getExternalFilesDir(null).getPath() + File.separator + ".DocScanner" + File.separator + "PDF_OCR";

    public static File getOutputMediaFile(String path, String name) {
        // To be safe, we should check that the SDCard is mounted
        if (!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            Timber.tag(IMAGE_TO_PDF_MODULE).e("External storage %s", Environment.getExternalStorageState());
            return null;
        }

        File dir = new File(path);
        // Create the storage directory if it doesn't exist
        if (!dir.exists()) {
            if (!dir.mkdirs()) {
                Timber.e("Failed to create directory");
                return null;
            }
        }

        return new File(dir.getPath() + File.separator + name);
    }

    public static File getOutputMediaFileOCR(String path, String name) {
        // To be safe, we should check that the SDCard is mounted
        if (!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            Timber.tag(IMAGE_TO_PDF_MODULE).e("External storage %s", Environment.getExternalStorageState());
            return null;
        }

        File dir = new File(path);
        // Create the storage directory if it doesn't exist
        if (!dir.exists()) {
            if (!dir.mkdirs()) {
                Timber.tag(IMAGE_TO_PDF_MODULE).e("Failed to create directory");
                return null;
            }
        }

        return new File(dir.getPath() + File.separator + name.substring(0, name.length() - 4));
    }

    public static String getPDFName(ArrayList<File> files)
    {
        String fileName  = "";
        for (int i = 0; i < files.size(); i++) {
            fileName += "_" + files.get(i).getName();
        }
        Timber.i(fileName);
        String md5 = getMd5(fileName);
        return "PDF_"+ md5 + ".pdf";
    }

    public static File getPDFFileFromName(String pdfName)
    {
        return new File(Utils.PDFS_PATH + File.separator + pdfName);
    }

    public static String getMd5(String text)
    {
        MessageDigest md = null;
        try {
            md = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return null;
        }

        md.update(text.getBytes());

        byte byteData[] = md.digest();

        //convert the byte to hex format method 1
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < byteData.length; i++) {
            sb.append(Integer.toString((byteData[i] & 0xff) + 0x100, 16).substring(1));
        }

        return sb.toString();
    }
}
