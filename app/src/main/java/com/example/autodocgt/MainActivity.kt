package com.example.autodocgt

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.BackHandler
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
        
        val auth = Firebase.auth
        val startScreen = if (auth.currentUser != null) "home" else "login"
        
        setContent {
            AutoDocGtTheme {
                var currentScreen by remember { mutableStateOf(startScreen) }
                var selectedVehicleForDetails by remember { mutableStateOf<Map<String, Any>?>(null) }

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
                            BackHandler { currentScreen = "login" }
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
                                onNavigateToExpenses = { currentScreen = "expenses" },
                                onNavigateToAddVehicle = { currentScreen = "add_vehicle" },
                                onNavigateToVehicleDetails = { vehicle ->
                                    selectedVehicleForDetails = vehicle
                                    currentScreen = "vehicle_details"
                                }
                            )
                        }
                        "settings" -> {
                            BackHandler { currentScreen = "home" }
                            SettingsScreen(
                                modifier = modifierWithPadding,
                                onBack = { currentScreen = "home" },
                                onLogout = { currentScreen = "login" }
                            )
                        }
                        "maintenance" -> {
                            BackHandler { currentScreen = "home" }
                            MaintenanceScreen(
                                modifier = modifierWithPadding,
                                onBack = { currentScreen = "home" },
                                onHomeClick = { currentScreen = "home" },
                                onDocumentsClick = { currentScreen = "documents" },
                                onRemindersClick = { currentScreen = "reminders" },
                                onExpensesClick = { currentScreen = "expenses" },
                                onNearbyWorkshopsClick = { currentScreen = "nearby_workshops" }
                            )
                        }
                        "reminders" -> {
                            BackHandler { currentScreen = "home" }
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
                            BackHandler { currentScreen = "home" }
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
                            BackHandler { currentScreen = "home" }
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
                            BackHandler { currentScreen = "documents" }
                            AddDocumentScreen(
                                modifier = modifierWithPadding,
                                onBack = { currentScreen = "documents" }
                            )
                        }
                        "add_vehicle" -> {
                            BackHandler { currentScreen = "home" }
                            AddVehicleScreen(
                                modifier = modifierWithPadding,
                                onBack = { currentScreen = "home" }
                            )
                        }
                        "vehicle_details" -> {
                            BackHandler { currentScreen = "home" }
                            if (selectedVehicleForDetails != null) {
                                VehicleDetailsScreen(
                                    vehicle = selectedVehicleForDetails!!,
                                    modifier = modifierWithPadding,
                                    onBack = { currentScreen = "home" },
                                    onEdit = { vehicle ->
                                        selectedVehicleForDetails = vehicle
                                        currentScreen = "edit_vehicle"
                                    }
                                )
                            } else {
                                currentScreen = "home"
                            }
                        }
                        "edit_vehicle" -> {
                            BackHandler { currentScreen = "vehicle_details" }
                            if (selectedVehicleForDetails != null) {
                                EditVehicleScreen(
                                    vehicle = selectedVehicleForDetails!!,
                                    modifier = modifierWithPadding,
                                    onBack = { currentScreen = "home" }
                                )
                            } else {
                                currentScreen = "home"
                            }
                        }
                        "nearby_workshops" -> {
                            BackHandler { currentScreen = "maintenance" }
                            NearbyWorkshopsScreen(
                                onBack = { currentScreen = "maintenance" },
                                modifier = modifierWithPadding
                            )
                        }
                    }
                }

            }
        }
    }
}
