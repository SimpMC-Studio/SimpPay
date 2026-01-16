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

public class HashUtils {
    public static String randomMD5() {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] messageDigest = md.digest((System.currentTimeMillis() + (long) randomInt(0, 999999) + "").getBytes("UTF-8"));
            BigInteger no = new BigInteger(1, messageDigest);
            StringBuilder hashtext = new StringBuilder(no.toString(16));

            while (hashtext.length() < 32) {
                hashtext.insert(0, "0");
            }

            return hashtext.toString();
        } catch (Exception var4) {
            return "";
        }
    }

    public static String md5Hash(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(input.getBytes());
            StringBuilder hexString = new StringBuilder();

            for (byte b : digest) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }

            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("MD5 algorithm not available.", e);
        }
    }

    public static int randomInt(int min, int max) {
        return (new Random()).nextInt(max - min + 1) + min;
    }

    public static int randomInt(String min, String max) {
        return randomInt(ri(min), ri(max));
    }

    public static double randomDouble(double min, double max) {
        Random r = new Random();
        return min + (max - min) * r.nextDouble();
    }

    public static double randomDouble(String min, String max) {
        return randomDouble(rd(max), rd(min));
    }

    public static double randomDoubleNnega(double d) {
        double nega = d * -1.0D;
        return Math.random() * (d - nega) + nega;
    }

    public static double rd(String s) {
        return Double.valueOf(s);
    }

    public static int ri(String s) {
        return Integer.valueOf(s);
    }

    public static boolean isValidData(JsonObject jsonObject, String expectedSignature, String checksumKey) {
        try {

            // Extract keys and sort
            List<String> keys = new ArrayList<>(jsonObject.keySet());
            Collections.sort(keys);

            // Build the data string: key1=value1&key2=value2...
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
            String dataToSign = sb.toString();

            // Compute HMAC-SHA256
            String actualSignature = hmacSha256Hex(checksumKey, dataToSign);

            // Compare
            return actualSignature.equalsIgnoreCase(expectedSignature);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Computes HMAC-SHA256 and returns the result as a lowercase hex string.
     */
    public static String hmacSha256Hex(String key, String data) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(key.getBytes("UTF-8"), "HmacSHA256"));
        byte[] rawHmac = mac.doFinal(data.getBytes("UTF-8"));

        // Convert to hex
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

}
