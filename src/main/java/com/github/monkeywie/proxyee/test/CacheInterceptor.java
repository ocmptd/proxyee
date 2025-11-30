package com.github.monkeywie.proxyee.test;

import com.github.monkeywie.proxyee.intercept.HttpProxyInterceptPipeline;
import com.github.monkeywie.proxyee.intercept.common.FullResponseIntercept;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.handler.codec.http.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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

    private static final Pattern STATIC_PATTERN = Pattern.compile(".*\\.(js|css|woff|font)(\\?.*)?$", Pattern.CASE_INSENSITIVE);
    private static final String CACHE_DIR = "./web_cache/";

    @Override
    public void beforeRequest(Channel clientChannel, HttpRequest httpRequest, HttpProxyInterceptPipeline pipeline) throws Exception {
        String uri = httpRequest.uri();

        // 仅处理JS/CSS资源
        if (STATIC_PATTERN.matcher(uri).matches()) {
            Path cacheFile = getCacheFilePath(uri);
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
            // 仅缓存成功的响应 (HTTP 200)
            if (httpResponse.status().code() == 200) {
                try {
                    saveToCache(request.uri(), httpResponse.content());
                } catch (IOException e) {
                    log.error("Error caching resource: {}", request.uri(), e);
                }
            }
        }
    }

    private Path getCacheFilePath(String uri) {
        String fileName = Integer.toHexString(uri.hashCode()) + ".cache";
        return Paths.get(CACHE_DIR, fileName);
    }

    private void sendCachedResponse(Channel channel, Path cacheFile, String uri) throws IOException {
        byte[] content = Files.readAllBytes(cacheFile);
        String contentType = uri.endsWith(".js") ?
                "application/javascript" : "text/css";

        HttpResponse response = new DefaultHttpResponse(
                HttpVersion.HTTP_1_1,
                HttpResponseStatus.OK
        );
        response.headers()
                .set(HttpHeaderNames.CONTENT_TYPE, contentType)
                .set(HttpHeaderNames.CONTENT_LENGTH, content.length);
        log.info("Sending cached response for: {}", uri);
        channel.write(response);
        channel.writeAndFlush(Unpooled.wrappedBuffer(content));
    }

    private void saveToCache(String uri, io.netty.buffer.ByteBuf content) throws IOException {
        Path cacheFile = getCacheFilePath(uri);
        byte[] bytes = new byte[content.readableBytes()];
        content.getBytes(content.readerIndex(), bytes);
        Files.write(cacheFile, bytes);
        System.out.println("Cached resource: " + uri);
    }
}
