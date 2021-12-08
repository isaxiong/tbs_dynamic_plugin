import 'dart:io';

import 'package:flutter/cupertino.dart';
import 'package:flutter/material.dart';
import 'package:webview_flutter/webview_flutter.dart';

import 'x5/x5_webview_widget.dart';


class X5WebViewPage extends StatefulWidget {

  final String x5DebugUrl = "http://debugtbs.qq.com";
  final String? url;
  X5WebViewPage({this.url});

  @override
  _X5WebViewState createState() => _X5WebViewState();
}

class _X5WebViewState extends State<X5WebViewPage> {
  late WebViewController _controller;

  @override
  void initState() {
    super.initState();
    if (Platform.isAndroid) {
      // WebView.platform = X5AndroidWebView();
      WebView.platform = SurfaceX5WebView();
    }
  }

  @override
  void dispose() {
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return WillPopScope(
          onWillPop: _onWillPop,
          child: Scaffold(
            appBar: AppBar(
              title: Text('X5WebView',),
              elevation: 0,
              //去除底部阴影
              leading: Navigator.of(context).canPop()
                  ? InkWell(
                onTap: () {
                  Navigator.of(context).maybePop();
                },
                child: SizedBox(
                  width: 60,
                  height: 44,
                  child: Icon(Icons.arrow_back),
                ),
              )
                  : null,
              //判断是否需要返回按钮
              centerTitle: true,
            ),
            body: Center(
              child: Stack(
                children: <Widget>[
                  WebView(
                    initialUrl: widget.url ?? widget.x5DebugUrl,
                    javascriptMode: JavascriptMode.unrestricted,
                    onWebViewCreated: (control) {
                      _controller = control;
                    },
                    onPageFinished: (url) {
                      print("webview  $url");
                    },
                    onProgress: (progress) {
                      print("webview加载进度------$progress%");
                    },
                  ),
                ],
              ),
            ),
          ),
    );
  }

  Future<bool> _onWillPop() async {
    bool canGoBack = await _controller.canGoBack();
    String url = await _controller.currentUrl() ?? "";
    print("当前的Url = $url");
    if (canGoBack && url != widget.url) {
        _controller.goBack();
      } else {
        Navigator.of(context).pop();
      }
    return Future.value(false);
  }
}
