package tv.own.owntv.mobile.dev

import android.content.Context
import android.view.SurfaceHolder
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.view.SurfaceView
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.androidx.compose.koinViewModel
import tv.own.owntv.player.OwnTVPlayer

/**
 * The Plan 3 Phase 3 harness: open core's database on a phone, sync a real playlist through core's
 * real sync path, list real channel names, and play one live stream on a bare surface.
 *
 * Deliberately ugly. No theme work, no cards, no icons, no controls — anything that looks designed
 * would only invite someone to keep it. Every screen in this app gets written from scratch in Plan 4.
 */
@Composable
fun DevHarnessScreen(vm: DevHarnessViewModel = koinViewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()

    if (state.playing != null) {
        // Full-bleed video, tap anywhere to stop. That is the whole player at this stage.
        BareVideoSurface(
            player = vm.player,
            modifier = Modifier.fillMaxSize().background(Color.Black).clickable { vm.stop() },
        )
        return
    }

    var m3uUrl by remember { mutableStateOf("") }
    var server by remember { mutableStateOf("") }
    var user by remember { mutableStateOf("") }
    var pass by remember { mutableStateOf("") }
    var backupPass by remember { mutableStateOf("") }

    // Plan 4 Phase 11's cacheDir bridge, proved here first: SAF hands back a content Uri, core's
    // BackupManager needs a File it can open repeatedly, so the picked document is copied into the
    // cache and the ViewModel deletes it when the restore finishes.
    val context = LocalContext.current
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        val copy = java.io.File(context.cacheDir, "restore.own")
        context.contentResolver.openInputStream(uri)?.use { input ->
            copy.outputStream().use(input::copyTo)
        }
        vm.restore(copy, backupPass)
    }

    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("status: ${state.status}")
        Text("profile ${state.profileId} · sources ${state.sourceIds}")
        Text("channels ${state.channelCount} · movies ${state.movieCount} · series ${state.seriesCount}")
        if (state.busy) LinearProgressIndicator(Modifier.fillMaxWidth())

        OutlinedTextField(
            value = m3uUrl,
            onValueChange = { m3uUrl = it },
            label = { Text("M3U url") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Button(onClick = { vm.addM3u(m3uUrl) }, enabled = !state.busy && m3uUrl.isNotBlank()) {
            Text("Import M3U")
        }

        OutlinedTextField(
            value = server,
            onValueChange = { server = it },
            label = { Text("Xtream server") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = user,
                onValueChange = { user = it },
                label = { Text("user") },
                singleLine = true,
                modifier = Modifier.weight(1f),
            )
            OutlinedTextField(
                value = pass,
                onValueChange = { pass = it },
                label = { Text("pass") },
                singleLine = true,
                modifier = Modifier.weight(1f),
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = { vm.addXtream(server, user, pass) },
                enabled = !state.busy && server.isNotBlank() && user.isNotBlank(),
            ) { Text("Import Xtream") }
            Button(onClick = vm::reload, enabled = !state.busy) { Text("Reload") }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = backupPass,
                onValueChange = { backupPass = it },
                label = { Text("backup password") },
                singleLine = true,
                modifier = Modifier.weight(1f),
            )
            Button(
                // Any MIME type: ".own" is our own extension, so most document providers report it
                // as application/octet-stream and a narrower filter would grey the file out.
                onClick = { picker.launch(arrayOf("*/*")) },
                enabled = !state.busy,
            ) { Text("Restore backup") }
        }

        LazyColumn(Modifier.fillMaxSize()) {
            items(state.channels, key = { it.id }) { channel ->
                Text(
                    text = channel.name,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { vm.play(channel) }
                        .padding(vertical = 12.dp),
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        }
    }
}

/**
 * The mpv output surface, stripped to the contract `:player-core` actually requires: hand it a
 * Surface on create, tell it the size on change, take it back on destroy. The TV app's
 * `MpvVideoSurface` adds zoom math, auto frame rate, the ExoPlayer subtitle overlay and the
 * freeze-frame — all of which are shell concerns, and none of which are needed to prove the engine
 * decodes on a phone.
 *
 * Keyed on `surfaceResetToken`: when the engine bumps it, the view is disposed and rebuilt so the
 * decoder gets a genuinely fresh Surface.
 */
@Composable
private fun BareVideoSurface(player: OwnTVPlayer, modifier: Modifier = Modifier) {
    val token by player.surfaceResetToken.collectAsStateWithLifecycle()
    androidx.compose.runtime.key(token) {
        AndroidView(modifier = modifier, factory = { ctx -> BareSurfaceView(ctx, player) })
    }
}

private class BareSurfaceView(context: Context, private val player: OwnTVPlayer) :
    SurfaceView(context), SurfaceHolder.Callback {

    init {
        holder.addCallback(this)
    }

    override fun surfaceCreated(holder: SurfaceHolder) = player.attachSurface(holder.surface)

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) =
        player.setSurfaceSize(width, height)

    override fun surfaceDestroyed(holder: SurfaceHolder) = player.detachSurface()
}
