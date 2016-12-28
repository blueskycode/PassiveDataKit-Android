package com.audacious_software.passive_data_kit.generators;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.audacious_software.passive_data_kit.activities.generators.DataPointViewHolder;
import com.audacious_software.pdk.passivedatakit.R;

import java.text.DateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

@SuppressWarnings("unused")
public abstract class Generator
{
    public static final String PDK_METADATA = "passive-data-metadata";
    public static final java.lang.String IDENTIFIER = "generator-id";
    public static final String TIMESTAMP = "timestamp";
    public static final String GENERATOR = "generator";
    public static final String SOURCE = "source";
    public static final String MEDIA_ATTACHMENT_KEY = "attachment";
    public static final String MEDIA_CONTENT_TYPE_KEY = "attachment-type";
    public static final String MEDIA_ATTACHMENT_GUID_KEY = "attachment-guid";
    public static final String LATITUDE = "latitude";
    public static final String LONGITUDE = "longitude";

    private static final String TABLE_SQLITE_MASTER = "sqlite_master";

    private static final String TABLE_METADATA = "metadata";
    private static String TABLE_METADATA_LAST_UPDATED = "last_updated";
    private static String TABLE_METADATA_KEY = "key";
    private static String TABLE_METADATA_VALUE = "value";

    protected Context mContext = null;

    public Generator(Context context)
    {
        this.mContext = context.getApplicationContext();
    }

    public static void start(Context context) {
        // Do nothing - override in subclasses...
    }

    public static void stop(Context context) {
        // Do nothing - override in subclasses.
    }

    public static boolean isEnabled(Context context)
    {
        return false;
    }

    public static boolean isRunning(Context context)
    {
        return false;
    }

    public static long latestPointGenerated(Context context) {
        return 0;
    }

    public static View fetchView(ViewGroup parent) {
        return LayoutInflater.from(parent.getContext()).inflate(R.layout.card_generator_generic, parent, false);
    }

    public static void bindViewHolder(DataPointViewHolder holder) {
        Class currentClass = new Object() { }.getClass().getEnclosingClass();

        String identifier = currentClass.getCanonicalName();

        TextView generatorLabel = (TextView) holder.itemView.findViewById(R.id.label_generator);

        generatorLabel.setText(identifier);
    }

    public static String formatTimestamp(Context context, double timestamp) {
        timestamp *= 1000;

        Calendar tsCalendar = Calendar.getInstance();
        tsCalendar.setTimeInMillis((long) timestamp);

        Calendar now = Calendar.getInstance();

        Date tsDate = tsCalendar.getTime();

        String time = android.text.format.DateFormat.getTimeFormat(context).format(tsDate);

        if (tsCalendar.get(Calendar.DAY_OF_MONTH) == now.get(Calendar.DAY_OF_MONTH)) {
            return time;
        }

        String date = android.text.format.DateFormat.getMediumDateFormat(context).format(tsDate);

        return context.getString(R.string.format_full_timestamp_pdk, date, time);
    }

    public abstract List<Bundle> fetchPayloads();

    protected int getDatabaseVersion(SQLiteDatabase db) {
        String where = "type = ? AND name = ?";
        String[] args = { "table", Generator.TABLE_METADATA };

        Cursor c = db.query(Generator.TABLE_SQLITE_MASTER, null, where, args, null, null, null);

        if (c.getCount() > 0) {
            // Do nothing - table exists...
        } else {
            db.execSQL(this.mContext.getString(R.string.pdk_generator_create_version_table));
        }

        c.close();

        String versionWhere = Generator.TABLE_METADATA_KEY + " = ?";
        String[] versionArgs = { "version" };

        c = db.query(Generator.TABLE_METADATA, null, versionWhere, versionArgs, null, null, Generator.TABLE_METADATA_LAST_UPDATED + " DESC");

        int version = 0;

        if (c.moveToNext()) {
            version = Integer.parseInt(c.getString(c.getColumnIndex(Generator.TABLE_METADATA_VALUE)));
        }

        c.close();

        return version;
    }

    protected void setDatabaseVersion(SQLiteDatabase db, int newVersion) {
        boolean keyExists = false;

        String versionWhere = Generator.TABLE_METADATA_KEY + " = ?";
        String[] versionArgs = { "version" };

        Cursor c = db.query(Generator.TABLE_METADATA, null, versionWhere, versionArgs, null, null, Generator.TABLE_METADATA_LAST_UPDATED + " DESC");

        if (c.getCount() > 0) {
            keyExists = true;
        }

        c.close();

        ContentValues values = new ContentValues();
        values.put(Generator.TABLE_METADATA_KEY, "version");
        values.put(Generator.TABLE_METADATA_VALUE, "" + newVersion);
        values.put(Generator.TABLE_METADATA_LAST_UPDATED, System.currentTimeMillis());

        if (keyExists) {
            db.update(Generator.TABLE_METADATA, values, versionWhere, versionArgs);
        } else {
            db.insert(Generator.TABLE_METADATA, null, values);
        }
    }
}
