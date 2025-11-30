package com.github.monkeywie.proxyee.test;

import com.github.monkeywie.proxyee.intercept.HttpProxyIntercept;
import com.github.monkeywie.proxyee.intercept.HttpProxyInterceptPipeline;
import com.github.monkeywie.proxyee.proxy.ProxyConfig;
import io.netty.channel.Channel;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 选择性代理拦截器
 * 只对特定的域名使用上游代理，其他域名直接访问
 */
public class SelectiveProxyIntercept extends HttpProxyIntercept {

    // 需要代理的域名列表
    private static final String[] PROXY_DOMAINS = {
            "google.com",
            "googleapis.com",
            "gstatic.com",
            "gvt2.com",
            "gvt1.com",
            "googleusercontent.com",
            "youtube.com",
            "ytimg.com",
            "googlevideo.com",
            "googletagmanager.com",
            "google-analytics.com",
            "gvt3.com",
            "mtalk.google.com",
            "www.gstatic.com"
    };
    private static final Logger log = LoggerFactory.getLogger(SelectiveProxyIntercept.class);

    // 上游代理配置
    private final ProxyConfig upstreamProxy;

    public SelectiveProxyIntercept(ProxyConfig upstreamProxy) {
        this.upstreamProxy = upstreamProxy;
    }

    @Override
    public void beforeRequest(Channel clientChannel, HttpRequest httpRequest,
                              HttpProxyInterceptPipeline pipeline) throws Exception {

        String host = getHostFromRequest(httpRequest);

        // 检查是否需要代理
        if (host != null && shouldUseProxy(httpRequest)) {
            log.info("使用代理访问: {}", host);
            pipeline.setProxyConfig(upstreamProxy);
        } else {
            pipeline.setProxyConfig(null); // 不使用代理
        }

        // 继续处理请求
        pipeline.beforeRequest(clientChannel, httpRequest);
    }

    @Override
    public void beforeRequest(Channel clientChannel, HttpContent httpContent,
                              HttpProxyInterceptPipeline pipeline) throws Exception {
        pipeline.beforeRequest(clientChannel, httpContent);
    }

    @Override
    public void afterResponse(Channel clientChannel, Channel proxyChannel, HttpResponse httpResponse,
                              HttpProxyInterceptPipeline pipeline) throws Exception {
        pipeline.afterResponse(clientChannel, proxyChannel, httpResponse);
    }

    @Override
    public void afterResponse(Channel clientChannel, Channel proxyChannel, HttpContent httpContent,
                              HttpProxyInterceptPipeline pipeline) throws Exception {
        pipeline.afterResponse(clientChannel, proxyChannel, httpContent);
    }

    /**
     * 从请求中获取主机名
     */
    private String getHostFromRequest(HttpRequest request) {
        String host = request.headers().get("Host");
        if (host != null) {
            // 移除端口部分
            int colonIndex = host.indexOf(':');
            if (colonIndex != -1) {
                host = host.substring(0, colonIndex);
            }
        }
        return host;
    }

    /**
     * 判断是否应该使用代理
     */
    private boolean shouldUseProxy(HttpRequest request) {
        String host = getHostFromRequest(request);
        if (host.equals("mlb25.theshow.com")) {
            String uri = request.getUri();
            if (uri.startsWith("/assets") ||  uri.startsWith("/rails")) {
                return true;
            }
        }
        for (String domain : PROXY_DOMAINS) {
            if (host.equals(domain) || host.endsWith(domain)) {
                return true;
            }
        }
        return false;
    }
}