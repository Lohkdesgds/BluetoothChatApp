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
    private var m_friend: ArrayList<String> = arrayListOf<String>()

    private var is_advertising: Boolean = false
    private var is_discoverying: Boolean = false

    // no need, byte payload goes as one: https://developers.google.com/nearby/connections/android/exchange-data?hl=en#bytes
    //private var m_payload_loading: MutableMap<String, String> = mutableMapOf<String, String>() // no need


    fun send(ath: String, msg: String)
    {
        if (m_friend.size > 0) {
            val obj = ath + "\n" + msg
            connectionsClient.sendPayload(
                m_friend.toList(),
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
                OnSuccessListener<Void> { m_system_message("Started advertising you to others successfully!") }
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
                OnSuccessListener<Void> { m_system_message("Started discovery successfully!") }
            )
            .addOnFailureListener(
                OnFailureListener { e: Exception? -> m_system_message("Failed to start discovery. Reason: ${e?.toString() ?: "unknown"}"); stopAndCleanUp() }
            )
    }

    fun stopAndCleanUp()
    {
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
    }

    private fun setupConnectionsClient()
    {
        if (!::connectionsClient.isInitialized) connectionsClient = Nearby.getConnectionsClient(activity)
    }

    private fun addFriend(fr: String)
    {
        if (m_friend.indexOf(fr) == -1) m_friend.add(fr)
    }

    private fun delFriend(fr: String)
    {
        if (m_friend.indexOf(fr) != -1) m_friend.remove(fr)
    }

    // Callbacks for connections to other devices
    // Related to Advertising
    private val connectionLifecycleCallback = object : ConnectionLifecycleCallback() {
        override fun onConnectionInitiated(endpointId: String, info: ConnectionInfo) {
            // Accepting a connection means you want to receive messages. Hence, the API expects
            // that you attach a PayloadCall to the acceptance
            //connectionsClient.acceptConnection(endpointId, payloadCallback)
            //opponentName = "Opponent\n(${info.endpointName})"
            m_system_message("New endpoint connecting! $endpointId (${info.endpointName}) [requires simple auth]")

            AlertDialog.Builder(context)
                .setTitle("Accept connection to " + info.endpointName)
                .setMessage("Confirm the code matches on both devices: " + info.authenticationDigits)
                .setPositiveButton(
                    "Accept"
                ) { _: DialogInterface?, _: Int ->  // The user confirmed, so we can accept the connection.
                    Nearby.getConnectionsClient(context)
                        .acceptConnection(endpointId, payloadCallback)
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
                addFriend(endpointId)
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
                    OnSuccessListener<Void> { m_system_message("Endpoint $endpointId (${info.endpointName}) works!"); addFriend(endpointId) }
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
                /*if (m_payload_loading[endpointId] == null) m_payload_loading[endpointId] = it.toString()
                else m_payload_loading[endpointId] += it.toString()*/
                val tmp: String = it.toString(UTF_8)

                if (tmp.indexOf('\n') == -1) return

                m_post_received(
                    tmp.substring(0, tmp.indexOf('\n')),
                    tmp.substring(tmp.indexOf('\n') + 1)
                )
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



// based on https://stackoverflow.com/questions/13450406/how-to-receive-serial-data-using-android-bluetooth

/*class MBluetoothSocketOn(
    val m_device: BluetoothDevice,
    val m_recv: (String) -> Unit
) {
    fun send(data: String)
    {

    }
}

@SuppressLint("MissingPermission")
@Suppress("DEPRECATION") // Older Android doesn't even know the newest way, so we have to work like this.
class MBluetoothWrapper(private val m_current_context: Context) {
    private lateinit var m_adapter: BluetoothAdapter
    private var m_nobt: Boolean = false

    val m_devices: Set<BluetoothDevice>
        get() { if (m_nobt) {return setOf<BluetoothDevice>() } else { if (!is_enabled) {requestBluetooth()}; return m_adapter.bondedDevices} }

    val is_enabled: Boolean
        get() { if (m_nobt) {return false} else {return m_adapter.isEnabled} }

    init {
        val bttest: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()

        if (bttest == null) {
            m_nobt = true
        }
        else {
            m_nobt = false
            m_adapter = bttest
        }
    }

    fun makeDiscoverable()
    {
        if (!is_enabled) requestBluetooth()
        else _popDiscoverable()
    }

    fun requestBluetooth()
    {
        val enableBluetooth = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
        ContextCompat.startActivity(m_current_context, enableBluetooth, null)
    }

    private fun _popDiscoverable()
    {
        val discoverableIntent = Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE)
        discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300)
        ContextCompat.startActivity(m_current_context, discoverableIntent, null)
    }

}*/




/*@SuppressLint("MissingPermission")
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
}*/