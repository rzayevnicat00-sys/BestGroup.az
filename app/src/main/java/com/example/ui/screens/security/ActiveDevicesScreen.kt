package com.example.ui.screens.security

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.DeviceSession
import com.example.ui.theme.BirbankRed
import com.example.ui.theme.BirbankRedLight
import com.example.ui.theme.BirbankTextPrimary
import com.example.ui.theme.BirbankTextSecondary
import com.example.viewmodel.MainViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActiveDevicesScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val activeSessions by viewModel.activeDeviceSessions.collectAsState()
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var sessionToRevoke by remember { mutableStateOf<DeviceSession?>(null) }
    var showRevokeAllOtherDialog by remember { mutableStateOf(false) }
    var isRevoking by remember { mutableStateOf(false) }

    val otherDevicesCount = activeSessions.count { !it.isCurrentDevice }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Aktiv Cihazlar",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Geri"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Information Card
            Surface(
                color = BirbankRedLight,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Devices,
                        contentDescription = null,
                        tint = BirbankRed,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = "Hesabınıza qoşulmuş cihazların siyahısı. Tanımadığınız cihazı dərhal buradan bağlaya bilərsiniz.",
                        style = MaterialTheme.typography.bodySmall,
                        color = BirbankTextPrimary,
                        lineHeight = 18.sp
                    )
                }
            }

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(activeSessions, key = { it.sessionId }) { session ->
                    DeviceSessionCard(
                        session = session,
                        onRevokeClick = { sessionToRevoke = session }
                    )
                }

                if (activeSessions.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 40.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Aktiv cihaz məlumatı yüklənir...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = BirbankTextSecondary
                            )
                        }
                    }
                }
            }

            // Bottom action button: Revoke all other devices
            if (otherDevicesCount > 0) {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 6.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Button(
                        onClick = { showRevokeAllOtherDialog = true },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                            .testTag("btn_revoke_all_other_devices")
                    ) {
                        Icon(
                            imageVector = Icons.Default.PowerSettingsNew,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Digər bütün cihazları bağla ($otherDevicesCount)",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }

    // Single Session Revoke Confirmation Dialog
    if (sessionToRevoke != null) {
        val s = sessionToRevoke!!
        AlertDialog(
            onDismissRequest = { if (!isRevoking) sessionToRevoke = null },
            title = { Text("Sessiyanı bitirmək istəyirsiniz?", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    text = "'${s.deviceName}' cihazındakı aktiv sessiyanız ləğv ediləcək və həmin cihazdan çıxış ediləcək.",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        isRevoking = true
                        viewModel.revokeDeviceSession(
                            sessionId = s.sessionId,
                            onSuccess = {
                                isRevoking = false
                                sessionToRevoke = null
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar("Cihazın sessiyası uğurla bitirildi.")
                                }
                            },
                            onError = { err ->
                                isRevoking = false
                                sessionToRevoke = null
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar("Xəta: $err")
                                }
                            }
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    if (isRevoking) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    } else {
                        Text("Bəli, Bitir")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { sessionToRevoke = null }, enabled = !isRevoking) {
                    Text("Ləğv et")
                }
            }
        )
    }

    // Revoke All Other Devices Dialog
    if (showRevokeAllOtherDialog) {
        AlertDialog(
            onDismissRequest = { if (!isRevoking) showRevokeAllOtherDialog = false },
            title = { Text("Bütün digər cihazları bağlamaq?", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    text = "Bu cihazdan başqa digər bütün ($otherDevicesCount) aktiv sessiyalarınız ləğv ediləcək. Davam edilsin?",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        isRevoking = true
                        viewModel.revokeAllOtherSessions(
                            onSuccess = { count ->
                                isRevoking = false
                                showRevokeAllOtherDialog = false
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar("$count cihazdan uğurla çıxış edildi.")
                                }
                            },
                            onError = { err ->
                                isRevoking = false
                                showRevokeAllOtherDialog = false
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar("Xəta: $err")
                                }
                            }
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    if (isRevoking) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    } else {
                        Text("Bütün Digərlərini Bağla")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showRevokeAllOtherDialog = false }, enabled = !isRevoking) {
                    Text("Ləğv et")
                }
            }
        )
    }
}

@Composable
fun DeviceSessionCard(
    session: DeviceSession,
    onRevokeClick: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("dd MMMM yyyy, HH:mm", Locale("az")) }
    val lastActiveFormatted = remember(session.lastActiveTimestamp) {
        dateFormat.format(Date(session.lastActiveTimestamp))
    }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = if (session.isCurrentDevice) 4.dp else 1.dp,
        border = if (session.isCurrentDevice) {
            androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFF16A34A).copy(alpha = 0.5f))
        } else null,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(if (session.isCurrentDevice) Color(0xFFDCFCE7) else MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (session.isCurrentDevice) Icons.Default.Smartphone else Icons.Default.PhonelinkSetup,
                            contentDescription = null,
                            tint = if (session.isCurrentDevice) Color(0xFF15803D) else BirbankTextSecondary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Column {
                        Text(
                            text = session.deviceName,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = BirbankTextPrimary
                        )
                        Text(
                            text = "${session.osVersion} • v${session.appVersion}",
                            style = MaterialTheme.typography.labelSmall,
                            color = BirbankTextSecondary
                        )
                    }
                }

                if (session.isCurrentDevice) {
                    Surface(
                        color = Color(0xFFDCFCE7),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "Cari Cihaz",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF15803D),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            HorizontalDivider(
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f),
                modifier = Modifier.padding(vertical = 12.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccessTime,
                            contentDescription = null,
                            tint = BirbankTextSecondary,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "Son aktivlik: $lastActiveFormatted",
                            style = MaterialTheme.typography.labelSmall,
                            color = BirbankTextSecondary
                        )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = BirbankTextSecondary,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "${session.location} (${session.ipAddress})",
                            style = MaterialTheme.typography.labelSmall,
                            color = BirbankTextSecondary
                        )
                    }
                }

                if (!session.isCurrentDevice) {
                    TextButton(
                        onClick = onRevokeClick,
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Sessiyanı bitir", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}
