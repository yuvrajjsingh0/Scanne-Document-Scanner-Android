package com.documentscanner.pdfscanner365.activity;

import androidx.fragment.app.FragmentManager;
import android.os.Bundle;
import android.widget.Toast;

import com.documentscanner.pdfscanner365.R;
import com.documentscanner.pdfscanner365.fragment.BackupFragment;
import com.documentscanner.pdfscanner365.fragment.BackupLoginFragment;
import com.documentscanner.pdfscanner365.utils.gdrive.DriveServiceHelper;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentTransaction;

import static com.documentscanner.pdfscanner365.utils.gdrive.DriveServiceHelper.getGoogleDriveService;

public class SyncActivity extends AppCompatActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sync);

        FragmentManager fm = getSupportFragmentManager();
        // add
        FragmentTransaction ft = fm.beginTransaction();

        GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(getApplicationContext());
        if (account != null) {
            //email.setText(account.getEmail());
            Toast.makeText(this, String.format(getString(R.string.already_logged_message), account.getDisplayName()), Toast.LENGTH_LONG).show();
            DriveServiceHelper mDriveServiceHelper = new DriveServiceHelper(getGoogleDriveService(getApplicationContext(), account, getString(R.string.app_name)));

            BackupFragment backupFragment = BackupFragment.newInstance(mDriveServiceHelper);
            ft.add(R.id.container, backupFragment, "backupfragment");

        }else{
            BackupLoginFragment backupLoginFragment = BackupLoginFragment.newInstance();
            ft.add(R.id.container, backupLoginFragment, "loginfragment");
        }
        ft.commit();

    }
}