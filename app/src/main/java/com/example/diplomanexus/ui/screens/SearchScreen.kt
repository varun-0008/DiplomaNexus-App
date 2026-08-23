package com.example.diplomanexus.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.platform.LocalContext
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
import com.example.diplomanexus.api.UserDto
import com.example.diplomanexus.api.PostDto
import com.example.diplomanexus.api.CommentDto
import com.example.diplomanexus.theme.*
import com.example.diplomanexus.viewmodel.AppViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    viewModel: AppViewModel,
    onStartChat: (Int) -> Unit,
    onOpenNotifications: () -> Unit,
    onOpenMarketplace: () -> Unit,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedStudent by remember { mutableStateOf<UserDto?>(null) }

    val searchResults by viewModel.searchResults.collectAsState()

    // Trigger search query on database when user types
    LaunchedEffect(searchQuery) {
        viewModel.searchUsers(searchQuery.trim())
    }

    if (selectedStudent != null) {
        StudentProfileView(
            student = selectedStudent!!,
            viewModel = viewModel,
            onBack = { selectedStudent = null },
            onStudentUpdate = { updated -> selectedStudent = updated },
            onStartChat = onStartChat,
            modifier = modifier
        )
    } else {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Search Campus", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 20.sp) },
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
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(DeepDark)
                    .padding(paddingValues)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    // Search Input Field
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search by name, branch, or PIN...", color = TextTertiary) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = TextSecondary) },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Close, contentDescription = "Clear", tint = TextSecondary)
                                }
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedBorderColor = BrandOrange,
                            unfocusedBorderColor = BorderColor,
                            focusedContainerColor = CardLightDark,
                            unfocusedContainerColor = CardLightDark
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp)
                    )

                    // List of Students
                    if (searchQuery.isBlank()) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Type in the search bar above to find students on campus.",
                                color = TextSecondary,
                                textAlign = TextAlign.Center,
                                fontSize = 14.sp
                            )
                        }
                    } else if (searchResults.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No students found matching \"$searchQuery\"",
                                color = TextSecondary,
                                textAlign = TextAlign.Center,
                                fontSize = 14.sp
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            contentPadding = PaddingValues(bottom = 80.dp)
                        ) {
                            items(searchResults) { student ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { selectedStudent = student },
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDarkCardColors,
                                    border = CardDefaults.outlinedCardBorder().copy(
                                        brush = Brush.horizontalGradient(listOf(BorderColor, BorderColor))
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier.padding(16.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // Profile Avatar Placeholder
                                        Box(
                                            modifier = Modifier
                                                .size(50.dp)
                                                .clip(CircleShape)
                                                .background(Brush.linearGradient(listOf(BrandOrange, AccentAmber))),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            AvatarView(
                                                base64 = student.profile_pic_base64,
                                                name = student.student_name ?: student.username,
                                                size = 48.dp
                                            )
                                        }

                                        Spacer(modifier = Modifier.width(16.dp))

                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.Start
                                            ) {
                                                Text(
                                                    text = student.student_name ?: student.username,
                                                    color = TextPrimary,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 15.sp
                                                )
                                                if (student.is_verified) {
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Icon(
                                                        imageVector = Icons.Default.Verified,
                                                        contentDescription = "Verified",
                                                        tint = VerifiedBlue,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                }
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Box(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(8.dp))
                                                        .background(
                                                            if (student.is_verified) Color(0xFF1B5E20).copy(alpha = 0.2f)
                                                            else Color(0xFF424242).copy(alpha = 0.2f)
                                                        )
                                                        .border(
                                                            width = 0.5.dp,
                                                            color = if (student.is_verified) Color(0xFF4CAF50) else Color(0xFF757575),
                                                            shape = RoundedCornerShape(8.dp)
                                                        )
                                                        .padding(horizontal = 6.dp, vertical = 1.5.dp)
                                                ) {
                                                    Text(
                                                        text = if (student.is_verified) "Active" else "Inactive",
                                                        color = if (student.is_verified) Color(0xFF81C784) else Color(0xFFB0BEC5),
                                                        fontSize = 9.sp,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                            }
                                            Spacer(modifier = Modifier.height(2.dp))
                                            val subText = when {
                                                !student.branch.isNullOrBlank() && !student.college_name.isNullOrBlank() -> 
                                                    "${student.branch} • ${student.college_name}"
                                                !student.branch.isNullOrBlank() -> student.branch
                                                !student.college_name.isNullOrBlank() -> student.college_name
                                                else -> "Student"
                                            }
                                            Text(
                                                text = subText,
                                                color = TextSecondary,
                                                fontSize = 12.sp
                                            )
                                        }
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
}

// Visual Helper for Color scheme matching CardDark
private val CardDarkCardColors: CardColors
    @Composable
    get() = CardDefaults.cardColors(containerColor = CardDark)

// Full-screen Instagram-style Student Profile View
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentProfileView(
    student: UserDto,
    viewModel: AppViewModel,
    onBack: () -> Unit,
    onStudentUpdate: (UserDto) -> Unit,
    onStartChat: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val posts by viewModel.posts.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    val targetUserPosts = remember(posts, student) {
        posts.filter { it.username == student.username && it.media_type != "story" }
    }
    val userPostsCount = remember(targetUserPosts) {
        targetUserPosts.count { it.media_type != "tweet" }
    }
    val userTweetsCount = remember(targetUserPosts) {
        targetUserPosts.count { it.media_type == "tweet" }
    }

    var expandedPostIds by remember { mutableStateOf(emptySet<Int>()) }
    var showShareDialog by remember { mutableStateOf(false) }
    var activePostForShare by remember { mutableStateOf<PostDto?>(null) }

    LaunchedEffect(showShareDialog) {
        if (showShareDialog) {
            viewModel.fetchConversations()
        }
    }
    var isBioExpanded by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("@${student.username}", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 18.sp) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                    }
                },
                windowInsets = WindowInsets(0.dp),
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DeepDark)
            )
        },
        modifier = modifier.fillMaxSize()
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(DeepDark)
                .padding(paddingValues)
        ) {
            // ─── Top Area: Floating Glassmorphic Dashboard ───────────────────
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
                        .padding(24.dp), // p-stack-lg
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Centered Avatar with light blue capsule checkmark overlay at bottom
                    Box(
                        contentAlignment = Alignment.BottomCenter,
                        modifier = Modifier.padding(bottom = 16.dp) // mb-stack-md
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(100.dp) // w-24 h-24 is 96dp + border
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.05f))
                                .border(2.dp, Color(0xFF131316), CircleShape) // border-2 border-background (bg is #131316)
                                .drawBehind {
                                    // Custom soft blue glow shadow
                                    drawCircle(
                                        brush = Brush.radialGradient(
                                            colors = listOf(Color(0xFF0CB3FF).copy(alpha = 0.4f), Color.Transparent),
                                            radius = 60.dp.toPx()
                                        ),
                                        radius = 60.dp.toPx()
                                    )
                                }
                        ) {
                            AvatarView(
                                base64 = student.profile_pic_base64,
                                name = student.student_name ?: student.username,
                                size = 96.dp
                            )
                        }
                    }
                    
                    // Centered Name + Active status
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(bottom = 4.dp)
                    ) {
                        Text(
                            text = student.student_name ?: "Guest User",
                            color = TextPrimary,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 20.sp // headline-md (20px)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF1B5E20).copy(alpha = 0.2f))
                                .border(0.5.dp, Color(0xFF4CAF50), RoundedCornerShape(8.dp))
                                .padding(horizontal = 6.dp, vertical = 1.5.dp)
                        ) {
                            Text(
                                text = "Active",
                                color = Color(0xFF81C784),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    
                    // Centered Branch Pill
                    val branchVal = student.branch ?: "Mechanical Engineering"
                    Box(
                        modifier = Modifier
                            .padding(bottom = 24.dp) // mb-stack-lg
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.05f)) // bg-white/5
                            .border(0.5.dp, Color.White.copy(alpha = 0.1f), CircleShape) // border-white/10
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = branchVal,
                            color = Color(0xFF8BCEFF), // secondary color
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp, // label-md
                            textAlign = TextAlign.Center
                        )
                    }
                    
                    // Centered Stats Row with thin vertical dividers
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(text = userPostsCount.toString(), color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                            Text(text = "Posts", color = TextSecondary, fontSize = 12.sp)
                        }
                        
                        Box(modifier = Modifier.width(1.dp).height(32.dp).background(Color.White.copy(alpha = 0.1f)))
                        
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(text = userTweetsCount.toString(), color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                            Text(text = "Tweets", color = TextSecondary, fontSize = 12.sp)
                        }
                        
                        Box(modifier = Modifier.width(1.dp).height(32.dp).background(Color.White.copy(alpha = 0.1f)))
                        
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(text = student.followers_count.toString(), color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                            Text(text = "Following", color = TextSecondary, fontSize = 12.sp)
                        }
                        
                        Box(modifier = Modifier.width(1.dp).height(32.dp).background(Color.White.copy(alpha = 0.1f)))
                        
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(text = student.followers_count.toString(), color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                            Text(text = "Followers", color = TextSecondary, fontSize = 12.sp)
                        }
                        
                        Box(modifier = Modifier.width(1.dp).height(32.dp).background(Color.White.copy(alpha = 0.1f)))
                        
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(text = student.friends_count.toString(), color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                            Text(text = "Friends", color = TextSecondary, fontSize = 12.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp)) // mt-stack-lg
                    
                    val bioTextVal = student.about_me ?: "This student has not written a bio yet."
                    val showSeeMore = (student.about_me ?: "").length > 90

                    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)) {
                        Text(
                            text = "BIOGRAPHY",
                            color = TextSecondary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            letterSpacing = 1.sp,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        
                        Text(
                            text = bioTextVal,
                            color = TextPrimary,
                            fontSize = 14.sp,
                            lineHeight = 20.sp,
                            maxLines = if (isBioExpanded) Int.MAX_VALUE else 3,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    if (student.about_me != null) {
                                        isBioExpanded = !isBioExpanded
                                    }
                                }
                        )
                        if (showSeeMore) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (isBioExpanded) "See less" else "See more",
                                color = BrandOrange,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .clickable { isBioExpanded = !isBioExpanded }
                                    .padding(vertical = 2.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    val currentUserLocal = viewModel.currentUser.collectAsState().value
                    val isSelf = student.id == currentUserLocal?.id

                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (isSelf) {
                            Button(
                                onClick = { },
                                enabled = false,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(40.dp),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color.White.copy(alpha = 0.05f),
                                    disabledContainerColor = Color.White.copy(alpha = 0.05f)
                                ),
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Text("This is You", color = TextSecondary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }

                            Button(
                                onClick = { onStartChat(student.id) },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(40.dp),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color.White.copy(alpha = 0.05f),
                                    contentColor = TextPrimary
                                ),
                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Text("Message", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        } else {
                            Button(
                                onClick = {
                                    if (student.is_following) {
                                        viewModel.unfollowUser(student.id) {
                                            onStudentUpdate(student.copy(
                                                is_following = false,
                                                followers_count = (student.followers_count - 1).coerceAtLeast(0)
                                            ))
                                        }
                                    } else {
                                        viewModel.followUser(student.id) {
                                            onStudentUpdate(student.copy(
                                                is_following = true,
                                                followers_count = student.followers_count + 1
                                            ))
                                        }
                                    }
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(40.dp),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (student.is_following) Color.White.copy(alpha = 0.05f) else BrandOrange,
                                    contentColor = if (student.is_following) TextPrimary else Color(0xFF5F1500)
                                ),
                                border = if (student.is_following) BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)) else null,
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Text(
                                    text = if (student.is_following) "Unfollow" else "Follow",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }

                            Button(
                                onClick = { onStartChat(student.id) },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(40.dp),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color.White.copy(alpha = 0.05f),
                                    contentColor = TextPrimary
                                ),
                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Text("Message", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }

            // ─── Bottom 2/3 Area: Scrollable Feed ────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(2f)
            ) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 80.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (targetUserPosts.isNotEmpty()) {
                        item {
                            Text(
                                text = "Posts & Tweets",
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                        }

                        items(targetUserPosts, key = { it.id }) { post ->
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
                                    .padding(vertical = 80.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(Icons.Default.PhotoLibrary, contentDescription = null, tint = TextTertiary, modifier = Modifier.size(48.dp))
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text("No posts or tweets yet", color = TextSecondary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    }
                }
            }
        }
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
}

@Composable
private fun ProfileStatItemSearch(
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
            fontSize = 15.sp
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            color = TextSecondary,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium
        )
    }
}
