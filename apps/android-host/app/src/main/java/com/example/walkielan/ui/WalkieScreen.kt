package com.example.walkielan.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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
import com.example.walkielan.audio.AudioRoute
import com.example.walkielan.audio.label
import com.example.walkielan.data.TransportMode
import com.example.walkielan.local.DiscoveredRoom

private val Ink = Color(0xFF112031)
private val Sand = Color(0xFFF5EFE5)
private val Clay = Color(0xFFD97D54)
private val SoftCard = Color.White.copy(alpha = 0.88f)

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
            color = Sand,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color(0xFFF7F2E7), Color(0xFFE2D6C6)),
                        ),
                    )
                    .safeDrawingPadding(),
            ) {
                if (state.session == null || state.snapshot == null) {
                    SetupScreen(
                        state = state,
                        onSetMode = viewModel::setSetupMode,
                        onNicknameChanged = viewModel::updateNickname,
                        onRoomNameChanged = viewModel::updateRoomName,
                        onChannelsChanged = viewModel::updateChannelsInput,
                        onRefreshLocalRooms = viewModel::refreshLocalRooms,
                        onJoinDiscoveredRoom = viewModel::joinDiscoveredRoom,
                        onCreateLocalRoom = viewModel::createLocalRoom,
                        onServerChanged = viewModel::updateServerBaseUrl,
                        onRoomCodeChanged = viewModel::updateRoomCodeInput,
                        onCreateAdvancedRoom = viewModel::createRoom,
                        onJoinAdvancedRoom = viewModel::joinRoom,
                    )
                } else {
                    ActiveRoomScreen(
                        state = state,
                        onEnableMic = ::requestMic,
                        onSelectChannel = viewModel::selectChannel,
                        onShowAudioRoutes = viewModel::showAudioRoutePicker,
                        onHideAudioRoutes = viewModel::hideAudioRoutePicker,
                        onSelectAudioRoute = viewModel::selectAudioRoute,
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
    onSetMode: (SetupMode) -> Unit,
    onNicknameChanged: (String) -> Unit,
    onRoomNameChanged: (String) -> Unit,
    onChannelsChanged: (String) -> Unit,
    onRefreshLocalRooms: () -> Unit,
    onJoinDiscoveredRoom: (DiscoveredRoom) -> Unit,
    onCreateLocalRoom: () -> Unit,
    onServerChanged: (String) -> Unit,
    onRoomCodeChanged: (String) -> Unit,
    onCreateAdvancedRoom: () -> Unit,
    onJoinAdvancedRoom: () -> Unit,
) {
    val channelsPreview = remember(state.channelsInput) {
        state.channelsInput.split(",")
            .map(String::trim)
            .filter(String::isNotBlank)
            .ifEmpty { listOf("Geral", "Operacao", "Suporte") }
            .take(8)
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(18.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Ink),
                shape = RoundedCornerShape(30.dp),
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Eyebrow("Walkie LAN V2")
                    Text("Android hospeda. A rede local faz o resto.", color = Sand, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                    Text(
                        "Crie uma sala no Android, anuncie por LAN, compartilhe o QR e use o navegador apenas como console auxiliar.",
                        color = Sand.copy(alpha = 0.92f),
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        ModeChip(
                            label = "Modo simples",
                            active = state.setupMode == SetupMode.SIMPLE,
                            onClick = { onSetMode(SetupMode.SIMPLE) },
                        )
                        ModeChip(
                            label = "Modo avancado",
                            active = state.setupMode == SetupMode.ADVANCED,
                            onClick = { onSetMode(SetupMode.ADVANCED) },
                        )
                    }
                }
            }
        }

        item {
            SectionCard(
                title = "Perfil rapido",
                body = "Seu apelido e os dados da equipe ficam salvos neste aparelho para reentrada rapida.",
            ) {
                OutlinedTextField(
                    value = state.nickname,
                    onValueChange = onNicknameChanged,
                    label = { Text("Apelido") },
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = state.roomName,
                    onValueChange = onRoomNameChanged,
                    label = { Text("Nome da equipe / sala") },
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = state.channelsInput,
                    onValueChange = onChannelsChanged,
                    label = { Text("Canais sugeridos") },
                    supportingText = { Text("Separe por virgula. Ate 8 canais.") },
                    modifier = Modifier.fillMaxWidth(),
                )
                Text("Canais previstos: ${channelsPreview.joinToString(" / ")}", color = Ink.copy(alpha = 0.76f))
            }
        }

        item {
            SectionCard(
                title = "Criar sala local",
                body = "Fluxo principal do produto. Sem IP manual, sem backend obrigatorio e com pareamento por codigo e QR.",
            ) {
                Button(onClick = onCreateLocalRoom, enabled = !state.busy, modifier = Modifier.fillMaxWidth()) {
                    Text(if (state.busy) "Criando..." else "Criar sala")
                }
                HintCard(
                    title = "O que acontece ao criar",
                    body = "Este Android vira o host oficial da sala, abre o endpoint local, anuncia a sala por LAN e mostra um QR para console auxiliar.",
                )
            }
        }

        item {
            SectionCard(
                title = "Entrar por descoberta local",
                body = "Veja as salas Android anunciadas na rede Wi-Fi atual. Nao precisa digitar IP.",
            ) {
                OutlinedButton(onClick = onRefreshLocalRooms, enabled = !state.busy, modifier = Modifier.fillMaxWidth()) {
                    Text(if (state.discoveryActive) "Atualizar salas LAN" else "Iniciar descoberta LAN")
                }
                if (state.discoveredRooms.isEmpty()) {
                    EmptyState(
                        title = "Nenhuma sala local encontrada",
                        body = "Se outra pessoa ja abriu a sala, toque em atualizar. Se voce for o host, use Criar sala.",
                    )
                } else {
                    state.discoveredRooms.forEach { room ->
                        LocalRoomCard(room = room, onJoin = { onJoinDiscoveredRoom(room) }, enabled = !state.busy)
                    }
                }
            }
        }

        if (state.setupMode == SetupMode.ADVANCED) {
            item {
                SectionCard(
                    title = "Modo avancado",
                    body = "Compatibilidade, depuracao e fluxo legado com servidor manual. Fora do caminho principal.",
                ) {
                    OutlinedTextField(
                        value = state.serverBaseUrl,
                        onValueChange = onServerChanged,
                        label = { Text("Endereco manual do servidor") },
                        placeholder = { Text("http://192.168.0.15:8787") },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = state.roomCodeInput,
                        onValueChange = onRoomCodeChanged,
                        label = { Text("Codigo da sala") },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                        Button(
                            onClick = onCreateAdvancedRoom,
                            enabled = !state.busy,
                            modifier = Modifier.weight(1f),
                        ) {
                            Text("Criar via servidor")
                        }
                        OutlinedButton(
                            onClick = onJoinAdvancedRoom,
                            enabled = !state.busy,
                            modifier = Modifier.weight(1f),
                        ) {
                            Text("Entrar via servidor")
                        }
                    }
                    HintCard(
                        title = "Uso recomendado",
                        body = "Deixe este modo para testes de compatibilidade, laboratorio web e ambientes onde a descoberta local nao estiver disponivel.",
                    )
                }
            }
        }

        item {
            StatusBlock(state.notice, state.errorMessage)
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun ActiveRoomScreen(
    state: MainUiState,
    onEnableMic: () -> Unit,
    onSelectChannel: (String) -> Unit,
    onShowAudioRoutes: () -> Unit,
    onHideAudioRoutes: () -> Unit,
    onSelectAudioRoute: (AudioRoute) -> Unit,
    onPressToTalk: () -> Unit,
    onReleaseToTalk: () -> Unit,
    onDisconnect: () -> Unit,
) {
    val snapshot = state.snapshot ?: return
    val session = state.session ?: return
    val self = snapshot.members.firstOrNull { it.peerId == session.peerId }
    val selectedChannel = snapshot.channels.firstOrNull { it.channelId == self?.selectedChannelId }
    val activeSpeakerName = selectedChannel?.activeSpeakerPeerId?.let { peerId ->
        snapshot.members.firstOrNull { it.peerId == peerId }?.nickname
    } ?: "Livre"
    val qrBitmap = remember(state.pairingUrl) {
        state.pairingUrl?.takeIf { it.isNotBlank() }?.let { buildQrCodeBitmap(it) }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(18.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Card(colors = CardDefaults.cardColors(containerColor = Ink), shape = RoundedCornerShape(30.dp)) {
                Column(
                    modifier = Modifier.padding(22.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Eyebrow(if (session.transportMode == TransportMode.LOCAL_LAN) "Sala local ativa" else "Sessao conectada")
                    Text(snapshot.roomName, color = Sand, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                    Text("Codigo ${session.roomCode}", color = Sand.copy(alpha = 0.94f), style = MaterialTheme.typography.headlineSmall)
                    Text(
                        "${snapshot.members.count { it.isConnected }}/${snapshot.capacity} participantes | Host ${snapshot.hostStatus}",
                        color = Sand.copy(alpha = 0.88f),
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        StatusChip(if (state.connected) "Conectado" else "Offline")
                        StatusChip(if (state.micReady) "Microfone pronto" else "Permissao pendente")
                        StatusChip(if (state.isTalking) "Transmitindo" else "Escuta")
                    }
                    StatusBlock(state.notice, state.errorMessage, ink = Sand, error = Color(0xFFFFD1C2))
                }
            }
        }

        if (state.localConsoleUrl != null || state.localHostBaseUrl != null) {
            item {
                SectionCard(
                    title = "Pareamento e console auxiliar",
                    body = "Use o QR no iPhone ou em outro navegador para abrir o console local desta sala.",
                ) {
                    if (qrBitmap != null) {
                        Image(
                            bitmap = qrBitmap,
                            contentDescription = "QR code da sala",
                            modifier = Modifier
                                .size(220.dp)
                                .align(Alignment.CenterHorizontally),
                        )
                    }
                    if (state.localHostBaseUrl != null) {
                        Text("Endpoint local: ${state.localHostBaseUrl}", color = Ink.copy(alpha = 0.78f))
                    }
                    if (state.localConsoleUrl != null) {
                        Text("Console auxiliar: ${state.localConsoleUrl}", color = Ink.copy(alpha = 0.78f))
                    }
                }
            }
        }

        item {
            SectionCard(
                title = "Painel de operacao",
                body = "Canal atual: ${selectedChannel?.name ?: "Sem canal"} | Locutor: $activeSpeakerName",
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    snapshot.channels.forEach { channel ->
                        ChannelChip(
                            label = channel.name,
                            activeLabel = if (channel.activeSpeakerPeerId == null) {
                                "Livre"
                            } else {
                                snapshot.members.firstOrNull { it.peerId == channel.activeSpeakerPeerId }?.nickname ?: "Falando"
                            },
                            selected = channel.channelId == self?.selectedChannelId,
                            onClick = { onSelectChannel(channel.channelId) },
                        )
                    }
                }
                Button(onClick = onEnableMic, modifier = Modifier.fillMaxWidth()) {
                    Text(if (state.micReady) "Revalidar microfone" else "Habilitar microfone")
                }
                OutlinedButton(
                    onClick = onShowAudioRoutes,
                    enabled = state.audioRouteSupported,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Saida de audio: ${state.selectedAudioRoute.label}")
                }
                if (state.audioRouteNotice != null) {
                    Text(state.audioRouteNotice, color = Ink.copy(alpha = 0.78f))
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(240.dp)
                        .background(Clay, RoundedCornerShape(34.dp))
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
                        Text("PTT", color = Color.White.copy(alpha = 0.78f))
                        Text("Aperte para falar", color = Color.White, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                        Text(selectedChannel?.name ?: "Sem canal", color = Color.White.copy(alpha = 0.88f))
                    }
                }
                OutlinedButton(onClick = onDisconnect, modifier = Modifier.fillMaxWidth()) {
                    Text("Encerrar sessao")
                }
            }
        }

        item {
            SectionCard(
                title = "Presenca",
                body = "Quem esta conectado agora e em qual canal cada pessoa esta ouvindo.",
            ) {
                snapshot.members.forEach { member ->
                    PresenceCard(
                        title = member.nickname,
                        subtitle = snapshot.channels.firstOrNull { it.channelId == member.selectedChannelId }?.name ?: "Sem canal",
                        badge = if (member.isHost) "Host" else member.role.name.lowercase(),
                        active = member.isConnected,
                    )
                }
            }
        }

        item {
            SectionCard(
                title = "Eventos recentes",
                body = "Entradas, saidas, troca de canal e inicio/fim de fala.",
            ) {
                snapshot.eventLog.takeLast(12).reversed().forEach { event ->
                    PresenceCard(
                        title = event.summary,
                        subtitle = event.occurredAt,
                        badge = event.type,
                        active = true,
                    )
                }
            }
        }
    }

    if (state.isAudioRoutePickerVisible) {
        ModalBottomSheet(onDismissRequest = onHideAudioRoutes) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text("Saida de audio", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text("Escolha para onde o audio da sala sera enviado nesta sessao.")
                state.availableAudioRoutes.forEach { route ->
                    val isSelected = route == state.selectedAudioRoute
                    if (isSelected) {
                        Button(
                            onClick = { onSelectAudioRoute(route) },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text("${route.label} (em uso)")
                        }
                    } else {
                        OutlinedButton(
                            onClick = { onSelectAudioRoute(route) },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(route.label)
                        }
                    }
                }
                OutlinedButton(onClick = onHideAudioRoutes, modifier = Modifier.fillMaxWidth()) {
                    Text("Fechar")
                }
            }
        }
    }
}

@Composable
private fun SectionCard(
    title: String,
    body: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = SoftCard),
        shape = RoundedCornerShape(28.dp),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            content = {
                Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = Ink)
                Text(body, color = Ink.copy(alpha = 0.78f))
                HorizontalDivider(color = Ink.copy(alpha = 0.08f))
                content()
            },
        )
    }
}

@Composable
private fun LocalRoomCard(
    room: DiscoveredRoom,
    onJoin: () -> Unit,
    enabled: Boolean,
) {
    Card(colors = CardDefaults.cardColors(containerColor = Color(0x14112031)), shape = RoundedCornerShape(24.dp)) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(room.roomName, fontWeight = FontWeight.SemiBold, color = Ink)
            Text("Codigo ${room.roomCode} | ${room.hostAddress}:${room.port}", color = Ink.copy(alpha = 0.72f))
            OutlinedButton(onClick = onJoin, enabled = enabled, modifier = Modifier.fillMaxWidth()) {
                Text("Entrar nesta sala")
            }
        }
    }
}

@Composable
private fun PresenceCard(
    title: String,
    subtitle: String,
    badge: String,
    active: Boolean,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0x14112031), RoundedCornerShape(20.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, fontWeight = FontWeight.SemiBold, color = Ink)
            Text(subtitle, color = Ink.copy(alpha = 0.68f))
        }
        Box(
            modifier = Modifier
                .background(if (active) Color(0x22326A4A) else Color(0x22112031), RoundedCornerShape(999.dp))
                .padding(horizontal = 12.dp, vertical = 8.dp),
        ) {
            Text(badge, color = if (active) Color(0xFF20523A) else Ink)
        }
    }
}

@Composable
private fun ChannelChip(
    label: String,
    activeLabel: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val textColor = if (selected) Sand else Ink
    if (selected) {
        Button(onClick = onClick, shape = RoundedCornerShape(22.dp)) {
            Column(horizontalAlignment = Alignment.Start) {
                Text(label, color = textColor, fontWeight = FontWeight.SemiBold)
                Text(activeLabel, color = textColor.copy(alpha = 0.72f))
            }
        }
    } else {
        OutlinedButton(onClick = onClick, shape = RoundedCornerShape(22.dp)) {
            Column(horizontalAlignment = Alignment.Start) {
                Text(label, color = textColor, fontWeight = FontWeight.SemiBold)
                Text(activeLabel, color = textColor.copy(alpha = 0.72f))
            }
        }
    }
}

@Composable
private fun Eyebrow(text: String) {
    Box(
        modifier = Modifier
            .background(Sand.copy(alpha = 0.14f), RoundedCornerShape(999.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        Text(text, color = Sand, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun ModeChip(label: String, active: Boolean, onClick: () -> Unit) {
    if (active) {
        Button(onClick = onClick) { Text(label) }
    } else {
        OutlinedButton(onClick = onClick) { Text(label) }
    }
}

@Composable
private fun HintCard(title: String, body: String) {
    Card(colors = CardDefaults.cardColors(containerColor = Color(0x14112031)), shape = RoundedCornerShape(22.dp)) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(title, fontWeight = FontWeight.SemiBold, color = Ink)
            Text(body, color = Ink.copy(alpha = 0.78f))
        }
    }
}

@Composable
private fun EmptyState(title: String, body: String) {
    Card(colors = CardDefaults.cardColors(containerColor = Color(0x14112031)), shape = RoundedCornerShape(22.dp)) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(title, fontWeight = FontWeight.SemiBold, color = Ink)
            Text(body, color = Ink.copy(alpha = 0.76f))
        }
    }
}

@Composable
private fun StatusBlock(
    notice: String,
    errorMessage: String?,
    ink: Color = Ink,
    error: Color = Color(0xFF932D23),
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(notice, color = ink)
        if (errorMessage != null) {
            Text(errorMessage, color = error)
        }
    }
}

@Composable
private fun StatusChip(text: String) {
    Box(
        modifier = Modifier
            .background(Sand.copy(alpha = 0.14f), RoundedCornerShape(999.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        Text(text, color = Sand)
    }
}
