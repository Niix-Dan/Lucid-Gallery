package com.niixlabs.lucidgallery.client.gui.util;

import com.niixlabs.lucidgallery.config.LucidConfig;
import net.minecraft.client.Minecraft;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;

public final class CatboxUploader {
    private static final String UPLOAD_URL = "https://litterbox.catbox.moe/resources/internals/api.php";
    private static final String USER_AGENT = "niix-dan/Lucid-Gallery";
    private static final int MAX_REQUEST_RETRIES = 2;
    private static final long RETRY_DELAY_MS = 500;
    private static final int REQUEST_TIMEOUT_MS = 10000;

    private static final String FILE_LIFETIME = "24h";
    private static final String FILE_NAME_LENGTH = "6";

    private static final HttpClient CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofMillis(REQUEST_TIMEOUT_MS))
            .build();

    private static final ConcurrentHashMap<String, String> UPLOAD_CACHE = new ConcurrentHashMap<>();

    private static volatile long lastUploadFinishedAt = 0L;
    private static volatile boolean uploadInProgress = false;

    public enum RejectReason { NONE, COOLDOWN, BUSY }

    public static String getCachedUrl(File file) {
        return UPLOAD_CACHE.get(file.getAbsolutePath());
    }

    public static RejectReason canUpload() {
        if (uploadInProgress) {
            return RejectReason.BUSY;
        }
        long cooldownMillis = Math.max(0, LucidConfig.screenshotUploadCooldownSeconds) * 1000L;
        if (System.currentTimeMillis() - lastUploadFinishedAt < cooldownMillis) {
            return RejectReason.COOLDOWN;
        }
        return RejectReason.NONE;
    }

    public static void uploadAsync(File file, BiConsumer<String, String> onResult) {
        String cached = UPLOAD_CACHE.get(file.getAbsolutePath());
        if (cached != null) {
            onResult.accept(cached, null);
            return;
        }

        RejectReason reason = canUpload();
        if (reason != RejectReason.NONE) {
            onResult.accept(null, reason.name());
            return;
        }

        uploadInProgress = true;

        CompletableFuture
                .supplyAsync(() -> doUpload(file))
                .whenComplete((url, throwable) -> {
                    uploadInProgress = false;
                    lastUploadFinishedAt = System.currentTimeMillis();

                    Minecraft.getInstance().execute(() -> {
                        if (throwable != null) {
                            onResult.accept(null, throwable.getCause() != null
                                    ? throwable.getCause().getMessage()
                                    : throwable.getMessage());
                        } else {
                            UPLOAD_CACHE.put(file.getAbsolutePath(), url);
                            onResult.accept(url, null);
                        }
                    });
                });
    }

    private static String doUpload(File file) {
        try {
            long lastSize = -1;
            for (int i = 0; i < 20; i++) {
                long currentSize = file.length();
                if (currentSize > 0 && currentSize == lastSize) break;
                lastSize = currentSize;
                Thread.sleep(150);
            }

            byte[] imageBytes = Files.readAllBytes(file.toPath());
            String boundary = "LucidBoundary" + UUID.randomUUID().toString().replace("-", "");
            byte[] body = buildMultipartBody(boundary, file.getName(), imageBytes);

            for (int attempt = 0; attempt <= MAX_REQUEST_RETRIES; attempt++) {
                try {
                    HttpRequest request = HttpRequest.newBuilder()
                            .uri(URI.create(UPLOAD_URL))
                            .timeout(Duration.ofMillis(REQUEST_TIMEOUT_MS))
                            .header("User-Agent", USER_AGENT)
                            .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                            .POST(HttpRequest.BodyPublishers.ofByteArray(body))
                            .build();

                    HttpResponse<String> response = CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
                    int status = response.statusCode();
                    String responseBody = response.body() == null ? "" : response.body().trim();

                    if (status == 408 || status == 425 || status == 429 || status >= 500) {
                        if (attempt < MAX_REQUEST_RETRIES) {
                            Thread.sleep((long) (RETRY_DELAY_MS * Math.pow(2, attempt)));
                            continue;
                        }
                    }

                    if (status / 100 != 2 || !responseBody.startsWith("https://litter.catbox.moe/")) {
                        throw new RuntimeException("HTTP " + status + " - " + (responseBody.isEmpty() ? "No Body" : responseBody));
                    }

                    return responseBody;

                } catch (HttpTimeoutException | java.net.ConnectException e) {
                    if (attempt < MAX_REQUEST_RETRIES) {
                        Thread.sleep((long) (RETRY_DELAY_MS * Math.pow(2, attempt)));
                        continue;
                    }
                    throw new RuntimeException("Falha na conexão com Litterbox após " + (MAX_REQUEST_RETRIES + 1) + " tentativas.", e);
                }
            }
            throw new RuntimeException("Upload falhou após atingir limite de tentativas.");
        } catch (Exception e) {
            if (e instanceof InterruptedException) Thread.currentThread().interrupt();
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    private static byte[] buildMultipartBody(String boundary, String fileName, byte[] imageBytes) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        String crlf = "\r\n";
        String twoHyphens = "--";

        out.write((twoHyphens + boundary + crlf).getBytes(StandardCharsets.UTF_8));
        out.write(("Content-Disposition: form-data; name=\"reqtype\"" + crlf + crlf).getBytes(StandardCharsets.UTF_8));
        out.write(("fileupload" + crlf).getBytes(StandardCharsets.UTF_8));

        out.write((twoHyphens + boundary + crlf).getBytes(StandardCharsets.UTF_8));
        out.write(("Content-Disposition: form-data; name=\"time\"" + crlf + crlf).getBytes(StandardCharsets.UTF_8));
        out.write((FILE_LIFETIME + crlf).getBytes(StandardCharsets.UTF_8));

        out.write((twoHyphens + boundary + crlf).getBytes(StandardCharsets.UTF_8));
        out.write(("Content-Disposition: form-data; name=\"fileNameLength\"" + crlf + crlf).getBytes(StandardCharsets.UTF_8));
        out.write((FILE_NAME_LENGTH + crlf).getBytes(StandardCharsets.UTF_8));

        out.write((twoHyphens + boundary + crlf).getBytes(StandardCharsets.UTF_8));
        out.write(("Content-Disposition: form-data; name=\"fileToUpload\"; filename=\"" + fileName + "\"" + crlf).getBytes(StandardCharsets.UTF_8));
        out.write(("Content-Type: image/png" + crlf + crlf).getBytes(StandardCharsets.UTF_8));

        out.write(imageBytes);
        out.write(crlf.getBytes(StandardCharsets.UTF_8));

        out.write((twoHyphens + boundary + twoHyphens + crlf).getBytes(StandardCharsets.UTF_8));

        return out.toByteArray();
    }
}