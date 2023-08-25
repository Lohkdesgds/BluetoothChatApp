package com.lsw.myapplication.ui

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.nio.charset.Charset
import java.util.UUID

// based on https://stackoverflow.com/questions/13450406/how-to-receive-serial-data-using-android-bluetooth

@SuppressLint("MissingPermission")
class BluetoothConnectedDevice (
    val device: BluetoothDevice,
    val cb_new_msg: (String, String) -> Unit
){
    companion object {
        private const val delimiter: Byte = 10 //This is the ASCII code for a newline character
    }

    private lateinit var socket: BluetoothSocket
    private lateinit var m_out: OutputStream
    private lateinit var m_in: InputStream
    private lateinit var worker_thread: Thread
    private lateinit var readBuffer: ByteArray
    private var readBufferPosition: Int = 0
    @Volatile
    private var stopWorker = false

    init {
        try {
            val uuid =
                UUID.fromString("00001101-0000-1000-8000-00805F9B34FB") //Standard SerialPortService ID

            socket = device.createRfcommSocketToServiceRecord(uuid)

            socket.connect()
            m_out = socket.getOutputStream()
            m_in = socket.getInputStream()

            stopWorker = false
            readBufferPosition = 0
            readBuffer = ByteArray(1024)

            worker_thread = Thread {
                while (!Thread.currentThread().isInterrupted && !stopWorker) {
                    try {
                        val bytesAvailable: Int = m_in.available()
                        if (bytesAvailable > 0) {
                            val packetBytes = ByteArray(bytesAvailable)
                            m_in.read(packetBytes)
                            for (i in 0 until bytesAvailable) {
                                val b = packetBytes[i]
                                if (b == Companion.delimiter) {
                                    val encodedBytes = ByteArray(readBufferPosition)
                                    System.arraycopy(
                                        readBuffer,
                                        0,
                                        encodedBytes,
                                        0,
                                        encodedBytes.size
                                    )
                                    val data = encodedBytes.toString(Charset.forName("US-ASCII"))
                                    readBufferPosition = 0

                                    cb_new_msg(
                                        data.substring(0, data.indexOf('\n')),
                                        data.substring(data.indexOf('\n') + 1)
                                    )
                                    //handler.post(Runnable { myLabel.setText(data) })
                                } else {
                                    readBuffer.set(readBufferPosition++, b)
                                }
                            }
                        }
                    } catch (ex: IOException) {
                        stopWorker = true
                    }
                }
            }

            worker_thread.start()
        }
        catch(err: IOException) { // happens to cause: java.io.IOException: read failed, socket might closed orr timeout, read ret -1
            cb_new_msg("BT DEBUG EXCEPTION", "IOException:\n$err");
        }
        catch(err : Exception) {
            cb_new_msg("BT DEBUG EXCEPTION", "Exception:\n$err");
        }
    }

    fun sendData(author: String, message: String) {
        m_out.write((author + "\n" + message).toByteArray())
    }

    fun closeBT() {
        stopWorker = true
        m_out.close()
        m_in.close()
        socket.close()
    }
}