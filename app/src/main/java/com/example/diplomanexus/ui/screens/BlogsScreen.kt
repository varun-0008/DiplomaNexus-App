package com.example.diplomanexus.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import com.example.diplomanexus.api.BlogDto
import com.example.diplomanexus.theme.*
import com.example.diplomanexus.viewmodel.AppViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BlogsScreen(
    viewModel: AppViewModel,
    onNavigateToVerify: () -> Unit,
    modifier: Modifier = Modifier
) {
    val blogs by viewModel.blogs.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    var showCreateBlogDialog by remember { mutableStateOf(false) }
    var blogTitle by remember { mutableStateOf("") }
    var blogContent by remember { mutableStateOf("") }

    // Active reading blog
    var activeBlogForReading by remember { mutableStateOf<BlogDto?>(null) }
    var showReadDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.fetchBlogs()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Campus Articles",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 20.sp,
                        color = TextPrimary
                    )
                },
                windowInsets = WindowInsets(0.dp),
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DeepDark),
                actions = {
                    IconButton(onClick = { viewModel.fetchBlogs() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh Blogs", tint = ElectricBlue)
                    }
                }
            )
        },
        floatingActionButton = {
            val isVerified = currentUser?.is_verified ?: false
            if (isVerified) {
                FloatingActionButton(
                    onClick = { showCreateBlogDialog = true },
                    containerColor = ElectricBlue,
                    contentColor = DeepDark,
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.Default.Edit, contentDescription = "Write Article")
                }
            }
        },
        modifier = modifier.fillMaxSize()
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(DeepDark)
                .padding(paddingValues)
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp, 16.dp, 16.dp, 80.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Warning if unverified
                val isVerified = currentUser?.is_verified ?: false
                if (!isVerified) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = CardDark),
                            border = CardDefaults.outlinedCardBorder().copy(
                                brush = Brush.horizontalGradient(colors = listOf(BorderColor, BorderColor))
                            )
                        ) {
                            Column(
                                modifier = Modifier
                                    .clickable { onNavigateToVerify() }
                                    .padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(Icons.Default.Lock, contentDescription = "Locked", tint = ElectricBlue, modifier = Modifier.size(32.dp))
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("Writing is locked to verified students.", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text("Verify your account via the SBTET portal to write articles.", color = TextSecondary, fontSize = 11.sp, modifier = Modifier.padding(top = 2.dp))
                            }
                        }
                    }
                }

                // Blogs List
                if (blogs.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(40.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "No articles published on this campus yet.",
                                color = TextSecondary,
                                fontSize = 14.sp
                            )
                        }
                    }
                } else {
                    items(blogs, key = { it.id }) { blog ->
                        BlogCard(
                            blog = blog,
                            onClick = {
                                activeBlogForReading = blog
                                showReadDialog = true
                            }
                        )
                    }
                }
            }
        }
    }

    // Write Blog Dialog
    if (showCreateBlogDialog) {
        AlertDialog(
            onDismissRequest = { showCreateBlogDialog = false },
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
                Text("Write Campus Article", fontWeight = FontWeight.Bold, color = TextPrimary)
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Title field
                    OutlinedTextField(
                        value = blogTitle,
                        onValueChange = { blogTitle = it },
                        placeholder = { Text("Article Title", color = TextSecondary) },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedBorderColor = ElectricBlue,
                            unfocusedBorderColor = BorderColor
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Content field
                    OutlinedTextField(
                        value = blogContent,
                        onValueChange = { blogContent = it },
                        placeholder = { Text("Write your thoughts or study material here...", color = TextSecondary) },
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedBorderColor = ElectricBlue,
                            unfocusedBorderColor = BorderColor
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (blogTitle.isNotBlank() && blogContent.isNotBlank()) {
                            viewModel.createBlog(blogTitle.trim(), blogContent.trim()) {
                                blogTitle = ""
                                blogContent = ""
                                showCreateBlogDialog = false
                            }
                        }
                    },
                    enabled = !isLoading && blogTitle.isNotBlank() && blogContent.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(color = DeepDark, modifier = Modifier.size(20.dp))
                    } else {
                        Text("Publish", color = DeepDark, fontWeight = FontWeight.Bold)
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateBlogDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            containerColor = CardDark.copy(alpha = 0.65f),
            shape = RoundedCornerShape(24.dp)
        )
    }

    // Read Blog Dialog
    if (showReadDialog && activeBlogForReading != null) {
        val blog = activeBlogForReading!!
        AlertDialog(
            onDismissRequest = { showReadDialog = false },
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
                Column {
                    Text(
                        text = blog.title,
                        fontWeight = FontWeight.ExtraBold,
                        color = TextPrimary,
                        fontSize = 20.sp,
                        lineHeight = 26.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AvatarView(base64 = blog.profile_pic_base64, name = blog.student_name ?: blog.username, size = 24.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "${blog.student_name ?: blog.username}",
                            color = ElectricBlue,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                        if (blog.is_verified) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(Icons.Default.Verified, contentDescription = null, tint = VerifiedBlue, modifier = Modifier.size(14.dp))
                        }
                    }
                }
            },
            text = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                ) {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        item {
                            Text(
                                text = blog.content,
                                color = TextPrimary,
                                fontSize = 14.sp,
                                lineHeight = 22.sp
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showReadDialog = false }) {
                    Text("Close", color = ElectricBlue, fontWeight = FontWeight.Bold)
                }
            },
            containerColor = CardDark.copy(alpha = 0.65f),
            shape = RoundedCornerShape(24.dp)
        )
    }
}

@Composable
fun BlogCard(
    blog: BlogDto,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardDark),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = Brush.horizontalGradient(colors = listOf(BorderColor, BorderColor))
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Author row
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                AvatarView(base64 = blog.profile_pic_base64, name = blog.student_name ?: blog.username, size = 28.dp)
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = blog.student_name ?: blog.username,
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                        if (blog.is_verified) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(Icons.Default.Verified, contentDescription = null, tint = VerifiedBlue, modifier = Modifier.size(14.dp))
                        }
                    }
                    Text(
                        text = "Published recently",
                        color = TextSecondary,
                        fontSize = 9.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Title
            Text(
                text = blog.title,
                color = TextPrimary,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 16.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Body Snippet
            Text(
                text = blog.content,
                color = TextSecondary,
                fontSize = 13.sp,
                maxLines = 3,
                lineHeight = 18.sp,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Read More trigger
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Text(
                    text = "Read Article",
                    color = ElectricBlue,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.Default.ArrowForward,
                    contentDescription = null,
                    tint = ElectricBlue,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}
