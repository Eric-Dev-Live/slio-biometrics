import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:slio_biometrics/slio_biometrics.dart';

void main() {
  const MethodChannel channel = MethodChannel('slio_biometrics');

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
    expect(await SlioBiometrics.platformVersion, '42');
  });
  test('getDeviceType', () async {
    expect(await SlioBiometrics.deviceType, '42');
  });
}
