package ski.agent;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;

/**
 * Receives Shizuku's server binder. The Shizuku manager app calls {@code call("sendBinder")}
 * on this provider every time its server starts; the binder rides in a Bundle as a
 * {@link moe.shizuku.api.BinderContainer}. Authority must be "ski.agent.shizuku"
 * (applicationId + ".shizuku"), matching what the manager looks up.
 */
public class ShizukuProvider extends ContentProvider {

    /** From Shizuku-API's ShizukuProvider. */
    static final String METHOD_SEND_BINDER = "sendBinder";
    static final String EXTRA_BINDER = "moe.shizuku.privileged.api.intent.extra.BINDER";

    @Override
    public boolean onCreate() {
        return true;
    }

    @Override
    public Bundle call(String method, String arg, Bundle extras) {
        if (!METHOD_SEND_BINDER.equals(method) || extras == null) return null;
        extras.setClassLoader(moe.shizuku.api.BinderContainer.class.getClassLoader());
        moe.shizuku.api.BinderContainer container = extras.getParcelable(EXTRA_BINDER);
        if (container != null && container.binder != null) {
            Shizuku.get(getContext()).onBinderReceived(container.binder);
        }
        return new Bundle();
    }

    @Override public Cursor query(Uri uri, String[] p, String s, String[] a, String o) { return null; }
    @Override public String getType(Uri uri) { return null; }
    @Override public Uri insert(Uri uri, ContentValues v) { return null; }
    @Override public int delete(Uri uri, String s, String[] a) { return 0; }
    @Override public int update(Uri uri, ContentValues v, String s, String[] a) { return 0; }
}
