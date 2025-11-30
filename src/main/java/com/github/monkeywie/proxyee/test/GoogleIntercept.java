package com.github.monkeywie.proxyee.test;

import com.github.monkeywie.proxyee.intercept.HttpProxyIntercept;
import com.github.monkeywie.proxyee.intercept.HttpProxyInterceptPipeline;
import io.netty.channel.Channel;
import io.netty.handler.codec.http.*;

/**
 * Google服务重定向拦截器
 * 将Google的reCAPTCHA请求重定向到recaptcha.net
 * 将Google Fonts请求重定向到fonts.font.im
 * 添加CORS支持以避免跨域错误
 */
public class GoogleIntercept extends HttpProxyIntercept {

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
        response.headers().set("Access-Control-Allow-Headers", "Content-Type, Authorization, X-Requested-With");
        response.headers().set("Access-Control-Allow-Credentials", "true");
        response.headers().set("Access-Control-Max-Age", "86400");
    }

    @Override
    public void beforeRequest(Channel clientChannel, HttpRequest httpRequest, HttpProxyInterceptPipeline pipeline) throws Exception {
        String host = httpRequest.headers().get("Host");
        if (host == null) {
            super.beforeRequest(clientChannel, httpRequest, pipeline);
            return;
        }

        // 处理OPTIONS预检请求
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

        // 处理Google Fonts重定向
        if (host.contains("fonts.googleapis.com")) {
//            httpRequest.headers().set("Host", "fonts.font.im");
//            httpRequest.headers().set("Referer", "https://fonts.font.im/");
//            httpRequest.headers().set("Origin", "https://fonts.font.im");
            super.beforeRequest(clientChannel, httpRequest, pipeline);
            return;
        }

        // 处理Google reCAPTCHA重定向 - 包括 google.com、gvt2.com、gstatic.cn 和 gstatic.com
        if (host.contains("google.com") || host.contains("gvt2.com") || host.contains("gstatic.cn") || host.contains("gstatic.com")) {
            String uri = httpRequest.uri();
            if (uri.startsWith("/recaptcha")) {
                // 构建重定向响应
                HttpResponse redirectResponse = new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.FOUND);
                redirectResponse.headers().set(HttpHeaderNames.LOCATION, "https://recaptcha.net" + uri);
                redirectResponse.headers().set(HttpHeaderNames.CONTENT_LENGTH, 0);
                
                // 添加CORS头
                setCorsHeaders(redirectResponse, httpRequest);
                
                // 发送重定向响应
                clientChannel.writeAndFlush(redirectResponse);
                clientChannel.writeAndFlush(LastHttpContent.EMPTY_LAST_CONTENT);
                
                // 不继续处理请求，直接返回
                return;
            }
        }

        super.beforeRequest(clientChannel, httpRequest, pipeline);
    }

    @Override
    public void beforeRequest(Channel clientChannel, HttpContent httpContent, HttpProxyInterceptPipeline pipeline) throws Exception {
        // 如果已经发送了重定向响应，就不再处理内容
        HttpRequest httpRequest = pipeline.getHttpRequest();
        if (httpRequest != null) {
            String host = httpRequest.headers().get("Host");
            String uri = httpRequest.uri();
            if (host != null && (host.contains("google.com") || host.contains("gvt2.com") || host.contains("gstatic.cn") || host.contains("gstatic.com")) && uri.startsWith("/recaptcha")) {
                // 已经处理了重定向，不再处理内容
                return;
            }
        }
        super.beforeRequest(clientChannel, httpContent, pipeline);
    }
}