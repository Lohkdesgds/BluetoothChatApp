package com.lsw.nearbychat

import android.content.Context
import android.content.SharedPreferences
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

class MMessageCard(val m_obj: MMessage, val is_sys: Int){
    @Composable
    fun draw()
    {
        val author_color: Color

        when (is_sys) {
            0 -> author_color = Color.Black     // default
            1 -> author_color = Color.Blue      // system
            2 -> author_color = Color.LightGray // old
            else -> author_color = Color.Red    // Error!
        }

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
                    text = "${m_obj.ath}",
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
                text = m_obj.msg
            )
        }
    }
}

@Suppress("DEPRECATION")
class MDisplay(private val m_pref: MPreferences, private val context: Context, private val m_input_handler: (String) -> Boolean, private val m_output_copy: (String, String) -> Unit) {
    companion object {
        const val MAX_MESSAGES_ON_LIST: Int = 100
        val OFFSET_FIX_INPUT_DP: Dp = 18.dp
    }

    var m_self_name: String = ""
    private lateinit var m_pref_ref: SharedPreferences
    private val m_input_value: MutableState<String> = mutableStateOf("")
    private val m_message_list: MutableState<List<MMessageCard>> = mutableStateOf(arrayListOf<MMessageCard>())

    init {
        m_self_name = m_pref.get("username", "Self_" + (Math.floor(Math.random() * 9999999.0f)).toInt().toString() + (Math.floor(Math.random() * 9999999.0f)).toInt().toString())

        populate_list_with(m_pref.getList("messages"))

        post_system("Hello there!")
        post_system("Welcome to Nearby Chat App, made by Lohk!")
        post_system("This application uses command lines for most stuff.")
        post_system("This is a WIP app. Please send feedback to @lohkdesgds")
        post_system("Hopefully this will work well someday!")
        post_system("Your name was set to $m_self_name. Change with /nick")

    }

    // this will broadcast
    fun post_self_auto(msg: String)
    {
        m_output_copy(m_self_name, msg)
        post(m_self_name, msg, false)
    }

    // this is local
    fun post_other_auto(ath: String, msg: String)
    {
        post(ath, msg, false)
    }

    // this is local
    fun post_system(msg: String)
    {
        post("SYSTEM", msg, true)
    }


    @Composable
    fun draw()
    {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .imePadding(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            //Text(modifier = Modifier.fillMaxWidth(), text = "THIS IS AN EARLY ACCESS, IT MAY CRASH YOUR PHONE!")

            // messages
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                reverseLayout = true
            ) {
                items(m_message_list.value.reversed()) { item -> item.draw() }
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

    private fun convert_list() : List<String> {
        var lst = arrayListOf<String>()

        m_message_list.value.forEach { it -> lst.add(it.m_obj.toString()) /* custom toString */ }

        return lst.toList()
    }

    private fun populate_list_with(lst: List<String>)
    {
        m_message_list.value = arrayListOf<MMessageCard>()
        lst.forEach { it ->
            val msg = MMessage(it)
            _postComplex(msg.ath, msg.msg, 2)
        }
    }

    private fun post_input_clr()
    {
        if (!m_input_handler(m_input_value.value)) {
            post_self_auto(m_input_value.value)
        }
        m_input_value.value = "";
    }

    private fun post(ath: String, msg: String, is_sys: Boolean){
        when (is_sys) {
            true -> _postComplex(ath, msg, 1)
            false -> _postComplex(ath, msg, 0)
        }
    }

    private fun _postComplex(ath: String, msg: String, is_sys: Int) {
        if (msg.length > 0) m_message_list.value += MMessageCard(MMessage(ath, msg), is_sys)
        else m_message_list.value += MMessageCard(MMessage(ath, "<empty>"), is_sys)

        if (m_message_list.value.size > MAX_MESSAGES_ON_LIST) m_message_list.value = m_message_list.value.drop(1)

        m_pref.setList("messages", convert_list())
    }
}