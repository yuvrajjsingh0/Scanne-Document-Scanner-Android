package com.documentscanner.pdfscanner365.main;

import android.content.Context;
import com.documentscanner.pdfscanner365.R;
import com.documentscanner.pdfscanner365.utils.AppOpenManager;
import com.google.android.gms.ads.MobileAds;
import com.raizlabs.android.dbflow.config.FlowConfig;
import com.raizlabs.android.dbflow.config.FlowManager;
import com.revenuecat.purchases.EntitlementInfo;
import com.revenuecat.purchases.PurchaserInfo;
import com.revenuecat.purchases.Purchases;
import com.revenuecat.purchases.PurchasesError;
import com.revenuecat.purchases.interfaces.ReceivePurchaserInfoListener;

import androidx.annotation.NonNull;
import androidx.lifecycle.ProcessLifecycleOwner;
import androidx.multidex.MultiDexApplication;
import timber.log.Timber;

public class App extends MultiDexApplication {

    public static Context context;
    public static boolean isAds = true;

    private static App sPhotoApp;

    private static AppOpenManager appOpenManager;

    @Override
    public void onCreate() {
        super.onCreate();

        FlowManager.init(new FlowConfig.Builder(this).build());

        sPhotoApp = this;

        Purchases.setDebugLogsEnabled(true);
        Purchases.configure(this, getString(R.string.revenue_cat_sdk_key));

        if(getSharedPreferences(getPackageName(), MODE_PRIVATE).getBoolean("pro", false)){
            isAds = false;
            if(appOpenManager != null){
                ProcessLifecycleOwner.get().getLifecycle().removeObserver(appOpenManager);
            }
        }

        Purchases.getSharedInstance().restorePurchases(new ReceivePurchaserInfoListener() {
            @Override
            public void onReceived(@NonNull PurchaserInfo purchaserInfo) {
                checkForProEntitlement(purchaserInfo);
            }

            @Override
            public void onError(@NonNull PurchasesError error) {
                Timber.e(error.getMessage());
                Timber.i("Subscription is invalid");
                isAds = true;
                getSharedPreferences(getPackageName(), MODE_PRIVATE).edit().putBoolean("pro", false).apply();
            }
        });

        MobileAds.initialize(this);
        ManagerInitializer.i.init(getApplicationContext());

        if(isAds){
            appOpenManager = new AppOpenManager(this);
        }

        context = getApplicationContext();

    }

    public void checkForProEntitlement(PurchaserInfo purchaserInfo) {
        EntitlementInfo proEntitlement = purchaserInfo.getEntitlements().get("PRO");
        if (proEntitlement != null && proEntitlement.isActive()) {
            Timber.i("Subscription is valid");
            App.isAds = false;
            if(appOpenManager != null){
                ProcessLifecycleOwner.get().getLifecycle().removeObserver(appOpenManager);
            }
            getSharedPreferences(getPackageName(), MODE_PRIVATE).edit().putBoolean("pro", true).apply();
        }else{
            Timber.i("Subscription is invalid");
            isAds = true;
            getSharedPreferences(getPackageName(), MODE_PRIVATE).edit().putBoolean("pro", false).apply();
        }
    }

    public Context getContext() {
        return sPhotoApp.getContext();
    }
}
