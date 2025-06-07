package org.simpmc.simppay.util.qrcode.optimized;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.simpmc.simppay.util.qrcode.fastqrcodegen.QrCode;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Optimized QR Code generator with caching and performance improvements
 */
@UtilityClass
@Slf4j
public class OptimizedQrCodeGenerator {
    
    // Cache for generated QR codes to avoid regeneration
    private static final ConcurrentHashMap<String, byte[]> qrCodeCache = new ConcurrentHashMap<>();
    
    // Default settings
    private static final int DEFAULT_SCALE = 8;
    private static final int DEFAULT_BORDER = 2;
    private static final Color DEFAULT_FOREGROUND = Color.BLACK;
    private static final Color DEFAULT_BACKGROUND = Color.WHITE;
    
    /**
     * Generate QR code as PNG bytes with caching
     */
    public static byte[] generateQrCodePng(String text) {
        return generateQrCodePng(text, DEFAULT_SCALE, DEFAULT_BORDER);
    }
    
    /**
     * Generate QR code as PNG bytes with custom scale and border
     */
    public static byte[] generateQrCodePng(String text, int scale, int border) {
        String cacheKey = generateCacheKey(text, scale, border);
        
        return qrCodeCache.computeIfAbsent(cacheKey, key -> {
            try {
                QrCode qr = QrCode.encodeText(text, QrCode.Ecc.MEDIUM);
                BufferedImage img = toBufferedImage(qr, scale, border);
                
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                ImageIO.write(img, "PNG", baos);
                return baos.toByteArray();
            } catch (Exception e) {
                log.error("Failed to generate QR code for text: {}", text, e);
                return new byte[0];
            }
        });
    }
    
    /**
     * Generate QR code with custom colors
     */
    public static byte[] generateQrCodePng(String text, int scale, int border, 
                                         Color foreground, Color background) {
        String cacheKey = generateCacheKey(text, scale, border, foreground, background);
        
        return qrCodeCache.computeIfAbsent(cacheKey, key -> {
            try {
                QrCode qr = QrCode.encodeText(text, QrCode.Ecc.MEDIUM);
                BufferedImage img = toBufferedImage(qr, scale, border, foreground, background);
                
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                ImageIO.write(img, "PNG", baos);
                return baos.toByteArray();
            } catch (Exception e) {
                log.error("Failed to generate QR code for text: {}", text, e);
                return new byte[0];
            }
        });
    }
    
    /**
     * Generate BufferedImage from QR code
     */
    private static BufferedImage toBufferedImage(QrCode qr, int scale, int border) {
        return toBufferedImage(qr, scale, border, DEFAULT_FOREGROUND, DEFAULT_BACKGROUND);
    }
    
    /**
     * Generate BufferedImage from QR code with custom colors
     */
    private static BufferedImage toBufferedImage(QrCode qr, int scale, int border, 
                                               Color foreground, Color background) {
        if (scale <= 0 || border < 0) {
            throw new IllegalArgumentException("Scale and border must be non-negative");
        }
        
        int size = (qr.size + border * 2) * scale;
        BufferedImage result = new BufferedImage(size, size, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = result.createGraphics();
        
        // Enable anti-aliasing for better quality
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        // Fill background
        g.setColor(background);
        g.fillRect(0, 0, size, size);
        
        // Draw QR code modules
        g.setColor(foreground);
        for (int y = 0; y < qr.size; y++) {
            for (int x = 0; x < qr.size; x++) {
                if (qr.getModule(x, y)) {
                    int startX = (x + border) * scale;
                    int startY = (y + border) * scale;
                    g.fillRect(startX, startY, scale, scale);
                }
            }
        }
        
        g.dispose();
        return result;
    }
    
    /**
     * Generate cache key for QR code
     */
    private static String generateCacheKey(String text, int scale, int border) {
        return String.format("%s_%d_%d", text.hashCode(), scale, border);
    }
    
    /**
     * Generate cache key for QR code with colors
     */
    private static String generateCacheKey(String text, int scale, int border, 
                                         Color foreground, Color background) {
        return String.format("%s_%d_%d_%d_%d", 
                text.hashCode(), scale, border, 
                foreground.getRGB(), background.getRGB());
    }
    
    /**
     * Clear QR code cache
     */
    public static void clearCache() {
        qrCodeCache.clear();
        log.info("QR code cache cleared");
    }
    
    /**
     * Get cache size
     */
    public static int getCacheSize() {
        return qrCodeCache.size();
    }
    
    /**
     * Remove specific QR code from cache
     */
    public static void removeFromCache(String text) {
        qrCodeCache.entrySet().removeIf(entry -> 
                entry.getKey().startsWith(String.valueOf(text.hashCode())));
    }
    
    /**
     * Check if QR code is cached
     */
    public static boolean isCached(String text, int scale, int border) {
        String cacheKey = generateCacheKey(text, scale, border);
        return qrCodeCache.containsKey(cacheKey);
    }
}
