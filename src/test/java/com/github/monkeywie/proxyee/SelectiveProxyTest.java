package com.github.monkeywie.proxyee;

import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URL;
import java.util.Arrays;
import java.util.List;

/**
 * 选择性代理测试类
 * 测试只有特定域名走代理，其他域名直接访问
 */
public class SelectiveProxyTest {
    
    // 测试的域名列表
    private static final List<String> TEST_DOMAINS = Arrays.asList(
        "https://www.google.com",
        "https://www.gstatic.com/recaptcha/releases/TkacYOdEJbdB_JjX802TMer9/recaptcha__zh_cn.js",
        "https://www.baidu.com",
        "https://github.com",
        "https://www.bing.com"
    );
    
    public static void main(String[] args) {
        System.out.println("=== 选择性代理测试 ===");
        System.out.println("代理地址: 127.0.0.1:8888");
        System.out.println("期望结果: google/gstatic 走代理，其他直接访问");
        System.out.println();
        
        // 设置代理
        Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress("127.0.0.1", 8888));
        
        for (String urlStr : TEST_DOMAINS) {
            System.out.println("测试: " + urlStr);
            try {
                URL url = new URL(urlStr);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection(proxy);
                
                // 设置连接超时
                connection.setConnectTimeout(5000);
                connection.setReadTimeout(5000);
                connection.setRequestMethod("GET");
                
                // 添加User-Agent
                connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36");
                
                // 获取响应码
                int responseCode = connection.getResponseCode();
                System.out.println("  响应码: " + responseCode);
                
                // 获取响应头
                String contentType = connection.getHeaderField("Content-Type");
                if (contentType != null) {
                    System.out.println("  内容类型: " + contentType);
                }
                
                // 检查是否有CORS头（如果配置了CORS处理）
                String corsHeader = connection.getHeaderField("Access-Control-Allow-Origin");
                if (corsHeader != null) {
                    System.out.println("  CORS头: " + corsHeader);
                }
                
                connection.disconnect();
                
            } catch (Exception e) {
                System.out.println("  错误: " + e.getMessage());
            }
            System.out.println();
        }
        
        System.out.println("=== 测试完成 ===");
        System.out.println("注意：查看代理服务器控制台输出，确认哪些域名走了代理");
    }
}