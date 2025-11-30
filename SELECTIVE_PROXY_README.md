# 选择性代理配置指南

## 🎯 核心功能

**选择性代理**：只对特定的域名（如谷歌相关）使用上游代理，其他域名直接访问，避免全部流量都走代理。

## 📋 代理域名列表

以下域名会自动使用上游代理：
- `google.com` - Google搜索
- `googleapis.com` - Google API服务
- `gstatic.com` / `gstatic.cn` - Google静态资源
- `gvt1.com` / `gvt2.com` - Google内容分发
- `googleusercontent.com` - Google用户内容
- `youtube.com` / `ytimg.com` / `googlevideo.com` - YouTube相关
- `recaptcha.net` - reCAPTCHA验证

## 🚀 使用方法

### 1. 基础选择性代理（仅代理）

使用 `SelectiveProxyServer.java`：

```bash
mvn compile exec:java -Dexec.mainClass="com.github.monkeywie.proxyee.SelectiveProxyServer"
```

### 2. 带CORS支持的选择性代理

使用 `SelectiveCorsProxyServer.java`：

```bash
mvn compile exec:java -Dexec.mainClass="com.github.monkeywie.proxyee.SelectiveCorsProxyServer"
```

### 3. 配置上游代理

编辑服务器文件，修改代理配置：

```java
ProxyConfig upstreamProxy = new ProxyConfig(
    ProxyType.SOCKS5,  // 代理类型：HTTP, SOCKS4, SOCKS5
    "127.0.0.1",       // 代理服务器地址
    1080               // 代理服务器端口
);
```

### 4. 浏览器配置

- 地址：`127.0.0.1`
- 端口：`8888`（基础代理）或 `8889`（带CORS）

## ⚡ 性能优势

- **智能分流**：只有谷歌相关域名走代理，其他网站直接访问
- **速度提升**：避免不必要的代理转发，大幅提高访问速度
- **节省流量**：减少代理服务器的流量消耗
- **稳定性**：降低代理服务器的负载压力

## 🔧 代理类型支持

支持以下代理类型：
- **HTTP**：HTTP代理
- **SOCKS4**：SOCKS4代理
- **SOCKS5**：SOCKS5代理（支持用户名密码认证）

## 📊 工作原理

```
浏览器请求 → 本地代理(8888) → 判断域名
                                    ↓
谷歌域名 → 上游代理 → 目标网站
                                    ↓
其他域名 → 直接访问 → 目标网站
```

## 🔍 调试信息

启动后会显示类似信息：
```
使用代理访问: www.google.com
直接访问: www.baidu.com
使用代理访问: www.gstatic.com
直接访问: github.com
```

## 🛠️ 自定义域名

如需添加其他域名到代理列表，修改 `SelectiveProxyIntercept.java`：

```java
private static final String[] PROXY_DOMAINS = {
    "google.com",
    "your-domain.com",  // 添加你的域名
    "*.your-domain.com" // 支持通配符
};
```