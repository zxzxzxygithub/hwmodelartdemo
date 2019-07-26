package com.ml.sdk;

import android.graphics.Bitmap;
import android.util.Base64;

import java.io.ByteArrayOutputStream;

/**
 * 对bitmap进行操作的方法
 */
public class BitmapUtil {
    /**
     * bitmap转base64
     *
     * @param bit
     * @return
     */
    public static String bitmapToBase64(Bitmap bit) {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        bit.compress(Bitmap.CompressFormat.JPEG, 85, bos);
        byte[] bytes = bos.toByteArray();
        return Base64.encodeToString(bytes, Base64.DEFAULT);
    }
}
