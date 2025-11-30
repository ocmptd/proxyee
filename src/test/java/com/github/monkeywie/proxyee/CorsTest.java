package com.github.monkeywie.proxyee;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.Map;

/**
 * CORS测试类
 * 测试GoogleIntercept是否正确处理CORS请求
 */
public class CorsTest {
    
    public static void main(String[] args) {
        try {
            // 测试 gstatic.cn 的 CORS 处理
            testCors("https://www.gstatic.cn/recaptcha/releases/TkacYOdEJbdB_JjX802TMer9/recaptcha__zh_cn.js");
            
            // 测试 google.com 的重定向
            testCors("https://www.google.com/recaptcha/api.js");
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private static void testCors(String urlString) throws Exception {
        System.out.println("\n=== 测试 URL: " + urlString + " ===");
        
        URL url = new URL(urlString);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        
        // 设置代理
        System.setProperty("http.proxyHost", "127.0.0.1");
        System.setProperty("http.proxyPort", "8888");
        System.setProperty("https.proxyHost", "127.0.0.1");
        System.setProperty("https.proxyPort", "8888");
        
        // 添加 CORS 相关的请求头
        connection.setRequestProperty("Origin", "https://example.com");
        connection.setRequestProperty("Access-Control-Request-Method", "GET");
        connection.setRequestProperty("Access-Control-Request-Headers", "content-type");
        
        connection.setRequestMethod("GET");
        
        // 获取响应码
        int responseCode = connection.getResponseCode();
        System.out.println("响应码: " + responseCode);
        
        // 获取所有响应头
        Map<String, List<String>> headers = connection.getHeaderFields();
        System.out.println("响应头:");
        for (Map.Entry<String, List<String>> entry : headers.entrySet()) {
            String key = entry.getKey();
            if (key != null && (key.toLowerCase().contains("access-control") || key.toLowerCase().contains("location"))) {
                System.out.println("  " + key + ": " + entry.getValue());
            }
        }
        
        // 测试 OPTIONS 预检请求
        System.out.println("\n--- 测试 OPTIONS 预检请求 ---");
        HttpURLConnection optionsConnection = (HttpURLConnection) url.openConnection();
        optionsConnection.setRequestMethod("OPTIONS");
        optionsConnection.setRequestProperty("Origin", "https://example.com");
        optionsConnection.setRequestProperty("Access-Control-Request-Method", "GET");
        
        int optionsResponseCode = optionsConnection.getResponseCode();
        System.out.println("OPTIONS 响应码: " + optionsResponseCode);
        
        Map<String, List<String>> optionsHeaders = optionsConnection.getHeaderFields();
        System.out.println("OPTIONS 响应头:");
        for (Map.Entry<String, List<String>> entry : optionsHeaders.entrySet()) {
            String key = entry.getKey();
            if (key != null && key.toLowerCase().contains("access-control")) {
                System.out.println("  " + key + ": " + entry.getValue());
            }
        }
        
        connection.disconnect();
        optionsConnection.disconnect();
    }
}