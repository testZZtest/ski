package ski.agent;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

/**
 * Bank mode: the phone's banks refuse to run while any accessibility service is on — Ski's is registered even when
 * it does nothing, and the bank's check sees only that it's there. So this turns Ski's accessibility service off in
 * the system's own list (settings put secure enabled_accessibility_services), which ends the bubble, the agent and
 * every phone tool until the user turns it back on: to the bank there is then no accessibility service at all.
 *
 * Toggling the secure setting needs the shell user: Shizuku does it (see Shizuku.runShell). Without Shizuku, this
 * opens the system's accessibility screen so the user flips Ski's switch themselves.
 *
 * Never automatic: the service comes back only when the user taps "Turn Ski back on" (the notification posted here,
 * or the switch in Settings). While Ski is off, this app is an ordinary one — the agent can't be asked to turn it
 * back on, because there is no agent.
 */
final class BankMode {
    /** Ski's accessibility service, as the secure settings list spells it. */
    static final String COMPONENT = "ski.agent/.SkiService";
    private static final int ID = 12;

    private BankMode() {
    }

    /** Bank mode is on: Ski's accessibility service is off (the bubble is gone too, so no one asks this in-app). */
    static boolean on(Context c) {
        return Prefs.bankMode(c);
    }

    /**
     * Turns Ski's accessibility service off and posts the come-back notification. Returns null when it worked;
     * otherwise the reason (Shizuku missing, a run still going) for the user.
     */
    static String enable(Context c) {
        if (ChatHub.get(c).runningCount() > 0) return "A chat is still working. Stop it first, then turn bank mode on.";
        Shizuku sh = Shizuku.get(c);
        if (!Shizuku.isInstalled(c)) return openAccessibilitySettings(c, true);
        sh.bind();
        // give the bind a moment (it's usually immediate)
        for (int i = 0; i < 20 && !sh.isBound(); i++) {
            try { Thread.sleep(100); } catch (InterruptedException e) { break; }
        }
        if (!sh.isBound()) return openAccessibilitySettings(c, true);
        String list = sh.runShell("settings get secure enabled_accessibility_services").trim();
        // runShell may append an exit-code note; the list itself never has one
        if (list.startsWith("Shizuku")) return openAccessibilitySettings(c, true);
        String rest = remove(list);
        if (rest.equals(list)) return "Ski's accessibility service isn't on in the system settings, so there's nothing "
                + "to turn off. Check Settings → Accessibility on the phone.";
        Prefs.setBankMode(c, true);
        sh.runShell("settings put secure enabled_accessibility_services " + rest);
        // hide Ski from the banks entirely: runtime permissions go too (restored on disable)
        if (sh.ready()) {
            for (String p : RESTRICTED_PERMS) sh.runShell("pm revoke ski.agent " + p);
        }
        // Android unbinds the service now: the bubble, the agent and the phone tools end with it
        postNotice(c);
        return null;
    }

    /** Turns Ski's accessibility service back on (from the notification or Settings). null when it worked. */
    static String disable(Context c) {
        Prefs.setBankMode(c, false);
        c.getSystemService(NotificationManager.class).cancel(ID);
        Shizuku sh = Shizuku.get(c);
        if (!Shizuku.isInstalled(c)) return null; // re-enabled by hand; nothing to write
        sh.bind();
        for (int i = 0; i < 20 && !sh.isBound(); i++) {
            try { Thread.sleep(100); } catch (InterruptedException e) { break; }
        }
        if (!sh.isBound()) return null;
        String list = sh.runShell("settings get secure enabled_accessibility_services").trim();
        String with = add(list);
        if (!with.equals(list)) sh.runShell("settings put secure enabled_accessibility_services " + with);
        if (sh.ready()) { // bring back what bank mode took away
            for (String p : RESTRICTED_PERMS) sh.runShell("pm grant ski.agent " + p);
        }
        return null;
    }

    /** Runtime permissions bank mode hides while it's on (banks probe for them). */
    private static final String[] RESTRICTED_PERMS = {
            "android.permission.RECORD_AUDIO",
            "android.permission.READ_CALENDAR",
            "android.permission.ACCESS_COARSE_LOCATION",
            "android.permission.ACCESS_FINE_LOCATION",
    };

    /** The list without Ski's service. */
    static String remove(String list) {
        if (list == null || list.isEmpty() || "null".equals(list)) return list == null ? "" : list;
        StringBuilder b = new StringBuilder();
        for (String s : list.split(":")) {
            if (!s.trim().isEmpty() && !s.trim().equalsIgnoreCase(COMPONENT)) {
                if (b.length() > 0) b.append(':');
                b.append(s.trim());
            }
        }
        return b.toString();
    }

    /** The list with Ski's service, if it isn't there already. */
    static String add(String list) {
        String base = list == null || list.isEmpty() || "null".equals(list) ? "" : list.trim();
        if (contains(base)) return base;
        return base.isEmpty() ? COMPONENT : base + ":" + COMPONENT;
    }

    static boolean contains(String list) {
        if (list == null) return false;
        for (String s : list.split(":")) if (s.trim().equalsIgnoreCase(COMPONENT)) return true;
        return false;
    }

    /** No Shizuku: the user flips the switch themselves. said: explain why first. */
    private static String openAccessibilitySettings(Context c, boolean said) {
        Prefs.setBankMode(c, true);
        try {
            c.startActivity(new Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        } catch (Exception ignored) {
        }
        return "Shizuku isn't set up, so Ski can't flip the switch itself: Android's accessibility settings just "
                + "opened — find Ski in the list and turn it off. Turn it back on from the notification or Settings "
                + "when you're done with the bank" + (said ? "" : ".");
    }

    /** The shade's notice while Ski is off: one tap brings it back. */
    private static void postNotice(Context c) {
        ControlIndicator.channel(c);
        PendingIntent back = PendingIntent.getBroadcast(c, ID, new Intent(c, HiddenNotice.class).setAction(RESUME),
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        PendingIntent open = PendingIntent.getActivity(c, ID, new Intent(c, MainActivity.class)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP),
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        Notification n = new Notification.Builder(c, ControlIndicator.CHANNEL)
                .setSmallIcon(R.drawable.ic_launcher_monochrome)
                .setContentTitle(c.getString(R.string.bank_mode_title))
                .setContentText(c.getString(R.string.bank_mode_on))
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setContentIntent(open)
                .addAction(new Notification.Action.Builder(null, c.getString(R.string.bank_mode_back), back).build())
                .build();
        try {
            c.getSystemService(NotificationManager.class).notify(ID, n);
        } catch (SecurityException ignored) {
        }
    }

    /** HiddenNotice's action: Ski is back. */
    static final String RESUME = "ski.agent.RESUME_BANK";
}
