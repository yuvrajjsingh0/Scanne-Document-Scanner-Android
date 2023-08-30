package com.documentscanner.pdfscanner365;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.widget.Toast;

import com.documentscanner.pdfscanner365.activity.HomeActivity;
import com.documentscanner.pdfscanner365.tesseract.Constants;
import com.documentscanner.pdfscanner365.tesseract.CrashUtils;
import com.documentscanner.pdfscanner365.tesseract.Tess;
import com.documentscanner.pdfscanner365.tesseract.Utils;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import androidx.core.app.NotificationCompat;
import androidx.preference.PreferenceManager;
import timber.log.Timber;

public class LanguageDownloaderService extends Service {

    public static final String CHANNEL_ID = "LanguageDownloaderServiceChannel";
    private final String TAG = "LanguageDownloaderServi";
    private final IBinder mBinder = new LocalBinder();

    private CrashUtils crashUtils;
    private File currentDirectory;
    private String size;
    private String[] languages;
    private int failedCount = 0;
    private int successCount = 0;

    private SharedPreferences sharedPrefs;
    private Set<String> selections;

    private StorageReference storageRef;

    public LanguageDownloaderService() {
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String input = intent.getStringExtra("numLangs");
        String languages = intent.getStringExtra("langs");
        createNotificationChannel();
        Intent notificationIntent = new Intent(this, HomeActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this,
                0, notificationIntent, 0);
        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(getString(R.string.downloading_lang))
                .setContentText(String.format(getString(R.string.total_lang_d_ct), input))
                .setOnlyAlertOnce(true)
                .setOngoing(true)
                .setProgress(100, 0, true)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentIntent(pendingIntent)
                .build();
        startForeground(1, notification);

        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        selections = sharedPrefs.getStringSet("ocr_lang", new HashSet<String>());

        crashUtils = new CrashUtils(getApplicationContext(), "");
        FirebaseStorage storage = FirebaseStorage.getInstance("gs://tapscanner-4c6ef.appspot.com");

        storageRef = storage.getReference();

        File dirBest = new File(getExternalFilesDir("best").getAbsolutePath());;
        currentDirectory = new File(dirBest, "tessdata");
        if(!currentDirectory.exists()){
            currentDirectory.mkdirs();
        }

        downloadLanguages(languages);
        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID,
                    "Foreground Service Channel",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(serviceChannel);
        }
    }

    private void downloadLanguages(String langs){
        languages = langs.split("\\+");
        for (String language : languages){
            downloadLanguage(language);
        }
    }

    private void downloadLanguage(String lang){
        String pdfEngine = Constants.TESSERACT_DATA_DOWNLOAD_PDF;

        String dataType = "best";

        if(!isLanguageDataExists(lang)){
            storageRef.child("/4.0/" + lang + ".traineddata").getDownloadUrl().addOnSuccessListener(uri -> {
                boolean result = true;
                String downloadURL = uri.toString();;
                String location;

                URL url, base, next;
                HttpURLConnection conn;
                try {
                    while (true) {
                        Timber.tag(TAG).v("downloading %s", downloadURL);
                        try {
                            url = new URL(downloadURL);
                        } catch (MalformedURLException ex) {
                            if(selections.contains(lang)){
                                selections.remove(lang);
                                sharedPrefs.edit().putStringSet("ocr_lang", selections).commit();
                            }
                            failedCount++;
                            if(failedCount+successCount == languages.length){
                                publishProgress(null, -10);
                                if(onDownloadListener != null){
                                    onDownloadListener.onDownloadComplete();
                                }
                                //stopSelf();
                            }
                            Timber.e("url " + downloadURL + " is bad: " + ex);
                            return;
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

                    destf = new File(currentDirectory, String.format(Constants.LANGUAGE_CODE, lang));
                    destf.createNewFile();
                    Timber.d("File location is %s", destf.getAbsolutePath());
                    OutputStream output = new FileOutputStream(destf);

                    byte[] data = new byte[1024 * 6];
                    int count, downloaded = 0;
                    while ((count = input.read(data)) != -1) {
                        output.write(data, 0, count);
                        downloaded += count;
                        int percentage = (downloaded * 100) / totalContentSize;
                        publishProgress(lang, percentage);
                    }
                    output.flush();
                    output.close();
                    input.close();
                    successCount++;
                    if(failedCount+successCount == languages.length){
                        publishProgress(null, -10);
                        if(onDownloadListener != null){
                            onDownloadListener.onDownloadComplete();
                        }
                        stopSelf();
                    }
                } catch (Exception e) {
                    failedCount++;
                    if(selections.contains(lang)){
                        selections.remove(lang);
                        sharedPrefs.edit().putStringSet("ocr_lang", selections).commit();
                    }
                    if(failedCount+successCount == languages.length){
                        publishProgress(null, -10);
                        if(onDownloadListener != null){
                            onDownloadListener.onDownloadComplete();
                        }
                        //stopSelf();
                    }else{
                        publishProgress(lang, -9);
                    }
                    result = false;
                    Timber.e("failed to download " + downloadURL + " : " + e);
                    e.printStackTrace();
                    crashUtils.logException(e);
                }

            }).addOnFailureListener(e -> {
                failedCount++;
                if(selections.contains(lang)){
                    selections.remove(lang);
                    sharedPrefs.edit().putStringSet("ocr_lang", selections).commit();
                }
                if(failedCount+successCount == languages.length){
                    publishProgress(null, -10);
                    if(onDownloadListener != null){
                        onDownloadListener.onDownloadComplete();
                    }
                    //stopSelf();
                }else{
                    publishProgress(lang, -9);
                }
            });
        }else{
            successCount++;
            Timber.d("downloaded %s", lang);
            Timber.d("downloaded ocr lang %s", sharedPrefs.getStringSet("ocr_lang", new HashSet<>()));
            if(failedCount+successCount == languages.length){
                publishProgress(null, -10);
                if(onDownloadListener != null){
                    onDownloadListener.onDownloadComplete();
                }
                //stopSelf();
            }
        }
    }

    private void publishProgress(String langName, int percentage){

        Intent notificationIntent = new Intent(this, HomeActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this,
                0, notificationIntent, 0);

        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentIntent(pendingIntent);
        if(percentage == -9){
            notificationBuilder.setPriority(NotificationCompat.PRIORITY_MAX);
            notificationBuilder.setContentTitle(getString(R.string.download_failed));
            notificationBuilder.setContentText(String.format(getString(R.string.download_failed_lang), langName));
        }else if(percentage == -10){
            if(onDownloadListener != null){
                onDownloadListener.onDownloadComplete();
            }
            notificationBuilder.setContentTitle(String.format(getString(R.string.downloaded_num_lang), String.valueOf(successCount)));
            notificationBuilder.setOngoing(false);
            notificationBuilder.setPriority(NotificationCompat.PRIORITY_MAX);
        }else{
            notificationBuilder.setContentTitle(String.format(getString(R.string.downloading_n), langName));
            notificationBuilder.setContentText(getString(R.string.please_wait));
            notificationBuilder.setProgress(100, percentage, true);
        }
        NotificationManager mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        mNotificationManager.notify(1, notificationBuilder.build());
    }

    private boolean isLanguageDataExists(final String lang) {
        File language = new File(currentDirectory, String.format(Constants.LANGUAGE_CODE, lang));
        return language.exists();
    }

    public class LocalBinder extends Binder {
        public LanguageDownloaderService getService() {
            // Return this instance of LocalService so clients can call public methods
            return LanguageDownloaderService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    public OnDownloadListener onDownloadListener = null;

    public interface OnDownloadListener{
        void onDownloadComplete();
    }

}