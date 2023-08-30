package com.documentscanner.pdfscanner365.activity;

import androidx.appcompat.app.AppCompatActivity;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.util.SparseArray;
import android.widget.ImageView;
import android.widget.TextView;

import com.documentscanner.pdfscanner365.main.App;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.InterstitialAd;
import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.text.Text;
import com.google.android.gms.vision.text.TextBlock;
import com.google.android.gms.vision.text.TextRecognizer;

import timber.log.Timber;
import com.documentscanner.pdfscanner365.R;

public class OCRActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ocr);

        if(App.isAds){

            AdView mAdView = findViewById(R.id.adView);
            AdRequest adRequest = new AdRequest.Builder().build();
            mAdView.loadAd(adRequest);

            InterstitialAd mInterstitialAd = new InterstitialAd(this);
            mInterstitialAd.setAdUnitId(getString(R.string.admob_interestitial_id));
            mInterstitialAd.loadAd(new AdRequest.Builder().build());
            mInterstitialAd.setAdListener(new AdListener(){
                @Override
                public void onAdLoaded() {
                    super.onAdLoaded();
                    mInterstitialAd.show();
                }
            });

            if (mInterstitialAd.isLoaded()) {
                mInterstitialAd.show();
            } else {
                Timber.d("The interstitial wasn't loaded yet.");
            }
        }

        String imagePath = getIntent().getStringExtra("image");

        ImageView imageView = findViewById(R.id.img_container);
        TextView visionText = findViewById(R.id.vision_text);

        visionText.setMovementMethod(new ScrollingMovementMethod());

        //Picasso.with(this).load(Uri.parse(imagePath)).into(imageView);

        Bitmap bitmap = BitmapFactory.decodeFile(imagePath);
        imageView.setImageBitmap(bitmap);

        TextRecognizer txtRecognizer = new TextRecognizer.Builder(getApplicationContext()).build();
        if (!txtRecognizer.isOperational()) {
            // Shows if your Google Play services is not up to date or OCR is not supported for the device
            visionText.setText("Detector dependencies are not yet available");
        } else {
            // Set the bitmap taken to the frame to perform OCR Operations.
            Frame frame = new Frame.Builder().setBitmap(bitmap).build();
            SparseArray items = txtRecognizer.detect(frame);
            StringBuilder strBuilder = new StringBuilder();
            for (int i = 0; i < items.size(); i++) {
                TextBlock item = (TextBlock) items.valueAt(i);
                strBuilder.append(item.getValue());
                strBuilder.append("/");
                // The following Process is used to show how to use lines & elements as well
                for (int i1 = 0; i1 < items.size(); i1++) {
                    TextBlock item1 = (TextBlock) items.valueAt(i1);
                    strBuilder.append(item1.getValue());
                    strBuilder.append("/");
                    for (Text line : item1.getComponents()) {
                        //extract scanned text lines here
                        Log.v("lines", line.getValue());
                        for (Text element : line.getComponents()) {
                            //extract scanned text words here
                            Log.v("element", element.getValue());
                        }
                    }
                }
            }
            visionText.setText(strBuilder.toString());
        }
    }
}