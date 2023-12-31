package com.lsw.nearbychat.ui

import android.R
import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import com.google.android.gms.nearby.Nearby
import com.google.android.gms.nearby.connection.AdvertisingOptions
import com.google.android.gms.nearby.connection.ConnectionInfo
import com.google.android.gms.nearby.connection.ConnectionLifecycleCallback
import com.google.android.gms.nearby.connection.ConnectionResolution
import com.google.android.gms.nearby.connection.ConnectionsClient
import com.google.android.gms.nearby.connection.DiscoveredEndpointInfo
import com.google.android.gms.nearby.connection.DiscoveryOptions
import com.google.android.gms.nearby.connection.EndpointDiscoveryCallback
import com.google.android.gms.nearby.connection.Payload
import com.google.android.gms.nearby.connection.PayloadCallback
import com.google.android.gms.nearby.connection.PayloadTransferUpdate
import com.google.android.gms.nearby.connection.Strategy
import com.google.android.gms.tasks.OnFailureListener
import com.google.android.gms.tasks.OnSuccessListener
import kotlin.text.Charsets.UTF_8


private val c_appid_nb = "1635"; // random me


class MNearbyWork(
    private val context: Context,
    private val activity: Activity,
    val m_nick_fcn: () -> String,
    val m_post_received: (String, String) -> Unit,
    val m_system_message: (String) -> Unit
) {
    companion object {
        private val c_strategy = Strategy.P2P_CLUSTER
    }

    private lateinit var connectionsClient: ConnectionsClient
    private var m_friend: MutableMap<String, String> = mutableMapOf<String, String>() // id, nick

    private var is_advertising: Boolean = false
    private var is_discoverying: Boolean = false

    // no need, byte payload goes as one: https://developers.google.com/nearby/connections/android/exchange-data?hl=en#bytes
    //private var m_payload_loading: MutableMap<String, String> = mutableMapOf<String, String>() // no need

    fun friendCount() : Int {
        return m_friend.size
    }

    fun friendList() : List<String>
    {
        var lst = arrayListOf<String>()
        m_friend.forEach { _, nick -> lst.add(nick) }
        return lst.toList()
    }

    fun send(ath: String, msg: String)
    {
        if (m_friend.size > 0) {
            val obj = ath + "\n" + msg
            var lst = arrayListOf<String>()

            m_friend.forEach { id, _ -> lst.add(id) }

            connectionsClient.sendPayload(
                lst.toList(),
                Payload.fromBytes(obj.toByteArray(UTF_8))
            )
        }
    }

    // This will announce yourself to others like "Hey, join me!"
    fun startAdvertising()
    {
        if (is_discoverying) stopAndCleanUp()
        setupConnectionsClient()

        val advertisingOptions = AdvertisingOptions.Builder().setStrategy(c_strategy).build()

        Nearby.getConnectionsClient(context)
            .startAdvertising(
               m_nick_fcn(), c_appid_nb, connectionLifecycleCallback, advertisingOptions
            )
            .addOnSuccessListener(
                OnSuccessListener<Void> { m_system_message("Started advertising you to others successfully!"); is_advertising = true  }
            )
            .addOnFailureListener(
                OnFailureListener { e: Exception? -> m_system_message("Failed to start advertisement. Reason: ${e?.toString() ?: "unknown"}"); stopAndCleanUp() }
            )


    }

    fun startDiscovering()
    {
        if (is_advertising) stopAndCleanUp()
        setupConnectionsClient()

        val discoveryOptions = DiscoveryOptions.Builder().setStrategy(c_strategy).build()
        Nearby.getConnectionsClient(context)
            .startDiscovery(c_appid_nb, endpointDiscoveryCallback, discoveryOptions)
            .addOnSuccessListener(
                OnSuccessListener<Void> { m_system_message("Started discovering devices successfully!"); is_discoverying = true }
            )
            .addOnFailureListener(
                OnFailureListener { e: Exception? -> m_system_message("Failed to start discovery. Reason: ${e?.toString() ?: "unknown"}"); stopAndCleanUp() }
            )
    }

    fun stopAndCleanUp() : Boolean
    {
        val was = is_advertising || is_discoverying

        if (::connectionsClient.isInitialized) {
            connectionsClient.apply {
                if (is_advertising) stopAdvertising()
                if (is_discoverying) stopDiscovery()
                stopAllEndpoints()
            }
        }
        //m_payload_loading.clear()
        is_advertising = false
        is_discoverying = false
        m_friend.clear()

        return was
    }

    private fun setupConnectionsClient()
    {
        if (!::connectionsClient.isInitialized) connectionsClient = Nearby.getConnectionsClient(activity)
    }

    private fun addFriend(fr: String, nick: String)
    {
        if (!m_friend.containsKey(fr)) m_friend[fr] = nick
    }

    private fun delFriend(fr: String)
    {
        if (m_friend.containsKey(fr)) m_friend.remove(fr)
    }

    // Callbacks for connections to other devices
    // Related to Advertising
    private val connectionLifecycleCallback = object : ConnectionLifecycleCallback() {
        override fun onConnectionInitiated(endpointId: String, info: ConnectionInfo) {
            // Accepting a connection means you want to receive messages. Hence, the API expects
            // that you attach a PayloadCall to the acceptance
            //connectionsClient.acceptConnection(endpointId, payloadCallback)
            //opponentName = "Opponent\n(${info.endpointName})"

            if (info.endpointName == m_nick_fcn() || m_friend.containsKey(endpointId)) {
                m_system_message("New endpoint $endpointId (${info.endpointName}) tried to connect, but nick is already present. Cancelled automatically")
                Nearby.getConnectionsClient(context).rejectConnection(endpointId)
                return;
            }

            m_system_message("New endpoint connecting! $endpointId (${info.endpointName}) [requires simple auth]")

            AlertDialog.Builder(context)
                .setTitle("Accept connection to " + info.endpointName)
                .setMessage("Confirm the code matches on both devices: " + info.authenticationDigits)
                .setPositiveButton(
                    "Accept"
                ) { _: DialogInterface?, _: Int ->  // The user confirmed, so we can accept the connection.
                    Nearby.getConnectionsClient(context)
                        .acceptConnection(endpointId, payloadCallback)

                    addFriend(endpointId, info.endpointName)
                }
                .setNegativeButton(
                    R.string.cancel
                ) { _: DialogInterface?, _: Int ->  // The user canceled, so we should reject the connection.
                    Nearby.getConnectionsClient(context).rejectConnection(endpointId)
                }
                .setIcon(R.drawable.ic_dialog_alert)
                .show()
        }

        override fun onConnectionResult(endpointId: String, result: ConnectionResolution) {
            if (result.status.isSuccess) {
                m_system_message("New endpoint connected successfully! $endpointId")

                /*connectionsClient.stopAdvertising()
                connectionsClient.stopDiscovery()
                opponentEndpointId = endpointId
                binding.opponentName.text = opponentName
                binding.status.text = "Connected"
                setGameControllerEnabled(true) // we can start playing*/
            }
            else {
                m_system_message("Endpoint failed to connect: $endpointId")
                delFriend(endpointId)
                //m_payload_loading.remove(endpointId)
            }
        }

        override fun onDisconnected(endpointId: String) {
            m_system_message("Lost endpoint: $endpointId")
            delFriend(endpointId)
            //m_payload_loading.remove(endpointId)
        }
    }

    // Callbacks for finding other devices
    // Related to Discovery
    private val endpointDiscoveryCallback = object : EndpointDiscoveryCallback() {
        override fun onEndpointFound(endpointId: String, info: DiscoveredEndpointInfo) {
            m_system_message("Found an endpoint: $endpointId! (${info.endpointName})")
            connectionsClient.requestConnection(m_nick_fcn(), endpointId, connectionLifecycleCallback)
                .addOnSuccessListener(
                    OnSuccessListener<Void> { m_system_message("Endpoint $endpointId (${info.endpointName}) works!"); addFriend(endpointId, info.endpointName) }
                )
                .addOnFailureListener(
                    OnFailureListener { e: Exception? ->
                        m_system_message("Endpoint $endpointId (${info.endpointName}) did not work! Reason: ${e?.toString() ?: "unknown"}")
                        delFriend(endpointId)
                        //m_payload_loading.remove(endpointId)
                    }
                )
        }

        override fun onEndpointLost(endpointId: String) {
            m_system_message("Lost an endpoint: $endpointId")
            delFriend(endpointId)
           // m_payload_loading.remove(endpointId)
        }
    }

    /** callback for receiving payloads */
    private val payloadCallback: PayloadCallback = object : PayloadCallback() {
        override fun onPayloadReceived(endpointId: String, payload: Payload) {
            payload.asBytes()?.let {

                val tmp: String = it.toString(UTF_8)
                if (tmp.indexOf('\n') == -1) return

                // strings
                val ath = tmp.substring(0, tmp.indexOf('\n'))
                val msg = tmp.substring(tmp.indexOf('\n') + 1)
                val self_nick = m_nick_fcn()

                if (ath != self_nick) { // self controlled, else wait for feedback from host
                    m_post_received(ath, msg)
                }

                if (is_advertising) { // is host, broadcast
                    send(ath, msg)
                }
            }
        }

        override fun onPayloadTransferUpdate(endpointId: String, update: PayloadTransferUpdate) {
            // Determines the winner and updates game state/UI after both players have chosen.
            // Feel free to refactor and extract this code into a different method

            when (update.status) {
                PayloadTransferUpdate.Status.SUCCESS -> {
                }
                PayloadTransferUpdate.Status.IN_PROGRESS -> {}
                PayloadTransferUpdate.Status.CANCELED -> {
                    m_system_message("Endpoint $endpointId cancelled a message transfer. (?)")
                    //m_payload_loading.remove(endpointId)
                }
                PayloadTransferUpdate.Status.FAILURE -> {
                    m_system_message("Endpoint $endpointId failed to transfer a message (oh no)")
                    //m_payload_loading.remove(endpointId)
                }
            }
        }
    }
}