package com.documentscanner.pdfscanner365.presenters;

import android.app.Activity;
import android.content.DialogInterface;
import androidx.appcompat.app.AlertDialog;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import java.util.List;

import com.documentscanner.pdfscanner365.R;
import com.documentscanner.pdfscanner365.activity.callbacks.HomeView;
import com.documentscanner.pdfscanner365.db.DBManager;
import com.documentscanner.pdfscanner365.db.models.NoteGroup;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public class HomePresenter implements Presenter<HomeView> {

    private HomeView homeView;

    @Override
    public void attachView(HomeView view) {
        this.homeView = view;
    }

    @Override
    public void detachView() {
        this.homeView = null;
    }

    public void loadNoteGroups()
    {
        List<NoteGroup> noteGroups = DBManager.getInstance().getAllNoteGroups();

        if(noteGroups==null || noteGroups.size()==0)
            homeView.showEmptyMessage();
        else
            homeView.loadNoteGroups(noteGroups);
    }

    public void showRenameDialog(final NoteGroup noteGroup, String name)
    {
        final MaterialAlertDialogBuilder dialogBuilder = new MaterialAlertDialogBuilder(homeView.getContext(), R.style.Theme_MaterialComponents_DayNight_Dialog_Bridge);
        LayoutInflater inflater = ((Activity)homeView.getContext()).getLayoutInflater();
        final View dialogView = inflater.inflate(R.layout.rename_alert_layout, null);
        dialogBuilder.setView(dialogView);
        dialogBuilder.setTitle(homeView.getContext().getString(R.string.rename_doc));
        dialogBuilder.setPositiveButton(homeView.getContext().getString(R.string.done), null);
        dialogBuilder.setNegativeButton(homeView.getContext().getString(R.string.lbl_cancel), (dialog, which) -> dialog.dismiss());
        final EditText editText = (EditText) dialogView.findViewById(R.id.rename_et);

        editText.setText(name);
        final AlertDialog alertDialog = dialogBuilder.create();

        final View.OnClickListener myListener = v -> {
            String text = editText.getText().toString().trim();
            if(!TextUtils.isEmpty(text)) {
                DBManager.getInstance().updateNoteGroupName(noteGroup.id, text);
                loadNoteGroups();
                alertDialog.dismiss();
            }
            else
                editText.setError(homeView.getContext().getString(R.string.enter_valid_name));
        };



        alertDialog.setOnShowListener(dialog -> {
            Button b = alertDialog.getButton(DialogInterface.BUTTON_POSITIVE);
            b.setOnClickListener(myListener);
        });
        dialogBuilder.setNegativeButton(homeView.getContext().getString(R.string.lbl_cancel), (dialog, whichButton) -> dialog.dismiss());

        alertDialog.show();
    }
}
