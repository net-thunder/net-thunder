package io.jaspercloud.sdwan.util;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import io.jaspercloud.sdwan.exception.ProcessException;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.util.StreamUtils;

import java.io.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

@Slf4j
public class HttpClient {

    private static OkHttpClient httpClient;

    static {
        httpClient = new OkHttpClient.Builder()
                .connectTimeout(30 * 1000, TimeUnit.MILLISECONDS)
                .callTimeout(30 * 1000, TimeUnit.MILLISECONDS)
                .writeTimeout(30 * 1000, TimeUnit.MILLISECONDS)
                .readTimeout(30 * 1000, TimeUnit.MILLISECONDS)
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

    public static void download(String url, File file, Consumer<Double> consumer, Runnable finish) throws IOException {
        httpClient.newCall(new Request.Builder().get().url(url).build()).enqueue(new Callback() {
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try {
                    if (200 != response.code()) {
                        MessageBox.showError("更新下载请求失败");
                        log.info("http: code={}, url={}", response.code(), url);
                        System.exit(0);
                        return;
                    }
                    try (BufferedOutputStream output = new BufferedOutputStream(new FileOutputStream(file))) {
                        try (BufferedInputStream input = new BufferedInputStream(response.body().byteStream())) {
                            long total = response.body().contentLength();
                            byte[] buf = new byte[StreamUtils.BUFFER_SIZE];
                            int read;
                            long current = 0;
                            while (-1 != (read = input.read(buf, 0, buf.length))) {
                                output.write(buf, 0, read);
                                current += read;
                                double progress = 1.0 * current / total;
                                consumer.accept(progress);
                            }
                        }
                    }
                    finish.run();
                } catch (Exception e) {
                    log.error(e.getMessage(), e);
                    MessageBox.showError("更新下载中断");
                    System.exit(0);
                } finally {
                    response.close();
                }
            }

            @Override
            public void onFailure(Call call, IOException e) {
                log.error(e.getMessage(), e);
                MessageBox.showError("更新下载失败");
                System.exit(0);
            }
        });
    }
}
