package com.example.autodocgt

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import com.example.autodocgt.ui.theme.AutoDocGtTheme
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        // Comprobar si hay una sesión activa de Firebase antes de mostrar nada
        val auth = Firebase.auth
        val startScreen = if (auth.currentUser != null) "home" else "login"
        
        setContent {
            AutoDocGtTheme {
                var hasCameraPermission by remember {
                    mutableStateOf(
                        ContextCompat.checkSelfPermission(
                            this,
                            Manifest.permission.CAMERA
                        ) == PackageManager.PERMISSION_GRANTED
                    )
                }

                val permissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestPermission(),
                    onResult = { isGranted ->
                        hasCameraPermission = isGranted
                    }
                )

                var currentScreen by remember { mutableStateOf(startScreen) }

                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    val modifierWithPadding = Modifier.padding(innerPadding)

                    when (currentScreen) {
                        "login" -> {
                            LoginScreen(
                                modifier = modifierWithPadding,
                                onLoginSuccess = { currentScreen = "home" },
                                onNavigateToRegister = { currentScreen = "register" }
                            )
                        }
                        "register" -> {
                            RegisterScreen(
                                modifier = modifierWithPadding,
                                onRegisterSuccess = { currentScreen = "home" },
                                onBackToLogin = { currentScreen = "login" }
                            )
                        }
                        "home" -> {
                            HomeScreen(
                                modifier = modifierWithPadding,
                                onNavigateToSettings = { currentScreen = "settings" }
                            )
                        }
                        "settings" -> {
                            SettingsScreen(
                                modifier = modifierWithPadding,
                                onBack = { currentScreen = "home" },
                                onLogout = { currentScreen = "login" }
                            )
                        }
                    }
                }

                LaunchedEffect(Unit) {
                    if (!hasCameraPermission) {
                        permissionLauncher.launch(Manifest.permission.CAMERA)
                    }
                }
            }
        }
    }
}
