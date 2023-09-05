@file:OptIn(ExperimentalComposeUiApi::class)

package com.lsw.myapplication


import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.ExperimentalComposeUiApi
import com.lsw.myapplication.ui.MBluetoothWrapper
import com.lsw.myapplication.ui.theme.MyApplicationTheme
import java.util.concurrent.Executors

class MainActivity : ComponentActivity() {
    //val bt = BluetoothWrapper()
    val m_display: MutableState<MDisplay> = mutableStateOf(MDisplay(::inputCmdHandler))
    lateinit var m_btwrap: MBluetoothWrapper
    val m_exec = Executors.newFixedThreadPool(2)
    var m_waiting_for_bt: Boolean = false


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestAllPermissions(this)

        setContent {
            MyApplicationTheme {
                //CombinedDraw()
                m_display.value.draw(/*LocalDensity.current*/)
            }
        }
    }

    private fun inputCmdHandler(inn: String) : Boolean
    {
        return when {
            inn.startsWith("/listbt") -> {
                m_exec.execute( Runnable { m_display.value.post(ath = "SYSTEM", msg = listBluetoothToString(), true) } )
                true
            }

            inn.startsWith("/connect") -> {
                m_exec.execute( Runnable { m_display.value.post(ath = "SYSTEM", msg = "WIP", true) } )
                true
            }

            inn.startsWith("/") -> {
                m_exec.execute( Runnable { m_display.value.post(ath = "SYSTEM", msg = "Unrecognized command. Please try /listbt or /connect", true) } )
                true
            }

            else -> {
                false
            }
        }
    }

    @Suppress("DEPRECATION")
    @SuppressLint("MissingPermission")
    private fun listBluetoothToString() : String {
        if (!::m_btwrap.isInitialized) {
            m_btwrap = MBluetoothWrapper()
        }
        if (!m_btwrap.is_enabled) {
            val enableBluetooth = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivityForResult(enableBluetooth, 0)
            return "Run the command again once bluetooth is enabled"
        }

        var str: String = "Devices (${m_btwrap.m_devices.size}):\n"

        m_btwrap.m_devices.forEachIndexed() {
            idx, each ->
                if (idx != m_btwrap.m_devices.size - 1) str += "#${idx} ${each.name}\n"
                else str += "#${idx} ${each.name}"
        }

        return str
    }

}