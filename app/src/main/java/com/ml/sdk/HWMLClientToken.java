package com.ml.sdk;

import android.graphics.Bitmap;
import android.util.Log;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;


public class HWMLClientToken {
    private String domainName;
    private String userName;
    private String password;
    private String region;
    private String mlToken = null;
    private long tokenTimestampMS = 0;
    private long tokenExpireMS = 24 * 3600 * 1000;


    public HWMLClientToken(String domainName, String userName, String password, String region) {
        this.domainName = domainName;
        this.userName = userName;
        this.password = password;
        this.region = region;
    }

    //  set required parameters to request the token of ml service
    private String requestBody() {
        JSONObject auth = new JSONObject();
        JSONObject identity = new JSONObject();
        JSONArray methods = new JSONArray();
        methods.add("password");
        identity.put("methods", methods);
        JSONObject pw = new JSONObject();
        JSONObject user = new JSONObject();
        user.put("name", userName);
        user.put("password", password);
        JSONObject domain = new JSONObject();
        domain.put("name", domainName);
        user.put("domain", domain);
        pw.put("user", user);
        identity.put("password", pw);
        JSONObject scope = new JSONObject();
        JSONObject scopeProject = new JSONObject();
        scopeProject.put("name", region);
        scope.put("project", scopeProject);
        auth.put("identity", identity);
        auth.put("scope", scope);
        JSONObject params = new JSONObject();
        params.put("auth", auth);
        return params.toJSONString();
    }

    /**
     * return ml token
     */
    public String getToken() {
        if (System.currentTimeMillis() - tokenTimestampMS < tokenExpireMS) {
            return mlToken;
        }
        final CountDownLatch latch = new CountDownLatch(1);
        new Thread() {
            @Override
            public void run() {
                String requestBodyJsonStr = requestBody();
                String url = "https://iam." + region + ".myhuaweicloud.com/v3/auth/tokens";
                OkHttpClient okHttpClient = new OkHttpClient();
                RequestBody requestBody = RequestBody.create(
                        MediaType.parse("application/json; charset=utf-8"), requestBodyJsonStr);
                Request request = new Request.Builder()
                        .url(url)
                        .post(requestBody)
                        .build();
                Call call = okHttpClient.newCall(request);
                call.enqueue(new Callback() {
                    @Override
                    public void onFailure(Call call, IOException e) {
                        Log.e("onFailure", "connect failed", e);
                        latch.countDown();
                    }

                    @Override
                    public void onResponse(Call call, Response response) throws IOException {
                        Log.d("onResponse", response.body().toString());
                        try {
                            if (response.header("X-Subject-Token") != null) {
                                mlToken = response.header("X-Subject-Token").toString();
                                tokenTimestampMS = System.currentTimeMillis();
                            } else {
                                Log.e("token", "Get token null, please check user proxy");
                            }
                        } catch (Exception e) {
                            Log.e("token", "response error: ", e);
                        }
                        latch.countDown();
                    }
                });

            }
        }.start();

        try {
            latch.await();
        } catch (Exception e) {
            Log.e("token", "get error: ", e);
        }
        return mlToken;
    }

    /**
     * request ml service
     *
     * @param uri      ml request URI
     * @param bit      detective image
     * @param option   optional parameters
     * @param callback the callback of success or Failure
     */

    public void requestmlTokenService(String uri, Bitmap bit, Map<String, String> option, Callback callback) {
        getToken();
        String fileBase64Str = BitmapUtil.bitmapToBase64(bit);
        JSONObject requestBodyJsonStr = new JSONObject();
        requestBodyJsonStr.put("images", fileBase64Str);
        if (option != null) {
            for (String k : option.keySet()) {
                requestBodyJsonStr.put(k, option.get(k));
            }
        }
        OkHttpClient okHttpClient = new OkHttpClient();
        RequestBody requestBody = RequestBody.create(MediaType.parse("application/json; charset=utf-8"), requestBodyJsonStr.toJSONString());
        okhttp3.Request request = new okhttp3.Request.Builder()
                .url(uri)
                .post(requestBody)
                .addHeader("X-Auth-Token", mlToken)
                .build();
        Call call = okHttpClient.newCall(request);
        call.enqueue(callback);
    }

    /**
     * request ml service
     * @param uri
     * @param file
     * @param option
     * @param callback
     */
    public void requestmlTokenServiceByFile(String uri, File file, Map<String, String> option, Callback callback) {
        getToken();
        MultipartBody.Builder requestBody = new MultipartBody.Builder().setType(MultipartBody.FORM);
        RequestBody body = RequestBody.create(MediaType.parse("multipart/form-data"), file);
        String filename = file.getName();
        requestBody.addFormDataPart("images", filename, body);
        OkHttpClient okHttpClient = new OkHttpClient();
        okhttp3.Request request = new okhttp3.Request.Builder()
                .url(uri)
                .post(requestBody.build())
                .addHeader("X-Auth-Token", mlToken)
                .build();
        Call call = okHttpClient.newCall(request);
        call.enqueue(callback);
    }
}
