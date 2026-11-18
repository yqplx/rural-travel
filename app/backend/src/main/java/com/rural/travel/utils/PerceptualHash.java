package com.rural.travel.utils;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

/** 64 位感知哈希(pHash):对缩放、轻微裁剪、亮度变化鲁棒 */
public class PerceptualHash {

    public static String hash(BufferedImage src) {
        BufferedImage img = new BufferedImage(32, 32, BufferedImage.TYPE_BYTE_GRAY);
        Graphics2D g = img.createGraphics();
        g.drawImage(src, 0, 0, 32, 32, null);
        g.dispose();

        double[][] vals = new double[32][32];
        for (int x = 0; x < 32; x++)
            for (int y = 0; y < 32; y++)
                vals[x][y] = img.getRGB(x, y) & 0xFF;

        // 取左上角 8x8 DCT 系数
        double[] dct = new double[64];
        for (int u = 0; u < 8; u++) {
            for (int v = 0; v < 8; v++) {
                double sum = 0;
                for (int x = 0; x < 32; x++)
                    for (int y = 0; y < 32; y++)
                        sum += vals[x][y]
                                * Math.cos((2 * x + 1) * u * Math.PI / 64.0)
                                * Math.cos((2 * y + 1) * v * Math.PI / 64.0);
                double cu = u == 0 ? 1 / Math.sqrt(2) : 1;
                double cv = v == 0 ? 1 / Math.sqrt(2) : 1;
                dct[u * 8 + v] = 0.25 * cu * cv * sum;
            }
        }
        double[] sorted = dct.clone();
        java.util.Arrays.sort(sorted);
        double median = (sorted[31] + sorted[32]) / 2.0;
        long bits = 0;
        for (int i = 0; i < 64; i++) {
            if (dct[i] > median) bits |= (1L << i);
        }
        return String.format("%016x", bits);
    }

    public static int hamming(String h1, String h2) {
        return Long.bitCount(Long.parseUnsignedLong(h1, 16) ^ Long.parseUnsignedLong(h2, 16));
    }
}
