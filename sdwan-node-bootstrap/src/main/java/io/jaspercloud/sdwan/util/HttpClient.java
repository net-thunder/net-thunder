package io.jaspercloud.sdwan.util;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import io.jaspercloud.sdwan.exception.ProcessException;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.springframework.util.StreamUtils;

import java.io.*;
import java.net.SocketException;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

@Slf4j
public class HttpClient {

    private static OkHttpClient httpClient;

    static {
        httpClient = new OkHttpClient.Builder()
                .connectTimeout(30 * 1000, TimeUnit.MILLISECONDS)
                .callTimeout(30 * 1000, TimeUnit.MILLISECONDS)
                .writeTimeout(120 * 1000, TimeUnit.MILLISECONDS)
                .readTimeout(120 * 1000, TimeUnit.MILLISECONDS)
                .build();
    }

    public static JSONObject get(String url) throws IOException {
        try (Response response = httpClient.newCall(new Request.Builder().get().url(url).build()).execute()) {
            if (200 != response.code()) {
                throw new ProcessException(String.format("http: code=%s, url=%s", response.code(), url));
            }
            byte[] bytes = response.body().bytes();
            String json = new String(bytes);
            JSONObject jsonObject = JSONUtil.parseObj(json);
            return jsonObject;
        }
    }

    public static void download(String url, File file, Consumer<Double> consumer, Runnable finish) throws Exception {
        boolean hasError;
        do {
            try {
                downloadRange(url, file, consumer, finish);
                hasError = false;
            } catch (InterruptedIOException | SocketException e) {
                log.error(String.format("download timeout: %s", e.getMessage()), e);
                hasError = true;
            }
        } while (hasError);
    }

    private static void downloadRange(String url, File file, Consumer<Double> consumer, Runnable finish) throws Exception {
        long current = getFileLength(file);
        Request request = new Request.Builder().get()
                .url(url)
                .header("Range", String.format("bytes=%s-", current))
                .build();
        try (Response response = httpClient.newCall(request).execute()) {
            if (!(200 == response.code() || 206 == response.code())) {
                throw new ProcessException("更新下载请求失败");
            }
            long total = Long.parseLong(response.headers().get("Content-Length"));
            try (RandomAccessFile raf = new RandomAccessFile(file, "rw")) {
                raf.seek(current);
                try (BufferedInputStream input = new BufferedInputStream(response.body().byteStream())) {
                    byte[] buf = new byte[StreamUtils.BUFFER_SIZE];
                    int read;
                    while (-1 != (read = input.read(buf, 0, buf.length))) {
                        raf.write(buf, 0, read);
                        current += read;
                        double progress = 1.0 * current / total;
                        consumer.accept(progress);
                    }
                }
            }
            finish.run();
        }
    }

    private static long getFileLength(File file) throws Exception {
        if (!file.exists()) {
            return 0;
        }
        try (RandomAccessFile raf = new RandomAccessFile(file, "rw")) {
            return raf.length();
        }
    }
}
