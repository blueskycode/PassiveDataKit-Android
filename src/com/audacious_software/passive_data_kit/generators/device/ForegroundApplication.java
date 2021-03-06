package com.audacious_software.passive_data_kit.generators.device;

import android.annotation.SuppressLint;
import android.app.AppOpsManager;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.audacious_software.passive_data_kit.PassiveDataKit;
import com.audacious_software.passive_data_kit.activities.generators.DataPointViewHolder;
import com.audacious_software.passive_data_kit.activities.generators.GeneratorViewHolder;
import com.audacious_software.passive_data_kit.diagnostics.DiagnosticAction;
import com.audacious_software.passive_data_kit.generators.Generator;
import com.audacious_software.passive_data_kit.generators.Generators;
import com.audacious_software.pdk.passivedatakit.R;
import com.rvalerio.fgchecker.AppChecker;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

@SuppressWarnings({"PointlessBooleanExpression", "SimplifiableIfStatement"})
public class ForegroundApplication extends Generator{

    private static final String GENERATOR_IDENTIFIER = "pdk-foreground-application";

    private static final String ENABLED = "com.audacious_software.passive_data_kit.generators.device.ForegroundApplication.ENABLED";
    private static final boolean ENABLED_DEFAULT = true;

    private static final String DATA_RETENTION_PERIOD = "com.audacious_software.passive_data_kit.generators.device.ForegroundApplication.DATA_RETENTION_PERIOD";
    private static final long DATA_RETENTION_PERIOD_DEFAULT = (60L * 24L * 60L * 60L * 1000L);

    private static final int DATABASE_VERSION = 3;

    private static final String TABLE_HISTORY = "history";
    private static final String HISTORY_OBSERVED = "observed";
    private static final String HISTORY_APPLICATION = "application";
    private static final String HISTORY_DURATION = "duration";
    private static final String HISTORY_SCREEN_ACTIVE = "screen_active";

    private static ForegroundApplication sInstance = null;

    private static final String DATABASE_PATH = "pdk-foreground-application.sqlite";

    private SQLiteDatabase mDatabase = null;
    private long mSampleInterval = 15000;
    private AppChecker mAppChecker = null;
    private long mLastTimestamp = 0;

    @SuppressWarnings("unused")
    public static String generatorIdentifier() {
        return ForegroundApplication.GENERATOR_IDENTIFIER;
    }

    @SuppressWarnings("WeakerAccess")
    public static ForegroundApplication getInstance(Context context) {
        if (ForegroundApplication.sInstance == null) {
            ForegroundApplication.sInstance = new ForegroundApplication(context.getApplicationContext());
        }

        return ForegroundApplication.sInstance;
    }

    @SuppressWarnings("WeakerAccess")
    public ForegroundApplication(Context context) {
        super(context);
    }

    @SuppressWarnings("unused")
    public static void start(final Context context) {
        ForegroundApplication.getInstance(context).startGenerator();
    }

    private void startGenerator() {
        final ForegroundApplication me = this;

        if (this.mAppChecker != null) {
            this.mAppChecker.stop();

            this.mAppChecker = null;
        }

        this.mAppChecker = new AppChecker();
        this.mAppChecker.other(new AppChecker.Listener() {
            @Override
            public void onForeground(final String process) {
                final long now = System.currentTimeMillis();

                WindowManager window = (WindowManager) me.mContext.getSystemService(Context.WINDOW_SERVICE);
                final Display display = window.getDefaultDisplay();

                Runnable r = new Runnable() {
                    @Override
                    public void run() {
                        boolean screenActive = true;

                        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.KITKAT) {
                            if (display.getState() != Display.STATE_ON) {
                                screenActive = false;
                            }
                        }

                        ContentValues values = new ContentValues();
                        values.put(ForegroundApplication.HISTORY_OBSERVED, now);
                        values.put(ForegroundApplication.HISTORY_APPLICATION, process);
                        values.put(ForegroundApplication.HISTORY_DURATION, me.mSampleInterval);
                        values.put(ForegroundApplication.HISTORY_SCREEN_ACTIVE, screenActive);

                        me.mDatabase.insert(ForegroundApplication.TABLE_HISTORY, null, values);

                        Bundle update = new Bundle();
                        update.putLong(ForegroundApplication.HISTORY_OBSERVED, now);
                        update.putString(ForegroundApplication.HISTORY_APPLICATION, process);
                        update.putLong(ForegroundApplication.HISTORY_DURATION, me.mSampleInterval);
                        update.putBoolean(ForegroundApplication.HISTORY_SCREEN_ACTIVE, screenActive);

                        Generators.getInstance(me.mContext).notifyGeneratorUpdated(ForegroundApplication.GENERATOR_IDENTIFIER, update);
                    }
                };

                try {
                    Thread t = new Thread(r);
                    t.start();
                } catch (OutOfMemoryError e) {
                    // Try again later...
                }
            }
        });

        this.mAppChecker.timeout((int) this.mSampleInterval);
        this.mAppChecker.start(this.mContext);

        File path = PassiveDataKit.getGeneratorsStorage(this.mContext);

        path = new File(path, ForegroundApplication.DATABASE_PATH);

        this.mDatabase = SQLiteDatabase.openOrCreateDatabase(path, null);

        int version = this.getDatabaseVersion(this.mDatabase);

        switch (version) {
            case 0:
                this.mDatabase.execSQL(this.mContext.getString(R.string.pdk_generator_foreground_applications_create_history_table));
            case 1:
                this.mDatabase.execSQL(this.mContext.getString(R.string.pdk_generator_foreground_applications_history_table_add_duration));
            case 2:
                this.mDatabase.execSQL(this.mContext.getString(R.string.pdk_generator_foreground_applications_history_table_add_screen_active));
        }

        if (version != ForegroundApplication.DATABASE_VERSION) {
            this.setDatabaseVersion(this.mDatabase, ForegroundApplication.DATABASE_VERSION);
        }

        Generators.getInstance(this.mContext).registerCustomViewClass(ForegroundApplication.GENERATOR_IDENTIFIER, ForegroundApplication.class);

        this.flushCachedData();
    }

    @SuppressWarnings("unused")
    public static boolean isEnabled(Context context) {
        SharedPreferences prefs = Generators.getInstance(context).getSharedPreferences(context);

        return prefs.getBoolean(ForegroundApplication.ENABLED, ForegroundApplication.ENABLED_DEFAULT);
    }

    @SuppressWarnings({"unused"})
    public static boolean isRunning(Context context) {
        if (ForegroundApplication.sInstance == null) {
            return false;
        }

        return ForegroundApplication.sInstance.mAppChecker != null;
    }

    @SuppressWarnings("unused")
    @SuppressLint("InlinedApi")
    public static ArrayList<DiagnosticAction> diagnostics(final Context context) {
        ArrayList<DiagnosticAction> actions = new ArrayList<>();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            AppOpsManager appOps = (AppOpsManager) context.getSystemService(Context.APP_OPS_SERVICE);
            int mode = appOps.checkOpNoThrow("android:get_usage_stats", android.os.Process.myUid(), context.getPackageName());

            if (mode != AppOpsManager.MODE_ALLOWED) {
                actions.add(new DiagnosticAction(context.getString(R.string.diagnostic_usage_stats_permission_required_title), context.getString(R.string.diagnostic_usage_stats_permission_required), new Runnable() {
                    @Override
                    public void run() {
                        Intent intent = new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS);
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        context.startActivity(intent);
                    }
                }));
            }
        }

        return actions;
    }

    public static String getGeneratorTitle(Context context) {
        return context.getString(R.string.generator_foreground_application);
    }

    @SuppressWarnings("unused")
    public static void bindDisclosureViewHolder(final GeneratorViewHolder holder) {
        TextView generatorLabel = holder.itemView.findViewById(R.id.label_generator);

        generatorLabel.setText(ForegroundApplication.getGeneratorTitle(holder.itemView.getContext()));
    }

    @SuppressWarnings("unused")
    public static void bindViewHolder(DataPointViewHolder holder) {
        long start = System.currentTimeMillis();

        final Context context = holder.itemView.getContext();

        long lastTimestamp = 0;

        ForegroundApplication generator = ForegroundApplication.getInstance(holder.itemView.getContext());

        Cursor c = generator.mDatabase.query(ForegroundApplication.TABLE_HISTORY, null, null, null, null, null, ForegroundApplication.HISTORY_OBSERVED + " DESC", "1");

        if (c.moveToNext()) {
            if (lastTimestamp == 0) {
                lastTimestamp = c.getLong(c.getColumnIndex(ForegroundApplication.HISTORY_OBSERVED));
            }
        }

        c.close();

        View cardContent = holder.itemView.findViewById(R.id.card_content);
        View cardEmpty = holder.itemView.findViewById(R.id.card_empty);
        TextView dateLabel = holder.itemView.findViewById(R.id.generator_data_point_date);

        long now = System.currentTimeMillis();
        long yesterday = now - (24 * 60 * 60 * 1000);

        HashMap<String,Double> appDurations = new HashMap<>();
        HashMap<String,Long> appWhens = new HashMap<>();

        ArrayList<String> latest = new ArrayList<>();

        String where = ForegroundApplication.HISTORY_OBSERVED + " > ? AND " + ForegroundApplication.HISTORY_SCREEN_ACTIVE + " = ?";
        String[] args = { "" + yesterday, "1" };

        c = generator.mDatabase.query(ForegroundApplication.TABLE_HISTORY, null, where, args, null, null, ForegroundApplication.HISTORY_OBSERVED);

        while (c.moveToNext()) {
            String application = c.getString(c.getColumnIndex(ForegroundApplication.HISTORY_APPLICATION));

            if (application != null) {
                if (latest.contains(application)) {
                    latest.remove(application);
                }

                latest.add(0, application);

                if (appDurations.containsKey(application) == false) {
                    appDurations.put(application, 0.0);
                }

                double appDuration = appDurations.get(application);
                double duration = c.getDouble(c.getColumnIndex(ForegroundApplication.HISTORY_DURATION));

                long lastObserved = c.getLong(c.getColumnIndex(ForegroundApplication.HISTORY_OBSERVED));

                appDuration += duration;

                appDurations.put(application, appDuration);
                appWhens.put(application, lastObserved);
            }
        }

        c.close();

        double largestUsage = 0.0;

        ArrayList<HashMap<String,Double>> largest = new ArrayList<>();

        for (String key : appDurations.keySet()) {
            HashMap<String, Double> app = new HashMap<>();

            double duration = appDurations.get(key);

            app.put(key, duration);

            if (duration > largestUsage) {
                largestUsage = duration;
            }

            largest.add(app);
        }

        Collections.sort(largest, new Comparator<HashMap<String, Double>>() {
            @Override
            public int compare(HashMap<String, Double> mapOne, HashMap<String, Double> mapTwo) {
                String keyOne = mapOne.keySet().iterator().next();
                String keyTwo = mapTwo.keySet().iterator().next();

                Double valueOne = mapOne.get(keyOne);
                Double valueTwo = mapTwo.get(keyTwo);

                return valueTwo.compareTo(valueOne);
            }
        });

        int[] appRowIds = { R.id.application_one, R.id.application_two, R.id.application_three, R.id.application_four };
        int[] whenAppRowIds = { R.id.application_recent_one, R.id.application_recent_two, R.id.application_recent_three, R.id.application_recent_four };

        while (largest.size() > appRowIds.length) {
            largest.remove(largest.size() - 1);
        }

        PackageManager packageManager = context.getPackageManager();

        if (largest.size() > 0) {
            for (int appRowId : appRowIds) {
                View row = cardContent.findViewById(appRowId);

                row.setVisibility(View.GONE);
            }

            for (int i = 0; i < appRowIds.length && i < largest.size(); i++) {
                HashMap<String, Double> appDef = largest.get(i);
                int appRowId = appRowIds[i];

                View row = cardContent.findViewById(appRowId);
                row.setVisibility(View.VISIBLE);

                TextView appName = row.findViewById(R.id.app_name);
                ImageView appIcon = row.findViewById(R.id.application_icon);

                View usedDuration = row.findViewById(R.id.app_used_duration);
                View remainderDuration = row.findViewById(R.id.app_remaining_duration);

                for (String key : appDef.keySet()) {
                    double duration = appDef.get(key);

                    double minutes = duration / (1000 * 60);

                    try {
                        String name = packageManager.getApplicationLabel(packageManager.getApplicationInfo(key, PackageManager.GET_META_DATA)).toString();

                        appName.setText(context.getString(R.string.generator_foreground_application_app_name_duration, name, minutes));
                        Drawable icon = packageManager.getApplicationIcon(key);
                        appIcon.setImageDrawable(icon);
                    } catch (PackageManager.NameNotFoundException e) {
                        e.printStackTrace();

                        appName.setText(context.getString(R.string.generator_foreground_application_app_name_duration, key, minutes));
                        appIcon.setImageDrawable(null);
                    }

                    double remainder = largestUsage - duration;

                    LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) usedDuration.getLayoutParams();
                    params.weight = (float) duration;
                    usedDuration.setLayoutParams(params);

                    params = (LinearLayout.LayoutParams) remainderDuration.getLayoutParams();
                    params.weight = (float) remainder;
                    remainderDuration.setLayoutParams(params);
                }
            }

            for (int i = 0; i < whenAppRowIds.length && i < latest.size(); i++) {
                int appRowId = whenAppRowIds[i];

                String appPackage = latest.get(i);

                while (appPackage == null && i < latest.size() - 1) {
                    i += 1;

                    appPackage = latest.get(i);
                }

                if (appPackage != null) {
                    View row = cardContent.findViewById(appRowId);
                    row.setVisibility(View.VISIBLE);

                    TextView appName = row.findViewById(R.id.app_name);
                    ImageView appIcon = row.findViewById(R.id.application_icon);

                    try {
                        String name = packageManager.getApplicationLabel(packageManager.getApplicationInfo(appPackage, PackageManager.GET_META_DATA)).toString();

                        appName.setText(name);
                        Drawable icon = packageManager.getApplicationIcon(appPackage);
                        appIcon.setImageDrawable(icon);
                    } catch (PackageManager.NameNotFoundException e) {
                        appName.setText(appPackage);
                        appIcon.setImageDrawable(null);
                    }

                    TextView appWhen = row.findViewById(R.id.app_last_used);
                    appWhen.setText(Generator.formatTimestamp(context, appWhens.get(appPackage) / 1000));
                }
            }

            cardContent.setVisibility(View.VISIBLE);
            cardEmpty.setVisibility(View.GONE);

            dateLabel.setText(Generator.formatTimestamp(context, lastTimestamp / 1000));
        } else {
            cardContent.setVisibility(View.GONE);
            cardEmpty.setVisibility(View.VISIBLE);

            dateLabel.setText(R.string.label_never_pdk);
        }
   }

    @Override
    public List<Bundle> fetchPayloads() {
        return new ArrayList<>();
    }

    @Override
    protected void flushCachedData() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this.mContext);

        long retentionPeriod = prefs.getLong(ForegroundApplication.DATA_RETENTION_PERIOD, ForegroundApplication.DATA_RETENTION_PERIOD_DEFAULT);

        long start = System.currentTimeMillis() - retentionPeriod;

        String where = ForegroundApplication.HISTORY_OBSERVED + " < ?";
        String[] args = { "" + start };

        this.mDatabase.delete(ForegroundApplication.TABLE_HISTORY, where, args);
    }

    @Override
    public void setCachedDataRetentionPeriod(long period) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this.mContext);
        SharedPreferences.Editor e = prefs.edit();

        e.putLong(ForegroundApplication.DATA_RETENTION_PERIOD, period);

        e.apply();
    }

    @SuppressWarnings("unused")
    public static View fetchView(ViewGroup parent)
    {
        return LayoutInflater.from(parent.getContext()).inflate(R.layout.card_generator_foreground_application, parent, false);
    }

    @SuppressWarnings("unused")
    public static long latestPointGenerated(Context context) {
        ForegroundApplication me = ForegroundApplication.getInstance(context);

        if (me.mLastTimestamp == 0) {
            Cursor c = me.mDatabase.query(ForegroundApplication.TABLE_HISTORY, null, null, null, null, null, ForegroundApplication.HISTORY_OBSERVED + " DESC");

            if (c.moveToNext()) {
                me.mLastTimestamp = c.getLong(c.getColumnIndex(ForegroundApplication.HISTORY_OBSERVED));
            }

            c.close();
        }

        return me.mLastTimestamp;
    }
}
