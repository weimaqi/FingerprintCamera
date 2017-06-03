package com.yi.fingerprintCamera;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.SurfaceTexture;
import android.hardware.usb.UsbDevice;
import android.media.MediaScannerConnection;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.serenegiant.usb.CameraDialog;
import com.serenegiant.usb.USBMonitor;
import com.serenegiant.usb.USBMonitor.OnDeviceConnectListener;
import com.serenegiant.usb.USBMonitor.UsbControlBlock;
import com.serenegiant.usb.UVCCamera;
import com.yi.encoder.MediaMuxerWrapper;
import com.yi.usb.UVCCameraHandler;
import com.yi.widget.PermissionHelper;
import com.yi.widget.UVCCameraTextureView;

import java.io.File;
import java.text.SimpleDateFormat;

public final class MainActivity extends AppCompatActivity implements CameraDialog.CameraDialogParent {
	private static final boolean DEBUG = true;
	private static final String TAG = MainActivity.class.getSimpleName();

	/**
	 * for accessing USB
	 */
	private USBMonitor mUSBMonitor;
	/**
	 * Handler to execute camera related methods sequentially on private thread
	 */
	private UVCCameraHandler mHandler;

	private TextView mTipTextView, textViewRecordingTime;
	/**
	 * for camera preview display
	 */
	private UVCCameraTextureView mUVCCameraView;
	/**
	 * for open&start / stop&close camera preview
	 */
	private ToggleButton mCameraButton;
	/**
	 * imageView for start/stop recording
	 */
	private ImageView mCaptureImage, mModeImage;

	private static final int WRITE_STORAGE_CODE = 12;
	private static final String WRITE_STORAGE_PERMISSION = Manifest.permission.WRITE_EXTERNAL_STORAGE;

	private static boolean ModeSwitch, CapturePicture;

	private RelativeLayout mRelativeLayout;

	private Long startTime;
	private Handler handler = new Handler();

	@Override
	protected void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (DEBUG) Log.v(TAG, "onCreate:");
		setContentView(R.layout.activity_main);

		mRelativeLayout = (RelativeLayout) findViewById(R.id.RelativeLayout1);

		mTipTextView = (TextView) findViewById(R.id.id_txt_info);

		mUVCCameraView = (UVCCameraTextureView) findViewById(R.id.camera_view);
		mUVCCameraView.setAspectRatio(UVCCamera.DEFAULT_PREVIEW_WIDTH / (float) UVCCamera.DEFAULT_PREVIEW_HEIGHT);
		mUVCCameraView.setSurfaceTextureListener(mSurfaceTextureListener);
		mUVCCameraView.setOnLongClickListener(mOnLongClickListener);
		mUVCCameraView.setRotationX(180.0f);
		mUVCCameraView.setRotationY(180.0f);

		mCameraButton = (ToggleButton) findViewById(R.id.camera_button);
		mCameraButton.setOnClickListener(mOnClickListener);

		mCaptureImage = (ImageView) findViewById(R.id.imageViewCapture);
		mCaptureImage.setOnClickListener(mOnClickListener);
		mCaptureImage.setEnabled(false);
		CapturePicture = true;
		ModeSwitch = true;

		mModeImage = (ImageView) findViewById(R.id.imageViewMode);
		mModeImage.setOnClickListener(mOnClickListener);

		textViewRecordingTime = (TextView) findViewById(R.id.textViewRecordingTime);

		mUSBMonitor = new USBMonitor(this, mOnDeviceConnectListener);
		mHandler = UVCCameraHandler.createHandler(this);

		PermissionHelper mPermissionHelper = new PermissionHelper(this);


		if (Build.VERSION.SDK_INT >= 23) {
			boolean b = mPermissionHelper.checkPermission(WRITE_STORAGE_PERMISSION);
			if (b) {
				mCaptureImage.setVisibility(View.VISIBLE);
				mModeImage.setVisibility(View.VISIBLE);
			} else {
				mPermissionHelper.permissionCheck(WRITE_STORAGE_PERMISSION, WRITE_STORAGE_CODE);
				mCaptureImage.setVisibility(View.INVISIBLE);
				mModeImage.setVisibility(View.INVISIBLE);
			}
		} else {
			mCaptureImage.setVisibility(View.VISIBLE);
			mModeImage.setVisibility(View.VISIBLE);
		}
	}

	public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
		super.onRequestPermissionsResult(requestCode, permissions, grantResults);

		switch (requestCode) {
			case WRITE_STORAGE_CODE:
				if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
					mCaptureImage.setVisibility(View.VISIBLE);
					mModeImage.setVisibility(View.VISIBLE);
				} else {
					//Toast.makeText(MainActivity.this, "Write Storage Denied", Toast.LENGTH_SHORT).show();
					mCaptureImage.setVisibility(View.INVISIBLE);
					mModeImage.setVisibility(View.INVISIBLE);
				}
				break;
		}
	}

	@Override
	public void onResume() {
		super.onResume();
		if (DEBUG) Log.e(TAG, "onResume:");
		mUSBMonitor.register();

	}

	@Override
	public void onPause() {
		if (DEBUG) Log.e(TAG, "onPause:");
		mHandler.close();    // #close include #stopRecording and #stopPreview
		mCameraButton.setChecked(false);
		mUSBMonitor.unregister();

		super.onPause();
	}

	@Override
	public void onDestroy() {
		if (DEBUG) Log.e(TAG, "onDestroy:");
		if (mHandler != null) {
			mHandler.release();
			mHandler = null;
		}
		if (mUSBMonitor != null) {
			mUSBMonitor.destroy();
			mUSBMonitor = null;
		}
		mUVCCameraView = null;
		mCameraButton = null;
		super.onDestroy();
	}

	/**
	 * event handler when click camera / capture button
	 */
	private final OnClickListener mOnClickListener = new OnClickListener() {
		@Override
		public void onClick(final View view) {
			switch (view.getId()) {
				case R.id.camera_button:
					if (!mHandler.isOpened()) {
						CameraDialog.showDialog(MainActivity.this);
					} else {
						mHandler.close();
					}
					break;

				case R.id.imageViewCapture:
					if (mHandler.isOpened()) {
						if (CapturePicture) {
							final File outputFile = MediaMuxerWrapper.getCaptureFile(Environment.DIRECTORY_DCIM, ".png");
							mHandler.captureStill(outputFile.toString());

							try {
								if (DEBUG) Log.i(TAG, "MediaScannerConnection#scanFile");
								MediaScannerConnection.scanFile(getApplicationContext(), new String[]{outputFile.toString()}, null, null);
							} catch (final Exception e) {
								Log.e(TAG, "MediaScannerConnection#scanFile:", e);
							}

							Toast.makeText(MainActivity.this, "Capture still image", Toast.LENGTH_SHORT).show();
						} else {
							if (!mHandler.isRecording()) {
								mCaptureImage.setImageDrawable(getResources().getDrawable(R.drawable.c_recording));
								mHandler.startRecording();
								mModeImage.setVisibility(View.INVISIBLE);
								startRunTimeProcess();
							} else {
								mCaptureImage.setSelected(false);
								mCaptureImage.setImageDrawable(getResources().getDrawable(R.drawable.c_record));
								mHandler.stopRecording();
								textViewRecordingTime.setVisibility(View.INVISIBLE);
								mModeImage.setVisibility(View.VISIBLE);
								Toast.makeText(MainActivity.this, "Recording image", Toast.LENGTH_SHORT).show();
							}
						}
					}
					break;
				case R.id.imageViewMode:
					if (ModeSwitch) {
						mCaptureImage.setImageDrawable(getResources().getDrawable(R.drawable.c_record));
						mModeImage.setImageDrawable(getResources().getDrawable(R.drawable.c_mode_camera));
						CapturePicture = false;
						ModeSwitch = false;
					} else {
						mCaptureImage.setImageDrawable(getResources().getDrawable(R.drawable.c_shot));
						mModeImage.setImageDrawable(getResources().getDrawable(R.drawable.c_mode_video));
						CapturePicture = true;
						ModeSwitch = true;
					}
					break;
			}
		}
	};

	/**
	 * Capture still image when you long click on preview image(not on buttons)
	 */
	private final OnLongClickListener mOnLongClickListener = new OnLongClickListener() {
		@Override
		public boolean onLongClick(final View view) {
			switch (view.getId()) {
				case R.id.camera_view:
					if (mHandler.isOpened()) {
						final File outputFile = MediaMuxerWrapper.getCaptureFile(Environment.DIRECTORY_DCIM, ".png");
						mHandler.captureStill(outputFile.toString());
						try {
							if (DEBUG) Log.i(TAG, "MediaScannerConnection#scanFile");
							MediaScannerConnection.scanFile(getApplicationContext(), new String[]{outputFile.toString()}, null, null);
						} catch (final Exception e) {
							Log.e(TAG, "MediaScannerConnection#scanFile:", e);
						}
						return true;
					}
			}
			return false;
		}
	};

	private void startPreview() {
		mHandler.startPreview();
		mCameraButton.setChecked(true);
		mCaptureImage.setEnabled(true);
		mTipTextView.setText(R.string.open_string);
		/*
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				mTipTextView.setText(R.string.open_string);
			}
		});
		*/
	}

	private final OnDeviceConnectListener mOnDeviceConnectListener = new OnDeviceConnectListener() {
		@Override
		public void onAttach(final UsbDevice device) {
			Log.e(TAG, "onAttach");
			Toast.makeText(MainActivity.this, "USB_DEVICE_ATTACHED", Toast.LENGTH_SHORT).show();
		}

		@Override
		public void onDetach(UsbDevice device) {
			Log.e(TAG, "onDetach");
			Toast.makeText(MainActivity.this, "USB_DEVICE_DETACHED", Toast.LENGTH_SHORT).show();
		}

		@Override
		public void onConnect(final UsbDevice device, final UsbControlBlock ctrlBlock, final boolean createNew) {
			if (DEBUG) Log.e(TAG, "onConnect:");
			mHandler.open(ctrlBlock);
			startPreview();
		}

		@Override
		public void onDisconnect(final UsbDevice device, final UsbControlBlock ctrlBlock) {
			if (DEBUG) Log.e(TAG, "onDisconnect:");
			if (mHandler != null) {
				mHandler.close();
				runOnUiThread(new Runnable() {
					@Override
					public void run() {
						if (!isFinishing()) {
							try {
								mTipTextView.setText(R.string.hint_string);
								mCameraButton.setChecked(false);
								mCaptureImage.setEnabled(false);
							} catch (final Exception e) {
							}
						}
					}
				});
			}
		}

		@Override
		public void onCancel() {
		}
	};

	/**
	 * To access from CameraDialog
	 *
	 * @return
	 */
	@Override
	public USBMonitor getUSBMonitor() {
		return mUSBMonitor;
	}

	private final TextureView.SurfaceTextureListener mSurfaceTextureListener = new TextureView.SurfaceTextureListener() {

		@Override
		public void onSurfaceTextureAvailable(final SurfaceTexture surface, final int width, final int height) {
			final Surface _surface = new Surface(surface);
			if (mHandler != null) {
				mHandler.addSurface(surface.hashCode(), _surface, false, null);
			}
		}

		@Override
		public void onSurfaceTextureSizeChanged(final SurfaceTexture surface, final int width, final int height) {
		}

		@Override
		public boolean onSurfaceTextureDestroyed(final SurfaceTexture surface) {
			if (mHandler != null) {
				mHandler.removeSurface(surface.hashCode());
			}
			return true;
		}

		@Override
		public void onSurfaceTextureUpdated(final SurfaceTexture surface) {
		}
	};

	private void startRunTimeProcess() {
		textViewRecordingTime.setVisibility(View.VISIBLE);
		textViewRecordingTime.setText("00:00:00");
		startTime = System.currentTimeMillis();
		handler.removeCallbacks(updateTimerThread);
		handler.postDelayed(updateTimerThread, 1000);
	}

	private Runnable updateTimerThread = new Runnable() {
		@Override
		public void run() {
			SimpleDateFormat format = new SimpleDateFormat("HH:mm:ss");
			Long spentTime = System.currentTimeMillis() - startTime;
			textViewRecordingTime.setText(format.format(spentTime));
			handler.postDelayed(this, 1000);
		}
	};
}
