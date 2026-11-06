package app.buddy.assistant;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

/**
 * Shizuku support: binds to the Shizuku service (moe.shizuku.privileged.API) so the agent
 * can run shell commands with elevated privileges (no root needed, granted via ADB or the
 * Shizuku app itself).
 *
 * Shizuku must be installed on the device and the user must grant Buddy permission through it.
 * This class wraps the binding; callers use runShell() to execute commands.
 */
public final class Shizuku {

    private static final String TAG = "Buddy.Shizuku";
    /** Shizuku's privileged service component. */
    private static final String SHIZUKU_PACKAGE = "moe.shizuku.privileged";
    private static final String SHIZUKU_SERVICE = SHIZUKU_PACKAGE + "/moe.shizuku.privileged.API";

    /** Whether Shizuku appears to be installed (package present). */
    public static boolean isInstalled(Context c) {
        try {
            c.getPackageManager().getPackageInfo(SHIZUKU_PACKAGE, 0);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /** Whether we're currently bound to Shizuku. */
    private boolean bound = false;
    private Object binder; // IShizukuService (AIDL) - set via reflection to avoid hard dep
    private final Context c;
    private final ServiceConnection conn = new ServiceConnection() {
        @Override public void onServiceConnected(ComponentName name, IBinder service) {
            binder = service; // callers use reflection through this field
            bound = true;
            Log.d(TAG, "Shizuku bound");
        }

        @Override public void onServiceDisconnected(ComponentName name) {
            binder = null;
            bound = false;
            Log.d(TAG, "Shizuku unbound");
        }
    };

    private Shizuku(Context c) { this.c = c; }

    private static volatile Shizuku instance;

    public static Shizuku get(Context c) {
        if (instance == null) instance = new Shizuku(c.getApplicationContext());
        return instance;
    }

    /** Try to bind to Shizuku. Returns true if binding started (not yet bound). */
    public boolean bind() {
        if (bound || !isInstalled(c)) return false;
        Intent i = new Intent(SHIZUKU_PACKAGE + ".API");
        i.setPackage(SHIZUKU_PACKAGE);
        boolean ok = c.bindService(i, conn, Context.BIND_AUTO_CREATE);
        Log.d(TAG, "Shizuku bind requested: " + ok);
        return ok;
    }

    public void unbind() {
        if (bound) {
            try { c.unbindService(conn); } catch (Exception ignored) {}
            bound = false;
            binder = null;
        }
    }

    /** Whether a call can be made right now (bound and ready). */
    public boolean isBound() { return bound && binder != null; }

    /**
     * Run a shell command through Shizuku (elevated privileges). Returns the combined stdout/stderr.
     * Uses reflection on the AIDL interface so we don't need Shizuku's classes at compile time.
     */
    public String runShell(String cmd) {
        if (!isBound()) return "Shizuku not bound (call bind() first)";
        try {
            // The AIDL interface: moe.shizuku.privileged.IShizukuService
            Class<?> shizukuCls = Class.forName("moe.shizuku.privileged.IShizukuService");
            // asInterface(IBinder) stub
            java.lang.reflect.Method asInterface = shizukuCls.getMethod("asInterface", IBinder.class);
            Object service = asInterface.invoke(null, (IBinder) binder);
            // executeShellCommand(String) -> String (or ParcelFileDescriptor in some versions)
            java.lang.reflect.Method exec = shizukuCls.getMethod("executeShellCommand", String.class);
            Object result = exec.invoke(service, cmd);
            if (result instanceof String) return (String) result;
            if (result == null) return "";
            return result.toString();
        } catch (NoSuchMethodException e) {
            // Try the older execute(ParcelFileDescriptor...) variant
            return runShellV1(cmd);
        } catch (Exception e) {
            Log.e(TAG, "Shizuku shell failed", e);
            return "Shizuku error: " + e.getMessage();
        }
    }

    private String runShellV1(String cmd) {
        try {
            Class<?> shizukuCls = Class.forName("moe.shizuku.privileged.IShizukuService");
            java.lang.reflect.Method asInterface = shizukuCls.getMethod("asInterface", IBinder.class);
            Object service = asInterface.invoke(null, (IBinder) binder);
            // Older: execute(ParcelFileDescriptor[] fds) writes to fd
            java.lang.reflect.Method exec = shizukuCls.getMethod("execute", android.os.ParcelFileDescriptor[].class);
            android.os.ParcelFileDescriptor[] pipe = android.os.ParcelFileDescriptor.createPipe();
            java.io.OutputStream stdin = new java.io.FileOutputStream(pipe[1].getFileDescriptor());
            stdin.write(cmd.getBytes());
            stdin.write(('\n').getBytes());
            stdin.close();
            Object result = exec.invoke(service, (Object) pipe);
            if (result instanceof Integer) {
                int code = (Integer) result;
                java.io.InputStream stdout = new java.io.FileInputStream(pipe[0].getFileDescriptor());
                java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
                byte[] buf = new byte[4096];
                int n;
                while ((n = stdout.read(buf)) > 0) out.write(buf, 0, n);
                return out.toString("UTF-8") + (code != 0 ? " (exit " + code + ")" : "");
            }
            return "Shizuku v1 returned " + (result == null ? "null" : result.getClass().getName());
        } catch (Exception e) {
            Log.e(TAG, "Shizuku v1 shell failed", e);
            return "Shizuku error: " + e.getMessage();
        }
    }

    /** Check if a specific permission (e.g. android.permission.READ_SMS) is granted via Shizuku. */
    public boolean checkPermission(String permission) {
        if (!isBound()) return false;
        try {
            Class<?> shizukuCls = Class.forName("moe.shizuku.privileged.IShizukuService");
            java.lang.reflect.Method asInterface = shizukuCls.getMethod("asInterface", IBinder.class);
            Object service = asInterface.invoke(null, (IBinder) binder);
            java.lang.reflect.Method check = shizukuCls.getMethod("checkPermission", String.class, String.class);
            Object result = check.invoke(service, c.getPackageName(), permission);
            if (result instanceof Boolean) return (Boolean) result;
            if (result instanceof Integer) return (Integer) result == 0; // PackageManager.PERMISSION_GRANTED
            return false;
        } catch (Exception e) {
            Log.e(TAG, "Shizuku checkPermission failed", e);
            return false;
        }
    }
}
