package cm.cym.PictureSelectDemo;

import android.Manifest;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.FileUtils;
import android.provider.MediaStore;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * 相机相册图片选择裁剪工具类
 */
public class PictureSelectUtil {

    /**
     * 传入Activity对象
     *
     * @param activity 调用时的Activity
     * @return PictureObject
     */
    public static PictureSelectFragment with(AppCompatActivity activity) {
        return new PictureSelectFragment(activity);
    }

    /**
     * 传入Fragment对象
     *
     * @param fragment 调用时的Fragment
     * @return PictureObject
     */
    public static PictureSelectFragment with(Fragment fragment) {
        return new PictureSelectFragment(fragment);
    }

    /**
     * 内部类，用来处理选择图片前的操作
     */
    public static class PictureSelectFragment extends Fragment {

        private static final int REQUEST_PERMISSION = 1000;// 请求拍照权限
        private static final int REQUEST_CAMERA_SELECT = 1001;// 请求通过拍照选择图片
        private static final int REQUEST_GALLERY_SELECT = 1002;// 请求通过相册选择图片
        private static final int REQUEST_CROP = 1003;// 请求裁剪

        private Context mContext;
        private AppCompatActivity mActivity;
        private Fragment mFragment;
        private Uri mPublicUri;
        private Uri mCropOutUri;
        private Uri mPrivateUri;

        private boolean mIsCamera = false;// 拍照选择图片
        private boolean mIsGallery = false;// 相册选择图片
        private int mCropX = -1;// 裁剪比例。不为-1说明需要裁剪
        private int mCropY = -1;// 裁剪比例。不为-1说明需要裁剪
        private OnCallback mOnCallback;

        private PictureSelectFragment(AppCompatActivity activity) {
            mActivity = activity;
            mContext = activity;
        }

        private PictureSelectFragment(Fragment fragment) {
            mFragment = fragment;
            mContext = fragment.getContext();
        }

        /**
         * 通过拍照选择图片
         *
         * @return this
         */
        public PictureSelectFragment camera() {
            mIsCamera = true;
            return this;
        }

        /**
         * 通过相册选择图片
         *
         * @return this
         */
        public PictureSelectFragment gallery() {
            mIsGallery = true;
            return this;
        }

        /**
         * 裁剪成正方型
         *
         * @return this
         */
        public PictureSelectFragment crop() {
            return crop(1, 1);
        }

        /**
         * 裁剪成指定比例
         *
         * @param x X
         * @param y Y
         * @return this
         */
        public PictureSelectFragment crop(int x, int y) {
            mCropX = x;
            mCropY = y;
            return this;
        }

        /**
         * 设置选择图片后的回调
         *
         * @param onCallback 回调
         * @return this
         */
        public PictureSelectFragment setCallback(OnCallback onCallback) {
            mOnCallback = onCallback;
            return this;
        }

        /**
         * 使用之前设置的参数，开始选择图片
         */
        public void select() {
            // 创建Fragment
            if (!isAdded()) {
                if (mActivity != null) {
                    mActivity.getSupportFragmentManager().beginTransaction().add(0, this).commitAllowingStateLoss();
                    mActivity.getSupportFragmentManager().executePendingTransactions();
                } else {
                    mFragment.getChildFragmentManager().beginTransaction().add(0, this).commitAllowingStateLoss();
                    mFragment.getChildFragmentManager().executePendingTransactions();
                }
            }
            // 进行图片选择
            if (mIsCamera) {
                // 通过拍照选择图片，需要拍照权限和储存权限
                if (!hasCameraPermission() || !hasStoragePermission()) {
                    requestCameraAndStoragePermission();
                    return;
                }
                // 拍照
                initPath();
                openCamera();
            } else {
                // 通过相册选择图片，需要储存权限
                if (!hasStoragePermission()) {
                    requestStoragePermission();
                    return;
                }
                // 打开相册
                initPath();
                openGallery();
            }
        }

        @Override
        public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
            super.onActivityResult(requestCode, resultCode, data);
            if (resultCode != Activity.RESULT_OK) {
                return;
            }
            switch (requestCode) {
                case REQUEST_CAMERA_SELECT:
                    // 通过相机拍照回来
                    // 相机拍照会把图片保存到公共目录
                    // 这里不判断data!=null是因为有些手机如vivo这里会回调空
                    if (mCropX >= 0 && mCropY >= 0) {
                        // 需要裁剪，继续裁剪公共目录的Uri
                        startCrop(mPublicUri);
                    } else {
                        // 不需要裁剪，但是要给Glide用，需要移动到私有目录
                        moveUri(mPublicUri, mPrivateUri);
                        if (mOnCallback != null) {
                            mOnCallback.onCallback(mPrivateUri);
                        }
                    }
                    break;
                case REQUEST_GALLERY_SELECT:
                    // 通过相册选择图片回来
                    // 相册会直接返回Uri
                    if (data != null) {
                        // 相册选择会返回图片的uri
                        Uri uri = data.getData();
                        if (mCropX >= 0 && mCropY >= 0) {
                            // 需要裁剪，继续裁剪
                            startCrop(uri);
                        } else {
                            // 不需要裁剪，这个Uri可以直接给Glide，所以直接回调
                            if (mOnCallback != null) {
                                mOnCallback.onCallback(uri);
                            }
                        }
                    }
                    break;
                case REQUEST_CROP:
                    // 裁剪回来
                    if (mOnCallback != null) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                            // Android11，Glide无法加载公共目录的图片，所以要移回私有目录
                            moveUri(mPublicUri, mPrivateUri);
                            mCropOutUri = mPrivateUri;
                        }
                        mOnCallback.onCallback(mCropOutUri);
                    }
                    break;
            }
        }

        @Override
        public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
            if (requestCode == REQUEST_PERMISSION) {
                // 请求拍照权限回来，重新走流程
                select();
            }
        }

        /**
         * 初始化文件保存的路径
         * 都保存成时间戳，因为Glide会根据文件名进行缓存
         * 记得要有WRITE_EXTERNAL_STORAGE权限，不然低版本安卓会闪退
         */
        private void initPath() {
            File publicFile = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).getPath(), System.currentTimeMillis() + ".jpg");
            File privateFile = new File(mContext.getExternalCacheDir(), System.currentTimeMillis() + ".jpg");
            mPublicUri = getContentUriByFile(publicFile);
            mPrivateUri = Uri.fromFile(privateFile);
        }

        /**
         * 通过相机选择图片
         */
        private void openCamera() {
            Intent intent = new Intent();
            intent.setAction(MediaStore.ACTION_IMAGE_CAPTURE);
            intent.putExtra(MediaStore.EXTRA_OUTPUT, mPublicUri);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivityForResult(intent, REQUEST_CAMERA_SELECT);
        }

        /**
         * 通过相册选择图片
         */
        private void openGallery() {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            startActivityForResult(intent, REQUEST_GALLERY_SELECT);
        }

        /**
         * 开始裁剪
         *
         * @param inUri 输入Uri
         */
        private void startCrop(Uri inUri) {
            Intent intent = new Intent("com.android.camera.action.CROP");
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                // 添加这一句表示对目标应用临时授权该Uri所代表的文件
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            }

            // 兼容部分手机无法裁剪相册选择的图片
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                String scheme = inUri.getScheme();
                String path = inUri.getPath();
                // 这个!.jpg是兼容小米有时候content://开头，.jpg结尾
                if (path == null || !path.endsWith(".jpg")) {
                    if (scheme != null && scheme.equals(ContentResolver.SCHEME_CONTENT)) {
                        Cursor cursor = mContext.getContentResolver().query(inUri, null, null, null, null);
                        if (cursor == null) {
                            return;
                        }
                        if (cursor.moveToFirst()) {
                            String id = cursor.getString(cursor.getColumnIndex(MediaStore.Images.Media._ID));
                            inUri = Uri.withAppendedPath(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id);
                        }
                        cursor.close();
                    }
                }
            }

            // 兼容Android11
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                // android 11以上，系统无法裁剪私有目录下的图片，所以将文件创建在公有目录
                mCropOutUri = mPublicUri;
            } else {
                // android 11以下，将文件创建在私有目录
                mCropOutUri = mPrivateUri;
            }

            intent.putExtra(MediaStore.EXTRA_OUTPUT, mCropOutUri);

            // 输入图片路径
            intent.setDataAndType(inUri, "image/*");
            intent.putExtra("crop", "true");
            intent.putExtra("aspectX", 9998);// 2019/5/8 修复华为手机默认为圆角裁剪的问题
            intent.putExtra("aspectY", (int) (9999 * 1.0f / mCropX * mCropY));
            intent.putExtra("scale", true);
            intent.putExtra("scaleUpIfNeeded", true);
            intent.putExtra("outputFormat", Bitmap.CompressFormat.JPEG.toString());
            intent.putExtra("return-data", false);
            try {
                startActivityForResult(intent, REQUEST_CROP);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        /**
         * 是否有拍照权限
         *
         * @return boolean
         */
        private boolean hasCameraPermission() {
            return ActivityCompat.checkSelfPermission(mContext, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
        }

        /**
         * 是否有储存权限
         *
         * @return boolean
         */
        private boolean hasStoragePermission() {
            return ActivityCompat.checkSelfPermission(mContext, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
                    && ActivityCompat.checkSelfPermission(mContext, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
        }

        /**
         * 申请储存权限
         */
        private void requestStoragePermission() {
            requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_PERMISSION);
        }

        /**
         * 申请拍照权限和存储权限
         */
        private void requestCameraAndStoragePermission() {
            requestPermissions(new String[]{Manifest.permission.CAMERA, Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_PERMISSION);
        }

        /**
         * 移动Uri文件
         */
        private void moveUri(Uri inUri, Uri outUri) {
            try {
                InputStream is = mContext.getContentResolver().openInputStream(inUri);
                if (is == null) {
                    return;
                }
                File file = new File(outUri.getPath());
                if (file.exists()) {
                    file.delete();
                }
                file.createNewFile();
                FileOutputStream fos = null;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    fos = new FileOutputStream(file);
                    FileUtils.copy(is, fos);
                } else {
                    ByteArrayOutputStream arrayOutputStream = new ByteArrayOutputStream();
                    byte[] buffer = new byte[1024 * 10];
                    while (true) {
                        int len = is.read(buffer);
                        if (len == -1) {
                            break;
                        }
                        arrayOutputStream.write(buffer, 0, len);
                    }
                    arrayOutputStream.close();
                    byte[] dataByte = arrayOutputStream.toByteArray();
                    if (dataByte.length > 0) {
                        fos = new FileOutputStream(file);
                        fos.write(dataByte);
                    }
                }
                if (fos != null) {
                    fos.close();
                }
                if (is != null) {
                    is.close();
                }
                // 删除公共目录的图片
                mContext.getContentResolver().delete(mPublicUri, null, null);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        /**
         * ContentUri转文件路径
         *
         * @param uri contentUri
         * @return filePath
         */
        private File getFileByContentUri(Uri uri) {
            String filePath;
            String[] filePathColumn = {MediaStore.MediaColumns.DATA};

            Cursor cursor = mContext.getContentResolver().query(uri, filePathColumn, null, null, null);
            cursor.moveToFirst();

            int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
            filePath = cursor.getString(columnIndex);
            cursor.close();
            return new File(filePath);
        }

        /**
         * 文件转ContentUri
         *
         * @param file 文件
         * @return contentUri
         */
        private Uri getContentUriByFile(File file) {
            ContentValues contentValues = new ContentValues();
            contentValues.put(MediaStore.Images.Media.DATA, file.getAbsolutePath());
            contentValues.put(MediaStore.Images.Media.DISPLAY_NAME, file.getName());
            contentValues.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
            return mContext.getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues);
        }


    }

    /**
     * 选择图片后的回调
     */
    public interface OnCallback {
        /**
         * 回调成Uri
         *
         * @param uri media://...
         */
        default void onCallback(Uri uri) {
        }
    }
}
