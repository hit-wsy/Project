package com.example.image_manage;

import androidx.appcompat.app.AppCompatActivity;

import android.app.Dialog;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.bigkoo.alertview.AlertView;
import com.bigkoo.alertview.OnItemClickListener;
import com.example.image_manage.bean.Album;
import com.example.image_manage.bean.Photo;
import com.example.image_manage.component.RoundedCornersTransformation;
import com.example.image_manage.component.ScreenSizeUtils;
import com.example.image_manage.component.changeState.changeState;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import com.squareup.picasso.Picasso;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.List;


public class MainActivity extends AppCompatActivity {
    private GridLayout gridLayout;
    private NetworkClient networkClient;
    private RelativeLayout add;
    private Dialog bottomDialog;
    private NetworkClient networkClient2;
    private boolean isLongPress = false;
    private static String url = "http://47.113.197.249:8080";
    private static String staticUrl = "http://47.113.197.249:8888/static";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //设置状态栏背景为灰色，文字为黑色
        changeState.setStatusBarColor(getWindow(),false,getResources().getColor(R.color.main_background));
        setContentView(R.layout.activity_main);
        gridLayout = findViewById(R.id.Imagines);
        add = findViewById(R.id.add);
        //新建照片类别
        add.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                createNewClass();
            }
        });
        getData();
    }

    private void setImagines(List<Album> albumList) {
        gridLayout.removeAllViews();
        // 动态获取并添加已有的 LinearLayout 布局
        LayoutInflater inflater = LayoutInflater.from(this);
        for (int i = 0; i < albumList.size(); i++) {
            LinearLayout linearLayout = (LinearLayout) inflater.inflate(R.layout.layout_item, null);
            // 你可以根据需要修改每个 LinearLayout 中的内容
            TextView name = linearLayout.findViewById(R.id.name);
            //TextView num = linearLayout.findViewById(R.id.num);
            ImageView image = linearLayout.findViewById(R.id.image);

            setFirstP(image,albumList.get(i).getId());

            name.setText(albumList.get(i).getAlbumName());
            //num.setText(albumList.get(i).getPnum() + "");

            // 设置 GridLayout.LayoutParams
            GridLayout.LayoutParams params= new GridLayout.LayoutParams();
            params.rowSpec = GridLayout.spec(i / 3);  // 计算行号
            params.columnSpec = GridLayout.spec(i % 3);  // 计算列号
            gridLayout.addView(linearLayout, params);

            // 设置点击事件监听器
            final int index = i;
            linearLayout.setOnTouchListener(new View.OnTouchListener() {
                    private Handler handler = new Handler();
                    private Runnable longPressRunnable = new Runnable() {
                     @Override
                        public void run() {
                            isLongPress = true;
                            // 处理长按事件
                             deleteClass(albumList.get(index).getAlbumName(),albumList.get(index).getId());
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
                                // 跳转到新界面，并传递数据
                                Intent intent = new Intent(MainActivity.this, DetailActivity.class);
                                intent.putExtra("id", albumList.get(index).getId());
                                intent.putExtra("name", albumList.get(index).getAlbumName());
                                startActivity(intent);
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

    private void setFirstP(ImageView imageView,int id) {
        networkClient = new NetworkClient();
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("id",id);

        networkClient.fetchData(url+"/api/albums/fetchFirstP", jsonObject.toString(), new NetworkClient.NetworkCallback() {
            @Override
            public void onSuccess(final String response) {
                // 使用 Gson 解析 JSON 响应
                try {
                    Gson gson = new Gson();
                    Type Tphoto = new TypeToken<Photo>() {}.getType();
                    Photo photo = gson.fromJson(response, Tphoto);
                    // 需要在主线程更新UI
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            // 成功事件
                            Picasso.get().load(staticUrl+"/"+photo.getUrl()).fit().centerCrop().transform(new RoundedCornersTransformation(20, 0)).into(imageView);
                        }
                    });
                } catch (JsonSyntaxException e) {
                    // 解析失败，处理异常
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(MainActivity.this, "解析失败", Toast.LENGTH_LONG).show();
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
                        //Toast.makeText(MainActivity.this, "没有封面或者出错了~", Toast.LENGTH_LONG).show();
                        Picasso.get().load(R.drawable.no_pic).fit().centerCrop().transform(new RoundedCornersTransformation(20, 0)).into(imageView);
                        // 处理失败响应
                    }
                });
            }
        });
    }

    private void deleteClass(String name,int id) {
        new AlertView.Builder().setContext(MainActivity.this)
                .setStyle(AlertView.Style.ActionSheet)
                .setTitle("是否确认删除类别\""+name+"\"？")
                .setMessage(null)
                .setCancelText("取消")
                .setDestructive("删除")
                .setOthers(null)
                .setOnItemClickListener(new OnItemClickListener() {
                    @Override
                    public void onItemClick(Object o, int position) {
                        if(position == -1)
                            return;
                        networkClient = new NetworkClient();
                        JsonObject jsonObject = new JsonObject();
                        jsonObject.addProperty("id",id);

                        networkClient.fetchData(url+"/api/albums/delete", jsonObject.toString(), new NetworkClient.NetworkCallback() {
                            @Override
                            public void onSuccess(final String response) {
                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            Toast.makeText(MainActivity.this, "删除成功", Toast.LENGTH_LONG).show();
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
                                        Toast.makeText(MainActivity.this, "Request Failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                                        // 处理失败响应
                                    }
                                });
                            }
                        });
                    }
                })
                .build()
                .show();
    }



    private void getData() {
        networkClient = new NetworkClient();
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("id",0);

        networkClient.fetchData(url+"/api/albums/fetch", jsonObject.toString(), new NetworkClient.NetworkCallback() {
            @Override
            public void onSuccess(final String response) {
                // 使用 Gson 解析 JSON 响应
                try {
                    Gson gson = new Gson();
                    Type albumListType = new TypeToken<List<Album>>() {}.getType();
                    List<Album> albumList = gson.fromJson(response, albumListType);
                    // 需要在主线程更新UI
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            // 成功事件
                            setImagines(albumList);
                        }
                    });
                } catch (JsonSyntaxException e) {
                    // 解析失败，处理异常
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(MainActivity.this, "Failed to parse JSON: " + e.getMessage(), Toast.LENGTH_LONG).show();
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
                        Toast.makeText(MainActivity.this, "Request Failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        // 处理失败响应
                    }
                });
            }
        });
    }

    private void createNewClass() {
        bottomDialog = new Dialog(this, R.style.BottomSheetDialogStyle);
        View view = View.inflate(this, R.layout.add_layout, null);
        bottomDialog.setContentView(view);
        bottomDialog.setCanceledOnTouchOutside(false);
        view.setMinimumHeight((int) (ScreenSizeUtils.getInstance(this).getScreenHeight() * 0.23f));
        Window dialogWindow = bottomDialog.getWindow();
        WindowManager.LayoutParams lp = dialogWindow.getAttributes();
        lp.width = (int) (ScreenSizeUtils.getInstance(this).getScreenWidth() * 0.9f);
        lp.height = WindowManager.LayoutParams.WRAP_CONTENT;
        lp.gravity = Gravity.CENTER;
        dialogWindow.setAttributes(lp);
        bottomDialog.show();

        EditText name = view.findViewById(R.id.name_edit);
        TextView confirm = view.findViewById(R.id.confirm);
        TextView cancel = view.findViewById(R.id.cancel);
        confirm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                networkClient2 = new NetworkClient();
                JsonObject jsonObject = new JsonObject();
                jsonObject.addProperty("albumName",name.getText().toString());
                networkClient2.fetchData(url+"/api/albums/create", jsonObject.toString(), new NetworkClient.NetworkCallback() {
                    @Override
                    public void onSuccess(final String response) {
                        // 使用 Gson 解析 JSON 响应
                        try {
                            Gson gson = new Gson();
                            Type albumType = new TypeToken<Album>() {}.getType();
                            Album album = gson.fromJson(response, albumType);
                            if(album != null){
                                // 需要在主线程更新UI
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        Toast.makeText(MainActivity.this, "创建成功", Toast.LENGTH_LONG).show();
                                        getData();
                                        bottomDialog.cancel();
                                    }
                                });

                            }

                        } catch (JsonSyntaxException e) {
                            // 解析失败，处理异常
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    Toast.makeText(MainActivity.this, "解析失败", Toast.LENGTH_LONG).show();
                                }
                            });
                        }
                    }

                    @Override
                    public void onFailure(final IOException e) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(MainActivity.this, "创建失败", Toast.LENGTH_LONG).show();
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

    @Override
    protected void onResume() {
        super.onResume();
        getData();
    }
}