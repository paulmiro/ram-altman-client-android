package de.paulmiro.ramaltman

import android.content.SharedPreferences
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import kotlin.reflect.KMutableProperty0

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val settings = Settings(this)
        setContent {
            val context = LocalContext.current
            val colors = if (isSystemInDarkTheme()) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            MaterialTheme(colorScheme = colors) {
                SettingsScreen(settings)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(settings: Settings) {
    var enabled by remember { mutableStateOf(settings.enabled) }
    var status by remember { mutableStateOf(settings.status) }

    // The worker writes its result into the prefs; mirror it here while the screen is open.
    DisposableEffect(settings) {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == "status") status = settings.status
        }
        settings.prefs.registerOnSharedPreferenceChangeListener(listener)
        onDispose { settings.prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }

    Scaffold(topBar = { TopAppBar(title = { Text("RAM Altman") }) }) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            ListItem(
                headlineContent = { Text("Report memory usage") },
                supportingContent = { if (status.isNotEmpty()) Text("Last report: $status") },
                trailingContent = {
                    Switch(checked = enabled, onCheckedChange = {
                        enabled = it
                        settings.enabled = it
                        settings.schedule()
                    })
                },
            )
            TextSetting("Server URL", settings::serverUrl, KeyboardType.Uri)
            TextSetting("API token", settings::token, KeyboardType.Password, secret = true)
            TextSetting("Username", settings::username)
            TextSetting("Hostname", settings::hostname)
            TextSetting("Color (optional, #rrggbb)", settings::color)

            NumberSetting(
                label = "Interval (minutes)",
                hint = "Minimum $MIN_INTERVAL_MINUTES",
                range = MIN_INTERVAL_MINUTES..Int.MAX_VALUE,
                setting = settings::intervalMinutes,
                onSaved = settings::schedule,
            )
            NumberSetting(
                label = "Timeout (minutes)",
                hint = "The dashboard drops this device after this long without a report ($MIN_TIMEOUT_MINUTES-$MAX_TIMEOUT_MINUTES)",
                range = MIN_TIMEOUT_MINUTES..MAX_TIMEOUT_MINUTES,
                setting = settings::timeoutMinutes,
            )
        }
    }
}

/** A number field that saves into [setting] only while the value is inside [range]. */
@Composable
fun NumberSetting(
    label: String,
    hint: String,
    range: IntRange,
    setting: KMutableProperty0<Int>,
    onSaved: () -> Unit = {},
) {
    var text by remember { mutableStateOf(setting.get().toString()) }
    OutlinedTextField(
        value = text,
        onValueChange = {
            text = it
            val number = it.toIntOrNull()
            if (number != null && number in range) {
                setting.set(number)
                onSaved()
            }
        },
        label = { Text(label) },
        supportingText = { Text(hint) },
        isError = text.toIntOrNull() !in range,
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = Modifier.fillMaxWidth(),
    )
}

/** A text field that saves every change straight into [setting]. */
@Composable
fun TextSetting(
    label: String,
    setting: KMutableProperty0<String>,
    keyboardType: KeyboardType = KeyboardType.Text,
    secret: Boolean = false,
) {
    var value by remember { mutableStateOf(setting.get()) }
    OutlinedTextField(
        value = value,
        onValueChange = { value = it; setting.set(it) },
        label = { Text(label) },
        singleLine = true,
        visualTransformation = if (secret) PasswordVisualTransformation() else VisualTransformation.None,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        modifier = Modifier.fillMaxWidth(),
    )
}
