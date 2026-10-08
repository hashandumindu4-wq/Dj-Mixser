package com.hashan.djmixer

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer

class MainActivity : ComponentActivity() {
    private lateinit var deckA: ExoPlayer
    private lateinit var deckB: ExoPlayer

    private var targetDeck = "A"
    private val picker = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri ?: return@registerForActivityResult
        try {
            contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        } catch (_: Exception) {}
        val player = if (targetDeck == "A") deckA else deckB
        player.setMediaItem(MediaItem.fromUri(uri))
        player.prepare()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        deckA = ExoPlayer.Builder(this).build()
        deckB = ExoPlayer.Builder(this).build()
        setContent {
            DJMixerApp(
                deckA = deckA,
                deckB = deckB,
                onPickA = { targetDeck = "A"; picker.launch(arrayOf("audio/*")) },
                onPickB = { targetDeck = "B"; picker.launch(arrayOf("audio/*")) }
            )
        }
    }

    override fun onDestroy() {
        deckA.release()
        deckB.release()
        super.onDestroy()
    }
}

@Composable
fun DJMixerApp(
    deckA: ExoPlayer, deckB: ExoPlayer,
    onPickA: () -> Unit, onPickB: () -> Unit
) {
    var theme by remember { mutableIntStateOf(0) }
    var cross by remember { mutableFloatStateOf(0.5f) }
    var master by remember { mutableFloatStateOf(1f) }
    var bass by remember { mutableFloatStateOf(0.5f) }
    var mid by remember { mutableFloatStateOf(0.5f) }
    var treble by remember { mutableFloatStateOf(0.5f) }
    var effect by remember { mutableStateOf("OFF") }

    val bg = listOf(Color(0xFF07090D), Color(0xFF0A1020), Color(0xFF160A18))[theme]
    val accent = listOf(Color(0xFF00F5A0), Color(0xFF00D9FF), Color(0xFFFF42D0))[theme]

    // Real-time two-deck crossfade: A is loudest at 0, B at 1.
    LaunchedEffect(cross, master) {
        deckA.volume = master * (1f - cross).coerceIn(0f, 1f)
        deckB.volume = master * cross.coerceIn(0f, 1f)
    }

    MaterialTheme {
        Column(
            Modifier.fillMaxSize().background(bg).padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("DJ MIXER", color=accent, style=MaterialTheme.typography.headlineSmall)
                Spacer(Modifier.weight(1f))
                Text("V2 • OFFLINE", color=Color.Gray)
                Spacer(Modifier.width(8.dp))
                OutlinedButton(onClick={ theme=(theme+1)%3 }) { Text("THEME") }
            }

            Row(Modifier.fillMaxSize(), horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                RealDeck("A", deckA, accent, onPickA, Modifier.weight(1f))
                CenterControls(
                    accent=accent, cross=cross, master=master,
                    bass=bass, mid=mid, treble=treble, effect=effect,
                    onCross={cross=it}, onMaster={master=it},
                    onBass={bass=it}, onMid={mid=it}, onTreble={treble=it},
                    onEffect={effect=if(effect=="OFF") "ECHO" else "OFF"}
                )
                RealDeck("B", deckB, accent, onPickB, Modifier.weight(1f))
            }
        }
    }
}

@Composable
fun RealDeck(
    name: String, player: ExoPlayer, accent: Color,
    onPick: () -> Unit, modifier: Modifier
) {
    var playing by remember { mutableStateOf(false) }
    var pitch by remember { mutableFloatStateOf(0f) }

    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) { playing = isPlaying }
        }
        player.addListener(listener)
        onDispose { player.removeListener(listener) }
    }

    LaunchedEffect(pitch) {
        player.setPlaybackSpeed((1f + pitch * 0.15f).coerceIn(0.85f, 1.15f))
    }

    Card(
        modifier=modifier.fillMaxHeight(),
        shape=RoundedCornerShape(18.dp),
        colors=CardDefaults.cardColors(containerColor=Color(0xFF111620))
    ) {
        Column(Modifier.fillMaxSize().padding(10.dp), horizontalAlignment=Alignment.CenterHorizontally) {
            Row(Modifier.fillMaxWidth(), verticalAlignment=Alignment.CenterVertically) {
                Text("DECK $name", color=accent, style=MaterialTheme.typography.titleLarge)
                Spacer(Modifier.weight(1f))
                Text(if(player.mediaItemCount>0) "LOADED" else "EMPTY", color=Color.Gray)
            }
            Spacer(Modifier.height(5.dp))
            Box(
                Modifier.size(135.dp).background(Color(0xFF080A0E), CircleShape),
                contentAlignment=Alignment.Center
            ) {
                Text("JOG", color=accent)
            }
            Spacer(Modifier.height(4.dp))
            Text(
                if(player.mediaItemCount>0) "READY • ${formatTime(player.duration)}"
                else "Choose an offline song",
                color=Color.LightGray
            )
            Row(horizontalArrangement=Arrangement.spacedBy(5.dp)) {
                Button(onClick=onPick) { Text("LOAD") }
                Button(onClick={
                    if(player.isPlaying) player.pause() else player.play()
                }) { Text(if(playing) "PAUSE" else "PLAY") }
            }
            Text("PITCH ${String.format("%.1f", pitch*15)}%", color=Color.Gray)
            Slider(value=pitch, onValueChange={pitch=it}, valueRange=-1f..1f)
            Row(horizontalArrangement=Arrangement.spacedBy(5.dp)) {
                OutlinedButton(onClick={ player.seekTo(0) }) { Text("CUE") }
                OutlinedButton(onClick={
                    val pos=(player.currentPosition-5000).coerceAtLeast(0)
                    player.seekTo(pos)
                }) { Text("-5s") }
                OutlinedButton(onClick={
                    player.seekTo((player.currentPosition+5000).coerceAtMost(player.duration.coerceAtLeast(0)))
                }) { Text("+5s") }
            }
        }
    }
}

@Composable
fun CenterControls(
    accent: Color, cross: Float, master: Float,
    bass: Float, mid: Float, treble: Float, effect: String,
    onCross:(Float)->Unit, onMaster:(Float)->Unit,
    onBass:(Float)->Unit, onMid:(Float)->Unit, onTreble:(Float)->Unit,
    onEffect:()->Unit
) {
    Card(
        Modifier.width(255.dp).fillMaxHeight(),
        shape=RoundedCornerShape(18.dp),
        colors=CardDefaults.cardColors(containerColor=Color(0xFF171C26))
    ) {
        Column(Modifier.fillMaxSize().padding(10.dp), horizontalAlignment=Alignment.CenterHorizontally) {
            Text("MIXER", color=accent, style=MaterialTheme.typography.titleLarge)
            Text("BASS", color=Color.Gray); Slider(bass,onBass)
            Text("MID", color=Color.Gray); Slider(mid,onMid)
            Text("TREBLE", color=Color.Gray); Slider(treble,onTreble)
            Text("CROSSFADER", color=Color.Gray)
            Slider(cross,onCross)
            Text("MASTER", color=Color.Gray)
            Slider(master,onMaster)
            Row(horizontalArrangement=Arrangement.spacedBy(5.dp)) {
                Button(onClick=onEffect) { Text(effect) }
                Button(onClick={}) { Text("LOOP") }
            }
            Text("A  ◀──────●──────▶  B", color=accent)
            Text("OFFLINE AUDIO ENGINE", color=Color.DarkGray)
        }
    }
}

fun formatTime(ms: Long): String {
    if(ms <= 0) return "00:00"
    val total = ms / 1000
    return "%02d:%02d".format(total/60, total%60)
}
