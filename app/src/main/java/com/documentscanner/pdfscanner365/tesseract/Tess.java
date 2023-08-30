package com.documentscanner.pdfscanner365.tesseract;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.view.View;
import android.widget.Toast;

import com.documentscanner.pdfscanner365.R;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.googlecode.tesseract.android.TessBaseAPI;

import java.io.BufferedInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import java.io.File;
import java.util.ArrayList;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import timber.log.Timber;

import static android.content.Context.CONNECTIVITY_SERVICE;

public class Tess implements TessBaseAPI.ProgressNotifier {

    private final String TAG = "TessOCREngine";

    private Context context;
    private CrashUtils crashUtils;
    private DownloadTrainingTask downloadTrainingTask;
    private File dirBest;
    private File dirStandard;
    private File dirFast;
    private File currentDirectory;

    private String selLang = null;

    private ProgressDialog mProgressDialog;
    private AlertDialog dialog;
    private ImageTextReader mImageTextReader;

    private String mTrainingDataType;

    private final Tess tess;
    /**
     * selected language on image or used for detection
     */
    private String mLanguage;

    private int mPageSegMode;

    public OnInitializedOCR onInitializedOCR;

    private StorageReference storageRef;

    public Tess(Context context, String lang){
        this.context = context;
        this.selLang = lang;
        //this.onInitializedOCR = listener;

        SpUtil.getInstance().init(context);
        crashUtils = new CrashUtils(context.getApplicationContext(), "");

        FirebaseStorage storage = FirebaseStorage.getInstance("gs://tapscanner-4c6ef.appspot.com");

        storageRef = storage.getReference();

        initDirectories();
        /*
         *initialize the OCR or download the training data
         */

        //initializeOCR();
        tess = this;


    }

    public interface OnInitializedOCR{
        void onInitializedOCR();
    }

    public Tess getInstance(){
        return tess;
    }

    private void initDirectories() {
        dirBest = new File(context.getExternalFilesDir("best").getAbsolutePath());
        Timber.d("Tessfile is in %s", context.getExternalFilesDir("best").getAbsolutePath());
        dirFast = new File(context.getExternalFilesDir("fast").getAbsolutePath());
        dirStandard = new File(context.getExternalFilesDir("standard").getAbsolutePath());
        dirBest.mkdirs();
        dirStandard.mkdirs();
        dirFast.mkdirs();
        currentDirectory = new File(dirBest, "tessdata");
        currentDirectory.mkdirs();
        currentDirectory = new File(dirStandard, "tessdata");
        currentDirectory.mkdirs();
        currentDirectory = new File(dirFast, "tessdata");
        currentDirectory.mkdirs();
    }

    private void downloadLanguageData(final String dataType, final String lang) {

        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(CONNECTIVITY_SERVICE);
        NetworkInfo ni = cm.getActiveNetworkInfo();

        ArrayList<String> langToDownload = new ArrayList<>();
        if (lang.contains("+")) {
            String[] lang_codes = lang.split("\\+");
            for (String lang_code : lang_codes) {
                if (!isLanguageDataExists(dataType, lang)) {
                    langToDownload.add(lang_code);
                }
            }
        }

        if (ni == null) {
            //You are not connected to Internet
            Toast.makeText(context, context.getString(R.string.you_are_not_connected_to_internet), Toast.LENGTH_SHORT).show();
        } else if (ni.isConnected()) {
            //region show confirmation dialog, On 'yes' download the training data.
            String msg = String.format(context.getString(R.string.download_description), lang);
            dialog = new MaterialAlertDialogBuilder(context, R.style.Theme_MaterialComponents_DayNight_Dialog_Bridge)
                    .setTitle(R.string.training_data_missing)
                    .setCancelable(false)
                    .setMessage(msg)
                    .setPositiveButton(R.string.yes, (dialog, which) -> {
                        dialog.cancel();
                        mProgressDialog = new ProgressDialog(context);
                        mProgressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
                        mProgressDialog.setIndeterminate(true);
                        mProgressDialog.setCancelable(false);
                        mProgressDialog.setMessage(context.getString(R.string.fetching_data));
                        mProgressDialog.show();

                        storageRef.child("/4.0/" + lang + ".traineddata").getDownloadUrl().addOnSuccessListener(uri -> {
                            DownloadLinks links = new DownloadLinks();
                            links.lang = lang;
                            links.link = uri.toString();
                            storageRef.child("pdf.ttf").getDownloadUrl().addOnSuccessListener(uri1 -> {
                                Constants.TESSERACT_DATA_DOWNLOAD_PDF = uri1.toString();
                                ArrayList<DownloadLinks> linksList = new ArrayList<>();
                                linksList.add(links);
                                downloadTrainingTask = new DownloadTrainingTask();
                                downloadTrainingTask.execute(linksList);
                            }).addOnFailureListener(e -> {
                                mProgressDialog.dismiss();
                                Toast.makeText(context, context.getString(R.string.error_occurred), Toast.LENGTH_LONG).show();
                            });

                        }).addOnFailureListener(e -> {
                            mProgressDialog.dismiss();
                            Toast.makeText(context, context.getString(R.string.error_occurred), Toast.LENGTH_LONG).show();
                        });
                    })
                    .setNegativeButton(R.string.lbl_cancel, (dialog, which) -> {
                        dialog.cancel();
                        if (!isLanguageDataExists(dataType, lang)) {
                            //TODO:  show dialog to change language
                            Timber.d(lang);
                        }

                    }).create();
            dialog.show();
            //endregion
        } else {
            Toast.makeText(context, context.getString(R.string.you_are_not_connected_to_internet), Toast.LENGTH_SHORT).show();
            //You are not connected to Internet
        }
    }

    private boolean isLanguageDataExists(final String dataType, final String lang) {
        switch (dataType) {
            case "best":
                currentDirectory = new File(dirBest, "tessdata");
                break;
            case "standard":
                currentDirectory = new File(dirStandard, "tessdata");
                break;
            default:
                currentDirectory = new File(dirFast, "tessdata");

        }
        if (lang.contains("+")) {
            String[] lang_codes = lang.split("\\+");
            for (String code : lang_codes) {
                File file = new File(currentDirectory, String.format(Constants.LANGUAGE_CODE, code));
                if (!file.exists()) return false;
            }
            return true;
        } else {
            File language = new File(currentDirectory, String.format(Constants.LANGUAGE_CODE, lang));
            return language.exists();
        }
    }

    private boolean isPDFDATAExists(){
        File pdfDat = new File(currentDirectory, "pdf.tts");
        return pdfDat.exists();
    }

    public void initializeOCR() {
        File cf;
        mTrainingDataType = Utils.getTrainingDataType();
        if(selLang == null){
            mLanguage = Utils.getTrainingDataLanguage();
        }else{
            mLanguage = selLang;
        }
        mPageSegMode = Utils.getPageSegMode();


        switch (mTrainingDataType) {
            case "best":
                currentDirectory = new File(dirBest, "tessdata");
                cf = dirBest;
                break;
            case "standard":
                cf = dirStandard;
                currentDirectory = new File(dirStandard, "tessdata");
                break;
            default:
                cf = dirFast;
                currentDirectory = new File(dirFast, "tessdata");

        }

        if (isLanguageDataExists(mTrainingDataType, mLanguage)) {
            /*region Initialize image text reader
            new Thread() {
                @Override
                public void run() {

                }
            }.start();
            //endregion*/
            try {
                if (mImageTextReader != null) {
                    mImageTextReader.tearDownEverything();
                    mImageTextReader = null;
                }
                mImageTextReader = ImageTextReader.geInstance(cf.getAbsolutePath(), mLanguage, mPageSegMode, Tess.this, context);
                //check if current language data is valid
                //if it is invalid(i.e. corrupted, half downloaded, tempered) then delete it
                if (!mImageTextReader.success) {
                    Timber.d("initializeOCR: Reader Failed %s", mLanguage);
                    File destf = new File(currentDirectory, String.format(Constants.LANGUAGE_CODE, mLanguage));
                    destf.delete();
                    mImageTextReader = null;
                } else {
                    Timber.d("initializeOCR: Reader is initialize with lang:%s", mLanguage);
                    if(onInitializedOCR != null){
                        onInitializedOCR.onInitializedOCR();
                    }
                }

            } catch (Exception e) {
                crashUtils.logException(e);
                Timber.e(e, "initializeOCR: Reader Failed");
                File destf = new File(currentDirectory, String.format(Constants.LANGUAGE_CODE, mLanguage));
                destf.delete();
                mImageTextReader = null;
            }
        } else {
            downloadLanguageData(mTrainingDataType, mLanguage);
        }
    }

    public ImageTextReader getImageTextReader(){
        return mImageTextReader;
    }

    public void onDestroy() {
        if(mImageTextReader != null){
            mImageTextReader.tearDownEverything();
        }

        if (downloadTrainingTask != null && downloadTrainingTask.getStatus() == AsyncTask.Status.RUNNING) {
            downloadTrainingTask.cancel(true);
        }
        if (dialog != null) {
            dialog.dismiss();
            dialog = null;
        }
        if (mProgressDialog != null) {
            mProgressDialog.cancel();
            mProgressDialog = null;
        }
    }

    @Override
    public void onProgressValues(TessBaseAPI.ProgressValues progressValues) {
        Timber.d("onProgressValues: percent %s", progressValues.getPercent());
        // ((AppCompatActivity)context).runOnUiThread(() -> mProgressIndicator.setProgress((int) (progressValues.getPercent() * 1.46)));
    }

    private class DownloadLinks{
        public String link;
        public String lang;
    }

    private class DownloadTrainingTask extends AsyncTask<ArrayList<DownloadLinks>, Integer, Boolean> {
        String size;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            //mProgressDialog.setTitle(context.getString(R.string.downloading));
            mProgressDialog.setMessage(context.getString(R.string.downloading_language));
            mProgressDialog.show();
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            super.onProgressUpdate(values);
            int percentage = values[0];
            if (mProgressDialog != null) {
                mProgressDialog.setMessage(percentage + context.getString(R.string.percentage_downloaded) + size);
                mProgressDialog.show();
            }
        }

        @Override
        protected void onPostExecute(Boolean bool) {
            if (mProgressDialog != null) {
                mProgressDialog.cancel();
                mProgressDialog = null;
            }
            initializeOCR();
        }

        @Override
        protected Boolean doInBackground(ArrayList<DownloadLinks>... languages) {
            ArrayList<DownloadLinks> dataType = languages[0];
            //String lang = languages[1];
            boolean ret = true;
            if(dataType.size() > 1){
                for(int i=0; i < dataType.size(); i++){
                    if (!isLanguageDataExists("best", dataType.get(i).lang)) {
                        ret &= downloadTraningData("best", dataType.get(i).lang, false, dataType.get(i).lang);
                    }
                }
                downloadTraningData("best", dataType.get(0).lang, true, dataType.get(0).link);
            }else{
                if(downloadTraningData("best", dataType.get(0).lang, false, dataType.get(0).link)){
                    return downloadTraningData("best", dataType.get(0).lang, true, dataType.get(0).link);
                }else {
                    return false;
                }
            }
            return ret;
        }


        /**
         * done the actual work of download
         *
         * @param dataType data type i.e best, fast, standard
         * @param lang     language
         * @return true if success else false
         */
        private boolean downloadTraningData(String dataType, String lang, boolean isPDF, String link) {
            boolean result = true;
            String downloadURL;
            String location;
            String pdfEngine = Constants.TESSERACT_DATA_DOWNLOAD_PDF;

            switch (dataType) {
                case "best":
                    downloadURL = link;
                    //downloadURL = String.format(Constants.TESSERACT_DATA_DOWNLOAD_URL_BEST, lang);
                    break;
                case "standard":
                    downloadURL = link;
                    //downloadURL = String.format(Constants.TESSERACT_DATA_DOWNLOAD_URL_STANDARD, lang);
                    break;
                default:
                    downloadURL = link;
                    //downloadURL = String.format(Constants.TESSERACT_DATA_DOWNLOAD_URL_FAST, lang);
            }

            if(isPDF) {
                downloadURL = pdfEngine;
            }

            URL url, base, next;
            HttpURLConnection conn;
            try {
                while (true) {
                    Timber.tag(TAG).v("downloading %s", downloadURL);
                    try {
                        url = new URL(downloadURL);
                    } catch (java.net.MalformedURLException ex) {
                        Timber.e("url " + downloadURL + " is bad: " + ex);
                        return false;
                    }
                    conn = (HttpURLConnection) url.openConnection();
                    conn.setInstanceFollowRedirects(false);
                    switch (conn.getResponseCode()) {
                        case HttpURLConnection.HTTP_MOVED_PERM:
                        case HttpURLConnection.HTTP_MOVED_TEMP:
                            location = conn.getHeaderField("Location");
                            base = new URL(downloadURL);
                            next = new URL(base, location);  // Deal with relative URLs
                            downloadURL = next.toExternalForm();
                            continue;
                    }
                    break;
                }
                conn.connect();

                int totalContentSize = conn.getContentLength();
                size = Utils.getSize(totalContentSize);

                InputStream input = new BufferedInputStream(url.openStream());

                File destf;

                if(!isPDF){
                    destf = new File(currentDirectory, String.format(Constants.LANGUAGE_CODE, lang));
                }else{
                    destf = new File(currentDirectory, "pdf.ttf");
                }
                destf.createNewFile();
                Timber.d("File location is %s", destf.getAbsolutePath());
                OutputStream output = new FileOutputStream(destf);

                byte[] data = new byte[1024 * 6];
                int count, downloaded = 0;
                while ((count = input.read(data)) != -1) {
                    output.write(data, 0, count);
                    downloaded += count;
                    int percentage = (downloaded * 100) / totalContentSize;
                    publishProgress(percentage);
                }
                output.flush();
                output.close();
                input.close();
            } catch (Exception e) {
                result = false;
                Timber.e("failed to download " + downloadURL + " : " + e);
                e.printStackTrace();
                crashUtils.logException(e);
            }
            return result;
        }


    }

    public void createPdf(ArrayList<File> files, OnOcrPdfGeneratorResult onOcrPdfGeneratorResult1){
        OCRInput ocrInput = new OCRInput();
        ocrInput.imageFiles = files;
        ocrInput.onOcrPdfGeneratorResult = onOcrPdfGeneratorResult1;
        CreateOCRPdf createOCRPdf = new CreateOCRPdf();
        createOCRPdf.execute(ocrInput);
    }

    private class OCRInput{
        public ArrayList<File> imageFiles = new ArrayList<>();
        public OnOcrPdfGeneratorResult onOcrPdfGeneratorResult;
    }

    public interface OnOcrPdfGeneratorResult{
        void onStarted();
        void onSuccess(int accuracy);
        void onFailure();
    }

    private class CreateOCRPdf extends AsyncTask<OCRInput, Void, Boolean> {

        OCRInput ocrInput;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            //ocrInput.onOcrPdfGeneratorResult.onStarted();
        }

        @Override
        protected void onPostExecute(Boolean aBoolean) {
            super.onPostExecute(aBoolean);
            if(aBoolean){
                ((AppCompatActivity)context).runOnUiThread(() -> ocrInput.onOcrPdfGeneratorResult.onSuccess(mImageTextReader.getAccuracy()));
            }else{
                ((AppCompatActivity)context).runOnUiThread(() -> ocrInput.onOcrPdfGeneratorResult.onFailure());
            }
        }

        @Override
        protected final Boolean doInBackground(OCRInput... inputs) {
            ocrInput = inputs[0];
            ((AppCompatActivity)context).runOnUiThread(() -> ocrInput.onOcrPdfGeneratorResult.onStarted());
            return mImageTextReader.createPDF(ocrInput.imageFiles);
        }
    }
}
