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

/**
 * 选择性CORS代理服务器
 * 只对谷歌相关域名使用上游代理，同时处理CORS问题
 */
public class SelectiveCorsProxyServer {

    public static void main(String[] args) throws Exception {
        // 配置上游代理 - 请根据你的代理服务器修改以下参数
        ProxyConfig upstreamProxy = new ProxyConfig(
            ProxyType.HTTP,  // 代理类型：HTTP, SOCKS4, SOCKS5
            "127.0.0.1",     // 代理服务器地址
            8578              // 代理服务器端口
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
                // 不设置全局代理配置，让拦截器决定何时使用代理
                .proxyInterceptInitializer(new HttpProxyInterceptInitializer() {
                    @Override
                    public void init(HttpProxyInterceptPipeline pipeline) {
                        pipeline.addLast(new CertDownIntercept());  // 处理证书下载
                        // 添加选择性代理拦截器
                        pipeline.addLast(new SelectiveProxyIntercept(upstreamProxy));
                        // 添加 GoogleIntercept 处理 CORS 问题（不重定向）
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
                .start(8888);
                
        System.out.println("选择性CORS代理服务器已启动！");
        System.out.println("监听端口: 8888");
        System.out.println("上游代理: " + upstreamProxy.getHost() + ":" + upstreamProxy.getPort());
        System.out.println("代理类型: " + upstreamProxy.getProxyType());
        System.out.println("\n功能特点：");
        System.out.println("- 谷歌相关域名通过上游代理访问");
        System.out.println("- 其他域名直接访问");
        System.out.println("- 支持CORS处理");
        System.out.println("- 配置浏览器代理为 127.0.0.1:8888");
    }
}