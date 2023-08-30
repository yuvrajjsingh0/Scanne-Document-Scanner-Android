package com.documentscanner.pdfscanner365.fragment;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import timber.log.Timber;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.documentscanner.pdfscanner365.R;
import com.documentscanner.pdfscanner365.utils.gdrive.DriveServiceHelper;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.drive.Drive;
import com.google.android.gms.tasks.Task;

import static com.documentscanner.pdfscanner365.utils.gdrive.DriveServiceHelper.getGoogleDriveService;

public class BackupLoginFragment extends Fragment {

    private static final int REQUEST_CODE_SIGN_IN = 100;
    private GoogleSignInClient mGoogleSignInClient;
    private DriveServiceHelper mDriveServiceHelper;

    public BackupLoginFragment() {
        // Required empty public constructor
    }

    public static BackupLoginFragment newInstance() {
        BackupLoginFragment fragment = new BackupLoginFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_backup_login, container, false);

        view.findViewById(R.id.signInButton).setOnClickListener((v) -> signIn());

        return view;
    }

    private void signIn() {

        mGoogleSignInClient = buildGoogleSignInClient();
        startActivityForResult(mGoogleSignInClient.getSignInIntent(), REQUEST_CODE_SIGN_IN);
    }

    private GoogleSignInClient buildGoogleSignInClient() {
        GoogleSignInOptions signInOptions =
                new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                        .requestScopes(Drive.SCOPE_FILE)
                        .requestEmail()
                        .build();
        return GoogleSignIn.getClient(getActivity().getApplicationContext(), signInOptions);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent resultData) {
        if (requestCode == REQUEST_CODE_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(resultData);
            handleSignInResult(task);
            Timber.d("onActivityResult");
        }

        super.onActivityResult(requestCode, resultCode, resultData);
    }

    private void handleSignInResult(Task<GoogleSignInAccount> completedTask) {
        try {
            GoogleSignInAccount account = completedTask.getResult(ApiException.class);
            // Signed in successfully, show authenticated UI.
            mDriveServiceHelper = new DriveServiceHelper(getGoogleDriveService(getActivity().getApplicationContext(), account, getString(R.string.app_name)));
            Timber.d("handleSignInResult: %s", mDriveServiceHelper);
            FragmentManager fragmentManager = getActivity().getSupportFragmentManager();
            fragmentManager.findFragmentByTag("loginfragment");
            FragmentTransaction ft = fragmentManager.beginTransaction();
            ft.remove(this);
            ft.add(R.id.container, BackupFragment.newInstance(mDriveServiceHelper), "backupfragment");
            ft.setCustomAnimations(R.anim.slide_in, R.anim.fade_out, R.anim.fade_in, R.anim.slide_out);
            ft.commit();
        } catch (ApiException e) {
            // The ApiException status code indicates the detailed failure reason.
            // Please refer to the GoogleSignInStatusCodes class reference for more information.
            Timber.w("signInResult:failed code=%s", e.getStatusCode());
        }
    }
}