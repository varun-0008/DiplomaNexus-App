package com.example.diplomanexus.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.diplomanexus.theme.*
import com.example.diplomanexus.viewmodel.AppViewModel

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: AppViewModel,
    onBack: () -> Unit,
    onOpenCloseFriends: () -> Unit,
    onNavigateToVerify: () -> Unit,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currentUser by viewModel.currentUser.collectAsState()
    val isVerified = currentUser?.is_verified == true

    androidx.activity.compose.BackHandler(onBack = onBack)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings", fontWeight = FontWeight.Bold, color = TextPrimary) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DeepDark)
            )
        },
        containerColor = DeepDark,
        modifier = modifier.fillMaxSize()
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Profile Card Preview
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White.copy(0.02f))
                    .border(1.dp, Color.White.copy(0.08f), RoundedCornerShape(16.dp))
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AvatarView(base64 = currentUser?.profile_pic_base64, name = currentUser?.student_name ?: currentUser?.username ?: "U", size = 48.dp)
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(currentUser?.student_name ?: currentUser?.username ?: "User", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Text("@${currentUser?.username ?: "username"}", color = TextSecondary, fontSize = 12.sp)
                }
            }

            Text("ACCOUNT & SECURITY", color = TextSecondary, fontWeight = FontWeight.Bold, fontSize = 10.sp, letterSpacing = 1.sp)

            // Account Verification Row
            SettingsRow(
                icon = Icons.Default.VerifiedUser,
                iconColor = if (isVerified) Color(0xFF4CAF50) else BrandOrange,
                title = "Verification Status",
                description = if (isVerified) "Verified Student Badge Active" else "Get verified to access academic features",
                onClick = { if (!isVerified) onNavigateToVerify() }
            )

            Text("CUSTOMIZATION", color = TextSecondary, fontWeight = FontWeight.Bold, fontSize = 10.sp, letterSpacing = 1.sp)

            // Close Friends Row
            SettingsRow(
                icon = Icons.Default.Star,
                iconColor = Color(0xFF4CAF50),
                title = "Close Friends List",
                description = "Choose who gets to see your exclusive stories",
                onClick = onOpenCloseFriends
            )

            Text("DEVELOPER & COMMUNITY", color = TextSecondary, fontWeight = FontWeight.Bold, fontSize = 10.sp, letterSpacing = 1.sp)

            // Developer Attribution Card
            com.example.diplomanexus.ui.components.DeveloperCreditCard()

            Spacer(modifier = Modifier.height(8.dp))

            // Logout Button
            Button(
                onClick = { viewModel.logout(onLogout) },
                colors = ButtonDefaults.buttonColors(containerColor = Color.Red.copy(0.1f)),
                border = BorderStroke(1.dp, Color.Red.copy(0.3f)),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.ExitToApp, contentDescription = null, tint = Color.Red)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Logout Session", color = Color.Red, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun SettingsRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconColor: Color,
    title: String,
    description: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(0.03f))
            .border(1.dp, Color.White.copy(0.06f), RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(iconColor.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(20.dp))
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            Text(description, color = TextSecondary, fontSize = 11.sp)
        }
        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = TextTertiary, modifier = Modifier.size(18.dp))
    }
}
