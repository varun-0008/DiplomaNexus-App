package com.example.diplomanexus.ui.screens

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.net.Uri
import android.util.Base64
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.PickVisualMediaRequest
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import com.example.diplomanexus.api.PostDto
import com.example.diplomanexus.api.CommentDto
import com.example.diplomanexus.utils.ImageCompressor
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import com.example.diplomanexus.theme.*
import com.example.diplomanexus.viewmodel.AppViewModel
import java.io.ByteArrayOutputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    viewModel: AppViewModel,
    onOpenSettings: () -> Unit,
    onNavigateToVerify: () -> Unit,
    onOpenNotifications: () -> Unit,
    onOpenMarketplace: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currentUser by viewModel.currentUser.collectAsState()
    val posts by viewModel.posts.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    val context = LocalContext.current
    var tempAvatarBase64 by remember { mutableStateOf<String?>(null) }
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            val base64 = ImageCompressor.compressImageFromUri(context, uri)
            if (base64 != null) {
                tempAvatarBase64 = base64
            }
        }
    }

    LaunchedEffect(Unit) {
        viewModel.fetchPosts()
    }

    val followersCount = currentUser?.followers_count ?: 0
    val followingCount = currentUser?.following_count ?: 0
    val friendsCount = currentUser?.friends_count ?: 0

    val userPostsCount = remember(posts, currentUser) {
        posts.count { it.username == currentUser?.username && it.media_type != "tweet" && it.media_type != "story" }
    }
    val userTweetsCount = remember(posts, currentUser) {
        posts.count { it.username == currentUser?.username && it.media_type == "tweet" }
    }

    val myFeedItems = remember(posts, currentUser) {
        posts.filter { it.username == currentUser?.username && it.media_type != "story" }
    }

    var expandedPostIds by remember { mutableStateOf(emptySet<Int>()) }

    val highlights by viewModel.highlights.collectAsState()
    val closeFriends by viewModel.closeFriends.collectAsState()

    var showCreateHighlightDialog by remember { mutableStateOf(false) }
    var activeHighlightStories by remember { mutableStateOf<List<PostDto>?>(null) }

    var showShareDialog by remember { mutableStateOf(false) }
    var activePostForShare by remember { mutableStateOf<PostDto?>(null) }

    LaunchedEffect(showShareDialog) {
        if (showShareDialog) {
            viewModel.fetchConversations()
        }
    }

    var isEditingMode by remember { mutableStateOf(false) }
    var bioText by remember { mutableStateOf(currentUser?.about_me ?: "") }
    var isBioExpanded by remember { mutableStateOf(false) }

    var showAvatarSelector by remember { mutableStateOf(false) }
    var selectedProfileTab by remember { mutableStateOf(0) } // 0 for Posts, 1 for Tweets

    var showUnverifiedPasswordDialog by remember { mutableStateOf(false) }
    var showChangePasswordDialog by remember { mutableStateOf(false) }

    // Avatar Presets (Hex Color combinations represented as small generated Base64 strings to mock real images)
    val avatarPresets = listOf(
        Pair("Azure Glow", "#00D2FF,#9D4EDD"),
        Pair("Cosmic Sunset", "#FF54B0,#9D4EDD"),
        Pair("Emerald Wave", "#00FF87,#60EFFF"),
        Pair("Gold Rush", "#FFD700,#FFA500")
    )

    // Helper to generate a colored bitmap -> Base64 string locally!
    fun generatePresetBase64(colorString: String): String {
        try {
            val colors = colorString.split(",")
            val c1 = android.graphics.Color.parseColor(colors[0])
            val c2 = android.graphics.Color.parseColor(colors[1])
            
            val bitmap = Bitmap.createBitmap(120, 120, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            val paint = Paint()
            val gradient = android.graphics.LinearGradient(
                0f, 0f, 120f, 120f,
                c1, c2,
                android.graphics.Shader.TileMode.CLAMP
            )
            paint.shader = gradient
            canvas.drawRect(0f, 0f, 120f, 120f, paint)

            // Draw a big white letter
            paint.shader = null
            paint.color = android.graphics.Color.WHITE
            paint.textSize = 64f
            paint.textAlign = Paint.Align.CENTER
            paint.isAntiAlias = true
            val xPos = canvas.width / 2f
            val yPos = (canvas.height / 2f) - ((paint.descent() + paint.ascent()) / 2f)
            canvas.drawText("D", xPos, yPos, paint)

            val stream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, stream)
            val bytes = stream.toByteArray()
            return Base64.encodeToString(bytes, Base64.DEFAULT)
        } catch (e: Exception) {
            return ""
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Profile", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 20.sp) },
                navigationIcon = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings", tint = TextPrimary)
                    }
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
                }
            )
        },
        modifier = modifier.fillMaxSize()
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(DeepDark)
                .padding(paddingValues),
            contentPadding = PaddingValues(bottom = 80.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // ─── Top Area: Floating Glassmorphic Dashboard ───────────────────
            item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .drawBehind {
                        // Top-Left soft blue glow (secondary-container)
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(Color(0xFF0CB3FF).copy(alpha = 0.2f), Color.Transparent),
                                radius = 250.dp.toPx()
                            ),
                            radius = 250.dp.toPx(),
                            center = androidx.compose.ui.geometry.Offset(-30.dp.toPx(), -30.dp.toPx())
                        )
                        // Bottom-Right fuchsia glow
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(Color(0xFFFF00FF).copy(alpha = 0.15f), Color.Transparent),
                                radius = 220.dp.toPx()
                            ),
                            radius = 220.dp.toPx(),
                            center = androidx.compose.ui.geometry.Offset(size.width + 30.dp.toPx(), size.height + 30.dp.toPx())
                        )
                    }
                    .background(
                        color = CardDark.copy(alpha = 0.3f), // bg-surface-container/30
                        shape = RoundedCornerShape(24.dp)
                    )
                    .border(
                        width = 1.dp,
                        color = Color.White.copy(alpha = 0.1f), // border-white/10
                        shape = RoundedCornerShape(24.dp)
                    )
            ) {
                // Specular Highlight Top Edge
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(
                            Brush.horizontalGradient(
                                listOf(Color.Transparent, Color.White.copy(alpha = 0.2f), Color.Transparent)
                            )
                        )
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp), // p-stack-lg reduced to 16.dp
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Centered Avatar with light blue capsule checkmark overlay at bottom
                    Box(
                        contentAlignment = Alignment.BottomCenter,
                        modifier = Modifier.padding(bottom = 10.dp) // mb-stack-md reduced
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(80.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.05f))
                                .border(1.5.dp, Color(0xFF131316), CircleShape)
                                .drawBehind {
                                    drawCircle(
                                        brush = Brush.radialGradient(
                                            colors = listOf(Color(0xFF0CB3FF).copy(alpha = 0.3f), Color.Transparent),
                                            radius = 48.dp.toPx()
                                        ),
                                        radius = 48.dp.toPx()
                                    )
                                }
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(76.dp)
                                    .clip(CircleShape)
                                    .clickable {
                                        if (!isEditingMode) {
                                            bioText = currentUser?.about_me ?: ""
                                            tempAvatarBase64 = currentUser?.profile_pic_base64
                                            isEditingMode = true
                                        } else {
                                            photoPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                                        }
                                    }
                            ) {
                                AvatarView(
                                    base64 = if (isEditingMode) tempAvatarBase64 else currentUser?.profile_pic_base64,
                                    name = currentUser?.student_name ?: currentUser?.username ?: "U",
                                    size = 76.dp
                                )
                                if (isEditingMode) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(Color.Black.copy(alpha = 0.4f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.PhotoCamera,
                                            contentDescription = "Change Photo",
                                            tint = Color.White.copy(alpha = 0.85f),
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                    
                    // Centered Name
                    Text(
                        text = currentUser?.student_name ?: "Guest User",
                        color = TextPrimary,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 18.sp, // headline-md -> reduced to 18.sp
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(bottom = 2.dp)
                    )
                    
                    // Centered Branch Pill
                    val branchVal = currentUser?.branch ?: "Mechanical Engineering"
                    Box(
                        modifier = Modifier
                            .padding(bottom = 14.dp) // mb-stack-lg -> reduced to 14.dp
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.05f)) // bg-white/5
                            .border(0.5.dp, Color.White.copy(alpha = 0.1f), CircleShape) // border-white/10
                            .padding(horizontal = 8.dp, vertical = 2.5.dp)
                    ) {
                        Text(
                            text = branchVal,
                            color = Color(0xFF8BCEFF), // secondary color
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp, // label-md -> reduced to 11.sp
                            textAlign = TextAlign.Center
                        )
                    }

                    // ─── Inline Avatar Presets (Editing Mode) ───────────────
                    if (isEditingMode) {
                        Text(
                            text = "CHOOSE AVATAR",
                            color = TextSecondary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 9.sp,
                            letterSpacing = 1.sp,
                            modifier = Modifier.padding(bottom = 6.dp, top = 2.dp)
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(bottom = 12.dp)
                        ) {
                            // Custom Photo Upload Action Circle
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.08f))
                                    .border(1.dp, Color.White.copy(alpha = 0.15f), CircleShape)
                                    .clickable {
                                        photoPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AddAPhoto,
                                    contentDescription = "Upload Custom Photo",
                                    tint = TextPrimary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }

                            avatarPresets.forEach { preset ->
                                val gradientColors = preset.second.split(",").map { android.graphics.Color.parseColor(it) }
                                val presetBase64 = generatePresetBase64(preset.second)
                                val isSelected = tempAvatarBase64 == presetBase64
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(Brush.linearGradient(colors = listOf(Color(gradientColors[0]), Color(gradientColors[1]))))
                                        .border(width = if (isSelected) 2.dp else 0.dp, color = if (isSelected) BrandOrange else Color.Transparent, shape = CircleShape)
                                        .clickable { if (presetBase64.isNotBlank()) { tempAvatarBase64 = presetBase64 } },
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isSelected) {
                                        Icon(imageVector = Icons.Default.Check, contentDescription = "Selected", tint = Color.White, modifier = Modifier.size(14.dp))
                                    }
                                }
                            }
                        }
                    }
                    
                    // Centered Stats Row with thin vertical dividers
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(text = userPostsCount.toString(), color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                            Text(text = "Posts", color = TextSecondary, fontSize = 11.sp)
                        }
                        
                        Box(modifier = Modifier.width(1.dp).height(24.dp).background(Color.White.copy(alpha = 0.1f))) // reduced height from 32
                        
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(text = userTweetsCount.toString(), color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                            Text(text = "Tweets", color = TextSecondary, fontSize = 11.sp)
                        }
                        
                        Box(modifier = Modifier.width(1.dp).height(24.dp).background(Color.White.copy(alpha = 0.1f)))
                        
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(text = followingCount.toString(), color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                            Text(text = "Following", color = TextSecondary, fontSize = 11.sp)
                        }
                        
                        Box(modifier = Modifier.width(1.dp).height(24.dp).background(Color.White.copy(alpha = 0.1f)))
                        
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(text = followersCount.toString(), color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                            Text(text = "Followers", color = TextSecondary, fontSize = 11.sp)
                        }
                        
                        Box(modifier = Modifier.width(1.dp).height(24.dp).background(Color.White.copy(alpha = 0.1f)))
                        
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(text = friendsCount.toString(), color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                            Text(text = "Friends", color = TextSecondary, fontSize = 11.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp)) // spacer between stats and bio
                    
                    // Biography Section (Inside the dashboard user info card)
                    val bioTextVal = currentUser?.about_me ?: "Click here to add your custom student bio..."
                    val showSeeMore = (currentUser?.about_me ?: "").length > 90

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp)
                    ) {
                        Text(
                            text = "BIOGRAPHY",
                            color = TextSecondary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp, // reduced to 10.sp
                            letterSpacing = 0.8.sp,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                        
                        if (isEditingMode) {
                            OutlinedTextField(
                                value = bioText,
                                onValueChange = { bioText = it },
                                placeholder = { Text("Tell the campus about yourself...", color = TextSecondary, fontSize = 12.sp) },
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextPrimary,
                                    focusedBorderColor = BrandOrange,
                                    unfocusedBorderColor = Color.White.copy(alpha = 0.15f),
                                    focusedContainerColor = Color.White.copy(alpha = 0.03f),
                                    unfocusedContainerColor = Color.White.copy(alpha = 0.01f)
                                ),
                                modifier = Modifier.fillMaxWidth().height(90.dp)
                            )
                        } else {
                            Text(
                                text = bioTextVal,
                                color = TextPrimary,
                                fontSize = 12.sp, // reduced to 12.sp
                                lineHeight = 16.sp,
                                maxLines = if (isBioExpanded) Int.MAX_VALUE else 2, // reduced default display to 2 lines
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        if (currentUser?.about_me == null) {
                                            bioText = ""
                                            tempAvatarBase64 = currentUser?.profile_pic_base64
                                            isEditingMode = true
                                        } else {
                                            isBioExpanded = !isBioExpanded
                                        }
                                    }
                            )
                            
                            if (showSeeMore) {
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = if (isBioExpanded) "See less" else "See more",
                                    color = BrandOrange,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier
                                        .clickable { isBioExpanded = !isBioExpanded }
                                        .padding(vertical = 1.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp)) // spacer between bio and buttons
                    
                    // Action Buttons Row (Edit Profile & Share Profile)
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (isEditingMode) {
                            Button(
                                onClick = {
                                    viewModel.updateProfile(bioText.trim(), tempAvatarBase64) { isEditingMode = false }
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(36.dp),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = BrandOrange,
                                    contentColor = Color.White
                                ),
                                contentPadding = PaddingValues(0.dp),
                                enabled = !isLoading
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    if (isLoading) {
                                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                                    } else {
                                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Text("Save", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                    }
                                }
                            }
                            
                            Button(
                                onClick = { isEditingMode = false },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(36.dp),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color.White.copy(alpha = 0.05f),
                                    contentColor = TextPrimary
                                ),
                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
                                contentPadding = PaddingValues(0.dp),
                                enabled = !isLoading
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Text("Cancel", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                }
                            }
                        } else {
                            Button(
                                onClick = {
                                    bioText = currentUser?.about_me ?: ""
                                    tempAvatarBase64 = currentUser?.profile_pic_base64
                                    isEditingMode = true
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(36.dp), // reduced from 40.dp
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = BrandOrange,
                                    contentColor = Color(0xFF5F1500)
                                ),
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Text("Edit Profile", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                }
                            }
                            
                            val context = LocalContext.current
                            Button(
                                onClick = {
                                    val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(android.content.Intent.EXTRA_SUBJECT, "DiplomaNexus Profile")
                                        putExtra(android.content.Intent.EXTRA_TEXT, "Check out ${currentUser?.student_name ?: currentUser?.username}'s profile on DiplomaNexus!")
                                    }
                                    context.startActivity(android.content.Intent.createChooser(intent, "Share Profile"))
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(36.dp), // reduced from 40.dp
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color.White.copy(alpha = 0.05f),
                                    contentColor = TextPrimary
                                ),
                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Text("Share Profile", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
            }
            } // close dashboard item

            // ─── Story Highlights Row ─────────────────────────────────────────
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "Highlights",
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                    
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())
                    ) {
                        // Create New Highlight plus button (Only for current user)
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .clickable { showCreateHighlightDialog = true }
                                .width(64.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.05f))
                                    .border(1.dp, Color.White.copy(alpha = 0.15f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "New Highlight",
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "New",
                                color = TextSecondary,
                                fontSize = 11.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        // Highlights Circles list
                        highlights.forEach { hl ->
                            var showMenu by remember { mutableStateOf(false) }
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .width(64.dp)
                                    .pointerInput(hl.id) {
                                        detectTapGestures(
                                            onTap = {
                                                val allPosts = viewModel.posts.value
                                                val hlStories = allPosts.filter { hl.storyIds.contains(it.id) }
                                                if (hlStories.isNotEmpty()) {
                                                    activeHighlightStories = hlStories
                                                }
                                            },
                                            onLongPress = {
                                                showMenu = true
                                            }
                                        )
                                    }
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(56.dp)
                                        .clip(CircleShape)
                                        .background(Color.White.copy(alpha = 0.08f))
                                        .border(1.dp, Color.White.copy(alpha = 0.2f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (hl.coverImage != null) {
                                        if (hl.coverImage.startsWith("base64,")) {
                                            val b64 = hl.coverImage.substringAfter("base64,")
                                            val bytes = Base64.decode(b64, Base64.DEFAULT)
                                            val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                                            if (bitmap != null) {
                                                Image(
                                                    bitmap = bitmap.asImageBitmap(),
                                                    contentDescription = null,
                                                    contentScale = ContentScale.Crop,
                                                    modifier = Modifier.fillMaxSize()
                                                )
                                            }
                                        } else {
                                            AsyncImage(
                                                model = hl.coverImage,
                                                contentDescription = null,
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier.fillMaxSize()
                                            )
                                        }
                                    } else {
                                        Text(
                                            text = hl.name.take(1).uppercase(),
                                            color = BrandOrange,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 18.sp
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = hl.name,
                                    color = TextPrimary,
                                    fontSize = 11.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )

                                DropdownMenu(
                                    expanded = showMenu,
                                    onDismissRequest = { showMenu = false },
                                    modifier = Modifier.background(CardDark)
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("Delete", color = AccentPink) },
                                        onClick = {
                                            showMenu = false
                                            viewModel.removeHighlight(hl.id)
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // ─── Bottom 2/3 Area: Scrollable Feed ────────────────────────────
            // Tab Selector Capsule Segment Row
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                                .height(44.dp)
                                .clip(RoundedCornerShape(22.dp))
                                .background(CardDark.copy(alpha = 0.5f))
                                .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(22.dp))
                                .padding(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(18.dp))
                                    .background(if (selectedProfileTab == 0) Color.White.copy(alpha = 0.1f) else Color.Transparent)
                                    .border(if (selectedProfileTab == 0) BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)) else BorderStroke(0.dp, Color.Transparent), RoundedCornerShape(18.dp))
                                    .clickable { selectedProfileTab = 0 },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Posts",
                                    color = if (selectedProfileTab == 0) TextPrimary else TextSecondary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                            
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(18.dp))
                                    .background(if (selectedProfileTab == 1) Color.White.copy(alpha = 0.1f) else Color.Transparent)
                                    .border(if (selectedProfileTab == 1) BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)) else BorderStroke(0.dp, Color.Transparent), RoundedCornerShape(18.dp))
                                    .clickable { selectedProfileTab = 1 },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Tweets",
                                    color = if (selectedProfileTab == 1) TextPrimary else TextSecondary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                    
                    if (selectedProfileTab == 0) {
                        // Posts Tab
                        // User's photo/video posts from API
                        val photoVideoPosts = myFeedItems.filter { it.media_type != "tweet" }
                        if (photoVideoPosts.isNotEmpty()) {
                            items(photoVideoPosts, key = { "post_${it.id}" }) { post ->
                                InstaPostCard(
                                    post = post,
                                    isExpanded = expandedPostIds.contains(post.id),
                                    viewModel = viewModel,
                                    onLikeClick = { viewModel.toggleLike(post.id) },
                                    onCommentClick = {
                                        expandedPostIds = if (expandedPostIds.contains(post.id)) {
                                            expandedPostIds - post.id
                                        } else {
                                            expandedPostIds + post.id
                                        }
                                    },
                                    onShareClick = {
                                        activePostForShare = post
                                        showShareDialog = true
                                    }
                                )
                            }
                        }
                    } else {
                        // Tweets Tab
                        val tweetPosts = myFeedItems.filter { it.media_type == "tweet" }
                        if (tweetPosts.isNotEmpty()) {
                            items(tweetPosts, key = { "tweet_${it.id}" }) { post ->
                                InstaPostCard(
                                    post = post,
                                    isExpanded = expandedPostIds.contains(post.id),
                                    viewModel = viewModel,
                                    onLikeClick = { viewModel.toggleLike(post.id) },
                                    onCommentClick = {
                                        expandedPostIds = if (expandedPostIds.contains(post.id)) {
                                            expandedPostIds - post.id
                                        } else {
                                            expandedPostIds + post.id
                                        }
                                    },
                                    onShareClick = {
                                        activePostForShare = post
                                        showShareDialog = true
                                    }
                                )
                            }
                        } else {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 60.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("No tweets yet.", color = TextSecondary, fontSize = 13.sp)
                                }
                            }
                        }

                    // Developer & Community Attribution Card
                    item(key = "developer_credit_card") {
                        Box(modifier = Modifier.padding(16.dp)) {
                            com.example.diplomanexus.ui.components.DeveloperCreditCard()
                        }
                    }
                }
            }

    if (showUnverifiedPasswordDialog) {
        AlertDialog(
            onDismissRequest = { showUnverifiedPasswordDialog = false },
            modifier = Modifier.border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.18f),
                        Color.White.copy(alpha = 0.02f)
                    )
                ),
                shape = RoundedCornerShape(24.dp)
            ),
            title = { Text("Account Unverified", fontWeight = FontWeight.Bold, color = TextPrimary) },
            text = {
                Text(
                    "You can only change your password after your account has been verified by an administrator. Please submit your academic verification first.",
                    color = TextSecondary,
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = { showUnverifiedPasswordDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = BrandOrange)
                ) {
                    Text("OK", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            containerColor = CardDark.copy(alpha = 0.65f),
            shape = RoundedCornerShape(24.dp)
        )
    }

    if (showChangePasswordDialog) {
        var oldPassword by remember { mutableStateOf("") }
        var newPassword by remember { mutableStateOf("") }
        var confirmPassword by remember { mutableStateOf("") }
        var statusMessage by remember { mutableStateOf<String?>(null) }
        var isSuccess by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { 
                if (!isLoading) {
                    showChangePasswordDialog = false 
                }
            },
            modifier = Modifier.border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.18f),
                        Color.White.copy(alpha = 0.02f)
                    )
                ),
                shape = RoundedCornerShape(24.dp)
            ),
            title = { Text("Change Password", fontWeight = FontWeight.Bold, color = TextPrimary) },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Enter your current password and choose a new secure password.", color = TextSecondary, fontSize = 13.sp)
                    
                    OutlinedTextField(
                        value = oldPassword,
                        onValueChange = { oldPassword = it },
                        placeholder = { Text("Current Password", color = TextSecondary) },
                        visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedBorderColor = BrandOrange,
                            unfocusedBorderColor = BorderColor
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = newPassword,
                        onValueChange = { newPassword = it },
                        placeholder = { Text("New Password", color = TextSecondary) },
                        visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedBorderColor = BrandOrange,
                            unfocusedBorderColor = BorderColor
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = confirmPassword,
                        onValueChange = { confirmPassword = it },
                        placeholder = { Text("Confirm New Password", color = TextSecondary) },
                        visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedBorderColor = BrandOrange,
                            unfocusedBorderColor = BorderColor
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    statusMessage?.let { msg ->
                        Text(
                            text = msg,
                            color = if (isSuccess) VerifiedBlue else AccentPink,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (oldPassword.isBlank() || newPassword.isBlank()) {
                            statusMessage = "Fields cannot be empty"
                            isSuccess = false
                            return@Button
                        }
                        if (newPassword != confirmPassword) {
                            statusMessage = "New passwords do not match"
                            isSuccess = false
                            return@Button
                        }
                        
                        viewModel.changePassword(oldPassword, newPassword, 
                            onSuccess = { msg ->
                                statusMessage = msg
                                isSuccess = true
                                oldPassword = ""
                                newPassword = ""
                                confirmPassword = ""
                            },
                            onError = { err ->
                                statusMessage = err
                                isSuccess = false
                            }
                        )
                    },
                    enabled = !isLoading && !isSuccess,
                    colors = ButtonDefaults.buttonColors(containerColor = BrandOrange)
                ) {
                    Text("Update", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showChangePasswordDialog = false },
                    enabled = !isLoading
                ) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            containerColor = CardDark.copy(alpha = 0.65f),
            shape = RoundedCornerShape(24.dp)
        )
    }

    // Share dialog
    if (showShareDialog && activePostForShare != null) {
        val post = activePostForShare!!
        val conversations by viewModel.conversations.collectAsState()
        val context = LocalContext.current
        var sentRooms by remember { mutableStateOf(setOf<Int>()) }

        AlertDialog(
            onDismissRequest = { showShareDialog = false },
            modifier = Modifier.border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.18f),
                        Color.White.copy(alpha = 0.02f)
                    )
                ),
                shape = RoundedCornerShape(24.dp)
            ),
            title = { Text("Share Post", fontWeight = FontWeight.Bold, color = TextPrimary) },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(340.dp)
                ) {
                    Text(
                        text = "Send via Direct Message",
                        color = TextSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (conversations.isEmpty()) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 32.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("No active chats yet.", color = TextSecondary, fontSize = 13.sp)
                                }
                            }
                        } else {
                            items(conversations) { conversation ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color.White.copy(alpha = 0.03f))
                                        .padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    AvatarView(
                                        base64 = conversation.other_profile_pic_base64,
                                        name = conversation.other_student_name ?: conversation.other_username,
                                        size = 32.dp
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = conversation.other_student_name ?: conversation.other_username,
                                        color = TextPrimary,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium,
                                        modifier = Modifier.weight(1f)
                                    )
                                    
                                    val alreadySent = sentRooms.contains(conversation.id)
                                    Button(
                                        onClick = {
                                            if (!alreadySent) {
                                                viewModel.sendMessage(conversation.id, post.id.toString(), "post")
                                                sentRooms = sentRooms + conversation.id
                                                android.widget.Toast.makeText(context, "Post shared successfully!", android.widget.Toast.LENGTH_SHORT).show()
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (alreadySent) Color(0xFF2E7D32) else BrandOrange,
                                            contentColor = Color.White
                                        ),
                                        shape = RoundedCornerShape(16.dp),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                                        modifier = Modifier.height(28.dp)
                                    ) {
                                        Text(
                                            text = if (alreadySent) "Sent" else "Send",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }

                    HorizontalDivider(color = BorderColor, modifier = Modifier.padding(vertical = 12.dp))

                    Button(
                        onClick = {
                            val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(android.content.Intent.EXTRA_SUBJECT, "Check out this post on DiplomaNexus")
                                val shareText = "${post.student_name ?: post.username} just posted on DiplomaNexus:\n\n${post.content.take(100)}...\n\nJoin the app to see the full post!"
                                putExtra(android.content.Intent.EXTRA_TEXT, shareText)
                            }
                            context.startActivity(android.content.Intent.createChooser(intent, "Share Post via"))
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White.copy(alpha = 0.08f),
                            contentColor = TextPrimary
                        ),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f)),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.fillMaxWidth().height(36.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                            Text("Share to other apps", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showShareDialog = false }) {
                    Text("Close", color = BrandOrange)
                }
            },
            containerColor = CardDark.copy(alpha = 0.65f),
            shape = RoundedCornerShape(24.dp)
        )
    }



    if (showCreateHighlightDialog) {
        var highlightName by remember { mutableStateOf("") }
        val myPastStories = remember(posts, currentUser) {
            posts.filter { it.username == currentUser?.username && (it.media_type == "story" || it.content.startsWith("[STICKER:")) }
        }
        var selectedStories by remember { mutableStateOf(setOf<Int>()) }

        AlertDialog(
            onDismissRequest = { showCreateHighlightDialog = false },
            title = { Text("New Highlight Circle 📸", fontWeight = FontWeight.Bold, color = TextPrimary) },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(380.dp)
                ) {
                    OutlinedTextField(
                        value = highlightName,
                        onValueChange = { highlightName = it },
                        placeholder = { Text("Highlight Name (e.g. Campus)", color = TextSecondary) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedBorderColor = BrandOrange,
                            unfocusedBorderColor = BorderColor
                        ),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                    )
                    
                    Text("Select Stories to Add:", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 8.dp))
                    
                    LazyColumn(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (myPastStories.isEmpty()) {
                            item {
                                Box(modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp), contentAlignment = Alignment.Center) {
                                    Text("No past stories available.", color = TextSecondary, fontSize = 13.sp)
                                }
                            }
                        } else {
                            items(myPastStories) { story ->
                                val isChecked = selectedStories.contains(story.id)
                                val cleanCaption = remember(story.content) { parseStoryContent(story.content).first }
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color.White.copy(alpha = 0.03f))
                                        .clickable {
                                            selectedStories = if (isChecked) selectedStories - story.id else selectedStories + story.id
                                        }
                                        .padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(Color.White.copy(alpha = 0.05f))
                                    ) {
                                        if (story.media_url != null) {
                                            AsyncImage(model = story.media_url, contentDescription = null, contentScale = ContentScale.Crop)
                                        } else if (story.image_base64 != null) {
                                            val b64 = story.image_base64.substringAfter("base64,")
                                            val bytes = Base64.decode(b64, Base64.DEFAULT)
                                            val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                                            if (bitmap != null) {
                                                Image(bitmap = bitmap.asImageBitmap(), contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                                            }
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = cleanCaption.ifBlank { "Story (${story.created_at.take(10)})" },
                                        color = TextPrimary,
                                        fontSize = 13.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Checkbox(
                                        checked = isChecked,
                                        onCheckedChange = {
                                            selectedStories = if (isChecked) selectedStories - story.id else selectedStories + story.id
                                        },
                                        colors = CheckboxDefaults.colors(checkedColor = BrandOrange)
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (highlightName.isNotBlank() && selectedStories.isNotEmpty()) {
                            viewModel.addHighlight(highlightName.trim(), selectedStories.toList())
                            showCreateHighlightDialog = false
                        }
                    },
                    enabled = highlightName.isNotBlank() && selectedStories.isNotEmpty(),
                    colors = ButtonDefaults.buttonColors(containerColor = BrandOrange)
                ) {
                    Text("Save", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateHighlightDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            containerColor = CardDark.copy(alpha = 0.65f),
            shape = RoundedCornerShape(24.dp)
        )
    }

    if (activeHighlightStories != null) {
        StoryViewerDialog(
            username = currentUser?.username ?: "me",
            stories = activeHighlightStories!!,
            onDismiss = { activeHighlightStories = null }
        )
    }
    }
}

// ─── Profile Stat Item ───────────────────────────────────────────────────────

@Composable
private fun ProfileStatItem(
    label: String,
    count: Int,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        Text(
            text = count.toString(),
            color = TextPrimary,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 18.sp
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            color = TextSecondary,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium
        )
    }
}
