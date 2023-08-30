package com.documentscanner.pdfscanner365.fragment;

import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.preference.ListPreference;

import androidx.preference.MultiSelectListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;
import timber.log.Timber;

import com.documentscanner.pdfscanner365.LanguageDownloaderService;
import com.documentscanner.pdfscanner365.R;
import com.documentscanner.pdfscanner365.activity.SettingsActivity;
import com.documentscanner.pdfscanner365.utils.TessLang;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import static android.content.Context.ACTIVITY_SERVICE;

public class SettingsFragment extends PreferenceFragmentCompat implements LanguageDownloaderService.OnDownloadListener {

    private SharedPreferences sharedPrefs;
    private Set<String> selections;

    LanguageDownloaderService languageDownloaderService = null;

    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            languageDownloaderService = ((LanguageDownloaderService.LocalBinder) iBinder).getService();
            languageDownloaderService.onDownloadListener = SettingsFragment.this;
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            languageDownloaderService = null;
        }
    };

    MultiSelectListPreference langForOCR;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.pref_main, rootKey);

        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(getContext());

        ListPreference themeSel = findPreference("theme_sel");

        String[] themes = new String[3];
        themes[0] = "Default";
        themes[1] = "Light";
        themes[2] = "Dark";

        themeSel.setEntries(themes);
        themeSel.setEntryValues(themes);
        themeSel.setDefaultValue(themes[0]);

        themeSel.setOnPreferenceChangeListener((preference, newValue) -> {
            SettingsActivity activity = (SettingsActivity) getActivity();
            return activity.changeTheme((String) newValue);
        });

        langForOCR = findPreference("ocr_lang");

        String json = loadJSONFromAsset("langs");
        Gson gson = new Gson();
        Type type = new TypeToken<List<TessLang>>() {}.getType();

        List<TessLang> langList = gson.fromJson(json, type);

        String[] langsDisplay = new String[langList.size()];

        for(int i = 0; i < langList.size(); i++){
            langsDisplay[i] = langList.get(i).getEnglishName();
        }

        String[] langs = new String[langList.size()];

        for(int i = 0; i < langList.size(); i++){
            langs[i] = langList.get(i).getThreeLetterLang();
        }

        langForOCR.setEntries(langsDisplay);
        langForOCR.setEntryValues(langs);

        langForOCR.setOnPreferenceChangeListener((preference, newValue) -> {
            HashSet<String> set = (HashSet<String>) newValue;
            String[] arr = new String[set.size()];
            set.toArray(arr);

            StringBuilder selectedLangs = new StringBuilder();
            for(int i = 0; i < arr.length; i++){
                if(i == 0){
                    selectedLangs.append(arr[0]);
                }else{
                    selectedLangs.append("+" + arr[i]);
                }
            }
            String languagesToDownload = selectedLangs.toString();
            Timber.d(languagesToDownload);

            if(isServiceRunning(LanguageDownloaderService.class.getName())){
                getActivity().stopService(new Intent(getActivity(), LanguageDownloaderService.class));
            }
            Intent serviceIntent = new Intent(getActivity(), LanguageDownloaderService.class);
            serviceIntent.putExtra("numLangs", "" + arr.length);
            serviceIntent.putExtra("langs", languagesToDownload);
            getActivity().startService(serviceIntent);
            getActivity().bindService(serviceIntent, mConnection, Context.BIND_AUTO_CREATE);
            return true;
        });

        findPreference("about_us").setOnPreferenceClickListener(preference -> {
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://tapscanner-4c6ef.web.app/"));
            startActivity(browserIntent);
            return false;
        });

        findPreference("contact_us").setOnPreferenceClickListener(preference -> {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            Uri data = Uri.parse("mailto:yuvrajjsingh0@gmail.com?subject=" + getString(R.string.app_name) + "&body=");
            intent.setData(data);
            startActivity(intent);
            return false;
        });

        findPreference("feedback").setOnPreferenceClickListener(preference -> {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            Uri data = Uri.parse("mailto:yuvrajjsingh0@gmail.com?subject=" + "Feedback" + "&body=");
            intent.setData(data);
            startActivity(intent);
            return false;
        });

    }

    public String loadJSONFromAsset(String file) {
        String json;
        try {
            InputStream is = getActivity().getAssets().open(file + ".json");
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();
            json = new String(buffer, StandardCharsets.UTF_8);
        } catch (IOException ex) {
            ex.printStackTrace();
            return null;
        }
        return json;
    }

    private boolean isServiceRunning(String serviceName){
        boolean serviceRunning = false;
        ActivityManager am = (ActivityManager) getActivity().getSystemService(ACTIVITY_SERVICE);
        List<ActivityManager.RunningServiceInfo> l = am.getRunningServices(50);
        for (ActivityManager.RunningServiceInfo runningServiceInfo : l) {
            if (runningServiceInfo.service.getClassName().equals(serviceName)) {
                serviceRunning = true;

                if (runningServiceInfo.foreground) {
                    //service run in foreground
                }
            }
        }
        return serviceRunning;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if(languageDownloaderService != null){
            languageDownloaderService.onDownloadListener = null;
            getActivity().unbindService(mConnection);
        }
    }

    @Override
    public void onDownloadComplete() {
        langForOCR.setValues(sharedPrefs.getStringSet("ocr_lang", new HashSet<>()));
        Toast.makeText(getContext(), getString(R.string.lang_downloaded), Toast.LENGTH_LONG).show();
        getActivity().stopService(new Intent(getActivity(), LanguageDownloaderService.class));
    }
}
