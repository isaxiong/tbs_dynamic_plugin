# tbs_dynamic_plugin

A Flutter Plugin For X5WebView with dynamic load x5 core which base on Google 'webview_flutter' Plugin

### 使用

#### 主要在项目中使用的有三个Dart文件

- **根目录/lib/tbs_dynamic_sdk.dart**
> **负责处理X5内核动态初始化加载**

- **example/lib/x5/x5_webview_widget.dart**
> **提供两种内嵌的X5WebView控件，方便嵌入到Flutter页面中**

- **example/lib/x5/x5_webview_platform_method_channel.dart**
> **负责处理X5WebView加载及回调的相关功能封装**

**具体的使用方式参考：`example\lib\main.dart` 文件**

### 说明

基于Google的 `webview_flutter` 插件暴露的接口，参考 `webview_flutter_x5` 的X5加载方式，进行二次封装实现，替换为X5内核，
与 `webview_flutter_x5` 插件不同的地方在于：

1. 处理X5内核动态下载中断导致X5内核不可用的问题
2. 与 `webview_flutter` 插件解耦，仍保留了其原有的功能拓展，较轻量
3. 与 `tbs_static` 插件（静态加载X5内核）搭配，可根据项目需求自由进行更换

### 参考

- **[官方 `webview_flutter`](https://github.com/flutter/plugins/tree/master/packages/webview_flutter)**
- **[`webview_flutter_x5`](https://github.com/buaashuai/plugins/tree/master/packages/webview_flutter)**
- **[`tbs_static`静态加载X5内核插件](https://github.com/isaxiong/tbs_static)**