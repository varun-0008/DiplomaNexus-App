package com.example.diplomanexus.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.diplomanexus.R
import com.example.diplomanexus.theme.*

@Composable
fun WelcomeScreen(
    onContinueToApp: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uriHandler = LocalUriHandler.current

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DeepDark),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Brand Header Badge
            Box(
                modifier = Modifier
                    .size(90.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            colors = listOf(ElectricBlue.copy(alpha = 0.3f), NeonPurple.copy(alpha = 0.3f))
                        )
                    )
                    .border(2.dp, Brush.linearGradient(listOf(ElectricBlue, NeonPurple)), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.app_logo),
                    contentDescription = "DiplomaNexus Identity Logo",
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Welcome to DiplomaNexus!",
                color = TextPrimary,
                fontSize = 26.sp,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(6.dp))

            Surface(
                shape = RoundedCornerShape(20.dp),
                color = BrandOrange.copy(alpha = 0.15f),
                border = androidx.compose.foundation.BorderStroke(1.dp, BrandOrange.copy(alpha = 0.3f))
            ) {
                Text(
                    text = "THE UNCENSORED DIPLOMA STUDENT NETWORK",
                    color = BrandOrange,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Core Rules & Features List
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                // 1. Bunks & Loopholes
                WelcomeFeatureCard(
                    icon = Icons.Default.AutoAwesome,
                    badgeColor = RatingGold,
                    title = "Post Bunks & Campus Loopholes Freely",
                    description = "It is the only social media application where diploma students can freely post about their bunks and loopholes they can find."
                )

                // 2. No Staff or Faculty
                WelcomeFeatureCard(
                    icon = Icons.Default.VisibilityOff,
                    badgeColor = AccentPink,
                    title = "Strictly Zero Staff & Faculty Access",
                    description = "No staff or faculty can access this application or your posts. Your identity and activity remain strictly private among students."
                )

                // 3. Decentralized & Untraceable
                WelcomeFeatureCard(
                    icon = Icons.Default.Shield,
                    badgeColor = VerifiedBlue,
                    title = "Decentralized & Untraceable Network",
                    description = "It is a decentralized application where no government or agency can track you and your accounts."
                )

                // 4. Zero Backlash
                WelcomeFeatureCard(
                    icon = Icons.Default.Security,
                    badgeColor = ElectricBlue,
                    title = "Zero Backlash Guarantee",
                    description = "Here no one faces backlash on their works. Share thoughts, meme culture, and projects without fear."
                )

                // 5. Academic Planning
                WelcomeFeatureCard(
                    icon = Icons.Default.School,
                    badgeColor = ElectricBlue,
                    title = "Full Academics & Life Planning",
                    description = "Here you can also check your academics fully and plan your life with direct SBTET reports and SGPA insights."
                )

                // 6. Insider Info (MOST IMPORTANT)
                WelcomeFeatureCard(
                    icon = Icons.Default.Warning,
                    badgeColor = BrandOrange,
                    title = "Fastest Insider Info (MOST IMPORTANT)",
                    description = "This is the fastest application to get insider information that faculty lie about. Stay ahead of college notices.",
                    isHighlight = true
                )

                // 7. Marketplace & Freelancing
                WelcomeFeatureCard(
                    icon = Icons.Default.Storefront,
                    badgeColor = RatingGold,
                    title = "Campus Marketplace & Freelancing",
                    description = "This application also has a marketplace and freelance environment where students can earn from their polytechnic campus."
                )

                // 8. Encryption
                WelcomeFeatureCard(
                    icon = Icons.Default.Lock,
                    badgeColor = NeonPurple,
                    title = "100% Encrypted & Secure",
                    description = "Everything is encrypted! Your messages, posts, and user data are protected end-to-end."
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Telegram Banner Call-To-Action
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .clickable { uriHandler.openUri("https://t.me/DiplomaNexus") },
                color = VerifiedBlue.copy(alpha = 0.15f),
                border = androidx.compose.foundation.BorderStroke(1.dp, VerifiedBlue.copy(alpha = 0.4f))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(VerifiedBlue.copy(alpha = 0.25f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Send,
                            contentDescription = "Telegram",
                            tint = VerifiedBlue,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "JOIN OUR TELEGRAM CHANNEL",
                            color = VerifiedBlue,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "For further updates visit t.me/DiplomaNexus",
                            color = TextPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Text(
                        text = "Join ↗",
                        color = VerifiedBlue,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Enter App Primary Button
            Button(
                onClick = onContinueToApp,
                colors = ButtonDefaults.buttonColors(containerColor = BrandOrange),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Enter DiplomaNexus",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = Icons.Default.ArrowForward,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun WelcomeFeatureCard(
    icon: ImageVector,
    badgeColor: Color,
    title: String,
    description: String,
    isHighlight: Boolean = false
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = if (isHighlight) BrandOrange.copy(alpha = 0.12f) else CardDark.copy(alpha = 0.7f),
        border = androidx.compose.foundation.BorderStroke(
            width = if (isHighlight) 1.5.dp else 1.dp,
            color = if (isHighlight) BrandOrange.copy(alpha = 0.5f) else Color.White.copy(alpha = 0.08f)
        )
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(badgeColor.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = badgeColor,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = if (isHighlight) BrandOrange else TextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = description,
                    color = TextSecondary,
                    fontSize = 12.sp,
                    lineHeight = 17.sp,
                    fontWeight = FontWeight.Normal
                )
            }
        }
    }
}
