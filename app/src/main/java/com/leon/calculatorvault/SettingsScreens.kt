package com.leon.calculatorvault

import android.content.Context
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val SettingsSurface = Color(0xFF171717)
private val SettingsAccent = Color(0xFF8FF5C7)

@Composable
fun CalculatorSettingsScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val prefs = remember {
        context.getSharedPreferences("calculator_settings", Context.MODE_PRIVATE)
    }

    var vibration by remember {
        mutableStateOf(prefs.getBoolean("button_vibration", true))
    }
    var sounds by remember {
        mutableStateOf(prefs.getBoolean("button_sounds", false))
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp)
    ) {
        SettingsHeader("Calculator settings", onBack)

        Spacer(Modifier.height(18.dp))

        SettingsSection("Calculator") {
            SettingsSwitch(
                title = "Button vibration",
                description = "Vibrate when calculator buttons are pressed.",
                checked = vibration,
                onCheckedChange = {
                    vibration = it
                    prefs.edit().putBoolean("button_vibration", it).apply()
                }
            )

            HorizontalDivider()

            SettingsSwitch(
                title = "Button sounds",
                description = "Play a small sound for calculator input.",
                checked = sounds,
                onCheckedChange = {
                    sounds = it
                    prefs.edit().putBoolean("button_sounds", it).apply()
                }
            )
        }

        Spacer(Modifier.height(14.dp))

        SettingsSection("Appearance") {
            SettingsInfo(
                title = "Theme",
                description = "Dark theme",
                value = "Dark"
            )
            SettingsInfo(
                title = "Accent",
                description = "Calculator accent color",
                value = "#8FF5C7"
            )
        }

        Spacer(Modifier.height(14.dp))

        SettingsSection("Private space") {
            SettingsInfo(
                title = "PIN settings",
                description = "PIN changes are available only after entering the private space.",
                value = "Protected"
            )
        }

        Spacer(Modifier.height(14.dp))

        SettingsSection("About") {
            SettingsInfo(
                title = "CalculatorVault",
                description = "Privacy-focused calculator and encrypted vault.",
                value = "0.1.0"
            )
        }
    }
}

@Composable
fun PrivateSettingsScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val store = remember { VaultStore(context) }
    var showChangePin by remember { mutableStateOf(false) }
    var filesEncrypted by remember { mutableStateOf(store.list().size) }

    val encryptPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        var successful = 0
        for (uri in uris) {
            runCatching {
                store.importAndEncryptUri(uri)
                successful++
            }
        }
        filesEncrypted = store.list().size

        Toast.makeText(
            context,
            if (successful == 1) {
                "File encrypted with AES-256-GCM"
            } else {
                "$successful files encrypted with AES-256-GCM"
            },
            Toast.LENGTH_SHORT
        ).show()
    }

    if (showChangePin) {
        ChangePinScreen(
            onBack = { showChangePin = false },
            onChanged = {
                showChangePin = false
                Toast.makeText(context, "Private PIN changed", Toast.LENGTH_SHORT).show()
            }
        )
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp)
    ) {
        SettingsHeader("Private space settings", onBack)

        Spacer(Modifier.height(18.dp))

        SettingsSection("Security") {
            SettingsAction(
                title = "Change private PIN",
                description = "Requires the current PIN before the new PIN is saved.",
                onClick = { showChangePin = true }
            )
        }

        Spacer(Modifier.height(14.dp))

        SettingsSection("AES encryption") {
            SettingsInfo(
                title = "Encryption",
                description = "Every vault import is encrypted before it is stored.",
                value = "AES-256-GCM"
            )

            HorizontalDivider()

            SettingsInfo(
                title = "Key protection",
                description = "Encryption key is stored in Android Keystore.",
                value = if (store.isEncryptionReady()) "Ready" else "Not created yet"
            )

            HorizontalDivider()

            SettingsAction(
                title = "Encrypt a file",
                description = "Choose a file and store an AES-256-GCM encrypted copy in the private vault.",
                onClick = {
                    store.ensureEncryptionReady()
                    encryptPicker.launch(arrayOf("*/*"))
                }
            )
        }

        Spacer(Modifier.height(14.dp))

        SettingsSection("Vault") {
            SettingsInfo(
                title = "Encrypted files",
                description = "Files currently stored in the private vault.",
                value = filesEncrypted.toString()
            )

            HorizontalDivider()

            SettingsInfo(
                title = "Storage",
                description = "Private app storage",
                value = "Internal"
            )
        }

        Spacer(Modifier.height(14.dp))

        SettingsSection("Protection") {
            SettingsInfo(
                title = "Screenshots",
                description = "Private screens are protected by Android FLAG_SECURE.",
                value = "Protected"
            )
        }
    }
}

@Composable
private fun SettingsHeader(
    title: String,
    onBack: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            title,
            fontSize = 28.sp,
            fontWeight = FontWeight.SemiBold
        )

        TextButton(onClick = onBack) {
            Text("Back")
        }
    }
}

@Composable
private fun SettingsSection(
    title: String,
    content: @Composable () -> Unit
) {
    Column {
        Text(
            title,
            color = SettingsAccent,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(Modifier.height(6.dp))

        Card(
            colors = CardDefaults.cardColors(
                containerColor = SettingsSurface
            )
        ) {
            Column(Modifier.fillMaxWidth()) {
                content()
            }
        }
    }
}

@Composable
private fun SettingsSwitch(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(3.dp))
            Text(description, color = Color.LightGray, fontSize = 13.sp)
        }

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

@Composable
private fun SettingsInfo(
    title: String,
    description: String,
    value: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(3.dp))
            Text(description, color = Color.LightGray, fontSize = 13.sp)
        }

        Text(
            value,
            color = SettingsAccent,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun SettingsAction(
    title: String,
    description: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(3.dp))
            Text(description, color = Color.LightGray, fontSize = 13.sp)
        }

        Text(
            "›",
            color = SettingsAccent,
            fontSize = 28.sp
        )
    }
}

@Composable
private fun ChangePinScreen(
    onBack: () -> Unit,
    onChanged: () -> Unit
) {
    val context = LocalContext.current
    val prefs = remember {
        context.getSharedPreferences("vault_prefs", Context.MODE_PRIVATE)
    }

    var current by remember { mutableStateOf("") }
    var next by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp)
    ) {
        SettingsHeader("Change private PIN", onBack)

        Spacer(Modifier.height(20.dp))

        OutlinedTextField(
            value = current,
            onValueChange = {
                if (it.length <= 12 && it.all(Char::isDigit)) current = it
            },
            label = { Text("Current PIN") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(10.dp))

        OutlinedTextField(
            value = next,
            onValueChange = {
                if (it.length <= 12 && it.all(Char::isDigit)) next = it
            },
            label = { Text("New PIN") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(10.dp))

        OutlinedTextField(
            value = confirm,
            onValueChange = {
                if (it.length <= 12 && it.all(Char::isDigit)) confirm = it
            },
            label = { Text("Confirm new PIN") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        if (error.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            Text(error, color = Color(0xFFFF8A80))
        }

        Spacer(Modifier.height(18.dp))

        Button(
            onClick = {
                when {
                    !PinStore.verify(prefs, current) ->
                        error = "Current PIN is incorrect."

                    next.length < 4 ->
                        error = "New PIN must be at least 4 digits."

                    next != confirm ->
                        error = "New PINs do not match."

                    current == next ->
                        error = "Choose a different PIN."

                    else -> {
                        PinStore.setPin(prefs, next)
                        onChanged()
                    }
                }
            }
        ) {
            Text("Save new PIN")
        }
    }
}
