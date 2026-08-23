package com.example.diplomanexus.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.diplomanexus.theme.*

@Composable
fun DeveloperCreditCard(
    modifier: Modifier = Modifier
) {
    val uriHandler = LocalUriHandler.current

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    colors = listOf(
                        ElectricBlue.copy(alpha = 0.5f),
                        NeonPurple.copy(alpha = 0.3f),
                        BrandOrange.copy(alpha = 0.2f)
                    )
                ),
                shape = RoundedCornerShape(24.dp)
            ),
        color = CardDark.copy(alpha = 0.85f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Community Statement Header Banner
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = ElectricBlue.copy(alpha = 0.12f),
                border = androidx.compose.foundation.BorderStroke(1.dp, ElectricBlue.copy(alpha = 0.25f))
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = null,
                        tint = VerifiedBlue,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "DIPLOMA STUDENTS COMMUNITY",
                            color = VerifiedBlue,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "A verified, safe & open space for diploma students to ask doubts, share resources, and express ideas freely without facing internet or faculty backlash.",
                            color = TextPrimary,
                            fontSize = 12.sp,
                            lineHeight = 17.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Creator Header Badge
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.05f))
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Favorite,
                    contentDescription = null,
                    tint = AccentPink,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Created by ",
                    color = TextSecondary,
                    fontSize = 13.sp
                )
                Text(
                    text = "Kompellivarun",
                    color = BrandOrange,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Icon(
                    imageVector = Icons.Default.Verified,
                    contentDescription = "Verified Creator",
                    tint = VerifiedBlue,
                    modifier = Modifier
                        .padding(start = 4.dp)
                        .size(16.dp)
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Founder of Codeminer Community",
                color = RatingGold,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Interactive Social Links Flow Row
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Codeminer Website Link
                SocialChip(
                    icon = Icons.Default.Language,
                    label = "Codeminer Community",
                    detail = "codeminer.firebaseapp.com",
                    badgeColor = RatingGold,
                    onClick = { uriHandler.openUri("https://codeminer.firebaseapp.com") }
                )

                // GitHub Link
                SocialChip(
                    icon = Icons.Default.Code,
                    label = "GitHub",
                    detail = "github.com/varun-0008",
                    badgeColor = ElectricBlue,
                    onClick = { uriHandler.openUri("https://github.com/varun-0008") }
                )

                // LinkedIn Link
                SocialChip(
                    icon = Icons.Default.Public,
                    label = "LinkedIn",
                    detail = "linkedin.com/in/kompelli-varun",
                    badgeColor = Color(0xFF0A66C2),
                    onClick = { uriHandler.openUri("https://www.linkedin.com/in/kompelli-varun") }
                )

                // Instagram Link
                SocialChip(
                    icon = Icons.Default.Person,
                    label = "Instagram",
                    detail = "instagram.com/kompellivarun8",
                    badgeColor = AccentPink,
                    onClick = { uriHandler.openUri("https://www.instagram.com/kompellivarun8/") }
                )
            }
        }
    }
}

@Composable
private fun SocialChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    detail: String,
    badgeColor: Color,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick),
        color = Color.White.copy(alpha = 0.04f),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(badgeColor.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = badgeColor,
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    color = TextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = detail,
                    color = TextSecondary,
                    fontSize = 11.sp
                )
            }

            Text(
                text = "Open ↗",
                color = badgeColor,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun DeveloperCreditBanner(
    modifier: Modifier = Modifier
) {
    val uriHandler = LocalUriHandler.current

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { uriHandler.openUri("https://codeminer.firebaseapp.com") },
        color = ElectricBlue.copy(alpha = 0.1f),
        border = androidx.compose.foundation.BorderStroke(1.dp, ElectricBlue.copy(alpha = 0.25f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Verified,
                    contentDescription = null,
                    tint = VerifiedBlue,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Diploma Community • Created by ",
                    color = TextSecondary,
                    fontSize = 11.sp
                )
                Text(
                    text = "Kompellivarun",
                    color = BrandOrange,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Text(
                text = "Codeminer ↗",
                color = ElectricBlue,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
