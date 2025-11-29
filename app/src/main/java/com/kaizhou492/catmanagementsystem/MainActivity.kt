package com.kaizhou492.catmanagementsystem

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import com.kaizhou492.catmanagementsystem.data.CatDataManager
import com.kaizhou492.catmanagementsystem.ui.CatManagementApp
import com.kaizhou492.catmanagementsystem.ui.theme.CatManagementSystemTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private lateinit var dataManager: CatDataManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        dataManager = CatDataManager(applicationContext)

        // 启动时更新猫咪状态
        lifecycleScope.launch {
            dataManager.updateCatStates()
        }

        setContent {
            CatManagementSystemTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    CatManagementApp(dataManager)
                }
            }
        }
    }
}