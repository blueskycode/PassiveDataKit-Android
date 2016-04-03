package com.audacious_software.passive_data_kit;

import android.content.Context;

/**
 * Created by cjkarr on 4/3/2016.
 */
public class Logger {
    private Context mContext = null;

    private static class LoggerHolder {
        public static Logger instance = new Logger();
    }

    public static Logger getInstance(Context context) {
        LoggerHolder.instance.setContext(context.getApplicationContext());

        return LoggerHolder.instance;
    }

    private void setContext(Context context) {
        this.mContext = context;
    }

    public void logThrowable(Throwable t) {
        t.printStackTrace();
    }
}
