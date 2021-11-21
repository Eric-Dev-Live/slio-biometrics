package com.sliosecure.slio_biometrics.data

import android.util.Base64
import com.sliosecure.slio_biometrics.data.Conversions

class Conversions {
    external fun StdToIso(itype: Int, input: ByteArray?, output: ByteArray?): Int
    external fun IsoToStd(itype: Int, input: ByteArray?, output: ByteArray?): Int
    external fun GetDataType(input: ByteArray?): Int
    external fun StdChangeCoord(input: ByteArray?, size: Int, output: ByteArray?, dk: Int): Int
    private fun IsoChangeCoord(input: ByteArray, dk: Int): String {
        val dt = GetDataType(input)
        if (dt == 3) {
            val output = ByteArray(512)
            val stddat = ByteArray(512)
            val crddat = ByteArray(512)
            IsoToStd(2, input, stddat)
            StdChangeCoord(stddat, 256, crddat, dk)
            StdToIso(2, crddat, output)
            return Base64.encodeToString(output, 0, 378, Base64.DEFAULT)
        }
        return ""
    }

    private fun IsoChangeOrientation(input: ByteArray?, dk: Int): String {
        val dt = GetDataType(input)
        if (dt == 3) {
            val output = ByteArray(512)
            val stddat = ByteArray(512)
            val crddat = ByteArray(512)
            IsoToStd(2, input, stddat)
            StdChangeCoord(stddat, 256, crddat, dk)
            StdToIso(2, crddat, output)
            return Base64.encodeToString(output, 0, 378, Base64.DEFAULT)
        }
        return ""
    }

    fun ToIso(input: ByteArray?, dk: Int): String {
        when (GetDataType(input)) {
            1 -> {
                val mTmpData = ByteArray(512)
                val mIsoData = ByteArray(512)
                StdChangeCoord(input, 256, mTmpData, dk)
                StdToIso(2, mTmpData, mIsoData)
                return Base64.encodeToString(mIsoData, 0, 378, Base64.DEFAULT)
            }
            2 -> {
                val mTmpData1 = ByteArray(512)
                val mTmpData2 = ByteArray(512)
                val mIsoData = ByteArray(512)
                IsoToStd(1, input, mTmpData1)
                StdChangeCoord(mTmpData1, 256, mTmpData2, dk)
                StdToIso(2, mTmpData2, mIsoData)
                return Base64.encodeToString(mIsoData, 0, 378, Base64.DEFAULT)
            }
            3 -> return IsoChangeOrientation(input, dk)
        }
        return ""
    }

    fun ToStd(input: ByteArray?, dk: Int): String {
        when (GetDataType(input)) {
            1 -> {
                val mTmpData = ByteArray(512)
                StdChangeCoord(input, 256, mTmpData, dk)
                return Base64.encodeToString(mTmpData, 0, 256, Base64.DEFAULT)
            }
            2 -> {
                val mTmpData1 = ByteArray(512)
                val mTmpData2 = ByteArray(512)
                IsoToStd(1, input, mTmpData1)
                StdChangeCoord(mTmpData1, 256, mTmpData2, dk)
                return Base64.encodeToString(mTmpData2, 0, 256, Base64.DEFAULT)
            }
            3 -> {
                val mTmpData1 = ByteArray(512)
                val mTmpData2 = ByteArray(512)
                IsoToStd(2, input, mTmpData1)
                StdChangeCoord(mTmpData1, 256, mTmpData2, dk)
                return Base64.encodeToString(mTmpData2, 0, 256, Base64.DEFAULT)
            }
        }
        return ""
    }

    companion object {
        private var mCom: Conversions? = null
        val instance: Conversions?
            get() {
                if (mCom == null) {
                    mCom = Conversions()
                }
                return mCom
            }

        init {
            System.loadLibrary("conversions")
        }
    }
}