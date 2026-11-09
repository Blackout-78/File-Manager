package com.blackout.files

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@Composable
fun BrowseScreen(
    st: UiState,
    dir: String,
    setDir: (String) -> Unit,
    onOpen: (File) -> Unit,
    onShare: (File) -> Unit
) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    var refresh by remember { mutableIntStateOf(0) }
    var searching by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    var menuFor by remember { mutableStateOf<File?>(null) }
    var renameFor by remember { mutableStateOf<File?>(null) }
    var extractFor by remember { mutableStateOf<File?>(null) }
    var deleteTargets by remember { mutableStateOf<List<File>>(emptyList()) }
    var newFolder by remember { mutableStateOf(false) }
    var bulkMenu by remember { mutableStateOf(false) }
    var dragging by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var selected by remember { mutableStateOf<Set<String>>(emptySet()) }
    var cats by remember { mutableStateOf<Map<FileType, Long>>(emptyMap()) }
    val root = FileOps.root.path
    val setDirNow by rememberUpdatedState(setDir)
    val onOpenNow by rememberUpdatedState(onOpen)

    BackHandler(enabled = selected.isNotEmpty()) { selected = emptySet() }
    BackHandler(enabled = selected.isEmpty() && dir != root) { setDir(File(dir).parent ?: root) }
    LaunchedEffect(dir) { selected = emptySet() }
    LaunchedEffect(refresh) { cats = withContext(Dispatchers.IO) { FileOps.scan() } }

    val all = remember(dir, st.showHidden, refresh) { FileOps.list(File(dir), st.showHidden) }
    val shown = if (query.isBlank()) all else all.filter { it.name.contains(query, ignoreCase = true) }
    val shownNow by rememberUpdatedState(shown)
    val storage = remember(refresh) { FileOps.storage() }

    fun toast(m: String) {
        Toast.makeText(ctx, m, Toast.LENGTH_SHORT).show()
    }

    fun itemAt(y: Float): File? {
        val info = listState.layoutInfo.visibleItemsInfo
            .firstOrNull { y >= it.offset && y < it.offset + it.size } ?: return null
        return shownNow.getOrNull(info.index)
    }

    fun tapItem(f: File) {
        if (selected.isNotEmpty()) {
            selected = if (f.path in selected) selected - f.path else selected + f.path
        } else if (f.isDirectory) {
            query = ""
            searching = false
            setDirNow(f.path)
        } else if (Archives.isArchive(f)) {
            extractFor = f
        } else {
            onOpenNow(f)
        }
    }

    fun trashFiles(items: List<File>) {
        var ok = 0
        items.forEach { if (FileOps.toTrash(st.prefs, it)) ok++ }
        selected = emptySet()
        refresh++
        toast(if (ok == items.size) "Moved $ok to trash" else "Moved $ok of ${items.size} to trash")
    }

    fun compress(kind: String) {
        val items = shown.filter { it.path in selected }
        if (items.isEmpty()) return
        val first = items[0]
        val base = if (items.size == 1) {
            if (first.isDirectory) first.name else first.nameWithoutExtension
        } else {
            "Archive"
        }
        val dest = FileOps.uniqueDest(File(dir), "$base.$kind")
        selected = emptySet()
        scope.launch {
            busy = true
            val ok = withContext(Dispatchers.IO) {
                try {
                    if (kind == "zip") Archives.zip(items, dest) else Archives.sevenZ(items, dest)
                    true
                } catch (e: Exception) {
                    dest.delete()
                    false
                }
            }
            busy = false
            refresh++
            toast(if (ok) "Created ${dest.name}" else "Couldn't create the archive")
        }
    }

    fun extract(a: File) {
        val out = FileOps.uniqueDest(File(dir), a.nameWithoutExtension)
        scope.launch {
            busy = true
            val ok = withContext(Dispatchers.IO) {
                try {
                    out.mkdirs()
                    Archives.extract(a, out)
                    true
                } catch (e: Exception) {
                    out.deleteRecursively()
                    false
                }
            }
            busy = false
            refresh++
            toast(if (ok) "Extracted to ${out.name}" else "Couldn't extract this archive")
        }
    }

    Column(Modifier.fillMaxSize()) {
        if (selected.isNotEmpty()) {
            Row(
                Modifier.fillMaxWidth().padding(start = 4.dp, end = 4.dp, top = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { selected = emptySet() }) {
                    Icon(Icons.Filled.Close, contentDescription = "Clear selection")
                }
                Text(
                    "${selected.size} selected",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = { selected = shown.map { it.path }.toSet() }) {
                    Icon(Icons.Filled.SelectAll, contentDescription = "Select all")
                }
                IconButton(onClick = { trashFiles(shown.filter { it.path in selected }) }) {
                    Icon(Icons.Filled.Delete, contentDescription = "Move to trash")
                }
                Box {
                    IconButton(onClick = { bulkMenu = true }) {
                        Icon(Icons.Filled.MoreVert, contentDescription = "More")
                    }
                    DropdownMenu(expanded = bulkMenu, onDismissRequest = { bulkMenu = false }) {
                        DropdownMenuItem(
                            text = { Text("Copy to…") },
                            onClick = {
                                st.clip = shown.filter { it.path in selected }
                                st.clipMove = false
                                selected = emptySet()
                                bulkMenu = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Move to…") },
                            onClick = {
                                st.clip = shown.filter { it.path in selected }
                                st.clipMove = true
                                selected = emptySet()
                                bulkMenu = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Compress to ZIP") },
                            onClick = { bulkMenu = false; compress("zip") }
                        )
                        DropdownMenuItem(
                            text = { Text("Compress to 7z") },
                            onClick = { bulkMenu = false; compress("7z") }
                        )
                        DropdownMenuItem(
                            text = { Text("Delete forever…") },
                            onClick = {
                                deleteTargets = shown.filter { it.path in selected }
                                bulkMenu = false
                            }
                        )
                    }
                }
            }
        } else {
            Header("Browse") {
                IconButton(onClick = {
                    searching = !searching
                    if (!searching) query = ""
                }) { Icon(Icons.Filled.Search, contentDescription = "Search") }
                IconButton(onClick = { newFolder = true }) {
                    Icon(Icons.Filled.CreateNewFolder, contentDescription = "New folder")
                }
            }
        }

        if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())

        if (searching && selected.isEmpty()) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                singleLine = true,
                placeholder = { Text("Search this folder") },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
            )
        }

        if (st.clip.isNotEmpty()) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.secondaryContainer)
                    .padding(start = 12.dp, end = 4.dp, top = 2.dp, bottom = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    (if (st.clipMove) "Moving " else "Copying ") +
                        (if (st.clip.size == 1) st.clip[0].name else "${st.clip.size} items"),
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                TextButton(onClick = {
                    val items = st.clip
                    val mv = st.clipMove
                    val target = File(dir)
                    st.clip = emptyList()
                    scope.launch {
                        busy = true
                        val results = withContext(Dispatchers.IO) {
                            items.map { it to FileOps.paste(it, target, mv) }
                        }
                        results.forEach { (src, r) ->
                            if (r != null && mv) st.renameFav(src.path, r.path)
                        }
                        busy = false
                        refresh++
                        val ok = results.count { it.second != null }
                        toast(if (ok == items.size) "Done" else "Pasted $ok of ${items.size}")
                    }
                }) { Text("Paste here") }
                TextButton(onClick = { st.clip = emptyList() }) { Text("Cancel") }
            }
        }

        StorageCard(st, storage.first, storage.second, cats)
        Text(
            "Internal" + dir.removePrefix(root).replace("/", "  /  "),
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
        )

        if (shown.isEmpty()) {
            Text(
                "This folder is empty.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(16.dp)
            )
        } else {
            LazyColumn(
                state = listState,
                userScrollEnabled = !dragging,
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onTap = { o -> itemAt(o.y)?.let { tapItem(it) } },
                            onLongPress = { o -> itemAt(o.y)?.let { selected = selected + it.path } }
                        )
                    }
                    .pointerInput(Unit) {
                        detectDragGesturesAfterLongPress(
                            onDragStart = { o ->
                                dragging = true
                                itemAt(o.y)?.let { selected = selected + it.path }
                            },
                            onDragEnd = { dragging = false },
                            onDragCancel = { dragging = false },
                            onDrag = { change, _ ->
                                itemAt(change.position.y)?.let { selected = selected + it.path }
                            }
                        )
                    }
            ) {
                items(shown, key = { it.path }) { f ->
                    val sel = f.path in selected
                    Box {
                        FileRow(
                            st = st,
                            f = f,
                            sub = rowSub(f),
                            fav = f.path in st.favs,
                            onClick = {},
                            selected = sel,
                            interactive = false
                        ) {
                            if (selected.isNotEmpty()) {
                                Checkbox(checked = sel, onCheckedChange = null)
                            } else {
                                IconButton(onClick = { menuFor = f }) {
                                    Icon(Icons.Filled.MoreVert, contentDescription = "More")
                                }
                            }
                        }
                        DropdownMenu(expanded = menuFor == f, onDismissRequest = { menuFor = null }) {
                            DropdownMenuItem(
                                text = { Text("Select") },
                                onClick = { selected = selected + f.path; menuFor = null }
                            )
                            DropdownMenuItem(
                                text = { Text(if (f.path in st.favs) "Remove favorite" else "Add to favorites") },
                                onClick = { st.toggleFav(f); menuFor = null }
                            )
                            DropdownMenuItem(
                                text = { Text("Rename") },
                                onClick = { renameFor = f; menuFor = null }
                            )
                            DropdownMenuItem(
                                text = { Text("Copy to…") },
                                onClick = { st.clip = listOf(f); st.clipMove = false; menuFor = null }
                            )
                            DropdownMenuItem(
                                text = { Text("Move to…") },
                                onClick = { st.clip = listOf(f); st.clipMove = true; menuFor = null }
                            )
                            if (f.isFile) {
                                DropdownMenuItem(
                                    text = { Text("Share") },
                                    onClick = { onShare(f); menuFor = null }
                                )
                            }
                            if (Archives.isArchive(f)) {
                                DropdownMenuItem(
                                    text = { Text("Extract here") },
                                    onClick = { menuFor = null; extract(f) }
                                )
                            }
                            DropdownMenuItem(
                                text = { Text("Move to trash") },
                                onClick = { menuFor = null; trashFiles(listOf(f)) }
                            )
                            DropdownMenuItem(
                                text = { Text("Delete forever…") },
                                onClick = { menuFor = null; deleteTargets = listOf(f) }
                            )
                        }
                    }
                }
            }
        }
    }

    renameFor?.let { f ->
        NameDialog(
            title = "Rename",
            initial = f.name,
            onOk = { n ->
                val moved = FileOps.rename(f, n)
                if (moved != null) st.renameFav(f.path, moved.path)
                renameFor = null
                refresh++
            },
            onDismiss = { renameFor = null }
        )
    }

    if (newFolder) {
        NameDialog(
            title = "New folder",
            initial = "",
            onOk = { n ->
                File(dir, n).mkdirs()
                newFolder = false
                refresh++
            },
            onDismiss = { newFolder = false }
        )
    }

    extractFor?.let { a ->
        AlertDialog(
            onDismissRequest = { extractFor = null },
            title = { Text("Extract archive") },
            text = { Text("Extract ${a.name} into a new folder here?") },
            confirmButton = {
                TextButton(onClick = { extractFor = null; extract(a) }) { Text("Extract") }
            },
            dismissButton = { TextButton(onClick = { extractFor = null }) { Text("Cancel") } }
        )
    }

    if (deleteTargets.isNotEmpty()) {
        val n = deleteTargets.size
        AlertDialog(
            onDismissRequest = { deleteTargets = emptyList() },
            title = {
                Text(if (n == 1) "Delete ${deleteTargets[0].name} forever?" else "Delete $n items forever?")
            },
            text = {
                Text("This can't be undone. Use Move to trash to keep a copy for ${st.trashDays} days.")
            },
            confirmButton = {
                TextButton(onClick = {
                    val items = deleteTargets
                    deleteTargets = emptyList()
                    selected = emptySet()
                    scope.launch {
                        busy = true
                        withContext(Dispatchers.IO) { items.forEach { it.deleteRecursively() } }
                        busy = false
                        refresh++
                        toast(if (items.size == 1) "Deleted" else "Deleted ${items.size} items")
                    }
                }) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { deleteTargets = emptyList() }) { Text("Cancel") } }
        )
    }
}
