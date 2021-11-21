/*
 * Copyright 2009 Cedric Priscal
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License. 
 */
package android_serialport_api

import java.io.FileDescriptor
import java.io.FileInputStream
import java.io.FileOutputStream
import java.lang.SecurityException
import java.io.IOException
import java.io.File
import java.lang.Exception
import android_serialport_api.SerialPort
import java.io.InputStream
import java.io.OutputStream
import android.os.SystemClock
import android.util.Log

class SerialPort {
    /*
	 * Do not remove or rename the field mFd: it is used by native method close();
	 */
    private var mFd: FileDescriptor? = null
    private var mFileInputStream: FileInputStream? = null
    private var mFileOutputStream: FileOutputStream? = null
    @Throws(SecurityException::class, IOException::class)
    fun OpenDevice(device: File, speed: Int, mode: Int, devtype: Int): Boolean {
        if (!device.canRead() || !device.canWrite()) {
            try {
                /* Missing read/write permission, trying to chmod the file */
                val su: Process
                su = Runtime.getRuntime().exec("/system/bin/su")
                val cmd = """
                    chmod 666 ${device.absolutePath}
                    exit
                    
                    """.trimIndent()
                su.outputStream.write(cmd.toByteArray())
                if (su.waitFor() != 0 || !device.canRead()
                        || !device.canWrite()) {
                    throw SecurityException()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                throw SecurityException()
            }
        }
        mFd = open(device.absolutePath, speed, mode, devtype)
        if (mFd == null) {
            Log.e(TAG, "native open returns null")
            throw IOException()
        }
        mFileInputStream = FileInputStream(mFd)
        mFileOutputStream = FileOutputStream(mFd)
        return true
    }

    fun CloseDevice() {
        close()
    }

    // Getters and setters
    val inputStream: InputStream?
        get() = mFileInputStream
    val outputStream: OutputStream?
        get() = mFileOutputStream

    fun PowerSwitch(sw: Boolean) {
        if (sw) {
            Log.i("xpb", "SPI Power ON")
            powercontrl(1)
            SystemClock.sleep(50)
            resetcontrol(0)
            SystemClock.sleep(200)
            resetcontrol(1)
            SystemClock.sleep(200)
        } else {
            Log.i("xpb", "SPI Power Off")
            resetcontrol(0)
            powercontrl(0)
        }
    }

    external fun close()
    external fun getmodel(): String?
    external fun powercontrl(`val`: Int)
    external fun resetcontrol(`val`: Int)

    companion object {
        private const val TAG = "SerialPort"

        // JNI
        private external fun open(path: String, speed: Int, mode: Int, devtype: Int): FileDescriptor?

        init {
            System.loadLibrary("serialport")
        }
    }
}