package com.documentscanner.pdfscanner365.utils;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;
import android.util.Log;

import com.documentscanner.pdfscanner365.R;
import com.documentscanner.pdfscanner365.main.App;
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.appopen.AppOpenAd;

import org.jetbrains.annotations.NotNull;

import java.util.Date;

import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.OnLifecycleEvent;
import androidx.lifecycle.ProcessLifecycleOwner;
import timber.log.Timber;

import static androidx.lifecycle.Lifecycle.Event.ON_START;

public class AppOpenManager  implements LifecycleObserver, Application.ActivityLifecycleCallbacks{
    private static final String LOG_TAG = "AppOpenManager";
    private static String AD_UNIT_ID;
    private AppOpenAd appOpenAd = null;

    private Activity currentActivity;

    private AppOpenAd.AppOpenAdLoadCallback loadCallback;

    private final App myApplication;

    private static boolean isShowingAd = false;

    private long loadTime = 0;

    /** Constructor */
    public AppOpenManager(App myApplication) {
        this.myApplication = myApplication;
        AD_UNIT_ID = myApplication.getString(R.string.admob_app_open_id);
        this.myApplication.registerActivityLifecycleCallbacks(this);
        ProcessLifecycleOwner.get().getLifecycle().addObserver(this);
    }

    /** Request an ad */
    public void fetchAd() {
        if (isAdAvailable()) {
            return;
        }

        loadCallback =
                new AppOpenAd.AppOpenAdLoadCallback() {
                    /**
                     * Called when an app open ad has loaded.
                     *
                     * @param ad the loaded app open ad.
                     */
                    @Override
                    public void onAppOpenAdLoaded(AppOpenAd ad) {
                        AppOpenManager.this.appOpenAd = ad;
                        AppOpenManager.this.loadTime = (new Date()).getTime();
                    }

                    /**
                     * Called when an app open ad has failed to load.
                     *
                     * @param loadAdError the error.
                     */
                    @Override
                    public void onAppOpenAdFailedToLoad(LoadAdError loadAdError) {
                        // Handle the error.
                    }

                };
        AdRequest request = getAdRequest();
        AppOpenAd.load(
                myApplication, AD_UNIT_ID, request,
                AppOpenAd.APP_OPEN_AD_ORIENTATION_PORTRAIT, loadCallback);
    }

    /** Creates and returns ad request. */
    private AdRequest getAdRequest() {
        return new AdRequest.Builder().build();
    }

    /** Utility method that checks if ad exists and can be shown. */
    public boolean isAdAvailable() {
        return appOpenAd != null && wasLoadTimeLessThanNHoursAgo(4);
    }

    private boolean wasLoadTimeLessThanNHoursAgo(long numHours) {
        long dateDifference = (new Date()).getTime() - this.loadTime;
        long numMilliSecondsPerHour = 3600000;
        return (dateDifference < (numMilliSecondsPerHour * numHours));
    }

    /** Shows the ad if one isn't already showing. */
    public void showAdIfAvailable() {
        // Only show ad if there is not already an app open ad currently showing
        // and an ad is available.
        if (!isShowingAd && isAdAvailable()) {
            Timber.d("Will show ad.");

            FullScreenContentCallback fullScreenContentCallback =
                    new FullScreenContentCallback() {
                        @Override
                        public void onAdDismissedFullScreenContent() {
                            // Set the reference to null so isAdAvailable() returns false.
                            AppOpenManager.this.appOpenAd = null;
                            isShowingAd = false;
                            fetchAd();
                        }

                        @Override
                        public void onAdFailedToShowFullScreenContent(AdError adError) {}

                        @Override
                        public void onAdShowedFullScreenContent() {
                            isShowingAd = true;
                        }
                    };

            if(currentActivity != null && currentActivity.getCallingActivity() == null){
                appOpenAd.show(currentActivity, fullScreenContentCallback);
            }

        } else {
            Timber.d("Can not show ad.");
            fetchAd();
        }
    }

    @OnLifecycleEvent(ON_START)
    public void onStart() {
        showAdIfAvailable();
        Timber.d("onStart");
    }

    /** ActivityLifecycleCallback methods */
    @Override
    public void onActivityCreated(@NotNull Activity activity, Bundle savedInstanceState) {}

    @Override
    public void onActivityStarted(@NotNull Activity activity) {
        currentActivity = activity;
    }

    @Override
    public void onActivityResumed(@NotNull Activity activity) {
        currentActivity = activity;
    }

    @Override
    public void onActivityStopped(@NotNull Activity activity) {}

    @Override
    public void onActivityPaused(@NotNull Activity activity) {}

    @Override
    public void onActivitySaveInstanceState(@NotNull Activity activity, @NotNull Bundle bundle) {}

    @Override
    public void onActivityDestroyed(@NotNull Activity activity) {
        currentActivity = null;
    }

}