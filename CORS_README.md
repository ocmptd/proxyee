# GoogleIntercept CORS 支持

## 概述

GoogleIntercept 现在支持 CORS（跨域资源共享），可以解决在代理环境下访问 Google 服务时遇到的 CORS 错误。

## 主要功能

1. **CORS 头支持**：自动在响应中添加必要的 CORS 头
2. **OPTIONS 预检请求处理**：正确处理浏览器的 OPTIONS 预检请求
3. **多域名支持**：支持 google.com、gvt2.com 和 gstatic.cn
4. **重定向功能**：将 Google reCAPTCHA 请求重定向到 recaptcha.net

## 使用说明

### 1. 启动代理服务器

运行测试服务器：
```bash
java -cp target/classes:target/test-classes com.github.monkeywie.proxyee.GoogleInterceptTestServer
```

### 2. 配置浏览器代理

将浏览器代理设置为：
- HTTP 代理：127.0.0.1:8888
- HTTPS 代理：127.0.0.1:8888

### 3. 测试 CORS 功能

#### 方法 1：使用 HTML 测试页面
1. 在浏览器中打开 `cors-test.html`
2. 点击各个测试按钮
3. 查看测试结果

#### 方法 2：使用 Java 测试程序
```bash
java -cp target/classes:target/test-classes com.github.monkeywie.proxyee.CorsTest
```

### 4. 验证功能

测试的 URL：
- `https://www.gstatic.cn/recaptcha/releases/TkacYOdEJbdB_JjX802TMer9/recaptcha__zh_cn.js`
- `https://www.google.com/recaptcha/api.js`

## CORS 头信息

代理会自动添加以下 CORS 头：

```
Access-Control-Allow-Origin: * (或具体的 Origin)
Access-Control-Allow-Methods: GET, POST, OPTIONS
Access-Control-Allow-Headers: Content-Type, Authorization, X-Requested-With
Access-Control-Allow-Credentials: true
Access-Control-Max-Age: 86400
```

## 注意事项

1. 确保代理服务器正在运行
2. 浏览器需要正确配置代理设置
3. 对于 HTTPS 网站，需要安装代理证书
4. 某些浏览器可能有额外的安全限制

## 故障排除

如果仍然遇到 CORS 错误：

1. 检查代理服务器是否正常运行
2. 确认浏览器代理设置正确
3. 查看控制台日志了解详细错误信息
4. 尝试清除浏览器缓存和 Cookie
5. 检查是否有其他安全软件干扰