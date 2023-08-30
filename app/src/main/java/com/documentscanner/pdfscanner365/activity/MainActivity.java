package com.documentscanner.pdfscanner365.activity;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.IBinder;
import android.os.PersistableBundle;

import com.documentscanner.pdfscanner365.LanguageDownloaderService;

import java.util.HashSet;
import java.util.Set;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

public abstract class MainActivity extends AppCompatActivity {

    private SharedPreferences sharedPrefs;
    private Set<String> selections;
    LanguageDownloaderService languageDownloaderService = null;

    private final ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            languageDownloaderService = ((LanguageDownloaderService.LocalBinder) iBinder).getService();
            // now you have the instance of service.
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            selections = sharedPrefs.getStringSet("ocr_lang", new HashSet<String>());
            languageDownloaderService = null;
        }
    };

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState, @Nullable PersistableBundle persistentState) {
        super.onCreate(savedInstanceState, persistentState);
        bindService(new Intent(this, LanguageDownloaderService.class), mConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (languageDownloaderService != null) {
            unbindService(mConnection);
        }
    }
}
