package com.documentscanner.pdfscanner365.activity;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import timber.log.Timber;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.SkuDetails;
import com.documentscanner.pdfscanner365.R;
import com.documentscanner.pdfscanner365.main.App;
import com.revenuecat.purchases.EntitlementInfo;
import com.revenuecat.purchases.Offering;
import com.revenuecat.purchases.Offerings;
import com.revenuecat.purchases.Package;
import com.revenuecat.purchases.CustomerInfo;
//import com.revenuecat.purchases.PurchaserInfo;
import com.revenuecat.purchases.Purchases;
import com.revenuecat.purchases.PurchasesError;
//import com.revenuecat.purchases.interfaces.MakePurchaseListener;
//import com.revenuecat.purchases.interfaces.ReceiveOfferingsListener;
//import com.revenuecat.purchases.interfaces.ReceivePurchaserInfoListener;
import com.revenuecat.purchases.interfaces.ReceiveCustomerInfoCallback;
import com.revenuecat.purchases.interfaces.PurchaseCallback;
import com.revenuecat.purchases.interfaces.ReceiveOfferingsCallback;
import com.revenuecat.purchases.models.StoreProduct;
import com.revenuecat.purchases.models.StoreTransaction;

public class UpgradePro extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_upgrade_pro);

        LinearLayout plansContainer = findViewById(R.id.plans_container);
        ProgressBar progressBar = findViewById(R.id.progress_bar);

        getSupportActionBar().hide();

        Button monthlyPurchaseView = findViewById(R.id.monthly);
        Button annualPurchaseView = findViewById(R.id.yearly);
        Button unlimitedPurchaseView = findViewById(R.id.lifetime);

        findViewById(R.id.dismiss).setOnClickListener((view) -> onBackPressed());

//        Purchases.getSharedInstance().restorePurchases(new ReceivePurchaserInfoListener() {
//            @Override
//            public void onReceived(@NonNull PurchaserInfo purchaserInfo) {
//                checkForProEntitlement(purchaserInfo);
//            }
//
//            @Override
//            public void onError(@NonNull PurchasesError error) {
//
//            }
//        });

        Purchases.getSharedInstance().restorePurchases(new ReceiveCustomerInfoCallback() {
            @Override
            public void onReceived(@NonNull CustomerInfo purchaserInfo) {
                checkForProEntitlement(purchaserInfo);
            }

            @Override
            public void onError(@NonNull PurchasesError error) {

            }
        });


//        Purchases.getSharedInstance().getOfferings(new ReceiveOfferingsListener() {
//
//            @Override
//            public void onReceived(@NonNull Offerings offerings) {
//                progressBar.setVisibility(View.GONE);
//                plansContainer.setVisibility(View.VISIBLE);
//
//                Offering currentOffering = offerings.getCurrent();
//                if (currentOffering != null) {
//                    setupPackageButton(currentOffering.getMonthly(), monthlyPurchaseView);
//                    setupPackageButton(currentOffering.getAnnual(), annualPurchaseView);
//                    setupPackageButton(currentOffering.getLifetime(), unlimitedPurchaseView);
//                } else {
//                    Timber.e("Error loading current offering");
//                }
//                Timber.d(offerings.toString());
//            }
//
//            @Override
//            public void onError(@NonNull PurchasesError error) {
//                progressBar.setVisibility(View.GONE);
//                Toast.makeText(UpgradePro.this, getString(R.string.error_occurred), Toast.LENGTH_LONG).show();
//            }
//        });
        Purchases.getSharedInstance().getOfferings(new ReceiveOfferingsCallback() {

            @Override
            public void onReceived(@NonNull Offerings offerings) {
                progressBar.setVisibility(View.GONE);
                plansContainer.setVisibility(View.VISIBLE);

                Offering currentOffering = offerings.getCurrent();
                if (currentOffering != null) {
                    setupPackageButton(currentOffering.getMonthly(), monthlyPurchaseView);
                    setupPackageButton(currentOffering.getAnnual(), annualPurchaseView);
                    setupPackageButton(currentOffering.getLifetime(), unlimitedPurchaseView);
                } else {
                    Timber.e("Error loading current offering");
                }
                Timber.d(offerings.toString());
            }

            @Override
            public void onError(@NonNull PurchasesError error) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(UpgradePro.this, getString(R.string.error_occurred), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void setupPackageButton(@Nullable final Package aPackage, final Button button) {
        if (aPackage != null) {
            //SkuDetails product = aPackage.getProduct();
            StoreProduct product = aPackage.getProduct();
            //product.getPrice().getCurrencyCode()
            //String loadedText = "Buy " + aPackage.getPackageType() + " - " + product.getPriceCurrencyCode() + " " + product.getPrice();
            String loadedText = "Buy " + aPackage.getPackageType() + " - " + product.getPrice().getCurrencyCode() + " " + product.getPrice().toString();
            button.setText(loadedText);
            //showLoading(button, false);
            button.setOnClickListener(v -> makePurchase(aPackage, button));
        } else {
            Timber.e("Error loading package");
        }
    }

    private void makePurchase(Package packageToPurchase, final Button button) {
        //showLoading(button, true);
//        Purchases.getSharedInstance().purchasePackage(this, packageToPurchase, new MakePurchaseListener() {
//            @Override
//            public void onCompleted(@NonNull Purchase purchase, @NonNull PurchaserInfo purchaserInfo) {
//                //showLoading(button, false);
//                checkForProEntitlement(purchaserInfo);
//            }
//
//            @Override
//            public void onError(@NonNull PurchasesError error, boolean userCancelled) {
//                if (!userCancelled) {
//                    Timber.e(error.getMessage());
//                }
//            }
//        });
        Purchases.getSharedInstance().purchasePackage(this, packageToPurchase, new PurchaseCallback() {
            @Override
            public void onCompleted(@NonNull StoreTransaction transaction, @NonNull CustomerInfo purchaserInfo) {
                //showLoading(button, false);
                checkForProEntitlement(purchaserInfo);
            }

            @Override
            public void onError(@NonNull PurchasesError error, boolean userCancelled) {
                if (!userCancelled) {
                    Timber.e(error.getMessage());
                }
            }
        });
    }

//    private void checkForProEntitlement(PurchaserInfo purchaserInfo) {
//        EntitlementInfo proEntitlement = purchaserInfo.getEntitlements().get("PRO");
//        if (proEntitlement != null && proEntitlement.isActive()) {
//            App.isAds = false;
//            getSharedPreferences(getPackageName(), MODE_PRIVATE).edit().putBoolean("pro", true).apply();
//            onBackPressed();
//        }
//    }

    private void checkForProEntitlement(CustomerInfo purchaserInfo) {
        EntitlementInfo proEntitlement = purchaserInfo.getEntitlements().get("PRO");
        if (proEntitlement != null && proEntitlement.isActive()) {
            App.isAds = false;
            getSharedPreferences(getPackageName(), MODE_PRIVATE).edit().putBoolean("pro", true).apply();
            onBackPressed();
        }
    }

    @Override
    public void onBackPressed() {
        startActivity(new Intent(this, HomeActivity.class));
        finish();
    }
}