package com.github.monkeywie.proxyee;

import com.github.monkeywie.proxyee.exception.HttpProxyExceptionHandle;
import com.github.monkeywie.proxyee.intercept.HttpProxyInterceptInitializer;
import com.github.monkeywie.proxyee.intercept.HttpProxyInterceptPipeline;
import com.github.monkeywie.proxyee.intercept.common.CertDownIntercept;
import com.github.monkeywie.proxyee.proxy.ProxyConfig;
import com.github.monkeywie.proxyee.proxy.ProxyType;
import com.github.monkeywie.proxyee.server.HttpProxyServer;
import com.github.monkeywie.proxyee.server.HttpProxyServerConfig;
import com.github.monkeywie.proxyee.test.GoogleIntercept;
import io.netty.channel.Channel;

public class GoogleCorsProxyServer {

    public static void main(String[] args) throws Exception {
        // 配置上游代理 - 请根据你的代理服务器修改以下参数
        ProxyConfig upstreamProxy = new ProxyConfig(
            ProxyType.HTTP,  // 代理类型：HTTP, SOCKS4, SOCKS5
            "127.0.0.1",     // 代理服务器地址
            1080              // 代理服务器端口
        );
        
        // 如果代理需要认证，使用以下配置
        // ProxyConfig upstreamProxy = new ProxyConfig(
        //     ProxyType.SOCKS5,  // 代理类型
        //     "127.0.0.1",       // 代理服务器地址
        //     1080,              // 代理服务器端口
        //     "username",        // 用户名
        //     "password"         // 密码
        // );

        HttpProxyServerConfig config = new HttpProxyServerConfig();
        config.setHandleSsl(true);
        
        new HttpProxyServer()
                .serverConfig(config)
                .proxyConfig(upstreamProxy)  // 设置上游代理
                .proxyInterceptInitializer(new HttpProxyInterceptInitializer() {
                    @Override
                    public void init(HttpProxyInterceptPipeline pipeline) {
                        pipeline.addLast(new CertDownIntercept());  // 处理证书下载
                        // 添加 GoogleIntercept 处理 CORS 问题，但不重定向
                        pipeline.addLast(new GoogleIntercept());
                    }
                })
                .httpProxyExceptionHandle(new HttpProxyExceptionHandle() {
                    @Override
                    public void beforeCatch(Channel clientChannel, Throwable cause) throws Exception {
                        System.err.println("代理异常: " + cause.getMessage());
                        cause.printStackTrace();
                    }

                    @Override
                    public void afterCatch(Channel clientChannel, Channel proxyChannel, Throwable cause)
                            throws Exception {
                        System.err.println("代理异常: " + cause.getMessage());
                        cause.printStackTrace();
                    }
                })
                .start(8889);
                
        System.out.println("Google CORS代理服务器已启动！");
        System.out.println("监听端口: 8889");
        System.out.println("上游代理: " + upstreamProxy.getHost() + ":" + upstreamProxy.getPort());
        System.out.println("代理类型: " + upstreamProxy.getProxyType());
        System.out.println("\n配置浏览器代理为 127.0.0.1:8889 即可通过指定代理访问谷歌服务，同时支持CORS");
    }
}