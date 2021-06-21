package cm.cym.PictureSelectDemo;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.provider.Settings;

import androidx.core.app.ActivityCompat;

public class PermissionUtil {

    /**
     * 请求权限的Code
     */
    public static final int REQUEST_BLUETOOTH = 1500;
    public static final int REQUEST_LOCATION_PERMISSION = 1501;
    public static final int REQUEST_LOCATION_SERVICE = 1502;
    public static final int REQUEST_CAMERA = 1503;
    public static final int REQUEST_STORAGE = 1504;

    /**
     * 是否有拍照权限
     *
     * @return boolean
     */
    public static boolean hasCameraPermission(Context context) {
        return ActivityCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
    }

    /**
     * 是否有外部储存权限
     *
     * @return boolean
     */
    public static boolean hasStoragePermission(Context context) {
        return ActivityCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
    }

    /**
     * 申请拍照权限
     */
    public static void requestCameraPermission(Activity activity) {
        ActivityCompat.requestPermissions(activity, new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA);
    }

    /**
     * 申请外部储存权限
     */
    public static void requestStoragePermission(Activity activity) {
        ActivityCompat.requestPermissions(activity, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, REQUEST_STORAGE);
    }

    /**
     * 同时申请拍照权限和外部储存权限
     */
    public static void requestCameraAndStoragePermission(Activity activity) {
        ActivityCompat.requestPermissions(activity, new String[]{Manifest.permission.CAMERA, Manifest.permission.READ_EXTERNAL_STORAGE}, REQUEST_STORAGE);
    }

    /**
     * 申请定位权限
     */
    public static void requestLocationPermission(Activity activity) {
        ActivityCompat.requestPermissions(activity, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_LOCATION_PERMISSION);
    }

    /**
     * 申请打开蓝牙
     */
    public static void requestBluetooth(Activity activity) {
        Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        activity.startActivityForResult(intent, REQUEST_BLUETOOTH);
    }

    /**
     * 申请打开定位服务
     */
    public static void requestLocationService(Activity activity) {
        Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
        activity.startActivityForResult(intent, REQUEST_LOCATION_SERVICE);
    }

}
