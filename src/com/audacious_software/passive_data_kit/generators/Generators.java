package com.audacious_software.passive_data_kit.generators;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.util.SparseArray;

import com.audacious_software.passive_data_kit.Logger;
import com.audacious_software.passive_data_kit.diagnostics.DiagnosticAction;
import com.audacious_software.pdk.passivedatakit.R;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

public class Generators {
    private Context mContext = null;
    private boolean mStarted = false;

    private ArrayList<String> mGenerators = new ArrayList<>();
    private HashSet<String> mActiveGenerators = new HashSet<>();
    private SharedPreferences mSharedPreferences = null;
    private HashMap<String, Class<? extends Generator>> mGeneratorMap = new HashMap<>();
    private SparseArray<Class<? extends Generator>> mViewTypeMap = new SparseArray<>();
    private HashSet<GeneratorUpdatedListener> mGeneratorUpdatedListeners = new HashSet<>();

    public void start() {
        if (!this.mStarted)
        {
            this.mGenerators.clear();

            for (String className : this.mContext.getResources().getStringArray(R.array.pdk_available_generators))
            {
                this.mGenerators.add(className);
            }

            for (String className : this.mContext.getResources().getStringArray(R.array.pdk_app_generators))
            {
                this.mGenerators.add(className);
            }

            for (String className : this.mGenerators)
            {
                try {
                    Log.e("PDK", "TRYING " + className);

                    Class<Generator> probeClass = (Class<Generator>) Class.forName(className);

                    Method isEnabled = probeClass.getDeclaredMethod("isEnabled", Context.class);

                    Boolean enabled = (Boolean) isEnabled.invoke(null, this.mContext);

                    Log.e("PDK", "GENERATOR ENABLED? " + probeClass + " ==> " + enabled);

                    if (enabled) {
                        this.startGenerator(className);
                    }
                    else {
                        this.stopGenerator(className);
                    }
                } catch (ClassNotFoundException e) {
                    Log.e("PDK", "ClassNotFoundException " + className);
                    Logger.getInstance(this.mContext).logThrowable(e);
                } catch (NoSuchMethodException e) {
                    Log.e("PDK", "NoSuchMethodException " + className);
                    Logger.getInstance(this.mContext).logThrowable(e);
                } catch (InvocationTargetException e) {
                    Log.e("PDK", "InvocationTargetException " + className);
                    Logger.getInstance(this.mContext).logThrowable(e);
                } catch (IllegalAccessException e) {
                    Log.e("PDK", "IllegalAccessException " + className);
                    Logger.getInstance(this.mContext).logThrowable(e);
                }
            }

            this.mStarted = true;
        }
    }

    private void startGenerator(String className) throws ClassNotFoundException, NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        if (!this.mActiveGenerators.contains(className)) {
            Class<Generator> generatorClass = (Class<Generator>) Class.forName(className);

            Method isRunning = generatorClass.getDeclaredMethod("isRunning", Context.class);
            Boolean running = (Boolean) isRunning.invoke(null, this.mContext);

            if (running) {
                this.stopGenerator(className);
            }
            else {
                Method start = generatorClass.getDeclaredMethod("start", Context.class);

                Log.e("PDK", "GOT START METHOD: " + className + " -> " + start);

                start.invoke(null, this.mContext);

                this.mActiveGenerators.add(className);
            }
        }
    }

    private void stopGenerator(String className) throws ClassNotFoundException, NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        if (this.mActiveGenerators.contains(className)) {
            Class<Generator> probeClass = (Class<Generator>) Class.forName(className);

            Method stop = probeClass.getDeclaredMethod("stop", probeClass);
            stop.invoke(null, this.mContext);

            this.mActiveGenerators.remove(className);
        }
    }

    public SharedPreferences getSharedPreferences(Context context) {
        if (this.mSharedPreferences == null)
            this.mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);

        return this.mSharedPreferences;
    }

    public ArrayList<DiagnosticAction> diagnostics() {
        ArrayList<DiagnosticAction> actions = new ArrayList<>();

        for (String className : this.mActiveGenerators) {
            try {
                Class<Generator> generatorClass = (Class<Generator>) Class.forName(className);

                Method diagnostics = generatorClass.getDeclaredMethod("diagnostics", Context.class);
                Collection<DiagnosticAction> generatorActions = (Collection<DiagnosticAction>) diagnostics.invoke(null, this.mContext);

                actions.addAll(generatorActions);
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            } catch (NoSuchMethodException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }

        return actions;
    }

    public String getSource() {
        return "unknown-user-please-set-me";
    }

    public String getGeneratorFullName(String identifier) {
        String pdkName = this.mContext.getString(R.string.pdk_name);
        String pdkVersion = this.mContext.getString(R.string.pdk_version);
        String appName = this.mContext.getString(this.mContext.getApplicationInfo().labelRes);

        String version = this.mContext.getString(R.string.unknown_version);

        try {
            PackageInfo pInfo = this.mContext.getPackageManager().getPackageInfo(this.mContext.getPackageName(), 0);

            version = pInfo.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            Logger.getInstance(this.mContext).logThrowable(e);
        }

        return identifier + ": " + appName + "/" + version + " " + pdkName + "/" + pdkVersion;
    }

    public void registerCustomViewClass(String identifier, Class<? extends Generator> generatorClass) {
        this.mGeneratorMap.put(identifier, generatorClass);
        this.mViewTypeMap.put(generatorClass.hashCode(), generatorClass);
    }

    public Class<? extends Generator> fetchCustomViewClass(String identifier) {
        Class<? extends Generator> generatorClass = this.mGeneratorMap.get(identifier);

        Log.e("PDK", "FETCH VIEW: " + identifier + " => " + generatorClass);

        if (generatorClass == null)
            generatorClass = Generator.class;

        return generatorClass;
    }

    public Class<? extends Generator> fetchCustomViewClass(int viewType) {
        Class<? extends Generator> generatorClass = this.mViewTypeMap.get(viewType);

        if (generatorClass == null)
            generatorClass = Generator.class;

        return generatorClass;
    }

    public Generator getGenerator(String className) {
        Log.e("BB", "GENERATOR FIND START");
        for (String name : this.mActiveGenerators) {
            Log.e("BB", "GENERATOR NAME: " + name);
        }
        Log.e("BB", "GENERATOR FIND END");

        if (this.mActiveGenerators.contains(className)) {
            try {
                Class<Generator> probeClass = (Class<Generator>) Class.forName(className);

                Method getInstance = probeClass.getDeclaredMethod("getInstance", Context.class);
                return (Generator) getInstance.invoke(null, this.mContext);
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            } catch (NoSuchMethodException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            }
        }

        return null;
    }

    public List<Class<? extends Generator>> activeGenerators() {
        ArrayList<Class<? extends Generator>> active = new ArrayList<>();

        for (String className : this.mActiveGenerators) {
            try {
                active.add((Class<? extends Generator>) Class.forName(className));
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
        }

        return active;
    }

    public void notifyGeneratorUpdated(String identifier, long timestamp, Bundle bundle) {
        for (GeneratorUpdatedListener listener : this.mGeneratorUpdatedListeners) {
            listener.onGeneratorUpdated(identifier, timestamp, bundle);
        }
    }

    public void notifyGeneratorUpdated(String identifier, Bundle bundle) {
        long timestamp = System.currentTimeMillis();

        for (GeneratorUpdatedListener listener : this.mGeneratorUpdatedListeners) {
            listener.onGeneratorUpdated(identifier, timestamp, bundle);
        }
    }

    private static class GeneratorsHolder {
        public static Generators instance = new Generators();
    }

    public static Generators getInstance(Context context)
    {
        if (context != null) {
            GeneratorsHolder.instance.setContext(context);
        }

        return GeneratorsHolder.instance;
    }

    private void setContext(Context context) {
        this.mContext = context.getApplicationContext();
    }

    public void addNewGeneratorUpdatedListener(Generators.GeneratorUpdatedListener listener) {
        this.mGeneratorUpdatedListeners.add(listener);
    }

    public void removeGeneratorUpdatedListener(Generators.GeneratorUpdatedListener listener) {
        this.mGeneratorUpdatedListeners.remove(listener);
    }

    public interface GeneratorUpdatedListener {
        void onGeneratorUpdated(String identifier, long timestamp, Bundle data);
    }
}
