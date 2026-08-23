package com.example.diplomanexus.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.diplomanexus.api.ConversationDto
import com.example.diplomanexus.api.MessageDto
import com.example.diplomanexus.api.PostDto
import com.example.diplomanexus.api.CommentDto
import com.example.diplomanexus.theme.*
import com.example.diplomanexus.viewmodel.AppViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.foundation.Image
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.BorderStroke

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessagesScreen(
    viewModel: AppViewModel,
    modifier: Modifier = Modifier
) {
    val conversations by viewModel.conversations.collectAsState()
    val activeMessages by viewModel.activeMessages.collectAsState()
    val activeConversationId by viewModel.activeConversationId.collectAsState()
    val onlineUsers by viewModel.onlineUsers.collectAsState()
    val typingUsers by viewModel.typingUsers.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val pendingUserId by viewModel.pendingOpenConversationUserId.collectAsState()

    var activeConversation by remember { mutableStateOf<ConversationDto?>(null) }
    val posts by viewModel.posts.collectAsState()
    var activePostForDetail by remember { mutableStateOf<PostDto?>(null) }

    // Fetch conversations when screen appears
    LaunchedEffect(Unit) {
        viewModel.fetchConversations()
    }

    // Handle pending conversation open from SearchScreen
    LaunchedEffect(pendingUserId) {
        val userId = pendingUserId ?: return@LaunchedEffect
        viewModel.openConversation(userId) { roomId ->
            activeConversation = conversations.find { it.id == roomId }
                ?: ConversationDto(
                    id = roomId,
                    other_user_id = userId,
                    other_username = "",
                    other_student_name = null,
                    other_profile_pic_base64 = null,
                    other_is_verified = false,
                    last_message = null,
                    last_message_time = null,
                    last_message_type = null,
                    unread_count = 0
                )
        }
        viewModel.clearPendingConversation()
    }

    // Sync activeConversation with conversations list
    LaunchedEffect(conversations, activeConversationId) {
        if (activeConversationId != null && activeConversation == null) {
            activeConversation = conversations.find { it.id == activeConversationId }
        }
    }

    if (activeConversation != null) {
        ChatView(
            conversation = activeConversation!!,
            messages = activeMessages,
            currentUserId = currentUser?.id ?: 0,
            isOtherOnline = onlineUsers.contains(activeConversation!!.other_user_id),
            isOtherTyping = typingUsers[activeConversation!!.id]?.isNotEmpty() == true,
            posts = posts,
            onPostClick = { activePostForDetail = it },
            onBack = {
                viewModel.setActiveConversation(null)
                activeConversation = null
                viewModel.fetchConversations()
            },
            onSendMessage = { content ->
                viewModel.sendMessage(activeConversation!!.id, content)
            },
            onTyping = { viewModel.handleTyping(activeConversation!!.id) },
            onStopTyping = { viewModel.handleStopTyping(activeConversation!!.id) },
            onLoadMore = {
                val oldest = activeMessages.firstOrNull()
                if (oldest != null) {
                    viewModel.fetchMessages(activeConversation!!.id, oldest.id)
                }
            },
            modifier = modifier
        )
    } else {
        ConversationListView(
            viewModel = viewModel,
            conversations = conversations,
            onlineUsers = onlineUsers,
            onConversationClick = { conv ->
                activeConversation = conv
                viewModel.setActiveConversation(conv.id)
            },
            modifier = modifier
        )
    }

    if (activePostForDetail != null) {
        val post = activePostForDetail!!
        var newCommentText by remember { mutableStateOf("") }
        val commentsList = remember(posts) {
            posts.find { it.id == post.id }?.comments ?: emptyList()
        }

        AlertDialog(
            onDismissRequest = { activePostForDetail = null },
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
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    AvatarView(
                        base64 = post.profile_pic_base64,
                        name = post.student_name ?: post.username,
                        size = 32.dp
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = post.student_name ?: post.username,
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            if (post.is_verified) {
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    imageVector = Icons.Default.Verified,
                                    contentDescription = "Verified",
                                    tint = BrandOrange,
                                    modifier = Modifier.size(13.dp)
                                )
                            }
                        }
                        Text(text = "@${post.username}", color = TextSecondary, fontSize = 11.sp)
                    }
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(380.dp)
                ) {
                    val displayContent = if (post.content.startsWith("[G:")) {
                        if (post.content.contains("]")) post.content.split("]")[1] else ""
                    } else post.content
                    
                    Text(
                        text = displayContent,
                        color = TextPrimary,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    if (post.media_type != "tweet" && !post.image_base64.isNullOrBlank()) {
                        val postImage = remember(post.image_base64) {
                            try {
                                val clean = if (post.image_base64.contains(",")) post.image_base64.split(",")[1] else post.image_base64
                                val bytes = Base64.decode(clean, Base64.DEFAULT)
                                BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
                            } catch (e: Exception) { null }
                        }
                        if (postImage != null) {
                            Image(
                                bitmap = postImage,
                                contentDescription = "Post Image",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(110.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .padding(bottom = 8.dp)
                            )
                        }
                    }

                    HorizontalDivider(color = BorderColor, modifier = Modifier.padding(vertical = 6.dp))

                    Text(
                        text = "Comments (${commentsList.size})",
                        color = TextSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )

                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (commentsList.isEmpty()) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("No comments yet.", color = TextSecondary, fontSize = 12.sp)
                                }
                            }
                        } else {
                            items(commentsList) { comment ->
                                CommentItem(comment)
                            }
                        }
                    }

                    HorizontalDivider(color = BorderColor, modifier = Modifier.padding(vertical = 8.dp))

                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = newCommentText,
                            onValueChange = { newCommentText = it },
                            placeholder = { Text("Add a comment…", color = TextSecondary, fontSize = 12.sp) },
                            singleLine = true,
                            shape = RoundedCornerShape(24.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary,
                                focusedBorderColor = BrandOrange,
                                unfocusedBorderColor = BorderColor
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = 8.dp)
                        )
                        IconButton(
                            onClick = {
                                if (newCommentText.isNotBlank()) {
                                    viewModel.addComment(post.id, newCommentText.trim())
                                    newCommentText = ""
                                }
                            },
                            enabled = newCommentText.isNotBlank(),
                            colors = IconButtonDefaults.iconButtonColors(contentColor = BrandOrange),
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.Default.Send, contentDescription = "Comment", modifier = Modifier.size(16.dp))
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { activePostForDetail = null }) {
                    Text("Close", color = BrandOrange)
                }
            },
            containerColor = CardDark.copy(alpha = 0.65f),
            shape = RoundedCornerShape(24.dp)
        )
    }
}

// ─── Conversation List View ────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ConversationListView(
    viewModel: AppViewModel,
    conversations: List<ConversationDto>,
    onlineUsers: Set<Int>,
    onConversationClick: (ConversationDto) -> Unit,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    val searchResults by viewModel.searchResults.collectAsState()

    val filteredConversations = remember(conversations, searchQuery) {
        if (searchQuery.isBlank()) {
            conversations
        } else {
            conversations.filter {
                it.other_username.contains(searchQuery, ignoreCase = true) ||
                (it.other_student_name?.contains(searchQuery, ignoreCase = true) == true)
            }
        }
    }

    LaunchedEffect(searchQuery, filteredConversations) {
        if (searchQuery.isNotBlank() && filteredConversations.isEmpty()) {
            viewModel.searchUsers(searchQuery.trim())
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Direct Messages", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 20.sp) },
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
            // Search Input Field
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search chats or find new students...", color = TextTertiary, fontSize = 14.sp) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = TextSecondary, modifier = Modifier.size(20.dp)) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear", tint = TextSecondary, modifier = Modifier.size(20.dp))
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
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )

            // Content Area
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                if (searchQuery.isBlank()) {
                    if (conversations.isEmpty()) {
                        // Empty state when no conversations exist
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Default.ChatBubbleOutline,
                                contentDescription = null,
                                tint = TextTertiary,
                                modifier = Modifier.size(64.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("No conversations yet", color = TextSecondary, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Type in the search bar above to message someone!", color = TextTertiary, fontSize = 13.sp)
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp, vertical = 4.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(conversations, key = { it.id }) { conversation ->
                                ConversationCard(
                                    conversation = conversation,
                                    isOnline = onlineUsers.contains(conversation.other_user_id),
                                    onClick = { onConversationClick(conversation) }
                                )
                            }
                        }
                    }
                } else {
                    // Search mode
                    if (filteredConversations.isNotEmpty()) {
                        // Show filtered local DMs
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp, vertical = 4.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(filteredConversations, key = { it.id }) { conversation ->
                                ConversationCard(
                                    conversation = conversation,
                                    isOnline = onlineUsers.contains(conversation.other_user_id),
                                    onClick = { onConversationClick(conversation) }
                                )
                            }
                        }
                    } else {
                        // Fallback: No matching DMs found. Show campus search results!
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp)
                        ) {
                            Text(
                                text = "No matching chats. Searching campus...",
                                color = TextTertiary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )

                            if (searchResults.isEmpty()) {
                                Box(
                                    modifier = Modifier.weight(1f).fillMaxWidth(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "No students found on campus matching \"$searchQuery\"",
                                        color = TextSecondary,
                                        fontSize = 13.sp
                                    )
                                }
                            } else {
                                LazyColumn(
                                    modifier = Modifier.weight(1f).fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    items(searchResults) { student ->
                                        // A simple, very premium card for user results
                                        Card(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    viewModel.openConversation(student.id) { roomId ->
                                                        val newConv = ConversationDto(
                                                            id = roomId,
                                                            other_user_id = student.id,
                                                            other_username = student.username,
                                                            other_student_name = student.student_name,
                                                            other_profile_pic_base64 = student.profile_pic_base64,
                                                            other_is_verified = student.is_verified,
                                                            last_message = null,
                                                            last_message_time = null,
                                                            last_message_type = null,
                                                            unread_count = 0
                                                        )
                                                        onConversationClick(newConv)
                                                    }
                                                },
                                            shape = RoundedCornerShape(14.dp),
                                            colors = CardDefaults.cardColors(containerColor = CardDark),
                                            border = CardDefaults.outlinedCardBorder().copy(
                                                brush = Brush.horizontalGradient(listOf(BorderColor, BorderColor))
                                            )
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(12.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                // Avatar
                                                Box(
                                                    modifier = Modifier
                                                        .size(40.dp)
                                                        .clip(CircleShape)
                                                        .background(Brush.linearGradient(listOf(BrandOrange, AccentAmber))),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    AvatarView(
                                                        base64 = student.profile_pic_base64,
                                                        name = student.student_name ?: student.username,
                                                        size = 38.dp
                                                    )
                                                }

                                                Spacer(modifier = Modifier.width(12.dp))

                                                Column(modifier = Modifier.weight(1f)) {
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        Text(
                                                            text = student.student_name ?: student.username,
                                                            color = TextPrimary,
                                                            fontWeight = FontWeight.Bold,
                                                            fontSize = 14.sp
                                                        )
                                                        if (student.is_verified) {
                                                            Spacer(modifier = Modifier.width(4.dp))
                                                            Icon(
                                                                imageVector = Icons.Default.Verified,
                                                                contentDescription = "Verified",
                                                                tint = VerifiedBlue,
                                                                modifier = Modifier.size(14.dp)
                                                            )
                                                        }
                                                    }
                                                    Text(
                                                        text = "@${student.username} • ${student.branch ?: "Student"}",
                                                        color = TextSecondary,
                                                        fontSize = 11.sp
                                                    )
                                                }

                                                Icon(
                                                    imageVector = Icons.AutoMirrored.Filled.Send,
                                                    contentDescription = "Chat",
                                                    tint = BrandOrange,
                                                    modifier = Modifier.size(18.dp)
                                                )
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
}

@Composable
private fun ConversationCard(
    conversation: ConversationDto,
    isOnline: Boolean,
    onClick: () -> Unit
) {
    val displayName = conversation.other_student_name ?: conversation.other_username

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardDark),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = Brush.horizontalGradient(listOf(BorderColor, BorderColor))
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar with online indicator
            Box(contentAlignment = Alignment.Center) {
                AvatarView(
                    base64 = conversation.other_profile_pic_base64,
                    name = displayName,
                    size = 52.dp
                )
                if (isOnline) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .size(14.dp)
                            .clip(CircleShape)
                            .background(DeepDark)
                            .padding(2.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape)
                                .background(AlertGreen)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        displayName,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        maxLines = 1,
                        modifier = Modifier.weight(1f)
                    )
                    if (conversation.last_message_time != null) {
                        Text(
                            formatTimeAgo(conversation.last_message_time),
                            color = TextTertiary,
                            fontSize = 11.sp
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val lastMessageText = when (conversation.last_message_type) {
                        "post" -> "Shared a post"
                        "tweet" -> "Shared a tweet"
                        else -> conversation.last_message ?: "Start a conversation"
                    }
                    Text(
                        text = lastMessageText,
                        color = if (conversation.unread_count > 0) TextPrimary else TextSecondary,
                        fontWeight = if (conversation.unread_count > 0) FontWeight.SemiBold else FontWeight.Normal,
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    if (conversation.unread_count > 0) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .size(22.dp)
                                .clip(CircleShape)
                                .background(BrandOrange),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (conversation.unread_count > 9) "9+" else conversation.unread_count.toString(),
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

// ─── Chat View ────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChatView(
    conversation: ConversationDto,
    messages: List<MessageDto>,
    currentUserId: Int,
    isOtherOnline: Boolean,
    isOtherTyping: Boolean,
    posts: List<PostDto>,
    onPostClick: (PostDto) -> Unit,
    onBack: () -> Unit,
    onSendMessage: (String) -> Unit,
    onTyping: () -> Unit,
    onStopTyping: () -> Unit,
    onLoadMore: () -> Unit,
    modifier: Modifier = Modifier
) {
    val displayName = conversation.other_student_name ?: conversation.other_username
    var inputMessage by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    // Auto-scroll to bottom when new messages arrive
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    // Detect scroll to top for pagination
    val firstVisibleItem by remember { derivedStateOf { listState.firstVisibleItemIndex } }
    LaunchedEffect(firstVisibleItem) {
        if (firstVisibleItem == 0 && messages.size >= 30) {
            onLoadMore()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(contentAlignment = Alignment.BottomEnd) {
                            AvatarView(
                                base64 = conversation.other_profile_pic_base64,
                                name = displayName,
                                size = 38.dp
                            )
                            if (isOtherOnline) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(AlertGreen)
                                        .border(1.5.dp, DeepDark, CircleShape)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(displayName, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Text(
                                text = if (isOtherTyping) "typing..." else if (isOtherOnline) "Active now" else "Offline",
                                color = if (isOtherTyping) BrandOrange else if (isOtherOnline) AlertGreen else TextSecondary,
                                fontSize = 11.sp
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color(0xFFD67F65))
                    }
                },
                actions = {
                    IconButton(onClick = { /* Info details */ }) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Details",
                            tint = Color(0xFFD67F65)
                        )
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
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF0F1115),
                            Color(0xFF160E18)
                        )
                    )
                )
                .padding(paddingValues)
        ) {
            HorizontalDivider(color = Color.White.copy(alpha = 0.05f))
            // Chat Message Log
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                var lastDayGroup = ""
                messages.forEach { msg ->
                    val parsedDate = parseIsoDate(msg.created_at)
                    if (parsedDate != null) {
                        val dayGroup = getDayGroupKey(parsedDate)
                        if (dayGroup != lastDayGroup) {
                            lastDayGroup = dayGroup
                            val timeStr = SimpleDateFormat("h:mm a", Locale.US).format(parsedDate)
                            item(key = "date_header_${msg.id}") {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "$dayGroup, $timeStr",
                                        color = Color(0xFFD67F65),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }

                    item(key = "msg_${msg.id}") {
                        val isMe = msg.sender_id == currentUserId
                        MessageBubble(
                            message = msg,
                            isMe = isMe,
                            posts = posts,
                            onPostClick = onPostClick
                        )
                    }
                }

                // Typing indicator
                if (isOtherTyping) {
                    item(key = "typing_indicator") {
                        TypingIndicator()
                    }
                }
            }

            // Bottom Message Bar
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = CardLightDark),
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = Brush.horizontalGradient(listOf(BorderColor, BorderColor))
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = inputMessage,
                        onValueChange = {
                            inputMessage = it
                            if (it.isNotBlank()) {
                                onTyping()
                            } else {
                                onStopTyping()
                            }
                        },
                        placeholder = { Text("Message...", color = TextTertiary, fontSize = 14.sp) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        maxLines = 5,
                        modifier = Modifier
                            .weight(1f)
                            .wrapContentHeight()
                            .defaultMinSize(minHeight = 40.dp)
                    )

                    if (inputMessage.isNotBlank()) {
                        IconButton(
                            onClick = {
                                val text = inputMessage.trim()
                                if (text.isNotBlank()) {
                                    inputMessage = ""
                                    onSendMessage(text)
                                    coroutineScope.launch {
                                        kotlinx.coroutines.delay(100)
                                        if (messages.isNotEmpty()) {
                                            listState.animateScrollToItem(messages.size - 1)
                                        }
                                    }
                                }
                            }
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send", tint = BrandOrange)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MessageBubble(
    message: MessageDto,
    isMe: Boolean,
    posts: List<PostDto>,
    onPostClick: (PostDto) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalAlignment = if (isMe) Alignment.End else Alignment.Start
    ) {
        Card(
            shape = RoundedCornerShape(
                topStart = 18.dp,
                topEnd = 18.dp,
                bottomStart = if (isMe) 18.dp else 4.dp,
                bottomEnd = if (isMe) 4.dp else 18.dp
            ),
            colors = CardDefaults.cardColors(
                containerColor = if (isMe) Color(0xFF5E2D20) else Color(0xFF232528)
            ),
            border = BorderStroke(
                width = 1.dp,
                color = if (isMe) Color(0xFF7A3E2F) else Color(0xFF2C2E32)
            ),
            modifier = Modifier.widthIn(max = 290.dp)
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                if (message.message_type == "post" || message.message_type == "tweet") {
                    val sharedPostId = message.text_content?.toIntOrNull()
                    val post = posts.find { it.id == sharedPostId }
                    if (post != null) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.White.copy(alpha = 0.05f))
                                .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(12.dp))
                                .clickable { onPostClick(post) }
                                .padding(10.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                AvatarView(
                                    base64 = post.profile_pic_base64,
                                    name = post.student_name ?: post.username,
                                    size = 24.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = post.student_name ?: post.username,
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        if (post.is_verified) {
                                            Spacer(modifier = Modifier.width(3.dp))
                                            Icon(
                                                imageVector = Icons.Default.Verified,
                                                contentDescription = "Verified",
                                                tint = BrandOrange,
                                                modifier = Modifier.size(11.dp)
                                            )
                                        }
                                    }
                                    Text(
                                        text = "@${post.username}",
                                        color = TextSecondary,
                                        fontSize = 10.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (post.media_type == "tweet") AccentAmber.copy(0.15f) else BrandOrange.copy(0.15f))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = if (post.media_type == "tweet") "TWEET" else "POST",
                                        color = if (post.media_type == "tweet") AccentAmber else BrandOrange,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 8.sp
                                    )
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(6.dp))
                            
                            val displayContent = if (post.content.startsWith("[G:")) {
                                if (post.content.contains("]")) post.content.split("]")[1] else ""
                            } else post.content

                            Text(
                                text = displayContent,
                                color = Color.White.copy(alpha = 0.9f),
                                fontSize = 12.sp,
                                maxLines = 3,
                                overflow = TextOverflow.Ellipsis
                            )
                            
                            if (post.media_type != "tweet" && !post.image_base64.isNullOrBlank()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                val postImage = remember(post.image_base64) {
                                    try {
                                        val clean = if (post.image_base64.contains(",")) post.image_base64.split(",")[1] else post.image_base64
                                        val bytes = Base64.decode(clean, Base64.DEFAULT)
                                        BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
                                    } catch (e: Exception) { null }
                                }
                                if (postImage != null) {
                                    Image(
                                        bitmap = postImage,
                                        contentDescription = "Preview",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(80.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                    )
                                }
                            }
                        }
                    } else {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.White.copy(alpha = 0.03f))
                                .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
                                .clickable { }
                                .padding(10.dp)
                        ) {
                            Icon(Icons.Default.Link, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Shared Post (Tap to load)",
                                color = TextSecondary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                } else if (!message.media_url.isNullOrBlank()) {
                    val imageBitmap = remember(message.media_url) {
                        try {
                            val clean = if (message.media_url.contains(",")) message.media_url.split(",")[1] else message.media_url
                            val bytes = Base64.decode(clean, Base64.DEFAULT)
                            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
                        } catch (e: Exception) { null }
                    }
                    if (imageBitmap != null) {
                        Image(
                            bitmap = imageBitmap,
                            contentDescription = "Shared Media",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(150.dp)
                                .clip(RoundedCornerShape(12.dp))
                        )
                    }
                    if (!message.text_content.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = message.text_content,
                            color = Color.White,
                            fontSize = 14.sp
                        )
                    }
                } else {
                    Text(
                        text = message.text_content ?: "",
                        color = Color.White,
                        fontSize = 14.sp
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(2.dp))
        
        Row(
            modifier = Modifier.padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = formatMessageTime(message.created_at),
                color = if (isMe) Color(0xFFD67F65) else Color(0xFF8E9196),
                fontSize = 10.sp
            )
            if (isMe) {
                Text(
                    text = if (message.is_read) "✓✓" else "✓",
                    color = if (message.is_read) Color(0xFFD67F65) else Color(0xFF8E9196).copy(alpha = 0.5f),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun TypingIndicator() {
    val infiniteTransition = rememberInfiniteTransition(label = "typing")
    val dot1Alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(600), RepeatMode.Reverse),
        label = "dot1"
    )
    val dot2Alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(600, delayMillis = 200), RepeatMode.Reverse),
        label = "dot2"
    )
    val dot3Alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(600, delayMillis = 400), RepeatMode.Reverse),
        label = "dot3"
    )

    Row(
        modifier = Modifier.padding(start = 8.dp, top = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        listOf(dot1Alpha, dot2Alpha, dot3Alpha).forEach { alpha ->
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(TextTertiary.copy(alpha = alpha))
            )
        }
    }
}

// ─── Time Formatting Helpers ──────────────────────────────────────

private fun formatTimeAgo(timestamp: String): String {
    return try {
        val formats = listOf(
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US),
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSS", Locale.US),
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
        )
        formats.forEach { it.timeZone = TimeZone.getTimeZone("UTC") }

        var date: Date? = null
        for (fmt in formats) {
            try { date = fmt.parse(timestamp); break } catch (_: Exception) {}
        }
        if (date == null) return ""

        val diff = System.currentTimeMillis() - date.time
        val minutes = diff / 60000
        val hours = minutes / 60
        val days = hours / 24

        when {
            minutes < 1 -> "now"
            minutes < 60 -> "${minutes}m"
            hours < 24 -> "${hours}h"
            days < 7 -> "${days}d"
            else -> SimpleDateFormat("MMM d", Locale.US).format(date)
        }
    } catch (e: Exception) { "" }
}

private fun formatMessageTime(timestamp: String): String {
    return try {
        val formats = listOf(
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US),
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSS", Locale.US),
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
        )
        formats.forEach { it.timeZone = TimeZone.getTimeZone("UTC") }

        var date: Date? = null
        for (fmt in formats) {
            try { date = fmt.parse(timestamp); break } catch (_: Exception) {}
        }
        if (date == null) return ""

        val localFmt = SimpleDateFormat("h:mm a", Locale.US)
        localFmt.format(date)
    } catch (e: Exception) { "" }
}

private fun parseIsoDate(timestamp: String): Date? {
    try {
        val formats = listOf(
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US),
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSS", Locale.US),
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
        )
        formats.forEach { it.timeZone = TimeZone.getTimeZone("UTC") }
        for (fmt in formats) {
            try { return fmt.parse(timestamp) } catch (_: Exception) {}
        }
    } catch (_: Exception) {}
    return null
}

private fun getDayGroupKey(date: Date): String {
    val today = Calendar.getInstance()
    val target = Calendar.getInstance().apply { time = date }
    return when {
        today.get(Calendar.YEAR) == target.get(Calendar.YEAR) &&
        today.get(Calendar.DAY_OF_YEAR) == target.get(Calendar.DAY_OF_YEAR) -> "Today"
        today.get(Calendar.YEAR) == target.get(Calendar.YEAR) &&
        today.get(Calendar.DAY_OF_YEAR) - target.get(Calendar.DAY_OF_YEAR) == 1 -> "Yesterday"
        else -> SimpleDateFormat("MMMM d, yyyy", Locale.US).format(date)
    }
}
