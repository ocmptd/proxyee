package com.github.monkeywie.proxyee.test;

import com.github.monkeywie.proxyee.intercept.HttpProxyIntercept;
import com.github.monkeywie.proxyee.intercept.HttpProxyInterceptPipeline;
import io.netty.channel.Channel;
import io.netty.handler.codec.http.*;

/**
 * Gstatic CORS 拦截器
 * 专门处理 gstatic.com 域名的 CORS 问题，不进行重定向
 */
public class GstaticCorsIntercept extends HttpProxyIntercept {

    /**
     * 设置CORS响应头
     */
    private void setCorsHeaders(HttpResponse response, HttpRequest request) {
        String origin = request.headers().get("Origin");
        if (origin != null) {
            response.headers().set("Access-Control-Allow-Origin", origin);
        } else {
            response.headers().set("Access-Control-Allow-Origin", "*");
        }
        response.headers().set("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
        response.headers().set("Access-Control-Allow-Headers", "Content-Type, Authorization, X-Requested-With, X-Requested-By");
        response.headers().set("Access-Control-Allow-Credentials", "true");
        response.headers().set("Access-Control-Max-Age", "86400");
        
        // 添加额外的安全头
        response.headers().set("Timing-Allow-Origin", "*");
        response.headers().set("Cross-Origin-Resource-Policy", "cross-origin");
        response.headers().set("Cross-Origin-Embedder-Policy", "unsafe-none");
    }

    @Override
    public void beforeRequest(Channel clientChannel, HttpRequest httpRequest, HttpProxyInterceptPipeline pipeline) throws Exception {
        String host = httpRequest.headers().get("Host");
        if (host == null) {
            super.beforeRequest(clientChannel, httpRequest, pipeline);
            return;
        }

        // 只处理 gstatic.com 相关的请求
        if (host.contains("gstatic.com")) {
            // 处理 OPTIONS 预检请求
            if (HttpMethod.OPTIONS.equals(httpRequest.method())) {
                HttpResponse optionsResponse = new DefaultHttpResponse(
                        HttpVersion.HTTP_1_1,
                        HttpResponseStatus.OK
                );
                setCorsHeaders(optionsResponse, httpRequest);
                optionsResponse.headers().set(HttpHeaderNames.CONTENT_LENGTH, 0);
                
                clientChannel.writeAndFlush(optionsResponse);
                clientChannel.writeAndFlush(LastHttpContent.EMPTY_LAST_CONTENT);
                return;
            }
        }

        super.beforeRequest(clientChannel, httpRequest, pipeline);
    }

    @Override
    public void afterResponse(Channel clientChannel, Channel proxyChannel, HttpResponse httpResponse,
                              HttpProxyInterceptPipeline pipeline) throws Exception {
        
        HttpRequest httpRequest = pipeline.getHttpRequest();
        if (httpRequest != null) {
            String host = httpRequest.headers().get("Host");
            if (host != null && host.contains("gstatic.com")) {
                // 为 gstatic.com 的响应添加 CORS 头
                setCorsHeaders(httpResponse, httpRequest);
            }
        }
        
        super.afterResponse(clientChannel, proxyChannel, httpResponse, pipeline);
    }
}