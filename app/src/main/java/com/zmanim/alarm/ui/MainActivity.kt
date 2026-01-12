package com.zmanim.alarm.ui

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zmanim.alarm.data.model.AlarmProviderType
import com.zmanim.alarm.ui.theme.ZmanimAlarmTheme
import com.zmanim.alarm.util.AlarmProviderUtil
import dagger.hilt.android.AndroidEntryPoint
import java.time.format.DateTimeFormatter

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        ) {
            viewModel.onLocationPermissionGranted()
        }
    }

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            viewModel.onNotificationPermissionGranted()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            ZmanimAlarmTheme {
                MainScreen(
                    viewModel = viewModel,
                    onRequestLocationPermission = { requestLocationPermission() },
                    onRequestNotificationPermission = { requestNotificationPermission() },
                    onRequestExactAlarmPermission = { requestExactAlarmPermission() }
                )
            }
        }
    }

    private fun requestLocationPermission() {
        locationPermissionLauncher.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    private fun requestExactAlarmPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                data = Uri.parse("package:$packageName")
            }
            startActivity(intent)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: MainViewModel,
    onRequestLocationPermission: () -> Unit,
    onRequestNotificationPermission: () -> Unit,
    onRequestExactAlarmPermission: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val alarmSettings by viewModel.alarmSettings.collectAsState()
    val canScheduleExactAlarms = viewModel.checkExactAlarmPermission()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Zmanim Alarm") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Exact Alarm Permission Warning
            if (!canScheduleExactAlarms) {
                PermissionWarningCard(
                    icon = Icons.Filled.Notifications,
                    title = "Exact Alarm Permission Required",
                    description = "This app needs permission to schedule exact alarms for accurate Zmanim notifications.",
                    buttonText = "Grant Permission",
                    onButtonClick = onRequestExactAlarmPermission
                )
            }

            when (val state = uiState) {
                is MainUiState.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }

                is MainUiState.Error -> {
                    ErrorCard(
                        message = state.message,
                        onRetry = { viewModel.loadZmanimData() },
                        onRequestLocationPermission = onRequestLocationPermission
                    )
                }

                is MainUiState.Success -> {
                    // Zmanim Display Card
                    ZmanimCard(
                        zmanimData = state.zmanimData
                    )

                    // Provider Selector Card
                    ProviderSelectorCard(
                        selectedProvider = alarmSettings.alarmProviderType,
                        onProviderChanged = { viewModel.updateAlarmProvider(it) },
                        onSyncToSleepAsAndroid = { viewModel.manualSyncToSleepAsAndroid() }
                    )

                    // Alarm Control Card
                    AlarmControlCard(
                        alarmSettings = alarmSettings,
                        nextAlarmTime = state.nextAlarmTime,
                        onToggleAlarm = { viewModel.toggleAlarm(it) },
                        onMinutesChanged = { viewModel.updateMinutesBefore(it) }
                    )
                }
            }
        }
    }
}

@Composable
fun ZmanimCard(zmanimData: com.zmanim.alarm.data.model.ZmanimData) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Today's Sof Zman Kriyat Shema",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )

            Divider()

            zmanimData.sofZmanShmaMGA?.let { time ->
                ZmanRow(
                    label = "Magen Avraham",
                    time = time.format(DateTimeFormatter.ofPattern("HH:mm"))
                )
            }

            zmanimData.sofZmanShmaGRA?.let { time ->
                ZmanRow(
                    label = "Gra",
                    time = time.format(DateTimeFormatter.ofPattern("HH:mm"))
                )
            }

            zmanimData.location.locationName?.let { location ->
                Text(
                    text = "📍 $location",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
    }
}

@Composable
fun ZmanRow(label: String, time: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = time,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
fun AlarmControlCard(
    alarmSettings: com.zmanim.alarm.data.model.AlarmSettings,
    nextAlarmTime: java.time.ZonedDateTime?,
    onToggleAlarm: (Boolean) -> Unit,
    onMinutesChanged: (Int) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Alarm Settings",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )

            Divider()

            // Alarm Toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Enable Zmanim Alarm",
                    fontSize = 16.sp
                )
                Switch(
                    checked = alarmSettings.isEnabled,
                    onCheckedChange = onToggleAlarm
                )
            }

            // Minutes Before Slider
            Column {
                Text(
                    text = "Minutes Before SZKS: ${alarmSettings.minutesBefore}",
                    fontSize = 16.sp
                )
                Slider(
                    value = alarmSettings.minutesBefore.toFloat(),
                    onValueChange = { onMinutesChanged(it.toInt()) },
                    valueRange = 5f..60f,
                    steps = 10,
                    enabled = alarmSettings.isEnabled
                )
            }

            // Next Alarm Display
            if (alarmSettings.isEnabled && nextAlarmTime != null) {
                Divider()
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Next Alarm",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = nextAlarmTime.format(
                            DateTimeFormatter.ofPattern("EEEE, MMM d 'at' HH:mm")
                        ),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
fun PermissionWarningCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String,
    buttonText: String,
    onButtonClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onErrorContainer
                )
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
            Text(
                text = description,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
            Button(
                onClick = onButtonClick,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(buttonText)
            }
        }
    }
}

@Composable
fun ProviderSelectorCard(
    selectedProvider: AlarmProviderType,
    onProviderChanged: (AlarmProviderType) -> Unit,
    onSyncToSleepAsAndroid: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val isSleepAsAndroidInstalled = AlarmProviderUtil.isSleepAsAndroidInstalled(context)

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Alarm Provider",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )

            Divider()

            // Internal Alarm Option
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = selectedProvider == AlarmProviderType.INTERNAL,
                    onClick = { onProviderChanged(AlarmProviderType.INTERNAL) }
                )
                Column(modifier = Modifier.padding(start = 8.dp)) {
                    Text(
                        text = "Internal Alarm",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "Use built-in alarm system",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Sleep as Android Option
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = selectedProvider == AlarmProviderType.SLEEP_AS_ANDROID,
                    onClick = {
                        if (isSleepAsAndroidInstalled) {
                            onProviderChanged(AlarmProviderType.SLEEP_AS_ANDROID)
                        }
                    },
                    enabled = isSleepAsAndroidInstalled
                )
                Column(modifier = Modifier.padding(start = 8.dp)) {
                    Text(
                        text = "Sleep as Android",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (isSleepAsAndroidInstalled)
                            MaterialTheme.colorScheme.onSurface
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = if (isSleepAsAndroidInstalled)
                            "Sync to Sleep as Android app"
                        else
                            "Not installed",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Warning/Info based on selection
            if (!isSleepAsAndroidInstalled && selectedProvider == AlarmProviderType.SLEEP_AS_ANDROID) {
                Divider()
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = MaterialTheme.shapes.small
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "⚠️ Sleep as Android not found",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Text(
                            text = "Falling back to Internal Alarm. Install Sleep as Android to use this feature.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Button(
                            onClick = { AlarmProviderUtil.openSleepAsAndroidInPlayStore(context) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Install Sleep as Android")
                        }
                    }
                }
            }

            // Manual Sync Button (only show when Sleep as Android is selected and installed)
            if (selectedProvider == AlarmProviderType.SLEEP_AS_ANDROID && isSleepAsAndroidInstalled) {
                Divider()
                OutlinedButton(
                    onClick = onSyncToSleepAsAndroid,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Sync Now to Sleep as Android")
                }
            }
        }
    }
}

@Composable
fun ErrorCard(
    message: String,
    onRetry: () -> Unit,
    onRequestLocationPermission: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Error",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
            Text(
                text = message,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(onClick = onRetry) {
                    Text("Retry")
                }
                if (message.contains("Location", ignoreCase = true)) {
                    OutlinedButton(onClick = onRequestLocationPermission) {
                        Text("Enable Location")
                    }
                }
            }
        }
    }
}
