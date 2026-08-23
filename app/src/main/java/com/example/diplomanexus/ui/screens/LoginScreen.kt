package com.example.diplomanexus.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.diplomanexus.theme.*
import com.example.diplomanexus.viewmodel.AppViewModel

@Composable
fun LoginScreen(
    viewModel: AppViewModel,
    onSuccess: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isRegisterMode by remember { mutableStateOf(false) }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var rememberMe by remember { mutableStateOf(true) }
    val isUsernameValidPin = remember(username) { isValidRollNumber(username) }

    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    // Clear errors when toggling modes
    LaunchedEffect(isRegisterMode) {
        viewModel.clearError()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0F0F12))
            .drawBehind {
                // Top-Left glow (Fuchsia / Fuchsia Pink)
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0xFFFF00FF).copy(alpha = 0.08f), Color.Transparent),
                        radius = 350.dp.toPx()
                    ),
                    radius = 350.dp.toPx(),
                    center = androidx.compose.ui.geometry.Offset(-50.dp.toPx(), -50.dp.toPx())
                )
                // Bottom-Right glow (Light Blue)
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0xFF00B2FF).copy(alpha = 0.08f), Color.Transparent),
                        radius = 450.dp.toPx()
                    ),
                    radius = 450.dp.toPx(),
                    center = androidx.compose.ui.geometry.Offset(size.width + 50.dp.toPx(), size.height + 50.dp.toPx())
                )
                // Mid-Right glow (Brand Orange)
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(BrandOrange.copy(alpha = 0.04f), Color.Transparent),
                        radius = 250.dp.toPx()
                    ),
                    radius = 250.dp.toPx(),
                    center = androidx.compose.ui.geometry.Offset(size.width * 0.8f, size.height * 0.3f)
                )
            }
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Logo Icon in circular glass container
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.06f))
                        .border(1.dp, Color.White.copy(alpha = 0.1f), CircleShape)
                        .padding(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Hub,
                        contentDescription = "Logo",
                        tint = BrandOrange,
                        modifier = Modifier.size(32.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Title
                Text(
                    text = "DiplomaNexus",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 32.sp,
                    color = BrandOrange,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Subtitle
                Text(
                    text = "Secure Student Portal",
                    color = TextSecondary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center
                )
            }

            // Main Form Card (Glass Panel)
            Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0x7318181F)),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Student ID Input
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = if (isRegisterMode) "STUDENT ID (ROLL NUMBER)" else "STUDENT ID",
                                color = TextSecondary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                letterSpacing = 1.sp
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            OutlinedTextField(
                                value = username,
                                onValueChange = { username = it },
                                placeholder = { Text(if (isRegisterMode) "e.g. 24054-cps-063" else "e.g. S12345678", color = TextTertiary) },
                                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = TextTertiary) },
                                supportingText = {
                                    if (isRegisterMode && username.isNotBlank()) {
                                        if (isUsernameValidPin) {
                                            Text("Valid Roll Number format ✓", color = AlertGreen)
                                        } else {
                                            Text("Format: YYCCC-BBB-NNN (e.g. 24054-cps-063)", color = MaterialTheme.colorScheme.error)
                                        }
                                    }
                                },
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextPrimary,
                                    focusedContainerColor = Color(0xBF18181F),
                                    unfocusedContainerColor = Color(0xBF18181F),
                                    focusedBorderColor = BrandOrange,
                                    unfocusedBorderColor = Color.White.copy(alpha = 0.1f),
                                    cursorColor = BrandOrange
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        // Password Input
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "PASSWORD",
                                    color = TextSecondary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    letterSpacing = 1.sp
                                )
                                Text(
                                    text = "Forgot Password?",
                                    color = Color(0xFF8BCEFF),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.clickable { /* Forgot Password action */ }
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            OutlinedTextField(
                                value = password,
                                onValueChange = { password = it },
                                placeholder = { Text("••••••••", color = TextTertiary) },
                                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = TextTertiary) },
                                trailingIcon = {
                                    val icon = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility
                                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                        Icon(icon, contentDescription = "Toggle password visibility", tint = TextTertiary)
                                    }
                                },
                                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextPrimary,
                                    focusedContainerColor = Color(0xBF18181F),
                                    unfocusedContainerColor = Color(0xBF18181F),
                                    focusedBorderColor = BrandOrange,
                                    unfocusedBorderColor = Color.White.copy(alpha = 0.1f),
                                    cursorColor = BrandOrange
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        // Keep Me Logged In checkbox
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = rememberMe,
                                onCheckedChange = { rememberMe = it },
                                colors = CheckboxDefaults.colors(
                                    checkedColor = BrandOrange,
                                    uncheckedColor = TextSecondary,
                                    checkmarkColor = Color.White
                                )
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Keep me securely logged in",
                                color = TextSecondary,
                                fontSize = 13.sp
                            )
                        }

                        // Error Message
                        if (errorMessage != null) {
                            Text(
                                text = errorMessage!!,
                                color = MaterialTheme.colorScheme.error,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        // Submit Button
                        Button(
                            onClick = {
                                if (username.isNotBlank() && password.isNotBlank()) {
                                    if (isRegisterMode) {
                                        viewModel.register(username.trim(), password, onSuccess)
                                    } else {
                                        viewModel.login(username.trim(), password, onSuccess)
                                    }
                                }
                            },
                            enabled = !isLoading && username.isNotBlank() && password.isNotBlank() && (!isRegisterMode || isUsernameValidPin),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            shape = RoundedCornerShape(24.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = BrandOrange,
                                disabledContainerColor = BrandOrange.copy(alpha = 0.5f)
                            )
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                            } else {
                                Text(
                                    text = if (isRegisterMode) "Register" else "Sign In",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                            }
                        }

                        // Toggle Mode Link
                        Row(
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = if (isRegisterMode) "Already have an account? " else "New to DiplomaNexus? ",
                                color = TextSecondary,
                                fontSize = 13.sp
                            )
                            Text(
                                text = if (isRegisterMode) "Login" else "Create an Account",
                                color = BrandOrange,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                modifier = Modifier.clickable {
                                    isRegisterMode = !isRegisterMode
                                }
                            )
                        }
                    }
                }

            // Footer Section
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 32.dp, bottom = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "© 2026 DiplomaNexus. Secure Student Portal.",
                    color = TextTertiary,
                    fontSize = 11.sp,
                    textAlign = TextAlign.Center
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Privacy Policy", color = TextTertiary, fontSize = 11.sp, modifier = Modifier.clickable {})
                    Text("Terms of Service", color = TextTertiary, fontSize = 11.sp, modifier = Modifier.clickable {})
                    Text("Help Center", color = TextTertiary, fontSize = 11.sp, modifier = Modifier.clickable {})
                }
            }
        }
    }
}
