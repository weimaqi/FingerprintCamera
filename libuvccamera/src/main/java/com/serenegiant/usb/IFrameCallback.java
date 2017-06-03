package com.serenegiant.usb;

import java.nio.ByteBuffer;

/**
 * Callback interface for UVCCamera class
 * If you need frame data as ByteBuffer, you can use this callback interface with UVCCamera#setFrameCallback.
 *
 * Created by YiChen on 2016/2/15.
 */
public interface IFrameCallback {
    /**
     * This method is called from native library via JNI on the same thread as UVCCamera#startCaptures.
     * You can use both UVCCamera#startCapture and #setFrameCallback
     * but it is better to use either for better performance.
     * You can also pass pixel format type to UVCCamera#setFrameCallback for this method.
     * Some frames may drops if this method takes a time.
     * @param frame
     */
    void onFrame(ByteBuffer frame);
}
