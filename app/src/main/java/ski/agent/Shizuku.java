package ski.agent;

import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Parcel;
import android.os.ParcelFileDescriptor;
import android.os.RemoteException;
import android.util.Log;

import java.io.InputStream;
import java.io.OutputStream;

/**
 * Shizuku support without the Shizuku library: talks to the Shizuku server with raw
 * binder transactions, mirroring the official rikka.shizuku client (same transaction
 * codes and parcel layout). The binder arrives through {@link ShizukuProvider}; the
 * first {@link #attach()} tells the server who we are (and whether permission was
 * already granted); {@link #requestPermission(int)} pops Shizuku's dialog and the
 * answer lands on the application binder's dispatchRequestPermissionResult.
 *
 * Transaction codes here are FIRST_CALL_TRANSACTION (1) + the AIDL method number:
 * IShizukuService: getVersion=2, getUid=3, checkPermission=4, newProcess=7,
 * requestPermission=14, checkSelfPermission=15, attachApplication=17.
 */
public final class Shizuku {

    private static final String TAG = "Ski.Shizuku";
    /** The AIDL interface descriptor of the Shizuku server. */
    private static final String DESCRIPTOR = "moe.shizuku.server.IShizukuService";

    private static volatile Shizuku instance;

    private IBinder server;      // the Shizuku server binder (from the provider)
    private final AppBinder appBinder = new AppBinder();
    private volatile boolean attached, permissionGranted;
    private Runnable grantedListener;

    private Shizuku(Context c) {
        // a listener is enough for now; the context is only kept for future needs
    }

    public static Shizuku get(Context c) {
        if (instance == null) instance = new Shizuku(c.getApplicationContext());
        return instance;
    }

    /** Whether the Shizuku app is installed. */
    public static boolean isInstalled(Context c) {
        try {
            c.getPackageManager().getPackageInfo("moe.shizuku.privileged.api", 0);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /** Called by {@link ShizukuProvider} when the manager hands over the server binder. */
    void onBinderReceived(IBinder binder) {
        server = binder;
        attached = false;
        Log.d(TAG, "Shizuku binder received");
        // attach right away so permissionGranted is known before the first command
        attach();
        if (grantedListener != null && permissionGranted) grantedListener.run();
    }

    /** Whether the server binder is alive (the user is running Shizuku). */
    public boolean isBound() {
        return server != null && server.pingBinder();
    }

    /** Legacy entry point: nothing to do — the binder arrives passively via the provider. */
    public boolean bind() {
        return isBound();
    }

    // ---------------------------------------------------------------- protocol

    /** The local half of the application protocol: the server calls back on this binder. */
    private class AppBinder extends Binder {
        @Override
        protected boolean onTransact(int code, Parcel data, Parcel reply, int flags) {
            // AIDL stubs route by code; the caller wrote the interface token first
            if (code == 1) { // bindApplication(Bundle) — the attach reply
                data.enforceInterface(DESCRIPTOR);
                Bundle b = data.readBundle(getClass().getClassLoader());
                permissionGranted = b != null && b.getBoolean("shizuku:attach-reply-permission-granted", false);
                return true;
            }
            if (code == 2) { // dispatchRequestPermissionResult(int, Bundle)
                data.enforceInterface(DESCRIPTOR);
                data.readInt();
                Bundle b = data.readBundle(getClass().getClassLoader());
                boolean allowed = b != null && b.getBoolean("shizuku:request-permission-reply-allowed", false);
                Log.d(TAG, "requestPermission result: " + allowed);
                if (allowed) permissionGranted = true;
                final boolean ok = allowed;
                android.os.Handler h = new android.os.Handler(android.os.Looper.getMainLooper());
                h.post(() -> {
                    Runnable l = grantedListener;
                    if (l != null) l.run();
                    SetupActivity.onShizukuPermission(ok);
                });
                return true;
            }
            try {
                return super.onTransact(code, data, reply, flags);
            } catch (RemoteException e) {
                return false;
            }
        }
    }

    private void attach() {
        if (attached || !isBound()) return;
        try {
            Parcel data = Parcel.obtain(), reply = Parcel.obtain();
            try {
                data.writeInterfaceToken(DESCRIPTOR);
                data.writeStrongBinder(appBinder);
                data.writeInt(1); // the official client writes the args-bundle count first
                Bundle args = new Bundle();
                args.putString("shizuku:attach-package-name", "ski.agent");
                args.putInt("shizuku:attach-api-version", 13);
                args.writeToParcel(data, 0);
                server.transact(18 /* attachApplication */, data, reply, 0);
                reply.readException();
                attached = true;
                Log.d(TAG, "attachApplication ok, granted=" + permissionGranted);
            } finally {
                data.recycle();
                reply.recycle();
            }
        } catch (Exception e) {
            Log.w(TAG, "attachApplication v13 failed, trying v11", e);
            try {
                Parcel data = Parcel.obtain(), reply = Parcel.obtain();
                try {
                    data.writeInterfaceToken(DESCRIPTOR);
                    data.writeStrongBinder(appBinder);
                    data.writeString("ski.agent");
                    server.transact(14 /* v11 attach */, data, reply, 0);
                    reply.readException();
                    attached = true;
                } finally {
                    data.recycle();
                    reply.recycle();
                }
            } catch (Exception e2) {
                Log.e(TAG, "attachApplication failed", e2);
            }
        }
    }

    /** Whether Ski may use Shizuku right now (server up and permission granted). */
    public boolean ready() {
        attach();
        return isBound() && permissionGranted;
    }

    /** Pop Shizuku's permission dialog; the answer reaches grantedListener/SetupActivity. */
    public void requestPermission(Runnable onGranted) {
        grantedListener = onGranted;
        attach();
        if (permissionGranted) {
            if (onGranted != null) onGranted.run();
            return;
        }
        try {
            Parcel data = Parcel.obtain(), reply = Parcel.obtain();
            try {
                data.writeInterfaceToken(DESCRIPTOR);
                data.writeInt(0); // requestCode
                server.transact(15 /* requestPermission */, data, reply, 0);
                reply.readException();
            } finally {
                data.recycle();
                reply.recycle();
            }
        } catch (Exception e) {
            Log.e(TAG, "requestPermission failed", e);
        }
    }

    /**
     * Run a shell command on the server (as the shell user, or root if Shizuku runs as root).
     * Returns the command's stdout+stderr (bounded) and appends " (exit N)" on non-zero.
     */
    public String runShell(String cmd) {
        if (!ready()) return "Shizuku not ready (install the app, start the server, grant permission)";
        try {
            Parcel data = Parcel.obtain(), reply = Parcel.obtain();
            IBinder proc;
            try {
                data.writeInterfaceToken(DESCRIPTOR);
                data.writeStringArray(new String[]{"/system/bin/sh", "-c", cmd});
                data.writeStringArray(null); // env
                data.writeString(null);      // dir
                server.transact(8 /* newProcess */, data, reply, 0);
                reply.readException();
                proc = reply.readStrongBinder();
            } finally {
                data.recycle();
                reply.recycle();
            }
            if (proc == null) return "Shizuku: no process returned";
            return readProcess(proc);
        } catch (Exception e) {
            Log.e(TAG, "runShell failed", e);
            return "Shizuku error: " + e.getMessage();
        }
    }

    /** Drain stdout/stderr of a newProcess result and wait for its exit. */
    private String readProcess(IBinder proc) throws Exception {
        final StringBuilder out = new StringBuilder();
        Thread t1 = drain(proc, 2 /* getInputStream */, out);
        Thread t2 = drain(proc, 3 /* getErrorStream */, out);
        t1.start();
        t2.start();
        int code = -1;
        try {
            Parcel data = Parcel.obtain(), reply = Parcel.obtain();
            try {
                data.writeInterfaceToken("moe.shizuku.server.IRemoteProcess");
                proc.transact(4 /* waitFor */, data, reply, 0);
                reply.readException();
                code = reply.readInt();
            } finally {
                data.recycle();
                reply.recycle();
            }
        } catch (Exception ignored) {
            try {
                Parcel data = Parcel.obtain(), reply = Parcel.obtain();
                try {
                    data.writeInterfaceToken("moe.shizuku.server.IRemoteProcess");
                    proc.transact(5 /* exitValue */, data, reply, 0);
                    reply.readException();
                    code = reply.readInt();
                } finally {
                    data.recycle();
                    reply.recycle();
                }
            } catch (Exception ignored2) {
            }
        }
        t1.join(3000);
        t2.join(3000);
        String s = out.toString();
        if (s.length() > 16000) s = s.substring(0, 16000) + "…";
        return s + (code != 0 ? " (exit " + code + ")" : "");
    }

    private Thread drain(final IBinder proc, final int code, final StringBuilder out) {
        return new Thread(() -> {
            try {
                Parcel data = Parcel.obtain(), reply = Parcel.obtain();
                ParcelFileDescriptor pfd;
                try {
                    data.writeInterfaceToken("moe.shizuku.server.IRemoteProcess");
                    proc.transact(code, data, reply, 0);
                    reply.readException();
                    pfd = reply.readFileDescriptor();
                } finally {
                    data.recycle();
                    reply.recycle();
                }
                InputStream in = new ParcelFileDescriptor.AutoCloseInputStream(pfd);
                byte[] buf = new byte[8192];
                int n;
                while ((n = in.read(buf)) > 0) out.append(new String(buf, 0, n));
                in.close();
            } catch (Exception ignored) {
            }
        });
    }

    /** Run a command and discard output; true when it exited 0. */
    public boolean run(String cmd) {
        String r = runShell(cmd);
        return !r.contains("(exit ");
    }

    /** Whether Ski itself holds a system permission (checked through the server's pm). */
    public boolean checkPermission(String permission) {
        return hasPermission(permission);
    }

    /** Whether a system permission (e.g. READ_SMS) is granted to Ski — via the server's pm. */
    public boolean hasPermission(String permission) {
        String r = runShell("dumpsys package ski.agent | grep " + permission + " | head -1");
        return r != null && r.contains("granted=true");
    }

    /** Grant runtime permissions to Ski with one tap (needs the Shizuku permission first). */
    public String grantSelf(String[] permissions) {
        StringBuilder log = new StringBuilder();
        for (String p : permissions) {
            String r = runShell("pm grant ski.agent " + p);
            log.append(p).append(": ").append(r.trim().isEmpty() ? "ok" : r.trim()).append('\n');
        }
        return log.toString().trim();
    }
}
