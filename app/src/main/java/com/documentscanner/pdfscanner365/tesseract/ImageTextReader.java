package com.documentscanner.pdfscanner365.tesseract;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;

import com.documentscanner.pdfscanner365.imagestopdf.Provider;
import com.documentscanner.pdfscanner365.main.Const;
import com.google.api.client.util.DateTime;
import com.googlecode.tesseract.android.TessBaseAPI;
import com.googlecode.tesseract.android.TessPdfRenderer;
import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Element;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.Rectangle;
import com.itextpdf.text.pdf.PdfAction;
import com.itextpdf.text.pdf.PdfDocument;
import com.itextpdf.text.pdf.PdfWriter;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

import timber.log.Timber;

public class ImageTextReader {

    private static Context context;
    public static final String TAG = "ImageTextReader";
    public  boolean success;
    /**
     * TessBaseAPI instance
     */

    private static volatile TessBaseAPI api;
    //  private static volatile TesseractImageTextReader INSTANCE;

    /**
     * initialize and train the tesseract engine
     *
     * @param path     a path to training data
     * @param language language code i.e. selected by user
     * @return the instance of this class for later use
     */
    public static ImageTextReader geInstance(String path, String language,int pageSegMode, TessBaseAPI.ProgressNotifier progressNotifier, Context c) {
        context = c;
        try {
            ImageTextReader imageTextReader=new ImageTextReader();
            api = new TessBaseAPI(progressNotifier);
            imageTextReader.success = api.init(path, language);
            api.setPageSegMode(pageSegMode);
            return imageTextReader;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }

    }

    /**
     * get the text from bitmap
     *
     * @param bitmap a image
     * @return text on image
     */
    public String getTextFromBitmap(Bitmap bitmap) {
        api.setImage(bitmap);
        String textOnImage;
        try {
            //textOnImage = api.getUTF8Text();
            textOnImage = api.getHOCRText(1);
        } catch (Exception e) {
            return "Scan Failed: Must be reported to developer!";
        }
        if (textOnImage.isEmpty()) {
            return "Scan Failed: Couldn't read the image\nProblem may be related to Tesseract or no Text on Image!";
        } else return textOnImage;

    }

    public boolean createPDF(ArrayList<File> bitmapFiles){

        File outputFile = com.documentscanner.pdfscanner365.imagestopdf.Utils.getOutputMediaFileOCR(com.documentscanner.pdfscanner365.imagestopdf.Utils.PDF_OCR_PATH,
                com.documentscanner.pdfscanner365.imagestopdf.Utils.getPDFName(bitmapFiles));

        /*Document document = new Document(PageSize.A4, 38, 38, 50, 38);

        Rectangle documentRect = document.getPageSize();

        try {
            PdfWriter.getInstance(document, new FileOutputStream(outputFile.getPath()));
        } catch (DocumentException | FileNotFoundException e) {
            e.printStackTrace();
        }*/

        TessPdfRenderer tessPdfRenderer = new TessPdfRenderer(api, outputFile.getPath());

        boolean begin = api.beginDocument(tessPdfRenderer, "New");
        if(begin){
            for(int i = 0; i < bitmapFiles.size(); i++){

                File bmpFile = new File(Const.FOLDERS.CROP_IMAGE_PATH + File.separator + bitmapFiles.get(i).getName());
                try {
                    Bitmap bitmap = MediaStore.Images.Media.getBitmap(context.getContentResolver() ,
                            Provider.getUriForFile(context,  context.getApplicationContext().getPackageName() + ".provider", bmpFile));

                    //api.setImage(bitmap);

                    boolean addedPage = api.addPageToDocument(Utils.preProcessBitmapToPix(bitmap),
                            Const.FOLDERS.CROP_IMAGE_PATH + File.separator + bitmapFiles.get(i).getName(), tessPdfRenderer);
                    if(addedPage){
                        Timber.d("Page Added Successfully");
                    }else{
                        Timber.d("Page not added");
                        return false;
                        
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }else{
            return false;
        }
        boolean b = api.endDocument(tessPdfRenderer);
        tessPdfRenderer.recycle();
        return b;
    }

    /**
     * stop the image TEXT reader
     */
    public void stop() {
        api.stop();
    }

    /**
     * find the confidence or
     *
     * @return confidence
     */
    public int getAccuracy() {
        return api.meanConfidence();
    }

    /**
     * Closes down tesseract and free up all memory.
     */
    public void tearDownEverything() {
        api.recycle();
    }

    /**
     * Frees up recognition results and any stored image data,
     */
    public void clearPreviousImage() {
        api.clear();
    }

}
