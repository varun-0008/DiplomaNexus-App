package com.example.diplomanexus.ui.screens

import android.graphics.BitmapFactory
import android.graphics.Bitmap
import android.util.Base64
import java.io.ByteArrayOutputStream
import android.net.Uri
import androidx.compose.ui.platform.LocalContext
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.runtime.DisposableEffect
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import com.example.diplomanexus.utils.MediaCacheManager
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.PickVisualMediaRequest
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.ui.draw.rotate
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.foundation.BorderStroke
import com.example.diplomanexus.api.CommentDto
import com.example.diplomanexus.api.PostDto
import com.example.diplomanexus.theme.*
import com.example.diplomanexus.viewmodel.AppViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedScreen(
    viewModel: AppViewModel,
    onNavigateToVerify: () -> Unit,
    onOpenCreatePost: () -> Unit,
    onOpenNotifications: () -> Unit,
    onOpenMarketplace: () -> Unit,
    modifier: Modifier = Modifier
) {
    val posts by viewModel.posts.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    val feedPosts = remember(posts) { posts.filter { it.media_type != "story" } }
    val storyPosts = remember(posts) { posts.filter { it.media_type == "story" } }
    val activeStories = remember(storyPosts) {
        storyPosts.filter { isWithin24Hours(it.created_at) }
    }
    val storiesByUser = remember(activeStories) {
        activeStories.groupBy { it.username }
    }
    var activeStoryUser by remember { mutableStateOf<String?>(null) }

    var showCreatePostDialog by remember { mutableStateOf(false) }
    var postText by remember { mutableStateOf("") }
    
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var selectedImageBase64 by remember { mutableStateOf<String?>(null) }
    var selectedMediaType by remember { mutableStateOf<String>("image") }
    val context = LocalContext.current

    val listState = rememberLazyListState()

    var caughtUpIndex by remember { mutableStateOf(-1) }
    var lastPostsSize by remember { mutableStateOf(0) }

    LaunchedEffect(posts) {
        val filtered = posts.filter { it.media_type != "story" }
        if (filtered.size != lastPostsSize) {
            caughtUpIndex = filtered.indexOfFirst { it.is_seen }
            lastPostsSize = filtered.size
        }
    }

    val visibleItemsInfo = remember {
        derivedStateOf {
            listState.layoutInfo.visibleItemsInfo
        }
    }

    LaunchedEffect(visibleItemsInfo.value) {
        visibleItemsInfo.value.forEach { item ->
            val postKey = item.key
            if (postKey is Int) {
                viewModel.markPostSeen(postKey)
            }
        }
    }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        selectedImageUri = uri
        if (uri != null) {
            try {
                val mimeType = context.contentResolver.getType(uri) ?: ""
                val isVideo = mimeType.startsWith("video")
                selectedMediaType = if (isVideo) "video" else "image"

                if (isVideo) {
                    val videoSize = com.example.diplomanexus.utils.ImageCompressor.getMediaSize(context, uri)
                    val maxVideoSize = 15 * 1024 * 1024 // 15MB limit
                    if (videoSize > maxVideoSize) {
                        android.widget.Toast.makeText(
                            context,
                            "Video is too large (Max 15MB). Please select a compressed video.",
                            android.widget.Toast.LENGTH_LONG
                        ).show()
                        selectedImageUri = null
                        selectedImageBase64 = null
                    } else {
                        val inputStream = context.contentResolver.openInputStream(uri)
                        val bytes = inputStream?.use { it.readBytes() }
                        if (bytes != null) {
                            selectedImageBase64 = "data:$mimeType;base64," + Base64.encodeToString(bytes, Base64.NO_WRAP)
                        }
                    }
                } else {
                    // Compress image using our memory-efficient stream utility
                    val compressedBase64 = com.example.diplomanexus.utils.ImageCompressor.compressImageFromUri(
                        context = context,
                        uri = uri,
                        targetWidth = 1080,
                        targetHeight = 1080,
                        quality = 80
                    )
                    if (compressedBase64 != null) {
                        selectedImageBase64 = compressedBase64
                    } else {
                        android.widget.Toast.makeText(
                            context,
                            "Failed to process image.",
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                        selectedImageUri = null
                    }
                }
            } catch (e: java.lang.Exception) {
                e.printStackTrace()
                android.widget.Toast.makeText(
                    context,
                    "Error loading media: ${e.localizedMessage}",
                    android.widget.Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    val gradientOptions = listOf(
        null,
        listOf(BrandOrange, AccentAmber),
        listOf(BrandOrangeLight, Color(0xFFFFD54F)),
        listOf(Color(0xFF6A1B9A), Color(0xFFE91E63)),
        listOf(Color(0xFF1565C0), Color(0xFF00BCD4))
    )
    var selectedGradientIndex by remember { mutableStateOf(0) }

    var expandedPostIds by remember { mutableStateOf(emptySet<Int>()) }
    var showNotificationsDialog by remember { mutableStateOf(false) }
    var showShareDialog by remember { mutableStateOf(false) }
    var activePostForShare by remember { mutableStateOf<PostDto?>(null) }

    LaunchedEffect(showShareDialog) {
        if (showShareDialog) {
            viewModel.fetchConversations()
        }
    }

    LaunchedEffect(Unit) { viewModel.fetchPosts() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.05f))
                                .border(1.dp, Color.White.copy(alpha = 0.2f), CircleShape)
                        ) {
                            AvatarView(
                                base64 = currentUser?.profile_pic_base64,
                                name = currentUser?.student_name ?: currentUser?.username ?: "U",
                                size = 40.dp
                            )
                        }
                        Text(
                            text = "DiplomaNexus",
                            fontWeight = FontWeight.Bold,
                            fontSize = 22.sp, // headline-lg-mobile (22px)
                            color = Color(0xFFFFB5A0) // primary (#ffb5a0)
                        )
                    }
                },
                windowInsets = WindowInsets(0.dp),
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(DeepDark)
                .drawBehind {
                    // Glow Orb 1 (Top-Left, Cyan, 60vw)
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(Color(0xFF00B2FF).copy(alpha = 0.12f), Color.Transparent),
                            radius = size.width * 0.6f
                        ),
                        radius = size.width * 0.6f,
                        center = androidx.compose.ui.geometry.Offset(-size.width * 0.1f, size.height * 0.2f)
                    )
                    // Glow Orb 2 (Bottom-Right, Magenta, 70vw)
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(Color(0xFFFF00FF).copy(alpha = 0.08f), Color.Transparent),
                            radius = size.width * 0.7f
                        ),
                        radius = size.width * 0.7f,
                        center = androidx.compose.ui.geometry.Offset(size.width * 1.1f, size.height * 0.9f)
                    )
                    // Glow Orb 3 (Centered, Orange, 80vw)
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(Color(0xFFFF5722).copy(alpha = 0.04f), Color.Transparent),
                            radius = size.width * 0.8f
                        ),
                        radius = size.width * 0.8f,
                        center = androidx.compose.ui.geometry.Offset(size.width * 0.5f, size.height * 0.5f)
                    )
                }
                .padding(paddingValues)
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 90.dp)
            ) {
                // Stories Row
                item {
                    StoriesRow(
                        currentUser = currentUser,
                        onNavigateToVerify = onNavigateToVerify,
                        onOpenCreatePost = onOpenCreatePost,
                        storiesByUser = storiesByUser,
                        onStoryClick = { username -> activeStoryUser = username }
                    )
                }

                // Community Mission Banner
                item {
                    Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                        com.example.diplomanexus.ui.components.DeveloperCreditBanner()
                    }
                }

                // Divider
                item {
                    HorizontalDivider(color = BorderColor.copy(alpha = 0.3f), thickness = 0.5.dp)
                }

                // Create Post Input Section (New Post input block from HTML)
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 12.dp)
                            .clip(CircleShape)
                            .background(Color(0xC01B1B1E)) // glass-input
                            .border(1.dp, Color.White.copy(alpha = 0.1f), CircleShape)
                            .clickable { onOpenCreatePost() }
                            .padding(4.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth().padding(end = 12.dp)
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.08f))
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "New Post", tint = TextSecondary, modifier = Modifier.size(20.dp))
                            }
                            
                            Spacer(modifier = Modifier.width(12.dp))
                            
                            Text(
                                text = "New Post",
                                color = TextSecondary.copy(alpha = 0.6f),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Normal,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                // Verification banner
                val isVerified = currentUser?.is_verified ?: false
                if (!isVerified) {
                    item { InstaUnverifiedBanner(currentUser, onNavigateToVerify) }
                }

                // Feed Posts
                if (feedPosts.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(60.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.Photo,
                                    contentDescription = null,
                                    tint = TextTertiary,
                                    modifier = Modifier.size(64.dp)
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    "No posts yet",
                                    color = TextSecondary,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    "Be the first to share something with your campus!",
                                    color = TextTertiary,
                                    fontSize = 13.sp,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(horizontal = 32.dp).padding(top = 6.dp)
                                )
                            }
                        }
                    }
                } else {
                    if (caughtUpIndex == 0) {
                        item(key = "caught_up_banner") {
                            CaughtUpBanner()
                        }
                    }

                    feedPosts.forEachIndexed { idx, post ->
                        item(key = post.id) {
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

                        if (caughtUpIndex > 0 && idx == caughtUpIndex - 1) {
                            item(key = "caught_up_banner") {
                                CaughtUpBanner()
                            }
                        }
                    }

                    if (caughtUpIndex == -1) {
                        item(key = "caught_up_banner") {
                            CaughtUpBanner()
                        }
                    }

                    // Developer & Community Card
                    item(key = "developer_credit_card") {
                        Box(modifier = Modifier.padding(16.dp)) {
                            com.example.diplomanexus.ui.components.DeveloperCreditCard()
                        }
                    }
                }
            }
        }
    }

    // Create Post Dialog
    if (showCreatePostDialog) {
        AlertDialog(
            onDismissRequest = { 
                showCreatePostDialog = false 
                selectedImageUri = null
                selectedImageBase64 = null
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
            title = { Text("New Post", fontWeight = FontWeight.Bold, color = TextPrimary) },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    val currentGradient = gradientOptions[selectedGradientIndex]
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .then(
                                if (currentGradient != null)
                                    Modifier.background(Brush.linearGradient(currentGradient))
                                else
                                    Modifier.background(CardLightDark)
                            )
                            .padding(16.dp)
                    ) {
                        TextField(
                            value = postText,
                            onValueChange = { postText = it },
                            placeholder = {
                                Text(
                                    "Share something with the campus...",
                                    color = if (currentGradient != null) DeepDark.copy(0.6f) else TextSecondary
                                )
                            },
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent,
                                focusedTextColor = if (currentGradient != null) DeepDark else TextPrimary,
                                unfocusedTextColor = if (currentGradient != null) DeepDark else TextPrimary
                            ),
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Image preview if selected
                    if (selectedImageUri != null && selectedImageBase64 != null) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(CardLightDark)
                                .padding(4.dp)
                        ) {
                            if (selectedMediaType == "video") {
                                Box(modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(8.dp)).background(Color.Black), contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.PlayCircleOutline, contentDescription = "Video", tint = Color.White, modifier = Modifier.size(48.dp))
                                }
                            } else {
                                val b64Data = selectedImageBase64!!.substringAfter("base64,")
                                val bytes = Base64.decode(b64Data, Base64.DEFAULT)
                                val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                                if (bitmap != null) {
                                    Image(
                                        bitmap = bitmap.asImageBitmap(),
                                        contentDescription = "Selected Image",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(8.dp))
                                    )
                                }
                            }
                            IconButton(
                                onClick = {
                                    selectedImageUri = null
                                    selectedImageBase64 = null
                                },
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .size(28.dp)
                                    .background(Color.Black.copy(0.6f), CircleShape)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Remove Photo", tint = Color.White, modifier = Modifier.size(16.dp))
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    // Background gradients selection
                    Text("Background:", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        gradientOptions.forEachIndexed { index, colors ->
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .then(
                                        if (colors != null) Modifier.background(Brush.linearGradient(colors))
                                        else Modifier.background(CardLightDark)
                                    )
                                    .border(
                                        width = if (selectedGradientIndex == index) 2.dp else 1.dp,
                                        color = if (selectedGradientIndex == index) BrandOrange else BorderColor,
                                        shape = CircleShape
                                    )
                                    .clickable { selectedGradientIndex = index }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = BorderColor)
                    Spacer(modifier = Modifier.height(8.dp))

                    // Add Photo button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Add media to post", color = TextSecondary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                        IconButton(
                            onClick = { imagePickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)) }
                        ) {
                            Icon(Icons.Default.PhotoLibrary, contentDescription = "Add Media", tint = BrandOrange, modifier = Modifier.size(28.dp))
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (postText.isNotBlank() || selectedImageBase64 != null) {
                            val formatted = if (selectedGradientIndex > 0) "[G:$selectedGradientIndex]$postText" else postText
                            viewModel.createPost(formatted, selectedImageBase64, selectedMediaType) {
                                postText = ""
                                selectedGradientIndex = 0
                                selectedImageUri = null
                                selectedImageBase64 = null
                                selectedMediaType = "image"
                                showCreatePostDialog = false
                            }
                        }
                    },
                    enabled = postText.isNotBlank() || selectedImageBase64 != null,
                    colors = ButtonDefaults.buttonColors(containerColor = BrandOrange)
                ) {
                    if (isLoading) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp))
                    else Text("Share", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { 
                        showCreatePostDialog = false 
                        selectedImageUri = null
                        selectedImageBase64 = null
                    }
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

    if (showNotificationsDialog) {
        AlertDialog(
            onDismissRequest = { showNotificationsDialog = false },
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
            title = {
                Text(
                    text = "Notifications",
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    val notifications = listOf(
                        "Diploma Nexus Team liked your post" to "5m ago",
                        "Rahul Sharma sent you a message" to "10m ago",
                        "SBTET document uploaded & pending admin verification" to "1h ago",
                        "Admin posted a new announcement: Semester Registration Open" to "1d ago"
                    )
                    notifications.forEach { notif ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(CardLightDark, RoundedCornerShape(12.dp))
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = BrandOrange,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(notif.first, color = TextPrimary, fontSize = 13.sp)
                                Text(notif.second, color = TextTertiary, fontSize = 10.sp)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { showNotificationsDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = BrandOrange),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Close", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            containerColor = CardDark.copy(alpha = 0.65f),
            shape = RoundedCornerShape(24.dp)
        )
    }

    if (activeStoryUser != null) {
        val userStories = storiesByUser[activeStoryUser!!] ?: emptyList()
        StoryViewerDialog(
            username = activeStoryUser!!,
            stories = userStories,
            onDismiss = { activeStoryUser = null },
            onAddYoursClick = { prompt ->
                onOpenCreatePost()
            }
        )
    }
}

// ─── Stories Row ─────────────────────────────────────────────────────────────

@Composable
fun StoriesRow(
    currentUser: com.example.diplomanexus.api.UserDto?,
    onNavigateToVerify: () -> Unit,
    onOpenCreatePost: () -> Unit,
    storiesByUser: Map<String, List<PostDto>>,
    onStoryClick: (String) -> Unit
) {
    val isVerified = currentUser?.is_verified ?: false
    val myStories = currentUser?.let { storiesByUser[it.username] } ?: emptyList()
    val hasMyStories = myStories.isNotEmpty()

    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 14.dp),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // "Your Story" item (dashed border, add plus sign)
        item {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.clickable {
                    if (!isVerified) {
                        onNavigateToVerify()
                    } else {
                        if (hasMyStories) {
                            onStoryClick(currentUser.username)
                        } else {
                            onOpenCreatePost()
                        }
                    }
                }
            ) {
                Box(
                    modifier = Modifier
                        .size(68.dp)
                        .then(
                            if (hasMyStories) {
                                val myIsCf = myStories.any { it.content.startsWith("[CF]") }
                                val ringColor = if (myIsCf) Color(0xFF4CAF50) else Color(0xFF00B2FF)
                                Modifier
                                    .drawBehind {
                                        drawCircle(
                                            brush = Brush.radialGradient(
                                                colors = listOf(ringColor.copy(alpha = 0.4f), Color.Transparent),
                                                radius = 34.dp.toPx()
                                            ),
                                            radius = 34.dp.toPx()
                                        )
                                    }
                                    .border(
                                        width = 2.dp,
                                        color = ringColor,
                                        shape = CircleShape
                                    )
                                    .padding(3.dp)
                            } else {
                                Modifier
                                    .drawBehind {
                                        val strokeWidth = 1.5.dp.toPx()
                                        val dashLength = 5.dp.toPx()
                                        val gapLength = 3.dp.toPx()
                                        drawCircle(
                                            color = Color(0xFFFF5722).copy(alpha = 0.5f), // BrandOrange/50
                                            style = Stroke(
                                                width = strokeWidth,
                                                pathEffect = PathEffect.dashPathEffect(
                                                    intervals = floatArrayOf(dashLength, gapLength),
                                                    phase = 0f
                                                )
                                            )
                                        )
                                    }
                                    .padding(3.dp)
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        AvatarView(
                            base64 = currentUser?.profile_pic_base64,
                            name = currentUser?.student_name ?: currentUser?.username ?: "Me",
                            size = 58.dp
                        )
                    }
                    
                    if (!isVerified || !hasMyStories) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .size(20.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFFF5722))
                                .border(2.dp, DeepDark, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Add,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(12.dp)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = if (isVerified) "Your Story" else "Verify",
                    color = TextSecondary,
                    fontSize = 12.sp, // label-md
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
            }
        }

        // Other users' active stories (Cyan glow and solid border)
        val otherActiveStories = storiesByUser.filterKeys { it != currentUser?.username }
        items(otherActiveStories.keys.toList()) { username ->
            val stories = otherActiveStories[username] ?: emptyList()
            val firstStory = stories.first()
            val displayName = firstStory.student_name ?: username
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.clickable { onStoryClick(username) }
            ) {
                val otherIsCf = stories.any { it.content.startsWith("[CF]") }
                val ringColor = if (otherIsCf) Color(0xFF4CAF50) else Color(0xFF00B2FF)
                Box(
                    modifier = Modifier
                        .size(68.dp)
                        .drawBehind {
                            drawCircle(
                                brush = Brush.radialGradient(
                                    colors = listOf(ringColor.copy(alpha = 0.4f), Color.Transparent),
                                    radius = 34.dp.toPx()
                                ),
                                radius = 34.dp.toPx()
                            )
                        }
                        .border(
                            width = 2.dp,
                            color = ringColor,
                            shape = CircleShape
                        )
                        .padding(3.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                    ) {
                        AvatarView(
                            base64 = firstStory.profile_pic_base64,
                            name = displayName,
                            size = 58.dp
                        )
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = displayName,
                    color = TextPrimary,
                    fontSize = 12.sp, // label-md
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.width(68.dp),
                    textAlign = TextAlign.Center
                )
            }
        }

        // Categories / Highlight Stories with beautiful Icons (Cyan tint theme)
        val categoryHighlights = listOf(
            Triple("Campus", Icons.Default.LocationOn, "Official Campus updates"),
            Triple("Events", Icons.Default.Event, "College events and fests"),
            Triple("Results", Icons.Default.School, "SBTET results announcements"),
            Triple("Clubs", Icons.Default.Group, "Student clubs activities"),
            Triple("Sports", Icons.Default.SportsBasketball, "Campus sports matches")
        )

        items(categoryHighlights) { (label, icon, _) ->
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(68.dp)
                        .clip(CircleShape)
                        .background(CardDark)
                        .border(1.dp, BorderColor, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = label,
                        tint = Color(0xFF00B2FF), // Cyan (#00B2FF)
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = label,
                    color = TextSecondary,
                    fontSize = 12.sp, // label-md
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.width(68.dp),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

// ─── Unverified Banner ───────────────────────────────────────────────────────

@Composable
fun InstaUnverifiedBanner(currentUser: com.example.diplomanexus.api.UserDto?, onNavigateToVerify: () -> Unit) {
    val pinSubmitted = currentUser?.pin?.isNotBlank() == true
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(
                Brush.horizontalGradient(listOf(BrandOrange.copy(0.15f), AccentAmber.copy(0.1f)))
            )
            .border(1.dp, BrandOrange.copy(0.4f), RoundedCornerShape(16.dp))
            .clickable { onNavigateToVerify() }
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(BrandOrange.copy(0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (pinSubmitted) Icons.Default.Info else Icons.Default.Security, 
                    contentDescription = null, 
                    tint = BrandOrange, 
                    modifier = Modifier.size(28.dp)
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (pinSubmitted) "Verification Pending" else "Verify your student status", 
                    color = TextPrimary, 
                    fontWeight = FontWeight.Bold, 
                    fontSize = 14.sp
                )
                Text(
                    text = if (pinSubmitted) {
                        "Your verification document has been uploaded. An admin will review and verify your account shortly."
                    } else {
                        "Tap to authenticate via SBTET portal and unlock full access."
                    },
                    color = TextSecondary,
                    fontSize = 12.sp,
                    lineHeight = 17.sp,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = BrandOrange)
        }
    }
}

// ─── Instagram-style Post Card ───────────────────────────────────────────────

@Composable
fun InstaPostCard(
    post: PostDto,
    isExpanded: Boolean,
    viewModel: AppViewModel,
    onLikeClick: () -> Unit,
    onCommentClick: () -> Unit,
    onShareClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (post.media_type == "tweet") {
        TweetCard(
            post = post,
            isExpanded = isExpanded,
            viewModel = viewModel,
            onLikeClick = onLikeClick,
            onCommentClick = onCommentClick,
            onShareClick = onShareClick,
            modifier = modifier
        )
    } else {
        var isLikeAnimating by remember { mutableStateOf(false) }
        val likeScale by animateFloatAsState(
            targetValue = if (isLikeAnimating) 1.4f else 1f,
            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
            finishedListener = { isLikeAnimating = false },
            label = "likeScale"
        )
        val likeColor by animateColorAsState(
            targetValue = if (post.is_liked_by_me) Color(0xFFFF3B5C) else TextSecondary,
            label = "likeColor"
        )

        val contentText = post.content
        val hasGradient = contentText.startsWith("[G:")
        val gradients = listOf(
            listOf(BrandOrange, AccentAmber),
            listOf(BrandOrangeLight, Color(0xFFFFD54F)),
            listOf(Color(0xFF6A1B9A), Color(0xFFE91E63)),
            listOf(Color(0xFF1565C0), Color(0xFF00BCD4))
        )

        var showStitchTooltip by remember { mutableStateOf(false) }

        Card(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0x662A2A2D)),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
            ) {
                // ── Header ──────────────────────────────────────────────────────
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Avatar with circular glowing outline
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF00B2FF))
                            .padding(1.5.dp)
                    ) {
                        AvatarView(base64 = post.profile_pic_base64, name = post.student_name ?: post.username, size = 37.dp)
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = post.student_name ?: post.username,
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (post.is_verified) {
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(Icons.Default.Verified, contentDescription = null, tint = VerifiedBlue, modifier = Modifier.size(14.dp))
                            }
                            BranchTagBadge(branch = post.branch)
                        }
                        Text(text = "@${post.username}", color = TextSecondary, fontSize = 11.sp)
                    }

                    IconButton(onClick = { }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Options", tint = TextSecondary)
                    }
                }

                // ── Post Content ────────────────────────────────────────────────
                val hasMedia = post.media_url != null || post.image_base64 != null
                
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .padding(horizontal = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (hasMedia) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(1.2f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.Black)
                                .pointerInput(Unit) {
                                    detectTapGestures(
                                        onDoubleTap = {
                                            isLikeAnimating = true
                                            if (!post.is_liked_by_me) onLikeClick()
                                        }
                                    )
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            if (post.upload_status == "pending") {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    CircularProgressIndicator(color = BrandOrange)
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text("Processing Media...", color = Color.White)
                                }
                            } else if (post.media_type == "video") {
                                if (post.media_url != null) {
                                    val context = LocalContext.current
                                    val exoPlayer = remember {
                                        ExoPlayer.Builder(context).build().apply {
                                            val dataSourceFactory = MediaCacheManager.getCacheDataSourceFactory(context)
                                            val mediaItem = MediaItem.fromUri(Uri.parse(post.media_url))
                                            val mediaSource = androidx.media3.exoplayer.source.DefaultMediaSourceFactory(dataSourceFactory).createMediaSource(mediaItem)
                                            setMediaSource(mediaSource)
                                            repeatMode = ExoPlayer.REPEAT_MODE_ALL
                                            playWhenReady = true
                                            prepare()
                                        }
                                    }
                                    DisposableEffect(Unit) {
                                        onDispose {
                                            exoPlayer.release()
                                        }
                                    }
                                    AndroidView(
                                        factory = { ctx ->
                                            PlayerView(ctx).apply {
                                                player = exoPlayer
                                                useController = false
                                            }
                                        },
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                            } else {
                                if (post.media_url != null) {
                                    AsyncImage(
                                        model = post.media_url,
                                        contentDescription = "Post Image",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } else if (post.image_base64 != null) {
                                    val b64Data = post.image_base64.substringAfter("base64,")
                                    val bytes = Base64.decode(b64Data, Base64.DEFAULT)
                                    val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                                    if (bitmap != null) {
                                        Image(
                                            bitmap = bitmap.asImageBitmap(),
                                            contentDescription = "Post Image",
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }
                                }
                            }
                            
                            // Double tap heart animation overlay
                            androidx.compose.animation.AnimatedVisibility(
                                visible = isLikeAnimating,
                                enter = androidx.compose.animation.scaleIn(spring(dampingRatio = Spring.DampingRatioHighBouncy)),
                                exit = androidx.compose.animation.scaleOut(spring(dampingRatio = Spring.DampingRatioNoBouncy))
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Favorite,
                                    contentDescription = null,
                                    tint = Color.White.copy(alpha = 0.9f),
                                    modifier = Modifier.size(80.dp)
                                )
                            }
                            
                            // Collaborative Orange Tooltip Overlay (Stitch Tooltip)
                            if (showStitchTooltip) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.BottomEnd)
                                        .padding(bottom = 16.dp, end = 16.dp)
                                        .width(180.dp)
                                        .drawBehind {
                                            drawRoundRect(
                                                brush = Brush.radialGradient(
                                                    colors = listOf(Color(0xFFFF5722).copy(alpha = 0.5f), Color.Transparent),
                                                    radius = 90.dp.toPx()
                                                ),
                                                size = size
                                            )
                                        }
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color(0xFFFF5722)) // BrandOrange
                                        .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                                        .padding(horizontal = 12.dp, vertical = 8.dp)
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = "Stitch",
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "(collaborate/combine posts)",
                                            color = Color.White.copy(alpha = 0.8f),
                                            fontSize = 10.sp,
                                            textAlign = TextAlign.Center,
                                            lineHeight = 12.sp
                                        )
                                    }
                                }
                            }
                        }
                    } else if (hasGradient) {
                        val endTag = contentText.indexOf("]")
                        val gradIndex = contentText.substring(3, endTag).toIntOrNull() ?: 1
                        val textOnly = contentText.substring(endTag + 1)
                        val selectedGrad = gradients.getOrElse(gradIndex - 1) { gradients[0] }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(1.2f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Brush.linearGradient(selectedGrad)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = textOnly,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(24.dp),
                                lineHeight = 26.sp
                            )
                        }
                    } else {
                        // Plain text post (Instagram text post style)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(CardLightDark)
                                .padding(horizontal = 16.dp, vertical = 14.dp)
                        ) {
                            Text(
                                text = contentText,
                                color = TextPrimary,
                                fontSize = 14.sp,
                                lineHeight = 20.sp
                            )
                        }
                    }
                }

                // ── Action Bar ──────────────────────────────────────────────────
                val context = LocalContext.current
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Like button with icon + text
                    Row(
                        modifier = Modifier
                            .clickable {
                                isLikeAnimating = true
                                onLikeClick()
                            }
                            .padding(vertical = 4.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (post.is_liked_by_me) Icons.Default.Favorite else Icons.Outlined.FavoriteBorder,
                            contentDescription = "Like",
                            tint = likeColor,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Like", color = TextSecondary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    }
                    
                    Spacer(modifier = Modifier.width(16.dp))
                    
                    // Comment button with icon + text
                    Row(
                        modifier = Modifier
                            .clickable { onCommentClick() }
                            .padding(vertical = 4.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.ChatBubbleOutline,
                            contentDescription = "Comment",
                            tint = TextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Comment", color = TextSecondary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    }
                    
                    Spacer(modifier = Modifier.weight(1f))
                    
                    // Share Button (opens dialog / sheet)
                    Button(
                        onClick = onShareClick,
                        modifier = Modifier.height(32.dp),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, Color(0xFFFF5722)),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Transparent,
                            contentColor = Color(0xFFFF5722)
                        ),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = "Share",
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "share",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }
                }

                // ── Likes Count ─────────────────────────────────────────────────
                if (post.likes_count > 0) {
                    Text(
                        text = "${post.likes_count} ${if (post.likes_count == 1) "like" else "likes"}",
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp)
                    )
                }

                // ── Caption ─────────────────────────────────────────────────────
                if (!hasGradient && contentText.isNotBlank() && hasMedia) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = post.student_name ?: post.username,
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = contentText,
                            color = TextPrimary,
                            fontSize = 13.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // ── Inline Comment Drawer ───────────────────────────────────────────────
                if (isExpanded) {
                    InlineCommentDrawer(post = post, viewModel = viewModel)
                } else {
                    val commentsCount = post.comments.size
                    Text(
                        text = if (commentsCount > 0) "View all $commentsCount comments" else "Add a comment...",
                        color = TextSecondary,
                        fontSize = 12.sp,
                        modifier = Modifier
                            .padding(horizontal = 16.dp, vertical = 4.dp)
                            .clickable { onCommentClick() }
                    )
                }
            }
        }
    }
}

// ─── Comment Item ─────────────────────────────────────────────────────────────

@Composable
fun CommentItem(comment: CommentDto, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(BrandOrange.copy(0.3f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = (comment.student_name ?: comment.username).take(1).uppercase(),
                color = BrandOrange,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp
            )
        }
        Spacer(modifier = Modifier.width(10.dp))
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = comment.student_name ?: comment.username,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
                if (comment.is_verified) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(Icons.Default.Verified, contentDescription = null, tint = VerifiedBlue, modifier = Modifier.size(11.dp))
                }
            }
            Text(text = comment.content, color = TextSecondary, fontSize = 12.sp)
        }
    }
}

// ─── Avatar View ─────────────────────────────────────────────────────────────

@Composable
fun AvatarView(
    base64: String?,
    name: String,
    size: Dp,
    modifier: Modifier = Modifier
) {
    val bitmap = remember(base64) {
        if (!base64.isNullOrBlank()) {
            try {
                val clean = if (base64.contains(",")) base64.split(",")[1] else base64
                val bytes = Base64.decode(clean, Base64.DEFAULT)
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            } catch (e: Exception) { null }
        } else null
    }

    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(Brush.linearGradient(listOf(BrandOrange, AccentAmber))),
        contentAlignment = Alignment.Center
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = "Avatar",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize().clip(CircleShape)
            )
        } else {
            Text(
                text = name.take(1).uppercase(),
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = (size.value * 0.45).sp
            )
        }
    }
}

// ─── Twitter-style Card ──────────────────────────────────────────────────────

@Composable
fun TweetCard(
    post: PostDto,
    isExpanded: Boolean,
    viewModel: AppViewModel,
    onLikeClick: () -> Unit,
    onCommentClick: () -> Unit,
    onShareClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isLikeAnimating by remember { mutableStateOf(false) }
    val likeScale by animateFloatAsState(
        targetValue = if (isLikeAnimating) 1.4f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        finishedListener = { isLikeAnimating = false },
        label = "likeScale"
    )
    val likeColor by animateColorAsState(
        targetValue = if (post.is_liked_by_me) Color(0xFFFF3B5C) else TextSecondary,
        label = "likeColor"
    )
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = CardDark.copy(alpha = 0.45f)),
        border = BorderStroke(
            width = 1.dp,
            brush = Brush.linearGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.18f),
                    Color.White.copy(alpha = 0.02f)
                )
            )
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            AvatarView(
                base64 = post.profile_pic_base64,
                name = post.student_name ?: post.username,
                size = 40.dp
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = post.student_name ?: post.username,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (post.is_verified) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Default.Verified,
                            contentDescription = null,
                            tint = VerifiedBlue,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                    BranchTagBadge(branch = post.branch)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "@${post.username}",
                        color = TextSecondary,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Text(
                        text = " · ${getRelativeTimeString(post.created_at)}",
                        color = TextSecondary,
                        fontSize = 12.sp,
                        maxLines = 1
                    )
                }
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = post.content,
                    color = TextPrimary,
                    fontSize = 15.sp,
                    lineHeight = 20.sp
                )
                
                val hasMedia = post.media_url != null || post.image_base64 != null
                if (hasMedia) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.Black)
                    ) {
                        if (post.media_url != null) {
                            AsyncImage(
                                model = post.media_url,
                                contentDescription = "Tweet Attachment",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else if (post.image_base64 != null) {
                            val b64Data = post.image_base64.substringAfter("base64,")
                            val bytes = Base64.decode(b64Data, Base64.DEFAULT)
                            val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                            if (bitmap != null) {
                                Image(
                                    bitmap = bitmap.asImageBitmap(),
                                    contentDescription = "Tweet Attachment",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable(onClick = onCommentClick)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.ChatBubbleOutline,
                            contentDescription = "Reply",
                            tint = TextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                        if (post.comments.isNotEmpty()) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = post.comments.size.toString(),
                                color = TextSecondary,
                                fontSize = 12.sp
                            )
                        }
                    }
                    
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable {
                            isLikeAnimating = true
                            onLikeClick()
                        }
                    ) {
                        Icon(
                            imageVector = if (post.is_liked_by_me) Icons.Default.Favorite else Icons.Outlined.FavoriteBorder,
                            contentDescription = "Like",
                            tint = likeColor,
                            modifier = Modifier.size(18.dp).scale(likeScale)
                        )
                        if (post.likes_count > 0) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = post.likes_count.toString(),
                                color = if (post.is_liked_by_me) Color(0xFFFF3B5C) else TextSecondary,
                                fontSize = 12.sp
                            )
                        }
                    }
                    
                    val context = LocalContext.current
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Share",
                        tint = TextSecondary,
                        modifier = Modifier
                            .size(18.dp)
                            .clickable { onShareClick() }
                    )
                    
                    Spacer(modifier = Modifier.width(20.dp))
                }

                if (isExpanded) {
                    InlineCommentDrawer(post = post, viewModel = viewModel)
                }
            }
        }
    }
}



// ─── Helpers ─────────────────────────────────────────────────────────────────

private fun isWithin24Hours(createdAtStr: String): Boolean {
    return try {
        val instant = java.time.Instant.parse(createdAtStr)
        val limit = java.time.Instant.now().minus(24, java.time.temporal.ChronoUnit.HOURS)
        instant.isAfter(limit)
    } catch (e: Exception) {
        true
    }
}

private fun getRelativeTimeString(createdAtStr: String): String {
    return try {
        val instant = java.time.Instant.parse(createdAtStr)
        val now = java.time.Instant.now()
        val diffSeconds = java.time.Duration.between(instant, now).seconds
        when {
            diffSeconds < 60 -> "Just now"
            diffSeconds < 3600 -> "${diffSeconds / 60}m ago"
            diffSeconds < 86400 -> "${diffSeconds / 3600}h ago"
            else -> "${diffSeconds / 86400}d ago"
        }
    } catch (e: Exception) {
        ""
    }
}

@Composable
fun CaughtUpBanner(modifier: Modifier = Modifier) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardDark),
        border = BorderStroke(
            1.dp,
            Brush.horizontalGradient(listOf(BrandOrange, AccentAmber))
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(BrandOrange.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = BrandOrange,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = "You're all caught up",
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "You've seen all posts from your community.",
                    color = TextSecondary,
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
fun InlineCommentDrawer(
    post: PostDto,
    viewModel: AppViewModel,
    modifier: Modifier = Modifier
) {
    var newCommentText by remember { mutableStateOf("") }
    var replyingToComment by remember { mutableStateOf<com.example.diplomanexus.api.CommentDto?>(null) }

    val parsedComments = remember(post.comments) {
        post.comments.map { comment ->
            if (comment.content.startsWith("[REPLY:")) {
                try {
                    val idEnd = comment.content.indexOf(']')
                    val parentId = comment.content.substring(7, idEnd).toInt()
                    val mentionEnd = comment.content.indexOf(' ', idEnd + 1)
                    val cleanContent = if (mentionEnd != -1) comment.content.substring(mentionEnd + 1) else comment.content.substring(idEnd + 1)
                    Triple(comment, parentId, cleanContent)
                } catch (e: Exception) {
                    Triple(comment, null, comment.content)
                }
            } else {
                Triple(comment, null, comment.content)
            }
        }
    }

    val baseComments = remember(parsedComments) { parsedComments.filter { it.second == null } }
    val replies = remember(parsedComments) { parsedComments.filter { it.second != null } }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .background(Color.White.copy(0.01f))
            .padding(8.dp)
    ) {
        if (baseComments.isEmpty()) {
            Text(
                text = "No comments yet. Be the first to share your thoughts!",
                color = TextSecondary,
                fontSize = 11.sp,
                modifier = Modifier.padding(vertical = 12.dp)
            )
        } else {
            baseComments.forEach { (comment, _, cleanContent) ->
                Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Top
                    ) {
                        AvatarView(base64 = null, name = comment.student_name ?: comment.username, size = 26.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(comment.student_name ?: comment.username, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                if (comment.is_verified) {
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(Icons.Default.Verified, contentDescription = null, tint = VerifiedBlue, modifier = Modifier.size(12.dp))
                                }
                                BranchTagBadge(branch = comment.branch)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(comment.created_at.take(10), color = TextSecondary, fontSize = 9.sp)
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(cleanContent, color = TextPrimary, fontSize = 12.sp)
                            
                            Text(
                                text = "Reply",
                                color = BrandOrange,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .padding(vertical = 4.dp)
                                    .clickable { replyingToComment = comment }
                            )
                        }
                    }

                    val commentReplies = replies.filter { it.second == comment.id }
                    if (commentReplies.isNotEmpty()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 24.dp)
                        ) {
                            commentReplies.forEach { (reply, _, cleanReplyText) ->
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .width(1.dp)
                                            .height(32.dp)
                                            .background(Color.White.copy(0.15f))
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    AvatarView(base64 = null, name = reply.student_name ?: reply.username, size = 20.dp)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(reply.student_name ?: reply.username, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                            if (reply.is_verified) {
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Icon(Icons.Default.Verified, contentDescription = null, tint = VerifiedBlue, modifier = Modifier.size(10.dp))
                                            }
                                            BranchTagBadge(branch = reply.branch)
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(reply.created_at.take(10), color = TextSecondary, fontSize = 8.sp)
                                        }
                                        Spacer(modifier = Modifier.height(1.dp))
                                        Text(cleanReplyText, color = TextPrimary, fontSize = 11.sp)
                                        
                                        Text(
                                            text = "Reply",
                                            color = BrandOrange,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier
                                                .padding(vertical = 2.dp)
                                                .clickable { replyingToComment = comment }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (replyingToComment != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(BrandOrange.copy(0.1f))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Replying to @${replyingToComment!!.username}", color = BrandOrange, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                IconButton(onClick = { replyingToComment = null }, modifier = Modifier.size(16.dp)) {
                    Icon(Icons.Default.Close, contentDescription = null, tint = BrandOrange, modifier = Modifier.size(12.dp))
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = newCommentText,
                onValueChange = { newCommentText = it },
                placeholder = { Text("Write comment...", color = TextSecondary, fontSize = 12.sp) },
                singleLine = true,
                shape = RoundedCornerShape(20.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    focusedBorderColor = BrandOrange,
                    unfocusedBorderColor = BorderColor,
                    focusedContainerColor = CardDark.copy(0.4f),
                    unfocusedContainerColor = CardDark.copy(0.2f)
                ),
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 8.dp)
            )
            IconButton(
                onClick = {
                    if (newCommentText.isNotBlank()) {
                        val finalMsg = if (replyingToComment != null) {
                            "[REPLY:${replyingToComment!!.id}]@${replyingToComment!!.username} ${newCommentText.trim()}"
                        } else {
                            newCommentText.trim()
                        }
                        viewModel.addComment(post.id, finalMsg)
                        newCommentText = ""
                        replyingToComment = null
                    }
                },
                enabled = newCommentText.isNotBlank(),
                colors = IconButtonDefaults.iconButtonColors(contentColor = BrandOrange)
            ) {
                Icon(Icons.Default.Send, contentDescription = "Send", modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
fun BranchTagBadge(branch: String?, modifier: Modifier = Modifier) {
    val abbr = remember(branch) {
        if (branch == null) return@remember null
        val b = branch.lowercase().trim()
        when {
            b.contains("cyber physical") || b.contains("cps") -> "CPS"
            b.contains("artificial intelligence") || b.contains("aiml") || b.contains("ai") -> "AI"
            b.contains("computer") || b.contains("cm") || b.contains("cse") -> "CM"
            b.contains("electronics") || b.contains("ece") || b.contains("ec") -> "EC"
            b.contains("electrical") || b.contains("eee") || b.contains("ee") -> "EE"
            b.contains("mechanical") || b.contains("me") -> "ME"
            b.contains("civil") -> "CE"
            else -> null
        }
    }

    if (abbr != null) {
        Box(
            modifier = modifier
                .padding(start = 6.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(BrandOrange.copy(alpha = 0.15f))
                .border(0.5.dp, BrandOrange.copy(alpha = 0.3f), RoundedCornerShape(4.dp))
                .padding(horizontal = 4.dp, vertical = 1.5.dp)
        ) {
            Text(
                text = abbr,
                color = BrandOrange,
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            )
        }
    }
}
