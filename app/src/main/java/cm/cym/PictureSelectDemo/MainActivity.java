package cm.cym.PictureSelectDemo;

import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.CheckBox;
import android.widget.ImageView;

import com.bumptech.glide.Glide;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private Context mContext;
    private Activity mActivity;

    private ImageView iv_img;
    private CheckBox cb_crop;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mContext = this;
        mActivity = this;
        iv_img = findViewById(R.id.iv_img);
        cb_crop = findViewById(R.id.cb_crop);
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.btn_camera) {
            boolean needCrop = cb_crop.isChecked();
            if (needCrop) {
                // 相机，需要裁剪
                PictureSelectUtil.with(this)
                        .camera()
                        .crop()
                        .setCallback(new PictureSelectUtil.OnCallback() {
                            @Override
                            public void onCallback(Uri uri) {
                                Glide.with(mContext).load(uri).into(iv_img);
                            }
                        }).select();
            } else {
                // 相机，不需要裁剪
                PictureSelectUtil.with(this)
                        .camera()
                        .setCallback(new PictureSelectUtil.OnCallback() {
                            @Override
                            public void onCallback(Uri uri) {
                                Glide.with(mContext).load(uri).into(iv_img);
                            }
                        }).select();
            }
        } else if (id == R.id.btn_gallery) {
            boolean needCrop = cb_crop.isChecked();
            if (needCrop) {
                // 相册，需要裁剪
                PictureSelectUtil.with(this)
                        .gallery()
                        .crop()
                        .setCallback(new PictureSelectUtil.OnCallback() {
                            @Override
                            public void onCallback(Uri uri) {
                                Glide.with(mContext).load(uri).into(iv_img);
                            }
                        }).select();
            } else {
                // 相册，不需要裁剪
                PictureSelectUtil.with(this)
                        .gallery()
                        .setCallback(new PictureSelectUtil.OnCallback() {
                            @Override
                            public void onCallback(Uri uri) {
                                Glide.with(mContext).load(uri).into(iv_img);
                            }
                        }).select();
            }
        }
    }
}