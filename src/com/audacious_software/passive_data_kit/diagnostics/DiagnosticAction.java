package com.audacious_software.passive_data_kit.diagnostics;

import android.content.Context;

/**
 * Created by cjkarr on 4/3/2016.
 */
public class DiagnosticAction {
    private String mMessage = null;
    private Runnable mAction = null;

    public DiagnosticAction(String message, Runnable action) {
        this.mMessage = message;
        this.mAction = action;
    }

    public void run() {
        if (this.mAction != null) {
            Thread t = new Thread(this.mAction);

            t.start();
        }
    }

    public String getMessage() {
        return this.mMessage;
    }
}
