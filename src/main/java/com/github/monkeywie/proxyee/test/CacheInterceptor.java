package com.github.monkeywie.proxyee.test;

import com.github.monkeywie.proxyee.intercept.HttpProxyInterceptPipeline;
import com.github.monkeywie.proxyee.intercept.common.FullResponseIntercept;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.handler.codec.http.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

/**
 *
 * create on 09:49
 *
 * @author github kloping
 * @since 2025/11/30
 */
public class CacheInterceptor extends FullResponseIntercept {
    private static final Logger log = LoggerFactory.getLogger(CacheInterceptor.class);

    private static final Pattern STATIC_PATTERN = Pattern.compile(".*\\.(js|css|woff|woff2|ttf|eot|otf|png|jpg|jpeg|gif|svg|ico|webp|json|xml|txt)(\\?.*)?$", Pattern.CASE_INSENSITIVE);
    private static final int DEFAULT_MAX_CONTENT_LENGTH = 1024 * 1024 * 12; // 10MB
    private static final String DEFAULT_CACHE_DIR = "./web_cache/";
    private static final long DEFAULT_CACHE_EXPIRY_HOURS = 7 * 24; // 缓存过期时间(小时)
    private static final long CLEANUP_INTERVAL_HOURS = 6; // 清理任务间隔(小时)

    private final String cacheDir;
    private final long cacheExpiryHours;
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    public CacheInterceptor() {
        this(DEFAULT_MAX_CONTENT_LENGTH, DEFAULT_CACHE_DIR, DEFAULT_CACHE_EXPIRY_HOURS);
    }

    public CacheInterceptor(String cacheDir) {
        this(DEFAULT_MAX_CONTENT_LENGTH, cacheDir, DEFAULT_CACHE_EXPIRY_HOURS);
    }

    public CacheInterceptor(int maxContentLength, String cacheDir) {
        this(maxContentLength, cacheDir, DEFAULT_CACHE_EXPIRY_HOURS);
    }

    public CacheInterceptor(int maxContentLength, String cacheDir, long cacheExpiryHours) {
        super(maxContentLength);
        this.cacheDir = cacheDir;
        this.cacheExpiryHours = cacheExpiryHours;

        // 启动定时清理任务
        startCleanupTask();
    }

    private void startCleanupTask() {
        scheduler.scheduleWithFixedDelay(this::cleanupExpiredCache,
                CLEANUP_INTERVAL_HOURS, CLEANUP_INTERVAL_HOURS, TimeUnit.HOURS);
    }

    private void cleanupExpiredCache() {
        try {
            Path cachePath = Paths.get(cacheDir);
            if (!Files.exists(cachePath)) {
                return;
            }

            Instant expiryThreshold = Instant.now().minus(cacheExpiryHours, ChronoUnit.HOURS);

            Files.walkFileTree(cachePath, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    if (attrs.lastModifiedTime().toInstant().isBefore(expiryThreshold)) {
                        Files.delete(file);
                        log.debug("Deleted expired cache file: {}", file);
                    }
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                    // 删除空目录
                    try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir)) {
                        if (!stream.iterator().hasNext()) {
                            Files.delete(dir);
                            log.debug("Deleted empty cache directory: {}", dir);
                        }
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            log.error("Error during cache cleanup", e);
        }
    }

    // 在应用关闭时调用此方法以关闭调度器
    public void shutdown() {
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public void beforeRequest(Channel clientChannel, HttpRequest httpRequest, HttpProxyInterceptPipeline pipeline) throws Exception {
        String uri = httpRequest.uri();

        // 处理静态资源请求
        if (STATIC_PATTERN.matcher(uri).matches()) {
            Path cacheFile = getCacheFilePath(httpRequest.headers().get("Host"),uri);
            // 如果缓存存在，直接返回本地文件
            if (Files.exists(cacheFile)) {
                System.out.println("Serving from cache: " + uri);
                sendCachedResponse(clientChannel, cacheFile, uri);
                return;
            }
        }

        // 非缓存资源或缓存不存在，继续正常请求
        pipeline.beforeRequest(clientChannel, httpRequest);
    }

    @Override
    public boolean match(HttpRequest httpRequest, HttpResponse httpResponse, HttpProxyInterceptPipeline pipeline) {
        return true;
    }

    @Override
    public void handleResponse(HttpRequest httpRequest, FullHttpResponse httpResponse, HttpProxyInterceptPipeline pipeline) {
        HttpRequest request = pipeline.getHttpRequest();
        if (request != null && STATIC_PATTERN.matcher(request.uri()).matches()) {
            int statusCode = httpResponse.status().code();
            // 缓存成功的响应 (HTTP 200) 和未修改的响应 (HTTP 304)
            if (statusCode == 200 || statusCode == 304) {
                // 对于 304 响应，我们不需要缓存内容，但可能需要更新缓存头
                if (statusCode == 200) {
                    try {
                        saveToCache(request, httpResponse.content());
                    } catch (IOException e) {
                        log.error("Error caching resource: {}", request.uri(), e);
                    }
                }
            } else {
                log.debug("Not caching resource {} with status code: {}", request.uri(), statusCode);
            }
        }
    }

    private Path getCacheFilePath(String host,String uri) {
        // 使用URI的MD5哈希值作为文件名，减少哈希冲突的可能性
//        String fileName = String.format("%032x", uri.hashCode()) + "_" +
//                Integer.toUnsignedString(uri.hashCode(), 36) + ".cache";
//        return Paths.get(cacheDir, fileName);
        String fileName = host + "_" + uri.replaceAll("/", "_") + ".cache";
        return Paths.get(cacheDir, fileName);
    }

    private void sendCachedResponse(Channel channel, Path cacheFile, String uri) throws IOException {
        byte[] content = Files.readAllBytes(cacheFile);
        String contentType = getContentType(uri);

        HttpResponse response = new DefaultHttpResponse(
                HttpVersion.HTTP_1_1,
                HttpResponseStatus.OK
        );
        response.headers()
                .set(HttpHeaderNames.CONTENT_TYPE, contentType)
                .set(HttpHeaderNames.CONTENT_LENGTH, content.length);

        // 添加基本的缓存控制头
        response.headers().set(HttpHeaderNames.CACHE_CONTROL, "public, max-age=3600");

        log.info("Sending cached response for: {}", uri);
        channel.write(response);
        channel.writeAndFlush(Unpooled.wrappedBuffer(content));
    }

    private void saveToCache(HttpRequest request, io.netty.buffer.ByteBuf content) throws IOException {
        String host = request.headers().get("Host");
        Path cacheFile = getCacheFilePath(host,request.uri());
        // 确保缓存目录存在
        Files.createDirectories(cacheFile.getParent());
        byte[] bytes = new byte[content.readableBytes()];
        content.getBytes(content.readerIndex(), bytes);
        Files.write(cacheFile, bytes);
        System.out.println("Cached resource: " + request.uri());
    }

    private String getContentType(String uri) {
        if (uri.endsWith(".js")) {
            return "application/javascript";
        } else if (uri.endsWith(".css")) {
            return "text/css";
        } else if (uri.endsWith(".woff")) {
            return "font/woff";
        } else if (uri.endsWith(".woff2")) {
            return "font/woff2";
        } else if (uri.endsWith(".ttf")) {
            return "font/ttf";
        } else if (uri.endsWith(".eot")) {
            return "application/vnd.ms-fontobject";
        } else if (uri.endsWith(".otf")) {
            return "font/otf";
        } else if (uri.endsWith(".png")) {
            return "image/png";
        } else if (uri.endsWith(".jpg") || uri.endsWith(".jpeg")) {
            return "image/jpeg";
        } else if (uri.endsWith(".gif")) {
            return "image/gif";
        } else if (uri.endsWith(".svg")) {
            return "image/svg+xml";
        } else if (uri.endsWith(".ico")) {
            return "image/x-icon";
        } else if (uri.endsWith(".webp")) {
            return "image/webp";
        } else if (uri.endsWith(".json")) {
            return "application/json";
        } else if (uri.endsWith(".xml")) {
            return "application/xml";
        } else if (uri.endsWith(".txt")) {
            return "text/plain";
        } else {
            // 默认返回二进制流
            return "application/octet-stream";
        }
    }
}