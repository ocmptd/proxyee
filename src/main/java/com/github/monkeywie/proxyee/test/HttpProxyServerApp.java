package com.github.monkeywie.proxyee.test;

import com.github.monkeywie.proxyee.intercept.HttpProxyInterceptInitializer;
import com.github.monkeywie.proxyee.intercept.HttpProxyInterceptPipeline;
import com.github.monkeywie.proxyee.intercept.common.CertDownIntercept;
import com.github.monkeywie.proxyee.proxy.ProxyConfig;
import com.github.monkeywie.proxyee.proxy.ProxyType;
import com.github.monkeywie.proxyee.server.HttpProxyServerConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CountDownLatch;
import java.util.regex.Pattern;

/**
 * @Author LiWei
 * @Description
 * @Date 2019/9/23 17:30
 */
public class HttpProxyServerApp extends HttpProxyInterceptInitializer {
    @Override
    public void init(HttpProxyInterceptPipeline pipeline) {
//        pipeline.addFirst(new GoogleIntercept());
        pipeline.addLast(new CertDownIntercept());
        pipeline.addLast(new CacheInterceptor());
        // 配置上游代理 - 请根据你的代理服务器修改以下参数
        ProxyConfig upstreamProxy = new ProxyConfig(ProxyType.HTTP, "127.0.0.1", 10809);
        pipeline.addLast(new GstaticCorsIntercept());
        pipeline.addLast(new SelectiveProxyIntercept(upstreamProxy));
    }

    private static final Logger log = LoggerFactory.getLogger(HttpProxyServerApp.class);

    public static void main(String[] args) throws InterruptedException {
        System.out.println("start proxy server");
        HttpProxyServerConfig config = new HttpProxyServerConfig();
        //enable HTTPS support
        //If not enabled, HTTPS will not be intercepted, but forwarded directly to the raw packet.
        config.setHandleSsl(true);
        config.setConnectTimeout(5000);
        config.setWorkerGroupThreads(32);
        config.setProxyGroupThreads(8);
        new com.github.monkeywie.proxyee.server.HttpProxyServer()
                .serverConfig(config).proxyInterceptInitializer(new HttpProxyServerApp())
                .startAsync("0.0.0.0", 8888);
        log.info("proxy server started");
        new CountDownLatch(1).await();
    }

}
