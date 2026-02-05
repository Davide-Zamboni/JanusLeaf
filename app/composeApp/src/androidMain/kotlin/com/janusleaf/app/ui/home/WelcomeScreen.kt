package com.janusleaf.app.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import com.janusleaf.app.ui.theme.JanusLeafTheme

@Composable
fun WelcomeScreen(
    userEmail: String?,
    onSignOut: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = "🍃", style = MaterialTheme.typography.displayLarge)
        Spacer(modifier = Modifier.size(12.dp))
        Text(
            text = "Welcome to JanusLeaf!",
            style = MaterialTheme.typography.headlineLarge,
            textAlign = TextAlign.Center
        )
        if (!userEmail.isNullOrBlank()) {
            Text(
                text = userEmail,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.size(12.dp))
        Text(
            text = "You're successfully logged in.\nJournal features are ready for you.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.size(20.dp))
        Button(onClick = onSignOut) {
            Text("Sign Out")
        }
    }
}

@Preview
@Composable
private fun WelcomeScreenPreview() {
    JanusLeafTheme {
        WelcomeScreen(userEmail = "maya@janusleaf.app", onSignOut = {})
    }
}
