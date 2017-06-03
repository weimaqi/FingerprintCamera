package com.serenegiant.usb;

import java.nio.ByteBuffer;

/**
 * Created by YiChen on 2016/2/15.
 */
public interface IStatusCallback {
    void onStatus(int statusClass, int event, int selector, int statusAttribute, ByteBuffer data);
}
