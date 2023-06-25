# PictureSelectDemo
简单的兼容Android11的图片选择，拍照，裁剪工具类，单文件，无需FileProvider

### 2021/06/21

<img src="https://raw.githubusercontent.com/yxzwym/Blog/main/static/img/2021/Screenshot_20210621_173253.3e22jjzz7so0.jpg" width=300 />

网上的要么是过时的，要么就过于臃肿，甚至自己封装了裁剪类

自己在做项目的时候，最多就是选择个头像，换一下封面图之类的，根本不需要那么臃肿的第三方库

所以抽时间写了个工具类，理论上兼容Android4.2.2 ~ 11

### 兼容性

| 型号 | 版本 | 结果 |
| :-- | :-- | :-: |
| pixel3 | 11 | true |
| vivo iqoo neo3 | 11 | true |
| OPPO A52 | 11 | true |
| MIX 2S | 10 | true |
| Meizu Note6 | 7.1.2 | true |
| vivo X9i | 7.1.2 | true |
| nubia NX569H | 6.0.1 | true |

没有更低版本的手机了，理论上越低版本权限越不严格，应该是不会有问题的

## 使用方法

将 `PictureSelectUtil.java` 复制到你自己的项目里

### 拍照选择
```
PictureSelectUtil.with(this)
    .camera()
    .crop()
    .setCallback(new PictureSelectUtil.OnCallback() {
        @Override
        public void onCallback(Uri uri) {
            Glide.with(mContext).load(uri).into(iv_img);
        }
    }).select();
```

### 相册选择
```
PictureSelectUtil.with(this)
    .gallery()
    .crop()
    .setCallback(new PictureSelectUtil.OnCallback() {
        @Override
        public void onCallback(Uri uri) {
            Glide.with(mContext).load(uri).into(iv_img);
        }
    }).select();
```

如果不需要裁剪就把 `.crop()` 去掉

另外我只在Activity下试过，不知道Fragment里能不能用

无所谓了，反正是写给自己用的，只要客户不投诉，就没有bug
