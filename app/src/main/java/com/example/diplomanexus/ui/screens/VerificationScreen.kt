package com.example.diplomanexus.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.diplomanexus.theme.*
import com.example.diplomanexus.viewmodel.AppViewModel

private val ROLL_NUMBER_REGEX = Regex("""^\d{2}\d{3}-[a-zA-Z]{1,6}-\d{3}$""")
fun isValidRollNumber(input: String): Boolean = ROLL_NUMBER_REGEX.matches(input.trim())

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VerificationScreen(
    viewModel: AppViewModel,
    onVerified: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val currentUser by viewModel.currentUser.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    val username = currentUser?.username ?: ""
    var pin by remember { mutableStateOf(username) }
    var phone by remember { mutableStateOf(currentUser?.mobile_number ?: "") }
    var otp by remember { mutableStateOf("") }
    
    var otpSent by remember { mutableStateOf(false) }
    var otpSentMessage by remember { mutableStateOf<String?>(null) }
    
    var pinError by remember { mutableStateOf<String?>(null) }
    var phoneError by remember { mutableStateOf<String?>(null) }
    var otpError by remember { mutableStateOf<String?>(null) }

    val scrollState = rememberScrollState()

    // Validate inputs
    LaunchedEffect(pin) {
        pinError = if (pin.isNotBlank() && !isValidRollNumber(pin)) {
            "Format: YYCCC-BBB-NNN (e.g. 24054-cps-063)"
        } else if (pin.isNotBlank() && pin.trim().lowercase() != username.trim().lowercase()) {
            "Must match account username: $username"
        } else null
    }

    LaunchedEffect(phone) {
        phoneError = if (phone.isNotBlank() && phone.trim().length < 10) {
            "Enter a valid 10-digit mobile number"
        } else null
    }

    LaunchedEffect(otp) {
        otpError = if (otp.isNotBlank() && otp.trim().length != 6) {
            "OTP must be 6 characters"
        } else null
    }

    // Reset error when clearing
    LaunchedEffect(errorMessage) {
        if (errorMessage != null) {
            Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show()
            viewModel.clearError()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "SBTET Verification",
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = BrandOrange)
                    }
                },
                windowInsets = WindowInsets(0.dp),
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DeepDark)
            )
        },
        modifier = modifier.fillMaxSize()
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(DeepDark)
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Header Icon
                Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = null,
                    tint = BrandOrange,
                    modifier = Modifier.size(64.dp)
                )

                Text(
                    text = "Verify Your Account",
                    color = TextPrimary,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = "Connect your account with the official Telangana SBTET Portal to fetch your consolidated marks memo, attendance, and dynamic academic analysis.",
                    color = TextSecondary,
                    fontSize = 13.sp,
                    lineHeight = 19.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )

                // Info Banner Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = CardLightDark)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = BrandOrange,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "To verify your student profile, enter your details to receive a temporary OTP code on your phone.",
                            color = TextSecondary,
                            fontSize = 12.sp,
                            lineHeight = 17.sp
                        )
                    }
                }

                // ─── Phase 1: PIN & Phone Fields ───
                OutlinedTextField(
                    value = pin,
                    onValueChange = { pin = it.trim() },
                    label = { Text("Roll Number (PIN)") },
                    placeholder = { Text("e.g. 24054-cps-063") },
                    readOnly = true, // Force verification of signed-up PIN
                    leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = TextTertiary) },
                    supportingText = {
                        if (pinError != null) {
                            Text(pinError!!, color = MaterialTheme.colorScheme.error)
                        } else {
                            Text("Your verified PIN must match your username.", color = TextTertiary)
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = BrandOrange,
                        unfocusedBorderColor = BorderColor,
                        disabledBorderColor = BorderColor
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !otpSent
                )

                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it.trim() },
                    label = { Text("Mobile Number") },
                    placeholder = { Text("10-digit registered mobile") },
                    leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null, tint = TextTertiary) },
                    supportingText = {
                        if (phoneError != null) {
                            Text(phoneError!!, color = MaterialTheme.colorScheme.error)
                        } else {
                            Text("Mobile number registered on the SBTET portal.", color = TextTertiary)
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = BrandOrange,
                        unfocusedBorderColor = BorderColor
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !otpSent
                )

                // OTP Trigger Button
                AnimatedVisibility(
                    visible = !otpSent,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Button(
                        onClick = {
                            if (pin.isNotBlank() && phone.isNotBlank() && pinError == null && phoneError == null) {
                                viewModel.generateSbtetOtp(pin, phone) { msg ->
                                    otpSent = true
                                    otpSentMessage = msg
                                    Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                                }
                            }
                        },
                        enabled = !isLoading && pin.isNotBlank() && phone.isNotBlank() && pinError == null && phoneError == null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = BrandOrange)
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                        } else {
                            Text("Send Verification OTP", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        }
                    }
                }

                // ─── Phase 2: OTP Input Area ───
                AnimatedVisibility(
                    visible = otpSent,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Success Sent Card
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = CardLightDark)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(
                                    text = otpSentMessage ?: "OTP sent successfully!",
                                    color = AlertGreen,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Enter the 6-digit verification code below to verify ownership.",
                                    color = TextSecondary,
                                    fontSize = 12.sp
                                )
                            }
                        }

                        OutlinedTextField(
                            value = otp,
                            onValueChange = { otp = it.trim().uppercase() },
                            label = { Text("Verification OTP") },
                            placeholder = { Text("6-character code") },
                            leadingIcon = { Icon(Icons.Default.Smartphone, contentDescription = null, tint = TextTertiary) },
                            supportingText = {
                                if (otpError != null) {
                                    Text(otpError!!, color = MaterialTheme.colorScheme.error)
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary,
                                focusedBorderColor = BrandOrange,
                                unfocusedBorderColor = BorderColor
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Verification Trigger
                        Button(
                            onClick = {
                                if (otp.isNotBlank() && otpError == null) {
                                    viewModel.verifySbtetOtp(pin, phone, otp) {
                                        Toast.makeText(context, "Verification Successful!", Toast.LENGTH_SHORT).show()
                                        onVerified()
                                    }
                                }
                            },
                            enabled = !isLoading && otp.isNotBlank() && otpError == null,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = AlertGreen)
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                            } else {
                                Text("Verify OTP & Complete", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            }
                        }

                        // Reset verification option
                        TextButton(
                            onClick = {
                                otpSent = false
                                otp = ""
                                otpSentMessage = null
                            },
                            enabled = !isLoading
                        ) {
                            Text("Change Phone Number or Resend", color = TextSecondary)
                        }
                    }
                }
            }
        }
    }
}
