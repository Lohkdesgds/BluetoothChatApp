@file:OptIn(ExperimentalComposeUiApi::class)

package com.lsw.myapplication


import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.ExperimentalComposeUiApi
import com.lsw.myapplication.ui.MNearbyWork
import com.lsw.myapplication.ui.theme.MyApplicationTheme
import java.util.concurrent.Executors

class MainActivity : ComponentActivity() {
    val m_display = mutableStateOf(MDisplay(::inputCmdHandler, ::tunnelBroadcastSelf))
    val m_exec = Executors.newFixedThreadPool(2)
    val m_nearby = MNearbyWork(this, this, m_nick_fcn = ::tunnelGetInnerSelfName, m_post_received = ::tunnelOthersMessage, m_system_message = ::tunnelSystemMessage)
    //var m_waiting_for_bt: Boolean = false


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestAllPermissions(this)

        setContent {
            MyApplicationTheme {
                m_display.value.draw()
            }
        }
    }


    private fun inputCmdHandler(inn: String) : Boolean
    {
        return when {
            inn.startsWith("/nick") -> {
                m_exec.execute( Runnable {
                    try {
                        val tmp = inn.substring("/nick ".length)
                        if (tmp.length > 0) {
                            m_display.value.m_self_name = tmp
                            m_display.value.post_system("Changed nick to ${m_display.value.m_self_name}")
                        }
                        else{
                            m_display.value.post_system("Invalid nick")
                        }
                    }
                    catch(e: Exception) {
                        m_display.value.post_system("Bad news, EXCEPTION: $e")
                    }
                } )
                true
            }

            inn.startsWith("/advertise") -> {
                m_exec.execute( Runnable {
                    try{
                        m_display.value.post_system("Enabling advertising...")
                        m_nearby.startAdvertising()
                        m_display.value.post_system("Hopefully good.")
                    }
                    catch(e: Exception) {
                        m_display.value.post_system("Bad news, EXCEPTION: $e")
                    }
                } )
                true
            }

            inn.startsWith("/discover") -> {
                m_exec.execute( Runnable {
                    try{
                        m_display.value.post_system("Enabling discovery...")
                        m_nearby.startDiscovering()
                        m_display.value.post_system("Hopefully good.")
                    }
                    catch(e: Exception) {
                        m_display.value.post_system("Bad news, EXCEPTION: $e")
                    }
                } )
                true
            }

            inn.startsWith("/stopall") -> {
                m_exec.execute( Runnable {
                    try {
                        m_display.value.post_system("Requesting stop of advertise/discover...")
                        m_nearby.stopAndCleanUp()
                        m_display.value.post_system("Hopefully good.")
                    }
                    catch(e: Exception) {
                        m_display.value.post_system("Bad news, EXCEPTION: $e")
                    }
                } )
                true
            }

            inn.startsWith("/") -> {
                m_exec.execute( Runnable {
                    m_display.value.post_system("Unrecognized command.\nPlease try:\n/nick <new nick>\n/advertise\n/discover\n/stopall")
                } )
                true
            }

            else -> {
                false
            }
        }
    }

    // triggered by self message. It should post (sendPayload)
    private fun tunnelBroadcastSelf(ath: String, msg: String)
    {
        m_nearby.send(ath, msg)
    }

    private fun tunnelGetInnerSelfName(): String
    {
        return m_display.value.m_self_name
    }

    private fun tunnelOthersMessage(ath: String, msg: String)
    {
        m_display.value.post_other_auto(ath, msg)
    }

    private fun tunnelSystemMessage(msg: String)
    {
        m_display.value.post_system(msg)
    }

    /*@Suppress("DEPRECATION")
    @SuppressLint("MissingPermission")
    private fun listBluetoothToString() : String {
        makeSureBluetoothIsInitialized()

        if (!m_btwrap.is_enabled) {
            m_btwrap.requestBluetooth()
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

    private fun makeSureBluetoothIsInitialized() {
        if (!::m_btwrap.isInitialized) {
            m_btwrap = MBluetoothWrapper(this)
        }
    }*/

}