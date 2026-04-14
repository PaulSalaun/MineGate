package com.saunaltech.mindgate.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import com.saunaltech.mindgate.app.navigation.MindGateNavigation
import com.saunaltech.mindgate.app.service.SyncWorker

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        SyncWorker.schedule(this)
        setContent {
            MaterialTheme {
                MindGateNavigation()
            }
        }
    }
}