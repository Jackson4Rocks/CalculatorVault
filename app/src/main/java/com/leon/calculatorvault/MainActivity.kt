package com.leon.calculatorvault

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.DecimalFormat
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE
        )
        setContent { CalculatorVaultApp() }
    }
}

private val Background = Color(0xFF090909)
private val CalculatorSurface = Color(0xFF171717)
private val Accent = Color(0xFF8FF5C7)

@Composable
private fun CalculatorVaultApp() {
    val context = LocalContext.current
    val prefs = remember {
        context.getSharedPreferences("vault_prefs", Context.MODE_PRIVATE)
    }
    var setup by remember { mutableStateOf(!PinStore.hasPin(prefs)) }
    var unlocked by remember { mutableStateOf(false) }
    var page by remember { mutableStateOf("calculator") }

    MaterialTheme(
        colorScheme = darkColorScheme(
            background = Background,
            surface = CalculatorSurface,
            primary = Accent,
            onPrimary = Color.Black
        )
    ) {
        Surface(Modifier.fillMaxSize(), color = Background) {
            when {
                setup -> SetupPinScreen {
                    setup = false
                    unlocked = false
                }

                unlocked && page == "vault" -> VaultScreen(
                    onBack = {
                        unlocked = false
                        page = "calculator"
                    },
                    onApps = { page = "apps" }
                )

                unlocked && page == "apps" -> AppsScreen(
                    onBack = { page = "vault" }
                )

                else -> CalculatorScreen(
                    onUnlock = {
                        if (PinStore.verify(prefs, it)) {
                            unlocked = true
                            page = "vault"
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun CalculatorScreen(onUnlock: (String) -> Unit) {
    val context = LocalContext.current
    var display by remember { mutableStateOf("0") }
    var expression by remember { mutableStateOf("") }

    fun press(key: String) {
        when (key) {
            "C" -> {
                expression = ""
                display = "0"
            }

            "⌫" -> {
                expression = expression.dropLast(1)
                display = expression.ifBlank { "0" }
            }

            "=" -> {
                val prefs = context.getSharedPreferences(
                    "vault_prefs",
                    Context.MODE_PRIVATE
                )
                val candidate = expression.trim()

                if (
                    candidate.length >= 4 &&
                    candidate.all(Char::isDigit) &&
                    PinStore.hasPin(prefs) &&
                    PinStore.verify(prefs, candidate)
                ) {
                    onUnlock(candidate)
                    return
                }

                display = runCatching {
                    CalculatorEngine.evaluate(expression.ifBlank { display })
                }.getOrDefault("Error")

                expression = if (display == "Error") "" else display
            }

            else -> {
                if (display == "Error") {
                    display = "0"
                    expression = ""
                }
                expression += key
                display = expression
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
            .padding(18.dp),
        verticalArrangement = Arrangement.Bottom
    ) {
        Text(
            text = display,
            modifier = Modifier.fillMaxWidth(),
            color = Color.White,
            fontSize = 48.sp,
            fontWeight = FontWeight.Light,
            textAlign = TextAlign.End,
            maxLines = 1
        )

        Spacer(Modifier.height(18.dp))

        val rows = listOf(
            listOf("C", "(", ")", "÷"),
            listOf("7", "8", "9", "×"),
            listOf("4", "5", "6", "−"),
            listOf("1", "2", "3", "+"),
            listOf("0", ".", "⌫", "=")
        )

        rows.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                row.forEach { key ->
                    CalculatorKey(
                        key = key,
                        modifier = Modifier.weight(1f),
                        onClick = { press(key) }
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
        }

        Text(
            text = "Calculator",
            color = Color.Transparent,
            fontSize = 1.sp
        )
    }
}

@Composable
private fun CalculatorKey(
    key: String,
    modifier: Modifier,
    onClick: () -> Unit
) {
    val operator = key in setOf("÷", "×", "−", "+", "=")

    Button(
        onClick = onClick,
        modifier = modifier.height(72.dp),
        shape = RoundedCornerShape(22.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (operator) Accent else CalculatorSurface,
            contentColor = if (operator) Color.Black else Color.White
        )
    ) {
        Text(key, fontSize = 25.sp)
    }
}

@Composable
private fun SetupPinScreen(onDone: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember {
        context.getSharedPreferences("vault_prefs", Context.MODE_PRIVATE)
    }
    var pin by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Calculator",
            fontSize = 34.sp,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(Modifier.height(8.dp))

        Text("Create your private PIN", color = Color.LightGray)

        Spacer(Modifier.height(20.dp))

        OutlinedTextField(
            value = pin,
            onValueChange = {
                if (it.length <= 12 && it.all(Char::isDigit)) {
                    pin = it
                }
            },
            label = { Text("PIN") },
            singleLine = true
        )

        Spacer(Modifier.height(10.dp))

        OutlinedTextField(
            value = confirm,
            onValueChange = {
                if (it.length <= 12 && it.all(Char::isDigit)) {
                    confirm = it
                }
            },
            label = { Text("Confirm PIN") },
            singleLine = true
        )

        if (error.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            Text(error, color = Color(0xFFFF8A80))
        }

        Spacer(Modifier.height(18.dp))

        Button(
            onClick = {
                when {
                    pin.length < 4 -> error = "Use at least 4 digits."
                    pin != confirm -> error = "PINs do not match."
                    else -> {
                        PinStore.setPin(prefs, pin)
                        Toast.makeText(
                            context,
                            "PIN saved",
                            Toast.LENGTH_SHORT
                        ).show()
                        onDone()
                    }
                }
            }
        ) {
            Text("Save PIN")
        }
    }
}

@Composable
private fun VaultScreen(
    onBack: () -> Unit,
    onApps: () -> Unit
) {
    val context = LocalContext.current
    val store = remember { VaultStore(context) }
    var files by remember { mutableStateOf(store.list()) }

    val picker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        for (uri in uris) {
            runCatching { store.importUri(uri) }
                .onFailure {
                    Toast.makeText(
                        context,
                        "Could not import file",
                        Toast.LENGTH_SHORT
                    ).show()
                }
        }
        files = store.list()
    }

    Column(Modifier.fillMaxSize().padding(18.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Vault",
                fontSize = 30.sp,
                fontWeight = FontWeight.SemiBold
            )

            TextButton(onClick = onBack) {
                Text("Lock")
            }
        }

        Spacer(Modifier.height(16.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(onClick = { picker.launch(arrayOf("*/*")) }) {
                Text("Import files")
            }

            OutlinedButton(onClick = onApps) {
                Text("Apps")
            }
        }

        Spacer(Modifier.height(18.dp))

        if (files.isEmpty()) {
            Text(
                "Your private files will appear here.",
                color = Color.LightGray
            )
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(files, key = { it.id }) { file ->
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = CalculatorSurface
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(
                                    file.name,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    file.sizeLabel,
                                    color = Color.LightGray,
                                    fontSize = 13.sp
                                )
                            }

                            TextButton(
                                onClick = {
                                    store.delete(file)
                                    files = store.list()
                                }
                            ) {
                                Text("Delete")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AppsScreen(onBack: () -> Unit) {
    val context = LocalContext.current

    val apps = remember {
        val query = Intent(Intent.ACTION_MAIN)
            .addCategory(Intent.CATEGORY_LAUNCHER)

        context.packageManager
            .queryIntentActivities(query, 0)
            .map { it.activityInfo.packageName }
            .distinct()
            .filter { it != context.packageName }
            .sorted()
    }

    Column(Modifier.fillMaxSize().padding(18.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Private Apps",
                fontSize = 30.sp,
                fontWeight = FontWeight.SemiBold
            )

            TextButton(onClick = onBack) {
                Text("Back")
            }
        }

        Spacer(Modifier.height(8.dp))

        Text(
            "These are private shortcuts. A normal app cannot universally remove other apps from the launcher.",
            color = Color.LightGray,
            fontSize = 13.sp
        )

        Spacer(Modifier.height(12.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(apps) { packageName ->
                val label = runCatching {
                    context.packageManager.getApplicationLabel(
                        context.packageManager.getApplicationInfo(
                            packageName,
                            0
                        )
                    ).toString()
                }.getOrDefault(packageName)

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            context.packageManager
                                .getLaunchIntentForPackage(packageName)
                                ?.let(context::startActivity)
                        },
                    colors = CardDefaults.cardColors(
                        containerColor = CalculatorSurface
                    )
                ) {
                    Text(
                        label,
                        modifier = Modifier.padding(16.dp),
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

private object CalculatorEngine {
    fun evaluate(raw: String): String {
        val normalized = raw
            .replace("×", "*")
            .replace("÷", "/")
            .replace("−", "-")
            .replace(" ", "")

        val value = Parser(normalized).parse()
        require(value.isFinite())

        return if (value % 1.0 == 0.0) {
            value.toLong().toString()
        } else {
            DecimalFormat("0.##########", Locale.US).format(value)
        }
    }

    private class Parser(private val source: String) {
        private var position = 0

        fun parse(): Double {
            require(source.isNotBlank())
            val value = expression()
            require(position == source.length)
            return value
        }

        private fun expression(): Double {
            var value = term()

            while (position < source.length) {
                when (source[position]) {
                    '+' -> {
                        position++
                        value += term()
                    }

                    '-' -> {
                        position++
                        value -= term()
                    }

                    else -> return value
                }
            }

            return value
        }

        private fun term(): Double {
            var value = factor()

            while (position < source.length) {
                when (source[position]) {
                    '*' -> {
                        position++
                        value *= factor()
                    }

                    '/' -> {
                        position++
                        val divisor = factor()
                        require(divisor != 0.0)
                        value /= divisor
                    }

                    else -> return value
                }
            }

            return value
        }

        private fun factor(): Double {
            require(position < source.length)

            return when (source[position]) {
                '(' -> {
                    position++
                    val value = expression()
                    require(position < source.length && source[position] == ')')
                    position++
                    value
                }

                '-' -> {
                    position++
                    -factor()
                }

                else -> number()
            }
        }

        private fun number(): Double {
            val start = position

            while (
                position < source.length &&
                (source[position].isDigit() || source[position] == '.')
            ) {
                position++
            }

            require(start != position)
            return source.substring(start, position).toDouble()
        }
    }
}
