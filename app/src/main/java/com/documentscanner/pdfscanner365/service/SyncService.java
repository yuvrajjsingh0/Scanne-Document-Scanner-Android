package com.documentscanner.pdfscanner365.service;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.text.TextUtils;
import android.widget.Toast;

import com.documentscanner.pdfscanner365.R;
import com.documentscanner.pdfscanner365.activity.SyncActivity;
import com.documentscanner.pdfscanner365.db.DBManager;
import com.documentscanner.pdfscanner365.db.PDFScannerDatabase;
import com.documentscanner.pdfscanner365.db.models.Note;
import com.documentscanner.pdfscanner365.db.models.NoteGroup;
import com.documentscanner.pdfscanner365.interfaces.SyncListener;
import com.documentscanner.pdfscanner365.main.Const;
import com.documentscanner.pdfscanner365.main.FileUtil;
import com.documentscanner.pdfscanner365.utils.gdrive.DriveServiceHelper;
import com.documentscanner.pdfscanner365.utils.gdrive.GoogleDriveFileHolder;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.gson.Gson;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import timber.log.Timber;

import static com.documentscanner.pdfscanner365.utils.gdrive.DriveServiceHelper.getGoogleDriveService;

public class SyncService extends Service {

    private Looper serviceLooper;
    private ServiceHandler serviceHandler;
    private String CHANNEL_ID = "Document Scanner-412";
    private int notificationId = 412;
    private SyncListener listener;

    private static SyncService instance;
    public static boolean isServiceRunning = false;

    private final IBinder binder = new SyncServiceBinder();

    @Override
    public void onCreate() {
        // Start up the thread running the service. Note that we create a
        // separate thread because the service normally runs in the process's
        // main thread, which we don't want to block. We also make it
        // background priority so CPU-intensive work doesn't disrupt our UI.
        HandlerThread thread = new HandlerThread("ServiceStartArguments", Process.THREAD_PRIORITY_BACKGROUND);
        thread.start();

        // Get the HandlerThread's Looper and use it for our Handler
        serviceLooper = thread.getLooper();
        serviceHandler = new ServiceHandler(serviceLooper);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        instance = this;
        Toast.makeText(this, getString(R.string.starting_sync), Toast.LENGTH_SHORT).show();

        if(intent != null && intent.getAction() != null){
            if(intent.getAction().equals("STOP")){
                stopSelf();
            }

        }

        isServiceRunning = true;

        // For each start request, send a message to start a job and deliver the
        // start ID so we know which request we're stopping when we finish the job
        Message msg = serviceHandler.obtainMessage();

        msg.arg1 = startId;

        if(intent != null){
            Bundle bundle = new Bundle();
            bundle.putBoolean("backup", intent.getBooleanExtra("backup", true));
            msg.setData(bundle);
        }

        serviceHandler.sendMessage(msg);

        // If we get killed, after returning from here, restart
        return START_REDELIVER_INTENT;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public void onDestroy() {
        isServiceRunning = false;
        Timber.d("Sync Service Destroyed");
    }

    public void setListener(SyncListener listener){
        this.listener = listener;
    }

    private final class ServiceHandler extends Handler {

        Message msg;

        public ServiceHandler(Looper looper) {
            super(looper);
        }
        @Override
        public void handleMessage(@NotNull Message msg) {
            this.msg = msg;
            boolean backup = true;

            if(msg.getData() != null){
                backup = msg.getData().getBoolean("backup", true);
                Timber.d("Backup %s", backup);
            }

            if(backup){
                backup();
            }else{
                restore();
            }

        }

        private void backup(){

            GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(getApplicationContext());

            if(account != null){
                DriveServiceHelper mDriveServiceHelper = new DriveServiceHelper(getGoogleDriveService(getApplicationContext(), account,
                        getString(R.string.app_name)));

                mDriveServiceHelper.createFolderIfNotExist("Document Scanner", null)
                        .addOnSuccessListener(googleDriveFileHolder -> {
                            Gson gson = new Gson();
                            Timber.d("onSuccess: %s", gson.toJson(googleDriveFileHolder));
                            backUp();
                            mDriveServiceHelper.createFolderIfNotExist("Scanned Files", googleDriveFileHolder.getId())
                                    .addOnSuccessListener(googleDriveFileHolder1 -> mDriveServiceHelper.queryFiles(googleDriveFileHolder1.getId()).addOnSuccessListener(googleDriveFileHolders -> {

                                        ArrayList<String> uploadedFiles = new ArrayList<>();

                                        for(int j = 0; j < googleDriveFileHolders.size(); j++){
                                            uploadedFiles.add(googleDriveFileHolders.get(j).getName());
                                        }

                                        Timber.d("onSuccess: %s", gson.toJson(googleDriveFileHolder1));
                                        boolean isLastNotebook = false;
                                        // Created
                                        List<NoteGroup> noteGroups = DBManager.getInstance().getAllNoteGroups();
                                        for(int i=0; i<noteGroups.size();i++){
                                            List<Note> notes = noteGroups.get(i).notes;
                                            if(i == noteGroups.size() - 1){
                                                isLastNotebook = true;
                                            }
                                            for(int i1 = 0; i1<notes.size(); i1++){
                                                Note note = notes.get(i1);
                                                boolean finalIsLastNotebook = isLastNotebook;
                                                int finalI = i1;
                                                try {
                                                    //onError();

                                                    File imageFile = new File(FileUtil.from(SyncService.this, note.getImagePath()).getPath());
                                                    if(!uploadedFiles.contains(imageFile.getName())){
                                                        mDriveServiceHelper.uploadFile(imageFile, DriveServiceHelper.TYPE_PHOTO, googleDriveFileHolder1.getId())
                                                                .addOnSuccessListener(googleDriveFileHolder2 -> {
                                                                    // Success have some shit in ur mouth
                                                                    Timber.d("onSuccess: %s", gson.toJson(googleDriveFileHolder2));
                                                                    Timber.d("Success");
                                                                    if(finalIsLastNotebook){
                                                                        if(finalI == notes.size() - 1){
                                                                            // Save Database
                                                                            mDriveServiceHelper.uploadFile(new File(getExternalFilesDir("database"), "taps-backup.db"), "application/vnd.sqlite3", googleDriveFileHolder1.getId()).addOnSuccessListener(googleDriveFileHolder3 -> {
                                                                                // Success have some shit in ur mouth
                                                                                Timber.d("onSuccess: %s", gson.toJson(googleDriveFileHolder));
                                                                                Timber.d("Success");
                                                                                if(finalIsLastNotebook){
                                                                                    if(finalI == notes.size() - 1){
                                                                                        // Save Database
                                                                                        onSuccess();
                                                                                    }
                                                                                }
                                                                            }).addOnFailureListener(Throwable::printStackTrace);
                                                                        }
                                                                    }
                                                                }).addOnFailureListener(Throwable::printStackTrace);
                                                    }else{
                                                        if(finalIsLastNotebook){
                                                            if(finalI == notes.size() - 1){
                                                                mDriveServiceHelper.uploadFile(new File(getExternalFilesDir("database"), "taps-backup.db"), "application/vnd.sqlite3", googleDriveFileHolder1.getId()).addOnSuccessListener(googleDriveFileHolder3 -> {
                                                                    // Success have some shit in ur mouth
                                                                    Timber.d("onSuccess: %s", gson.toJson(googleDriveFileHolder1));
                                                                    Timber.d("Success");
                                                                    Timber.d("onSuccess: %s", gson.toJson(googleDriveFileHolder));
                                                                    Timber.d("Success");
                                                                    if(finalIsLastNotebook){
                                                                        if(finalI == notes.size() - 1){
                                                                            // Save Database
                                                                            onSuccess();
                                                                        }
                                                                    }
                                                                }).addOnFailureListener(Throwable::printStackTrace);
                                                            }
                                                        }
                                                    }

                                                } catch (IOException e) {
                                                    e.printStackTrace(); }
                                            }
                                        }
                                    })).addOnFailureListener(e -> {
                                Timber.e(e);
                                onError();
                            });
                        })
                        .addOnFailureListener(e -> {
                            Timber.e(e);
                            onError();
                        });
            }else{
                if(listener!= null){
                    listener.onComplete();
                }
                stopSelf(msg.arg1);

            }
        }

        private void restore() {
            GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(getApplicationContext());

            if (account != null) {
                DriveServiceHelper mDriveServiceHelper = new DriveServiceHelper(getGoogleDriveService(getApplicationContext(), account,
                        getString(R.string.app_name)));
                mDriveServiceHelper.searchFolder("Scanned Files")
                        .addOnSuccessListener(googleDriveFileHolders -> {
                            Gson gson = new Gson();
                            Timber.d("onSuccess: %s", gson.toJson(googleDriveFileHolders));

                            if(googleDriveFileHolders.size() != 0){
                                GoogleDriveFileHolder folder = null;
                                for(int i = 0; i < googleDriveFileHolders.size(); i++){
                                    if(googleDriveFileHolders.get(i).getName().equals("Scanned Files")){
                                        folder = googleDriveFileHolders.get(i);
                                    }
                                }

                                if(folder != null){
                                    mDriveServiceHelper.queryFiles(folder.getId()).addOnSuccessListener(googleDriveFileHolders1 -> {
                                        for(int i = 0; i < googleDriveFileHolders1.size(); i++){
                                            //TODO: Setup restore logic (file download)
                                            mDriveServiceHelper.downloadFile(new File(Const.FOLDERS.CROP_IMAGE_PATH, googleDriveFileHolders1.get(i).getName()), googleDriveFileHolders1.get(i).getId())
                                                    .addOnSuccessListener(aVoid -> {
                                                        File[] files = (new File(Const.FOLDERS.CROP_IMAGE_PATH)).listFiles();
                                                        assert files != null;
                                                        List<File> filesList = Arrays.asList(files);

                                                    });
                                        }
                                    });
                                }
                            }else{
                                stopSelf(msg.arg1);
                            }
                        })
                        .addOnFailureListener(e -> {
                            Timber.d("onFailure: %s", e.getMessage());
                            stopSelf(msg.arg1);
                        });
            }else{
                stopSelf(msg.arg1);
            }
        }

        private void onError(){

            NotificationManager notificationManager = (NotificationManager) getSystemService(Service.NOTIFICATION_SERVICE);

            Intent intent = new Intent(SyncService.this, SyncActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            PendingIntent pendingIntent = PendingIntent.getActivity(SyncService.this, 0, intent, 0);


            NotificationCompat.Builder builder = new NotificationCompat.Builder(SyncService.this, CHANNEL_ID)
                    .setSmallIcon(R.mipmap.ic_launcher)
                    .setContentTitle(getString(R.string.error))
                    .setContentText(getString(R.string.sync_error))
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                    .setContentIntent(pendingIntent)
                    .setAutoCancel(true);

            createNotificationChannel();
            notificationManager.notify(notificationId, builder.build());
            stopSelf(msg.arg1);
        }

        private void onSuccess(){

            NotificationManager notificationManager = (NotificationManager) getSystemService(Service.NOTIFICATION_SERVICE);

            Intent intent = new Intent(SyncService.this, SyncActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            PendingIntent pendingIntent = PendingIntent.getActivity(SyncService.this, 0, intent, 0);

            NotificationCompat.Builder builder = new NotificationCompat.Builder(SyncService.this, CHANNEL_ID)
                    .setSmallIcon(R.mipmap.ic_launcher)
                    .setContentTitle(getString(R.string.success))
                    .setContentText(getString(R.string.sync_success))
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                    .setContentIntent(pendingIntent)
                    .setAutoCancel(true);

            createNotificationChannel();
            notificationManager.notify(notificationId, builder.build());

            if(listener != null){
                listener.onComplete();
            }
        }

        private void createNotificationChannel() {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                CharSequence name = CHANNEL_ID;
                String description = CHANNEL_ID;
                int importance = NotificationManager.IMPORTANCE_DEFAULT;
                NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
                channel.setDescription(description);
                NotificationManager notificationManager = getSystemService(NotificationManager.class);
                notificationManager.createNotificationChannel(channel);
            }
        }

        public boolean isNotificationChannelEnabled(Context context, @Nullable String channelId){
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if(!TextUtils.isEmpty(channelId)) {
                    NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
                    NotificationChannel channel = manager.getNotificationChannel(channelId);
                    return channel.getImportance() != NotificationManager.IMPORTANCE_NONE;
                }
                return false;
            } else {
                return NotificationManagerCompat.from(context).areNotificationsEnabled();
            }
        }
    }

    public void backUp() {
        try {
            File sd = getExternalFilesDir("database");
            File data = Environment.getDataDirectory();

            if (sd.canWrite()) {
                //String currentDBPath = getDatabasePath("PDFScannerDatabase");
                String backupDBPath = "taps-backup.db";

                //File currentDB = new File(data, currentDBPath);
                File backupDB = new File(sd, backupDBPath);

                Timber.d("%s", getDatabasePath(PDFScannerDatabase.NAME + ".db").getPath());

                if (getDatabasePath(PDFScannerDatabase.NAME + ".db").exists()) {
                    FileChannel src = new FileInputStream(getDatabasePath(PDFScannerDatabase.NAME + ".db")).getChannel();
                    FileChannel dst = new FileOutputStream(backupDB).getChannel();
                    dst.transferFrom(src, 0, src.size());
                    src.close();
                    dst.close();
                    //Toast.makeText(getApplicationContext(), "Backup is successful to SD card", Toast.LENGTH_SHORT).show();
                }else{
                    Timber.d("Database doesn't exist");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static SyncService getInstance(){
        return instance;
    }

    public class SyncServiceBinder extends Binder {
        public SyncService getService() {
            // Return this instance of LocalService so clients can call public methods
            return SyncService.this;
        }
    }
}
