import 'package:flutter/material.dart';
import 'package:fluttertoast/fluttertoast.dart';
import 'package:tbs_dynamic_plugin/tbs_dynamic_sdk.dart';

import 'x5_webview_page.dart';

void main() {
  runApp(const MyApp());
}

class MyApp extends StatefulWidget {
  const MyApp({Key? key}) : super(key: key);

  @override
  State<MyApp> createState() => _MyAppState();
}

const String testUrl = "https://ykt.eduyun.cn/ykt/sjykt/index.html";
class _MyAppState extends State<MyApp> {
  @override
  void initState() {
    super.initState();
  }

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      home: Builder(builder: buildScaffold,)
    );
  }

  Widget buildScaffold(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('Plugin example app'),
      ),
      body: Center(
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: <Widget>[
            GestureDetector(
              onTap: () {
                TbsDynamicSdk.initX5Core(listener: X5LoadListenerHandler());
              },
              child: Container(
                width: 100.0,
                height: 45.0,
                color: Colors.blue[200],
                alignment: Alignment.center,
                child: const Text(
                  'X5初始化',
                ),
              ),
            ),
            GestureDetector(
              onTap: () {
                TbsDynamicSdk.isX5Available().then((value) => Fluttertoast.showToast(msg: "X5加载结果：$value"));
              },
              child: Container(
                width: 100.0,
                height: 45.0,
                margin: EdgeInsets.only(top: 20),
                color: Colors.blue[200],
                alignment: Alignment.center,
                child: const Text(
                  'X5加载结果',
                ),
              ),
            ),
            GestureDetector(
              onTap: () {
                Navigator.of(context).push(MaterialPageRoute(builder: (context) {
                  return X5WebViewPage(url: testUrl,);
                }));
              },
              child: Container(
                width: 100.0,
                height: 45.0,
                margin: EdgeInsets.only(top: 20),
                color: Colors.blue[200],
                alignment: Alignment.center,
                child: const Text(
                  '跳转X5测试页',
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }
}

class X5LoadListenerHandler implements X5CoreDynamicLoadListener {
  @override
  void onX5CoreLoadFailed(int errorCode) {
    print("xiong -- Flutter onX5CoreLoadFailed: errorCode = $errorCode");
  }

  @override
  void onX5CoreLoadSuccess() {
    print("xiong -- Flutter onX5CoreLoadSuccess");
  }
}
