package com.example.image_manage;

import okhttp3.*;

import java.io.IOException;

public class NetworkClient {

    public static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    private OkHttpClient client;

    public NetworkClient() {
        client = new OkHttpClient();
    }

    public interface NetworkCallback {
        void onSuccess(String response);
        void onFailure(IOException e);
    }

    public void fetchData(String url, String json, final NetworkCallback callback) {
        RequestBody body = RequestBody.create(json, JSON);
        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                callback.onFailure(e);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    callback.onSuccess(response.body().string());
                } else {
                    callback.onFailure(new IOException("Unexpected code " + response));
                }
            }
        });
    }
}
