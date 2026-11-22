package com.example.ui.auth

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Store
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import java.util.Locale

object AgentAuthConfig {
    const val DEMO_AGENT_PIN = "2026"
    val STORE_BRANCHES = listOf(
        "Sanu Flagship · Downtown",
        "Sanu Outlet · Westside",
        "Sanu Warehouse Hub · North"
    )
    val DEMO_AGENTS = listOf(
        "AGT-104" to "Sanu Flagship · Downtown",
        "AGT-208" to "Sanu Warehouse Hub · North"
    )

    fun validateAgentCredentials(badgeId: String, pin: String): String? {
        val cleanBadge = badgeId.trim().uppercase(Locale.US)
        if (cleanBadge.length < 3) {
            return "Enter a valid Agent Badge ID (at least 3 characters, e.g. AGT-104)."
        }
        if (pin.trim() != DEMO_AGENT_PIN) {
            return "Invalid Store Security PIN. Demo PIN is $DEMO_AGENT_PIN."
        }
        return null
    }
}

@Composable
fun AgentCredentialsForm(
    badgeId: String,
    onBadgeIdChange: (String) -> Unit,
    pin: String,
    onPinChange: (String) -> Unit,
    branch: String,
    onBranchChange: (String) -> Unit,
    validationError: String?,
    modifier: Modifier = Modifier
) {
    var showPin by remember { mutableStateOf(false) }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Surface(
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Demo Agent Credentials",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Badges: AGT-104 / AGT-208  ·  PIN: ${AgentAuthConfig.DEMO_AGENT_PIN}",
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                    TextButton(
                        onClick = {
                            onBadgeIdChange("AGT-104")
                            onPinChange(AgentAuthConfig.DEMO_AGENT_PIN)
                            onBranchChange(AgentAuthConfig.STORE_BRANCHES.first())
                        },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        modifier = Modifier.testTag("fill_demo_agent_button")
                    ) {
                        Text("Quick Fill")
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AgentAuthConfig.DEMO_AGENTS.forEach { (demoBadge, demoBranch) ->
                        AssistChip(
                            onClick = {
                                onBadgeIdChange(demoBadge)
                                onPinChange(AgentAuthConfig.DEMO_AGENT_PIN)
                                onBranchChange(demoBranch)
                            },
                            label = { Text(demoBadge) },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Filled.Badge,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        )
                    }
                }
            }
        }

        OutlinedTextField(
            value = badgeId,
            onValueChange = { onBadgeIdChange(it.uppercase(Locale.US)) },
            label = { Text("Agent Badge ID") },
            placeholder = { Text("AGT-104") },
            leadingIcon = { Icon(Icons.Filled.Badge, contentDescription = null) },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("agent_badge_input")
        )

        OutlinedTextField(
            value = pin,
            onValueChange = { onPinChange(it.filter(Char::isDigit).take(8)) },
            label = { Text("Store Security PIN") },
            placeholder = { Text(AgentAuthConfig.DEMO_AGENT_PIN) },
            leadingIcon = { Icon(Icons.Filled.Lock, contentDescription = null) },
            trailingIcon = {
                IconButton(onClick = { showPin = !showPin }) {
                    Icon(
                        imageVector = if (showPin) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                        contentDescription = if (showPin) "Hide PIN" else "Show PIN"
                    )
                }
            },
            visualTransformation = if (showPin) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("agent_pin_input")
        )

        Text(
            text = "Store / Warehouse Branch",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            AgentAuthConfig.STORE_BRANCHES.forEach { branchOption ->
                FilterChip(
                    selected = branch == branchOption,
                    onClick = { onBranchChange(branchOption) },
                    label = { Text(branchOption) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Filled.Store,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                )
            }
        }

        if (validationError != null) {
            Surface(
                color = MaterialTheme.colorScheme.errorContainer,
                shape = MaterialTheme.shapes.small,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = validationError,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(10.dp)
                )
            }
        }
    }
}

@Composable
fun AgentLoginDialog(
    initialBranch: String,
    onDismiss: () -> Unit,
    onAgentSignIn: (badgeId: String, pin: String, branch: String) -> Boolean
) {
    var badgeId by remember { mutableStateOf("") }
    var pin by remember { mutableStateOf("") }
    var branch by remember {
        mutableStateOf(initialBranch.ifBlank { AgentAuthConfig.STORE_BRANCHES.first() })
    }
    var validationError by remember { mutableStateOf<String?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surfaceContainerLowest,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.Badge,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(26.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text("Store Agent Login", style = MaterialTheme.typography.titleLarge)
                        Text(
                            text = "Activate inventory, QR restock & analytics access",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                AgentCredentialsForm(
                    badgeId = badgeId,
                    onBadgeIdChange = {
                        badgeId = it
                        validationError = null
                    },
                    pin = pin,
                    onPinChange = {
                        pin = it
                        validationError = null
                    },
                    branch = branch,
                    onBranchChange = { branch = it },
                    validationError = validationError
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancel")
                    }
                    Button(
                        onClick = {
                            val err = AgentAuthConfig.validateAgentCredentials(badgeId, pin)
                            if (err != null) {
                                validationError = err
                                return@Button
                            }
                            if (onAgentSignIn(badgeId, pin, branch)) {
                                onDismiss()
                            }
                        },
                        modifier = Modifier
                            .weight(1.2f)
                            .height(48.dp)
                            .testTag("dialog_agent_login_submit_button")
                    ) {
                        Text("Sign In as Agent")
                    }
                }
            }
        }
    }
}
