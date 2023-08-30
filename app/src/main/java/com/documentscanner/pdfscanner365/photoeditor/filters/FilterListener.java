package com.documentscanner.pdfscanner365.photoeditor.filters;

import com.documentscanner.pdfscanner365.photoeditor.core.PhotoFilter;

public interface FilterListener {
    void onFilterSelected(PhotoFilter photoFilter);
}