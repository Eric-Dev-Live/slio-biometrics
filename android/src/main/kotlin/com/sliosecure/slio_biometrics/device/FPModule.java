package com.sliosecure.slio_biometrics.device;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.fpi.GpioControl;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.Handler;
import android.os.SystemClock;
import android.util.Log;


import com.sliosecure.slio_biometrics.fpcore.FPMatch;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Objects;

public class FPModule {

    public byte bmpdata[] = new byte[Constants.RESBMP_SIZE];            //73728+1078=74806
    public int bmpsize[] = new int[1];
    public byte rawdata[] = new byte[Constants.RESRAW_SIZE];            //73728
    public int rawsize[] = new int[1];

    public byte refdata[] = new byte[2048];
    public int refsize = 0;
    public byte tmpdata[] = new byte[512];
    public int tmpsize[] = new int[1];

    private int mDeviceType = Constants.DEV_UNKNOW;
    private int mDeviceIO = Constants.DEV_IO_UNKNOW;
    private int mBaudRate = 0;
    private String mUartName = "";

    private Context pContext = null;
    private Handler mHandler = null;
    private int m_ImageType = 0;

    private volatile boolean bCancel = false;
    private volatile boolean isopening = false;
    private volatile boolean isworking = false;
    private volatile boolean isCheckLift = false;
    private volatile boolean lastCheckLift = false;

    private int captureCount = 0;
    private int captureIndex = 0;
    private int captureTimeout = 0;
    private int timeoutCount = 0;

    public static final String ACTION_USB_PERMISSION = "com.fgtit.device.USB_PERMISSION";
    private PendingIntent mPermissionIntent;
    private IntentFilter filter;

    private SerialModule serialModule;
    private UsbModule usbModule;
    private HighModule highModule;

    public FPModule() {
        mDeviceType = getDeviceType();
        mDeviceIO = getDeviceIO(mDeviceType);
        mBaudRate = getUartBaudRate(mDeviceType);
        mUartName = getUartName(mDeviceType);

        serialModule = new SerialModule();
        usbModule = new UsbModule();
        highModule = new HighModule();
        SerialModuleInit();
        HighModuleInit();

        Log.i("xpb",
                "Device Info=" + String.valueOf(mDeviceType) + "/" + String.valueOf(mDeviceIO) +
                        "/" + String.valueOf(mBaudRate) + "/" + mUartName);
    }

    public int getDeviceType() {
        Log.d("FPModule", "come in ");
        String devname = android.os.Build.MODEL;
        String devid = android.os.Build.DEVICE;
        String devmodel = android.os.Build.DISPLAY;

        //8'
        if (devname.equals("FP08") || devname.equals("FP-08") || devname.equals("FP-08T") || devname.equals("TIQ-805Q")) {
            if (devname.equals("FP-08T")) {
                return Constants.DEV_8_4G_UART;
            } else if (devmodel.indexOf("35SM") >= 0) {
                return Constants.DEV_8_4G_USB;
            }
            return Constants.DEV_8_WIFI_USB;
        }

        //7'
        if (devname.equals("b82") || devname.equals("FP07") || devname.equals("FP-07")) {
            if (devname.equals("b82")) {
                return Constants.DEV_7_3G_SPI;
            } else {
                if (devid.equals("b906")) return Constants.DEV_7_3G_SPI;
                else if (devmodel.indexOf("35SM") >= 0) return Constants.DEV_7_4G_USB;
                else if (devmodel.indexOf("80M") >= 0) return Constants.DEV_7_3G_USB;
                return Constants.DEV_7_3G_SPI;
            }
        }

        //6'
        if (devname.equals("FP06") || devname.equals("FP-06")) {
            return Constants.DEV_6_4G_UART;
        }

        //Access
        if (devname.equals("FT06") || devname.equals("FT-06")) {
            return Constants.DEV_5_3G_UART_AC;
        }

        if (devname.equals("HF-A5")) {
            return Constants.DEV_5_3G_UART_A5;
        }

        //7.0
        if (devname.equals("FP--05")) {
            return Constants.DEV_5_4G_UART_AC;
        }

        if (devname.equals("mbk82_tb_kk") || devname.equals("iMA122") || devname.equals("iMA321") || devname.equals("iMA322") || devname.equals("BioMatch FM-01") || devname.equals("FP05") || devname.equals("FP-05") || devname.equals("KT-7500")) {
            if (devmodel.indexOf("35SM") >= 0) {
                return Constants.DEV_5_4G_UART_A6;
            } else if (devmodel.indexOf("80M") >= 0) {
                return Constants.DEV_5_3G_UART_A6;
            } else if(devmodel.indexOf("37SM") >= 0) {
                return Constants.DEV_5_4G_UART_A6;
            } else {
                if (devmodel.substring(0, 15).equals("FGT_FP05_v1.1.2")) {
                    return Constants.DEV_5_3G_UART_340M;
                }
                return Constants.DEV_5_3G_UART_A4;
            }
        }

        if(devmodel.indexOf("37SM") >= 0) {
            return Constants.DEV_5_4G_UART_A6;
        }


        return Constants.DEV_UNKNOW;
    }

    public int getDeviceIO(int devType) {
        switch (devType) {
            case Constants.DEV_5_3G_UART_A4:
            case Constants.DEV_5_3G_UART_340M:
            case Constants.DEV_5_4G_UART_A6:
            case Constants.DEV_5_3G_UART_A6:
            case Constants.DEV_5_3G_UART_AC:
            case Constants.DEV_5_3G_UART_A5:
            case Constants.DEV_5_4G_UART_AC:
            case Constants.DEV_6_4G_UART:
            case Constants.DEV_8_4G_UART:
                return Constants.DEV_IO_UART;

            case Constants.DEV_7_3G_SPI:
                return Constants.DEV_IO_SPI;

            case Constants.DEV_8_WIFI_USB:
            case Constants.DEV_8_4G_USB:
            case Constants.DEV_7_3G_USB:
            case Constants.DEV_7_4G_USB:
                return Constants.DEV_IO_USB;
        }
        return Constants.DEV_IO_UNKNOW;
    }

    public String getUartName(int devType) {
        switch (devType) {
            case Constants.DEV_5_3G_UART_A4:
                return "/dev/ttyMT3";
            case Constants.DEV_5_3G_UART_340M:
                return "/dev/ttyMT1";
            case Constants.DEV_7_3G_SPI:
                return "/dev/spidev0.0";
            case Constants.DEV_5_4G_UART_A6:
                return "/dev/ttyMT1";
            case Constants.DEV_5_3G_UART_A6:
                return "/dev/ttyMT1";
            case Constants.DEV_5_3G_UART_AC:
                return "/dev/ttyMT1";
            case Constants.DEV_5_3G_UART_A5:
                return "/dev/ttyMT1";
            case Constants.DEV_5_4G_UART_AC:
                return "/dev/ttyMT1";
            case Constants.DEV_6_4G_UART:
            case Constants.DEV_8_4G_UART:
                return "/dev/ttyMT2";
        }
        return "";
    }

    public int getUartBaudRate(int devType) {
        if (Constants.DEV_8_4G_UART == devType) {
            return 921600;
        }
        if (Constants.DEV_7_3G_SPI == devType) {
            return 2000 * 1000;
        } else {
            return 460800;
        }
    }

    public boolean PowerControl(boolean bOn) {
        switch (mDeviceType) {
            case Constants.DEV_5_3G_UART_A4: {
                GpioControl ca = new GpioControl();
                if (bOn) {
                    ca.setGpioMode(119, 0);
                    ca.setGpioDir(119, 1);
                    ca.setGpioOut(119, 1);
                } else {
                    ca.setGpioMode(119, 0);
                    ca.setGpioDir(119, 1);
                    ca.setGpioOut(119, 0);
                }
            }
            return true;
            case Constants.DEV_5_3G_UART_340M: {
                GpioControl ca = new GpioControl();
                if (bOn) {
                    ca.setGpioMode(65, 0);
                    ca.setGpioDir(65, 1);
                    ca.setGpioOut(65, 1);
                } else {
                    ca.setGpioMode(65, 0);
                    ca.setGpioDir(65, 1);
                    ca.setGpioOut(65, 0);
                }
            }
            return true;
            case Constants.DEV_5_4G_UART_A6: {
                GpioControl ca = new GpioControl();
                if (bOn) {
                    ca.setGpioMode(54, 0);
                    ca.setGpioDir(54, 1);
                    ca.setGpioOut(54, 1);
                } else {
                    ca.setGpioMode(54, 0);
                    ca.setGpioDir(54, 1);
                    ca.setGpioOut(54, 0);
                }
            }
            return true;
            case Constants.DEV_5_3G_UART_A6: {
                Log.d("FPModule", "??????");
                GpioControl ca = new GpioControl();
                if (bOn) {
                    ca.setGpioMode(65, 0);
                    ca.setGpioDir(65, 1);
                    ca.setGpioOut(65, 1);
                } else {
                    ca.setGpioMode(65, 0);
                    ca.setGpioDir(65, 1);
                    ca.setGpioOut(65, 0);
                }
            }
            return true;
            case Constants.DEV_5_3G_UART_AC: {
                GpioControl ca = new GpioControl();
                if (bOn) {
                    ca.setGpioMode(14, 0);
                    ca.setGpioDir(14, 1);
                    ca.setGpioOut(14, 1);
                } else {
                    ca.setGpioMode(14, 0);
                    ca.setGpioDir(14, 1);
                    ca.setGpioOut(14, 0);
                }
            }
            return true;
            case Constants.DEV_5_3G_UART_A5: {
                GpioControl ca = new GpioControl();
                if (bOn) {
                    ca.setGpioMode(15, 0);
                    ca.setGpioDir(15, 1);
                    ca.setGpioOut(15, 1);
                } else {
                    ca.setGpioMode(15, 0);
                    ca.setGpioDir(15, 1);
                    ca.setGpioOut(15, 0);
                }
            }
            return true;
            case Constants.DEV_5_4G_UART_AC: {
                GpioControl ca = new GpioControl();
                if (bOn) {
                    ca.setGpioMode(59, 0);
                    ca.setGpioDir(59, 1);
                    ca.setGpioOut(59, 1);
                } else {
                    ca.setGpioMode(59, 0);
                    ca.setGpioDir(59, 1);
                    ca.setGpioOut(59, 0);
                }
            }
            return true;
            case Constants.DEV_6_4G_UART: {
                GpioControl ca = new GpioControl();
                if (bOn) {
                    ca.setGpioMode(54, 0);
                    ca.setGpioDir(54, 1);
                    ca.setGpioOut(54, 1);
                } else {
                    ca.setGpioMode(54, 0);
                    ca.setGpioDir(54, 1);
                    ca.setGpioOut(54, 0);
                }
            }
            return true;
            case Constants.DEV_7_3G_SPI: {
			/*
			SerialPort sp=new SerialPort();
			if(bOn){
				sp.PowerSwitch(true);
			}else{
				sp.PowerSwitch(false);
			}
			*/
            }
            return true;
            case Constants.DEV_7_3G_USB: {
            }
            return true;
            case Constants.DEV_7_4G_USB: {
            }
            return true;
            case Constants.DEV_8_WIFI_USB: {
            }
            return true;
            case Constants.DEV_8_4G_USB: {
                GpioControl ca = new GpioControl();
                if (bOn) {
                    ca.setGpioMode(54, 0);
                    ca.setGpioDir(54, 1);
                    ca.setGpioOut(54, 1);
                } else {
                    ca.setGpioMode(54, 0);
                    ca.setGpioDir(54, 1);
                    ca.setGpioOut(54, 0);
                }
            }
            return true;
            case Constants.DEV_8_4G_UART: {
                GpioControl ca = new GpioControl();
                if (bOn) {
                    ca.setGpioMode(54, 0);
                    ca.setGpioDir(54, 1);
                    ca.setGpioOut(54, 1);
                } else {
                    ca.setGpioMode(54, 0);
                    ca.setGpioDir(54, 1);
                    ca.setGpioOut(54, 0);
                }
            }
            return true;
        }
        return false;
    }

    public void OpenDevice() {
        bCancel = false;
        isopening = false;
        isworking = false;
        if (mDeviceIO == Constants.DEV_IO_USB) {
            if (mDeviceType == Constants.DEV_8_WIFI_USB) {
                requestPermission();
            } else {
                PowerControl(true);
            }
        } else if (mDeviceIO == Constants.DEV_IO_SPI) {
            PowerControl(true);
            if (serialModule.OpenDevice(mUartName, mBaudRate, true)) {
                serialModule.Cancel(false);
                PostNclMsg(Constants.FPM_DEVICE, Constants.DEV_OK);
                isopening = true;
            } else {
                PostNclMsg(Constants.FPM_DEVICE, Constants.DEV_FAIL);
                isopening = false;
            }
        } else if (mDeviceIO == Constants.DEV_IO_UART) {
            PowerControl(true);
            if (mDeviceType == Constants.DEV_8_4G_UART) {
                if (highModule.OpenDevice(mUartName, mBaudRate)) {
                    highModule.Cancel(false);
                    PostNclMsg(Constants.FPM_DEVICE, Constants.DEV_OK);
                    isopening = true;
                } else {
                    PostNclMsg(Constants.FPM_DEVICE, Constants.DEV_FAIL);
                    isopening = false;
                }
            } else {
                if (serialModule.OpenDevice(mUartName, mBaudRate, false)) {
                    serialModule.Cancel(false);
                    PostNclMsg(Constants.FPM_DEVICE, Constants.DEV_OK);
                    isopening = true;
                } else {
                    PostNclMsg(Constants.FPM_DEVICE, Constants.DEV_FAIL);
                    isopening = false;
                }
            }
        }
    }

    public void CloseDevice() {
        bCancel = true;
        isopening = false;
        isworking = false;
        if (mDeviceIO == Constants.DEV_IO_USB) {
            if (mDeviceType == Constants.DEV_8_WIFI_USB) {
                usbModule.CloseDevice();
            } else {
                PowerControl(false);
            }
        } else if (mDeviceIO == Constants.DEV_IO_SPI) {
            serialModule.Cancel(true);
            serialModule.CloseDevice();
            PowerControl(false);
        } else if (mDeviceIO == Constants.DEV_IO_UART) {
            if (mDeviceType == Constants.DEV_8_4G_UART) {
                highModule.Cancel(true);
                highModule.CloseDevice();
            } else {
                serialModule.Cancel(true);
                serialModule.CloseDevice();
            }
            PowerControl(false);
        }
        PostNclMsg(Constants.FPM_LIFT, Constants.DEV_CLOSE);
    }

    public void SetContextHandler(Context parentContext, Handler handler) {
        pContext = parentContext;
        mHandler = handler;
        usbModule.SetInstance(pContext);
    }

    private void PostNclMsg(int sdkmsg, int psdkresult) {
        mHandler.obtainMessage(sdkmsg, psdkresult, -1).sendToTarget();
    }

    public void ResumeRegister() {
        mPermissionIntent = PendingIntent.getBroadcast(pContext, 0,
                new Intent(ACTION_USB_PERMISSION), 0);
        filter = new IntentFilter(ACTION_USB_PERMISSION);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
        pContext.registerReceiver(mUsbReceiver, filter);
    }

    public void PauseUnRegister() {
        if (mUsbReceiver != null) {
            pContext.unregisterReceiver(mUsbReceiver);
        }
    }

    public void requestPermission() {
        UsbManager pmusbManager =
                (UsbManager) ((Activity) pContext).getSystemService(Context.USB_SERVICE);
        if (pmusbManager == null) {
            PostNclMsg(Constants.FPM_DEVICE, Constants.DEV_FAIL);
            return;
        }

        UsbDevice pmusbDevice = null;
        HashMap<String, UsbDevice> devlist = pmusbManager.getDeviceList();
        Iterator<UsbDevice> deviter = devlist.values().iterator();
        while (deviter.hasNext()) {
            UsbDevice tmpusbdev = deviter.next();
            Log.i("xpb", "find=" + String.valueOf(tmpusbdev.getVendorId()));
            if ((tmpusbdev.getVendorId() == 0x0453) && (tmpusbdev.getProductId() == 0x9005)) {
                Log.i("xpb", "usb=0x0453,0x9005");
                pmusbDevice = tmpusbdev;
                break;
            } else if ((tmpusbdev.getVendorId() == 0x2009) && (tmpusbdev.getProductId() == 0x7638)) {
                Log.i("xpb", "usb=0x2009,0x7638");
                pmusbDevice = tmpusbdev;
                break;
            } else if ((tmpusbdev.getVendorId() == 0x2109) && (tmpusbdev.getProductId() == 0x7638)) {
                Log.i("xpb", "usb=0x2109,0x7638");
                pmusbDevice = tmpusbdev;
                break;
            } else if ((tmpusbdev.getVendorId() == 0x0483) && (tmpusbdev.getProductId() == 0x5720)) {
                Log.i("xpb", "usb=0x0483,0x5720");
                pmusbDevice = tmpusbdev;
                break;
            }
        }

        if (pmusbDevice != null) {
            if (!pmusbManager.hasPermission(pmusbDevice)) {
                synchronized (mUsbReceiver) {
                    pmusbManager.requestPermission(pmusbDevice, mPermissionIntent);
                }
                //Toast.makeText(pContext, "����Ȩ��", Toast.LENGTH_LONG).show();
            } else {
                //Toast.makeText(pContext, "����Ȩ��", Toast.LENGTH_LONG).show();
                usbModule.CloseDevice();
                if (usbModule.OpenDevice() == 0) {
                    PostNclMsg(Constants.FPM_DEVICE, Constants.DEV_OK);
                    isopening = true;
                } else {
                    PostNclMsg(Constants.FPM_DEVICE, Constants.DEV_FAIL);
                    isopening = false;
                }
            }
        }
    }

    private final BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (ACTION_USB_PERMISSION.equals(action)) {
                synchronized (this) {
                    UsbDevice device =
                            (UsbDevice) intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                    if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                        if (device != null) {
                            //Toast.makeText(pContext, "Ȩ���Ѿ�����", Toast.LENGTH_LONG).show();
                            usbModule.CloseDevice();
                            if (usbModule.OpenDevice() == 0) {
                                PostNclMsg(Constants.FPM_DEVICE, Constants.DEV_OK);
                                isopening = true;
                            } else {
                                PostNclMsg(Constants.FPM_DEVICE, Constants.DEV_FAIL);
                                isopening = false;
                            }
                        } else {
                            PostNclMsg(Constants.FPM_DEVICE, Constants.DEV_NOFOUND);
                        }
                    } else {
                        PostNclMsg(Constants.FPM_DEVICE, Constants.DEV_FAIL);
                    }
                }
            } else if (UsbManager.ACTION_USB_DEVICE_ATTACHED.equals(action)) {
                PostNclMsg(Constants.FPM_DEVICE, Constants.DEV_ATTACHED);
                requestPermission();
            } else if (UsbManager.ACTION_USB_DEVICE_DETACHED.equals(action)) {
                PostNclMsg(Constants.FPM_DEVICE, Constants.DEV_DETACHED);
                CloseDevice();
            }
        }
    };

    public int InitMatch() {
        return Objects.requireNonNull(FPMatch.Companion.getInstance()).InitMatch();
    }

    public boolean MatchTemplate(byte[] refdat, int refsize, byte[] matdat, int matsize,
                                 int score) {
        int rc = refsize / 256;
        int mc = matsize / 256;
        byte[] ref = new byte[256];
        byte[] mat = new byte[256];
        for (int i = 0; i < mc; i++) {
            System.arraycopy(matdat, i * 256, mat, 0, 256);
            for (int j = 0; j < rc; j++) {
                System.arraycopy(refdat, j * 256, ref, 0, 256);
                if (Objects.requireNonNull(FPMatch.Companion.getInstance()).MatchTemplate(ref, mat) >= score) {
                    return true;
                }
            }
        }
        return false;
    }

    protected void SerialModuleInit() {
        serialModule.setOnGetImageListener(new SerialModule.OnGetImageListener() {
            @Override
            public void onGetImageSuccess() {
                if (isCheckLift) {
                    serialModule.FP_GetImage();
                } else {
                    PostNclMsg(Constants.FPM_LIFT, Constants.RET_OK);
                    serialModule.FP_UpImage();
                }
            }

            @Override
            public void onGetImageFail() {
                captureTimeout++;
                if (captureTimeout > timeoutCount) {
                    PostNclMsg(Constants.FPM_TIMEOUT, Constants.RET_FAIL);
                    isworking = false;
                    serialModule.Cancel(true);
                    return;
                }
                if (bCancel) {
                    bCancel = false;
                    isworking = false;
                    serialModule.Cancel(true);
                    Log.i("xpb", "bCancel Get Image");
                } else {
                    if (isCheckLift) {
                        isCheckLift = false;
                        if (lastCheckLift) {
                            if (captureIndex >= captureCount) {
                                PostNclMsg(Constants.FPM_GENCHAR, Constants.RET_OK);
                                isworking = false;
                                return;
                            }
                        }
                        PostNclMsg(Constants.FPM_PLACE, Constants.RET_OK);
                    }
                    serialModule.FP_GetImage();
                }
            }
        });

        serialModule.setOnUpImageListener(new SerialModule.OnUpImageListener() {
            @Override
            public void onUpImageSuccess(byte[] data) {
                Log.i("whw", "up image data.length=" + data.length);
                bmpsize[0] = data.length;
                System.arraycopy(data, 0, bmpdata, 0, data.length);
                PostNclMsg(Constants.FPM_NEWIMAGE, 0);
                serialModule.FP_GenChar(1);
            }

            @Override
            public void onUpImageFail() {
                PostNclMsg(Constants.FPM_GENCHAR, Constants.RET_FAIL);
                isworking = false;
            }
        });

        serialModule.setOnGenCharListener(new SerialModule.OnGenCharListener() {
            @Override
            public void onGenCharSuccess(int bufferId) {
                serialModule.FP_UpChar();
            }

            @Override
            public void onGenCharFail() {
                PostNclMsg(Constants.FPM_GENCHAR, Constants.RET_FAIL);
                isworking = false;
            }
        });

        serialModule.setOnUpCharListener(new SerialModule.OnUpCharListener() {
            @Override
            public void onUpCharSuccess(byte[] model) {
                if (captureCount == 1) {
                    refsize = 256;
                    System.arraycopy(model, 0, refdata, 0, model.length);
                    captureIndex++;
                    if (lastCheckLift) {
                        isCheckLift = true;
                        PostNclMsg(Constants.FPM_LIFT, Constants.RET_OK);
                        serialModule.FP_GetImage();
                        captureTimeout = 0;
                    } else {
                        PostNclMsg(Constants.FPM_GENCHAR, Constants.RET_OK);
                        isworking = false;
                    }
                } else {
                    refsize = (captureIndex + 1) * 256;
                    System.arraycopy(model, 0, refdata, captureIndex * 256, 256);
                    captureIndex++;
                    if (captureIndex >= captureCount) {
                        if (lastCheckLift) {
                            isCheckLift = true;
                            PostNclMsg(Constants.FPM_LIFT, Constants.RET_OK);
                            serialModule.FP_GetImage();
                            captureTimeout = 0;
                        } else {
                            PostNclMsg(Constants.FPM_GENCHAR, Constants.RET_OK);
                            isworking = false;
                        }
                    } else {
                        isCheckLift = true;
                        PostNclMsg(Constants.FPM_LIFT, Constants.RET_OK);
                        serialModule.FP_GetImage();
                        captureTimeout = 0;
                    }
                }
            }

            @Override
            public void onUpCharFail() {
                PostNclMsg(Constants.FPM_GENCHAR, Constants.RET_FAIL);
                isworking = false;
            }
        });
    }

    protected void HighModuleInit() {
        highModule.setOnGetImageListener(new HighModule.OnGetImageListener() {
            @Override
            public void onGetImageSuccess() {
                if (isCheckLift) {
                    highModule.FP_GetImage();
                } else {
                    PostNclMsg(Constants.FPM_LIFT, Constants.RET_OK);
                    highModule.FP_UpImage();
                }
            }

            @Override
            public void onGetImageFail() {
                captureTimeout++;
                if (captureTimeout > timeoutCount) {
                    PostNclMsg(Constants.FPM_TIMEOUT, Constants.RET_FAIL);
                    isworking = false;
                    highModule.Cancel(true);
                    return;
                }
                if (bCancel) {
                    isworking = false;
                    bCancel = false;
                    highModule.Cancel(true);
                    Log.i("xpb", "bCancel Get Image");
                } else {
                    if (isCheckLift) {
                        isCheckLift = false;
                        if (lastCheckLift) {
                            if (captureIndex >= captureCount) {
                                PostNclMsg(Constants.FPM_GENCHAR, Constants.RET_OK);
                                isworking = false;
                                return;
                            }
                        }
                        PostNclMsg(Constants.FPM_PLACE, Constants.RET_OK);
                    }
                    highModule.FP_GetImage();
                }
            }
        });

        highModule.setOnUpImageListener(new HighModule.OnUpImageListener() {
            @Override
            public void onUpImageSuccess(byte[] data) {
                Log.i("whw", "up image data.length=" + data.length);
                bmpsize[0] = data.length;
                System.arraycopy(data, 0, bmpdata, 0, data.length);
                PostNclMsg(Constants.FPM_NEWIMAGE, 0);
                highModule.FP_GenChar(1);
            }

            @Override
            public void onUpImageFail() {
                PostNclMsg(Constants.FPM_GENCHAR, Constants.RET_FAIL);
                isworking = false;
            }
        });

        highModule.setOnGenCharListener(new HighModule.OnGenCharListener() {
            @Override
            public void onGenCharSuccess(int bufferId) {
                highModule.FP_UpChar();
            }

            @Override
            public void onGenCharFail() {
                PostNclMsg(Constants.FPM_GENCHAR, Constants.RET_FAIL);
                isworking = false;
            }
        });

        highModule.setOnUpCharListener(new HighModule.OnUpCharListener() {
            @Override
            public void onUpCharSuccess(byte[] model) {
                if (captureCount == 1) {
                    refsize = 256;
                    System.arraycopy(model, 0, refdata, 0, model.length);
                    captureIndex++;
                    if (lastCheckLift) {
                        isCheckLift = true;
                        PostNclMsg(Constants.FPM_LIFT, Constants.RET_OK);
                        highModule.FP_GetImage();
                        captureTimeout = 0;
                    } else {
                        PostNclMsg(Constants.FPM_GENCHAR, Constants.RET_OK);
                        isworking = false;
                    }
                } else {
                    refsize = (captureIndex + 1) * 256;
                    System.arraycopy(model, 0, refdata, captureIndex * 256, 256);
                    captureIndex++;
                    if (captureIndex >= captureCount) {
                        if (lastCheckLift) {
                            isCheckLift = true;
                            PostNclMsg(Constants.FPM_LIFT, Constants.RET_OK);
                            highModule.FP_GetImage();
                            captureTimeout = 0;
                        } else {
                            PostNclMsg(Constants.FPM_GENCHAR, Constants.RET_OK);
                            isworking = false;
                        }
                    } else {
                        isCheckLift = true;
                        PostNclMsg(Constants.FPM_LIFT, Constants.RET_OK);
                        highModule.FP_GetImage();
                        captureTimeout = 0;
                    }
                }

                isworking = false;
            }

            @Override
            public void onUpCharFail() {
                PostNclMsg(Constants.FPM_GENCHAR, Constants.RET_FAIL);
                isworking = false;
            }
        });
    }


    public class WorkThread implements Runnable {
        public WorkThread() {
            //isworking=true;
        }

        public boolean CheckFingerLift() {
            int ret = 1;
            int timeout = 0;
            while (true) {
                ret = usbModule.FxGetImage(m_ImageType, 0xffffffff);
                if (ret != 0) return true;

                SystemClock.sleep(100);
                timeout++;
                if (timeout > timeoutCount) {
                    PostNclMsg(Constants.FPM_TIMEOUT, Constants.RET_FAIL);
                    isworking = false;
                    return false;
                }
                if (bCancel) {
                    bCancel = false;
                    isworking = false;
                    return false;
                }
            }
        }

        public void run() {
            int timeout = 0;
            if (!isopening) {
                PostNclMsg(Constants.FPM_DEVICE, Constants.DEV_FAIL);
                isworking = false;
                return;
            }

            for (captureIndex = 0; captureIndex < captureCount; captureIndex++) {
                PostNclMsg(Constants.FPM_PLACE, Constants.RET_OK);
                int ret = 1;
                while (true) {
                    ret = usbModule.FxGetImage(m_ImageType, 0xffffffff);
                    if (ret == 0) break;
                    SystemClock.sleep(100);
                    timeout++;
                    if (timeout > timeoutCount) {
                        PostNclMsg(Constants.FPM_TIMEOUT, Constants.RET_FAIL);
                        isworking = false;
                        return;
                    }
                    if (bCancel) {
                        bCancel = false;
                        isworking = false;
                        return;
                    }
                }
                PostNclMsg(Constants.FPM_LIFT, Constants.RET_OK);
                if (usbModule.FxUpImage(m_ImageType, 0xffffffff, rawdata, rawsize) == 0) {
                    usbModule.FPImageToBitmap(m_ImageType, rawdata, bmpdata);
                    PostNclMsg(Constants.FPM_NEWIMAGE, 0);
                } else {
                    PostNclMsg(Constants.FPM_GENCHAR, Constants.RET_FAIL);
                    isworking = false;
                    return;
                }
                if (usbModule.FxGenChar(m_ImageType, 0xffffffff, 0x01) != 0) {
                    tmpsize[0] = 0;
                    PostNclMsg(Constants.FPM_GENCHAR, Constants.RET_FAIL);
                    isworking = false;
                    return;
                }
                if (usbModule.FPUpChar(0xffffffff, 0x01, tmpdata, tmpsize) == 0) {
                    if (captureCount == 1) {
                        if (lastCheckLift) {
                            if (!CheckFingerLift()) {
                                PostNclMsg(Constants.FPM_GENCHAR, Constants.RET_FAIL);
                            }
                        }
                        refsize = 256;
                        System.arraycopy(tmpdata, 0, refdata, 0, 256);
                        PostNclMsg(Constants.FPM_GENCHAR, Constants.RET_OK);
                        isworking = false;
                        return;
                    } else {
                        refsize = (captureIndex + 1) * 256;
                        System.arraycopy(tmpdata, 0, refdata, captureIndex * 256, 256);
                        if ((captureIndex + 1) < captureCount) {
                            PostNclMsg(Constants.FPM_LIFT, Constants.RET_OK);
                            if (!CheckFingerLift()) {
                                PostNclMsg(Constants.FPM_GENCHAR, Constants.RET_FAIL);
                            }
                        } else {
                            if (lastCheckLift) {
                                if (!CheckFingerLift()) {
                                    PostNclMsg(Constants.FPM_GENCHAR, Constants.RET_FAIL);
                                }
                            }
                            PostNclMsg(Constants.FPM_GENCHAR, Constants.RET_OK);
                        }
                    }
                } else {
                    PostNclMsg(Constants.FPM_GENCHAR, Constants.RET_FAIL);
                }
            }
            isworking = false;
            return;
        }
    }

    private synchronized void ThreadGenrate(final int count) {
        if (isworking) {
            captureCount = count;
            if (captureCount > 4) captureCount = 4;
            captureIndex = 0;
            isCheckLift = false;
            captureTimeout = 0;
			
			/*
			bCancel=true;
			Handler handler = new Handler();
			handler.postDelayed(new Runnable() {
				@Override
				public void run() {
					if(mDeviceIO==Constants.DEV_IO_USB){
						isworking=true;
						Thread workThread = new Thread(new WorkThread());
			        	workThread.start();	        	
					}else{
						if(mDeviceType==Constants.DEV_8_4G_UART){
							highModule.Cancel(false);
							highModule.FP_GetImage();
							isworking=true;
							PostNclMsg(Constants.FPM_PLACE,Constants.RET_OK);
						}else{
							serialModule.Cancel(false);
							serialModule.FP_GetImage();
							isworking=true;
							PostNclMsg(Constants.FPM_PLACE,Constants.RET_OK);
						}
					}
					captureCount=count;
					if(captureCount>4)
						captureCount=4;
					captureIndex=0;	
					isCheckLift=false;
					captureTimeout=0;
				}
			}, 1500);
			*/
        } else {
            Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (mDeviceIO == Constants.DEV_IO_USB) {
                        isworking = true;
                        Thread workThread = new Thread(new WorkThread());
                        workThread.start();
                    } else {
                        if (mDeviceType == Constants.DEV_8_4G_UART) {
                            highModule.Cancel(false);
                            highModule.FP_GetImage();
                            isworking = true;
                            PostNclMsg(Constants.FPM_PLACE, Constants.RET_OK);
                        } else {
                            serialModule.Cancel(false);
                            serialModule.FP_GetImage();
                            isworking = true;
                            PostNclMsg(Constants.FPM_PLACE, Constants.RET_OK);
                        }
                    }
                    captureCount = count;
                    if (captureCount > 4) captureCount = 4;
                    captureIndex = 0;
                    isCheckLift = false;
                    captureTimeout = 0;
                }
            }, 100);
        }
    }

    public boolean GenerateTemplate(int count) {
        if (!isopening) return false;
        ThreadGenrate(count);
        return true;
    }

    public int GetTemplateByGen(byte[] lptemplate) {
        memcpy(lptemplate, 0, refdata, 0, refsize);
        return refsize;
    }

    public int GetBmpImage(byte[] lpbmpdate) {
        if (m_ImageType == 0) {
            memcpy(lpbmpdate, 0, bmpdata, 0, Constants.STDBMP_SIZE);
            return Constants.STDBMP_SIZE;
        } else {
            memcpy(lpbmpdate, 0, bmpdata, 0, Constants.RESBMP_SIZE);
            return Constants.RESBMP_SIZE;
        }
    }

    private void memcpy(byte[] dstbuf, int dstoffset, byte[] srcbuf, int srcoffset, int size) {
        for (int i = 0; i < size; i++) {
            dstbuf[dstoffset + i] = srcbuf[srcoffset + i];
        }
    }

    public void SetTimeOut(int tm) {
        //timeoutCount=tm*50/30;
        timeoutCount = tm;
        if (timeoutCount < 10) timeoutCount = 10;
    }

    public void Cancle() {
        if (isworking) {
            bCancel = true;
        }
    }

    public void SetLastCheckLift(boolean isLift) {
        lastCheckLift = isLift;
    }
}
