package org.simpmc.simppay.handler;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import okhttp3.*;
import org.simpmc.simppay.data.PaymentStatus;
import org.simpmc.simppay.model.PaymentResult;
import org.simpmc.simppay.model.detail.CardDetail;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public abstract class CardHandler implements CardAdapter, PaymentHandler {
    public CompletableFuture<String> postFormData(List<Map<String, String>> formData, String url) {
        return CompletableFuture.supplyAsync(() -> {

//            TrustManager[] trustAllCerts = new TrustManager[]{
//                    new X509TrustManager() {
//                        @Override
//                        public void checkClientTrusted(java.security.cert.X509Certificate[] chain, String authType) {
//                        }
//
//                        @Override
//                        public void checkServerTrusted(java.security.cert.X509Certificate[] chain, String authType) {
//                        }
//
//                        @Override
//                        public java.security.cert.X509Certificate[] getAcceptedIssuers() {
//                            return new java.security.cert.X509Certificate[]{};
//                        }
//                    }
//            };
//            SSLContext sslContext = null;
//            try {
//                sslContext = SSLContext.getInstance("SSL");
//            } catch (NoSuchAlgorithmException e) {
//                throw new RuntimeException(e);
//            }
//            try {
//                sslContext.init(null, trustAllCerts, new java.security.SecureRandom());
//            } catch (KeyManagementException e) {
//                throw new RuntimeException(e);
//            }
//            Proxy proxy = new Proxy(
//                    Proxy.Type.HTTP,
//                    new InetSocketAddress("127.0.0.1", 8000)
//            );
//            OkHttpClient.Builder newBuilder = new OkHttpClient.Builder();
//            newBuilder.sslSocketFactory(sslContext.getSocketFactory(), (X509TrustManager) trustAllCerts[0]);
//            newBuilder.hostnameVerifier((hostname, session) -> true);
//            newBuilder.proxy(proxy);

            OkHttpClient client = new OkHttpClient.Builder().build();

            // 1) Build the multipart/form-data body
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

            // 2) Build the request
            Request request = new Request.Builder()
                    .url(url)
                    .post(requestBody)
                    .build();

            // 3) Enqueue the call (asynchronous)
            Call call = client.newCall(request);
            try {
                Response response = call.execute();
                return response.body().string();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });

    }

    protected PaymentResult getNencerAPIResult(CardDetail detail, String response) {
        JsonObject jsonResponse = JsonParser.parseString(response).getAsJsonObject();
        if (jsonResponse.get("status").getAsInt() == 1) {
            return new PaymentResult(
                    PaymentStatus.SUCCESS,
                    (int) detail.getAmount(),
                    jsonResponse.get("message").getAsString()
            );

        }
        if (jsonResponse.get("status").getAsInt() == 2) {
            return new PaymentResult(
                    PaymentStatus.WRONG_PRICE,
                    jsonResponse.get("value").getAsInt(),
                    "Sai menh gia, menh gia thuc la: " + jsonResponse.get("declared_value").getAsString()
            );
        }
        if (jsonResponse.get("status").getAsInt() == 3) {
            return new PaymentResult(
                    PaymentStatus.FAILED,
                    (int) detail.getAmount(),
                    jsonResponse.get("message").getAsString()
            );
        }
        if (jsonResponse.get("status").getAsInt() == 99) {
            return new PaymentResult(
                    PaymentStatus.PENDING,
                    (int) detail.getAmount(),
                    jsonResponse.get("message").getAsString()
            );
        }
        return new PaymentResult(
                PaymentStatus.FAILED,
                (int) detail.getAmount(),
                ""
        );
    }
}
