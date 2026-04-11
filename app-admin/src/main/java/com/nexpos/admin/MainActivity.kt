package com.nexpos.admin

import android.app.AlertDialog
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.nexpos.admin.ui.navigation.AdminNavGraph
import com.nexpos.core.ui.theme.NexPosTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        NexPosAdminApp.getLastCrash(application as NexPosAdminApp)?.let { crash ->
            AlertDialog.Builder(this)
                .setTitle("Detail Error (kirim ke developer)")
                .setMessage(crash)
                .setPositiveButton("OK", null)
                .show()
        }

        setContent {
            NexPosTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AdminNavGraph()
                }
            }
        }
    }
}
