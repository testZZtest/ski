package moe.shizuku.api;

import android.os.IBinder;
import android.os.Parcel;
import android.os.Parcelable;

/**
 * The parcelable Shizuku's manager uses to hand Ski the server binder (provider call
 * "sendBinder"). This is the protocol class from Shizuku-API, vendored verbatim so the
 * bundle unmarshals without depending on the Shizuku library (Ski builds against the
 * android.jar alone).
 */
public class BinderContainer implements Parcelable {

    public IBinder binder;

    public BinderContainer(IBinder binder) {
        this.binder = binder;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeStrongBinder(this.binder);
    }

    protected BinderContainer(Parcel in) {
        this.binder = in.readStrongBinder();
    }

    public static final Creator<BinderContainer> CREATOR = new Creator<BinderContainer>() {
        @Override
        public BinderContainer createFromParcel(Parcel source) {
            return new BinderContainer(source);
        }

        @Override
        public BinderContainer[] newArray(int size) {
            return new BinderContainer[size];
        }
    };
}
