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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

class MMessageCommander(val m_text: String, val m_action: () -> Unit)

class MMessageCard(val m_obj: MMessage, val is_sys: Int, val m_cmd : List<MMessageCommander> = listOf<MMessageCommander>()){
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
                text = "${m_obj.msg}"
            )

            if (m_cmd.size > 0) {
                for (i in 0 .. (m_cmd.size - 1) step 2) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(0.dp)

                    ) {
                        Button(
                            onClick = { m_cmd[i].m_action() },
                            modifier = Modifier
                                .weight(0.5f)
                                .padding(all = 5.dp)
                                .requiredHeight(34.dp),
                            shape = RoundedCornerShape(12.dp)

                        ) {
                            Text(text = m_cmd[i].m_text)
                        }

                        if ((i + 1) < m_cmd.size) {
                            Button(
                                onClick = { m_cmd[i+1].m_action() },
                                modifier = Modifier
                                    .weight(0.5f)
                                    .padding(all = 5.dp)
                                    .requiredHeight(34.dp),
                                shape = RoundedCornerShape(12.dp)

                            ) {
                                Text(text = m_cmd[i+1].m_text)
                            }
                        }

                        /*m_cmd.forEach {
                            Button(
                                onClick = { it.m_action() },
                                modifier = Modifier
                                    .weight(0.5f)
                                    .padding(all = 5.dp)
                                    .requiredHeight(34.dp),
                                shape = RoundedCornerShape(12.dp)

                            ) {
                                Text(text = it.m_text)
                            }

                        }*/
                    }
                }


            }
        }
    }
}

@Suppress("DEPRECATION")
class MDisplay(
    private val m_pref: MPreferences,
    private val context: Context,
    private val m_input_handler: (String) -> Boolean,
    private val m_output_copy: (String, String) -> Unit,
    private val m_cmd_list: List<MMessageCommander>
) {
    companion object {
        const val MAX_MESSAGES_ON_LIST: Int = 100
        val OFFSET_FIX_INPUT_DP: Dp = 18.dp
    }

    var m_self_name: MutableState<String> = mutableStateOf("")
    private lateinit var m_pref_ref: SharedPreferences
    private val m_input_value: MutableState<String> = mutableStateOf("")
    private val m_message_list: MutableState<List<MMessageCard>> = mutableStateOf(arrayListOf<MMessageCard>())

    init {
        m_self_name.value = m_pref.get("username", "Self_" + (Math.floor(Math.random() * 9999999.0f)).toInt().toString() + (Math.floor(Math.random() * 9999999.0f)).toInt().toString())
        m_pref.set("username", m_self_name.value)

        val curr_msgs = m_pref.getList("messages")
        populate_list_with(curr_msgs)

        if (curr_msgs.size == 0) {
            post_system("Hello there! Welcome to Nearby Chat App, made by Lohk! (Version: V1.0.0rc3)")
            post_system("This is a WIP app. Please send feedback to @lohkdesgds")
            post_system("Your name was randomly set to ${m_self_name.value}. Please change with /nick <new name>!", listOf(MMessageCommander("Pre-type /nick", {cmd_pretype_nick()})))
        }
        else {
            post_system("Welcome back, ${m_self_name.value}! (Version: V1.0.0rc3)")

        }
    }

    // this will broadcast
    fun post_self_auto(msg: String)
    {
        m_output_copy(m_self_name.value, msg)
        post(m_self_name.value, msg, false)
    }

    // this is local
    fun post_other_auto(ath: String, msg: String)
    {
        post(ath, msg, false)
    }

    // this is local
    fun post_system(msg: String, m_cmd : List<MMessageCommander> = listOf<MMessageCommander>())
    {
        post("SYSTEM", msg, true, m_cmd)
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
            Column(
                modifier = Modifier.fillMaxWidth().shadow(5.dp).background(Color.hsl(200.0f, 0.75f, 0.2f, 1f)),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(text = "Your name: ${m_self_name.value}", modifier = Modifier.padding(vertical = 8.dp),
                    style = LocalTextStyle.current.copy(color = Color.White))
            }

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
                Button(
                    onClick = { post_commands_box() },
                    modifier = Modifier
                        .padding(vertical = 5.dp)
                        .requiredHeight(34.dp),
                    shape = RoundedCornerShape(12.dp)

                ) {
                    Text(text = "/")
                }
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
                    shape = RoundedCornerShape(12.dp)

                ) {
                    Text(text = "Send")
                }
            }
        }
    }

    fun convert_list() : List<String> {
        var lst = arrayListOf<String>()

        m_message_list.value.forEach { it -> lst.add(it.m_obj.toString()) /* custom toString */ }

        return lst.toList()
    }

    private fun populate_list_with(lst: List<String>)
    {
        m_message_list.value = arrayListOf<MMessageCard>()
        lst.forEach { it ->
            val msg = MMessage(it)
            _postComplex(msg.ath, msg.msg, 2, listOf<MMessageCommander>())
        }
    }

    private fun post_input_clr()
    {
        if (!m_input_handler(m_input_value.value)) {
            post_self_auto(m_input_value.value)
        }
        m_input_value.value = "";
    }

    fun cmd_pretype_nick()
    {
        m_input_value.value = "/nick ";
    }

    private fun post_commands_box()
    {
        post_system("Here are some commands!", m_cmd_list)
            /*listOf<MMessageCommander>(
                MMessageCommander("Send aaa") { post_other_auto("TEST", "aaa") },
                MMessageCommander("Send bbb") { val tst = ::post_other_auto; tst("TEST", "bbb") }
            )
        );*/
    }

    private fun post(ath: String, msg: String, is_sys: Boolean, m_cmd : List<MMessageCommander> = listOf<MMessageCommander>()){
        when (is_sys) {
            true -> _postComplex(ath, msg, 1, m_cmd)
            false -> _postComplex(ath, msg, 0, m_cmd)
        }
    }

    private fun _postComplex(ath: String, msg: String, is_sys: Int, m_cmd: List<MMessageCommander>) {
        if (msg.length > 0) m_message_list.value += MMessageCard(MMessage(ath, msg), is_sys, m_cmd)
        else m_message_list.value += MMessageCard(MMessage(ath, "<empty>"), is_sys, m_cmd)

        if (m_message_list.value.size > MAX_MESSAGES_ON_LIST) m_message_list.value = m_message_list.value.drop(1)
    }
}