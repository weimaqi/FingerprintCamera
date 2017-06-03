package com.serenegiant.usb;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.Locale;

/**
 * Created by YiChen on 2016/2/15.
 */
public class Size implements Parcelable {

    public int type;
    public int index;
    public int width;
    public int height;

    public Size(final int _type, final int _index, final int _width, final int _height) {
        type = _type;
        index = _index;
        width = _width;
        height = _height;
    }

    private Size(final Parcel source) {
        type = source.readInt();
        index = source.readInt();
        width = source.readInt();
        height = source.readInt();
    }

    public Size set(final Size other) {
        if (other != null) {
            type = other.type;
            index = other.index;
            width = other.width;
            height = other.height;
        }
        return this;
    }

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(final Parcel dest, final int flags) {
        dest.writeInt(type);
        dest.writeInt(index);
        dest.writeInt(width);
        dest.writeInt(height);
    }

    public String toString() {
        return String.format(Locale.US, "Size(%d%d, typr:%d, index:%d)", width, height, type, index);
    }

    public static final Creator<Size> CREATOR = new Creator<Size>() {

        @Override
        public Size createFromParcel(Parcel source) {
            return new Size(source);
        }

        @Override
        public Size[] newArray(int size) {
            return new Size[size];
        }
    };
}
