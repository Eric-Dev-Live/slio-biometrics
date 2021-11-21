package com.sliosecure.slio_biometrics.device

import android.annotation.SuppressLint
import android.content.Context
import android.hardware.usb.UsbManager
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbDeviceConnection
import android.hardware.usb.UsbEndpoint
import android.widget.Toast
import android.hardware.usb.UsbConstants
import android.os.Handler
import android.os.SystemClock
import java.util.*
import kotlin.experimental.and

@SuppressLint("NewApi")
class UsbModule {
    private var mHandler: Handler? = null
    private val g_iworkmsg = IntArray(5)
    private val g_iretmsg = IntArray(5)
    private val g_imsginc = 0
    private var g_IsOpen = false
    private var g_IsLink = false
    private var g_bExit = false
    private val g_iTimeCount = 0
    private val g_bIsStart = false
    var bmpdata = ByteArray(74806)
    var bmpsize = IntArray(1)
    var rawdata = ByteArray(73728)
    var rawsize = IntArray(1)
    var refdata = ByteArray(512)
    var refsize = IntArray(1)
    var matdata = ByteArray(512)
    var matsize = IntArray(1)
    private var pContext: Context? = null
    private var devtype = 0
    private var usbManager: UsbManager? = null
    private var usbDevice: UsbDevice? = null
    private var usbConn: UsbDeviceConnection? = null
    private var usbEndpointIn: UsbEndpoint? = null
    private var usbEndpointOut: UsbEndpoint? = null
    private var m_nEPInSize = 0
    private var m_nEPOutSize = 0
    var bAutoPermission = false

    //private PendingIntent mPermissionIntent;
    private var bUpImage = true
    fun SetUpImage(isup: Boolean) {
        bUpImage = isup
    }

    /////////////////////////////////////////////////////////////////////////////////
    private fun DebugShowInfo(data: ByteArray, size: Int) {
        var msg = ""
        for (i in 0 until size) {
            msg = msg + "," + Integer.toHexString((data[i] and 0xFF.toByte()).toInt()).uppercase(Locale.getDefault())
        }
        Toast.makeText(pContext, msg, Toast.LENGTH_LONG).show()
    }

    @SuppressLint("NewApi")
    private fun usb_get_device(): Boolean {
        if (pContext == null) return false
        usbManager = pContext!!.getSystemService(Context.USB_SERVICE) as UsbManager
        if (usbManager == null) {
            return false
        }
        val devlist = usbManager!!.deviceList
        val deviter: Iterator<UsbDevice> = devlist.values.iterator()
        while (deviter.hasNext()) {
            val tmpusbdev = deviter.next()
            if (tmpusbdev.vendorId == 0x0453 && tmpusbdev.productId == 0x9005) {
                devtype = 0
                usbDevice = tmpusbdev
                break
            } else if (tmpusbdev.vendorId == 0x2009 && tmpusbdev.productId == 0x7638) {
                devtype = 1
                usbDevice = tmpusbdev
                break
            } else if (tmpusbdev.vendorId == 0x2109 && tmpusbdev.productId == 0x7638) {
                devtype = 1
                usbDevice = tmpusbdev
                break
            } else if (tmpusbdev.vendorId == 0x0483 && tmpusbdev.productId == 0x5720) {
                devtype = 2
                usbDevice = tmpusbdev
                break
            }
        }
        return usbDevice != null
    }

    @SuppressLint("NewApi")
    private fun usb_open_device(): Boolean {
        usbConn = usbManager!!.openDevice(usbDevice)
        return if (usbConn == null) {
            false
        } else {
            val usbInterface = usbDevice!!.getInterface(0) //usbDevice.getInterface(1);
            usbConn!!.claimInterface(usbInterface, true)
            for (i in 0 until usbInterface.endpointCount) {
                if (usbInterface.getEndpoint(i).type == UsbConstants.USB_ENDPOINT_XFER_BULK) {
                    if (usbInterface.getEndpoint(i).direction == UsbConstants.USB_DIR_IN) {
                        usbEndpointIn = usbInterface.getEndpoint(i)
                    } else if (usbInterface.getEndpoint(i).direction == UsbConstants.USB_DIR_OUT) {
                        usbEndpointOut = usbInterface.getEndpoint(i)
                    }
                }
            }
            m_nEPInSize = usbEndpointIn!!.maxPacketSize
            m_nEPOutSize = usbEndpointOut!!.maxPacketSize
            true
        }
    }

    @SuppressLint("NewApi")
    private fun usb_close_device(): Boolean {
        if (usbConn != null) {
            usbConn!!.close()
        }
        return true
    }

    @SuppressLint("NewApi")
    private fun usb_controlmsg(requesttype: Int, request: Int, value: Int, index: Int, buffer: ByteArray, length: Int, timeout: Int): Int {
        return if (usbConn != null) usbConn!!.controlTransfer(requesttype, request, value, index, buffer, length, timeout) else -1
    }

    @SuppressLint("NewApi")
    private fun usb_bulkread(buffer: ByteArray, length: Int, timeout: Int): Int {
        return if (usbConn != null) usbConn!!.bulkTransfer(usbEndpointIn, buffer, length, timeout) else -1
    }

    @SuppressLint("NewApi")
    private fun usb_read_device(databuf: ByteArray, datasize: Int, timeout: Int): Int {
        return if (usbConn != null) {
            /*
			int r=1;
			r=usbConn.bulkTransfer(usbEndpointIn,databuf,datasize,timeout);
			if(r>=0)
				return 0;
			else
				return -1;
			*/
            val n = datasize / m_nEPInSize
            val r = datasize % m_nEPInSize
            var rs = 0
            var i = 0
            val tmp = ByteArray(512)
            i = 0
            while (i < n) {
                rs = usbConn!!.bulkTransfer(usbEndpointIn, tmp, m_nEPInSize, timeout)
                //if (rs != m_nEPInSize)
                //    return -1;
                System.arraycopy(tmp, 0, databuf, i * m_nEPInSize, m_nEPInSize)
                i++
            }
            if (r > 0) {
                rs = usbConn!!.bulkTransfer(usbEndpointIn, tmp, r, timeout)
                //if (rs != r)
                //    return -1;
                System.arraycopy(tmp, 0, databuf, i * m_nEPInSize, r)
            }
            0
        } else {
            -1
        }
    }

    @SuppressLint("NewApi")
    private fun usb_write_device(databuf: ByteArray, datasize: Int, timeout: Int): Int {
        return if (usbConn != null) {
            /*
			int r=1;
			r=usbConn.bulkTransfer(usbEndpointOut,databuf,datasize,timeout);
			if(r>=0)
				return 0;
			else
				return -2;
			*/
            val n = datasize / m_nEPOutSize
            val r = datasize % m_nEPOutSize
            var rs = 0
            var i = 0
            val tmp = ByteArray(512)
            i = 0
            while (i < n) {
                System.arraycopy(databuf, i * m_nEPOutSize, tmp, 0, m_nEPOutSize)
                rs = usbConn!!.bulkTransfer(usbEndpointOut, tmp, m_nEPOutSize, timeout)
                i++
            }
            if (r > 0) {
                System.arraycopy(databuf, i * m_nEPOutSize, tmp, 0, r)
                rs = usbConn!!.bulkTransfer(usbEndpointOut, tmp, r, timeout)
                //if (rs != r)
                //    return -2;
            }
            0
        } else {
            -2
        }
    }

    /////////////////////////////////////////////////////////////////////////////////
    private fun LibUSBOpen(): Boolean {
        if (usb_get_device()) {
            if (usb_open_device()) {
                return true
            }
        }
        return false
    }

    private fun LibUSBClose(): Boolean {
        return usb_close_device()
    }

    private fun LibUSBDownData(DataBuf: ByteArray, nLen: Int): Int {
        SystemClock.sleep(50)
        when (devtype) {
            0 -> {
                var nRet = 0
                val buf = ByteArray(10)
                val r = usb_controlmsg(Usb_Request_Type0 or Usb_Request_Type4 or Usb_Request_Type1, 0, nLen, 0, buf, 10, TIME_OUT)
                nRet = usb_write_device(DataBuf, nLen, TIME_OUT)
                return nRet
            }
            1 -> {
                val nRet = 0
                val do_CBW = ByteArray(33)
                val di_CSW = ByteArray(16)
                run {
                    var i = 0
                    while (i < 33) {
                        do_CBW[i] = 0x00.toByte()
                        i++
                    }
                }
                do_CBW[0] = 0x55.toByte()
                do_CBW[1] = 0x53.toByte()
                do_CBW[2] = 0x42.toByte()
                do_CBW[3] = 0x43.toByte()
                do_CBW[4] = 0xB0.toByte()
                do_CBW[5] = 0xFA.toByte()
                do_CBW[6] = 0x69.toByte()
                do_CBW[7] = 0x86.toByte()
                do_CBW[8] = 0x00.toByte()
                do_CBW[9] = 0x00.toByte()
                do_CBW[10] = 0x00.toByte()
                do_CBW[11] = 0x00.toByte()
                do_CBW[12] = 0x00.toByte()
                do_CBW[13] = 0x00.toByte()
                do_CBW[14] = 0x0A.toByte()
                do_CBW[15] = 0x86.toByte()
                run {
                    var i = 0
                    while (i < 16) {
                        di_CSW[i] = 0x00.toByte()
                        i++
                    }
                }
                do_CBW[8] = (nLen and 0xff).toByte()
                do_CBW[9] = (nLen shr 8 and 0xff).toByte()
                do_CBW[10] = (nLen shr 16 and 0xff).toByte()
                do_CBW[11] = (nLen shr 24 and 0xff).toByte()
                val r = usb_write_device(do_CBW, 31, TIME_OUT)
                if (r != 0) return -1
                var ret = usb_write_device(DataBuf, nLen, TIME_OUT)
                if (ret != 0) return -1
                ret = usb_read_device(di_CSW, 13, TIME_OUT)
                if (di_CSW[3] != 0x53.toByte() || di_CSW[12] != 0x00.toByte()) return -1
                di_CSW[3] = 0x43
                var i = 0
                while (i < 12) {
                    if (di_CSW[i] != do_CBW[i]) return -1
                    i++
                }
                return nRet
            }
            2 -> {
                val nRet = 0
                val do_CBW = ByteArray(33)
                val di_CSW = ByteArray(16)
                run {
                    var i = 0
                    while (i < 33) {
                        do_CBW[i] = 0x00.toByte()
                        i++
                    }
                }
                do_CBW[0] = 0x55.toByte()
                do_CBW[1] = 0x53.toByte()
                do_CBW[2] = 0x42.toByte()
                do_CBW[3] = 0x43.toByte()
                do_CBW[4] = 0xB0.toByte()
                do_CBW[5] = 0xFA.toByte()
                do_CBW[6] = 0x69.toByte()
                do_CBW[7] = 0x86.toByte()
                do_CBW[8] = 0x00.toByte()
                do_CBW[9] = 0x00.toByte()
                do_CBW[10] = 0x00.toByte()
                do_CBW[11] = 0x00.toByte()
                do_CBW[12] = 0x00.toByte()
                do_CBW[13] = 0x00.toByte()
                do_CBW[14] = 0x0A.toByte()
                do_CBW[15] = 0x86.toByte()
                var i = 0
                while (i < 16) {
                    di_CSW[i] = 0x00.toByte()
                    i++
                }
                do_CBW[8] = (nLen and 0xff).toByte()
                do_CBW[9] = (nLen shr 8 and 0xff).toByte()
                do_CBW[10] = (nLen shr 16 and 0xff).toByte()
                do_CBW[11] = (nLen shr 24 and 0xff).toByte()
                val r = usb_write_device(do_CBW, 31, TIME_OUT)
                if (r != 0) return -1
                var ret = usb_write_device(DataBuf, nLen, TIME_OUT)
                if (ret != 0) return -1
                ret = usb_read_device(di_CSW, 13, TIME_OUT)
                if (di_CSW[3] != 0x53.toByte() || di_CSW[12] != 0x00.toByte()) return -1
                di_CSW[3] = 0x43
                //DebugShowInfo(do_CBW,13);
                //DebugShowInfo(di_CSW,13);
                /*
				for(int i=0; i<12; i++){
					if((byte)di_CSW[i]!=(byte)do_CBW[i])
						return -1;
				}
				*/return nRet
            }
        }
        return -1
    }

    private fun LibUSBDownData1(DataBuf: ByteArray, nLen: Int): Int {
        when (devtype) {
            0 -> {
                var nRet = 0
                val buf = ByteArray(10)
                val r = usb_controlmsg(Usb_Request_Type0 or Usb_Request_Type4 or Usb_Request_Type1, 1, nLen, 0, buf, 10, TIME_OUT)
                nRet = usb_write_device(DataBuf, nLen, TIME_OUT)
                return nRet
            }
            1 -> {
                return LibUSBDownData(DataBuf, nLen)
            }
            2 -> {
                return LibUSBDownData(DataBuf, nLen)
            }
        }
        return -1
    }

    private fun LibUSBGetData(DataBuf: ByteArray, nLen: Int, Timeout: Int): Int {
        when (devtype) {
            0 -> {
                var nRet = 0
                val buf = ByteArray(10)
                var r = usb_controlmsg(Usb_Request_Type0 or Usb_Request_Type4 or Usb_Request_Type1, 1, nLen, 0, buf, 10, TIME_OUT)
                //nRet=usb_read_device (DataBuf,nLen,TIME_OUT);
                r = usb_bulkread(DataBuf, nLen, TIME_OUT)
                nRet = if (r >= 0) 0 else -1
                return nRet
            }
            1 -> {
                val nRet = 0
                val do_CBW = ByteArray(33)
                val di_CSW = ByteArray(16)
                run {
                    var i = 0
                    while (i < 33) {
                        do_CBW[i] = 0x00.toByte()
                        i++
                    }
                }
                do_CBW[0] = 0x55.toByte()
                do_CBW[1] = 0x53.toByte()
                do_CBW[2] = 0x42.toByte()
                do_CBW[3] = 0x43.toByte()
                do_CBW[4] = 0xB0.toByte()
                do_CBW[5] = 0xFA.toByte()
                do_CBW[6] = 0x69.toByte()
                do_CBW[7] = 0x86.toByte()
                do_CBW[8] = 0x00.toByte()
                do_CBW[9] = 0x00.toByte()
                do_CBW[10] = 0x00.toByte()
                do_CBW[11] = 0x00.toByte()
                do_CBW[12] = 0x80.toByte()
                do_CBW[13] = 0x00.toByte()
                do_CBW[14] = 0x0A.toByte()
                do_CBW[15] = 0x85.toByte()
                run {
                    var i = 0
                    while (i < 16) {
                        di_CSW[i] = 0x00.toByte()
                        i++
                    }
                }
                do_CBW[8] = (nLen and 0xff).toByte()
                do_CBW[9] = (nLen shr 8 and 0xff).toByte()
                do_CBW[10] = (nLen shr 16 and 0xff).toByte()
                do_CBW[11] = (nLen shr 24 and 0xff).toByte()
                val r = usb_write_device(do_CBW, 31, TIME_OUT)
                if (r < 0) return -1
                var ret = usb_read_device(DataBuf, nLen, TIME_OUT)
                if (ret < 0) return -1
                ret = usb_read_device(di_CSW, 13, TIME_OUT)
                if (di_CSW[3] != 0x53.toByte() || di_CSW[12] != 0x00.toByte()) return -1
                var i = 4
                while (i < 8) {
                    if (di_CSW[i] != do_CBW[i]) return -1
                    i++
                }
                return nRet
            }
            2 -> {
                val nRet = 0
                val do_CBW = ByteArray(33)
                val di_CSW = ByteArray(16)
                run {
                    var i = 0
                    while (i < 33) {
                        do_CBW[i] = 0x00.toByte()
                        i++
                    }
                }
                do_CBW[0] = 0x55.toByte()
                do_CBW[1] = 0x53.toByte()
                do_CBW[2] = 0x42.toByte()
                do_CBW[3] = 0x43.toByte()
                do_CBW[4] = 0xB0.toByte()
                do_CBW[5] = 0xFA.toByte()
                do_CBW[6] = 0x69.toByte()
                do_CBW[7] = 0x86.toByte()
                do_CBW[8] = 0x00.toByte()
                do_CBW[9] = 0x00.toByte()
                do_CBW[10] = 0x00.toByte()
                do_CBW[11] = 0x00.toByte()
                do_CBW[12] = 0x80.toByte()
                do_CBW[13] = 0x00.toByte()
                do_CBW[14] = 0x0A.toByte()
                do_CBW[15] = 0x85.toByte()
                var i = 0
                while (i < 16) {
                    di_CSW[i] = 0x00.toByte()
                    i++
                }
                do_CBW[8] = (nLen and 0xff).toByte()
                do_CBW[9] = (nLen shr 8 and 0xff).toByte()
                do_CBW[10] = (nLen shr 16 and 0xff).toByte()
                do_CBW[11] = (nLen shr 24 and 0xff).toByte()
                val r = usb_write_device(do_CBW, 31, Timeout)
                if (r < 0) return -1
                var ret = usb_read_device(DataBuf, nLen, Timeout)
                if (ret < 0) return -1
                ret = usb_read_device(di_CSW, 13, Timeout)
                return if (di_CSW[3] != 0x53.toByte() || di_CSW[12] != 0x00.toByte()) -1 else nRet
                /*
			for(int i=4; i<8; i++){
				if((byte)di_CSW[i]!=(byte)do_CBW[i])
					return -1;
			}
			*/
            }
        }
        return -1
    }

    private fun LibUSBGetImage(DataBuf: ByteArray, nLen: Int): Int {
        when (devtype) {
            0 -> {
                var r = 1
                val buf = ByteArray(10)
                val n = 8
                val len = nLen / n
                val tmp = ByteArray(len)
                r = usb_controlmsg(Usb_Request_Type0 or Usb_Request_Type4 or Usb_Request_Type1, 1, nLen, 0, buf, 10, TIME_OUT)
                var k = 0
                while (k < 8) {
                    r = usb_bulkread(tmp, len, TIME_OUT)
                    val t = len * k
                    var i = 0
                    while (i < len) {
                        DataBuf[t + i] = tmp[i]
                        i++
                    }
                    k++
                }
                return if (r >= 0) {
                    0
                } else -1
            }
            1 -> {
                var r = 1
                val n = 8
                val len = nLen / n
                val tmp = ByteArray(len)
                var k = 0
                while (k < n) {
                    r = LibUSBGetData(tmp, len, TIME_OUT)
                    val t = len * k
                    var i = 0
                    while (i < len) {
                        DataBuf[t + i] = tmp[i]
                        i++
                    }
                    k++
                }
                return if (r >= 0) 0 else -1
            }
            2 -> {

                ///*
                var r = 1
                val n = 2
                val len = nLen / n
                val tmp = ByteArray(len)
                var k = 0
                while (k < n) {
                    r = LibUSBGetData(tmp, len, TIME_OUT * 5)
                    val t = len * k
                    var i = 0
                    while (i < len) {
                        DataBuf[t + i] = tmp[i]
                        i++
                    }
                    k++
                }
                return if (r >= 0) 0 else -1
            }
        }
        return -1
    }

    private fun LibUSBDownImage(DataBuf: ByteArray, nLen: Int): Int {
        when (devtype) {
            0 -> {
                var nRet = 0
                val buf = ByteArray(10)
                val len = nLen / 4
                val tmp = ByteArray(len)
                val r = usb_controlmsg(Usb_Request_Type0 or Usb_Request_Type4 or Usb_Request_Type1, 1, nLen, 0, buf, 10, TIME_OUT)
                run {
                    var i = 0
                    while (i < len) {
                        tmp[i] = DataBuf[i]
                        i++
                    }
                }
                nRet = usb_write_device(tmp, len, TIME_OUT) //8000
                if (nRet != 0) return nRet
                run {
                    var i = 0
                    while (i < len) {
                        tmp[i] = DataBuf[i + len]
                        i++
                    }
                }
                nRet = usb_write_device(tmp, len, TIME_OUT) //8000
                if (nRet != 0) return nRet
                run {
                    var i = 0
                    while (i < len) {
                        tmp[i] = DataBuf[i + len * 2]
                        i++
                    }
                }
                nRet = usb_write_device(tmp, len, TIME_OUT) //8000
                if (nRet != 0) return nRet
                var i = 0
                while (i < len) {
                    tmp[i] = DataBuf[i + len * 3]
                    i++
                }
                nRet = usb_write_device(tmp, len, TIME_OUT) //8000
                return nRet
            }
            1 -> {
                return LibUSBDownData(DataBuf, nLen)
            }
            2 -> {
                return LibUSBDownData(DataBuf, nLen)
            }
        }
        return -1
    }

    /////////////////////////////////////////////////////////////////////////////////
    private fun EnCode(nAddr: Int, pSource: ByteArray, iSourceLength: Int, pDestination: ByteArray, iDestinationLength: IntArray): Boolean {
        var i: Int
        var n: Int
        iDestinationLength[0] = 0
        if (iSourceLength > MAX_PACKAGE_SIZE - 4) //��ȥ��ͷ��У���
            return false
        pDestination[0] = 0xEF.toByte()
        pDestination[1] = 0x01.toByte()
        pDestination[2] = (nAddr shr 24 and 0xff).toByte()
        pDestination[3] = (nAddr shr 16 and 0xff).toByte()
        pDestination[4] = (nAddr shr 8 and 0xff).toByte()
        pDestination[5] = (nAddr and 0xff).toByte()
        i = 0
        n = 6
        var ChkSum = 0
        i = 0
        while (i < iSourceLength - 2) {
            ChkSum += pSource[i]
            pDestination[n++] = pSource[i]
            i++
        }
        val ValH: Int
        val ValL: Int
        ValL = ChkSum and 0xff
        ValH = ChkSum shr 8 and 0xff
        pDestination[n++] = ValH.toByte()
        pDestination[n++] = ValL.toByte()
        iDestinationLength[0] = iSourceLength + 6
        return true
    }

    private fun DeCode(pSource: ByteArray, iSourceLength: Int, pDestination: ByteArray, iDestinationLength: IntArray): Boolean {
        iDestinationLength[0] = 0
        val tag1 = if (pSource[0] >= 0) pSource[0].toInt() else pSource[0] + 256
        val tag2 = if (pSource[1] >= 0) pSource[1].toInt() else pSource[1] + 256
        //if(pSource[0]!=0xEF || pSource[1]!=0x01) //byte�������з��ϵ�
        if (tag1 != 0xEF || tag2 != 0x01) {
            return false
        }
        val hi = if (pSource[7] >= 0) pSource[7].toInt() else pSource[7] + 256
        val lo = if (pSource[8] >= 0) pSource[8].toInt() else pSource[8] + 256
        //int nLen=((pSource[7])<<8)+(pSource[8])+1;
        val nLen = (hi shl 8 and 0xff00) + lo + 1
        for (i in 0 until nLen) pDestination[i] = pSource[i + 6]
        iDestinationLength[0] = nLen
        return true
    }

    private fun GetPackage(pData: ByteArray, nLen: Int, nTimeOut: Int): Boolean {
        val recvBuf = ByteArray(1024)
        val nDecodedLen = IntArray(1)
        if (nTimeOut != 0) {
            if (LibUSBGetData(recvBuf, nLen, nTimeOut) != 0) return false
        } else {
            if (LibUSBGetData(recvBuf, nLen, TIME_OUT) != 0) return false
        }

        //����
        return DeCode(recvBuf, nLen, pData, nDecodedLen)
    }

    private fun GetPackageLength(pData: ByteArray): Int {
        // |  ����ʶ   |   ������	  |   ...{����}     |  У���    |
        // |  1 byte   |     2 bytes  |	  ...{������}   |  2 bytes   |
        var length = 0
        length = pData[1] * 256 + pData[2] + 1 + 2
        return length
    }

    private fun GetPackageContentLength(pData: ByteArray): Int {
        var length = 0
        length = pData[1] * 256 + pData[2]
        return length
    }

    private fun SendPackage(nAddr: Int, pData: ByteArray): Boolean {
        val iLength: Int
        val iEncodedLength = IntArray(1)
        val encodedBuf = ByteArray(MAX_PACKAGE_SIZE + 20)
        val bSuccess = false
        iLength = GetPackageLength(pData) //�õ�������
        if (iLength > MAX_PACKAGE_SIZE) return false

        //����
        if (!EnCode(nAddr, pData, iLength, encodedBuf, iEncodedLength)) return false
        if (iEncodedLength[0] > MAX_PACKAGE_SIZE) //������������
            return false
        return LibUSBDownData(encodedBuf, iEncodedLength[0]) == 0
    }

    private fun FillPackage(pData: ByteArray, nPackageType: Int, nLength: Int, pContent: ByteArray): Int {
        // |  ����ʶ    |   ������	  |   ...{����}     |  
        // |  1 byte    |   2 bytes   |	  ...{������}   | 
        var nLength = nLength
        var totalLen = 0
        val checksum: Long = 0
        if (nLength < 0 || nLength > MAX_PACKAGE_SIZE) return 0
        if (nPackageType != CMD.toInt() && nPackageType != DATA.toInt() && nPackageType != ENDDATA.toInt()) return 0
        nLength += 2
        pData[0] = nPackageType.toByte() //������
        pData[1] = (nLength shr 8 and 0xff).toByte() //�����ݳ���
        pData[2] = (nLength and 0xff).toByte() //�����ݳ���
        if (nLength > 0) {
            for (i in 0 until nLength) {
                pData[3 + i] = pContent[i]
            }
            //memcpy(pData+3, pContent, nLength);
        }
        totalLen = nLength + 3
        return totalLen
    }

    private fun VerifyResponsePackage(nPackageType: Byte, pData: ByteArray): Int {
        val checkSum: Long = 0
        if (pData[0] != nPackageType) return -3
        val iLength: Int
        iLength = GetPackageLength(pData) //�õ�������
        return if (nPackageType == RESPONSE) pData[3].toInt() else 0 //ȷ����
    }

    private fun memset(pbuf: ByteArray, size: Int) {
        for (i in 0 until size) {
            pbuf[i] = 0
        }
    }

    private fun memcpy(dstbuf: ByteArray, dstoffset: Int, srcbuf: ByteArray, srcoffset: Int, size: Int) {
        for (i in 0 until size) {
            dstbuf[dstoffset + i] = srcbuf[srcoffset + i]
        }
    }

    private fun memcmp(dstbuf: ByteArray, srcbuf: ByteArray, size: Int): Int {
        for (i in 0 until size) {
            if (dstbuf[i] != srcbuf[i]) return -1
        }
        return 0
    }

    fun FPImageToBitmap(imgType: Int, pImageData: ByteArray, pBitmap: ByteArray): Int {
        memset(pBitmap, 1078)
        pBitmap[0] = 0x42
        pBitmap[1] = 0x4d
        pBitmap[10] = 0x36
        pBitmap[11] = 0x04
        pBitmap[14] = 0x28
        pBitmap[26] = 0x01
        pBitmap[28] = 0x08
        pBitmap[18] = (IMAGE_X shr 0 and 0xFF).toByte()
        pBitmap[19] = (IMAGE_X shr 8 and 0xFF).toByte()
        pBitmap[20] = (IMAGE_X shr 16 and 0xFF).toByte()
        pBitmap[21] = (IMAGE_X shr 24 and 0xFF).toByte()
        pBitmap[22] = (IMAGE_Y shr 0 and 0xFF).toByte()
        pBitmap[23] = (IMAGE_Y shr 8 and 0xFF).toByte()
        pBitmap[24] = (IMAGE_Y shr 16 and 0xFF).toByte()
        pBitmap[25] = (IMAGE_Y shr 24 and 0xFF).toByte()
        var j = 0
        var i = 54
        while (i < 1078) {
            pBitmap[i] = j.toByte()
            pBitmap[i + 1] = j.toByte()
            pBitmap[i + 2] = j.toByte()
            pBitmap[i + 3] = 0
            j++
            i = i + 4
        }
        bmpsize[0] = 74806
        //memcpy(pBitmap,1078,pImageData,0,IMAGE_X*IMAGE_Y);
        if (imgType == 0) {
            bmpsize[0] = Constants.STDBMP_SIZE
            memcpy(pBitmap, 1078, pImageData, 0, Constants.STDIMAGE_X * Constants.STDIMAGE_Y)
        } else {
            bmpsize[0] = Constants.RESBMP_SIZE
            memcpy(pBitmap, 1078, pImageData, 0, Constants.RESIMAGE_X * Constants.RESIMAGE_Y)
        }
        return 0
    }

    /////////////////////////////////////////////////////////////////////////////////
    fun FPErr2Str(nErrCode: Int): String {
        val sErrorString: String
        sErrorString = when (nErrCode) {
            -1 -> "�������ݰ�ʧ��"
            -2 -> "�������ݰ�ʧ��"
            -3 -> "У��ʹ�"
            -4 -> "�������ų�����Χ��flashģ�����Ч"
            -5 -> "��ȫ�ȼ���Ч"
            -6 -> "����ָ���ļ�ʧ��"
            -7 -> "ָ���ļ�������"
            -8 -> "�ļ���С���Ϸ�"
            -9 -> "�ڴ����ʧ��"
            0 -> "ִ�гɹ�"
            1 -> "���ݰ����մ���"
            2 -> "��������û����ָ"
            3 -> "¼��ָ��ͼ��ʧ��"
            4 -> "ָ��̫��"
            5 -> "ָ��̫��"
            6 -> "ָ��̫��"
            7 -> "ָ��������̫��"
            8 -> "ָ�Ʋ�ƥ��"
            9 -> "û������ָ��"
            10 -> "�����ϲ�ʧ��"
            11 -> "��ַ�ų���ָ�ƿⷶΧ"
            12 -> "��ָ�ƿ��ģ�����"
            13 -> "�ϴ�����ʧ��"
            14 -> "ģ�鲻�ܽ��պ������ݰ�"
            15 -> "�ϴ�ͼ��ʧ��"
            16 -> "ɾ��ģ��ʧ��"
            17 -> "���ָ�ƿ�ʧ��"
            18 -> "���ܽ�������"
            19 -> "�����ȷ"
            20 -> "ϵͳ��λʧ��"
            21 -> "��Чָ��ͼ��"
            22 -> "��������ʧ��"
            23 -> "������δ�ƶ�"
            24 -> "��ʾ��дFLASH����"
            25 -> "δ�������"
            26 -> "��Ч�Ĵ�����"
            27 -> "�Ĵ����趨���ݴ����"
            28 -> "���±�ҳ��ָ������"
            29 -> "�˿ڲ���ʧ��"
            30 -> "�Զ�ע�ᣨenroll��ʧ��"
            31 -> "ָ�ƿ���"
            0xf0 -> "�к������ݰ���ָ���ȷ���պ���0xf0Ӧ��"
            0xf1 -> "�к������ݰ���ָ��������0xf1Ӧ��"
            0xf2 -> "��ʾ��д�ڲ�FLASHʱ��У��ʹ���"
            0xf3 -> "��ʾ��д�ڲ�FLASHʱ������ʶ����"
            0xf4 -> "��ʾ��д�ڲ�FLASHʱ�������ȴ���"
            0xf5 -> "��ʾ��д�ڲ�FLASHʱ�����볤��̫��"
            0xf6 -> "��ʾ��д�ڲ�FLASHʱ����дFLASHʧ��"
            0x20 -> "�ղ���"
            else -> "δ֪����"
        }
        return sErrorString
    }

    /////////////////////////////////////////////////////////////////////////////////
    fun SetInstance(parentContext: Context?) {
        pContext = parentContext
    }

    fun SetContextHandler(parentContext: Context?, handler: Handler?) {
        pContext = parentContext
        mHandler = handler
    }

    fun FPOpenDevice(): Int {
        return if (LibUSBOpen()) {
            g_IsOpen = true
            val xpb = ByteArray(4)
            xpb[0] = 0x78
            xpb[1] = 0x70
            xpb[2] = 0x62
            xpb[3] = 0x65
            if (FPVfyDev(-0x1, xpb) == 0) {
                g_IsLink = true
                g_bExit = false
                return 0
            } else {
                xpb[0] = 0x78
                xpb[1] = 0x69
                xpb[2] = 0x61
                xpb[3] = 0x6f
                if (FPVfyDev(-0x1, xpb) == 0) {
                    g_IsLink = true
                    g_bExit = false
                    return 0
                }
            }
            xpb[0] = 0x00
            xpb[1] = 0x00
            xpb[2] = 0x00
            xpb[3] = 0x00
            if (FPVfyDev(-0x1, xpb) == 0) {
                g_IsLink = true
                g_bExit = false
                return 0
            }
            -1
        } else -2
    }

    fun FPCloseDevice(): Int {
        return if (LibUSBClose()) {
            g_IsOpen = false
            g_IsLink = false
            0
        } else -1
    }

    fun FPGetImage(nAddr: Int): Int {
        val num: Int
        val cCmd = ByteArray(10)
        val result: Int
        val iSendData = ByteArray(MAX_PACKAGE_SIZE)
        val iGetData = ByteArray(MAX_PACKAGE_SIZE)
        memset(iSendData, MAX_PACKAGE_SIZE)
        memset(iGetData, MAX_PACKAGE_SIZE)
        cCmd[0] = GET_IMAGE //������
        num = FillPackage(iSendData, CMD.toInt(), 1, cCmd) //������ݰ�
        if (!SendPackage(nAddr, iSendData)) return -1
        if (!GetPackage(iGetData, 64, TIME_OUT)) return -2
        result = VerifyResponsePackage(RESPONSE, iGetData) //У��Ӧ���
        return result
    }

    fun FPGetImageEx(nAddr: Int): Int {
        val num: Int
        val cCmd = ByteArray(10)
        val result: Int
        val iSendData = ByteArray(MAX_PACKAGE_SIZE)
        val iGetData = ByteArray(MAX_PACKAGE_SIZE)
        memset(iSendData, MAX_PACKAGE_SIZE)
        memset(iGetData, MAX_PACKAGE_SIZE)
        cCmd[0] = GET_IMAGEEX //������
        num = FillPackage(iSendData, CMD.toInt(), 1, cCmd) //������ݰ�
        if (!SendPackage(nAddr, iSendData)) return -1
        if (!GetPackage(iGetData, 64, TIME_OUT)) return -2
        result = VerifyResponsePackage(RESPONSE, iGetData) //У��Ӧ���
        return result
    }

    fun FPGenChar(nAddr: Int, iBufferID: Int): Int {
        val cCmd = ByteArray(10)
        val num: Int
        val result: Int
        val iSendData = ByteArray(MAX_PACKAGE_SIZE)
        val iGetData = ByteArray(MAX_PACKAGE_SIZE)
        memset(iSendData, MAX_PACKAGE_SIZE)
        memset(iGetData, MAX_PACKAGE_SIZE)
        cCmd[0] = GEN_CHAR //ָ��
        cCmd[1] = iBufferID.toByte() //��������
        num = FillPackage(iSendData, CMD.toInt(), 2, cCmd) //������ݰ�
        if (!SendPackage(nAddr, iSendData)) return -1
        if (!GetPackage(iGetData, 64, TIME_OUT)) return -2
        result = VerifyResponsePackage(RESPONSE, iGetData) //У��Ӧ���
        return result
    }

    fun FPGenCharEx(nAddr: Int, iBufferID: Int): Int {
        val cCmd = ByteArray(10)
        val num: Int
        val result: Int
        val iSendData = ByteArray(MAX_PACKAGE_SIZE)
        val iGetData = ByteArray(MAX_PACKAGE_SIZE)
        memset(iSendData, MAX_PACKAGE_SIZE)
        memset(iGetData, MAX_PACKAGE_SIZE)
        cCmd[0] = GEN_CHAREX //ָ��
        cCmd[1] = iBufferID.toByte() //��������
        num = FillPackage(iSendData, CMD.toInt(), 2, cCmd) //������ݰ�
        if (!SendPackage(nAddr, iSendData)) return -1
        if (!GetPackage(iGetData, 64, TIME_OUT)) return -2
        result = VerifyResponsePackage(RESPONSE, iGetData) //У��Ӧ���
        return result
    }

    fun FPUpChar(nAddr: Int, iBufferID: Int, pTemplet: ByteArray, iTempletLength: IntArray): Int {
        val num: Int
        val result: Int
        val cContent = ByteArray(10)
        val iSendData = ByteArray(MAX_PACKAGE_SIZE)
        val iGetData = ByteArray(MAX_PACKAGE_SIZE)
        memset(iSendData, MAX_PACKAGE_SIZE)
        memset(iGetData, MAX_PACKAGE_SIZE)
        if (iBufferID < 1 || iBufferID > 3) return -4
        cContent[0] = UP_CHAR //ָ��
        cContent[1] = iBufferID.toByte()
        num = FillPackage(iSendData, CMD.toInt(), 2, cContent) //������ݰ�
        if (!SendPackage(nAddr, iSendData)) return -1
        iTempletLength[0] = 512
        result = LibUSBGetData(pTemplet, 512, TIME_OUT)
        return result
    }

    fun FPDownChar(nAddr: Int, iBufferID: Int, pTemplet: ByteArray, iTempletLength: Int): Int {
        val num: Int
        var result: Int
        val cContent = ByteArray(10)
        val iSendData = ByteArray(MAX_PACKAGE_SIZE)
        val iGetData = ByteArray(MAX_PACKAGE_SIZE)
        memset(iSendData, MAX_PACKAGE_SIZE)
        memset(iGetData, MAX_PACKAGE_SIZE)
        if (iBufferID < 1 || iBufferID > 3) return -4
        cContent[0] = DOWN_CHAR //ָ��
        cContent[1] = iBufferID.toByte()
        num = FillPackage(iSendData, CMD.toInt(), 2, cContent) //������ݰ�
        return if (!SendPackage(nAddr, iSendData)) -1 else LibUSBDownData1(pTemplet, 512)
    }

    fun FPMatch(nAddr: Int, iScore: IntArray): Int {
        val cCmd = ByteArray(10)
        val num: Int
        val result: Int
        val iSendData = ByteArray(MAX_PACKAGE_SIZE)
        val iGetData = ByteArray(MAX_PACKAGE_SIZE)
        memset(iSendData, MAX_PACKAGE_SIZE)
        memset(iGetData, MAX_PACKAGE_SIZE)
        cCmd[0] = MATCH //ָ��
        num = FillPackage(iSendData, CMD.toInt(), 1, cCmd) //������ݰ�
        if (!SendPackage(nAddr, iSendData)) return -1
        if (!GetPackage(iGetData, 64, TIME_OUT)) return -2
        result = VerifyResponsePackage(RESPONSE, iGetData) //У��Ӧ���
        val hi = if (iGetData[HEAD_LENGTH + 1] >= 0) iGetData[HEAD_LENGTH + 1].toInt() else iGetData[HEAD_LENGTH + 1] + 256
        val lo = if (iGetData[HEAD_LENGTH + 2] >= 0) iGetData[HEAD_LENGTH + 2].toInt() else iGetData[HEAD_LENGTH + 2] + 256

        //iScore[0] = iGetData[HEAD_LENGTH+1]<<8;
        //iScore[0] |= iGetData[HEAD_LENGTH+2];
        iScore[0] = (hi shl 8 and 0xff00) + lo
        return result
    }

    fun FPRegModule(nAddr: Int): Int {
        val num: Int
        val result: Int
        val cCmd = ByteArray(10)
        val iSendData = ByteArray(MAX_PACKAGE_SIZE)
        val iGetData = ByteArray(MAX_PACKAGE_SIZE)
        memset(iSendData, MAX_PACKAGE_SIZE)
        memset(iGetData, MAX_PACKAGE_SIZE)
        cCmd[0] = REG_MODULE //ָ��
        num = FillPackage(iSendData, CMD.toInt(), 1, cCmd) //������ݰ�
        if (!SendPackage(nAddr, iSendData)) return -1
        if (!GetPackage(iGetData, 64, TIME_OUT)) return -2
        result = VerifyResponsePackage(RESPONSE, iGetData) //У��Ӧ���
        return result
    }

    fun FPUpImage(nAddr: Int, pImageData: ByteArray, iImageLength: IntArray): Int {
        val num: Int
        var result: Int
        val cCmd = ByteArray(10)
        val iSendData = ByteArray(MAX_PACKAGE_SIZE)
        val iGetData = ByteArray(MAX_PACKAGE_SIZE)
        memset(iSendData, MAX_PACKAGE_SIZE)
        memset(iGetData, MAX_PACKAGE_SIZE)
        cCmd[0] = UP_IMAGE //ָ��
        num = FillPackage(iSendData, CMD.toInt(), 1, cCmd) //������ݰ�
        if (!SendPackage(nAddr, iSendData)) return -1
        iImageLength[0] = IMAGE_X * IMAGE_Y
        return LibUSBGetImage(pImageData, IMAGE_Y * IMAGE_X)
    }

    fun FPUpImageEx(nAddr: Int, pImageData: ByteArray, iImageLength: IntArray): Int {
        val num: Int
        var result: Int
        val cCmd = ByteArray(10)
        val iSendData = ByteArray(MAX_PACKAGE_SIZE)
        val iGetData = ByteArray(MAX_PACKAGE_SIZE)
        memset(iSendData, MAX_PACKAGE_SIZE)
        memset(iGetData, MAX_PACKAGE_SIZE)
        cCmd[0] = UP_IMAGEEX //ָ��
        num = FillPackage(iSendData, CMD.toInt(), 1, cCmd) //������ݰ�
        if (!SendPackage(nAddr, iSendData)) return -1
        iImageLength[0] = Constants.RESIMAGE_X * Constants.RESIMAGE_Y
        return LibUSBGetImage(pImageData, Constants.RESIMAGE_Y * Constants.RESIMAGE_X)
    }

    fun FPSetPwd(nAddr: Int, pPassword: ByteArray): Int {
        val num: Int
        val result: Int
        val cContent = ByteArray(10)
        val iSendData = ByteArray(MAX_PACKAGE_SIZE)
        val iGetData = ByteArray(MAX_PACKAGE_SIZE)
        memset(iSendData, MAX_PACKAGE_SIZE)
        memset(iGetData, MAX_PACKAGE_SIZE)
        cContent[0] = SET_PWD //ָ��
        cContent[1] = pPassword[0]
        cContent[2] = pPassword[1]
        cContent[3] = pPassword[2]
        cContent[4] = pPassword[3]
        num = FillPackage(iSendData, CMD.toInt(), 5, cContent) //������ݰ�
        if (!SendPackage(nAddr, iSendData)) return -1
        if (!GetPackage(iGetData, 64, 1000)) return -2
        result = VerifyResponsePackage(RESPONSE, iGetData) //У��Ӧ���
        return result
    }

    fun FPVfyDev(nAddr: Int, pPassword: ByteArray): Int {
        val num: Int
        val result: Int
        val cContent = ByteArray(10)
        val iSendData = ByteArray(MAX_PACKAGE_SIZE)
        val iGetData = ByteArray(MAX_PACKAGE_SIZE)
        memset(iSendData, MAX_PACKAGE_SIZE)
        memset(iGetData, MAX_PACKAGE_SIZE)
        cContent[0] = VFY_PWD //ָ��
        cContent[1] = pPassword[0]
        cContent[2] = pPassword[1]
        cContent[3] = pPassword[2]
        cContent[4] = pPassword[3]
        num = FillPackage(iSendData, CMD.toInt(), 5, cContent) //������ݰ�
        if (!SendPackage(nAddr, iSendData)) return -1
        if (!GetPackage(iGetData, 64, 1000)) return -2
        result = VerifyResponsePackage(RESPONSE, iGetData) //У��Ӧ���
        return result
    }

    fun FPReadInfo(nAddr: Int, nPage: Int, UserContent: ByteArray): Int {
        val num: Int
        var result = 0
        val cContent = ByteArray(10)
        val iSendData = ByteArray(MAX_PACKAGE_SIZE)
        val iGetData = ByteArray(MAX_PACKAGE_SIZE)
        memset(iSendData, MAX_PACKAGE_SIZE)
        memset(iGetData, MAX_PACKAGE_SIZE)
        cContent[0] = READ_NOTEPAD
        cContent[1] = nPage.toByte()
        num = FillPackage(iSendData, CMD.toInt(), 2, cContent) //������ݰ�
        if (!SendPackage(nAddr, iSendData)) return -1
        if (!GetPackage(iGetData, 64, TIME_OUT)) return -2
        result = VerifyResponsePackage(RESPONSE, iGetData) //У��Ӧ���
        if (result != 0) return result
        memcpy(UserContent, 0, iGetData, HEAD_LENGTH + 1, 32)
        return result
    }

    fun FPOpenDoor(nAddr: Int, iSwitch: Int): Int {
        val cCmd = ByteArray(10)
        val num: Int
        val result: Int
        val iSendData = ByteArray(MAX_PACKAGE_SIZE)
        val iGetData = ByteArray(MAX_PACKAGE_SIZE)
        memset(iSendData, MAX_PACKAGE_SIZE)
        memset(iGetData, MAX_PACKAGE_SIZE)
        cCmd[0] = OPENDOOR //ָ��
        cCmd[1] = iSwitch.toByte() //��������
        num = FillPackage(iSendData, CMD.toInt(), 2, cCmd) //������ݰ�
        if (!SendPackage(nAddr, iSendData)) return -1
        if (!GetPackage(iGetData, 64, TIME_OUT)) return -2
        result = VerifyResponsePackage(RESPONSE, iGetData) //У��Ӧ���
        return result
    }

    fun FPReadCardSn(nAddr: Int, cardsn: ByteArray): Int {
        val cCmd = ByteArray(10)
        val num: Int
        val result: Int
        val iSendData = ByteArray(MAX_PACKAGE_SIZE)
        val iGetData = ByteArray(MAX_PACKAGE_SIZE)
        memset(iSendData, MAX_PACKAGE_SIZE)
        memset(iGetData, MAX_PACKAGE_SIZE)
        cCmd[0] = READCARDSN //ָ��
        num = FillPackage(iSendData, CMD.toInt(), 1, cCmd) //������ݰ�
        if (!SendPackage(nAddr, iSendData)) return -1
        if (!GetPackage(iGetData, 64, TIME_OUT)) return -2
        result = VerifyResponsePackage(RESPONSE, iGetData) //У��Ӧ���
        if (result != 0) return result
        memcpy(cardsn, 0, iGetData, HEAD_LENGTH + 1, 5)
        return result
    }

    fun FxGetImage(imgType: Int, nAddr: Int): Int {
        return if (imgType == 0) {
            FPGetImage(nAddr)
        } else {
            FPGetImageEx(nAddr)
        }
    }

    fun FxGenChar(imgType: Int, nAddr: Int, iBufferID: Int): Int {
        return if (imgType == 0) {
            FPGenChar(-0x1, iBufferID)
        } else {
            FPGenCharEx(-0x1, iBufferID)
        }
    }

    fun FxUpImage(imgType: Int, nAddr: Int, pImageData: ByteArray, iImageLength: IntArray): Int {
        return if (imgType == 0) {
            FPUpImage(nAddr, pImageData, iImageLength)
        } else {
            FPUpImageEx(nAddr, pImageData, iImageLength)
        }
    }

    fun OpenDevice(): Int {
        return FPOpenDevice()
    }

    fun CloseDevice(): Int {
        return FPCloseDevice()
    }

    fun MatchTemplate(ptep1dat: ByteArray, ptep1size: Int, ptep2dat: ByteArray, ptep2size: Int): Int {
        val matval = IntArray(1)
        if (FPDownChar(-0x1, 1, ptep1dat, ptep1size) == 0) {
            if (FPDownChar(-0x1, 2, ptep2dat, ptep2size) == 0) {
                val ret = FPMatch(-0x1, matval)
                return if (ret == 0) matval[0] else 0
            }
        }
        return 0
    } //public native void GetLinkInfo(int itype,byte[] info);   

    //static {
    //   System.loadLibrary("fgtitinit");
    //}
    companion object {
        const val ACTION_USB_PERMISSION = "com.example.fpdemo.USB"
        private const val TIME_OUT = 10000
        private const val EP_IN = 0x81
        private const val EP_OUT = 0x02
        private const val Usb_Request_Type0 = 0x80
        private const val Usb_Request_Type1 = 0x00
        private const val Usb_Request_Type2 = 0x00
        private const val Usb_Request_Type3 = 0x20
        private const val Usb_Request_Type4 = 0x40
        private const val Usb_Request_Type5 = 0x60
        private const val Usb_Request_Type6 = 0x00
        private const val Usb_Request_Type7 = 0x01
        private const val Usb_Request_Type8 = 0x02
        private const val Usb_Request_Type9 = 0x03
        private const val MAX_PACKAGE_SIZE = 350 //���ݰ���󳤶�
        private const val HEAD_LENGTH = 3 //��ͷ����
        private const val CMD: Byte = 0x01 //�����
        private const val DATA: Byte = 0x02 //���ݰ�
        private const val ENDDATA: Byte = 0x08 //���һ�����ݰ�
        private const val RESPONSE: Byte = 0x07 //Ӧ���
        private const val IMAGE_X = 256
        private const val IMAGE_Y = 288
        private const val GET_IMAGE: Byte = 0x01 //��ȡͼ��
        private const val GEN_CHAR: Byte = 0x02 //����ԭʼͼ������ָ����������CharBuffer1��CharBuffer2
        private const val MATCH: Byte = 0x03 //��ȷ�ȶ�CharBuffer1��CharBuffer2�е������ļ�
        private const val SEARCH: Byte = 0x04 //��CharBuffer1��CharBuffer2�е������ļ����������򲿷�ָ�ƿ�
        private const val REG_MODULE: Byte = 0x05 //��CharBuffer1��CharBuffer2�е������ļ��ϲ�����ģ�����CharBuffer2
        private const val STORE_CHAR: Byte = 0x06 //�������������е��ļ����浽flashָ�ƿ���
        private const val LOAD_CHAR: Byte = 0x07 //��flashָ�ƿ��ж�ȡһ��ģ�嵽����������
        private const val UP_CHAR: Byte = 0x08 //�������������е��ļ��ϴ�����λ��
        private const val DOWN_CHAR: Byte = 0x09 //����λ������һ�������ļ�������������
        private const val UP_IMAGE: Byte = 0x0a //�ϴ�ԭʼͼ��
        private const val DOWN_IMAGE: Byte = 0x0b //����ԭʼͼ��
        private const val DEL_CHAR: Byte = 0x0c //ɾ��flashָ�ƿ��е�һ�������ļ�
        private const val EMPTY: Byte = 0x0d //���flashָ�ƿ�
        private const val WRITE_REG: Byte = 0x0e //дģ��Ĵ���ָ��
        private const val READ_PAR_TABLE: Byte = 0x0f //��ϵͳ������
        private const val ENROLL: Byte = 0x10 //�͹���ָ��
        private const val IDENTIFY: Byte = 0x11 //ϵͳ��ʼ��ָ��Դ�����ƫ�����Ƚ��м���¼
        private const val SET_PWD: Byte = 0x12 //�����豸���ֿ���
        private const val VFY_PWD: Byte = 0x13 //��֤�豸���ֿ���
        private const val GET_RANDOM: Byte = 0x14 //��ȡ�����_
        private const val SET_CHIPADDR: Byte = 0x15 //����оƬ��ַ
        private const val READ_INFPAGE: Byte = 0x16 //���ɣΣƣ�ҳ
        private const val PORT_CONTROL: Byte = 0x17 //ϵͳ��λ�������ϵ��ʼ״̬
        private const val WRITE_NOTEPAD: Byte = 0x18 //д���±�
        private const val READ_NOTEPAD: Byte = 0x19 //�����±�         
        private const val BURN_CODE: Byte = 0x1a //��д����
        private const val HIGH_SPEED_SEARCH: Byte = 0x1b //��������
        private const val GEN_BINIMAGE: Byte = 0x1c //����ϸ��ͼ
        private const val TEMPLATE_NUM: Byte = 0x1d
        private const val USERDEFINE: Byte = 0x1e //�û��Զ�������
        private const val READ_INDEXTABLE: Byte = 0x1f //��ȡģ��������
        private const val GETIMAGEX: Byte = 0x30 //��ȡԭʼͼ��
        private const val UPIMAGEX: Byte = 0x31 //�ϴ�ԭʼͼ��
        private const val GENCHAREX: Byte = 0x32
        private const val CALIBRATESENSOR: Byte = 0x40 //�궨RT1020ָ����оƬ
        private const val READCARDSN: Byte = 0x50 //������
        private const val OPENDOOR: Byte = 0x54 //�̵���
        private const val REG_BAUD: Byte = 4 //�����ʼĴ���
        private const val REG_SECURE_LEVEL: Byte = 5 //��ȫ�ȼ��Ĵ��� 
        private const val REG_PACKETSIZE: Byte = 6 //���ݰ���С�Ĵ���
        private const val GET_IMAGEEX: Byte = 0x30 //��ȡͼ��
        private const val UP_IMAGEEX: Byte = 0x31 //�ϴ�ͼ��256*360
        private const val GEN_CHAREX: Byte = 0x32
        const val FPM_DEVICE = 0x01 //�豸
        const val FPM_PLACE = 0x02 //�밴��ָ
        const val FPM_LIFT = 0x03 //��̧����ָ
        const val FPM_CAPTURE = 0x04 //�ɼ�ͼ�����
        const val FPM_GENCHAR = 0x05 //�ɼ�������
        const val FPM_ENRFPT = 0x06 //�Ǽ�ָ��
        const val FPM_NEWIMAGE = 0x07 //�µ�ָ��ͼ��
        const val FPM_TIMEOUT = 0x08
        const val FPM_IMGVAL = 0x09
        const val RET_OK = 1
        const val RET_FAIL = 0
        private const val NCM_IMAGE = 0x01 //�ɼ�ͼ��
        private const val NCM_ENROL = 0x02 //�Ǽ�ָ��ģ��
        private const val NCM_GENCHAR = 0x03 //�ɼ�������
    }
}