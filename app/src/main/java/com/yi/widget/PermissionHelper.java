package com.yi.widget;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;

import com.yi.fingerprintCamera.R;

/**
 * Created by YiChen on 2016/2/25.
 */
public class PermissionHelper {
    private static final String TAG = PermissionHelper.class.getSimpleName();

    private Context mContext;
    public static final String PACKAGE = "package : ";

    public PermissionHelper(Context context) {
        this.mContext = context;
    }

    /**
     * 判斷權限集合
     *
     * @param permissions   檢測權限的集合
     * @return  權限已全部獲取返回true，反之，false
     */
    public boolean checkPermissions(String... permissions) {
        for (String permission : permissions) {
            if (!checkPermission(permission)) {
                return false;
            }
        }
        return true;
    }

    /**
     * 判斷權限是否獲取
     *
     * @param permission    權限名稱
     * @return  已授權返回true，反之，false
     */
    public boolean checkPermission(String permission) {
        return ContextCompat.checkSelfPermission(mContext, permission) == PackageManager.PERMISSION_GRANTED;
    }

    public void permissionCheck(String permission, int resultCode) {
        // 注意這裡要使用 shouldShowRequestPermissionRationale ，而不要使用requestPermissions方法
        // 因為requestPermissions方法會顯示"不再詢問"按鈕
        /*
        if (ActivityCompat.shouldShowRequestPermissionRationale((Activity)mContext, permission)) {
            // 如果用戶拒絕過給予權限，則給提示
            showMissingPermissionDialog("Must be allowed storage permission", permission, resultCode);
        } else {
            // 進行權限請求
            ActivityCompat.requestPermissions((Activity)mContext, new String[]{permission}, resultCode);
        }
        */
        ActivityCompat.requestPermissions((Activity)mContext, new String[]{permission}, resultCode);
    }

    private void showMissingPermissionDialog(final String message, final String permission, final int resultCode) {
        AlertDialog.Builder builder = new AlertDialog.Builder(mContext, R.style.AppCompatAlertDialogStyle);
        final AlertDialog alertDialog = builder.create();

        builder.setMessage(message);
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                alertDialog.dismiss();
            }
        });
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                ActivityCompat.requestPermissions((Activity) mContext, new String[]{permission}, resultCode);
            }
        });
        builder.show();
    }
}
