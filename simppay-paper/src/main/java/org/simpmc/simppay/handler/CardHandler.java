package org.simpmc.simppay.handler;

import okhttp3.*;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public abstract class CardHandler implements CardAdapter, PaymentHandler {

    public CompletableFuture<String> postFormData(List<Map<String, String>> formData, String url) {
        return CompletableFuture.supplyAsync(() -> {
            OkHttpClient client = new OkHttpClient.Builder().build();

            MultipartBody.Builder requestBodyBuilder = new MultipartBody.Builder()
                    .setType(MultipartBody.FORM);

            for (Map<String, String> field : formData) {
                for (Map.Entry<String, String> entry : field.entrySet()) {
                    String key = entry.getKey();
                    String value = entry.getValue();
                    if (value != null) {
                        requestBodyBuilder.addFormDataPart(key, value);
                    }
                }
            }
            RequestBody requestBody = requestBodyBuilder.build();

            Request request = new Request.Builder()
                    .url(url)
                    .post(requestBody)
                    .build();

            Call call = client.newCall(request);
            try {
                Response response = call.execute();
                return response.body().string();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }
}
