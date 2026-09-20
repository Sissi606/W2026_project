package com.example.cpen321application.ui.login

import android.app.Activity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.cpen321application.R
import com.example.cpen321application.data.repository.BackendInfo
import com.example.cpen321application.data.remote.UserDto

@Composable
fun LoginScreen(
    state: LoginUiState,
    onSignIn: (Activity) -> Unit,
    onSignOut: () -> Unit,
    onDismissError: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Credential Manager renders a system bottom sheet, which needs the hosting
    // Activity rather than the application context.
    val activity = LocalContext.current as Activity

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = stringResource(R.string.login_title),
            style = MaterialTheme.typography.headlineSmall
        )

        when {
            state.isLoading -> LoadingBlock()

            state.isSignedIn -> {
                // Both non-null whenever isSignedIn is true.
                val user = state.signedInUser
                val info = state.info
                if (user != null && info != null) {
                    InfoCard(
                        user = user,
                        info = info,
                        clientTime = state.clientTime.orEmpty()
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = { onSignIn(activity) },
                            modifier = Modifier.weight(1f)
                        ) { Text(stringResource(R.string.refresh)) }

                        Button(
                            onClick = onSignOut,
                            modifier = Modifier
                                .weight(1f)
                                .semantics { contentDescription = "sign_out" }
                        ) { Text(stringResource(R.string.sign_out)) }
                    }
                }
            }

            else -> SignedOutBlock(
                errorMessage = state.errorMessage,
                onSignIn = { onSignIn(activity) },
                onDismissError = onDismissError
            )
        }

        TextButton(onClick = onBack) { Text(stringResource(R.string.back)) }
    }
}

@Composable
private fun LoadingBlock() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        CircularProgressIndicator()
        Text(
            text = stringResource(R.string.signing_in),
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
private fun SignedOutBlock(
    errorMessage: String?,
    onSignIn: () -> Unit,
    onDismissError: () -> Unit
) {
    Text(
        text = stringResource(R.string.sign_in_prompt),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )

    if (errorMessage != null) {
        ErrorCard(message = errorMessage, onDismiss = onDismissError)
    }

    Button(
        onClick = onSignIn,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .semantics { contentDescription = "sign_in_button" }
    ) {
        Text(
            text = stringResource(R.string.sign_in_with_google),
            style = MaterialTheme.typography.titleMedium
        )
    }
}

@Composable
private fun ErrorCard(message: String, onDismiss: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
            TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.End)) {
                Text(stringResource(R.string.dismiss))
            }
        }
    }
}

/**
 * The six required values, grouped so the screen reads at a glance rather than
 * as a wall of text. Grouping is presentational only -- each value still comes
 * from the endpoint it belongs to.
 */
@Composable
private fun InfoCard(user: UserDto, info: BackendInfo, clientTime: String) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            SectionHeader(stringResource(R.string.section_connection))
            LabeledValue(stringResource(R.string.label_server_ip), info.serverIp)
            LabeledValue(stringResource(R.string.label_client_ip), info.clientIp)

            SectionDivider()
            SectionHeader(stringResource(R.string.section_clocks))
            LabeledValue(stringResource(R.string.label_server_time), info.serverTime)
            LabeledValue(stringResource(R.string.label_client_time), clientTime)

            SectionDivider()
            SectionHeader(stringResource(R.string.section_identity))
            LabeledValue(
                stringResource(R.string.label_developer_name),
                "${info.developerFirstName} ${info.developerLastName}"
            )
            LabeledValue(
                stringResource(R.string.label_account_name),
                "${user.firstName} ${user.lastName}".trim().ifBlank { user.email }
            )
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(bottom = 4.dp)
    )
}

@Composable
private fun SectionDivider() {
    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
}

/**
 * Label above value, rather than side by side: IPv6 addresses and long names
 * would otherwise be squeezed into half the width and wrap awkwardly.
 */
@Composable
private fun LabeledValue(label: String, value: String) {
    Column(modifier = Modifier.padding(bottom = 10.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            fontFamily = FontFamily.Monospace,
            textAlign = TextAlign.Start,
            modifier = Modifier.semantics { contentDescription = "value_$label" }
        )
    }
}
