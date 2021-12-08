import 'package:flutter/foundation.dart';
import 'package:flutter/gestures.dart';
import 'package:flutter/rendering.dart';
import 'package:flutter/services.dart';
import 'package:flutter/widgets.dart';
import 'package:tbs_dynamic_plugin_example/x5/x5_webview_platform_method_channel.dart';
import 'package:webview_flutter/webview_flutter.dart';

class SurfaceX5WebView extends AndroidWebView {
  @override
  Widget build({
    required BuildContext context,
    required CreationParams creationParams,
    required WebViewPlatformCallbacksHandler webViewPlatformCallbacksHandler,
    required JavascriptChannelRegistry javascriptChannelRegistry,
    WebViewPlatformCreatedCallback? onWebViewPlatformCreated,
    Set<Factory<OneSequenceGestureRecognizer>>? gestureRecognizers,
  }) {
    return PlatformViewLink(
      viewType: 'com.xiong.tbs_dynamic/x5webview',
      surfaceFactory: (
        BuildContext context,
        PlatformViewController controller,
      ) {
        return AndroidViewSurface(
          controller: controller as AndroidViewController,
          gestureRecognizers: gestureRecognizers ??
              const <Factory<OneSequenceGestureRecognizer>>{},
          hitTestBehavior: PlatformViewHitTestBehavior.opaque,
        );
      },
      onCreatePlatformView: (PlatformViewCreationParams params) {
        return PlatformViewsService.initSurfaceAndroidView(
          id: params.id,
          viewType: 'com.xiong.tbs_dynamic/x5webview',
          layoutDirection: TextDirection.rtl,
          creationParams: MethodChannelX5WebViewPlatform.creationParamsToMap(
            creationParams,
            usesHybridComposition: false,
          ),
          creationParamsCodec: const StandardMessageCodec(),
        )
          ..addOnPlatformViewCreatedListener(params.onPlatformViewCreated)
          ..addOnPlatformViewCreatedListener((int id) {
            if (onWebViewPlatformCreated == null) {
              return;
            }
            onWebViewPlatformCreated(
              MethodChannelX5WebViewPlatform(id,
                  webViewPlatformCallbacksHandler, javascriptChannelRegistry),
            );
          })
          ..create();
      },
    );
  }
}


class X5AndroidWebView extends WebViewPlatform {
  @override
  Widget build({
    required BuildContext context,
    required CreationParams creationParams,
    required WebViewPlatformCallbacksHandler webViewPlatformCallbacksHandler,
    required JavascriptChannelRegistry javascriptChannelRegistry,
    WebViewPlatformCreatedCallback? onWebViewPlatformCreated,
    Set<Factory<OneSequenceGestureRecognizer>>? gestureRecognizers,
  }) {
    return AndroidView(
      //在这里对应上native中的AndroidView
      viewType: 'com.xiong.tbs_dynamic/x5webview',
      onPlatformViewCreated: (int id) {
        if (onWebViewPlatformCreated == null) {
          return;
        }
        onWebViewPlatformCreated(
          MethodChannelX5WebViewPlatform(
              id, webViewPlatformCallbacksHandler, javascriptChannelRegistry),
        );
      },
      creationParamsCodec: const StandardMessageCodec(),
      creationParams: MethodChannelX5WebViewPlatform.creationParamsToMap(
        creationParams,
        usesHybridComposition: true,
      ),
      layoutDirection: TextDirection.rtl,
    );
  }
}
