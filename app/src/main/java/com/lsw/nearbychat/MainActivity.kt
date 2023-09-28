@file:OptIn(ExperimentalComposeUiApi::class)

package com.lsw.nearbychat


import android.os.Bundle
import android.preference.PreferenceManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.ExperimentalComposeUiApi
import com.lsw.nearbychat.ui.MNearbyWork
import com.lsw.nearbychat.ui.theme.MyApplicationTheme
import java.util.concurrent.Executors

@Suppress("DEPRECATION")
class MainActivity : ComponentActivity() {
    lateinit var m_pref: MPreferences
    lateinit var m_display: MutableState<MDisplay>
    val m_exec = Executors.newFixedThreadPool(2)
    val m_nearby = MNearbyWork(this, this, m_nick_fcn = ::tunnelGetInnerSelfName, m_post_received = ::tunnelOthersMessage, m_system_message = ::tunnelSystemMessage)
    //var m_waiting_for_bt: Boolean = false

    private var m_avoid_save = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestAllPermissions(this)

        m_pref = MPreferences(PreferenceManager.getDefaultSharedPreferences(this))
        m_display = mutableStateOf(MDisplay(
            m_pref,
            this,
            ::inputCmdHandler,
            ::tunnelBroadcastSelf,
            list_cmd())
        )

        setContent {
            MyApplicationTheme { m_display.value.draw() }
        }
    }

    override fun onBackPressed() {
        if (!m_avoid_save) m_pref.setList("messages", m_display.value.convert_list())
        cmd_stopall()
        super.onBackPressed()
    }


    /*override fun onPause() { // left, but kept on
        super.onPause()
    }

    override fun onResume() { // back from pause
        super.onResume()
        m_display.value.post_system("Hello! Welcome back once again!")
    }*/

    override fun onStop() {
        if (!m_avoid_save) m_pref.setList("messages", m_display.value.convert_list())
        cmd_stopall()
        super.onStop()
    }

    private fun inputCmdHandler(inn: String) : Boolean
    {
        return when {
            inn.startsWith("/nick") -> {
                cmd_nick(inn)
                true
            }

            inn.startsWith("/friendcount") -> {
                cmd_friendcount()
                true
            }

            inn.startsWith("/friendlist") -> {
                cmd_friendlist()
                true
            }

            inn.startsWith("/advertise") -> {
                cmd_advertise()
                true
            }

            inn.startsWith("/discover") -> {
                cmd_discover()
                true
            }

            inn.startsWith("/stopall") -> {
                cmd_stopall()
                true
            }

            inn.startsWith("/") -> {
                m_exec.execute( Runnable {
                    m_display.value.post_system("Unrecognized command.\nPlease try:\n/nick <new nick>\n/advertise\n/discover\n/stopall\n/friendcount\n/friendlist")
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
        return m_display.value.m_self_name.value
    }

    private fun tunnelOthersMessage(ath: String, msg: String)
    {
        m_display.value.post_other_auto(ath, msg)
    }

    private fun tunnelSystemMessage(msg: String)
    {
        m_display.value.post_system(msg)
    }


    private fun list_cmd() : List<MMessageCommander> {
        return listOf(
            MMessageCommander("/friendcount") { cmd_friendcount() },
            MMessageCommander("/friendlist") { cmd_friendlist() },
            MMessageCommander("/advertise (host)") { cmd_advertise() },
            MMessageCommander("/discover (client)") { cmd_discover() },
            MMessageCommander("/stopall") { cmd_stopall() },
            MMessageCommander("/nick (pre-type)") { cmd_pretype_nick() },
            MMessageCommander("/reset") { m_avoid_save = true; m_pref.clear(); this.finishAffinity() }
        )
    }

    private fun cmd_pretype_nick()
    {
        m_display.value.cmd_pretype_nick()
    }

    private fun cmd_nick(inn: String)
    {
        m_exec.execute( Runnable {
            try {
                if (m_nearby.friendCount() > 0) {
                    m_display.value.post_system("You may not change your nick once connected to another device!")
                }
                else {
                    val tmp = inn.substring("/nick ".length)
                    if (tmp.length > 0) {
                        m_display.value.m_self_name.value = tmp
                        m_pref.set("username", tmp)
                        m_display.value.post_system("Changed nick to ${m_display.value.m_self_name.value}")
                    }
                    else{
                        m_display.value.post_system("Invalid nick")
                    }
                }
            }
            catch(e: Exception) {
                m_display.value.post_system("Bad news, EXCEPTION: $e")
            }
        } )
    }

    private fun cmd_friendcount()
    {
        m_exec.execute( Runnable {
            try {
                m_display.value.post_system("Friends connected to this device: ${m_nearby.friendCount()}")
            }
            catch(e: Exception) {
                m_display.value.post_system("Bad news, EXCEPTION: $e")
            }
        } )
    }

    private fun cmd_friendlist()
    {
        m_exec.execute( Runnable {
            try {
                var str: String = ""
                m_nearby.friendList().forEach {
                    str += it + "\n"
                }
                if (str.length > 0) str.dropLast(1)

                m_display.value.post_system("Friends connected to this device:\n$str")
            }
            catch(e: Exception) {
                m_display.value.post_system("Bad news, EXCEPTION: $e")
            }
        } )
    }

    private fun cmd_advertise()
    {
        m_exec.execute( Runnable {
            try{
                m_nearby.startAdvertising()
            }
            catch(e: Exception) {
                m_display.value.post_system("Could not enable advertising, EXCEPTION: $e")
            }
            finally {
                m_display.value.post_system("It should be ready soon!")
            }
        } )
    }

    private fun cmd_discover()
    {
        m_exec.execute( Runnable {
            try{
                m_nearby.startDiscovering()
            }
            catch(e: Exception) {
                m_display.value.post_system("Could not enable discovery, EXCEPTION: $e")
            }
            finally {
                m_display.value.post_system("It should be ready soon!")
            }
        } )
    }

    private fun cmd_stopall()
    {
        m_exec.execute( Runnable {
            var was_enabled = false
            try {
                was_enabled = m_nearby.stopAndCleanUp()
            }
            catch(e: Exception) {
                m_display.value.post_system("Could not stop? EXCEPTION: $e")
            }
            finally {
                if (was_enabled) m_display.value.post_system("Stopped connections.")
            }
        } )
    }

}