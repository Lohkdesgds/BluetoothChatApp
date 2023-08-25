@file:OptIn(ExperimentalComposeUiApi::class)

package com.lsw.myapplication


import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import com.lsw.myapplication.ui.BluetoothConnectedDevice
import com.lsw.myapplication.ui.theme.MyApplicationTheme
import java.io.IOException
import androidx.compose.material3.Card as Card1


class MyMessage(val ath: String, val msg: String)

class FunkyWrapper(
    val lst: MutableState<List<MyMessage>> = mutableStateOf(arrayListOf<MyMessage>()),
    val txt: MutableState<String> = mutableStateOf(""),
    var in_height: MutableState<Dp> = mutableStateOf(0.dp),
    var all_height: MutableState<Dp> = mutableStateOf(0.dp)
)
internal object RequestCode {
    const val BLUETOOTH_CONNECT_CODE = 100
}

class BluetoothWrapper {
    var mBluetoothAdapter: BluetoothAdapter? = null
    var pairedDevices: Set<BluetoothDevice>? = null
    var connectedDevices = listOf<BluetoothConnectedDevice>(); // dumbthing: don't forget to closeBT() before removal
    //var mmSocket: BluetoothSocket? = null
    //var mmOutputStream: OutputStream? = null
    //var mmInputStream: InputStream? = null
    //var workerThread: Thread? = null
    //lateinit var readBuffer: ByteArray
    //var readBufferPosition = 0
    //var counter = 0
    //@Volatile
    //var stopWorker = false
}

class MainActivity : ComponentActivity() {
    val funky = FunkyWrapper()
    val bt = BluetoothWrapper()

    //val test = arrayListOf<String>("a", "b");
    //var has_new = false;

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.BLUETOOTH_CONNECT
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.BLUETOOTH_CONNECT), RequestCode.BLUETOOTH_CONNECT_CODE)
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
        }



        setContent {
            MyApplicationTheme {
                CombinedDraw()
            }
        }
    }

    fun connectBluetooth(device: BluetoothDevice)
    {
        bt.connectedDevices += BluetoothConnectedDevice(device, { ath, msg -> postChat(ath, msg) })
    }

    fun stringConnectBluetooth(checkcmd: String) {
        if (checkcmd.indexOf("/connect ") != 0) return;

        val num = checkcmd.substring("/connect ".length).toInt()

        postChat("DEBUG", "Connect to #$num requested")

        connectBluetooth(bt.pairedDevices!!.toList()[num])
    }

    @SuppressLint("MissingPermission")
    fun listBT() {
        try {
            var list_of_devices: String = "";

            bt.pairedDevices!!.toList().forEachIndexed() { index, device ->
                list_of_devices += "DEVICE #$index: [${device.name}];\n";
            }

            postChat("DEBUG", "LIST OF DEVICES:\n$list_of_devices");
        }
        catch(err: IOException) {
            postChat("DEBUG EXCEPTION", "IOException:\n$err");
        }
        catch(err : Exception) {
            postChat("DEBUG EXCEPTION", "Exception:\n$err");
        }
    }

    @SuppressLint("MissingPermission")
    @Suppress("DEPRECATION")
    fun findBT() : Boolean {
        try {
            bt.mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
            if (bt.mBluetoothAdapter == null) {
                return false;
            }

            if (!bt.mBluetoothAdapter!!.isEnabled()) {
                val enableBluetooth = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);

                startActivityForResult(enableBluetooth, 0)
            }

            bt.pairedDevices =  bt.mBluetoothAdapter!!.getBondedDevices();

            /*var list_of_devices: String = "";

            for (device in bt.pairedDevices!!.toList()) {
                list_of_devices += "DEVICE:[" + device.name + "];\n";
            }*/

            postChat("DEBUG", "Found ${bt.pairedDevices!!.size} device(s)");
        }
        catch(err: IOException) {
            postChat("DEBUG EXCEPTION", "IOException:\n$err");
        }
        catch(err : Exception) {
            postChat("DEBUG EXCEPTION", "Exception:\n$err");
        }

        return true;
    }

    fun postChat(author: String, message: String) {
        funky.lst.value += MyMessage(author, message)
        if (funky.lst.value.size > 200) funky.lst.value = funky.lst.value.drop(1)
    }

    fun postSelf(message: String) {
        when {
            message == "/findbt" -> { findBT(); listBT() }
            message.startsWith("/connect") -> stringConnectBluetooth(message)
            else -> {
                postChat("Me", message)
                for(i in bt.connectedDevices) i.sendData("Guest", message)
            };
        }
    }

    @Preview(showBackground = true)
    @Composable
    fun CombinedDraw()
    {
        val localDensity = LocalDensity.current

        Column(
            modifier = Modifier
                .fillMaxSize()
                .imePadding()
                .onGloballyPositioned { coordinates ->
                    funky.all_height.value = with(localDensity) { coordinates.size.height.toDp() }
                },
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            LoggingChat()
            InputBox()
        }
    }

    @Composable
    fun LoggingChat()
    {
        val h = funky.all_height.value - funky.in_height.value;
        var mod = Modifier
            .fillMaxWidth()
            .background(color = Color.LightGray)
            .requiredHeight(h - 18.dp) // somehow this works

        LazyColumn(
            modifier = mod,
            reverseLayout = true
        ) {
            itemsIndexed(funky.lst.value.reversed()) {
                    index, item ->
                Card1(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(all = 8.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Card1(
                        modifier = Modifier
                            .padding(all = 5.dp)
                    ) {
                        Text(
                            text = "${index}: ${item.ath}",
                            fontWeight = FontWeight.Bold
                        )
                        Text(text = item.msg)

                        /*var comboed: String = "";
                        for (i in item.msg) {
                            comboed += (i.code).toString() + " "
                        }*/

                        //Text(text = "VAL: ${comboed} | Limit: ${h}")
                    }
                }
            }
        }
    }

    @Composable
    fun InputBox()
    {
        val localDensity = LocalDensity.current

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp)

        ) {
            BasicTextField(value = funky.txt.value, onValueChange = { newText ->
                funky.txt.value = newText
            },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(36.dp)
                    .offset(0.dp, 4.dp)
                    .weight(1f)
                    .padding(horizontal = 2.dp)
                    .background(Color.Red)
                    .onGloballyPositioned { coordinates ->
                        funky.in_height.value =
                            with(localDensity) { coordinates.size.height.toDp() }
                    },
                singleLine = true,
                keyboardActions = KeyboardActions(
                    onDone = { postSelf(funky.txt.value); funky.txt.value = "";}
                )
            )
            Button(
                onClick = { postSelf(funky.txt.value); funky.txt.value = ""; },
                modifier = Modifier
                    .padding(horizontal = 4.dp),
                shape = RoundedCornerShape(8.dp)

            ) {
                Text(text = "Send")
            }
        }
    }
}