# 谷歌代理配置指南

本项目支持通过指定代理服务器访问谷歌相关服务。以下是配置方法：

## 基本原理

项目通过以下方式实现谷歌代理访问：

1. **上游代理配置**：使用 `ProxyConfig` 配置上游代理服务器
2. **拦截器处理**：使用 `GoogleIntercept` 处理 CORS 相关问题
3. **保持原始域名**：不再重定向 Google 域名，保持原始请求不变

## 配置方法

### 1. 基本代理配置（仅代理，无CORS处理）

使用 `GoogleProxyServer.java`：

```java
ProxyConfig upstreamProxy = new ProxyConfig(
    ProxyType.HTTP,  // 代理类型：HTTP, SOCKS4, SOCKS5
    "127.0.0.1",     // 代理服务器地址
    1080              // 代理服务器端口
);

new HttpProxyServer()
    .serverConfig(config)
    .proxyConfig(upstreamProxy)  // 设置上游代理
    .start(8888);
```

### 2. 带CORS支持的代理配置

使用 `GoogleCorsProxyServer.java` 或 `GoogleInterceptTestServer.java`：

```java
ProxyConfig upstreamProxy = new ProxyConfig(
    ProxyType.SOCKS5,  // 代理类型
    "127.0.0.1",       // 代理服务器地址
    1080,              // 代理服务器端口
    "username",        // 用户名（可选）
    "password"         // 密码（可选）
);

new HttpProxyServer()
    .serverConfig(config)
    .proxyConfig(upstreamProxy)  // 设置上游代理
    .proxyInterceptInitializer(new HttpProxyInterceptInitializer() {
        @Override
        public void init(HttpProxyInterceptPipeline pipeline) {
            pipeline.addLast(new CertDownIntercept());
            pipeline.addLast(new GoogleIntercept());  // 处理CORS问题
        }
    })
    .start(8889);
```

## 代理类型支持

支持以下代理类型：

- **HTTP**：HTTP代理
- **SOCKS4**：SOCKS4代理
- **SOCKS5**：SOCKS5代理（支持用户名密码认证）

## 使用步骤

1. **修改代理配置**：编辑相应的服务器文件，修改代理服务器地址、端口和类型

2. **启动代理服务器**：
   ```bash
   mvn compile exec:java -Dexec.mainClass="com.github.monkeywie.proxyee.GoogleProxyServer"
   # 或
   mvn compile exec:java -Dexec.mainClass="com.github.monkeywie.proxyee.GoogleCorsProxyServer"
   ```

3. **配置浏览器代理**：
   - 地址：127.0.0.1
   - 端口：8888（或 8889）

4. **测试访问**：访问 Google 相关服务，所有请求将通过指定的上游代理转发

## 注意事项

1. **证书安装**：首次使用需要安装代理证书，访问 `http://server.ip:port` 下载证书
2. **代理链**：本项目作为本地代理，会将所有请求转发到配置的上游代理
3. **CORS支持**：如果遇到跨域问题，使用带 GoogleIntercept 的配置
4. **性能优化**：根据网络环境选择合适的代理类型

## 故障排除

- **连接失败**：检查上游代理服务器是否正常运行
- **认证失败**：确认代理用户名和密码是否正确
- **证书问题**：重新下载并安装代理证书
- **速度慢**：尝试更换代理服务器或代理类型

## 示例配置

### Clash 配置示例
```java
ProxyConfig upstreamProxy = new ProxyConfig(
    ProxyType.SOCKS5,
    "127.0.0.1",
    7890  // Clash 默认 SOCKS5 端口
);
```

### V2Ray 配置示例
```java
ProxyConfig upstreamProxy = new ProxyConfig(
    ProxyType.SOCKS5,
    "127.0.0.1",
    10808  // V2Ray 默认 SOCKS5 端口
);
```

### 系统代理配置示例
```java
ProxyConfig upstreamProxy = new ProxyConfig(
    ProxyType.HTTP,
    "127.0.0.1",
    8080  // 系统 HTTP 代理端口
);
```