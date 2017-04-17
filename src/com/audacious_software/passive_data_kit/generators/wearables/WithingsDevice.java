package com.audacious_software.passive_data_kit.generators.wearables;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.audacious_software.passive_data_kit.PassiveDataKit;
import com.audacious_software.passive_data_kit.activities.generators.DataPointViewHolder;
import com.audacious_software.passive_data_kit.diagnostics.DiagnosticAction;
import com.audacious_software.passive_data_kit.generators.Generator;
import com.audacious_software.passive_data_kit.generators.Generators;
import com.audacious_software.passive_data_kit.generators.diagnostics.AppEvent;
import com.audacious_software.pdk.passivedatakit.R;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.UUID;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class WithingsDevice extends Generator {
    private static final String GENERATOR_IDENTIFIER = "pdk-withings-device";

    private static final String ENABLED = "com.audacious_software.passive_data_kit.generators.wearables.WithingsDevice.ENABLED";
    private static final boolean ENABLED_DEFAULT = true;

    private static final String DATASTREAM = "datastream";
    private static final String DATASTREAM_ACTIVITY_MEASURES = "activity-measures";

    private static final String TABLE_ACTIVITY_MEASURE_HISTORY = "activity_measure_history";
    private static final String ACTIVITY_MEASURE_HISTORY_DATE_START = "date_start";
    private static final String ACTIVITY_MEASURE_HISTORY_TIMEZONE = "timezone";
    private static final String ACTIVITY_MEASURE_STEPS = "steps";
    private static final String ACTIVITY_MEASURE_DISTANCE = "distance";
    private static final String ACTIVITY_MEASURE_ACTIVE_CALORIES = "active_calories";
    private static final String ACTIVITY_MEASURE_TOTAL_CALORIES = "total_calories";
    private static final String ACTIVITY_MEASURE_ELEVATION = "elevation";
    private static final String ACTIVITY_MEASURE_SOFT_ACTIVITY_DURATION = "soft_activity_duration";
    private static final String ACTIVITY_MEASURE_MODERATE_ACTIVITY_DURATION = "moderate_activity_duration";
    private static final String ACTIVITY_MEASURE_INTENSE_ACTIVITY_DURATION = "intense_activity_duration";

    private static final String TABLE_BODY_MEASURE_HISTORY = "body_measure_history";
    private static final String BODY_MEASURE_STATUS_UNKNOWN = "unknown";
    private static final String BODY_MEASURE_STATUS_USER_DEVICE = "user-device";
    private static final String BODY_MEASURE_STATUS_SHARED_DEVICE = "shared-device";
    private static final String BODY_MEASURE_STATUS_MANUAL_ENTRY = "manual-entry";
    private static final String BODY_MEASURE_STATUS_MANUAL_ENTRY_CREATION = "manual-entry-creation";
    private static final String BODY_MEASURE_STATUS_AUTO_DEVICE = "auto-device";
    private static final String BODY_MEASURE_STATUS_MEASURE_CONFIRMED = "measure-confirmed";

    private static final String BODY_MEASURE_CATEGORY_UNKNOWN = "unknown";
    private static final String BODY_MEASURE_CATEGORY_REAL_MEASUREMENTS = "real-measurements";
    private static final String BODY_MEASURE_CATEGORY_USER_OBJECTIVES = "user-objectives";

    private static final String BODY_MEASURE_TYPE_UNKNOWN = "unknown";
    private static final String BODY_MEASURE_TYPE_WEIGHT = "weight";
    private static final String BODY_MEASURE_TYPE_HEIGHT = "height";
    private static final String BODY_MEASURE_TYPE_FAT_FREE_MASS = "fat-free-mass";
    private static final String BODY_MEASURE_TYPE_FAT_RATIO = "fat-ratio";
    private static final String BODY_MEASURE_TYPE_FAT_MASS_WEIGHT = "fat-mass-weight";
    private static final String BODY_MEASURE_TYPE_DIASTOLIC_BLOOD_PRESSURE = "diastolic-blood-pressure";
    private static final String BODY_MEASURE_TYPE_SYSTOLIC_BLOOD_PRESSURE = "systolic-blood-pressure";
    private static final String BODY_MEASURE_TYPE_HEART_PULSE = "heart-pulse";
    private static final String BODY_MEASURE_TYPE_TEMPERATURE = "temperature";
    private static final String BODY_MEASURE_TYPE_OXYGEN_SATURATION = "oxygen-saturation";
    private static final String BODY_MEASURE_TYPE_BODY_TEMPERATURE = "body-temperature";
    private static final String BODY_MEASURE_TYPE_SKIN_TEMPERATURE = "skin-temperature";
    private static final String BODY_MEASURE_TYPE_MUSCLE_MASS = "muscle-mass";
    private static final String BODY_MEASURE_TYPE_HYDRATION = "hydration";
    private static final String BODY_MEASURE_TYPE_BONE_MASS = "bone-mass";
    private static final String BODY_MEASURE_TYPE_PULSE_WAVE_VELOCITY = "pulse-wave-velocity";

    private static final String BODY_MEASURE_HISTORY_DATE = "measure_date";
    private static final String BODY_MEASURE_HISTORY_STATUS = "measure_status";
    private static final String BODY_MEASURE_HISTORY_CATEGORY = "measure_category";
    private static final String BODY_MEASURE_HISTORY_TYPE = "measure_type";
    private static final String BODY_MEASURE_HISTORY_VALUE = "measure_value";

    private static final String TABLE_INTRADAY_ACTIVITY_HISTORY = "intraday_activity_history";

    private static final String TABLE_SLEEP_MEASURE_HISTORY = "sleep_measure_history";
    private static final String SLEEP_MEASURE_MODEL_UNKNOWN = "unknown";
    private static final String SLEEP_MEASURE_MODEL_ACTIVITY_TRACKER = "activity-tracker";
    private static final String SLEEP_MEASURE_MODEL_AURA = "aura";

    private static final String SLEEP_MEASURE_STATE_UNKNOWN = "unknown";
    private static final String SLEEP_MEASURE_STATE_AWAKE = "awake";
    private static final String SLEEP_MEASURE_STATE_LIGHT_SLEEP = "light-sleep";
    private static final String SLEEP_MEASURE_STATE_DEEP_SLEEP = "deep-sleep";
    private static final String SLEEP_MEASURE_STATE_REM_SLEEP = "rem-sleep";

    private static final String SLEEP_MEASURE_START_DATE = "start_date";
    private static final String SLEEP_MEASURE_END_DATE = "end_date";
    private static final String SLEEP_MEASURE_STATE = "state";
    private static final String SLEEP_MEASURE_MEASUREMENT_DEVICE = "measurement_device";


    private static final String TABLE_SLEEP_SUMMARY_HISTORY = "sleep_summary_history";
    private static final String SLEEP_SUMMARY_MODEL_UNKNOWN = "unknown";
    private static final String SLEEP_SUMMARY_MODEL_ACTIVITY_TRACKER = "activity-tracker";
    private static final String SLEEP_SUMMARY_MODEL_AURA = "aura";

    private static final String SLEEP_SUMMARY_START_DATE = "start_date";
    private static final String SLEEP_SUMMARY_END_DATE = "end_date";
    private static final String SLEEP_SUMMARY_TIMEZONE = "timezone";
    private static final String SLEEP_SUMMARY_MEASUREMENT_DEVICE = "measurement_device";
    private static final String SLEEP_SUMMARY_WAKE_DURATION = "wake_duration";
    private static final String SLEEP_SUMMARY_LIGHT_SLEEP_DURATION = "light_sleep_duration";
    private static final String SLEEP_SUMMARY_DEEP_SLEEP_DURATION = "deep_sleep_duration";
    private static final String SLEEP_SUMMARY_TO_SLEEP_DURATION = "to_sleep_duration";
    private static final String SLEEP_SUMMARY_WAKE_COUNT = "wake_count";
    private static final String SLEEP_SUMMARY_REM_SLEEP_DURATION = "rem_sleep_duration";
    private static final String SLEEP_SUMMARY_TO_WAKE_DURATION = "to_wake_duration";

    private static final String TABLE_WORKOUT_HISTORY = "workout_history";

    private static final String HISTORY_OBSERVED = "observed";
    private static final String DATABASE_PATH = "pdk-withings-device.sqlite";;
    private static final int DATABASE_VERSION = 1;

    private static final String LAST_DATA_FETCH = "com.audacious_software.passive_data_kit.generators.wearables.WithingsDevice.LAST_DATA_FETCH";

    private static final String DATA_FETCH_INTERVAL = "com.audacious_software.passive_data_kit.generators.wearables.WithingsDevice.DATA_FETCH_INTERVAL";
    private static final long DATA_FETCH_INTERVAL_DEFAULT = (60 * 60 * 1000);

    private static final String ACTIVITY_MEASURES_ENABLED = "com.audacious_software.passive_data_kit.generators.wearables.WithingsDevice.ACTIVITY_MEASURES_ENABLED";
    private static final boolean ACTIVITY_MEASURES_ENABLED_DEFAULT = true;

    private static final String BODY_MEASURES_ENABLED = "com.audacious_software.passive_data_kit.generators.wearables.WithingsDevice.BODY_MEASURES_ENABLED";
    private static final boolean BODY_MEASURES_ENABLED_DEFAULT = true;

    private static final String INTRADAY_ACTIVITY_ENABLED = "com.audacious_software.passive_data_kit.generators.wearables.WithingsDevice.INTRADAY_ACTIVITY_ENABLED";
    private static final boolean INTRADAY_ACTIVITY_ENABLED_DEFAULT = false;

    private static final String SLEEP_MEASURES_ENABLED = "com.audacious_software.passive_data_kit.generators.wearables.WithingsDevice.SLEEP_MEASURES_ENABLED";
    private static final boolean SLEEP_MEASURES_ENABLED_DEFAULT = true;

    private static final String SLEEP_SUMMARY_ENABLED = "com.audacious_software.passive_data_kit.generators.wearables.WithingsDevice.SLEEP_SUMMARY_ENABLED";
    private static final boolean SLEEP_SUMMARY_ENABLED_DEFAULT = true;

    private static final String WORKOUTS_ENABLED = "com.audacious_software.passive_data_kit.generators.wearables.WithingsDevice.WORKOUTS_ENABLED";
    private static final boolean WORKOUTS_ENABLED_DEFAULT = true;

    public static final String OPTION_OAUTH_CALLBACK_URL = "com.audacious_software.passive_data_kit.generators.wearables.WithingsDevice.OPTION_CALLBACK_URL";
    public static final String OPTION_OAUTH_CONSUMER_KEY = "com.audacious_software.passive_data_kit.generators.wearables.WithingsDevice.OPTION_OAUTH_CONSUMER_KEY";
    public static final String OPTION_OAUTH_CONSUMER_SECRET = "com.audacious_software.passive_data_kit.generators.wearables.WithingsDevice.OPTION_OAUTH_CONSUMER_SECRET";
    private static final String OPTION_OAUTH_ACCESS_TOKEN = "com.audacious_software.passive_data_kit.generators.wearables.WithingsDevice.OPTION_OAUTH_ACCESS_TOKEN";
    private static final String OPTION_OAUTH_ACCESS_TOKEN_SECRET = "com.audacious_software.passive_data_kit.generators.wearables.WithingsDevice.OPTION_OAUTH_ACCESS_TOKEN_SECRET";
    private static final String OPTION_OAUTH_REQUEST_SECRET = "com.audacious_software.passive_data_kit.generators.wearables.WithingsDevice.OPTION_OAUTH_REQUEST_SECRET";
    private static final String OPTION_OAUTH_ACCESS_USER_ID = "com.audacious_software.passive_data_kit.generators.wearables.WithingsDevice.OPTION_OAUTH_ACCESS_USER_ID";

    private static final String API_ACTION_ACTIVITY_URL = "https://wbsapi.withings.net/v2/measure?action=getactivity";
    private static final String API_ACTION_BODY_MEASURES_URL = "https://wbsapi.withings.net/measure?action=getmeas";
    private static final String API_ACTION_INTRADAY_ACTIVITY_URL = "https://wbsapi.withings.net/v2/measure?action=getintradayactivity";
    private static final String API_ACTION_SLEEP_MEASURES_URL = "https://wbsapi.withings.net/v2/sleep?action=get";
    private static final String API_ACTION_SLEEP_SUMMARY_URL = "https://wbsapi.withings.net/v2/sleep?action=getsummary";
    private static final String API_ACTION_WORKOUTS_URL = "https://wbsapi.withings.net/v2/measure?action=getworkouts";
    public static final String API_OAUTH_CALLBACK_PATH = "/oauth/withings";

    private static WithingsDevice sInstance = null;
    private Context mContext = null;
    private SQLiteDatabase mDatabase = null;
    private Handler mHandler = null;
    private Map<String, String> mProperties = new HashMap<>();

    public static WithingsDevice getInstance(Context context) {
        if (WithingsDevice.sInstance == null) {
            WithingsDevice.sInstance = new WithingsDevice(context.getApplicationContext());
        }

        return WithingsDevice.sInstance;
    }

    public WithingsDevice(Context context) {
        super(context);

        this.mContext = context.getApplicationContext();

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this.mContext);

        SharedPreferences.Editor e = prefs.edit();
        e.remove(WithingsDevice.LAST_DATA_FETCH);
        e.apply();
    }

    public static void start(final Context context) {
        WithingsDevice.getInstance(context).startGenerator();
    }

    private void startGenerator() {
        final WithingsDevice me = this;

        if (this.mHandler != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                this.mHandler.getLooper().quitSafely();
            } else {
                this.mHandler.getLooper().quit();
            }

            this.mHandler = null;
        }

        final Runnable fetchData = new Runnable() {
            @Override
            public void run() {
                Log.e("PDK", "WITHINGS FETCH DATA");

                final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(me.mContext);
                long fetchInterval = prefs.getLong(WithingsDevice.DATA_FETCH_INTERVAL, WithingsDevice.DATA_FETCH_INTERVAL_DEFAULT);

                if (me.approvalGranted()) {
                    Log.e("PDK", "WITHINGS APPROVED");

                    long lastFetch = prefs.getLong(WithingsDevice.LAST_DATA_FETCH, 0);

                    long now = System.currentTimeMillis();

                    if (now - lastFetch > fetchInterval) {
                        Log.e("PDK", "TIME TO FETCH");

                        Runnable r = new Runnable() {
                            @Override
                            public void run() {
                                if (prefs.getBoolean(WithingsDevice.ACTIVITY_MEASURES_ENABLED, WithingsDevice.ACTIVITY_MEASURES_ENABLED_DEFAULT)) {
                                    me.fetchActivityMeasures();
                                }

                                if (prefs.getBoolean(WithingsDevice.BODY_MEASURES_ENABLED, WithingsDevice.BODY_MEASURES_ENABLED_DEFAULT)) {
                                    me.fetchBodyMeasures();
                                }

                                if (prefs.getBoolean(WithingsDevice.INTRADAY_ACTIVITY_ENABLED, WithingsDevice.INTRADAY_ACTIVITY_ENABLED_DEFAULT)) {
                                    me.fetchIntradayActivities();
                                }

                                if (prefs.getBoolean(WithingsDevice.SLEEP_MEASURES_ENABLED, WithingsDevice.SLEEP_MEASURES_ENABLED_DEFAULT)) {
                                    me.fetchSleepMeasures();
                                }

                                if (prefs.getBoolean(WithingsDevice.SLEEP_SUMMARY_ENABLED, WithingsDevice.SLEEP_SUMMARY_ENABLED_DEFAULT)) {
                                    me.fetchSleepSummary();
                                }

                                if (prefs.getBoolean(WithingsDevice.WORKOUTS_ENABLED, WithingsDevice.WORKOUTS_ENABLED_DEFAULT)) {
                                    me.fetchWorkouts();
                                }
                            }
                        };

                        Thread t = new Thread(r);
                        t.start();

                        SharedPreferences.Editor e = prefs.edit();
                        e.putLong(WithingsDevice.LAST_DATA_FETCH, now);
                        e.apply();
                    } else {
                        Log.e("PDK", "NOT TIME TO FETCH");
                    }
                } else {
                    Log.e("PDK", "WITHINGS NOT APPROVED");
                }

                if (me.mHandler != null) {
                    me.mHandler.postDelayed(this, fetchInterval);
                }
            }
        };

        File path = PassiveDataKit.getGeneratorsStorage(this.mContext);

        path = new File(path, WithingsDevice.DATABASE_PATH);

        this.mDatabase = SQLiteDatabase.openOrCreateDatabase(path, null);

        int version = this.getDatabaseVersion(this.mDatabase);

        switch (version) {
            case 0:
                this.mDatabase.execSQL(this.mContext.getString(R.string.pdk_generator_withings_create_activity_measure_history_table));
                this.mDatabase.execSQL(this.mContext.getString(R.string.pdk_generator_withings_create_body_measure_history_table));
                this.mDatabase.execSQL(this.mContext.getString(R.string.pdk_generator_withings_create_intraday_activity_history_table));
                this.mDatabase.execSQL(this.mContext.getString(R.string.pdk_generator_withings_create_sleep_measure_history_table));
                this.mDatabase.execSQL(this.mContext.getString(R.string.pdk_generator_withings_create_sleep_summary_history_table));
                this.mDatabase.execSQL(this.mContext.getString(R.string.pdk_generator_withings_create_workout_history_table));
        }

        this.setDatabaseVersion(this.mDatabase, WithingsDevice.DATABASE_VERSION);

        Runnable r = new Runnable() {
            @Override
            public void run() {
                Looper.prepare();

                me.mHandler = new Handler();

                Looper.loop();
            }
        };

        Thread t = new Thread(r);
        t.start();

        try {
            Thread.sleep(50);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        me.mHandler.post(fetchData);

        Generators.getInstance(this.mContext).registerCustomViewClass(WithingsDevice.GENERATOR_IDENTIFIER, WithingsDevice.class);
    }

    private String getProperty(String key) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this.mContext);

        if (WithingsDevice.OPTION_OAUTH_ACCESS_TOKEN.equals(key)) {
            return prefs.getString(WithingsDevice.OPTION_OAUTH_ACCESS_TOKEN, null);
        } else if (WithingsDevice.OPTION_OAUTH_ACCESS_TOKEN_SECRET.equals(key)) {
            return prefs.getString(WithingsDevice.OPTION_OAUTH_ACCESS_TOKEN_SECRET, null);
        } else if (WithingsDevice.OPTION_OAUTH_ACCESS_USER_ID.equals(key)) {
            return prefs.getString(WithingsDevice.OPTION_OAUTH_ACCESS_USER_ID, null);
        }

        return this.mProperties.get(key);
    }

    private JSONObject queryApi(String apiUrl) {
        final WithingsDevice me = this;

        String apiKey = this.getProperty(WithingsDevice.OPTION_OAUTH_CONSUMER_KEY);
        String apiSecret = this.getProperty(WithingsDevice.OPTION_OAUTH_CONSUMER_SECRET);
        String token = this.getProperty(WithingsDevice.OPTION_OAUTH_ACCESS_TOKEN);
        String tokenSecret = this.getProperty(WithingsDevice.OPTION_OAUTH_ACCESS_TOKEN_SECRET);

        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);

        String startDate = null;
        String endDate = null;

        long startTime = 0;
        long endTime = 0;

        if (apiKey != null && apiSecret != null && token != null && tokenSecret != null) {
            if (WithingsDevice.API_ACTION_ACTIVITY_URL.equals(apiUrl) ||
                WithingsDevice.API_ACTION_SLEEP_SUMMARY_URL.equals(apiUrl) ||
                WithingsDevice.API_ACTION_WORKOUTS_URL.equals(apiUrl)) {
                SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");

                startDate = format.format(cal.getTime());

                cal.add(Calendar.DATE, 1);

                endDate = format.format(cal.getTime());
            } else if (WithingsDevice.API_ACTION_BODY_MEASURES_URL.equals(apiUrl) ||
                       WithingsDevice.API_ACTION_INTRADAY_ACTIVITY_URL.equals(apiUrl) ||
                       WithingsDevice.API_ACTION_SLEEP_MEASURES_URL.equals(apiUrl)) {

                startTime = cal.getTimeInMillis() / 1000;

                cal.add(Calendar.DATE, 1);

                endTime = cal.getTimeInMillis() / 1000;
            }
            Uri apiUri = Uri.parse(apiUrl);

            Uri.Builder builder = new Uri.Builder();
            builder.scheme(apiUri.getScheme());
            builder.authority(apiUri.getAuthority());
            builder.path(apiUri.getPath());

            try {
                String signature = "GET&" + URLEncoder.encode(builder.build().toString(), "UTF-8");

                String action = apiUri.getQueryParameter("action");
                String nonce = UUID.randomUUID().toString();

                builder.appendQueryParameter("action", action);

                if (endTime != 0) {
                    builder.appendQueryParameter("enddate", "" + endTime);
                }

                if (endDate != null) {
                    builder.appendQueryParameter("enddateymd", endDate);
                }

                builder.appendQueryParameter("oauth_consumer_key", apiKey);
                builder.appendQueryParameter("oauth_nonce", nonce);
                builder.appendQueryParameter("oauth_signature_method", "HMAC-SHA1");
                builder.appendQueryParameter("oauth_timestamp", "" + (System.currentTimeMillis() / 1000));
                builder.appendQueryParameter("oauth_token", token);
                builder.appendQueryParameter("oauth_version", "1.0");

                if (startTime != 0) {
                    builder.appendQueryParameter("startdate", "" + startTime);
                }

                if (startDate != null) {
                    builder.appendQueryParameter("startdateymd", startDate);
                }

                builder.appendQueryParameter("userid", this.getProperty(WithingsDevice.OPTION_OAUTH_ACCESS_USER_ID));

                Uri baseUri = builder.build();

                signature += "&" + URLEncoder.encode(baseUri.getEncodedQuery(), "UTF-8");

                String key = apiSecret + "&" + tokenSecret;

                SecretKeySpec secret = new SecretKeySpec(key.getBytes(), "HmacSHA1");
                Mac mac = Mac.getInstance("HmacSHA1");
                mac.init(secret);

                byte[] bytes = mac.doFinal(signature.getBytes(Charset.forName("UTF-8")));

                signature = Base64.encodeToString(bytes, Base64.DEFAULT);

                builder.appendQueryParameter("oauth_signature", signature.trim());

                Uri uri = builder.build();

                OkHttpClient client = new OkHttpClient();

                Request request = new Request.Builder()
                        .url(uri.toString())
                        .build();

                Response response = client.newCall(request).execute();

                if (response.isSuccessful()) {
                    return new JSONObject(response.body().string());
                }
            } catch (NoSuchAlgorithmException e) {
                AppEvent.getInstance(me.mContext).logThrowable(e);
            } catch (UnsupportedEncodingException e) {
                AppEvent.getInstance(me.mContext).logThrowable(e);
            } catch (IOException e) {
                AppEvent.getInstance(me.mContext).logThrowable(e);
            } catch (InvalidKeyException e) {
                AppEvent.getInstance(me.mContext).logThrowable(e);
            } catch (JSONException e) {
                AppEvent.getInstance(me.mContext).logThrowable(e);
            }
        }

        return null;
    }

    private void fetchActivityMeasures() {
        JSONObject response = this.queryApi(WithingsDevice.API_ACTION_ACTIVITY_URL);

        if (response != null) {
            try {
                if (response.getInt("status") == 0) {
                    JSONObject body = response.getJSONObject("body");
                    JSONArray activities = body.getJSONArray("activities");

                    for (int i = 0; i < activities.length(); i++) {
                        JSONObject activity = activities.getJSONObject(i);

                        Calendar cal = Calendar.getInstance();

                        String[] tokens = activity.getString("date").split("-");

                        cal.set(Calendar.YEAR, Integer.parseInt(tokens[0]));
                        cal.set(Calendar.MONTH, Integer.parseInt(tokens[1]));
                        cal.set(Calendar.DAY_OF_MONTH, Integer.parseInt(tokens[2]));
                        cal.set(Calendar.HOUR_OF_DAY, 0);
                        cal.set(Calendar.MINUTE, 0);
                        cal.set(Calendar.SECOND, 0);
                        cal.set(Calendar.MILLISECOND, 0);

                        ContentValues values = new ContentValues();
                        values.put(WithingsDevice.HISTORY_OBSERVED, System.currentTimeMillis());

                        values.put(WithingsDevice.ACTIVITY_MEASURE_HISTORY_DATE_START, cal.getTimeInMillis());
                        values.put(WithingsDevice.ACTIVITY_MEASURE_HISTORY_TIMEZONE, activity.getString("timezone"));
                        values.put(WithingsDevice.ACTIVITY_MEASURE_STEPS, activity.getDouble("steps"));
                        values.put(WithingsDevice.ACTIVITY_MEASURE_DISTANCE, activity.getDouble("distance"));
                        values.put(WithingsDevice.ACTIVITY_MEASURE_ACTIVE_CALORIES, activity.getDouble("calories"));
                        values.put(WithingsDevice.ACTIVITY_MEASURE_TOTAL_CALORIES, activity.getDouble("totalcalories"));
                        values.put(WithingsDevice.ACTIVITY_MEASURE_ELEVATION, activity.getDouble("elevation"));
                        values.put(WithingsDevice.ACTIVITY_MEASURE_SOFT_ACTIVITY_DURATION, activity.getDouble("soft"));
                        values.put(WithingsDevice.ACTIVITY_MEASURE_MODERATE_ACTIVITY_DURATION, activity.getDouble("moderate"));
                        values.put(WithingsDevice.ACTIVITY_MEASURE_INTENSE_ACTIVITY_DURATION, activity.getDouble("intense"));

                        this.mDatabase.insert(WithingsDevice.TABLE_ACTIVITY_MEASURE_HISTORY, null, values);

                        Bundle updated = new Bundle();

                        updated.putLong(WithingsDevice.HISTORY_OBSERVED, System.currentTimeMillis());
                        updated.putLong(WithingsDevice.ACTIVITY_MEASURE_HISTORY_DATE_START, cal.getTimeInMillis());
                        updated.putString(WithingsDevice.ACTIVITY_MEASURE_HISTORY_TIMEZONE, activity.getString("timezone"));
                        updated.putDouble(WithingsDevice.ACTIVITY_MEASURE_STEPS, activity.getDouble("steps"));
                        updated.putDouble(WithingsDevice.ACTIVITY_MEASURE_DISTANCE, activity.getDouble("distance"));
                        updated.putDouble(WithingsDevice.ACTIVITY_MEASURE_ACTIVE_CALORIES, activity.getDouble("calories"));
                        updated.putDouble(WithingsDevice.ACTIVITY_MEASURE_TOTAL_CALORIES, activity.getDouble("totalcalories"));
                        updated.putDouble(WithingsDevice.ACTIVITY_MEASURE_ELEVATION, activity.getDouble("elevation"));
                        updated.putDouble(WithingsDevice.ACTIVITY_MEASURE_SOFT_ACTIVITY_DURATION, activity.getDouble("soft"));
                        updated.putDouble(WithingsDevice.ACTIVITY_MEASURE_MODERATE_ACTIVITY_DURATION, activity.getDouble("moderate"));
                        updated.putDouble(WithingsDevice.ACTIVITY_MEASURE_INTENSE_ACTIVITY_DURATION, activity.getDouble("intense"));
                        updated.putString(WithingsDevice.DATASTREAM, WithingsDevice.DATASTREAM_ACTIVITY_MEASURES);

                        Generators.getInstance(this.mContext).notifyGeneratorUpdated(WithingsDevice.GENERATOR_IDENTIFIER, updated);
                    }
                }
            } catch (JSONException e) {
                AppEvent.getInstance(this.mContext).logThrowable(e);
            }
        }
    }

    private void fetchBodyMeasures() {
        JSONObject response = this.queryApi(WithingsDevice.API_ACTION_BODY_MEASURES_URL);

        if (response != null) {
            try {
                if (response.getInt("status") == 0) {
                    JSONObject body = response.getJSONObject("body");
                    JSONArray measureGroups = body.getJSONArray("measuregrps");

                    for (int i = 0; i < measureGroups.length(); i++) {
                        JSONObject measureGroup = measureGroups.getJSONObject(i);

                        long measureDate = measureGroup.getLong("date");
                        long now = System.currentTimeMillis();

                        String status = WithingsDevice.BODY_MEASURE_STATUS_UNKNOWN;

                        switch (measureGroup.getInt("attrib")) {
                            case 0:
                                status = WithingsDevice.BODY_MEASURE_STATUS_USER_DEVICE;
                                break;
                            case 1:
                                status = WithingsDevice.BODY_MEASURE_STATUS_SHARED_DEVICE;
                                break;
                            case 2:
                                status = WithingsDevice.BODY_MEASURE_STATUS_MANUAL_ENTRY;
                                break;
                            case 4:
                                status = WithingsDevice.BODY_MEASURE_STATUS_MANUAL_ENTRY_CREATION;
                                break;
                            case 5:
                                status = WithingsDevice.BODY_MEASURE_STATUS_AUTO_DEVICE;
                                break;
                            case 7:
                                status = WithingsDevice.BODY_MEASURE_STATUS_MEASURE_CONFIRMED;
                                break;
                        }

                        String category = WithingsDevice.BODY_MEASURE_CATEGORY_UNKNOWN;

                        switch (measureGroup.getInt("category")) {
                            case 1:
                                category = WithingsDevice.BODY_MEASURE_CATEGORY_REAL_MEASUREMENTS;
                                break;
                            case 2:
                                category = WithingsDevice.BODY_MEASURE_CATEGORY_USER_OBJECTIVES;
                                break;
                        }

                        JSONArray measures = measureGroup.getJSONArray("measures");

                        for (int j = 0; j < measures.length(); j++) {
                            JSONObject measure = measures.optJSONObject(j);

                            ContentValues values = new ContentValues();
                            values.put(WithingsDevice.HISTORY_OBSERVED, now);

                            String type = WithingsDevice.BODY_MEASURE_TYPE_UNKNOWN;

                            switch (measure.getInt("type")) {
                                case 1:
                                    type = WithingsDevice.BODY_MEASURE_TYPE_WEIGHT;
                                    break;
                                case 4:
                                    type = WithingsDevice.BODY_MEASURE_TYPE_HEIGHT;
                                    break;
                                case 5:
                                    type = WithingsDevice.BODY_MEASURE_TYPE_FAT_FREE_MASS;
                                    break;
                                case 6:
                                    type = WithingsDevice.BODY_MEASURE_TYPE_FAT_RATIO;
                                    break;
                                case 8:
                                    type = WithingsDevice.BODY_MEASURE_TYPE_FAT_MASS_WEIGHT;
                                    break;
                                case 9:
                                    type = WithingsDevice.BODY_MEASURE_TYPE_DIASTOLIC_BLOOD_PRESSURE;
                                    break;
                                case 10:
                                    type = WithingsDevice.BODY_MEASURE_TYPE_SYSTOLIC_BLOOD_PRESSURE;
                                    break;
                                case 11:
                                    type = WithingsDevice.BODY_MEASURE_TYPE_HEART_PULSE;
                                    break;
                                case 12:
                                    type = WithingsDevice.BODY_MEASURE_TYPE_TEMPERATURE;
                                    break;
                                case 54:
                                    type = WithingsDevice.BODY_MEASURE_TYPE_OXYGEN_SATURATION;
                                    break;
                                case 71:
                                    type = WithingsDevice.BODY_MEASURE_TYPE_BODY_TEMPERATURE;
                                    break;
                                case 73:
                                    type = WithingsDevice.BODY_MEASURE_TYPE_SKIN_TEMPERATURE;
                                    break;
                                case 76:
                                    type = WithingsDevice.BODY_MEASURE_TYPE_MUSCLE_MASS;
                                    break;
                                case 77:
                                    type = WithingsDevice.BODY_MEASURE_TYPE_HYDRATION;
                                    break;
                                case 88:
                                    type = WithingsDevice.BODY_MEASURE_TYPE_BONE_MASS;
                                    break;
                                case 91:
                                    type = WithingsDevice.BODY_MEASURE_TYPE_PULSE_WAVE_VELOCITY;
                                    break;
                            }

                            double value = measure.getDouble("value") * Math.pow(10, measure.getDouble("unit"));

                            values.put(WithingsDevice.BODY_MEASURE_HISTORY_DATE, measureDate);
                            values.put(WithingsDevice.BODY_MEASURE_HISTORY_STATUS, status);
                            values.put(WithingsDevice.BODY_MEASURE_HISTORY_CATEGORY, category);
                            values.put(WithingsDevice.BODY_MEASURE_HISTORY_TYPE, type);
                            values.put(WithingsDevice.BODY_MEASURE_HISTORY_VALUE, value);

                            this.mDatabase.insert(WithingsDevice.TABLE_BODY_MEASURE_HISTORY, null, values);

                            Bundle updated = new Bundle();

                            updated.putLong(WithingsDevice.HISTORY_OBSERVED, System.currentTimeMillis());
                            updated.putLong(WithingsDevice.BODY_MEASURE_HISTORY_DATE, measureDate);
                            updated.putString(WithingsDevice.BODY_MEASURE_HISTORY_STATUS, status);
                            updated.putString(WithingsDevice.BODY_MEASURE_HISTORY_CATEGORY, category);
                            updated.putString(WithingsDevice.BODY_MEASURE_HISTORY_TYPE, type);
                            updated.putDouble(WithingsDevice.BODY_MEASURE_HISTORY_VALUE, value);

                            Generators.getInstance(this.mContext).notifyGeneratorUpdated(WithingsDevice.GENERATOR_IDENTIFIER, updated);
                        }
                    }
                }
            } catch (JSONException e) {
                AppEvent.getInstance(this.mContext).logThrowable(e);
            }
        }
    }

    private void fetchIntradayActivities() {
        JSONObject response = this.queryApi(WithingsDevice.API_ACTION_INTRADAY_ACTIVITY_URL);

        if (response != null) {
            // TODO
        }
    }

    private void fetchSleepMeasures() {
        JSONObject response = this.queryApi(WithingsDevice.API_ACTION_SLEEP_MEASURES_URL);

        if (response != null) {
            try {
                if (response.getInt("status") == 0) {
                    JSONObject body = response.getJSONObject("body");

                    String model = WithingsDevice.SLEEP_MEASURE_MODEL_UNKNOWN;

                    switch(body.getInt("model")) {
                        case 16:
                            model = WithingsDevice.SLEEP_MEASURE_MODEL_ACTIVITY_TRACKER;
                            break;
                        case 32:
                            model = WithingsDevice.SLEEP_MEASURE_MODEL_AURA;
                            break;
                    }

                    JSONArray series = body.getJSONArray("series");

                    for (int i = 0; i < series.length(); i++) {
                        JSONObject item = series.getJSONObject(i);

                        long now = System.currentTimeMillis();

                        String state = WithingsDevice.SLEEP_MEASURE_STATE_UNKNOWN;

                        switch (item.getInt("state")) {
                            case 0:
                                state = WithingsDevice.SLEEP_MEASURE_STATE_AWAKE;
                                break;
                            case 1:
                                state = WithingsDevice.SLEEP_MEASURE_STATE_LIGHT_SLEEP;
                                break;
                            case 2:
                                state = WithingsDevice.SLEEP_MEASURE_STATE_DEEP_SLEEP;
                                break;
                            case 3:
                                state = WithingsDevice.SLEEP_MEASURE_STATE_REM_SLEEP;
                                break;
                        }

                        ContentValues values = new ContentValues();
                        values.put(WithingsDevice.HISTORY_OBSERVED, now);
                        values.put(WithingsDevice.SLEEP_MEASURE_START_DATE, item.getLong("startdate"));
                        values.put(WithingsDevice.SLEEP_MEASURE_END_DATE, item.getLong("enddate"));
                        values.put(WithingsDevice.SLEEP_MEASURE_STATE, state);
                        values.put(WithingsDevice.SLEEP_MEASURE_MEASUREMENT_DEVICE, model);

                        this.mDatabase.insert(WithingsDevice.TABLE_SLEEP_MEASURE_HISTORY, null, values);

                        Bundle updated = new Bundle();
                        updated.putLong(WithingsDevice.HISTORY_OBSERVED, System.currentTimeMillis());
                        updated.putLong(WithingsDevice.SLEEP_MEASURE_START_DATE, item.getLong("startdate"));
                        updated.putLong(WithingsDevice.SLEEP_MEASURE_END_DATE, item.getLong("enddate"));
                        updated.putString(WithingsDevice.SLEEP_MEASURE_STATE, state);
                        updated.putString(WithingsDevice.SLEEP_MEASURE_MEASUREMENT_DEVICE, model);

                        Generators.getInstance(this.mContext).notifyGeneratorUpdated(WithingsDevice.GENERATOR_IDENTIFIER, updated);
                    }
                }
            } catch (JSONException e) {
                AppEvent.getInstance(this.mContext).logThrowable(e);
            }
        }
    }

    private void fetchSleepSummary() {
        JSONObject response = this.queryApi(WithingsDevice.API_ACTION_SLEEP_SUMMARY_URL);

        if (response != null) {
            try {
                if (response.getInt("status") == 0) {
                    JSONObject body = response.getJSONObject("body");

                    JSONArray series = body.getJSONArray("series");

                    long now = System.currentTimeMillis();

                    for (int i = 0; i < series.length(); i++) {
                        JSONObject item = series.getJSONObject(i);

                        String timezone = body.getString("timezone");

                        String model = WithingsDevice.SLEEP_SUMMARY_MODEL_UNKNOWN;

                        switch(body.getInt("model")) {
                            case 16:
                                model = WithingsDevice.SLEEP_SUMMARY_MODEL_ACTIVITY_TRACKER;
                                break;
                            case 32:
                                model = WithingsDevice.SLEEP_SUMMARY_MODEL_AURA;
                                break;
                        }

                        JSONObject data = item.getJSONObject("data");

                        ContentValues values = new ContentValues();
                        values.put(WithingsDevice.HISTORY_OBSERVED, now);
                        values.put(WithingsDevice.SLEEP_SUMMARY_START_DATE, item.getLong("startdate"));
                        values.put(WithingsDevice.SLEEP_SUMMARY_END_DATE, item.getLong("enddate"));
                        values.put(WithingsDevice.SLEEP_SUMMARY_TIMEZONE, timezone);
                        values.put(WithingsDevice.SLEEP_SUMMARY_MEASUREMENT_DEVICE, model);
                        values.put(WithingsDevice.SLEEP_SUMMARY_WAKE_DURATION, data.getDouble("wakeupduration"));
                        values.put(WithingsDevice.SLEEP_SUMMARY_LIGHT_SLEEP_DURATION, data.getDouble("lightsleepduration"));
                        values.put(WithingsDevice.SLEEP_SUMMARY_DEEP_SLEEP_DURATION, data.getDouble("deepsleepduration"));
                        values.put(WithingsDevice.SLEEP_SUMMARY_TO_SLEEP_DURATION, data.getDouble("durationtosleep"));
                        values.put(WithingsDevice.SLEEP_SUMMARY_WAKE_COUNT, data.getDouble("wakeupcount"));

                        if (data.has("remsleepduration")) {
                            values.put(WithingsDevice.SLEEP_SUMMARY_REM_SLEEP_DURATION, data.getDouble("remsleepduration"));
                        }

                        if (data.has("durationtowakeup")) {
                            values.put(WithingsDevice.SLEEP_SUMMARY_TO_WAKE_DURATION, data.getDouble("durationtowakeup"));
                        }

                        this.mDatabase.insert(WithingsDevice.TABLE_SLEEP_MEASURE_HISTORY, null, values);

                        Bundle updated = new Bundle();
                        updated.putLong(WithingsDevice.HISTORY_OBSERVED, now);
                        updated.putLong(WithingsDevice.SLEEP_SUMMARY_START_DATE, item.getLong("startdate"));
                        updated.putLong(WithingsDevice.SLEEP_SUMMARY_END_DATE, item.getLong("enddate"));
                        updated.putString(WithingsDevice.SLEEP_SUMMARY_TIMEZONE, timezone);
                        updated.putString(WithingsDevice.SLEEP_SUMMARY_MEASUREMENT_DEVICE, model);
                        updated.putDouble(WithingsDevice.SLEEP_SUMMARY_WAKE_DURATION, data.getDouble("wakeupduration"));
                        updated.putDouble(WithingsDevice.SLEEP_SUMMARY_LIGHT_SLEEP_DURATION, data.getDouble("lightsleepduration"));
                        updated.putDouble(WithingsDevice.SLEEP_SUMMARY_DEEP_SLEEP_DURATION, data.getDouble("deepsleepduration"));
                        updated.putDouble(WithingsDevice.SLEEP_SUMMARY_TO_SLEEP_DURATION, data.getDouble("durationtosleep"));
                        updated.putDouble(WithingsDevice.SLEEP_SUMMARY_WAKE_COUNT, data.getDouble("wakeupcount"));

                        if (data.has("remsleepduration")) {
                            updated.putDouble(WithingsDevice.SLEEP_SUMMARY_REM_SLEEP_DURATION, data.getDouble("remsleepduration"));
                        }

                        if (data.has("durationtowakeup")) {
                            updated.putDouble(WithingsDevice.SLEEP_SUMMARY_TO_WAKE_DURATION, data.getDouble("durationtowakeup"));
                        }

                        Generators.getInstance(this.mContext).notifyGeneratorUpdated(WithingsDevice.GENERATOR_IDENTIFIER, updated);
                    }
                }
            } catch (JSONException e) {
                AppEvent.getInstance(this.mContext).logThrowable(e);
            }
        }
    }

    private void fetchWorkouts() {
        JSONObject response = this.queryApi(WithingsDevice.API_ACTION_WORKOUTS_URL);

        if (response != null) {

        }
    }

    private boolean approvalGranted() {
        String apiToken = this.getProperty(WithingsDevice.OPTION_OAUTH_ACCESS_TOKEN);

        if (apiToken != null) {
            return true;
        }

        return false;
    }

    public static boolean isEnabled(Context context) {
        SharedPreferences prefs = Generators.getInstance(context).getSharedPreferences(context);

        return prefs.getBoolean(WithingsDevice.ENABLED, WithingsDevice.ENABLED_DEFAULT);
    }

    public static boolean isRunning(Context context) {
        if (WithingsDevice.sInstance == null) {
            return false;
        }

        return WithingsDevice.sInstance.mHandler != null;
    }

    public static ArrayList<DiagnosticAction> diagnostics(final Context context) {
        final WithingsDevice me = WithingsDevice.getInstance(context);

        ArrayList<DiagnosticAction> actions = new ArrayList<>();

        actions.add(new DiagnosticAction(context.getString(R.string.diagnostic_withings_auth_required_title), context.getString(R.string.diagnostic_withings_auth_required), new Runnable() {

            @Override
            public void run() {
                Runnable r = new Runnable() {
                    @Override
                    public void run() {
                        try {
                            String requestUrl = "https://oauth.withings.com/account/request_token";

                            Uri apiUri = Uri.parse(requestUrl);

                            Uri.Builder builder = new Uri.Builder();
                            builder.scheme(apiUri.getScheme());
                            builder.authority(apiUri.getAuthority());
                            builder.path(apiUri.getPath());

                            String signature = "GET&" + URLEncoder.encode(builder.build().toString(), "UTF-8");

                            String callbackUrl = me.getProperty(WithingsDevice.OPTION_OAUTH_CALLBACK_URL);
                            String apiKey = me.getProperty(WithingsDevice.OPTION_OAUTH_CONSUMER_KEY);
                            String apiSecret = me.getProperty(WithingsDevice.OPTION_OAUTH_CONSUMER_SECRET);

                            String nonce = UUID.randomUUID().toString();

                            builder.appendQueryParameter("oauth_callback", callbackUrl);
                            builder.appendQueryParameter("oauth_consumer_key", apiKey);
                            builder.appendQueryParameter("oauth_nonce", nonce);
                            builder.appendQueryParameter("oauth_signature_method", "HMAC-SHA1");
                            builder.appendQueryParameter("oauth_timestamp", "" + (System.currentTimeMillis() / 1000));
                            builder.appendQueryParameter("oauth_version", "1.0");

                            Uri baseUri = builder.build();

                            signature += "&" + URLEncoder.encode(baseUri.getEncodedQuery(), "UTF-8");

                            String key = apiSecret + "&";

                            SecretKeySpec secret = new SecretKeySpec(key.getBytes(), "HmacSHA1");
                            Mac mac = Mac.getInstance("HmacSHA1");
                            mac.init(secret);

                            byte[] bytes = mac.doFinal(signature.getBytes());

                            signature = Base64.encodeToString(bytes, Base64.DEFAULT).trim();

                            builder.appendQueryParameter("oauth_signature", signature);

                            Uri uri = builder.build();

                            OkHttpClient client = new OkHttpClient();

                            Request request = new Request.Builder()
                                    .url(uri.toString())
                                    .build();

                            Response response = client.newCall(request).execute();

                            String responseBody = response.body().string();

                            StringTokenizer st = new StringTokenizer(responseBody, "&");

                            String requestToken = null;
                            String requestSecret = null;

                            while (st.hasMoreTokens()) {
                                String authToken = st.nextToken();

                                if (authToken.startsWith("oauth_token=")) {
                                    requestToken = authToken.replace("oauth_token=", "");
                                } else if (authToken.startsWith("oauth_token_secret=")) {
                                    requestSecret = authToken.replace("oauth_token_secret=", "");
                                }
                            }

                            key = apiSecret + "&" + requestSecret;

                            me.setProperty(WithingsDevice.OPTION_OAUTH_REQUEST_SECRET, requestSecret);

                            builder = new Uri.Builder();
                            builder.scheme("https");
                            builder.authority("oauth.withings.com");
                            builder.path("/account/authorize");

                            signature = "GET&" + URLEncoder.encode(builder.build().toString(), "UTF-8");

                            nonce = UUID.randomUUID().toString();

                            builder.appendQueryParameter("oauth_consumer_key", apiKey);
                            builder.appendQueryParameter("oauth_nonce", nonce);
                            builder.appendQueryParameter("oauth_signature_method", "HMAC-SHA1");
                            builder.appendQueryParameter("oauth_timestamp", "" + (System.currentTimeMillis() / 1000));
                            builder.appendQueryParameter("oauth_token", requestToken);
                            builder.appendQueryParameter("oauth_version", "1.0");

                            baseUri = builder.build();

                            signature += "&" + URLEncoder.encode(baseUri.getEncodedQuery(), "UTF-8");

                            secret = new SecretKeySpec(key.getBytes(), "HmacSHA1");
                            mac = Mac.getInstance("HmacSHA1");
                            mac.init(secret);

                            bytes = mac.doFinal(signature.getBytes(Charset.forName("UTF-8")));

                            signature = Base64.encodeToString(bytes, Base64.DEFAULT);

                            builder.appendQueryParameter("oauth_signature", signature.trim());

                            Intent intent = new Intent(Intent.ACTION_VIEW, builder.build());
                            context.startActivity(intent);
                        } catch (NoSuchAlgorithmException e) {
                            AppEvent.getInstance(context).logThrowable(e);
                        } catch (InvalidKeyException e) {
                            AppEvent.getInstance(context).logThrowable(e);
                        } catch (UnsupportedEncodingException e) {
                            AppEvent.getInstance(context).logThrowable(e);
                        } catch (IOException e) {
                            AppEvent.getInstance(context).logThrowable(e);
                        }
                    }
                };

                Thread t = new Thread(r);
                t.start();
            }
        }));

        return actions;
    }

    public void finishAuthentication(final Uri responseUri) {
        final WithingsDevice me = this;

        Runnable r = new Runnable() {
            @Override
            public void run() {
                try {
                    String requestUrl = "https://oauth.withings.com/account/access_token";

                    Uri apiUri = Uri.parse(requestUrl);

                    Uri.Builder builder = new Uri.Builder();
                    builder.scheme(apiUri.getScheme());
                    builder.authority(apiUri.getAuthority());
                    builder.path(apiUri.getPath());

                    String signature = "GET&" + URLEncoder.encode(builder.build().toString(), "UTF-8");

                    String callbackUrl = me.getProperty(WithingsDevice.OPTION_OAUTH_CALLBACK_URL);
                    String apiKey = me.getProperty(WithingsDevice.OPTION_OAUTH_CONSUMER_KEY);
                    String apiSecret = me.getProperty(WithingsDevice.OPTION_OAUTH_CONSUMER_SECRET);

                    String nonce = UUID.randomUUID().toString();

                    builder.appendQueryParameter("oauth_consumer_key", apiKey);
                    builder.appendQueryParameter("oauth_nonce", nonce);
                    builder.appendQueryParameter("oauth_signature_method", "HMAC-SHA1");
                    builder.appendQueryParameter("oauth_timestamp", "" + (System.currentTimeMillis() / 1000));
                    builder.appendQueryParameter("oauth_token", responseUri.getQueryParameter("oauth_token"));
                    builder.appendQueryParameter("oauth_version", "1.0");

                    Uri baseUri = builder.build();

                    signature += "&" + URLEncoder.encode(baseUri.getEncodedQuery(), "UTF-8");

                    String key = apiSecret + "&" + me.getProperty(WithingsDevice.OPTION_OAUTH_REQUEST_SECRET);

                    SecretKeySpec secret = new SecretKeySpec(key.getBytes(), "HmacSHA1");
                    Mac mac = Mac.getInstance("HmacSHA1");
                    mac.init(secret);

                    byte[] bytes = mac.doFinal(signature.getBytes());

                    signature = Base64.encodeToString(bytes, Base64.DEFAULT).trim();

                    builder.appendQueryParameter("oauth_signature", signature);

                    Uri uri = builder.build();

                    OkHttpClient client = new OkHttpClient();

                    Request request = new Request.Builder()
                            .url(uri.toString())
                            .build();

                    Response response = client.newCall(request).execute();

                    String responseBody = response.body().string();

                    StringTokenizer st = new StringTokenizer(responseBody, "&");

                    while (st.hasMoreTokens()) {
                        String tokenString = st.nextToken();

                        if (tokenString.startsWith("oauth_token=")) {
                            me.setProperty(WithingsDevice.OPTION_OAUTH_ACCESS_TOKEN, tokenString.replace("oauth_token=", ""));
                        } else if (tokenString.startsWith("oauth_token_secret=")) {
                            me.setProperty(WithingsDevice.OPTION_OAUTH_ACCESS_TOKEN_SECRET, tokenString.replace("oauth_token_secret=", ""));
                        } else if (tokenString.startsWith("userid=")) {
                            me.setProperty(WithingsDevice.OPTION_OAUTH_ACCESS_USER_ID, tokenString.replace("userid=", ""));
                        }
                    }

                    me.fetchActivityMeasures();

                } catch (UnsupportedEncodingException e) {
                    AppEvent.getInstance(me.mContext).logThrowable(e);
                } catch (NoSuchAlgorithmException e) {
                    AppEvent.getInstance(me.mContext).logThrowable(e);
                } catch (IOException e) {
                    AppEvent.getInstance(me.mContext).logThrowable(e);
                } catch (InvalidKeyException e) {
                    AppEvent.getInstance(me.mContext).logThrowable(e);
                }
            }
        };

        Thread t = new Thread(r);
        t.start();
    }

    public static void bindViewHolder(DataPointViewHolder holder) {
/*
        final Context context = holder.itemView.getContext();

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

        long lastTimestamp = 0;
        long lastDuration = 0;
        String callType = null;

        long totalIncoming = 0;
        long totalOutgoing = 0;
        long totalMissed = 0;
        long total = 0;

        WithingsDevice generator = WithingsDevice.getInstance(holder.itemView.getContext());

        View cardContent = holder.itemView.findViewById(R.id.card_content);
        View cardEmpty = holder.itemView.findViewById(R.id.card_empty);
        TextView dateLabel = (TextView) holder.itemView.findViewById(R.id.generator_data_point_date);
*/
    }

    @Override
    public List<Bundle> fetchPayloads() {
        return new ArrayList<>();
    }

    public static View fetchView(ViewGroup parent)
    {
        return LayoutInflater.from(parent.getContext()).inflate(R.layout.card_generator_withings_device, parent, false);
    }

    public static long latestPointGenerated(Context context) {
        long timestamp = 0;

        WithingsDevice me = WithingsDevice.getInstance(context);

//        Cursor c = me.mDatabase.query(WithingsDevice.TABLE_HISTORY, null, null, null, null, null, WithingsDevice.HISTORY_OBSERVED + " DESC");
//
//        if (c.moveToNext()) {
//            timestamp = c.getLong(c.getColumnIndex(WithingsDevice.HISTORY_OBSERVED));
//        }
//
//        c.close();

        return timestamp;
    }

    public void setProperty(String key, String value) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this.mContext);

        if (WithingsDevice.OPTION_OAUTH_ACCESS_TOKEN.equals(key)) {
            SharedPreferences.Editor e = prefs.edit();
            e.putString(WithingsDevice.OPTION_OAUTH_ACCESS_TOKEN, value);
            e.apply();
        } else if (WithingsDevice.OPTION_OAUTH_ACCESS_TOKEN_SECRET.equals(key)) {
            SharedPreferences.Editor e = prefs.edit();
            e.putString(WithingsDevice.OPTION_OAUTH_ACCESS_TOKEN_SECRET, value);
            e.apply();
        } else if (WithingsDevice.OPTION_OAUTH_ACCESS_USER_ID.equals(key)) {
            SharedPreferences.Editor e = prefs.edit();
            e.putString(WithingsDevice.OPTION_OAUTH_ACCESS_USER_ID, value);
            e.apply();
        }

        this.mProperties.put(key, value);
    }
}