package android.fpi

class GpioControl {
    // gpio
    external fun openGpioDev(): Int
    external fun closeGpioDev(): Int
    external fun setGpioMode(pin: Int, mode: Int): Int
    external fun setGpioDir(pin: Int, dir: Int): Int
    external fun setGpioPullEnable(pin: Int, enable: Int): Int
    external fun setGpioPullSelect(pin: Int, select: Int): Int
    external fun setGpioOut(pin: Int, out: Int): Int
    external fun getGpioIn(pin: Int): Int

    //serialport  
    external fun openCom(port: String?, baudrate: Int, bits: Int, event: Char, stop: Int): Int
    external fun openComEx(port: String?, baudrate: Int, bits: Int, event: Char, stop: Int, flags: Int): Int
    external fun writeCom(fd: Int, buf: ByteArray?, sizes: Int): Int
    external fun readCom(fd: Int, buf: ByteArray?, sizes: Int): Int
    external fun readComEx(fd: Int, buf: ByteArray?, sizes: Int, sec: Int, usec: Int): Int
    external fun closeCom(fd: Int)

    companion object {
        init {
            System.loadLibrary("fpgpio")
        }
    }

    init {
        openGpioDev()
    }
}