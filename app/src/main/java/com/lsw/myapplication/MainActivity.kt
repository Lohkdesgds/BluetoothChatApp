@file:OptIn(ExperimentalComposeUiApi::class)

package com.lsw.myapplication

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
import com.lsw.myapplication.ui.theme.MyApplicationTheme
import androidx.compose.material3.Card as Card1

class MyMessage(val ath: String, val msg: String)

class FunkyWrapper(
    val lst: MutableState<List<MyMessage>> = mutableStateOf(arrayListOf<MyMessage>()),
    val txt: MutableState<String> = mutableStateOf(""),
    var in_height: MutableState<Dp> = mutableStateOf(0.dp),
    var all_height: MutableState<Dp> = mutableStateOf(0.dp)
)
{}

class MainActivity : ComponentActivity() {
    val funky = FunkyWrapper()

    //val test = arrayListOf<String>("a", "b");
    //var has_new = false;

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MyApplicationTheme {
                CombinedDraw()
            }
        }
    }

    fun postChat(author: String, message: String) {
        funky.lst.value += MyMessage(author, message)
        if (funky.lst.value.size > 200) funky.lst.value = funky.lst.value.drop(1)
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
                    funky.all_height.value = with (localDensity) { coordinates.size.height.toDp() }
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
        var mod = Modifier.fillMaxWidth().background(color = Color.LightGray).requiredHeight(h - 18.dp) // somehow this works

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

                        var comboed: String = "";
                        for (i in item.msg) {
                            comboed += (i.code).toString() + " "
                        }

                        Text(text = "VAL: ${comboed} | Limit: ${h}")
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
                /*postChat("DEBUG", newText)
                if (funky.txt.value.contains((10).toChar())) {
                    postChat("Myself", funky.txt.value.substring(0, funky.txt.value.length - 1))
                    funky.txt.value = "";
                }*/
            },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(36.dp)
                    .offset(0.dp, 4.dp)
                    .weight(1f)
                    .padding(horizontal = 2.dp)
                    .background(Color.Red)
                    .onGloballyPositioned { coordinates ->
                        funky.in_height.value = with (localDensity) { coordinates.size.height.toDp() }
                    }
                    /*.onKeyEvent {
                        postChat("DEBUG", it.key.toString())
                        if (it.key == Key.Enter || it.key == Key.NavigateNext || it.key == Key.Search) {
                            postChat("Myself", funky.txt.value.substring(0, funky.txt.value.length - 1))
                            funky.txt.value = "";
                            true
                        }
                        false
                    }*/,
                singleLine = true,
                keyboardActions = KeyboardActions(
                    onDone = {postChat("Myself", funky.txt.value); funky.txt.value = "";}
                )
            )
            Button(
                onClick = { postChat("Myself",funky.txt.value); funky.txt.value = ""; },
                modifier = Modifier
                    .padding(horizontal = 4.dp),
                shape = RoundedCornerShape(8.dp)

            ) {
                Text(text = "Send")
            }
        }
    }
}