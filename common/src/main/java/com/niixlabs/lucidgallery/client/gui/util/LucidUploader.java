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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class LucidUploader {
    private static final String UPLOAD_URL = "https://uguu.se/upload.php";
    private static final String USER_AGENT = "niix-dan/Lucid-Gallery";
    private static final int MAX_REQUEST_RETRIES = 2;
    private static final long RETRY_DELAY_MS = 500;
    private static final int REQUEST_TIMEOUT_MS = 10000;

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

                    if (status / 100 != 2) {
                        throw new RuntimeException("HTTP " + status + " - " + (responseBody.isEmpty() ? "No Body" : responseBody));
                    }

                    Matcher matcher = Pattern.compile("\"url\"\\s*:\\s*\"([^\"]+)\"").matcher(responseBody);
                    if (matcher.find()) {
                        return matcher.group(1).replace("\\/", "/");
                    } else {
                        throw new RuntimeException("Failed to parse Uguu.se response.");
                    }

                } catch (HttpTimeoutException | java.net.ConnectException e) {
                    if (attempt < MAX_REQUEST_RETRIES) {
                        Thread.sleep((long) (RETRY_DELAY_MS * Math.pow(2, attempt)));
                        continue;
                    }
                    throw new RuntimeException("Connection to Uguu.se failed after " + (MAX_REQUEST_RETRIES + 1) + " attempts.", e);
                }
            }
            throw new RuntimeException("Upload failed after reaching maximum retries.");
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

        out.write(("Content-Disposition: form-data; name=\"files[]\"; filename=\"" + fileName + "\"" + crlf).getBytes(StandardCharsets.UTF_8));
        out.write(("Content-Type: image/png" + crlf + crlf).getBytes(StandardCharsets.UTF_8));

        out.write(imageBytes);
        out.write(crlf.getBytes(StandardCharsets.UTF_8));

        out.write((twoHyphens + boundary + twoHyphens + crlf).getBytes(StandardCharsets.UTF_8));

        return out.toByteArray();
    }
}