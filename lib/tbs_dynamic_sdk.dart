
import 'package:flutter/foundation.dart';
import 'package:flutter/services.dart';

class TbsDynamicSdk {

  static const int LOAD_SUCCESS = 0;
  static const int ERROR_UNKNOWN = -1;

  static const MethodChannel _channel = MethodChannel('tbs_dynamic');

  static Future<void> initX5Core({X5CoreDynamicLoadListener? listener}) async {
    if (defaultTargetPlatform == TargetPlatform.android) {
      int result = await _channel.invokeMethod('initX5Core');
      if (result == LOAD_SUCCESS) {
        listener?.onX5CoreLoadSuccess();
      } else {
        listener?.onX5CoreLoadFailed(result);
      }
    }
    return Future.value(null);
  }

  static Future<bool> isX5Available() async {
    return await _channel.invokeMethod('isX5Available');
  }
}

abstract class X5CoreDynamicLoadListener {

  void onX5CoreLoadFailed(int errorCode);

  void onX5CoreLoadSuccess();
}