package com.github.monkeywie.proxyee.test;

import com.github.monkeywie.proxyee.intercept.HttpProxyInterceptInitializer;
import com.github.monkeywie.proxyee.intercept.HttpProxyInterceptPipeline;
import com.github.monkeywie.proxyee.intercept.common.CertDownIntercept;
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
        pipeline.addLast(new CertDownIntercept());
        pipeline.addLast(new CacheInterceptor());
    }

    private static final Logger log = LoggerFactory.getLogger(HttpProxyServerApp.class);
    private static final Pattern GOOGLE_PATTERN = Pattern.compile(".*?google.*?", Pattern.CASE_INSENSITIVE);

    public static void main(String[] args) throws InterruptedException {
        System.out.println("start proxy server");
        HttpProxyServerConfig config = new HttpProxyServerConfig();
        //enable HTTPS support
        //If not enabled, HTTPS will not be intercepted, but forwarded directly to the raw packet.
        config.setHandleSsl(true);
        config.setWorkerGroupThreads(64);
        config.setProxyGroupThreads(64);
        new com.github.monkeywie.proxyee.server.HttpProxyServer()
                .serverConfig(config).proxyInterceptInitializer(new HttpProxyServerApp())
                .startAsync("0.0.0.0", 8888);
        log.info("proxy server started");
        new CountDownLatch(1).await();
    }

}
