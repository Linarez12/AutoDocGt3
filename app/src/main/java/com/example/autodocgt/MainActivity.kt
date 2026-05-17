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
                                onNavigateToSettings = { currentScreen = "settings" },
                                onNavigateToMaintenance = { currentScreen = "maintenance" },
                                onNavigateToDocuments = { currentScreen = "documents" },
                                onNavigateToReminders = { currentScreen = "reminders" },
                                onNavigateToExpenses = { currentScreen = "expenses" }
                            )
                        }
                        "settings" -> {
                            SettingsScreen(
                                modifier = modifierWithPadding,
                                onBack = { currentScreen = "home" },
                                onLogout = { currentScreen = "login" }
                            )
                        }
                        "maintenance" -> {
                            MaintenanceScreen(
                                modifier = modifierWithPadding,
                                onBack = { currentScreen = "home" },
                                onHomeClick = { currentScreen = "home" },
                                onDocumentsClick = { currentScreen = "documents" },
                                onRemindersClick = { currentScreen = "reminders" },
                                onExpensesClick = { currentScreen = "expenses" }
                            )
                        }
                        "reminders" -> {
                            RemindersScreen(
                                modifier = modifierWithPadding,
                                onBack = { currentScreen = "home" },
                                onHomeClick = { currentScreen = "home" },
                                onMaintenanceClick = { currentScreen = "maintenance" },
                                onDocumentsClick = { currentScreen = "documents" },
                                onExpensesClick = { currentScreen = "expenses" }
                            )
                        }
                        "documents" -> {
                            DocumentsScreen(
                                modifier = modifierWithPadding,
                                onBack = { currentScreen = "home" },
                                onHomeClick = { currentScreen = "home" },
                                onMaintenanceClick = { currentScreen = "maintenance" },
                                onRemindersClick = { currentScreen = "reminders" },
                                onExpensesClick = { currentScreen = "expenses" },
                                onNavigateToAddDocument = { currentScreen = "add_document" }
                            )
                        }
                        "expenses" -> {
                            ExpensesScreen(
                                modifier = modifierWithPadding,
                                onBack = { currentScreen = "home" },
                                onHomeClick = { currentScreen = "home" },
                                onMaintenanceClick = { currentScreen = "maintenance" },
                                onDocumentsClick = { currentScreen = "documents" },
                                onRemindersClick = { currentScreen = "reminders" }
                            )
                        }
                        "add_document" -> {
                            AddDocumentScreen(
                                modifier = modifierWithPadding,
                                onBack = { currentScreen = "documents" }
                            )
                        }
                    }
                }

            }
        }
    }
}
