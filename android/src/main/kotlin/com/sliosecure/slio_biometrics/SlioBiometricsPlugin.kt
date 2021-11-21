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
import java.lang.Error

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
    when (call.method) {
        "getPlatformVersion" -> {
          result.success("Android ${android.os.Build.VERSION.RELEASE}")
        }
        "getDeviceType" -> {
          result.success(fpm.deviceType)
        }
        "getDeviceIO" -> {
          val deviceType = call.argument<Int>("type")
          try {
            val type = fpm.getDeviceIO(deviceType)
            result.success(type)
          }catch (e: Error){
            result.error("Device Type Error ", e.message , null)
          }
        }
        "getUartName" -> {
          result.success(fpm.getUartName)
          try {
            val uart = fpm.getUartName()
            result.success(uart)
          }catch (e: Error){
            result.error("UART FPM Error ", e.message , null)
          }
        }
        "getUartBaudRate" -> {
          try {
            val baudRate =  fpm.getUartBaudRate()
            result.success(baudRate)
          }catch (e: Error){
            result.error("BaudRate FPM Error ", e.message , null)
          }
        }
        "PowerControl" -> {
          try {
            fpm.PowerControl()
            result.success()
          }catch (e: Error){
            result.error("PowerControl FPM Error ", e.message , null)
          }
        }
        "OpenDevice" -> {
          try {
            fpm.OpenDevice()
            result.success()
          }catch (e: Error){
            result.error("Open FPM Error ", e.message , null)
          }
        }
        "CloseDevice" -> {
          try {
            fpm.CloseDevice()
            result.success()
          }catch (e: Error){
            result.error("Close FPM Error ", e.message , null)
          }
        }
        "InitMatch" -> {
          try {
            fpm.InitMatch()
            result.success()
          }catch (e: Error){
            result.error("Initialization Error ", e.message , null)
          }
        }
        else -> {
          result.notImplemented()
        }
    }
  }

  override fun onDetachedFromEngine(@NonNull binding: FlutterPlugin.FlutterPluginBinding) {
    channel.setMethodCallHandler(null)
  }
}
