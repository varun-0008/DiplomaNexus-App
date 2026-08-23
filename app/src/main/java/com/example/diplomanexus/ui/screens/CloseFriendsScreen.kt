package com.example.diplomanexus.ui.screens

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.diplomanexus.theme.*
import com.example.diplomanexus.viewmodel.AppViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CloseFriendsScreen(
    viewModel: AppViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val conversations by viewModel.conversations.collectAsState()
    val searchResults by viewModel.searchResults.collectAsState()
    val closeFriends by viewModel.closeFriends.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()

    var searchQuery by remember { mutableStateOf("") }

    val filteredUsers = remember(conversations, searchResults, searchQuery) {
        if (searchQuery.isBlank()) {
            conversations.map {
                com.example.diplomanexus.api.UserDto(
                    id = it.other_user_id,
                    username = it.other_username,
                    pin = null,
                    student_name = it.other_student_name,
                    branch = null,
                    college_name = null,
                    mobile_number = null,
                    is_verified = it.other_is_verified,
                    about_me = null,
                    profile_pic_base64 = it.other_profile_pic_base64,
                    subscription_tier = "free"
                )
            }
        } else {
            searchResults.filter { it.id != currentUser?.id }
        }
    }

    LaunchedEffect(searchQuery) {
        if (searchQuery.isNotBlank()) {
            viewModel.searchUsers(searchQuery)
        }
    }

    androidx.activity.compose.BackHandler(onBack = onBack)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Close Friends List ⭐️", fontWeight = FontWeight.Bold, color = TextPrimary) },
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
                .padding(16.dp)
        ) {
            Text(
                text = "Close Friends will get a green ring border on stories and posts marked as [CF] exclusive.",
                color = TextSecondary,
                fontSize = 12.sp,
                lineHeight = 16.sp,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search campus friends...", color = TextSecondary, fontSize = 13.sp) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(18.dp)) },
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    focusedBorderColor = BrandOrange,
                    unfocusedBorderColor = BorderColor,
                    focusedContainerColor = CardDark.copy(0.3f),
                    unfocusedContainerColor = CardDark.copy(0.1f)
                ),
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
            )

            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (filteredUsers.isEmpty()) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp), contentAlignment = Alignment.Center) {
                            Text("No friends found.", color = TextSecondary, fontSize = 13.sp)
                        }
                    }
                } else {
                    items(filteredUsers) { user ->
                        val isCF = closeFriends.contains(user.id)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color.White.copy(alpha = 0.03f))
                                .border(
                                    1.dp,
                                    if (isCF) Color(0xFF4CAF50).copy(0.3f) else Color.White.copy(0.06f),
                                    RoundedCornerShape(14.dp)
                                )
                                .clickable { viewModel.toggleCloseFriend(user.id) }
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AvatarView(
                                base64 = user.profile_pic_base64,
                                name = user.student_name ?: user.username,
                                size = 36.dp
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = user.student_name ?: user.username,
                                    color = TextPrimary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = "@${user.username}",
                                    color = TextSecondary,
                                    fontSize = 11.sp
                                )
                            }
                            IconButton(onClick = { viewModel.toggleCloseFriend(user.id) }) {
                                Icon(
                                    imageVector = if (isCF) Icons.Default.Star else Icons.Default.StarBorder,
                                    contentDescription = "Toggle Close Friend",
                                    tint = if (isCF) Color(0xFF4CAF50) else TextTertiary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
