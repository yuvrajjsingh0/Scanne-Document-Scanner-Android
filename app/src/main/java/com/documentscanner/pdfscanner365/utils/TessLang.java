package com.documentscanner.pdfscanner365.utils;

import com.google.gson.annotations.SerializedName;

public class TessLang {

    @SerializedName("alpha3-b")
    private String threeLetterLang;

    @SerializedName("alpha2")
    private String twoLetterLang;

    @SerializedName("English")
    private String englishName;

    public String getThreeLetterLang() {
        return threeLetterLang;
    }

    public void setThreeLetterLang(String threeLetterLang) {
        this.threeLetterLang = threeLetterLang;
    }

    public String getTwoLetterLang() {
        return twoLetterLang;
    }

    public void setTwoLetterLang(String twoLetterLang) {
        this.twoLetterLang = twoLetterLang;
    }

    public String getEnglishName() {
        return englishName;
    }

    public void setEnglishName(String englishName) {
        this.englishName = englishName;
    }
}
