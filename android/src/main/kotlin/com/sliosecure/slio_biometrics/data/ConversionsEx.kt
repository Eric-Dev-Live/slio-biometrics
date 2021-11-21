package com.sliosecure.slio_biometrics.data

import android.util.Base64
import com.sliosecure.slio_biometrics.data.ConversionsEx

class ConversionsEx {
    external fun StdToAnsiIso(input: ByteArray?, output: ByteArray?, size: Int, imgw: Int, imgh: Int, resx: Int, resy: Int, type: Int): Int
    external fun AnsiIsoToStd(input: ByteArray?, output: ByteArray?, type: Int): Int
    external fun StdChangeCoord(input: ByteArray?, size: Int, output: ByteArray?, dk: Int): Int
    external fun GetDataType(input: ByteArray?): Int
    fun ToAnsiIso(input: ByteArray?, type: Int, dk: Int): String {
        val dt = GetDataType(input)
        if (dt == STD_TEMPLATE) {
            val output = ByteArray(512)
            val tmpdat = ByteArray(512)
            StdChangeCoord(input, 256, tmpdat, dk)
            //if(StdToAnsiIso(tmpdat,output,378,256,288,199,199,type)>0)
            return if (StdToAnsiIso(tmpdat, output, 378, 260, 300, 199, 199, type) > 0) {
                Base64.encodeToString(output, 0, 378, Base64.DEFAULT)
            } else ""
        }
        return ""
    }

    fun To_Ansi378_2004(input: ByteArray?): String {
        return ToAnsiIso(input, ANSI_378_2004, COORD_MIRRORV)
    }

    fun To_Iso19794_2005(input: ByteArray?): String {
        return ToAnsiIso(input, ISO_19794_2005, COORD_MIRRORV)
    }

    fun To_Iso19794_2009(input: ByteArray?): String {
        return ToAnsiIso(input, ISO_19794_2009, COORD_MIRRORV)
    }

    fun To_Iso19794_2011(input: ByteArray?): String {
        return ToAnsiIso(input, ISO_19794_2011, COORD_MIRRORV)
    }

    companion object {
        var STD_TEMPLATE = 0
        var ANSI_378_2004 = 1
        var ISO_19794_2005 = 2
        var ISO_19794_2009 = 3
        var ISO_19794_2011 = 4
        var COORD_NOTCHANGE = 0
        var COORD_MIRRORV = 1
        var COORD_MIRRORH = 2
        var COORD_ROTAING = 3
        private var mCom: ConversionsEx? = null
        val instance: ConversionsEx?
            get() {
                if (mCom == null) {
                    mCom = ConversionsEx()
                }
                return mCom
            }

        init {
            System.loadLibrary("conversionsex")
        }
    }
}