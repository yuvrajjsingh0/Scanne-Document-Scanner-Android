package com.documentscanner.pdfscanner365.utils;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import androidx.appcompat.app.AlertDialog;

import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import timber.log.Timber;
import com.documentscanner.pdfscanner365.R;
import com.documentscanner.pdfscanner365.db.models.Note;

public class AppUtility {


    public static File getOutputMediaFile(String path, String name) {
        // To be safe, we should check that the SDCard is mounted
        if (!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            Timber.e("External storage %s", Environment.getExternalStorageState());
            return null;
        }

        /*if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ContentResolver resolver = context.getContentResolver();
            ContentValues contentValues = new ContentValues();
            contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, name);
            contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "image/jpg");
            contentValues.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOCUMENTS);
            Uri uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues);

            return new File(String.valueOf(uri));

        }else {
            File dir = new File(path);
            // Create the storage directory if it doesn't exist
            if (!dir.exists()) {
                if (!dir.mkdirs()) {
                    Timber.e("Failed to create directory");
                    return null;
                }
            }
            return new File(dir.getPath() + File.separator + name);
        }*/
        File dir = new File(path);
        // Create the storage directory if it doesn't exist
        if (!dir.exists()) {
            if (!dir.mkdirs()) {
                Timber.e("Failed to create directory");
                return null;
            }
        }
        return new File(dir.getPath() + File.separator + name);

    }

    public static void showErrorDialog(Context context, String errorMessage)
    {
        AlertDialog.Builder builder =
                new AlertDialog.Builder(context);
        builder.setTitle("Oops!");
        builder.setMessage(errorMessage);
        builder.setNeutralButton("OK", (dialog, which) -> dialog.dismiss());
        builder.show();
    }

    public static void shareDocuments(Context context, ArrayList<Uri> uris)
    {
        Intent shareIntent = new Intent();
        shareIntent.setAction(Intent.ACTION_SEND_MULTIPLE);
        //shareIntent.setAction(Intent.ACTION_SEND);
        // shareIntent.setType("image/jpeg");
        shareIntent.setType("image/*");
        shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        shareIntent.putExtra(Intent.EXTRA_STREAM, uris);
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, context.getString(R.string.app_name));

        context.startActivity(Intent.createChooser(shareIntent, context.getString(R.string.share_notes)));
    }

    public static void shareDocument(Context context, Uri uri)
    {
        Intent shareIntent = new Intent();
        shareIntent.setAction(Intent.ACTION_SEND);
        shareIntent.setType("image/*");
        shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        shareIntent.putExtra(Intent.EXTRA_STREAM, uri);
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, context.getString(R.string.app_name));

        context.startActivity(Intent.createChooser(shareIntent, context.getString(R.string.share_notes)));
    }

    public static void askAlertDialog(Context context,String title,String message, DialogInterface.OnClickListener positiveListener, DialogInterface.OnClickListener negativeListener)
    {
        new AlertDialog.Builder(context, R.style.AlertDialog)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton(context.getString(R.string.yes), positiveListener)
                .setNegativeButton(context.getString(R.string.no), negativeListener)
                .show();
    }

    public static void rateOnPlayStore(Context context)
    {
        Uri uri = Uri.parse("market://details?id=" + context.getPackageName());
        Intent myAppLinkToMarket = new Intent(Intent.ACTION_VIEW, uri);
        try {
            context.startActivity(myAppLinkToMarket);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(context, " Unable to find Play Store", Toast.LENGTH_LONG).show();
        }
    }

    public static ArrayList<Uri> getUrisFromNotes(List<Note> notes)
    {
        ArrayList<Uri> uris = new ArrayList<>();
        for (int index = 0; index < notes.size(); index++) {
            uris.add(notes.get(index).getImagePath());
        }

        return uris;
    }
}
