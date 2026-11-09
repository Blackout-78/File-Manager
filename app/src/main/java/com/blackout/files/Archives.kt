package com.blackout.files

import org.apache.commons.compress.archivers.sevenz.SevenZFile
import org.apache.commons.compress.archivers.sevenz.SevenZOutputFile
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

object Archives {
    fun isArchive(f: File): Boolean =
        f.isFile && (f.extension.lowercase() == "zip" || f.extension.lowercase() == "7z")

    private fun safeChild(dest: File, name: String): File {
        val f = File(dest, name)
        val base = dest.canonicalPath + File.separator
        if (!f.canonicalPath.startsWith(base)) throw SecurityException("Bad entry path: $name")
        return f
    }

    fun zip(sources: List<File>, dest: File) {
        ZipOutputStream(BufferedOutputStream(FileOutputStream(dest))).use { z ->
            fun add(f: File, name: String) {
                if (f.isDirectory) {
                    z.putNextEntry(ZipEntry("$name/"))
                    z.closeEntry()
                    f.listFiles()?.forEach { add(it, "$name/${it.name}") }
                } else {
                    z.putNextEntry(ZipEntry(name))
                    FileInputStream(f).use { it.copyTo(z) }
                    z.closeEntry()
                }
            }
            sources.forEach { add(it, it.name) }
        }
    }

    fun sevenZ(sources: List<File>, dest: File) {
        SevenZOutputFile(dest).use { out ->
            fun add(f: File, name: String) {
                if (f.isDirectory) {
                    out.putArchiveEntry(out.createArchiveEntry(f, name))
                    out.closeArchiveEntry()
                    f.listFiles()?.forEach { add(it, "$name/${it.name}") }
                } else {
                    out.putArchiveEntry(out.createArchiveEntry(f, name))
                    FileInputStream(f).use { ins ->
                        val buf = ByteArray(65536)
                        var n = ins.read(buf)
                        while (n > 0) {
                            out.write(buf, 0, n)
                            n = ins.read(buf)
                        }
                    }
                    out.closeArchiveEntry()
                }
            }
            sources.forEach { add(it, it.name) }
        }
    }

    fun extract(src: File, destDir: File) {
        if (src.extension.lowercase() == "7z") extract7z(src, destDir) else extractZip(src, destDir)
    }

    private fun extractZip(src: File, destDir: File) {
        ZipInputStream(BufferedInputStream(FileInputStream(src))).use { z ->
            var e = z.nextEntry
            while (e != null) {
                val out = safeChild(destDir, e.name)
                if (e.isDirectory) {
                    out.mkdirs()
                } else {
                    out.parentFile?.mkdirs()
                    FileOutputStream(out).use { z.copyTo(it) }
                }
                z.closeEntry()
                e = z.nextEntry
            }
        }
    }

    private fun extract7z(src: File, destDir: File) {
        SevenZFile(src).use { z ->
            var e = z.nextEntry
            while (e != null) {
                val out = safeChild(destDir, e.name)
                if (e.isDirectory) {
                    out.mkdirs()
                } else {
                    out.parentFile?.mkdirs()
                    FileOutputStream(out).use { os ->
                        val buf = ByteArray(65536)
                        var n = z.read(buf)
                        while (n > 0) {
                            os.write(buf, 0, n)
                            n = z.read(buf)
                        }
                    }
                }
                e = z.nextEntry
            }
        }
    }
}
