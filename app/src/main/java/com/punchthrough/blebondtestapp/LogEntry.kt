package com.punchthrough.blebondtestapp

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.Divider
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import timber.log.Timber
import java.time.format.DateTimeFormatter
import java.util.*

private val dateFormatter = DateTimeFormatter.ofPattern("MMM d, HH:mm:ss", Locale.US)

enum class LogLevel {
    DEBUG, WARN, ERROR
}

data class LogEntry(
    val message: String,
    val level: LogLevel,
    val millisTimestamp: Long = System.currentTimeMillis()
) {
    fun formatted(): String = "${dateFormatter.dateFormatted(millisTimestamp)}: $message"
}

fun SnapshotStateList<LogEntry>.log(message: String, level: LogLevel) {
    when (level) {
        LogLevel.DEBUG -> Timber.d(message)
        LogLevel.WARN -> Timber.w(message)
        LogLevel.ERROR -> Timber.e(message)
    }
    this += LogEntry(message, level)
}

@Composable
fun LogTable(logEntries: List<LogEntry>) {
    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            "Log",
            modifier = Modifier.padding(bottom = 8.dp),
            style = MaterialTheme.typography.h5
        )
        Divider(color = MaterialTheme.colors.onSurface)
        LazyColumn(
            state = listState,
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(items = logEntries) {
                Text(
                    text = it.formatted(),
                    color = when (it.level) {
                        LogLevel.DEBUG -> MaterialTheme.colors.onSurface
                        LogLevel.WARN -> MaterialTheme.colors.primary
                        LogLevel.ERROR -> MaterialTheme.colors.error
                    }
                )
                LaunchedEffect(logEntries) {
                    coroutineScope.launch {
                        listState.animateScrollToItem(logEntries.lastIndex)
                    }
                }
            }
        }
    }
}
