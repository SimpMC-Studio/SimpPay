package org.simpmc.simppay.util;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class HashUtil {

    public static String md5(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] messageDigest = md.digest(input.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : messageDigest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("MD5 algorithm not found", e);
        }
    }

    public static String randomMD5() {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] messageDigest = md.digest((System.currentTimeMillis() + (long) new Random().nextInt(1000000) + "").getBytes("UTF-8"));
            BigInteger no = new BigInteger(1, messageDigest);
            StringBuilder hashtext = new StringBuilder(no.toString(16));
            while (hashtext.length() < 32) {
                hashtext.insert(0, "0");
            }
            return hashtext.toString();
        } catch (Exception e) {
            return "";
        }
    }

    public static String hmacSha256Hex(String key, String data) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(key.getBytes("UTF-8"), "HmacSHA256"));
        byte[] rawHmac = mac.doFinal(data.getBytes("UTF-8"));
        StringBuilder hex = new StringBuilder(2 * rawHmac.length);
        for (byte b : rawHmac) {
            String h = Integer.toHexString(Byte.toUnsignedInt(b));
            if (h.length() == 1) {
                hex.append('0');
            }
            hex.append(h);
        }
        return hex.toString();
    }

    public static boolean isValidData(JsonObject jsonObject, String expectedSignature, String checksumKey) {
        try {
            List<String> keys = new ArrayList<>(jsonObject.keySet());
            Collections.sort(keys);
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < keys.size(); i++) {
                String key = keys.get(i);
                JsonElement elem = jsonObject.get(key);
                String value = elem.isJsonNull() ? "" : elem.getAsString();
                sb.append(key).append('=').append(value);
                if (i < keys.size() - 1) {
                    sb.append('&');
                }
            }
            String actualSignature = hmacSha256Hex(checksumKey, sb.toString());
            return actualSignature.equalsIgnoreCase(expectedSignature);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}
