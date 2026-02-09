package org.simpmc.simppay.handler;

import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import org.simpmc.simppay.util.MessageUtil;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;

public abstract class BankHandler implements PaymentHandler {
    public CompletableFuture<String> get(String url) {
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

            // 1) Build the request

            Request request = new Request.Builder()
                    .url(url)
                    .get()
                    .build();

            // 2) Enqueue the call (asynchronous)
            Call call = client.newCall(request);
            try {
                Response response = call.execute();
                return response.body().string();
            } catch (IOException e) {
                MessageUtil.warn("[BankHandler] GET request failed for URL: " + url + " - " + e.getMessage());
                throw new RuntimeException(e);
            }
        });
    }
}
