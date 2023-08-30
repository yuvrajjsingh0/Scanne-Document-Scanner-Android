package com.documentscanner.pdfscanner365.utils;

import androidx.appcompat.app.AppCompatDelegate;

public class Themes {

    static String[] themes = new String[3];

    public static int getTheme(String newValue){
        themes[0] = "Default";
        themes[1] = "Light";
        themes[2] = "Dark";

        if(newValue.equals(themes[0])){
            return AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;
        }else if(newValue.equals(themes[1])){
            return AppCompatDelegate.MODE_NIGHT_NO;
        }else if(newValue.equals(themes[2])){
            return AppCompatDelegate.MODE_NIGHT_YES;
        }

        return 0;
    }
}
