package jakubweg.mobishit;

import android.app.Application;
import android.support.v7.app.AppCompatDelegate;
import android.util.Log;

import com.google.firebase.FirebaseApp;
import com.squareup.leakcanary.LeakCanary;

import jakubweg.mobishit.helper.CrashHandler;
import jakubweg.mobishit.helper.MobiregPreferences;

public class MobishitApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        if (LeakCanary.isInAnalyzerProcess(this)) {
            return;
        }
        LeakCanary.install(this);
        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true);
        FirebaseApp.initializeApp(this);


        if (!BuildConfig.DEBUG) {
            MobiregPreferences prefs = MobiregPreferences.Companion.get(getApplicationContext());

            long lastCrash = prefs.getLastCrashTime();
            if (lastCrash + 20 * 1000L < System.currentTimeMillis())
                Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {

                    @SuppressWarnings("ResultOfMethodCallIgnored")
                    @Override
                    public void uncaughtException(Thread thread, Throwable exception) {
                        try {
                            Log.e("FATAL EXCEPTION", "Uncaught exception crashed Mobishit", exception);
                            MobiregPreferences prefs = MobiregPreferences.Companion.get(getApplicationContext());

                            prefs.setLastCrashTime(System.currentTimeMillis());
                            CrashHandler.INSTANCE.onNewCrash(getApplicationContext(), thread, exception);
                        } catch (Throwable e) {
                            e.printStackTrace();
                            throw new RuntimeException(e);
                        }
                    }
                });
        }
    }
}
