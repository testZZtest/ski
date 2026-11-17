package ski.agent;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.provider.MediaStore;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * Backup and move (Settings): chats, settings, rules, pictures, skills, projects and what the agents keep of each
 * conversation, in one file in Downloads/Ski, to restore on this phone or another. Sign-ins and API keys stay out:
 * they're secrets, and the keys can't leave this phone's keystore anyway.
 */
final class Backup {
    /** Ski's settings and chats (the API key services' list and choices stay out, as their keys do). */
    private static final String[] PREFS = {"ski", "ski_sessions", "browser"};
    /** In Ski's Linux, what to keep (no sign-ins): projects without their builds, skills, the agents' conversations. */
    private static final String LINUX_PARTS = "root/.ski/sessions root/.ski/work root/.agents/skills root/.claude/projects "
            + "root/.codex/sessions root/.local/share/opencode/opencode.db";

    /**
     * Settings that never leave this phone in a backup and are never taken from one: the tokens that open Ski's
     * phone tools and its background helper (anyone holding the file would hold them), and what belongs to this
     * phone's own set-up (its pairing, its API key services).
     */
    private static final java.util.Set<String> OWN = new java.util.HashSet<>(java.util.Arrays.asList(
            "token", "bg_token", "bg_paired", "bg_adb_port", "providers"));
    /**
     * Settings a backup can't loosen: on restore this phone's own values stay. A backup is a file from outside, and
     * one made to harm (or just made on a phone set up differently) shouldn't switch on phone commands, Linux tools
     * or Trust for everything, add an MCP server or a "never ask" button.
     */
    private static final java.util.Set<String> SAFETY = new java.util.HashSet<>(java.util.Arrays.asList(
            "bg_shell", "bg_shell_ask", "agents_tap_ski", "dev", "autonomous_default", "guard_builtin", "guard_allow",
            "ssh_server", "remote_session", "codex_remote", "mcp_servers", "ask_new_apps"));

    private Backup() {
    }

    /** Writes the backup to Downloads/Ski and returns its name. */
    static String create(Context c) throws Exception {
        String name = "ski-backup-" + new SimpleDateFormat("yyyy-MM-dd-HHmm", Locale.ROOT).format(new Date()) + ".zip";
        File tmp = new File(c.getCacheDir(), "backup.zip");
        File linux = new File(c.getCacheDir(), "linux.tar.gz");
        linux.delete();
        if (Linux.installed(c)) {
            String out = Linux.exec(c, "cd / && rm -f /tmp/ski-backup.tar.gz; p=$(ls -d " + LINUX_PARTS + " 2>/dev/null); "
                    + "[ -z \"$p\" ] || tar czf /tmp/ski-backup.tar.gz --exclude=node_modules --exclude=build "
                    + "--exclude=.gradle --exclude=.cxx $p 2>&1 && echo DONE", 1800);
            File made = new File(Linux.root(c), "tmp/ski-backup.tar.gz");
            if (!out.contains("DONE")) throw new Exception(out);
            if (made.isFile()) {
                if (!made.renameTo(linux)) copyFile(made, linux);
                made.delete();
            }
        }
        File prefsDir = new File(c.getApplicationInfo().dataDir, "shared_prefs");
        try (ZipOutputStream z = new ZipOutputStream(new FileOutputStream(tmp))) {
            z.putNextEntry(new ZipEntry("ski-backup.txt"));
            z.write(("Ski " + BuildInfo.VERSION + " backup\n").getBytes());
            for (String p : PREFS) {
                File f = new File(prefsDir, p + ".xml");
                if (!f.isFile()) continue;
                String xml = new String(java.nio.file.Files.readAllBytes(f.toPath()), "UTF-8");
                // services and their choices only make sense with their keys, which stay on this phone; nor the
                // tokens and this phone's pairing (see OWN)
                if (p.equals("ski")) xml = xml.replaceAll("\\s*<string name=\"(providers|agent_default_[a-z]+)\">[^<]*</string>", "")
                        .replaceAll("\\s*<string name=\"token\">[^<]*</string>", "")
                        .replaceAll("\\s*<string name=\"bg_token\">[^<]*</string>", "")
                        .replaceAll("\\s*<(boolean|int) name=\"(bg_paired|bg_adb_port)\"[^>]*/>", "");
                z.putNextEntry(new ZipEntry("prefs/" + p + ".xml"));
                z.write(xml.getBytes("UTF-8"));
            }
            addTree(z, new File(c.getFilesDir(), "attachments"), "files/attachments/");
            File face = new File(c.getFilesDir(), "face.png");
            if (face.isFile()) addFile(z, face, "files/face.png");
            if (linux.isFile()) addFile(z, linux, "linux.tar.gz");
        } finally {
            linux.delete();
        }
        ContentValues v = new ContentValues();
        v.put(MediaStore.MediaColumns.DISPLAY_NAME, name);
        v.put(MediaStore.MediaColumns.RELATIVE_PATH, "Download/Ski");
        v.put(MediaStore.MediaColumns.MIME_TYPE, "application/zip");
        Uri u = c.getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, v);
        if (u == null) throw new Exception("Couldn't write to Downloads.");
        try (OutputStream o = c.getContentResolver().openOutputStream(u); InputStream in = new FileInputStream(tmp)) {
            pump(in, o);
        } finally {
            tmp.delete();
        }
        return name;
    }

    /**
     * Puts a backup's chats, settings and files in place of this phone's, then restarts Ski so they're read fresh.
     * Its Linux part needs Ski's Linux set up first: false when it was skipped for that.
     */
    static boolean restore(Context c, Uri uri) throws Exception {
        File prefsDir = new File(c.getApplicationInfo().dataDir, "shared_prefs");
        File staging = new File(c.getCacheDir(), "restore");
        Linux.deleteTree(staging);
        staging.mkdirs();
        boolean ok = false, linuxDone = false;
        String root = staging.getCanonicalPath() + File.separator;
        try (InputStream in = c.getContentResolver().openInputStream(uri); ZipInputStream z = new ZipInputStream(in)) {
            for (ZipEntry e; (e = z.getNextEntry()) != null; ) {
                if (e.isDirectory()) continue;
                File out = new File(staging, e.getName());
                if (!out.getCanonicalPath().startsWith(root)) continue;
                out.getParentFile().mkdirs();
                try (OutputStream o = new FileOutputStream(out)) {
                    pump(z, o);
                }
                if (e.getName().equals("ski-backup.txt")) ok = true;
            }
        }
        if (!ok) throw new IllegalArgumentException(c.getString(R.string.backup_not_one));
        File linux = new File(staging, "linux.tar.gz");
        if (linux.isFile() && Linux.installed(c)) {
            File in = new File(Linux.root(c), "tmp/ski-restore.tar.gz");
            copyFile(linux, in);
            // only the parts a backup holds, and nothing that reaches outside them: the archive is a file from
            // outside, and unpacked as it is at / it could write over Ski's own scripts. So: every name must sit
            // under one of the parts, it is unpacked into a folder of its own first, links that end outside the
            // parts are dropped there, and only then it goes into place
            String allowed = "^(\\./)?(" + LINUX_PARTS.replace(".", "\\.").replace(" ", "|") + ")(/|$)";
            String out = Linux.exec(c, "cd / && d=/tmp/ski-restore.d && rm -rf $d && mkdir -p $d && f=/tmp/ski-restore.tar.gz && "
                    + "if tar tzf $f | grep -Ev '" + allowed + "' | grep -q . || tar tzf $f | grep -Eq '(^|/)\\.\\.(/|$)'; "
                    + "then echo UNSAFE; else tar xzf $f -C $d && "
                    // a link stays when it ends inside one of the parts (a project's "current -> ../releases/v2")
                    + "D=$(readlink -f $d); find $d -type l | while IFS= read -r l; do t=$(readlink \"$l\"); "
                    + "case \"$t\" in /*) rm -f \"$l\"; continue;; esac; "
                    + "r=$(cd \"$(dirname \"$l\")\" && readlink -f \"$t\" 2>/dev/null); "
                    + "k=; for q in " + LINUX_PARTS + "; do case \"$r\" in \"$D/$q\"|\"$D/$q\"/*) k=1;; esac; done; "
                    + "[ -n \"$k\" ] || rm -f \"$l\"; done; "
                    // the parts only, so the folders above them (/, /root) keep their own modes
                    + "(cd $d && p=$(ls -d " + LINUX_PARTS + " 2>/dev/null); [ -z \"$p\" ] || tar cf - $p) | tar xf - -C / && echo DONE; fi; "
                    + "rm -rf $d $f", 1800);
            if (out.contains("UNSAFE")) throw new IllegalArgumentException(c.getString(R.string.backup_not_one));
            linuxDone = out.contains("DONE");
        }
        File att = new File(staging, "files/attachments");
        if (att.isDirectory()) {
            File dest = new File(c.getFilesDir(), "attachments");
            dest.mkdirs();
            File[] fs = att.listFiles();
            if (fs != null) for (File f : fs) if (f.isFile()) copyFile(f, new File(dest, f.getName()));
        }
        File face = new File(staging, "files/face.png");
        if (face.isFile()) copyFile(face, new File(c.getFilesDir(), "face.png"));
        // this phone's own token, keys' services and choices stay; the rest is the backup's
        String token = Prefs.token(c), providers = Prefs.providers(c);
        java.util.Map<String, String> defaults = new java.util.HashMap<>();
        for (Account a : Account.ALL) defaults.put(a.agent, Prefs.agentDefault(c, a.agent));
        for (String p : PREFS) {
            File f = new File(staging, "prefs/" + p + ".xml");
            if (!f.isFile()) continue;
            String xml = new String(java.nio.file.Files.readAllBytes(f.toPath()), "UTF-8");
            if (p.equals("ski")) {
                StringBuilder keep = new StringBuilder("<string name=\"token\">" + esc(token) + "</string>\n")
                        .append("<string name=\"providers\">").append(esc(providers)).append("</string>\n");
                for (java.util.Map.Entry<String, String> d : defaults.entrySet()) {
                    keep.append("<string name=\"agent_default_").append(d.getKey()).append("\">").append(esc(d.getValue())).append("</string>\n");
                }
                // a backup can't loosen this phone's safety settings (see SAFETY and stays): their values here stay
                xml = xml.replaceAll("\\s*<string name=\"(token|providers|agent_default_[a-z]+)\">[^<]*</string>", "")
                        .replace("</map>", keep + "</map>");
                xml = dropSafety(c, xml);
            }
            c.getSharedPreferences(p, Context.MODE_PRIVATE); // loaded, so nothing pending writes over the new file later
            try (OutputStream o = new FileOutputStream(new File(prefsDir, p + ".xml"))) {
                o.write(xml.getBytes("UTF-8"));
            }
        }
        boolean skipped = linux.isFile() && !linuxDone;
        Linux.deleteTree(staging);
        return !skipped;
    }

    /**
     * Takes the safety settings out of a backup's prefs XML: whatever decides what an agent may do (SAFETY, the
     * per-app modes except the stricter of the two, where downloads and requests go) keeps this phone's value.
     * The XML keeps entries only for the keys this phone has, so nothing is loosened and nothing new is added.
     */
    private static String dropSafety(Context c, String xml) throws java.io.IOException {
        android.content.SharedPreferences sp = c.getSharedPreferences("ski", Context.MODE_PRIVATE);
        java.util.Map<String, ?> mine = sp.getAll();
        StringBuilder out = new StringBuilder();
        java.util.regex.Pattern entry = java.util.regex.Pattern.compile(
                "\\s*<(string|boolean|int|long|float|set) name=\"([^\"]+)\"[^>]*>([\\s\\S]*?)</\\1>|\\s*<(string|boolean|int|long|float) name=\"([^\"]+)\"[^>]*/>");
        java.util.regex.Matcher m = entry.matcher(xml);
        int start = xml.indexOf("<map");
        out.append(xml, 0, xml.indexOf('>', start) + 1);
        while (m.find()) {
            String key = m.group(2) != null ? m.group(2) : m.group(5);
            if (stays(key) || key.startsWith("app_safety_") || key.equals("guard_ask") || key.equals("rules")
                    || key.startsWith("mirror") || key.startsWith("api_base_") || key.startsWith("reviewer")) {
                Object here = mine.get(key);
                if (here == null) continue; // this phone has none: the backup's is not taken
                // this phone's own value, written back in its place
                String value = esc(String.valueOf(here));
                if (here instanceof Boolean) out.append("\n    <boolean name=\"").append(key).append("\">").append(value).append("</boolean>");
                else if (here instanceof Integer) out.append("\n    <int name=\"").append(key).append("\">").append(value).append("</int>");
                else if (here instanceof Long) out.append("\n    <long name=\"").append(key).append("\">").append(value).append("</long>");
                else out.append("\n    <string name=\"").append(key).append("\">").append(value).append("</string>");
                continue;
            }
            out.append(m.group(0));
        }
        out.append("\n</map>");
        return out.toString();
    }

    /** A setting that is never taken from a backup: this phone's own value stays (or none). */
    private static boolean stays(String k) {
        return OWN.contains(k) || SAFETY.contains(k) || k.startsWith("agent_default_");
    }

    /** Starts Ski again in a new process, so the restored settings and chats are what it reads. */
    static void restart(Context c) {
        Intent i = new Intent(c, MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        android.app.PendingIntent pi = android.app.PendingIntent.getActivity(c, 0, i,
                android.app.PendingIntent.FLAG_IMMUTABLE | android.app.PendingIntent.FLAG_CANCEL_CURRENT);
        android.app.AlarmManager am = (android.app.AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
        am.set(android.app.AlarmManager.RTC, System.currentTimeMillis() + 800, pi);
        android.os.Process.killProcess(android.os.Process.myPid());
    }

    private static String esc(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }

    private static void addTree(ZipOutputStream z, File dir, String prefix) throws Exception {
        File[] fs = dir.listFiles();
        if (fs == null) return;
        for (File f : fs) {
            if (f.isDirectory()) addTree(z, f, prefix + f.getName() + "/");
            else addFile(z, f, prefix + f.getName());
        }
    }

    private static void addFile(ZipOutputStream z, File f, String name) throws Exception {
        z.putNextEntry(new ZipEntry(name));
        try (InputStream in = new FileInputStream(f)) {
            pump(in, z);
        }
        z.closeEntry();
    }

    /** in to out; neither is closed. */
    private static void pump(InputStream in, OutputStream out) throws Exception {
        byte[] buf = new byte[64 * 1024];
        int n;
        while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
    }

    private static void copyFile(File from, File to) throws Exception {
        try (InputStream in = new FileInputStream(from); OutputStream out = new FileOutputStream(to)) {
            pump(in, out);
        }
    }
}
