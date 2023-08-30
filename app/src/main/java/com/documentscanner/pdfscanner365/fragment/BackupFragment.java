package com.documentscanner.pdfscanner365.fragment;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import timber.log.Timber;

import android.os.IBinder;
import android.os.Parcelable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.documentscanner.pdfscanner365.R;
import com.documentscanner.pdfscanner365.interfaces.SyncListener;
import com.documentscanner.pdfscanner365.service.SyncService;
import com.documentscanner.pdfscanner365.utils.gdrive.DriveServiceHelper;
import com.documentscanner.pdfscanner365.utils.gdrive.GoogleDriveFileHolder;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.gson.Gson;

import org.jetbrains.annotations.NotNull;

import static com.documentscanner.pdfscanner365.utils.gdrive.DriveServiceHelper.getGoogleDriveService;

public class BackupFragment extends Fragment {

    DriveServiceHelper mDriveServiceHelper;
    SyncService service;

    View rootView;

    private boolean mBound = false;

    private ServiceConnection connection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder service1) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            SyncService.SyncServiceBinder binder = (SyncService.SyncServiceBinder) service1;
            service = binder.getService();
            mBound = true;
            service.setListener(() -> {
                service.stopSelf();

                rootView.findViewById(R.id.startSync).setEnabled(true);
            });
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            mBound = false;
        }
    };

    public BackupFragment() {
        // Required empty public constructor
    }

    public static BackupFragment newInstance(DriveServiceHelper driveServiceHelper){
        return new BackupFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(getActivity().getApplicationContext());
        mDriveServiceHelper = new DriveServiceHelper(getGoogleDriveService(getActivity().getApplicationContext(), account, getString(R.string.app_name)));
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_backup, container, false);
        rootView = view;

        service = SyncService.getInstance();
        if(service != null){
            view.findViewById(R.id.startSync).setEnabled(false);
            service.setListener(() -> view.findViewById(R.id.startSync).setEnabled(true));
        }

        view.findViewById(R.id.startSync).setOnClickListener(v -> {
            Intent intent = new Intent(getContext(), SyncService.class);
            intent.putExtra("backup", true);
            getContext().startService(intent);
            service = SyncService.getInstance();
            view.findViewById(R.id.startSync).setEnabled(false);
            if(service != null){

                view.findViewById(R.id.startSync).setEnabled(false);
                service.setListener(() -> {

                    Intent intent1 = new Intent(getContext(), SyncService.class);
                    intent1.setAction("STOP");
                    getContext().startService(intent1);

                    view.findViewById(R.id.startSync).setEnabled(true);
                });
            }
            getContext().bindService(intent, connection, Context.BIND_AUTO_CREATE);
        });
        
        view.findViewById(R.id.startRestore).setOnClickListener(v -> {
            // Start restoring of ur shit
            Intent intent = new Intent(getContext(), SyncService.class);
            intent.putExtra("backup", false);
            getContext().startService(intent);
            getContext().bindService(intent, connection, Context.BIND_AUTO_CREATE);
        });

        return view;
    }

    @Override
    public void onCreateOptionsMenu(@NotNull Menu menu, MenuInflater inflater) {
        menu.clear();
        inflater.inflate(R.menu.menu_backup, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.logout) {
            signOut();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if(service != null && mBound){
            getContext().unbindService(connection);
            mBound = false;
        }
    }

    private void signOut() {
        GoogleSignInOptions gso = new GoogleSignInOptions.
                Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).
                build();

        GoogleSignInClient mGoogleSignInClient = GoogleSignIn.getClient(getContext(), gso);
        mGoogleSignInClient.signOut()
                .addOnCompleteListener(task -> {
                    Toast.makeText(getContext(), getString(R.string.logged_out_message), Toast.LENGTH_LONG).show();
                    FragmentManager fragmentManager = getActivity().getSupportFragmentManager();
                    fragmentManager.findFragmentByTag("backupfragment");
                    FragmentTransaction ft = fragmentManager.beginTransaction();
                    ft.remove(this);
                    ft.add(R.id.container, BackupLoginFragment.newInstance(), "loginfragment");
                    ft.setCustomAnimations(R.anim.slide_in, R.anim.fade_out, R.anim.fade_in, R.anim.slide_out);
                    ft.commit();
                });
    }
}