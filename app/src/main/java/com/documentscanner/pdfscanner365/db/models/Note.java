package com.documentscanner.pdfscanner365.db.models;

import android.net.Uri;

import com.raizlabs.android.dbflow.annotation.Column;
import com.raizlabs.android.dbflow.annotation.ForeignKey;
import com.raizlabs.android.dbflow.annotation.ForeignKeyAction;
import com.raizlabs.android.dbflow.annotation.ForeignKeyReference;
import com.raizlabs.android.dbflow.annotation.PrimaryKey;
import com.raizlabs.android.dbflow.annotation.Table;
import com.raizlabs.android.dbflow.structure.BaseModel;

import org.parceler.Parcel;
import org.parceler.Transient;

import java.io.File;
import java.util.Date;

import androidx.core.content.FileProvider;
import com.documentscanner.pdfscanner365.BuildConfig;
import com.documentscanner.pdfscanner365.db.PDFScannerDatabase;
import com.documentscanner.pdfscanner365.main.App;
import com.documentscanner.pdfscanner365.main.Const;

/**
 * Created by droidNinja on 19/04/16.
 */
@Table(database = PDFScannerDatabase.class)
@Parcel(analyze={Note.class})
public class Note extends BaseModel{

    @Column
    @PrimaryKey(autoincrement = true)
    public int id;

    @Column
    public String name;

    @Column
    public Date createdAt;

    @Column
    @ForeignKey(references = {@ForeignKeyReference(columnName = "noteGroupId",  foreignKeyColumnName = "id")}, saveForeignKeyModel = false, onDelete = ForeignKeyAction.CASCADE, onUpdate = ForeignKeyAction.CASCADE)
    @Transient
    NoteGroup noteGroupForeignKeyContainer;

    public void associateNoteGroup(NoteGroup noteGroup) {
        noteGroupForeignKeyContainer = noteGroup;
    }

    public Uri getImagePath()
    {
        File newFile = new File(Const.FOLDERS.CROP_IMAGE_PATH + File.separator + name);
        return FileProvider.getUriForFile(App.context, BuildConfig.APPLICATION_ID + ".provider", newFile);
    }
}