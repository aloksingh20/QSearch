package com.example.yourassistant

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.SaverScope
import androidx.compose.runtime.saveable.rememberSaveable

class ConversationSaver : Saver<MutableList<ConversationItem>, List<ConversationItem>> {
    override fun restore(value: List<ConversationItem>): MutableList<ConversationItem> =
        value.toMutableList()

    override fun SaverScope.save(value: MutableList<ConversationItem>): List<ConversationItem> =
        value.toList()
}

@Composable
fun rememberSaveable(
    initialValue: MutableList<ConversationItem>,
    saver: Saver<MutableList<ConversationItem>, List<ConversationItem>>
): MutableState<MutableList<ConversationItem>> {
    return rememberSaveable(saver) { mutableStateOf(initialValue) }
}
