package com.example.image_manage;

import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;


import com.bigkoo.alertview.AlertView;
import com.bigkoo.alertview.OnItemClickListener;
import com.example.image_manage.bean.Album;
import com.example.image_manage.bean.Photo;
import com.example.image_manage.component.ScreenSizeUtils;
import com.example.image_manage.component.changeState.changeState;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import com.squareup.picasso.Picasso;

import java.io.IOException;
import java.lang.reflect.Type;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import cc.shinichi.library.ImagePreview;
import cc.shinichi.library.view.listener.OnBigImageClickListener;
import cc.shinichi.library.view.listener.OnBigImageLongClickListener;

public class DetailActivity extends AppCompatActivity {
    GridLayout gridLayout;
    ImageView imageView;
    List<String> imageList = new ArrayList<>();
    TextView title;
    private NetworkClient networkClient;
    private static String url = "http://47.113.197.249:8080";
    private static String staticUrl = "http://47.113.197.249:8888/static";
    private int album_id;
    private boolean isLongPress = false;
    private Dialog bottomDialog;
    List<Photo> photoList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);

        changeState.setStatusBarColor(getWindow(),false,getResources().getColor(R.color.main_background));

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setDisplayShowTitleEnabled(false); // 隐藏默认的标题
        }
        Intent intent = getIntent();
        album_id = intent.getIntExtra("id",-1);
        String album_name = intent.getStringExtra("name");
        gridLayout = findViewById(R.id.Imagines);
        imageView = findViewById(R.id.add);
        title = findViewById(R.id.title);
        title.setText(album_name);
        imageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(DetailActivity.this, AddActivity.class);
                intent.putExtra("album_id",album_id);
                startActivity(intent);
            }
        });
        getData();

    }

    private void getData() {
        networkClient = new NetworkClient();
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("albumId",album_id);
        networkClient.fetchData(url+"/api/photos/fetch", jsonObject.toString(), new NetworkClient.NetworkCallback() {
            @Override
            public void onSuccess(final String response) {
                // 使用 Gson 解析 JSON 响应
                try {
                    Gson gson = new Gson();
                    Type albumListType = new TypeToken<List<Photo>>() {}.getType();
                    photoList = gson.fromJson(response, albumListType);
                    // 需要在主线程更新UI
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            // 成功事件
                            setImagines(photoList);
                        }
                    });
                } catch (JsonSyntaxException e) {
                    // 解析失败，处理异常
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(DetailActivity.this, "Failed to parse JSON: " + e.getMessage(), Toast.LENGTH_LONG).show();
                            // 处理解析失败响应
                        }
                    });
                }
            }

            @Override
            public void onFailure(final IOException e) {
                // 需要在主线程更新UI
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(DetailActivity.this, "Request Failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        // 处理失败响应
                    }
                });
            }
        });
    }

    private void setImagines(List<Photo> photoList) {
        gridLayout.removeAllViews();
        imageList.clear();
        for(Photo photo : photoList){
            imageList.add(staticUrl+"/"+photo.getUrl());
        }
        // 动态获取并添加已有的 LinearLayout 布局
        LayoutInflater inflater = LayoutInflater.from(this);
        for (int i = 0; i < photoList.size(); i++) {
            LinearLayout linearLayout = (LinearLayout) inflater.inflate(R.layout.item_img, null);

            // 获取屏幕宽度
            DisplayMetrics displayMetrics = new DisplayMetrics();
            getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
            int screenWidth = displayMetrics.widthPixels;
            // 计算四分之一宽度
            int quarterWidth = screenWidth / 4;


            // 找到 ImageView
            ImageView imageView = linearLayout.findViewById(R.id.image_view);
            // 设置 ImageView 的宽度为四分之一宽度
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(quarterWidth, quarterWidth);
            imageView.setLayoutParams(params);
            Picasso.get().load(staticUrl+"/"+photoList.get(i).getUrl()).into(imageView);
            // 设置 GridLayout.LayoutParams
            GridLayout.LayoutParams params2= new GridLayout.LayoutParams();
            params2.rowSpec = GridLayout.spec(i / 4);  // 计算行号
            params2.columnSpec = GridLayout.spec(i % 4);
            gridLayout.addView(linearLayout, params2);

            // 设置点击事件监听器
            final int index = i;

            linearLayout.setOnTouchListener(new View.OnTouchListener() {
                private Handler handler = new Handler();
                private Runnable longPressRunnable = new Runnable() {
                    @Override
                    public void run() {
                        isLongPress = true;
                        // 处理长按事件
                        showDetail(photoList,index);
                    }

                };

                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    switch (event.getAction()) {
                        case MotionEvent.ACTION_DOWN:
                            isLongPress = false;
                            handler.postDelayed(longPressRunnable, 1000); // 长按时间500毫秒
                            break;
                        case MotionEvent.ACTION_UP:
                            handler.removeCallbacks(longPressRunnable);
                            if (!isLongPress) {
                                ImagePreview
                                        .getInstance()
                                        .setContext(DetailActivity.this)
                                        .setIndex(index)
                                        .setImageList(imageList)
                                        .start();
                            }
                            break;
                        case MotionEvent.ACTION_MOVE:
                            // 如果在移动过程中检测到手指偏移过大，可以取消长按
                            break;
                        case MotionEvent.ACTION_CANCEL:
                            handler.removeCallbacks(longPressRunnable);
                            break;
                    }
                    return true;
                }
            });
        }
    }

    private void showDetail(List<Photo> photoList ,int i) {

        bottomDialog = new Dialog(this, R.style.BottomSheetDialogStyle);
        View view = View.inflate(this, R.layout.detail_layout, null);
        bottomDialog.setContentView(view);
        bottomDialog.setCanceledOnTouchOutside(false);

        Window dialogWindow = bottomDialog.getWindow();
        WindowManager.LayoutParams lp = dialogWindow.getAttributes();
        lp.width = (int) (ScreenSizeUtils.getInstance(this).getScreenWidth() * 0.9f);
        lp.height = WindowManager.LayoutParams.WRAP_CONTENT;
        lp.gravity = Gravity.CENTER;

        lp.type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        bottomDialog.show();

        TextView time = view.findViewById(R.id.time);
        EditText detail = view.findViewById(R.id.name_edit);
        TextView confirm = view.findViewById(R.id.confirm);
        TextView cancel = view.findViewById(R.id.cancel);
        TextView delete = view.findViewById(R.id.delete);
        String Time = convertTimestamp( photoList.get(i).getUrl().replace(".jpg",""));

        time.setText(Time);
        if(photoList.get(i).getComment() != null){
            String comment = photoList.get(i).getComment();
            detail.setText(comment);
        }
        delete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                deletePhoto(i);
                bottomDialog.cancel();
            }
        });
        confirm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                networkClient = new NetworkClient();
                JsonObject jsonObject = new JsonObject();
                jsonObject.addProperty("id",photoList.get(i).getId());
                jsonObject.addProperty("comment",detail.getText().toString());
                networkClient.fetchData(url+"/api/photos/updata", jsonObject.toString(), new NetworkClient.NetworkCallback() {
                    @Override
                    public void onSuccess(final String response) {
                        // 使用 Gson 解析 JSON 响应
                                // 需要在主线程更新UI
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        Toast.makeText(DetailActivity.this, "修改注释成功", Toast.LENGTH_LONG).show();
                                        getData();
                                        bottomDialog.cancel();
                                    }
                                });
                    }
                    @Override
                    public void onFailure(final IOException e) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(DetailActivity.this, "修改注释失败", Toast.LENGTH_LONG).show();
                            }
                        });
                    }
                });
            }
        });
        cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                bottomDialog.cancel();
            }
        });
    }

    private void deletePhoto(int i) {

                        networkClient = new NetworkClient();
                        JsonObject jsonObject = new JsonObject();
                        jsonObject.addProperty("id",photoList.get(i).getId());
                        jsonObject.addProperty("albumId",album_id);
                        networkClient.fetchData(url+"/api/photos/delete", jsonObject.toString(), new NetworkClient.NetworkCallback() {
                            @Override
                            public void onSuccess(final String response) {
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        Toast.makeText(DetailActivity.this, "删除成功", Toast.LENGTH_LONG).show();
                                        getData();
                                    }
                                });
                            }
                            @Override
                            public void onFailure(final IOException e) {
                                // 需要在主线程更新UI
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        Toast.makeText(DetailActivity.this, "Request Failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                                        // 处理失败响应
                                    }
                                });
                            }
                        });

    }


    public static String convertTimestamp(String timestamp) {
        // 时间戳格式化器
        SimpleDateFormat inputFormat = new SimpleDateFormat("yyyyMMddHHmmssSSS");
        // 目标格式化器，只保留到秒
        SimpleDateFormat outputFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        try {
            // 解析时间戳字符串
            Date date = inputFormat.parse(timestamp);
            // 格式化日期
            return outputFormat.format(date);
        } catch (ParseException e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        getData();
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