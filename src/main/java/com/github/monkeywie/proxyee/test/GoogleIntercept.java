package com.github.monkeywie.proxyee.test;

import com.github.monkeywie.proxyee.intercept.HttpProxyInterceptPipeline;
import com.github.monkeywie.proxyee.intercept.common.FullRequestIntercept;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpRequest;

/**
 *
 * create on 10:51
 *
 * @author github kloping
 * @since 2025/11/30
 */
public class GoogleIntercept extends FullRequestIntercept {

    @Override
    public boolean match(HttpRequest httpRequest, HttpProxyInterceptPipeline pipeline) {
        String host = httpRequest.headers().get("Host");
        if (host == null) return false;
        if (host.contains("google.com")) return true;
        if (host.contains("gvt2.com")) return true;
        return false;
    }

    @Override
    public void handleRequest(FullHttpRequest httpRequest, HttpProxyInterceptPipeline pipeline) {
        super.handleRequest(httpRequest, pipeline);
        //将 fonts.googleapis.com 重定向到 googlefonts.cn
        String host = httpRequest.headers().get("Host");
        if (host.contains("fonts.googleapis.com")) {
            httpRequest.headers().set("Host", "googlefonts.cn");
        }
    }
}

