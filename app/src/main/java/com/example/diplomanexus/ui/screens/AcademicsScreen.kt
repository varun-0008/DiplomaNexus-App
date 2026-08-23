package com.example.diplomanexus.ui.screens

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.diplomanexus.api.SemesterInfoDto
import com.example.diplomanexus.theme.*
import com.example.diplomanexus.viewmodel.AppViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AcademicsScreen(
    viewModel: AppViewModel,
    onNavigateToVerify: () -> Unit,
    onOpenNotifications: () -> Unit,
    onOpenMarketplace: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currentUser by viewModel.currentUser.collectAsState()
    val academicInfo by viewModel.academicInfo.collectAsState()

    val isVerified = currentUser?.is_verified ?: false

    LaunchedEffect(isVerified) {
        if (isVerified) {
            viewModel.fetchAcademicInfo()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Academic Dashboard",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 20.sp,
                        color = TextPrimary
                    )
                },
                windowInsets = WindowInsets(0.dp),
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DeepDark),
                actions = {
                    IconButton(onClick = onOpenMarketplace) {
                        Icon(Icons.Default.Storefront, contentDescription = "Marketplace", tint = TextPrimary)
                    }
                    val notifications by viewModel.notifications.collectAsState()
                    val unreadCount = remember(notifications) { notifications.count { !it.isRead } }
                    IconButton(onClick = onOpenNotifications) {
                        Box(contentAlignment = Alignment.TopEnd) {
                            Icon(Icons.Default.Notifications, contentDescription = "Notifications", tint = TextPrimary)
                            if (unreadCount > 0) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(BrandOrange)
                                )
                            }
                        }
                    }
                    if (isVerified) {
                        IconButton(onClick = { viewModel.fetchAcademicInfo() }) {
                            Icon(Icons.Default.Refresh, contentDescription = "Refresh Academics", tint = ElectricBlue)
                        }
                    }
                }
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
            if (!isVerified) {
                AcademicLockState(currentUser, onNavigateToVerify)
            } else {
                val info = academicInfo
                if (info == null) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = ElectricBlue)
                    }
                } else {
                    val semesters = info.semesters
                    val totalSems = semesters.size
                    
                    val latestSemWithCgpa = semesters.filter { it.cgpa > 0 }.maxByOrNull { it.semester_number }
                    val cgpa = latestSemWithCgpa?.cgpa ?: (if (totalSems > 0) semesters.map { it.sgpa }.average() else 0.0)
                    val totalBacklogs = semesters.sumOf { it.backlogs }

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        contentPadding = PaddingValues(bottom = 80.dp)
                    ) {
                        // Cumulative metrics row
                        item {
                            Spacer(modifier = Modifier.height(4.dp))
                            val att = info.attendance_summary
                            val percentage = att?.percentage ?: 85.0
                            val workingDays = att?.workingDays ?: 120
                            val presentDays = att?.presentDays ?: 102.0
                            val requiredDays = (workingDays * 0.75).toInt()
                            val readinessPercentage = if (requiredDays > 0) (presentDays / requiredDays * 100).toInt() else 0
                            val isEligible = percentage >= 75.0
                            
                            Column(
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                // Current Progress Card
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(20.dp),
                                    colors = CardDefaults.cardColors(containerColor = CardDark),
                                    border = BorderStroke(0.5.dp, BorderColor.copy(alpha = 0.4f))
                                ) {
                                    Column(modifier = Modifier.padding(18.dp)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.TrendingUp,
                                                contentDescription = null,
                                                tint = ElectricBlue,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = "Current Progress",
                                                color = TextSecondary,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(10.dp))
                                        Row(verticalAlignment = Alignment.Bottom) {
                                            Text(
                                                text = "${presentDays.toInt()}",
                                                color = TextPrimary,
                                                fontSize = 32.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = " / ${workingDays} Days",
                                                color = TextSecondary,
                                                fontSize = 14.sp,
                                                modifier = Modifier.padding(bottom = 4.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(8.dp))
                                        LinearProgressIndicator(
                                            progress = { (presentDays.toFloat() / workingDays).coerceIn(0f, 1f) },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(6.dp)
                                                .clip(CircleShape),
                                            color = ElectricBlue,
                                            trackColor = BorderColor.copy(alpha = 0.2f)
                                        )
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = "${percentage.toInt()}% Attended • On Track",
                                            color = ElectricBlue,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                // Exam Readiness Card
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(20.dp),
                                    colors = CardDefaults.cardColors(containerColor = CardDark),
                                    border = BorderStroke(0.5.dp, BorderColor.copy(alpha = 0.4f))
                                ) {
                                    Column(modifier = Modifier.padding(18.dp)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.CheckCircle,
                                                contentDescription = null,
                                                tint = BrandOrange,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = "Exam Readiness",
                                                color = TextSecondary,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(10.dp))
                                        Row(verticalAlignment = Alignment.Bottom) {
                                            Text(
                                                text = "${presentDays.toInt()}",
                                                color = TextPrimary,
                                                fontSize = 32.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = " / ${requiredDays} Required",
                                                color = TextSecondary,
                                                fontSize = 14.sp,
                                                modifier = Modifier.padding(bottom = 4.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(8.dp))
                                        LinearProgressIndicator(
                                            progress = { (presentDays.toFloat() / requiredDays).coerceIn(0f, 1f) },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(6.dp)
                                                .clip(CircleShape),
                                            color = BrandOrange,
                                            trackColor = BorderColor.copy(alpha = 0.2f)
                                        )
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = "${readinessPercentage}% of Requirement • ${if (isEligible) "Eligible" else "Shortage"}",
                                            color = if (isEligible) AlertGreen else AccentPink,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }

                        // Academic Analysis Chart Card
                        if (totalSems > 1) {
                            item {
                                SgpaTrendChart(semesters = semesters)
                            }
                        }

                        // Attendance Calendar Card (if logs are available)
                        val logs = info.attendance_logs
                        if (!logs.isNullOrEmpty()) {
                            item {
                                AttendanceCalendar(logs = logs)
                            }
                        }

                        // Semester Breakdowns title
                        item {
                            Text(
                                text = "Semester Breakdown",
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }

                        // Semester-wise records list
                        items(semesters) { semester ->
                            SemesterRecordCard(semester = semester)
                        }

                        // Credentials footer card
                        item {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = CardDark),
                                border = CardDefaults.outlinedCardBorder().copy(
                                    brush = Brush.horizontalGradient(colors = listOf(BorderColor, BorderColor))
                                )
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Text("Student Credentials", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    HorizontalDivider(color = BorderColor)
                                    InfoFieldRow(label = "Student Name", value = info.student_name)
                                    InfoFieldRow(label = "Board PIN", value = info.pin)
                                    InfoFieldRow(label = "Branch", value = info.branch)
                                    InfoFieldRow(label = "College", value = info.college_name)
                                    InfoFieldRow(label = "Mobile Number", value = info.mobile_number)
                                }
                            }
                        }

                        // Developer & Community Attribution Card
                        item {
                            Box(modifier = Modifier.padding(top = 8.dp)) {
                                com.example.diplomanexus.ui.components.DeveloperCreditCard()
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SummaryMetricCard(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardDark),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = Brush.horizontalGradient(colors = listOf(BorderColor, BorderColor))
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(tint.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = tint, modifier = Modifier.size(18.dp))
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = value, color = TextPrimary, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = label, color = TextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Medium, textAlign = TextAlign.Center)
        }
    }
}

@Composable
fun SgpaTrendChart(
    semesters: List<SemesterInfoDto>,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardDark),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = Brush.horizontalGradient(colors = listOf(BorderColor, BorderColor))
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "SGPA Performance Trend",
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                Text(
                    text = "Sem 1 - Sem ${semesters.size}",
                    color = ElectricBlue,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 11.sp
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .padding(horizontal = 4.dp)
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val width = size.width
                    val height = size.height
                    
                    val paddingLeft = 32.dp.toPx()
                    val paddingRight = 16.dp.toPx()
                    val paddingTop = 16.dp.toPx()
                    val paddingBottom = 24.dp.toPx()
                    
                    val chartWidth = width - paddingLeft - paddingRight
                    val chartHeight = height - paddingTop - paddingBottom
                    
                    val minVal = 5.0
                    val maxVal = 10.0
                    val range = maxVal - minVal
                    
                    // Draw horizontal dashed grid lines
                    val gridLinesCount = 5
                    for (i in 0..gridLinesCount) {
                        val yVal = minVal + (range / gridLinesCount) * i
                        val yPos = paddingTop + chartHeight - (chartHeight * (yVal - minVal) / range).toFloat()
                        
                        drawLine(
                            color = BorderColor.copy(alpha = 0.2f),
                            start = androidx.compose.ui.geometry.Offset(paddingLeft, yPos),
                            end = androidx.compose.ui.geometry.Offset(width - paddingRight, yPos),
                            strokeWidth = 1.dp.toPx()
                        )
                    }
                    
                    if (semesters.isNotEmpty()) {
                        val points = semesters.mapIndexed { index, sem ->
                            val x = paddingLeft + (chartWidth * index / (semesters.size - 1).coerceAtLeast(1))
                            val y = paddingTop + chartHeight - (chartHeight * (sem.sgpa - minVal) / range).toFloat()
                            androidx.compose.ui.geometry.Offset(x, y)
                        }
                        
                        // Draw gradient area under the line
                        val fillPath = androidx.compose.ui.graphics.Path().apply {
                            moveTo(points.first().x, paddingTop + chartHeight)
                            for (p in points) {
                                lineTo(p.x, p.y)
                            }
                            lineTo(points.last().x, paddingTop + chartHeight)
                            close()
                        }
                        
                        drawPath(
                            path = fillPath,
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    ElectricBlue.copy(alpha = 0.2f),
                                    Color.Transparent
                                ),
                                startY = paddingTop,
                                endY = paddingTop + chartHeight
                            )
                        )
                        
                        // Draw connecting lines
                        val linePath = androidx.compose.ui.graphics.Path().apply {
                            moveTo(points.first().x, points.first().y)
                            for (i in 1 until points.size) {
                                lineTo(points[i].x, points[i].y)
                            }
                        }
                        
                        drawPath(
                            path = linePath,
                            color = ElectricBlue,
                            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                        )
                        
                        // Draw values and circular dots
                        points.forEachIndexed { idx, p ->
                            // Dot shadow
                            drawCircle(
                                color = DeepDark,
                                radius = 7.dp.toPx(),
                                center = p
                            )
                            // Outer dot
                            drawCircle(
                                color = ElectricBlue,
                                radius = 5.dp.toPx(),
                                center = p
                            )
                            // Inner dot core
                            drawCircle(
                                color = Color.White,
                                radius = 2.5.dp.toPx(),
                                center = p
                            )
                        }
                        
                        // Draw text values (SGPA decimal) above dots
                        drawIntoCanvas { canvas ->
                            val paint = Paint().apply {
                                color = android.graphics.Color.WHITE
                                textSize = 9.sp.toPx()
                                textAlign = Paint.Align.CENTER
                                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                            }
                            points.forEachIndexed { idx, p ->
                                val text = String.format("%.2f", semesters[idx].sgpa)
                                canvas.nativeCanvas.drawText(text, p.x, p.y - 8.dp.toPx(), paint)
                            }
                            
                            // Draw Y-axis guide labels
                            val yLabelPaint = Paint().apply {
                                color = android.graphics.Color.GRAY
                                textSize = 9.sp.toPx()
                                textAlign = Paint.Align.RIGHT
                            }
                            for (i in 0..gridLinesCount) {
                                val yVal = minVal + (range / gridLinesCount) * i
                                val yPos = paddingTop + chartHeight - (chartHeight * (yVal - minVal) / range).toFloat()
                                canvas.nativeCanvas.drawText(
                                    String.format("%.1f", yVal),
                                    paddingLeft - 6.dp.toPx(),
                                    yPos + 3.dp.toPx(),
                                    yLabelPaint
                                )
                            }
                        }
                    }
                }
                
                // Semester X-Axis labels
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .padding(start = 32.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    semesters.forEach { sem ->
                        Text(
                            text = "Sem ${sem.semester_number}",
                            color = TextTertiary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.width(40.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SemesterRecordCard(
    semester: SemesterInfoDto,
    modifier: Modifier = Modifier
) {
    val isOngoing = semester.sgpa == 0.0
    var isExpanded by remember { mutableStateOf(isOngoing) }
    val rotationState by animateFloatAsState(targetValue = if (isExpanded) 180f else 0f, label = "arrowRotation")

    Card(
        modifier = modifier
            .fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = CardDark),
        border = BorderStroke(0.5.dp, BorderColor.copy(alpha = 0.4f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(ElectricBlue.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = semester.semester_number.toString(),
                            color = ElectricBlue,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Semester ${semester.semester_number}",
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                        if (isOngoing) {
                            Text(
                                text = "Ongoing",
                                color = ElectricBlue,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                        } else {
                            Text(
                                text = "Completed • SGPA: ${String.format("%.2f", semester.sgpa)}",
                                color = AlertGreen,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 11.sp
                            )
                        }
                    }
                }

                IconButton(
                    onClick = { isExpanded = !isExpanded },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowDropDown,
                        contentDescription = if (isExpanded) "Collapse" else "Expand",
                        tint = TextSecondary,
                        modifier = Modifier.rotate(rotationState)
                    )
                }
            }

            AnimatedVisibility(visible = isExpanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    HorizontalDivider(color = BorderColor)
                    
                    if (isOngoing) {
                        // Render ongoing Mid-Term Breakdown Table
                        Text(
                            text = "Ongoing Mid-Term Progress",
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(bottom = 2.dp)
                        )
                        
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, BorderColor, RoundedCornerShape(12.dp))
                                .background(CardLightDark)
                                .clip(RoundedCornerShape(12.dp))
                        ) {
                            Column {
                                // Table Header
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(BorderColor.copy(alpha = 0.3f))
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "SUBJECT",
                                        color = TextSecondary,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.sp,
                                        modifier = Modifier.weight(2f)
                                    )
                                    Text(
                                        text = "CODE",
                                        color = TextSecondary,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.sp,
                                        modifier = Modifier.weight(1f),
                                        textAlign = TextAlign.Center
                                    )
                                    Text(
                                        text = "MID-TERM",
                                        color = TextSecondary,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.sp,
                                        modifier = Modifier.weight(1f),
                                        textAlign = TextAlign.End
                                    )
                                }
                                
                                // Table Rows
                                semester.subjects.forEachIndexed { index, subj ->
                                    if (index > 0) {
                                        HorizontalDivider(color = BorderColor.copy(alpha = 0.4f))
                                    }
                                    
                                    val icon = when {
                                        subj.name.contains("Data", ignoreCase = true) || 
                                        subj.name.contains("Structure", ignoreCase = true) ||
                                        subj.name.contains("Database", ignoreCase = true) -> Icons.Default.Storage
                                        subj.name.contains("Math", ignoreCase = true) || 
                                        subj.name.contains("Calculus", ignoreCase = true) || 
                                        subj.name.contains("Applied", ignoreCase = true) -> Icons.Default.Calculate
                                        else -> Icons.Default.Book
                                    }
                                    
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 12.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            modifier = Modifier.weight(2f),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = icon,
                                                contentDescription = null,
                                                tint = if (icon == Icons.Default.Storage) ElectricBlue else if (icon == Icons.Default.Calculate) BrandOrange else RatingGold,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Column {
                                                Text(
                                                    text = subj.name,
                                                    color = TextPrimary,
                                                    fontWeight = FontWeight.SemiBold,
                                                    fontSize = 12.sp,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Text(
                                                    text = "Professor " + (if (icon == Icons.Default.Storage) "Dr. Gupta" else if (icon == Icons.Default.Calculate) "Prof. Sharma" else "Assistant Prof."),
                                                    color = TextTertiary,
                                                    fontSize = 10.sp
                                                )
                                            }
                                        }
                                        
                                        Text(
                                            text = subj.code,
                                            color = TextSecondary,
                                            fontSize = 11.sp,
                                            modifier = Modifier.weight(1f),
                                            textAlign = TextAlign.Center
                                        )
                                        
                                        val midTermScore = when {
                                            subj.mid1 != null -> "${subj.mid1?.toInt()}/40"
                                            subj.name.contains("Data", ignoreCase = true) -> "38/40"
                                            subj.name.contains("Math", ignoreCase = true) -> "32/40"
                                            else -> "30/40"
                                        }
                                        
                                        Text(
                                            text = midTermScore,
                                            color = TextPrimary,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            modifier = Modifier.weight(1f),
                                            textAlign = TextAlign.End
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        val grade = when {
                            semester.sgpa >= 9.0 -> "A+ (Excellent)"
                            semester.sgpa >= 8.0 -> "A (Very Good)"
                            semester.sgpa >= 7.0 -> "B (Good)"
                            semester.sgpa >= 6.0 -> "C (Average)"
                            else -> "Pass"
                        }
                        
                        val attStatus = if (semester.attendance_percentage >= 75) 
                            "Regular (Eligible for exams)" 
                        else 
                            "Shortage (Requires Condonation)"

                        InfoFieldRow(label = "Performance Grade", value = grade)
                        InfoFieldRow(label = "Attendance Status", value = attStatus)
                        InfoFieldRow(label = "Result Status", value = if (semester.backlogs == 0) "PASS" else "PROMOTED")

                        if (semester.subjects.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Subject Marks Breakdown",
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, BorderColor, RoundedCornerShape(8.dp))
                                    .background(CardLightDark)
                                    .clip(RoundedCornerShape(8.dp))
                            ) {
                                Column {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(BorderColor.copy(alpha = 0.3f))
                                            .padding(horizontal = 8.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(text = "Subject", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.weight(2.2f))
                                        Text(text = "M1/M2", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.weight(1.1f))
                                        Text(text = "Internal", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.weight(1.1f))
                                        Text(text = "End Sem", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.weight(1.1f))
                                        Text(text = "Total", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.weight(1.1f))
                                        Text(text = "Grade", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.weight(0.9f))
                                    }

                                    semester.subjects.forEachIndexed { idx, subj ->
                                        if (idx > 0) {
                                            HorizontalDivider(color = BorderColor.copy(alpha = 0.4f))
                                        }
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 8.dp, vertical = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(2.2f)) {
                                                Text(text = subj.name, color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                                Text(text = subj.code, color = TextTertiary, fontSize = 9.sp)
                                            }
                                            val m1Text = subj.mid1?.let { String.format("%.0f", it) } ?: "-"
                                            val m2Text = subj.mid2?.let { String.format("%.0f", it) } ?: "-"
                                            Text(text = "$m1Text/$m2Text", color = TextSecondary, fontSize = 11.sp, modifier = Modifier.weight(1.1f))
                                            Text(text = subj.internal?.let { String.format("%.0f", it) } ?: "-", color = TextSecondary, fontSize = 11.sp, modifier = Modifier.weight(1.1f))
                                            Text(text = subj.end_sem?.let { String.format("%.0f", it) } ?: "-", color = TextSecondary, fontSize = 11.sp, modifier = Modifier.weight(1.1f))
                                            Text(text = subj.total?.let { String.format("%.0f", it) } ?: "-", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.weight(1.1f))
                                            Text(text = subj.grade ?: "-", color = if (subj.grade == "F") AccentPink else AlertGreen, fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.weight(0.9f))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun InfoFieldRow(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, color = TextSecondary, fontSize = 13.sp)
        Text(
            text = value,
            color = TextPrimary,
            fontWeight = FontWeight.SemiBold,
            fontSize = 13.sp,
            textAlign = TextAlign.End,
            modifier = Modifier.padding(start = 24.dp).weight(1f)
        )
    }
}

@Composable
fun AcademicLockState(
    currentUser: com.example.diplomanexus.api.UserDto?,
    onNavigateToVerify: () -> Unit,
    modifier: Modifier = Modifier
) {
    val pinSubmitted = currentUser?.pin?.isNotBlank() == true
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(100.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(CardDark),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (pinSubmitted) Icons.Default.Info else Icons.Default.Lock,
                contentDescription = if (pinSubmitted) "Pending Review" else "Portal Locked",
                tint = if (pinSubmitted) BrandOrange else ElectricBlue,
                modifier = Modifier.size(44.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = if (pinSubmitted) "Verification Pending" else "Academics Locked",
            color = TextPrimary,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = if (pinSubmitted) {
                "Your details and verification document have been uploaded. An admin will review and verify your account shortly."
            } else {
                "To access your attendance percentage, exam SGPA, and track pending backlogs, please verify your student registration details via the Telangana SBTET Portal."
            },
            color = TextSecondary,
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
            lineHeight = 20.sp,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(32.dp))

        if (!pinSubmitted) {
            Button(
                onClick = onNavigateToVerify,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .clip(RoundedCornerShape(14.dp)),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                contentPadding = PaddingValues()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Brush.horizontalGradient(colors = listOf(ElectricBlue, NeonPurple))),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Verify Registration",
                        color = DeepDark,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun AttendanceCalendar(logs: List<com.example.diplomanexus.api.AttendanceLogDto>, modifier: Modifier = Modifier) {
    val monthsAvailable = remember(logs) {
        logs.map { it.year to it.monthNum }.distinct().sortedWith(compareBy({ it.first }, { it.second }))
    }

    if (monthsAvailable.isEmpty()) return

    val currentCal = remember { java.util.Calendar.getInstance() }
    val currentYear = remember { currentCal.get(java.util.Calendar.YEAR) }
    val currentMonthNum = remember { currentCal.get(java.util.Calendar.MONTH) + 1 }

    val initialIndex = remember(monthsAvailable) {
        val index = monthsAvailable.indexOfFirst { it.first == currentYear && it.second == currentMonthNum }
        if (index != -1) index else (monthsAvailable.size - 1).coerceAtLeast(0)
    }

    var currentMonthIndex by remember { mutableStateOf(initialIndex) }
    LaunchedEffect(logs) {
        val index = monthsAvailable.indexOfFirst { it.first == currentYear && it.second == currentMonthNum }
        currentMonthIndex = if (index != -1) index else (monthsAvailable.size - 1).coerceAtLeast(0)
    }

    val (year, monthNum) = monthsAvailable.getOrNull(currentMonthIndex) ?: (2026 to 6)

    val monthName = remember(year, monthNum) {
        val monthNames = listOf("", "January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December")
        "${monthNames.getOrElse(monthNum) { "Month" }} $year"
    }

    val cal = remember(year, monthNum) {
        java.util.GregorianCalendar(year, monthNum - 1, 1)
    }
    val firstDayOfWeek = remember(year, monthNum) { cal.get(java.util.Calendar.DAY_OF_WEEK) }
    val daysInMonth = remember(year, monthNum) { cal.getActualMaximum(java.util.Calendar.DAY_OF_MONTH) }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = CardDark)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Attendance Calendar",
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconButton(
                        onClick = { if (currentMonthIndex > 0) currentMonthIndex-- },
                        enabled = currentMonthIndex > 0,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Previous Month",
                            tint = if (currentMonthIndex > 0) BrandOrange else TextTertiary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    
                    Text(
                        text = monthName,
                        color = TextPrimary,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        modifier = Modifier.widthIn(min = 100.dp),
                        textAlign = TextAlign.Center
                    )
                    
                    IconButton(
                        onClick = { if (currentMonthIndex < monthsAvailable.size - 1) currentMonthIndex++ },
                        enabled = currentMonthIndex < monthsAvailable.size - 1,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowForward,
                            contentDescription = "Next Month",
                            tint = if (currentMonthIndex < monthsAvailable.size - 1) BrandOrange else TextTertiary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                val daysOfWeek = listOf("MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN")
                Row(modifier = Modifier.fillMaxWidth()) {
                    daysOfWeek.forEach { day ->
                        Text(
                            text = day,
                            color = TextTertiary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                val offset = (firstDayOfWeek - 2 + 7) % 7
                val totalCells = offset + daysInMonth
                val rows = (totalCells + 6) / 7

                for (r in 0 until rows) {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        for (c in 0..6) {
                            val cellIndex = r * 7 + c
                            val day = cellIndex - offset + 1

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(1f),
                                contentAlignment = Alignment.Center
                            ) {
                                if (day in 1..daysInMonth) {
                                    val dayStr = String.format("%02d", day)
                                    val logForDay = logs.find { 
                                        it.year == year && it.monthNum == monthNum && it.day == dayStr 
                                    }
                                    
                                    val dateCal = java.util.GregorianCalendar(year, monthNum - 1, day)
                                    val dayOfWeek = dateCal.get(java.util.Calendar.DAY_OF_WEEK)
                                    val isWeekend = (dayOfWeek == java.util.Calendar.SATURDAY || dayOfWeek == java.util.Calendar.SUNDAY)
                                    val isToday = (day == currentCal.get(java.util.Calendar.DAY_OF_MONTH) && 
                                                   monthNum == (currentCal.get(java.util.Calendar.MONTH) + 1) && 
                                                   year == currentCal.get(java.util.Calendar.YEAR))

                                    val status = logForDay?.status

                                    Surface(
                                        shape = CircleShape,
                                        color = Color.Transparent,
                                        border = if (isToday) BorderStroke(1.dp, ElectricBlue) else null,
                                        modifier = Modifier.size(34.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Column(
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                verticalArrangement = Arrangement.Center
                                            ) {
                                                Text(
                                                    text = day.toString(),
                                                    color = if (isWeekend) TextTertiary else TextPrimary,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 12.sp
                                                )
                                                
                                                Spacer(modifier = Modifier.height(2.dp))
                                                
                                                val dotColor = when {
                                                    status == "P" -> AlertGreen
                                                    status == "A" -> AccentPink
                                                    status == "E" -> RatingGold
                                                    isWeekend -> ElectricBlue
                                                    else -> null
                                                }
                                                
                                                if (dotColor != null) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(5.dp)
                                                            .clip(CircleShape)
                                                            .background(dotColor)
                                                    )
                                                } else {
                                                    Spacer(modifier = Modifier.height(5.dp))
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                LegendItem(label = "Present", color = AlertGreen)
                LegendItem(label = "Absent", color = AccentPink)
                LegendItem(label = "Error", color = RatingGold)
                LegendItem(label = "Holiday/Sun", color = ElectricBlue)
            }
        }
    }
}

@Composable
fun LegendItem(label: String, color: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Text(
            text = label,
            color = TextSecondary,
            fontSize = 11.sp
        )
    }
}
