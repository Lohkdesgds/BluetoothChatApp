package com.lsw.myapplication

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Divider
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

class MMessageCard(val m_ath: String, val m_msg: String, val is_sys: Boolean){
    @Composable
    fun draw()
    {
        val author_color: Color

        if (is_sys) author_color = Color.Blue
        else author_color = Color.Black

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(all = 10.dp)
                .shadow(elevation = 10.dp, spotColor = Color.DarkGray, shape = RoundedCornerShape(8.dp)),
            shape = RoundedCornerShape(12.dp)

        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ){
                Text(
                    modifier = Modifier.padding(start = 8.dp, top = 8.dp, end = 8.dp, bottom = 4.dp).fillMaxWidth(0.95f),
                    text = "${m_ath}",
                    fontWeight = FontWeight.Bold,
                    color = author_color
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                Divider(Modifier.fillMaxWidth(0.93f), color = Color.Gray, thickness = 2.dp)
            }

            Text(
                modifier = Modifier.padding(start = 20.dp, top = 8.dp, end = 20.dp, bottom = 14.dp),
                text = m_msg
            )
        }
    }
}

class MDisplay(private val m_input_handler: (String) -> Boolean) {
    companion object {
        const val MAX_MESSAGES_ON_LIST: Int = 30
        val OFFSET_FIX_INPUT_DP: Dp = 18.dp
    }

    private val m_input_value: MutableState<String> = mutableStateOf("")
    private val m_message_list: MutableState<List<MutableState<MMessageCard>>> = mutableStateOf(arrayListOf<MutableState<MMessageCard>>())

    init {
        post("SYSTEM", "Hello there!", true)
        post("SYSTEM", "Welcome to BluetoothChat App, made by Lohk!", true)
        post("SYSTEM", "This application uses command lines for most stuff.", true)
        post("SYSTEM", "You can try to use something like /listbt to list all devices on your Bluetooth settings", true)
        post("SYSTEM", "This is a WIP app. Please send feedback to @lohkdesgds", true)
        post("SYSTEM", "Hopefully this will work well someday!", true)
    }

    fun post(ath: String, msg: String, is_sys: Boolean){
        if (msg.length > 0) m_message_list.value += mutableStateOf(MMessageCard(ath, msg, is_sys))
        else m_message_list.value += mutableStateOf(MMessageCard(ath, "<empty>", is_sys))

        if (m_message_list.value.size > MAX_MESSAGES_ON_LIST) m_message_list.value = m_message_list.value.drop(1)
    }

    @Composable
    fun draw(/*localDensity : Density*/)
    {
        val ld = LocalDensity.current;

        Column(
            modifier = Modifier
                .fillMaxSize()
                .imePadding(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(modifier = Modifier.fillMaxWidth(), text = "THIS IS AN EARLY ACCESS, IT MAY CRASH YOUR PHONE!")

            // messages
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                reverseLayout = true
            ) {
                items(m_message_list.value.reversed()) { item -> item.value.draw() }
            }


            // input
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 6.dp)

            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(all = 5.dp)
                        .requiredHeight(34.dp),
                    shape = RoundedCornerShape(8.dp)

                ) {
                    BasicTextField(
                        value = m_input_value.value,
                        onValueChange = { newText -> m_input_value.value = newText },
                        modifier = Modifier
                            /*.fillMaxWidth()
                            .height(36.dp)*/
                            .fillMaxSize()
                            .background(Color.hsl(0f, 0.3f, 0.4f, 1f))
                            /*.offset(0.dp, 4.dp)*/
                            .padding(7.dp),
                        singleLine = true,
                        textStyle = LocalTextStyle.current.copy(color = Color.White),
                        keyboardActions = KeyboardActions(onDone = { post_input_clr() })
                    )
                }
                Button(
                    onClick = { post_input_clr() },
                    modifier = Modifier
                        .padding(all = 5.dp)
                        .requiredHeight(34.dp),
                    shape = RoundedCornerShape(8.dp)

                ) {
                    Text(text = "Send")
                }
            }
        }
    }

    private fun post_input_clr()
    {
        if (!m_input_handler(m_input_value.value)) {
            post("Self", m_input_value.value, false)
        }
        m_input_value.value = "";
    }
}