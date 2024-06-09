package com.example.image_manage;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.FileProvider;

import android.app.Dialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.image_manage.component.changeState.changeState;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class AddActivity extends AppCompatActivity {
    private LinearLayout shot;
    private TextView accept;
    private Bitmap bmp;
    private RelativeLayout photoContent;
    private String img;
    private final int REQUEST_ALBUM = 200;
    private static String url = "http://47.113.197.249:8080";
    private NetworkClient networkClient;
    int album_id;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        changeState.setStatusBarColor(getWindow(),false,getResources().getColor(R.color.main_background));
        setContentView(R.layout.activity_add);
        Intent intent = getIntent();
        album_id = intent.getIntExtra("album_id",-1);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setDisplayShowTitleEnabled(false); // 隐藏默认的标题
        }
        initComponent();
    }

    private void initComponent() {
        shot = findViewById(R.id.shot);
        photoContent = findViewById(R.id.content_photo);
        accept = findViewById(R.id.accept);
        shot.setOnClickListener(this::Album);
        accept.setOnClickListener(this::upload);
    }


    private void Album(View v) {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setDataAndType(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "image/*");
        startActivityForResult(intent, REQUEST_ALBUM);
    }

    private void changePhoto(Uri uri) {
        ImageView imgView = new ImageView(this);
        try {
            bmp = BitmapFactory.decodeStream(getContentResolver().openInputStream(uri));
            imgView.setImageBitmap(bmp);
        } catch (FileNotFoundException e) {
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
            return;
        }
        ConstraintLayout.LayoutParams params = new ConstraintLayout.LayoutParams(CommonUtils.dp2px(this, 300), CommonUtils.dp2px(this, 200));
        params.startToStart = ConstraintLayout.LayoutParams.PARENT_ID;
        params.endToEnd = ConstraintLayout.LayoutParams.PARENT_ID;
        params.topToTop = ConstraintLayout.LayoutParams.PARENT_ID;
        params.bottomToBottom = ConstraintLayout.LayoutParams.PARENT_ID;
        params.topMargin = CommonUtils.dp2px(this, 5);
        imgView.setLayoutParams(params);
        imgView.setOnClickListener(this::Album);
        photoContent.removeAllViews();
        photoContent.addView(imgView);
    }

    public static String bitmapToBase64(Bitmap bitmap) {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream);
        byte[] byteArray = byteArrayOutputStream.toByteArray();
        return Base64.encodeToString(byteArray, Base64.DEFAULT);
    }

    private void upload(View v) {
        if (!check()) {
            return;
        }

        if (bmp != null) {
            try {
                Bitmap bmp1 = bmp;
                while (bmp1.getRowBytes() > 1024 * 1024 * 4) {
                    bmp1 = compressQuality(bmp, 80);
                }
                img = bitmapBase64Utils.bitmapToBase64(bmp1);
                img = img.replace("\n", "").replace("\r", "");
                networkClient = new NetworkClient();
                JsonObject jsonObject = new JsonObject();
                jsonObject.addProperty("data",img);
                jsonObject.addProperty("albumId",album_id);

                networkClient.fetchData(url+"/api/photos/create", jsonObject.toString(), new NetworkClient.NetworkCallback() {
                    @Override
                    public void onSuccess(final String response) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(AddActivity.this, "上传成功", Toast.LENGTH_LONG).show();
                                finish();
                            }
                        });
                    }
                    @Override
                    public void onFailure(final IOException e) {
                        // 需要在主线程更新UI
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(AddActivity.this, "Request Failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                                // 处理失败响应
                            }
                        });
                    }
                });
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private boolean check() {
        if (bmp == null) {
            Toast.makeText(this, "请上传照片", Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    private Bitmap compressQuality(Bitmap bm, int quality) {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        bm.compress(Bitmap.CompressFormat.JPEG, quality, bos);
        byte[] bytes = bos.toByteArray();
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
         if (requestCode == REQUEST_ALBUM) {
            if (resultCode == RESULT_OK) {
                Uri uri = data.getData();
                changePhoto(uri);
            } else if (resultCode != RESULT_CANCELED) {
                Toast.makeText(this, "未知错误", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PermissionUtils.REQUEST_CODE) {
            for (int grantResult : grantResults) {
                if (grantResult != PackageManager.PERMISSION_GRANTED) {
                    Log.e("Permission", "授权失败！");
                    Toast.makeText(this, "获取授权失败", Toast.LENGTH_SHORT).show();
                    return;
                }
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
