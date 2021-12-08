import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:tbs_dynamic_plugin/tbs_dynamic_sdk.dart';

void main() {
  const MethodChannel channel = MethodChannel('tbs_dynamic');

  TestWidgetsFlutterBinding.ensureInitialized();

  setUp(() {
    channel.setMockMethodCallHandler((MethodCall methodCall) async {
      return '42';
    });
  });

  tearDown(() {
    channel.setMockMethodCallHandler(null);
  });

  test('getPlatformVersion', () async {
    // expect(await TbsDynamicSdk.platformVersion, '42');
  });
}
