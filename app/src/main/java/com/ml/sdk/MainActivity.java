package com.ml.sdk;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    private ImageView ivLocalImage;//图片显示控件
    private Bitmap localBitmap;//手机相册图片转bitmap
    private String localResult;//手机图片识别结果
    private TextView tvResult;//显示结果的文本框
    private Uri imageUri;
    private HWMLClientToken ocrToken;

    //请求状态码
    private static final int REQUEST_PERMISSION_CODE = 1;
    private static final int REQUEST_GALLERY_CODE = 2;
    private static final int REQUEST_TAKE_PHOTO_CODE = 3;

    //首先声明一个数组permissions，将所有需要申请的权限都放在里面
    String[] permissions = new String[]{Manifest.permission.CAMERA,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };
    //创建一个mPermissionList，逐个判断哪些权限未授权，将未授权的权限存储到mPermissionList中
    List<String> mPermissionList = new ArrayList<>();

    String domainName = ""; // if the user isn't IAM user, domain_name is the same with username
    String userName = "";
    String password = "";
    private String region = "cn-south-1";
    private String url = "https://86a244ebacee403eb4a6897beb88b5f8.apigw.cn-north-1.huaweicloud.com/v1/infers/67bf2303-c36e-4b9a-963c-885653beedc5";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        findViewById(R.id.btn_takephoto).setOnClickListener(this);
        findViewById(R.id.btn_choose).setOnClickListener(this);
        ivLocalImage = findViewById(R.id.iv_img);
        tvResult = findViewById(R.id.tv_result);
        localBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.pic_02);
        initData();
        //动态申请相机和读写权限
        initPermission();
        //用户认证
        ocrToken = new HWMLClientToken(domainName, userName, password, region);
    }

    private void initData() {
        domainName = getMetaDataValue("DNAME");
        userName = getMetaDataValue("UNAME");
        password = getMetaDataValue("UPWD");
    }
    File outputImage;
    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_takephoto://拍照
                //创建File对象，用于存储拍照后的图片
                outputImage = new File(getExternalCacheDir(), "outputImage.jpg");
                try {
                    if (outputImage.exists()) {
                        outputImage.delete();
                    }
                    outputImage.createNewFile();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                if (Build.VERSION.SDK_INT >= 24) {
                    imageUri = FileProvider.getUriForFile(this,
                            "com.ocr.sdk.provider", outputImage);
                } else {
                    imageUri = Uri.fromFile(outputImage);
                }
                //启动相机程序
                Intent intent = new Intent("android.media.action.IMAGE_CAPTURE");
                intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
                startActivityForResult(intent, REQUEST_TAKE_PHOTO_CODE);
                break;
            case R.id.btn_choose://选择手机相册图片
                Intent intentFromGallery = new Intent(Intent.ACTION_PICK, null);
                intentFromGallery.setDataAndType(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "image/*");
                startActivityForResult(intentFromGallery, REQUEST_GALLERY_CODE);
                break;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case REQUEST_GALLERY_CODE://相册返回结果
                if (resultCode == RESULT_CANCELED) {
                    Toast.makeText(getApplication(), "点击取消从相册选择", Toast.LENGTH_LONG).show();
                    return;
                }
                try {
                    Uri uri = data.getData();
                    String filePath = getRealPathFromURI(uri);
                    localBitmap = getResizePhoto(filePath);
                    ivLocalImage.setImageBitmap(localBitmap);
                    if (localBitmap != null) {
                        //请求机器识别服务
                        tvResult.setText("");
                        requestMlTokenService(new File(filePath));
                    } else {
                        Log.e("error", "localBitmap is null");
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;
            case REQUEST_TAKE_PHOTO_CODE://拍照结果
                if (resultCode == RESULT_OK) {
                    try {
                        //将拍摄的照片显示出来
                        tvResult.setText("");
                        requestMlTokenService(outputImage);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                break;
        }
    }

    /**
     * 请求机器识别服务
     *
     * @param file
     */
    private void requestMlTokenService(File file) {
        ocrToken.requestmlTokenServiceByFile(url, file, null, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                localResult = e.toString();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        tvResult.setText(localResult);
                    }
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                localResult = response.body().string();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        //显示识别结果
                        tvResult.setText(localResult);
                    }
                });
            }
        });
    }

    /**
     * 从URI获取String类型的文件路径
     *
     * @param contentUri
     * @return
     */
    public String getRealPathFromURI(Uri contentUri) {
        Cursor cursor = null;
        try {
            String[] projection = {MediaStore.Images.Media.DATA};
            //由context.getContentResolver()获取contentProvider再获取cursor(游标）用游标获取文件路径返回
            cursor = this.getContentResolver().query(contentUri, projection, null, null, null);
            cursor.moveToFirst();
            int columnIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
            return cursor.getString(columnIndex);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

    }

    /**
     * 根据文件路径调整图片大小防止OOM并且返回bitmap
     *
     * @param ImagePath
     * @return
     */
    private Bitmap getResizePhoto(String ImagePath) {
        if (ImagePath != null) {
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(ImagePath, options);
            double ratio = Math.max(options.outWidth * 1.0d / 1024f, options.outHeight * 1.0d / 1024);
            options.inSampleSize = (int) Math.ceil(ratio);
            options.inJustDecodeBounds = false;
            Bitmap bitmap = BitmapFactory.decodeFile(ImagePath, options);
            return bitmap;
        }
        return null;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        boolean hasPermissionDismiss = false;//有权限没有通过
        if (REQUEST_PERMISSION_CODE == requestCode) {
            for (int i = 0; i < grantResults.length; i++) {
                if (grantResults[i] == -1) {
                    hasPermissionDismiss = true;
                    break;
                }
            }
        }
        if (hasPermissionDismiss) {//如果有没有被允许的权限
            showPermissionDialog();
        } else {
            //权限已经都通过了，可以将程序继续打开了
        }
    }

    /**
     * 权限未通过弹框
     */
    private AlertDialog alertDialog;

    private void showPermissionDialog() {
        if (alertDialog == null) {
            alertDialog = new AlertDialog.Builder(this)
                    .setMessage("有权限未通过，部分功能可能不能正常使用，请手动授予")
                    .setPositiveButton("设置", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            cancelPermissionDialog();
                            //去手机系统设置权限
                            Intent intent = new Intent();
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            if (Build.VERSION.SDK_INT >= 9) {
                                intent.setAction("android.settings.APPLICATION_DETAILS_SETTINGS");
                                intent.setData(Uri.fromParts("package", getPackageName(), null));
                            } else if (Build.VERSION.SDK_INT <= 8) {
                                intent.setAction(Intent.ACTION_VIEW);
                                intent.setClassName("com.android.settings", "com.android.settings.InstalledAppDetails");
                                intent.putExtra("com.android.settings.ApplicationPkgName", getPackageName());
                            }
                            startActivity(intent);
                        }
                    })
                    .setNegativeButton("取消", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            //关闭页面或者做其他操作
                            cancelPermissionDialog();
                            MainActivity.this.finish();
                        }
                    })
                    .create();
        }
        alertDialog.show();
    }

    /**
     * 取消弹框
     */
    private void cancelPermissionDialog() {
        alertDialog.cancel();
    }

    //权限判断和申请
    private void initPermission() {
        mPermissionList.clear();//清空已经允许的没有通过的权限
        //逐个判断是否还有未通过的权限
        for (int i = 0; i < permissions.length; i++) {
            if (ContextCompat.checkSelfPermission(this, permissions[i]) !=
                    PackageManager.PERMISSION_GRANTED) {
                mPermissionList.add(permissions[i]);//添加还未授予的权限到mPermissionList中
            }
        }
        //申请权限
        if (mPermissionList.size() > 0) {//有权限没有通过，需要申请
            ActivityCompat.requestPermissions(this, permissions, REQUEST_PERMISSION_CODE);
        } else {
            //权限已经都通过了，可以将程序继续打开了
        }
    }

    /**
     * 获取metadata
     * @param metaDataName
     * @return
     */
    public  String getMetaDataValue( String metaDataName) {
        PackageManager pm = getPackageManager();
        ApplicationInfo appinfo;
        String metaDataValue = "";
        try {
            appinfo = pm.getApplicationInfo(getPackageName(),PackageManager.GET_META_DATA);
            Bundle metaData = appinfo.metaData;
            metaDataValue = metaData.getString(metaDataName);
            return metaDataValue;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return metaDataValue;
    }
}
