package com.example.walkielan.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.walkielan.MainViewModel

@Composable
fun WalkieApp(viewModel: MainViewModel) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
    ) { result ->
        if (result.values.all { it }) {
            viewModel.enableMicrophone()
        }
    }

    fun requestMic() {
        val permissions = buildList {
            add(Manifest.permission.RECORD_AUDIO)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
        val denied = permissions.filter {
            ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
        }
        if (denied.isEmpty()) {
            viewModel.enableMicrophone()
        } else {
            permissionLauncher.launch(denied.toTypedArray())
        }
    }

    MaterialTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color(0xFFF3EFE4),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color(0xFFF7F2E7), Color(0xFFE5DACE)),
                        ),
                    )
                    .safeDrawingPadding(),
            ) {
                if (state.session == null || state.snapshot == null) {
                    SetupScreen(
                        state = state,
                        onServerChanged = viewModel::updateServerBaseUrl,
                        onNicknameChanged = viewModel::updateNickname,
                        onRoomNameChanged = viewModel::updateRoomName,
                        onChannelsChanged = viewModel::updateChannelsInput,
                        onRoomCodeChanged = viewModel::updateRoomCodeInput,
                        onCreateRoom = viewModel::createRoom,
                        onJoinRoom = viewModel::joinRoom,
                    )
                } else {
                    ActiveRoomScreen(
                        state = state,
                        onEnableMic = ::requestMic,
                        onSelectChannel = viewModel::selectChannel,
                        onPressToTalk = viewModel::startTalking,
                        onReleaseToTalk = viewModel::stopTalking,
                        onDisconnect = viewModel::disconnect,
                    )
                }
            }
        }
    }
}

@Composable
private fun SetupScreen(
    state: MainUiState,
    onServerChanged: (String) -> Unit,
    onNicknameChanged: (String) -> Unit,
    onRoomNameChanged: (String) -> Unit,
    onChannelsChanged: (String) -> Unit,
    onRoomCodeChanged: (String) -> Unit,
    onCreateRoom: () -> Unit,
    onJoinRoom: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        item {
            HeroCard(
                title = "Walkie LAN Hibrido",
                body = "Android hospeda a sala. iPhone entra pelo navegador com codigo curto e PTT em primeiro plano.",
            )
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.82f))) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    OutlinedTextField(
                        value = state.serverBaseUrl,
                        onValueChange = onServerChanged,
                        label = { Text("Endereco do servidor") },
                        placeholder = { Text("http://192.168.0.15:8787") },
                        supportingText = {
                            Text("Celular real: use o IP do computador. Emulador Android: use http://10.0.2.2:8787")
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    HintCard(
                        title = "O que colocar aqui",
                        body = "Se o servidor estiver rodando no seu computador, use o IP dele na mesma rede Wi-Fi. Exemplo: http://192.168.0.15:8787. O endereco 10.0.2.2 funciona so no emulador.",
                    )
                    OutlinedTextField(
                        value = state.nickname,
                        onValueChange = onNicknameChanged,
                        label = { Text("Apelido") },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    HorizontalDivider()
                    OutlinedTextField(
                        value = state.roomName,
                        onValueChange = onRoomNameChanged,
                        label = { Text("Nome da sala") },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = state.channelsInput,
                        onValueChange = onChannelsChanged,
                        label = { Text("Canais separados por virgula") },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Button(onClick = onCreateRoom, enabled = !state.busy, modifier = Modifier.fillMaxWidth()) {
                        Text("Criar sala Android host")
                    }
                    HorizontalDivider()
                    OutlinedTextField(
                        value = state.roomCodeInput,
                        onValueChange = onRoomCodeChanged,
                        label = { Text("Codigo da sala") },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedButton(onClick = onJoinRoom, enabled = !state.busy, modifier = Modifier.fillMaxWidth()) {
                        Text("Entrar na sala")
                    }
                    StatusBlock(state.notice, state.errorMessage)
                }
            }
        }
    }
}

@Composable
private fun ActiveRoomScreen(
    state: MainUiState,
    onEnableMic: () -> Unit,
    onSelectChannel: (String) -> Unit,
    onPressToTalk: () -> Unit,
    onReleaseToTalk: () -> Unit,
    onDisconnect: () -> Unit,
) {
    val snapshot = state.snapshot ?: return
    val session = state.session ?: return
    val self = snapshot.members.firstOrNull { it.peerId == session.peerId }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        item {
            Card(colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.82f))) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text(snapshot.roomName, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                    Text("Codigo ${session.roomCode} | Host ${snapshot.hostStatus} | ${if (state.connected) "conectado" else "offline"}")
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        StatusChip(if (state.micReady) "Microfone pronto" else "Microfone pendente")
                        StatusChip(if (state.isTalking) "Transmitindo" else "Escuta")
                    }
                    StatusBlock(state.notice, state.errorMessage)
                }
            }
        }

        item {
            Card(colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.82f))) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    snapshot.channels.forEach { channel ->
                        OutlinedButton(
                            onClick = { onSelectChannel(channel.channelId) },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            val activeLabel = if (channel.activeSpeakerPeerId == null) {
                                "Livre"
                            } else {
                                snapshot.members.firstOrNull { it.peerId == channel.activeSpeakerPeerId }?.nickname ?: "Falando"
                            }
                            Text("${channel.name} | $activeLabel")
                        }
                    }
                    Button(onClick = onEnableMic, modifier = Modifier.fillMaxWidth()) {
                        Text(if (state.micReady) "Revalidar microfone" else "Habilitar microfone")
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp)
                            .background(Color(0xFFD97D54), RoundedCornerShape(32.dp))
                            .pointerInput(state.micReady) {
                                detectTapGestures(
                                    onPress = {
                                        onPressToTalk()
                                        tryAwaitRelease()
                                        onReleaseToTalk()
                                    },
                                )
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Aperte para falar", color = Color.White, style = MaterialTheme.typography.headlineSmall)
                            Text(
                                self?.let { member ->
                                    snapshot.channels.firstOrNull { it.channelId == member.selectedChannelId }?.name
                                } ?: "",
                                color = Color.White.copy(alpha = 0.85f),
                            )
                        }
                    }
                    OutlinedButton(onClick = onDisconnect, modifier = Modifier.fillMaxWidth()) {
                        Text("Encerrar sessao")
                    }
                }
            }
        }

        item {
            HeroCard(title = "Presenca", body = "Participantes e canal selecionado no momento.")
        }

        items(snapshot.members, key = { it.peerId }) { member ->
            Card(colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.78f))) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(member.nickname, fontWeight = FontWeight.SemiBold)
                    Text(snapshot.channels.firstOrNull { it.channelId == member.selectedChannelId }?.name ?: "Sem canal")
                    Text(if (member.isHost) "Host" else member.clientType.name.lowercase())
                }
            }
        }

        item {
            HeroCard(title = "Eventos recentes", body = "Log local de entrada, fala, troca de canal e encerramento.")
        }

        items(snapshot.eventLog.takeLast(12).reversed(), key = { it.eventId }) { event ->
            Card(colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.78f))) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(event.summary, fontWeight = FontWeight.Medium)
                    Text(event.occurredAt)
                }
            }
        }
    }
}

@Composable
private fun HeroCard(title: String, body: String) {
    Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF112031))) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, color = Color(0xFFF3EFE4), style = MaterialTheme.typography.headlineSmall)
            Text(body, color = Color(0xFFF3EFE4).copy(alpha = 0.92f))
        }
    }
}

@Composable
private fun HintCard(title: String, body: String) {
    Card(colors = CardDefaults.cardColors(containerColor = Color(0x14112031))) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(title, fontWeight = FontWeight.SemiBold, color = Color(0xFF112031))
            Text(body, color = Color(0xCC112031))
        }
    }
}

@Composable
private fun StatusBlock(notice: String, errorMessage: String?) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(notice, color = Color(0xFF112031))
        if (errorMessage != null) {
            Text(errorMessage, color = Color(0xFF932D23))
        }
    }
}

@Composable
private fun StatusChip(text: String) {
    Box(
        modifier = Modifier
            .background(Color(0x22112031), RoundedCornerShape(999.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        Text(text)
    }
}
