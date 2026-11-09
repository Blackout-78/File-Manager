package com.blackout.files

import android.os.Environment
import android.os.StatFs
import android.webkit.MimeTypeMap
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class FileType(val label: String, val defaultColor: Int) {
    FOLDER("Folders", 0xFFF5B93A.toInt()),
    IMAGE("Photos", 0xFFEC6FA0.toInt()),
    VIDEO("Video", 0xFF9B8CF0.toInt()),
    AUDIO("Music", 0xFF3FCB9B.toInt()),
    PDF("PDF", 0xFFEF5B5B.toInt()),
    DOC("Documents", 0xFF5AA9F0.toInt()),
    ARCHIVE("Archives", 0xFFF08A5D.toInt()),
    APK("Apps", 0xFF7BC74D.toInt()),
    OTHER("Other", 0xFF9AA0B2.toInt())
}

object FileOps {
    val root: File = Environment.getExternalStorageDirectory()
    private val trashDir: File get() = File(root, ".FilesTrash")

    fun typeOf(f: File): FileType {
        if (f.isDirectory) return FileType.FOLDER
        return when (f.extension.lowercase()) {
            "jpg", "jpeg", "png", "gif", "webp", "heic", "heif", "bmp", "avif" -> FileType.IMAGE
            "mp4", "mkv", "webm", "avi", "mov", "3gp", "m4v" -> FileType.VIDEO
            "mp3", "m4a", "aac", "wav", "flac", "ogg", "opus", "wma" -> FileType.AUDIO
            "pdf" -> FileType.PDF
            "txt", "doc", "docx", "odt", "rtf", "xls", "xlsx", "ppt", "pptx", "csv", "md", "json", "xml", "html" -> FileType.DOC
            "zip", "rar", "7z", "tar", "gz", "bz2", "xz" -> FileType.ARCHIVE
            "apk", "xapk", "apks" -> FileType.APK
            else -> FileType.OTHER
        }
    }

    fun mime(f: File): String =
        MimeTypeMap.getSingleton().getMimeTypeFromExtension(f.extension.lowercase()) ?: "*/*"

    fun list(dir: File, showHidden: Boolean): List<File> {
        val all = dir.listFiles() ?: return emptyList()
        return all
            .filter { showHidden || !it.name.startsWith(".") }
            .sortedWith(compareBy<File>({ !it.isDirectory }, { it.name.lowercase() }))
    }

    fun size(b: Long): String {
        if (b < 1024) return "$b B"
        val units = arrayOf("KB", "MB", "GB", "TB")
        var v = b.toDouble()
        var i = -1
        do {
            v /= 1024.0
            i++
        } while (v >= 1024.0 && i < units.size - 1)
        return String.format(Locale.US, "%.1f %s", v, units[i])
    }

    fun date(ms: Long): String = SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date(ms))

    fun storage(): Pair<Long, Long> {
        val s = StatFs(root.path)
        val total = s.blockCountLong * s.blockSizeLong
        val free = s.availableBlocksLong * s.blockSizeLong
        return Pair(total, free)
    }

    fun scan(): Map<FileType, Long> {
        val m = HashMap<FileType, Long>()
        root.walkTopDown()
            .onEnter { d -> !(d.parentFile == root && d.name == "Android") }
            .forEach { f ->
                if (f.isFile) {
                    val t = typeOf(f)
                    m[t] = (m[t] ?: 0L) + f.length()
                }
            }
        return m
    }

    fun recent(showHidden: Boolean): List<File> {
        return root.walkTopDown()
            .onEnter { d ->
                !(d.parentFile == root && d.name == "Android") &&
                    (showHidden || d == root || !d.name.startsWith("."))
            }
            .filter { it.isFile && (showHidden || !it.name.startsWith(".")) }
            .map { it to it.lastModified() }
            .sortedByDescending { it.second }
            .take(60)
            .map { it.first }
            .toList()
    }

    fun rename(f: File, name: String): File? {
        val clean = name.trim()
        if (clean.isEmpty() || clean.contains('/')) return null
        val dest = File(f.parentFile, clean)
        if (dest.exists()) return null
        return if (f.renameTo(dest)) dest else null
    }

    private fun uniqueDest(dir: File, name: String): File {
        var d = File(dir, name)
        if (!d.exists()) return d
        val base = File(name).nameWithoutExtension
        val ext = File(name).extension.let { if (it.isEmpty()) "" else ".$it" }
        var i = 1
        while (d.exists()) {
            d = File(dir, "$base ($i)$ext")
            i++
        }
        return d
    }

    fun paste(src: File, destDir: File, move: Boolean): File? {
        if (!src.exists() || !destDir.isDirectory) return null
        if (src.isDirectory && (destDir.path == src.path || destDir.path.startsWith(src.path + "/"))) return null
        if (move && src.parentFile == destDir) return src
        val dest = uniqueDest(destDir, src.name)
        return try {
            if (move) {
                if (!src.renameTo(dest)) {
                    if (!src.copyRecursively(dest, overwrite = false)) return null
                    src.deleteRecursively()
                }
                dest
            } else if (src.copyRecursively(dest, overwrite = false)) {
                dest
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    fun trashFile(item: TrashItem): File = File(trashDir, item.trashName)

    fun toTrash(prefs: Prefs, f: File): Boolean {
        trashDir.mkdirs()
        val now = System.currentTimeMillis()
        val name = now.toString() + "_" + f.name
        if (!f.renameTo(File(trashDir, name))) return false
        prefs.setTrash(prefs.trash() + TrashItem(name, f.path, now))
        return true
    }

    fun restore(prefs: Prefs, item: TrashItem): Boolean {
        val src = trashFile(item)
        var dest = File(item.original)
        dest.parentFile?.mkdirs()
        if (dest.exists()) {
            val ext = if (dest.extension.isEmpty()) "" else "." + dest.extension
            dest = File(dest.parentFile, dest.nameWithoutExtension + " (restored)" + ext)
        }
        val ok = src.renameTo(dest)
        if (ok) prefs.setTrash(prefs.trash().filter { it.trashName != item.trashName })
        return ok
    }

    fun deleteForever(prefs: Prefs, item: TrashItem) {
        trashFile(item).deleteRecursively()
        prefs.setTrash(prefs.trash().filter { it.trashName != item.trashName })
    }

    fun purge(prefs: Prefs, days: Int) {
        val limit = System.currentTimeMillis() - days * 86_400_000L
        prefs.trash().filter { it.deletedAt < limit }.forEach { deleteForever(prefs, it) }
    }
}
