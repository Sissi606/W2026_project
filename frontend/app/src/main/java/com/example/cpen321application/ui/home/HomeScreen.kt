package com.example.cpen321application.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.cpen321application.R

/**
 * The three top-level buttons. Buttons 2 and 3 are placeholders for now, shown
 * disabled rather than hidden so the layout matches the required screen.
 */
@Composable
fun HomeScreen(
    onButtonOneClick: () -> Unit,
    onButtonTwoClick: () -> Unit,
    onButtonThreeClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(R.string.home_title),
            style = MaterialTheme.typography.headlineMedium
        )
        Text(
            text = stringResource(R.string.home_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        HomeButton(
            label = stringResource(R.string.button_one),
            onClick = onButtonOneClick,
            enabled = true,
            testTag = "button_one"
        )
        HomeButton(
            label = stringResource(R.string.button_two),
            onClick = onButtonTwoClick,
            enabled = true,
            testTag = "button_two"
        )
        HomeButton(
            label = stringResource(R.string.button_three),
            onClick = onButtonThreeClick,
            enabled = true,
            testTag = "button_three"
        )
    }
}

@Composable
private fun HomeButton(
    label: String,
    onClick: () -> Unit,
    enabled: Boolean,
    testTag: String
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .semantics { contentDescription = testTag }
    ) {
        Text(text = label, style = MaterialTheme.typography.titleMedium)
    }
}
