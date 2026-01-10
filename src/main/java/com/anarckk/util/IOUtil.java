package com.anarckk.util;

import java.io.*;
import java.nio.charset.StandardCharsets;

/**
 * Created by anarckk on 2026/1/10
 **/
public class IOUtil {
    /**
     * 将InputStream按UTF-8编码读取为字符串，并确保流被正确关闭
     * @param inputStream 输入流（方法执行后会被关闭）
     * @return UTF-8编码的字符串内容
     * @throws IOException 读取异常
     */
    public static String inputStreamToString(InputStream inputStream) throws IOException {
        if (inputStream == null) {
            return null;
        }
        StringBuilder result = new StringBuilder();
        try (InputStream is = inputStream;
             InputStreamReader isr = new InputStreamReader(is, StandardCharsets.UTF_8);
             BufferedReader reader = new BufferedReader(isr)) {
            String line;
            while ((line = reader.readLine()) != null) {
                result.append(line).append(System.lineSeparator());
            }
        }
        if (!result.isEmpty()) {
            result.setLength(result.length() - System.lineSeparator().length());
        }
        return result.toString();
    }
}
