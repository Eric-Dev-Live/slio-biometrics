package com.sliosecure.slio_biometrics.data

import com.sliosecure.slio_biometrics.data.wsq
import java.io.File
import java.io.RandomAccessFile
import java.io.IOException

class wsq {
    external fun RawToWsq(inpdata: ByteArray?, inpsize: Int, width: Int, height: Int, outdata: ByteArray?, outsize: IntArray?, bitrate: Float): Int
    external fun WsqToRaw(inpdata: ByteArray?, inpsize: Int, outdata: ByteArray?, outsize: IntArray?): Int

    companion object {
        private var mCom: wsq? = null
        val instance: wsq?
            get() {
                if (mCom == null) {
                    mCom = wsq()
                }
                return mCom
            }

        init {
            System.loadLibrary("wsq")
        }
    }

    fun SaveWsqFile(rawdata: ByteArray?, rawsize: Int, filename: String) {
        val outdata = ByteArray(73728)
        val outsize = IntArray(1)
        instance!!.RawToWsq(rawdata, rawsize, 256, 288, outdata, outsize, 2.833755f)
        try {
            val fs = File("/sdcard/$filename")
            if (fs.exists()) {
                fs.delete()
            }
            File("/sdcard/$filename")
            val randomFile = RandomAccessFile("/sdcard/$filename", "rw")
            val fileLength = randomFile.length()
            randomFile.seek(fileLength)
            randomFile.write(outdata, 0, outsize[0])
            randomFile.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }
}