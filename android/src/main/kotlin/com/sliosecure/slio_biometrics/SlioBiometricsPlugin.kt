package com.sliosecure.slio_biometrics

import androidx.annotation.NonNull

import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result

import com.sliosecure.slio_biometrics.data.wsq;
import com.sliosecure.slio_biometrics.device.Constants;
import com.sliosecure.slio_biometrics.device.FPModule;
import com.sliosecure.slio_biometrics.fpcore.FPMatch;

/** SlioBiometricsPlugin */
class SlioBiometricsPlugin: FlutterPlugin, MethodCallHandler {
  /// The MethodChannel that will the communication between Flutter and native Android
  ///
  /// This local reference serves to register the plugin with the Flutter Engine and unregister it
  /// when the Flutter Engine is detached from the Activity
  private lateinit var channel : MethodChannel
  private val fpm = FPModule()

  private val bmpdata = ByteArray(Constants.RESBMP_SIZE)
  private val bmpsize = 0

  private val refdata = ByteArray(Constants.TEMPLATESIZE * 2)
  private val refsize = 0

  private val matdata = ByteArray(Constants.TEMPLATESIZE * 2)
  private val matsize = 0

  private val worktype = 0

  override fun onAttachedToEngine(@NonNull flutterPluginBinding: FlutterPlugin.FlutterPluginBinding) {
    channel = MethodChannel(flutterPluginBinding.binaryMessenger, "slio_biometrics")
    channel.setMethodCallHandler(this)
  }

  override fun onMethodCall(@NonNull call: MethodCall, @NonNull result: Result) {
    if (call.method == "getPlatformVersion") {
      result.success("Android ${android.os.Build.VERSION.RELEASE}")
    } else if (call.method == "getDeviceType") {
      result.success(fpm.deviceType)
    }else if (call.method == "getDeviceIO") {
      result.success(fpm.getDeviceIO)
    }else if (call.method == "getUartName") {
      result.success(fpm.getUartName)
    }else if (call.method == "getUartBaudRate") {
      result.success(fpm.getUartBaudRate)
    }else if (call.method == "PowerControl") {
      result.success(fpm.PowerControl)
    }else if (call.method == "OpenDevice") {
      result.success(fpm.OpenDevice)
    }else if (call.method == "CloseDevice") {
      result.success(fpm.CloseDevice)
    }else if (call.method == "InitMatch") {
      result.success(fpm.InitMatch)
    }
    else {
      result.notImplemented()
    }
  }

  override fun onDetachedFromEngine(@NonNull binding: FlutterPlugin.FlutterPluginBinding) {
    channel.setMethodCallHandler(null)
  }
}
