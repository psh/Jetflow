package com.wwt.jetflow

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Card
import androidx.compose.material.Divider
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun ScreenLayout(title: String, content: @Composable ColumnScope.() -> Unit = {}) {
    Column(Modifier.fillMaxSize()) {
        Text(
            text = title,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(16.dp),
            style = MaterialTheme.typography.h5
        )
        Divider()
        content()
    }
}

@Composable
fun SubScreenLayout(title: String, content: @Composable ColumnScope.() -> Unit = {}) {
    val scrollState = rememberScrollState()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 32.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = title,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.h6
        )
        content()
    }
}

@Composable
fun DialogLayout(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            Modifier.padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = title,
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.h6
            )
            content()
        }

    }
}

@Composable
fun CenteredText(text: String) {
    Text(text, textAlign = TextAlign.Center)
}