package ski.agent;

import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;

import java.util.Locale;

/** Base for Ski's screens: applies the theme and rebuilds when the appearance settings change. */
abstract class Screen extends Activity {
    private String themeKey;

    @Override
    protected void onCreate(Bundle state) {
        Theme.apply(this);
        applyLocale(this);
        themeKey = Theme.key();
        super.onCreate(state);
    }

    /**
     * In-app language on Android 12 and below (13+ handles it natively through
     * LocaleManager, set by the picker). Wraps the activity's configuration.
     */
    static void applyLocale(Context c) {
        String tag = Prefs.appLanguage(c);
        if ("sys".equals(tag) || Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) return;
        try {
            Locale loc = tag.contains("-") && !tag.contains("zh")
                    ? new Locale(tag.substring(0, 2), tag.substring(3))
                    : (tag.startsWith("zh-r") ? new Locale("zh", tag.substring(4)) : new Locale(tag));
            Configuration cfg = new Configuration(c.getResources().getConfiguration());
            cfg.setLocale(loc);
            c.getResources().updateConfiguration(cfg, c.getResources().getDisplayMetrics());
        } catch (Exception ignored) {
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        Theme.load(this);
        if (!Theme.key().equals(themeKey)) recreate();
    }

    /** Re-apply the theme in place (after a change made on this screen). */
    protected void rethemed() {
        Theme.apply(this);
        themeKey = Theme.key();
    }
}
