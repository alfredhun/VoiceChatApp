package com.voicechat

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.voicechat.data.ConfigRepository
import com.voicechat.ui.screens.ChatScreen
import com.voicechat.ui.screens.ChatViewModel
import com.voicechat.ui.screens.ConfigScreen
import com.voicechat.ui.screens.ConfigViewModel
import com.voicechat.ui.theme.VoiceChatTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            VoiceChatTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    VoiceChatApp()
                }
            }
        }
    }
}

@Composable
fun VoiceChatApp() {
    val navController = rememberNavController()
    
    NavHost(
        navController = navController,
        startDestination = "main"
    ) {
        composable("main") {
            ChatScreen(
                onNavigateToConfig = {
                    navController.navigate("config")
                }
            )
        }
        
        composable("config") {
            ConfigScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}

sealed class Screen(val route: String) {
    object Main : Screen("main")
    object Config : Screen("config")
}
