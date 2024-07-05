package com.example.image_manage;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.PopupMenu;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.bigkoo.alertview.AlertView;
import com.bigkoo.alertview.OnItemClickListener;
import com.example.image_manage.bean.Album;
import com.example.image_manage.bean.Photo;
import com.example.image_manage.bean.User;
import com.example.image_manage.component.RoundedCornersTransformation;
import com.example.image_manage.component.ScreenSizeUtils;
import com.example.image_manage.component.changeState.changeState;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import com.squareup.picasso.Picasso;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;


public class MainActivity extends AppCompatActivity {
    private GridLayout gridLayout;
    private NetworkClient networkClient;
    private RelativeLayout add;
    private LinearLayout switchUser;
    private Dialog bottomDialog;
    private NetworkClient networkClient2;
    private boolean isLongPress = false;
    private static String url = "http://47.113.197.249:8080";
    private static String staticUrl = "http://47.113.197.249:8888/static";
    private List<String> userList;
    private CustomAdapter adapter;
    private TextView user;
    private SharedPreferences sharedPreferences;
    private String value,valueNow;
    List<User> AllUsers;
    private int userid;
    Map<String, String> list;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //设置状态栏背景为灰色，文字为黑色
        changeState.setStatusBarColor(getWindow(),false,getResources().getColor(R.color.main_background));
        setContentView(R.layout.activity_main);
        userList = new ArrayList<>();
        fetchUserList();
        sharedPreferences = getSharedPreferences("MyPreferences", Context.MODE_PRIVATE);
//        SharedPreferences.Editor editor = sharedPreferences.edit();
//        editor.putString("key", "");
//        editor.putString("now", "");
//        editor.apply();
        value = sharedPreferences.getString("key", "");
        valueNow = sharedPreferences.getString("now", " ");



        user = findViewById(R.id.user);
        gridLayout = findViewById(R.id.Imagines);
        add = findViewById(R.id.add);
        //新建照片类别
        add.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                createNewClass();
            }
        });
        if(value.equals("")){
            Toast.makeText(MainActivity.this, "请新创建一个用户哦~", Toast.LENGTH_SHORT).show();
            add.setClickable(false);
        }else{
            add.setClickable(true);
            list = StringToMapConverter.convertStringToMap(value);
            userList = new ArrayList<>();
            for(String s : list.keySet()){
                userList.add(s);
            }
            user.setText(valueNow);
            userid = Integer.parseInt(list.get(valueNow));
            getData();

        }

        switchUser = findViewById(R.id.switchUser);
        switchUser.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showCustomMenu();
            }
        });
        getData();
    }

    private void showCustomMenu() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        ListView listView = new ListView(this);
        adapter = new CustomAdapter(this, userList);
        listView.setAdapter(adapter);

        builder.setView(listView);
        AlertDialog dialog = builder.create();

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                dialog.dismiss();
                if (position == userList.size()) {
                    showNewUserDialog();
                } else {
                    String selectedUser = userList.get(position);
                    Toast.makeText(MainActivity.this, "切换到 " + selectedUser, Toast.LENGTH_SHORT).show();
                    SharedPreferences.Editor editor = sharedPreferences.edit();
                    editor.putString("now", selectedUser);
                    editor.apply();
                    for(User user : AllUsers){
                        if(user.getUserName().equals(selectedUser))
                            userid = user.getId();
                    }
                    getData();
                    user.setText(selectedUser);
                }
            }
        });

        dialog.show();
    }

    private void showNewUserDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.CustomAlertDialogTheme);
        LayoutInflater inflater = this.getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_new_user, null);
        builder.setView(dialogView);

        final EditText newUserEditText = dialogView.findViewById(R.id.new_user_name);

        builder.setTitle("用户新建/登录")
                .setPositiveButton("创建/登录", (dialog, which) -> {
                    String newUser = newUserEditText.getText().toString();
                    if (!newUser.isEmpty()) {
                        userList.add(newUser);
                        adapter.notifyDataSetChanged();
                        addUser(newUser);
                        add.setClickable(true);
                    } else {
                        Toast.makeText(MainActivity.this, "用户名不能为空", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("取消", (dialog, which) -> dialog.dismiss());

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void addUser(String username) {
        networkClient = new NetworkClient();
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("userName",username);

        networkClient.fetchData(url+"/api/users/create", jsonObject.toString(), new NetworkClient.NetworkCallback() {
            @Override
            public void onSuccess(final String response) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(MainActivity.this, "用户登录成功", Toast.LENGTH_LONG).show();
                        add.setClickable(true);
                        fetchUserList();
                        userid  = extractId(response);
                        user.setText(username);
                        SharedPreferences.Editor editor = sharedPreferences.edit();
                        editor.putString("key", value+"#"+username+"#"+userid);
                        editor.putString("now", username);
                        editor.apply();
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


    private void setImagines(List<Album> albumList) {
        gridLayout.removeAllViews();
        // 动态获取并添加已有的 LinearLayout 布局
        LayoutInflater inflater = LayoutInflater.from(this);
        for (int i = 0; i < albumList.size(); i++) {
            LinearLayout linearLayout = (LinearLayout) inflater.inflate(R.layout.layout_item, null);
            // 你可以根据需要修改每个 LinearLayout 中的内容
            TextView name = linearLayout.findViewById(R.id.name);
            TextView num = linearLayout.findViewById(R.id.num);
            ImageView image = linearLayout.findViewById(R.id.image);

            if(albumList.get(i).getpNum() == 0){
                Picasso.get().load(R.drawable.no_pic).fit().centerCrop().transform(new RoundedCornersTransformation(20, 0)).into(image);
            }else{
                Picasso.get().load(staticUrl+"/"+albumList.get(i).getFirstP()).fit().centerCrop().transform(new RoundedCornersTransformation(20, 0)).into(image);
            }

            name.setText(albumList.get(i).getAlbumName());
            num.setText(albumList.get(i).getpNum() + "");

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
        jsonObject.addProperty("userId",userid);
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
                jsonObject.addProperty("userId",userid);
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

    private void fetchUserList(){
        networkClient = new NetworkClient();
        JsonObject jsonObject = new JsonObject();
        networkClient.fetchData("http://47.113.197.249:8080/api/users/fetch", jsonObject.toString(), new NetworkClient.NetworkCallback() {
            @Override
            public void onSuccess(final String response) {
                // 使用 Gson 解析 JSON 响应
                try {
                    Gson gson = new Gson();
                    Type UserListType = new TypeToken<List<User>>() {}.getType();
                    List<User> UserList = gson.fromJson(response, UserListType);
                    AllUsers = UserList;
                } catch (JsonSyntaxException e) {

                }
            }

            @Override
            public void onFailure(final IOException e) {
                // 需要在主线程更新UI

            }
        });
    }

    public static int extractId(String jsonString) {
        JsonObject jsonObject = JsonParser.parseString(jsonString).getAsJsonObject();
        return jsonObject.get("id").getAsInt();
    }

    @Override
    protected void onResume() {
        super.onResume();
        getData();
    }
}