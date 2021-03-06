package com.audacious_software.passive_data_kit.generators.diagnostics;

import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Build;
import android.os.Bundle;
import android.os.StatFs;
import android.preference.PreferenceManager;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.audacious_software.passive_data_kit.PassiveDataKit;
import com.audacious_software.passive_data_kit.activities.generators.DataPointViewHolder;
import com.audacious_software.passive_data_kit.activities.generators.GeneratorViewHolder;
import com.audacious_software.passive_data_kit.diagnostics.DiagnosticAction;
import com.audacious_software.passive_data_kit.generators.Generator;
import com.audacious_software.passive_data_kit.generators.Generators;
import com.audacious_software.pdk.passivedatakit.R;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.IAxisValueFormatter;

import java.io.File;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

@SuppressWarnings("SimplifiableIfStatement")
@SuppressLint("NewApi")
public class SystemStatus extends Generator {
    private static final String GENERATOR_IDENTIFIER = "pdk-system-status";

    private static final String ENABLED = "com.audacious_software.passive_data_kit.generators.diagnostics.SystemStatus.ENABLED";
    private static final boolean ENABLED_DEFAULT = true;

    private static final String DATA_RETENTION_PERIOD = "com.audacious_software.passive_data_kit.generators.diagnostics.SystemStatus.DATA_RETENTION_PERIOD";
    private static final long DATA_RETENTION_PERIOD_DEFAULT = (60L * 24L * 60L * 60L * 1000L);

    private static final String ACTION_HEARTBEAT = "com.audacious_software.passive_data_kit.generators.diagnostics.SystemStatus.ACTION_HEARTBEAT";

    private static final String DATABASE_PATH = "pdk-system-status.sqlite";
    private static final int DATABASE_VERSION = 1;

    private static final String TABLE_HISTORY = "history";

    private static final String HISTORY_OBSERVED = "observed";
    private static final String HISTORY_RUNTIME = "runtime";
    private static final String HISTORY_STORAGE_USED_APP = "storage_app";
    private static final String HISTORY_STORAGE_USED_OTHER = "storage_other";
    private static final String HISTORY_STORAGE_AVAILABLE = "storage_available";
    private static final String HISTORY_STORAGE_TOTAL = "storage_total";
    private static final String HISTORY_STORAGE_PATH = "storage_path";
    private static final double GIGABYTE = (1024 * 1024 * 1024);

    private static SystemStatus sInstance = null;

    private BroadcastReceiver mReceiver = null;

    private SQLiteDatabase mDatabase = null;

    private long mLastTimestamp = 0;
    private long mRefreshInterval = (5 * 60 * 1000);

    @SuppressWarnings("unused")
    public static String generatorIdentifier() {
        return SystemStatus.GENERATOR_IDENTIFIER;
    }

    @SuppressWarnings("WeakerAccess")
    public static SystemStatus getInstance(Context context) {
        if (SystemStatus.sInstance == null) {
            SystemStatus.sInstance = new SystemStatus(context.getApplicationContext());
        }

        return SystemStatus.sInstance;
    }

    @SuppressWarnings("WeakerAccess")
    public SystemStatus(Context context) {
        super(context);
    }

    @SuppressWarnings("unused")
    public static void start(final Context context) {
        SystemStatus.getInstance(context).startGenerator();
    }

    private void startGenerator() {
        final SystemStatus me = this;

        final long runtimeStart = System.currentTimeMillis();

        Generators.getInstance(this.mContext).registerCustomViewClass(SystemStatus.GENERATOR_IDENTIFIER, SystemStatus.class);

        File path = new File(PassiveDataKit.getGeneratorsStorage(this.mContext), SystemStatus.DATABASE_PATH);

        this.mDatabase = SQLiteDatabase.openOrCreateDatabase(path, null);

        int version = this.getDatabaseVersion(this.mDatabase);

        switch (version) {
            case 0:
                this.mDatabase.execSQL(this.mContext.getString(R.string.pdk_generator_diagnostics_system_status_create_history_table));
        }

        if (version != SystemStatus.DATABASE_VERSION) {
            this.setDatabaseVersion(this.mDatabase, SystemStatus.DATABASE_VERSION);
        }

        this.mReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(final Context context, Intent intent) {
                final long now = System.currentTimeMillis();

                Runnable r = new Runnable() {
                    @Override
                    public void run() {
                        File path = PassiveDataKit.getGeneratorsStorage(context);

                        me.mLastTimestamp = now;

                        PassiveDataKit.getInstance(context).start();

                        StatFs fsInfo = new StatFs(path.getAbsolutePath());

                        String storagePath = path.getAbsolutePath();

                        long bytesTotal = fsInfo.getTotalBytes();
                        long bytesAvailable = fsInfo.getBlockSizeLong() * fsInfo.getAvailableBlocksLong();

                        long bytesAppUsed = SystemStatus.getFileSize(context.getFilesDir());
                        bytesAppUsed += SystemStatus.getFileSize(context.getExternalFilesDir(null));
                        bytesAppUsed += SystemStatus.getFileSize(context.getCacheDir());
                        bytesAppUsed += SystemStatus.getFileSize(context.getExternalCacheDir());

                        long bytesOtherUsed = bytesTotal - bytesAvailable - bytesAppUsed;

                        ContentValues values = new ContentValues();
                        values.put(SystemStatus.HISTORY_OBSERVED, now);
                        values.put(SystemStatus.HISTORY_RUNTIME, now - runtimeStart);
                        values.put(SystemStatus.HISTORY_STORAGE_PATH, storagePath);
                        values.put(SystemStatus.HISTORY_STORAGE_TOTAL, bytesTotal);
                        values.put(SystemStatus.HISTORY_STORAGE_AVAILABLE, bytesAvailable);
                        values.put(SystemStatus.HISTORY_STORAGE_USED_APP, bytesAppUsed);
                        values.put(SystemStatus.HISTORY_STORAGE_USED_OTHER, bytesOtherUsed);

                        Bundle update = new Bundle();
                        update.putLong(SystemStatus.HISTORY_OBSERVED, now);
                        update.putLong(SystemStatus.HISTORY_RUNTIME, now - runtimeStart);
                        update.putString(SystemStatus.HISTORY_STORAGE_PATH, storagePath);
                        update.putLong(SystemStatus.HISTORY_STORAGE_TOTAL, bytesTotal);
                        update.putLong(SystemStatus.HISTORY_STORAGE_AVAILABLE, bytesAvailable);
                        update.putLong(SystemStatus.HISTORY_STORAGE_USED_APP, bytesAppUsed);
                        update.putLong(SystemStatus.HISTORY_STORAGE_USED_OTHER, bytesOtherUsed);

                        me.mDatabase.insert(SystemStatus.TABLE_HISTORY, null, values);

                        Generators.getInstance(context).notifyGeneratorUpdated(SystemStatus.GENERATOR_IDENTIFIER, update);                    }
                };

                Thread t = new Thread(r);
                t.start();

                AlarmManager alarms = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
                PendingIntent pi = PendingIntent.getBroadcast(context, 0, new Intent(SystemStatus.ACTION_HEARTBEAT), PendingIntent.FLAG_UPDATE_CURRENT);

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarms.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, now + me.mRefreshInterval, pi);
                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                    alarms.setExact(AlarmManager.RTC_WAKEUP, now + me.mRefreshInterval, pi);
                } else {
                    alarms.set(AlarmManager.RTC_WAKEUP, now + me.mRefreshInterval, pi);
                }
            }
        };

        this.mReceiver.onReceive(this.mContext, null);

        IntentFilter filter = new IntentFilter(SystemStatus.ACTION_HEARTBEAT);
        this.mContext.registerReceiver(this.mReceiver, filter);

        this.flushCachedData();
    }

    @SuppressWarnings("unused")
    public static boolean isEnabled(Context context) {
        SharedPreferences prefs = Generators.getInstance(context).getSharedPreferences(context);

        return prefs.getBoolean(SystemStatus.ENABLED, SystemStatus.ENABLED_DEFAULT);
    }

    @SuppressWarnings({"unused"})
    public static boolean isRunning(Context context) {
        if (SystemStatus.sInstance == null) {
            return false;
        }

        return SystemStatus.sInstance.mReceiver != null;
    }

    @SuppressWarnings({"unused"})
    public static ArrayList<DiagnosticAction> diagnostics(Context context) {
        return new ArrayList<>();
    }

    @SuppressWarnings("WeakerAccess")
    public static String getGeneratorTitle(Context context) {
        return context.getString(R.string.generator_diagnostics_system_status);
    }

    @SuppressWarnings("unused")
    public static void bindDisclosureViewHolder(final GeneratorViewHolder holder) {
        TextView generatorLabel = holder.itemView.findViewById(R.id.label_generator);

        generatorLabel.setText(SystemStatus.getGeneratorTitle(holder.itemView.getContext()));
    }

    @SuppressWarnings("unused")
    public static void bindViewHolder(DataPointViewHolder holder) {
        final Context context = holder.itemView.getContext();

        SystemStatus generator = SystemStatus.getInstance(context);

        long now = System.currentTimeMillis();
        long start = now - (24 * 60 * 60 * 1000);

        String where = SystemStatus.HISTORY_OBSERVED + " >= ?";
        String[] args = { "" + start };

        Cursor c = generator.mDatabase.query(SystemStatus.TABLE_HISTORY, null, where, args, null, null, SystemStatus.HISTORY_OBSERVED + " DESC");

        View cardContent = holder.itemView.findViewById(R.id.card_content);
        View cardEmpty = holder.itemView.findViewById(R.id.card_empty);
        TextView dateLabel = holder.itemView.findViewById(R.id.generator_data_point_date);

        if (c.moveToNext()) {
            cardContent.setVisibility(View.VISIBLE);
            cardEmpty.setVisibility(View.GONE);

            long timestamp = c.getLong(c.getColumnIndex(SystemStatus.HISTORY_OBSERVED)) / 1000;

            dateLabel.setText(Generator.formatTimestamp(context, timestamp));

            c.moveToPrevious();

            final LineChart chart = holder.itemView.findViewById(R.id.system_status_chart);
            chart.setViewPortOffsets(0,0,0,0);
            chart.setHighlightPerDragEnabled(false);
            chart.setHighlightPerTapEnabled(false);
            chart.setBackgroundColor(ContextCompat.getColor(context, android.R.color.black));
            chart.setPinchZoom(false);

            final DateFormat timeFormat = android.text.format.DateFormat.getTimeFormat(context);

            final XAxis xAxis = chart.getXAxis();
            xAxis.setPosition(XAxis.XAxisPosition.BOTTOM_INSIDE);
            xAxis.setTextSize(10f);
            xAxis.setDrawAxisLine(true);
            xAxis.setDrawGridLines(true);
            xAxis.setCenterAxisLabels(true);
            xAxis.setDrawLabels(true);
            xAxis.setTextColor(ContextCompat.getColor(context, android.R.color.white));
            xAxis.setGranularityEnabled(true);
            xAxis.setGranularity(1);
            xAxis.setAxisMinimum(start);
            xAxis.setAxisMaximum(now);
            xAxis.setValueFormatter(new IAxisValueFormatter() {
                @Override
                public String getFormattedValue(float value, AxisBase axis) {
                   Date date = new Date((long) value);

                    return timeFormat.format(date);
                }
            });

            YAxis leftAxis = chart.getAxisLeft();
            leftAxis.setPosition(YAxis.YAxisLabelPosition.INSIDE_CHART);
            leftAxis.setDrawGridLines(true);
            leftAxis.setDrawAxisLine(true);
            leftAxis.setGranularityEnabled(true);
            leftAxis.setTextColor(ContextCompat.getColor(context, android.R.color.white));
            leftAxis.setValueFormatter(new IAxisValueFormatter() {
                @Override
                public String getFormattedValue(float value, AxisBase axis) {
                    return "" + value + " GB";
                }
            });

            YAxis rightAxis = chart.getAxisRight();
            rightAxis.setEnabled(false);

            chart.getLegend().setEnabled(false);
            chart.getDescription().setEnabled(false);

            int observedIndex = c.getColumnIndex(SystemStatus.HISTORY_OBSERVED);
            int availableIndex = c.getColumnIndex(SystemStatus.HISTORY_STORAGE_AVAILABLE);
            int appUsedIndex = c.getColumnIndex(SystemStatus.HISTORY_STORAGE_USED_APP);
            // int othersUsedIndex = c.getColumnIndex(SystemStatus.HISTORY_STORAGE_USED_OTHER);

            ArrayList<Entry> availableValues = new ArrayList<>();
            ArrayList<Entry> appValues = new ArrayList<>();
            // ArrayList<Entry> otherValues = new ArrayList<>();

            long runtime = -1;

            while (c.moveToNext()) {
                long when = c.getLong(observedIndex);

                double available = (double) c.getLong(availableIndex);
                double app = (double) c.getLong(appUsedIndex);
                // double other = (double) c.getLong(othersUsedIndex);

                availableValues.add(0, new Entry(when, (float) (available / SystemStatus.GIGABYTE)));
                appValues.add(0, new Entry(when, (float) (app / SystemStatus.GIGABYTE)));
                // otherValues.add(0, new Entry(when, (float) (other / SystemStatus.GIGABYTE)));

                if (runtime == -1) {
                    runtime = c.getLong(c.getColumnIndex(SystemStatus.HISTORY_RUNTIME));
                }
            }

            LineData sets = new LineData();

            LineDataSet set = new LineDataSet(availableValues, "available");
            set.setAxisDependency(YAxis.AxisDependency.LEFT);
            set.setLineWidth(1.0f);
            set.setDrawCircles(true);
            set.setFillAlpha(192);
            set.setDrawFilled(false);
            set.setDrawValues(true);
            set.setCircleColor(ContextCompat.getColor(context, R.color.generator_system_status_free));
            set.setCircleRadius(1.5f);
            set.setCircleHoleRadius(0.0f);
            set.setDrawCircleHole(false);
            set.setDrawValues(false);
            set.setColor(ContextCompat.getColor(context, R.color.generator_system_status_free));
            set.setMode(LineDataSet.Mode.LINEAR);

            sets.addDataSet(set);

            set = new LineDataSet(appValues, "app");
            set.setAxisDependency(YAxis.AxisDependency.LEFT);
            set.setLineWidth(1.0f);
            set.setDrawCircles(true);
            set.setCircleColor(ContextCompat.getColor(context, R.color.generator_system_status_app));
            set.setCircleRadius(1.5f);
            set.setCircleHoleRadius(0.0f);
            set.setFillAlpha(192);
            set.setDrawFilled(false);
            set.setDrawValues(true);
            set.setColor(ContextCompat.getColor(context, R.color.generator_system_status_app));
            set.setDrawCircleHole(false);
            set.setDrawValues(false);
            set.setMode(LineDataSet.Mode.LINEAR);

            sets.addDataSet(set);

            chart.setData(sets);

            TextView runtimeLabel = holder.itemView.findViewById(R.id.system_status_runtime);
            runtimeLabel.setText(context.getString(R.string.generator_system_status_runtime, SystemStatus.formatRuntime(context, runtime)));
       } else {
            cardContent.setVisibility(View.GONE);
            cardEmpty.setVisibility(View.VISIBLE);

            dateLabel.setText(R.string.label_never_pdk);
        }

        c.close();
    }

    @SuppressWarnings("StringConcatenationInLoop")
    private static String formatRuntime(Context context, long runtime) {
        long days = runtime / (24 * 60 * 60 * 1000);
        runtime -= (24 * 60 * 60 * 1000) * days;

        long hours = runtime / (60 * 60 * 1000);
        runtime -= (60 * 60 * 1000) * hours;

        long minutes = runtime / (60 * 1000);
        runtime -= (60 * 1000) * minutes;

        long seconds = runtime / (1000);
        runtime -= (1000) * seconds;

        String hourString = "" + hours;

        if (hourString.length() == 1) {
            hourString = "0" + hourString;
        }

        String minuteString = "" + minutes;

        if (minuteString.length() == 1) {
            minuteString = "0" + minuteString;
        }

        String secondString = "" + seconds;

        if (secondString.length() == 1) {
            secondString = "0" + secondString;
        }

        String msString = "" + runtime;

        while (msString.length() < 3) {
            msString = "0" + msString;
        }

        return context.getString(R.string.generator_system_status_runtime_formatted, days, hourString, minuteString, secondString, msString);
    }

    private static long getFileSize(final File file)
    {
        if (file == null||!file.exists()) {
            return 0;
        }

        if (!file.isDirectory()) {
            return file.length();
        }

        final List<File> dirs = new LinkedList<>();

        dirs.add(file);

        long result=0;

        while(!dirs.isEmpty()) {
            final File dir = dirs.remove(0);

            if (!dir.exists()) {
                continue;
            }

            final File[] listFiles = dir.listFiles();

            if (listFiles==null||listFiles.length==0) {
                continue;
            }

            for (final File child : listFiles) {
                result += child.length();

                if (child.isDirectory()) {
                    dirs.add(child);
                }
            }
        }

        return result;
    }

    @SuppressWarnings("unused")
    public static View fetchView(ViewGroup parent)
    {
        return LayoutInflater.from(parent.getContext()).inflate(R.layout.card_generator_diagnostics_system_status, parent, false);
    }

    @Override
    public List<Bundle> fetchPayloads() {
        return new ArrayList<>();
    }

    @SuppressWarnings("unused")
    public static long latestPointGenerated(Context context) {
        SystemStatus me = SystemStatus.getInstance(context);

        if (me.mLastTimestamp == 0) {
            Cursor c = me.mDatabase.query(SystemStatus.TABLE_HISTORY, null, null, null, null, null, SystemStatus.HISTORY_OBSERVED + " DESC");

            if (c.moveToNext()) {
                me.mLastTimestamp = c.getLong(c.getColumnIndex(SystemStatus.HISTORY_OBSERVED));
            }

            c.close();
        }

        return me.mLastTimestamp;
    }

    public Cursor queryHistory(String[] cols, String where, String[] args, String orderBy) {
        return this.mDatabase.query(SystemStatus.TABLE_HISTORY, cols, where, args, null, null, orderBy);
    }

    @Override
    protected void flushCachedData() {
        final SystemStatus me = this;

        Runnable r = new Runnable() {
            @Override
            public void run() {
                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(me.mContext);

                long retentionPeriod = prefs.getLong(SystemStatus.DATA_RETENTION_PERIOD, SystemStatus.DATA_RETENTION_PERIOD_DEFAULT);

                long start = System.currentTimeMillis() - retentionPeriod;

                String where = SystemStatus.HISTORY_OBSERVED + " < ?";
                String[] args = { "" + start };

                me.mDatabase.delete(SystemStatus.TABLE_HISTORY, where, args);
            }
        };

        Thread t = new Thread(r);
        t.start();
    }

    @Override
    public void setCachedDataRetentionPeriod(long period) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this.mContext);
        SharedPreferences.Editor e = prefs.edit();

        e.putLong(SystemStatus.DATA_RETENTION_PERIOD, period);

        e.apply();
    }
}
