package com.documentscanner.pdfscanner365.manager;

import java.util.Observer;

/**
 * Created by droidNinja on 28/03/16.
 */
public interface NotificationObserver extends Observer {
    void registerNotifications();
    void deRegisterNotifications();
}