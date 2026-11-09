package com.blackout.files

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import java.io.File

class MainActivity : ComponentActivity() {
    private var access by mutableStateOf(false)

    private val legacyPermission = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { access = hasAccess() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val state = UiState(Prefs(this))
        access = hasAccess()
        setContent {
            FilesApp(
                st = state,
                access = access,
                onAsk = { askAccess() },
                onOpen = { openFile(it) },
                onShare = { shareFile(it) }
            )
        }
    }

    override fun onResume() {
        super.onResume()
        access = hasAccess()
    }

    private fun hasAccess(): Boolean {
        return if (Build.VERSION.SDK_INT >= 30) {
            Environment.isExternalStorageManager()
        } else {
            ContextCompat.checkSelfPermission(
                this, Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun askAccess() {
        if (Build.VERSION.SDK_INT >= 30) {
            val i = Intent(
                Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                Uri.parse("package:$packageName")
            )
            startActivity(i)
        } else {
            legacyPermission.launch(
                arrayOf(
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                )
            )
        }
    }

    private fun uriFor(f: File): Uri =
        FileProvider.getUriForFile(this, "$packageName.fileprovider", f)

    private fun openFile(f: File) {
        try {
            val i = Intent(Intent.ACTION_VIEW)
                .setDataAndType(uriFor(f), FileOps.mime(f))
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            startActivity(i)
        } catch (e: Exception) {
            Toast.makeText(this, "No app can open this file", Toast.LENGTH_SHORT).show()
        }
    }

    private fun shareFile(f: File) {
        try {
            val i = Intent(Intent.ACTION_SEND)
                .setType(FileOps.mime(f))
                .putExtra(Intent.EXTRA_STREAM, uriFor(f))
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            startActivity(Intent.createChooser(i, "Share"))
        } catch (e: Exception) {
            Toast.makeText(this, "Couldn't share this file", Toast.LENGTH_SHORT).show()
        }
    }
}
