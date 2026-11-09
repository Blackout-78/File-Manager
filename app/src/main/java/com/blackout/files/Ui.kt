package com.blackout.files

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Folder as OFolder
import androidx.compose.material.icons.outlined.Image as OImage
import androidx.compose.material.icons.outlined.Movie as OMovie
import androidx.compose.material.icons.outlined.MusicNote as OMusicNote
import androidx.compose.material.icons.outlined.PictureAsPdf as OPictureAsPdf
import androidx.compose.material.icons.outlined.Description as ODescription
import androidx.compose.material.icons.outlined.FolderZip as OFolderZip
import androidx.compose.material.icons.outlined.Android as OAndroid
import androidx.compose.material.icons.outlined.InsertDriveFile as OInsertDriveFile
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class UiState(val prefs: Prefs) {
    var hue by mutableFloatStateOf(prefs.accentHue)
    var showHidden by mutableStateOf(prefs.showHidden)
    var trashDays by mutableIntStateOf(prefs.trashDays)
    var favs by mutableStateOf(prefs.favorites())
    var clip by mutableStateOf<List<File>>(emptyList())
    var clipMove by mutableStateOf(false)
    var iconBg by mutableIntStateOf(prefs.iconBg)
    var iconFg by mutableIntStateOf(prefs.iconFg)
    var iconStyle by mutableIntStateOf(prefs.iconStyle)

    fun saveIconColors() {
        prefs.iconBg = iconBg
        prefs.iconFg = iconFg
        prefs.iconStyle = iconStyle
    }
    val colors = mutableStateMapOf<FileType, Int>().also { m ->
        FileType.values().forEach { m[it] = prefs.typeColor(it) }
    }

    fun color(t: FileType): Color = Color(colors[t] ?: t.defaultColor)

    fun setColor(t: FileType, c: Int) {
        colors[t] = c
        prefs.setTypeColor(t, c)
    }

    fun resetColors() {
        prefs.resetTypeColors()
        FileType.values().forEach { colors[it] = it.defaultColor }
    }

    fun toggleFav(f: File) {
        val p = f.path
        favs = if (p in favs) favs - p else favs + p
        prefs.setFavorites(favs)
    }

    fun renameFav(old: String, new: String) {
        if (old in favs) {
            favs = favs - old + new
            prefs.setFavorites(favs)
        }
    }
}

fun nightScheme(hue: Float) = darkColorScheme(
    primary = Color.hsv(hue, 0.55f, 0.97f),
    onPrimary = Color(0xFF0B0D12),
    background = Color(0xFF0B0D12),
    onBackground = Color(0xFFE8EAF0),
    surface = Color(0xFF0E1118),
    onSurface = Color(0xFFE8EAF0),
    surfaceVariant = Color(0xFF141824),
    onSurfaceVariant = Color(0xFF9AA0B2),
    outlineVariant = Color(0xFF1C2030),
    secondaryContainer = Color.hsv(hue, 0.45f, 0.35f),
    onSecondaryContainer = Color(0xFFE8EAF0)
)

fun iconFor(t: FileType): ImageVector = when (t) {
    FileType.FOLDER -> Icons.Outlined.OFolder
    FileType.IMAGE -> Icons.Outlined.OImage
    FileType.VIDEO -> Icons.Outlined.OMovie
    FileType.AUDIO -> Icons.Outlined.OMusicNote
    FileType.PDF -> Icons.Outlined.OPictureAsPdf
    FileType.DOC -> Icons.Outlined.ODescription
    FileType.ARCHIVE -> Icons.Outlined.OFolderZip
    FileType.APK -> Icons.Outlined.OAndroid
    FileType.OTHER -> Icons.Outlined.OInsertDriveFile
}

fun rowSub(f: File): String =
    if (f.isDirectory) {
        "${f.list()?.size ?: 0} items"
    } else {
        FileOps.size(f.length()) + "  ·  " + FileOps.date(f.lastModified())
    }

@Composable
fun FilesApp(
    st: UiState,
    access: Boolean,
    onAsk: () -> Unit,
    onOpen: (File) -> Unit,
    onShare: (File) -> Unit
) {
    MaterialTheme(colorScheme = nightScheme(st.hue)) {
        var tab by rememberSaveable { mutableStateOf(0) }
        var trashOpen by rememberSaveable { mutableStateOf(false) }
        var dir by rememberSaveable { mutableStateOf(FileOps.root.path) }
        val accent = MaterialTheme.colorScheme.primary
        val muted = MaterialTheme.colorScheme.onSurfaceVariant

        LaunchedEffect(access) {
            if (access) {
                withContext(Dispatchers.IO) { FileOps.purge(st.prefs, st.trashDays) }
            }
        }

        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            bottomBar = {
                NavigationBar(containerColor = Color(0xFF0E1118)) {
                    val tabs = listOf(
                        "Browse" to Icons.Filled.Folder,
                        "Recent" to Icons.Filled.Schedule,
                        "Favorites" to Icons.Filled.Star,
                        "Settings" to Icons.Filled.Settings
                    )
                    tabs.forEachIndexed { i, t ->
                        NavigationBarItem(
                            selected = tab == i,
                            onClick = { tab = i; trashOpen = false },
                            icon = { Icon(t.second, contentDescription = t.first) },
                            label = { Text(t.first, fontSize = 11.sp) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = accent,
                                selectedTextColor = accent,
                                indicatorColor = MaterialTheme.colorScheme.secondaryContainer,
                                unselectedIconColor = muted,
                                unselectedTextColor = muted
                            )
                        )
                    }
                }
            }
        ) { pad ->
            Box(Modifier.padding(pad).fillMaxSize()) {
                when {
                    !access -> PermissionScreen(onAsk)
                    trashOpen -> TrashScreen(st) { trashOpen = false }
                    tab == 0 -> BrowseScreen(st, dir, { dir = it }, onOpen, onShare)
                    tab == 1 -> RecentScreen(st, onOpen)
                    tab == 2 -> FavoritesScreen(st, onOpen) { dir = it; tab = 0 }
                    else -> SettingsScreen(st) { trashOpen = true }
                }
            }
        }
    }
}

@Composable
fun PermissionScreen(onAsk: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            Icons.Filled.Folder,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(56.dp)
        )
        Spacer(Modifier.height(16.dp))
        Text("File Manager needs access to your storage", fontSize = 18.sp, fontWeight = FontWeight.Medium)
        Spacer(Modifier.height(8.dp))
        Text(
            "It's the only way to browse and manage your files. Nothing leaves this phone.",
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(20.dp))
        Button(onClick = onAsk) { Text("Allow access") }
    }
}

@Composable
fun Header(title: String, actions: @Composable RowScope.() -> Unit = {}) {
    Row(
        Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp, top = 12.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, fontSize = 22.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
        actions()
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FileRow(
    st: UiState,
    f: File,
    sub: String,
    fav: Boolean,
    onClick: () -> Unit,
    onLong: (() -> Unit)? = null,
    title: String = f.name,
    selected: Boolean = false,
    interactive: Boolean = true,
    trailing: @Composable () -> Unit = {}
) {
    val t = FileOps.typeOf(f)
    val c = st.color(t)
    Row(
        Modifier
            .fillMaxWidth()
            .background(if (selected) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent)
            .then(if (interactive) Modifier.combinedClickable(onClick = onClick, onLongClick = onLong) else Modifier)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier.size(40.dp).clip(CircleShape).background(c),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                iconFor(t),
                contentDescription = null,
                tint = if (c.luminance() > 0.6f) Color(0xFF121214) else Color.White,
                modifier = Modifier.size(22.dp)
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                sub,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (fav) {
            Icon(
                Icons.Filled.Star,
                contentDescription = "Favorite",
                tint = st.color(FileType.FOLDER),
                modifier = Modifier.size(16.dp)
            )
        }
        trailing()
    }
}

@Composable
fun NameDialog(title: String, initial: String, onOk: (String) -> Unit, onDismiss: () -> Unit) {
    var text by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { OutlinedTextField(value = text, onValueChange = { text = it }, singleLine = true) },
        confirmButton = {
            TextButton(onClick = {
                if (text.isNotBlank() && !text.contains('/')) onOk(text.trim())
            }) { Text("OK") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun StorageCard(st: UiState, total: Long, free: Long, cats: Map<FileType, Long>) {
    val used = (total - free).coerceAtLeast(1L)
    val counted = cats.values.sum()
    val rest = (total - counted).coerceAtLeast(1L)
    Column(
        Modifier
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(12.dp)
    ) {
        Row {
            Text("Internal storage", modifier = Modifier.weight(1f))
            Text(
                FileOps.size(used) + " of " + FileOps.size(total),
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.height(8.dp))
        Row(
            Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(Color(0xFF2A3042))
        ) {
            FileType.values().forEach { t ->
                val s = cats[t] ?: 0L
                if (s > 0L) {
                    Box(Modifier.weight(s.toFloat()).fillMaxHeight().background(st.color(t)))
                }
            }
            Box(Modifier.weight(rest.toFloat()).fillMaxHeight())
        }
    }
}

@Composable
fun RecentScreen(st: UiState, onOpen: (File) -> Unit) {
    var found by remember { mutableStateOf<List<File>?>(null) }
    LaunchedEffect(st.showHidden) {
        found = withContext(Dispatchers.IO) { FileOps.recent(st.showHidden) }
    }
    Column(Modifier.fillMaxSize()) {
        Header("Recent")
        val list = found
        if (list == null) {
            Text(
                "Looking through your files…",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(16.dp)
            )
        } else if (list.isEmpty()) {
            Text(
                "No recent files yet.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(16.dp)
            )
        } else {
            LazyColumn(Modifier.fillMaxSize()) {
                items(list, key = { it.path }) { f ->
                    FileRow(
                        st = st,
                        f = f,
                        sub = FileOps.date(f.lastModified()) + "  ·  " + (f.parentFile?.name ?: ""),
                        fav = f.path in st.favs,
                        onClick = { onOpen(f) }
                    )
                }
            }
        }
    }
}

@Composable
fun FavoritesScreen(st: UiState, onOpen: (File) -> Unit, onFolder: (String) -> Unit) {
    val list = st.favs.map { File(it) }.filter { it.exists() }.sortedBy { it.name.lowercase() }
    Column(Modifier.fillMaxSize()) {
        Header("Favorites")
        if (list.isEmpty()) {
            Text(
                "Hold a file or folder in Browse and choose Add to favorites.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(16.dp)
            )
        } else {
            LazyColumn(Modifier.fillMaxSize()) {
                items(list, key = { it.path }) { f ->
                    FileRow(
                        st = st,
                        f = f,
                        sub = rowSub(f),
                        fav = true,
                        onClick = { if (f.isDirectory) onFolder(f.path) else onOpen(f) },
                        onLong = { st.toggleFav(f) }
                    )
                }
                item {
                    Text(
                        "Hold an item to remove it from favorites.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun Pill(text: String, selected: Boolean, onClick: () -> Unit) {
    val c = MaterialTheme.colorScheme
    Box(
        Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(if (selected) c.primary else c.surfaceVariant)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 7.dp)
    ) {
        Text(text, fontSize = 13.sp, color = if (selected) c.onPrimary else c.onSurface)
    }
}

@Composable
fun SectionLabel(text: String) {
    Text(
        text,
        fontSize = 13.sp,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 4.dp)
    )
}

@Composable
fun SettingsScreen(st: UiState, openTrash: () -> Unit) {
    val swatches = listOf(
        0xFFF5B93A, 0xFFEC6FA0, 0xFF9B8CF0, 0xFF3FCB9B, 0xFF5AA9F0,
        0xFFEF5B5B, 0xFFF08A5D, 0xFF7BC74D, 0xFF9AA0B2
    ).map { it.toInt() }
    val ctx = LocalContext.current
    var chosenIcon by remember { mutableStateOf(AppIcons.current(ctx)) }

    LazyColumn(Modifier.fillMaxSize()) {
        item { Header("Settings") }
        item { CrashReport() }

        item { SectionLabel("Theme color") }
        item {
            Slider(
                value = st.hue,
                onValueChange = { st.hue = it },
                onValueChangeFinished = { st.prefs.accentHue = st.hue },
                valueRange = 0f..360f,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }

        item { SectionLabel("App icon") }
        item {
            Row(
                Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AppIcons.options.forEach { opt ->
                    val sel = chosenIcon == opt.alias
                    Column(
                        Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .clickable {
                                AppIcons.set(ctx, opt.alias)
                                chosenIcon = opt.alias
                                Toast.makeText(
                                    ctx,
                                    "Icon changed. Your launcher may take a few seconds to update.",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                            .then(
                                if (sel) Modifier.border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(14.dp))
                                else Modifier
                            )
                            .padding(6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(Modifier.size(60.dp).clip(RoundedCornerShape(16.dp)).background(Color(opt.bg))) {
                            Image(
                                painterResource(opt.fg),
                                contentDescription = opt.label,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(opt.label, fontSize = 11.sp)
                    }
                }
            }
        }

        item { IconColors(st) }

        item { SectionLabel("File type icon colors") }
        items(FileType.values().toList()) { t ->
            Column(Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier.size(28.dp).clip(CircleShape)
                            .background(st.color(t)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(iconFor(t), contentDescription = null, tint = if (st.color(t).luminance() > 0.6f) Color(0xFF121214) else Color.White, modifier = Modifier.size(16.dp))
                    }
                    Spacer(Modifier.width(10.dp))
                    Text(t.label)
                }
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    swatches.forEach { sw ->
                        val selected = (st.colors[t] ?: t.defaultColor) == sw
                        Box(
                            Modifier
                                .size(26.dp)
                                .clip(CircleShape)
                                .background(Color(sw))
                                .then(if (selected) Modifier.border(2.dp, Color.White, CircleShape) else Modifier)
                                .clickable { st.setColor(t, sw) }
                        )
                    }
                }
            }
        }
        item {
            TextButton(
                onClick = { st.resetColors() },
                modifier = Modifier.padding(horizontal = 8.dp)
            ) { Text("Reset icon colors") }
        }

        item { SectionLabel("Browsing") }
        item {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Show hidden files", modifier = Modifier.weight(1f))
                Switch(
                    checked = st.showHidden,
                    onCheckedChange = { st.showHidden = it; st.prefs.showHidden = it }
                )
            }
        }

        item { SectionLabel("Trash") }
        item {
            Row(
                Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(7, 30, 90).forEach { d ->
                    Pill("$d days", st.trashDays == d) { st.trashDays = d; st.prefs.trashDays = d }
                }
            }
        }
        item {
            Row(
                Modifier.fillMaxWidth().clickable { openTrash() }.padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Filled.Delete, contentDescription = null, tint = st.color(FileType.PDF))
                Spacer(Modifier.width(12.dp))
                Text("Open trash")
            }
        }

        item {
            Text(
                "File Manager works fully offline and collects no data. Everything stays on this phone.",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}

@Composable
fun TrashScreen(st: UiState, onBack: () -> Unit) {
    val ctx = LocalContext.current
    var entries by remember { mutableStateOf(st.prefs.trash()) }
    BackHandler { onBack() }

    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().padding(start = 4.dp, end = 8.dp, top = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Text("Trash", fontSize = 22.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
            if (entries.isNotEmpty()) {
                TextButton(onClick = {
                    entries.forEach { FileOps.deleteForever(st.prefs, it) }
                    entries = st.prefs.trash()
                }) { Text("Empty") }
            }
        }
        Text(
            "Items are deleted for good after ${st.trashDays} days.",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
        )
        if (entries.isEmpty()) {
            Text(
                "Trash is empty.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(16.dp)
            )
        } else {
            LazyColumn(Modifier.fillMaxSize()) {
                items(entries, key = { it.trashName }) { e ->
                    FileRow(
                        st = st,
                        f = FileOps.trashFile(e),
                        sub = "From " + (File(e.original).parentFile?.name ?: ""),
                        fav = false,
                        onClick = {},
                        title = File(e.original).name
                    ) {
                        IconButton(onClick = {
                            if (!FileOps.restore(st.prefs, e)) {
                                Toast.makeText(ctx, "Couldn't restore this item", Toast.LENGTH_SHORT).show()
                            }
                            entries = st.prefs.trash()
                        }) { Icon(Icons.Filled.Restore, contentDescription = "Restore") }
                        IconButton(onClick = {
                            FileOps.deleteForever(st.prefs, e)
                            entries = st.prefs.trash()
                        }) { Icon(Icons.Filled.DeleteForever, contentDescription = "Delete forever") }
                    }
                }
            }
        }
    }
}
