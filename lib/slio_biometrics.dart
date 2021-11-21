library slio_biometrics;

import 'dart:async';

import 'package:flutter/services.dart';

class SlioBiometrics {
  static const MethodChannel _channel = MethodChannel('slio_biometrics');

  static Future<String?> get platformVersion async {
    final String? version = await _channel.invokeMethod('getPlatformVersion');
    return version;
  }
  static Future<dynamic> get  deviceType async {
    final dynamic type = await _channel.invokeMethod('getDeviceType');
    return type;
  }
    static Future<dynamic> get  deviceIO async {
    final dynamic type = await _channel.invokeMethod('getDeviceIO');
    return type;
  }
      static Future<dynamic> get  uartName async {
    final dynamic type = await _channel.invokeMethod('getUartName');
    return type;
  }
      static Future<dynamic> get  uartBaudRate async {
    final dynamic type = await _channel.invokeMethod('getUartBaudRate');
    return type;
  }
      static Future<dynamic> get  PowerControl async {
    final dynamic type = await _channel.invokeMethod('PowerControl');
    return type;
  }
      static Future<dynamic> get  OpenDevice async {
    final dynamic type = await _channel.invokeMethod('OpenDevice');
    return type;
  }
      static Future<dynamic> get  CloseDevice async {
    final dynamic type = await _channel.invokeMethod('CloseDevice');
    return type;
  }
      static Future<dynamic> get  InitMatch async {
    final dynamic type = await _channel.invokeMethod('InitMatch');
    return type;
  }
}

