package com.kentaro.guts.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import com.kentaro.guts.ui.theme.GoogleSansCodeFont
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kentaro.guts.viewmodel.LoginEvent
import com.kentaro.guts.viewmodel.LoginState
import com.kentaro.guts.viewmodel.LoginViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    viewModel: LoginViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.state.collectAsState()
    
    LaunchedEffect(state.error) {
        state.error?.let {
            // Auto-clear error after 5 seconds
            kotlinx.coroutines.delay(5000)
            viewModel.onEvent(LoginEvent.ClearError)
        }
    }
    
    LaunchedEffect(state.success) {
        state.success?.let {
            // Auto-clear success after 3 seconds
            kotlinx.coroutines.delay(3000)
            viewModel.onEvent(LoginEvent.ClearSuccess)
        }
    }
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Spacer(modifier = Modifier.height(32.dp))
        
        // Title
        Text(
            text = "vihla",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.primary
        )
        
        Text(
            text = "Enter your credentials to access your account",
            fontSize = 16.sp,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        // Show cached credentials button if available
        if (state.hasCachedCredentials) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "🔐 Cached Credentials Available",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "You have cached login credentials. You can view them or proceed with a new login.",
                        textAlign = TextAlign.Center,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = { viewModel.onEvent(LoginEvent.ShowCachedCredentials) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("View Cached Credentials")
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Username field
        OutlinedTextField(
            value = state.username,
            onValueChange = { input ->
                // If input is exactly 6 characters and doesn't already have @srmist.edu.in, append it
                val processedInput = if (input.length == 6 && !input.contains("@")) {
                    "$input@srmist.edu.in"
                } else {
                    input
                }
                viewModel.onEvent(LoginEvent.UsernameChanged(processedInput))
            },
            label = { Text("Username") },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Text,
                imeAction = ImeAction.Next
            ),
            singleLine = true,
            enabled = !state.isLoading
        )
        
        // Password field
        OutlinedTextField(
            value = state.password,
            onValueChange = { viewModel.onEvent(LoginEvent.PasswordChanged(it)) },
            label = { Text("Password") },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done
            ),
            visualTransformation = PasswordVisualTransformation(),
            singleLine = true,
            enabled = !state.isLoading
        )
        
        // Login Button
        Button(
            onClick = { viewModel.onEvent(LoginEvent.Login) },
            modifier = Modifier.fillMaxWidth(),
            enabled = state.username.isNotBlank() && state.password.isNotBlank() && !state.isLoading
        ) {
            if (state.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(if (state.isAuthenticated) "Login Successful ✓" else "Login")
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Status messages
        state.error?.let { error ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Text(
                    text = error,
                    modifier = Modifier.padding(16.dp),
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    textAlign = TextAlign.Center
                )
            }
        }
        
        state.success?.let { success ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Text(
                    text = success,
                    modifier = Modifier.padding(16.dp),
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    textAlign = TextAlign.Center
                )
            }
        }
        
        // Authentication status
        if (state.isAuthenticated) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF4CAF50).copy(alpha = 0.1f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "🎉 Authentication Successful!",
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF4CAF50)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "You are now logged in to vihla",
                        textAlign = TextAlign.Center,
                        color = Color(0xFF4CAF50)
                    )
                }
            }
        }
        

        
        Spacer(modifier = Modifier.height(32.dp))
    }
} 