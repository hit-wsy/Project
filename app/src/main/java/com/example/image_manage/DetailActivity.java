package com.example.image_manage;

import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;


import com.example.image_manage.bean.Album;
import com.example.image_manage.bean.Photo;
import com.example.image_manage.component.changeState.changeState;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import com.squareup.picasso.Picasso;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import cc.shinichi.library.ImagePreview;

public class DetailActivity extends AppCompatActivity {
    GridLayout gridLayout;
    ImageView imageView;
    List<String> imageList = new ArrayList<>();
    TextView title;
    private NetworkClient networkClient;
    private static String url = "http://47.113.197.249:8080";
    private static String staticUrl = "http://47.113.197.249:8888/static";
    int album_id;

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
                    List<Photo> photoList = gson.fromJson(response, albumListType);
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
            params2.columnSpec = GridLayout.spec(i % 4);  // 计算列号
            gridLayout.addView(linearLayout, params2);

            // 设置点击事件监听器
            final int index = i;
            linearLayout.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    ImagePreview
                    .getInstance()
                    .setContext(DetailActivity.this)
                    .setIndex(index)
                    .setImageList(imageList)
                    .start();
                }
            });
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