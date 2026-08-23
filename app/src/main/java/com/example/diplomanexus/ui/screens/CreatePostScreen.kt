package com.example.diplomanexus.ui.screens

import android.Manifest
import android.content.ContentUris
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Base64
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.PickVisualMediaRequest
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.diplomanexus.theme.*
import com.example.diplomanexus.viewmodel.AppViewModel
import com.example.diplomanexus.utils.ImageCompressor
import java.io.ByteArrayOutputStream
import androidx.compose.ui.draw.scale
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.text.style.TextOverflow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreatePostScreen(
    viewModel: AppViewModel,
    onClose: () -> Unit,
    prefilledText: String? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isLoading by viewModel.isLoading.collectAsState()

    var selectedMode by remember { mutableStateOf("POST") } // "POST", "TWEET", "STORY"
    var postText by remember { mutableStateOf(prefilledText ?: "") }

    LaunchedEffect(prefilledText) {
        if (prefilledText != null) {
            postText = prefilledText
        }
    }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var selectedImageBase64 by remember { mutableStateOf<String?>(null) }
    var selectedMediaType by remember { mutableStateOf("image") }
    
    var isCloseFriendsOnly by remember { mutableStateOf(false) }
    var activeSticker by remember { mutableStateOf<com.example.diplomanexus.api.StorySticker?>(null) }
    
    var showStickerSelector by remember { mutableStateOf(false) }
    var showPollEditor by remember { mutableStateOf(false) }
    var showQuizEditor by remember { mutableStateOf(false) }
    var showQuestionEditor by remember { mutableStateOf(false) }
    var showCountdownEditor by remember { mutableStateOf(false) }
    var showAddYoursEditor by remember { mutableStateOf(false) }
    var showLinkEditor by remember { mutableStateOf(false) }

    // Media list fetched from MediaStore
    var deviceImages by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var hasStoragePermission by remember { mutableStateOf(false) }

    // Fallback campus Unsplash URLs
    val mockCampusImages = remember {
        listOf(
            "https://images.unsplash.com/photo-1541339907198-e08756dedf3f?w=500&auto=format&fit=crop",
            "https://images.unsplash.com/photo-1523050854058-8df90110c9f1?w=500&auto=format&fit=crop",
            "https://images.unsplash.com/photo-1527891751199-7225231a68dd?w=500&auto=format&fit=crop",
            "https://images.unsplash.com/photo-1434030216411-0b793f4b4173?w=500&auto=format&fit=crop",
            "https://images.unsplash.com/photo-1498243691581-b145c3f54a5c?w=500&auto=format&fit=crop",
            "https://images.unsplash.com/photo-1581092921461-eab62e97a780?w=500&auto=format&fit=crop",
            "https://images.unsplash.com/photo-1517245386807-bb43f82c33c4?w=500&auto=format&fit=crop",
            "https://images.unsplash.com/photo-1522202176988-66273c2fd55f?w=500&auto=format&fit=crop"
        )
    }

    // Load device images helper
    fun loadImagesFromDevice() {
        val imageList = mutableListOf<Uri>()
        val projection = arrayOf(MediaStore.Images.Media._ID, MediaStore.Images.Media.DATE_ADDED)
        try {
            val cursor = context.contentResolver.query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                projection,
                null,
                null,
                "${MediaStore.Images.Media.DATE_ADDED} DESC"
            )
            cursor?.use {
                val idColumn = it.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                var count = 0
                while (it.moveToNext() && count < 30) {
                    val id = it.getLong(idColumn)
                    val contentUri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id)
                    imageList.add(contentUri)
                    count++
                }
            }
            deviceImages = imageList
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // Permission check launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasStoragePermission = isGranted
        if (isGranted) {
            loadImagesFromDevice()
        }
    }

    // Check permissions on enter
    LaunchedEffect(Unit) {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_IMAGES
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
        val isGranted = context.checkSelfPermission(permission) == android.content.pm.PackageManager.PERMISSION_GRANTED
        hasStoragePermission = isGranted
        if (isGranted) {
            loadImagesFromDevice()
        } else {
            permissionLauncher.launch(permission)
        }
    }

    // System Image Picker launcher (plus camera support)
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedImageUri = uri
            val mime = context.contentResolver.getType(uri) ?: ""
            selectedMediaType = if (mime.startsWith("video")) "video" else "image"

            if (selectedMediaType == "video") {
                val size = ImageCompressor.getMediaSize(context, uri)
                if (size > 15 * 1024 * 1024) {
                    Toast.makeText(context, "Video is too large (Max 15MB).", Toast.LENGTH_LONG).show()
                    selectedImageUri = null
                    selectedImageBase64 = null
                } else {
                    val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                    if (bytes != null) {
                        selectedImageBase64 = "data:$mime;base64," + Base64.encodeToString(bytes, Base64.NO_WRAP)
                    }
                }
            } else {
                selectedImageBase64 = ImageCompressor.compressImageFromUri(context, uri)
            }
        }
    }

    // Camera launcher
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap: Bitmap? ->
        if (bitmap != null) {
            val stream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 80, stream)
            val bytes = stream.toByteArray()
            selectedImageBase64 = "data:image/jpeg;base64," + Base64.encodeToString(bytes, Base64.NO_WRAP)
            selectedImageUri = null
            selectedMediaType = "image"
        }
    }

    // Helper to clear composition state
    fun resetState() {
        postText = ""
        selectedImageUri = null
        selectedImageBase64 = null
        selectedMediaType = "image"
        isCloseFriendsOnly = false
        activeSticker = null
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = when (selectedMode) {
                            "POST" -> "New Post"
                            "TWEET" -> "New Tweet"
                            else -> "Add to Story"
                        },
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        resetState()
                        onClose()
                    }) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = TextPrimary)
                    }
                },
                actions = {
                    Button(
                        onClick = {
                            val finalType = when (selectedMode) {
                                "POST" -> selectedMediaType
                                "TWEET" -> "tweet"
                                else -> "story"
                            }
                            var compiledText = postText
                            if (activeSticker != null) {
                                val gson = com.google.gson.Gson()
                                val stickerData = when (val s = activeSticker!!) {
                                    is com.example.diplomanexus.api.StorySticker.Poll -> mapOf("type" to "poll", "question" to s.question, "options" to s.options, "votes" to s.votes, "votedIndex" to s.votedIndex)
                                    is com.example.diplomanexus.api.StorySticker.Quiz -> mapOf("type" to "quiz", "question" to s.question, "options" to s.options, "correctIndex" to s.correctIndex, "selectedIndex" to s.selectedIndex)
                                    is com.example.diplomanexus.api.StorySticker.Question -> mapOf("type" to "question", "prompt" to s.prompt)
                                    is com.example.diplomanexus.api.StorySticker.Countdown -> mapOf("type" to "countdown", "title" to s.title, "targetEpochMs" to s.targetEpochMs)
                                    is com.example.diplomanexus.api.StorySticker.AddYours -> mapOf("type" to "add_yours", "prompt" to s.prompt)
                                    is com.example.diplomanexus.api.StorySticker.Link -> mapOf("type" to "link", "url" to s.url, "label" to s.label)
                                }
                                compiledText = "[STICKER:${gson.toJson(stickerData)}]$compiledText"
                            }
                            if (isCloseFriendsOnly) {
                                compiledText = "[CF]$compiledText"
                            }
                            viewModel.createPost(compiledText, selectedImageBase64, finalType) {
                                resetState()
                                onClose()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = BrandOrange),
                        enabled = (postText.isNotBlank() || selectedImageBase64 != null) && !isLoading,
                        shape = RoundedCornerShape(18.dp)
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(18.dp))
                        } else {
                            Text(
                                text = when (selectedMode) {
                                    "POST" -> "Share"
                                    "TWEET" -> "Tweet"
                                    else -> "Add"
                                },
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DeepDark)
            )
        },
        bottomBar = {
            // Instagram-style horizontal scroll tabs
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DeepDark)
            ) {
                HorizontalDivider(color = BorderColor.copy(alpha = 0.5f), thickness = 0.5.dp)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val modes = listOf("POST", "TWEET", "STORY")
                    modes.forEach { mode ->
                        val isSelected = selectedMode == mode
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .padding(horizontal = 20.dp)
                                .clickable {
                                    selectedMode = mode
                                    if (mode == "TWEET" && selectedImageBase64 == null) {
                                        // Tweeting doesn't enforce media
                                    }
                                }
                        ) {
                            Text(
                                text = mode,
                                color = if (isSelected) Color.White else TextSecondary,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 14.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Box(
                                modifier = Modifier
                                    .size(width = 16.dp, height = 2.dp)
                                    .background(if (isSelected) BrandOrange else Color.Transparent)
                            )
                        }
                    }
                }
            }
        },
        modifier = modifier.fillMaxSize()
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(DeepDark)
                .padding(paddingValues)
        ) {
            when (selectedMode) {
                "TWEET" -> {
                    // Tweet Composer Card
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(16.dp)
                    ) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = CardDark),
                            border = CardDefaults.outlinedCardBorder().copy(
                                brush = Brush.horizontalGradient(listOf(BorderColor, BorderColor))
                            )
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(verticalAlignment = Alignment.Top) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(CircleShape)
                                            .background(BrandOrange.copy(0.2f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Default.Send, contentDescription = null, tint = BrandOrange, modifier = Modifier.size(20.dp))
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    TextField(
                                        value = postText,
                                        onValueChange = { if (it.length <= 280) postText = it },
                                        placeholder = { Text("What's happening on campus?", color = TextSecondary) },
                                        colors = TextFieldDefaults.colors(
                                            focusedContainerColor = Color.Transparent,
                                            unfocusedContainerColor = Color.Transparent,
                                            focusedIndicatorColor = Color.Transparent,
                                            unfocusedIndicatorColor = Color.Transparent,
                                            focusedTextColor = TextPrimary,
                                            unfocusedTextColor = TextPrimary
                                        ),
                                        modifier = Modifier.weight(1f).height(120.dp)
                                    )
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    Text(
                                        text = "${postText.length}/280",
                                        color = if (postText.length >= 260) AccentPink else TextTertiary,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Tweet Image selection box
                        if (selectedImageBase64 != null) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(160.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(CardDark)
                            ) {
                                if (selectedImageUri != null) {
                                    AsyncImage(
                                        model = selectedImageUri,
                                        contentDescription = "Tweet Image Attachment",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } else {
                                    // Custom mock base64 render
                                    val clean = selectedImageBase64!!.substringAfter("base64,")
                                    val bytes = Base64.decode(clean, Base64.DEFAULT)
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
                                IconButton(
                                    onClick = {
                                        selectedImageUri = null
                                        selectedImageBase64 = null
                                    },
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(8.dp)
                                        .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                                        .size(28.dp)
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = "Remove", tint = Color.White, modifier = Modifier.size(16.dp))
                                }
                            }
                        } else {
                            // Empty media picker hint for tweet
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(100.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .border(1.dp, BorderColor, RoundedCornerShape(12.dp))
                                    .clickable { filePickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(Icons.Default.PhotoLibrary, contentDescription = null, tint = TextSecondary)
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text("Add photo to tweet (optional)", color = TextSecondary, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
                "POST", "STORY" -> {
                    // Immersive post/story layouts featuring large preview & grid gallery
                    val isStory = selectedMode == "STORY"
                    val previewAspect = if (isStory) 0.5625f else 1f // 9:16 for Story, 1:1 for Post

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        // Caption and Preview Container
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Box(
                                modifier = Modifier
                                    .weight(1.3f)
                                    .aspectRatio(previewAspect)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(CardDark),
                                contentAlignment = Alignment.Center
                            ) {
                                if (selectedImageBase64 != null) {
                                    if (selectedMediaType == "video") {
                                        Box(
                                            modifier = Modifier.fillMaxSize().background(Color.Black),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(Icons.Default.PlayCircle, contentDescription = "Video preview", tint = Color.White, modifier = Modifier.size(48.dp))
                                        }
                                    } else if (selectedImageUri != null) {
                                        AsyncImage(
                                            model = selectedImageUri,
                                            contentDescription = "Preview Image",
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    } else {
                                        // Unsplash URL or Custom bitmap
                                        if (selectedImageBase64!!.startsWith("http")) {
                                            AsyncImage(
                                                model = selectedImageBase64,
                                                contentDescription = "Preview Unsplash Image",
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier.fillMaxSize()
                                            )
                                        } else {
                                            val clean = selectedImageBase64!!.substringAfter("base64,")
                                            val bytes = Base64.decode(clean, Base64.DEFAULT)
                                            val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                                            if (bitmap != null) {
                                                Image(
                                                    bitmap = bitmap.asImageBitmap(),
                                                    contentDescription = "Preview Camera Image",
                                                    contentScale = ContentScale.Crop,
                                                    modifier = Modifier.fillMaxSize()
                                                )
                                            }
                                        }
                                    }
                                    if (activeSticker != null) {
                                        Box(
                                            modifier = Modifier
                                                .align(Alignment.Center)
                                                .padding(8.dp)
                                        ) {
                                            StickerPreviewComponent(activeSticker!!)
                                        }
                                    }
                                } else {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(Icons.Default.Image, contentDescription = null, tint = TextTertiary, modifier = Modifier.size(36.dp))
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text("Select Photo/Video", color = TextSecondary, fontSize = 11.sp, textAlign = TextAlign.Center)
                                    }
                                }
                            }
                            
                            Spacer(modifier = Modifier.width(16.dp))
                            
                            // Text Input next to preview (only in POST mode, stories don't require caption text)
                            Box(
                                modifier = Modifier
                                    .weight(2f)
                                    .fillMaxHeight()
                            ) {
                                if (!isStory) {
                                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                        TextField(
                                            value = postText,
                                            onValueChange = { postText = it },
                                            placeholder = { Text("Write a caption...", color = TextSecondary, fontSize = 14.sp) },
                                            colors = TextFieldDefaults.colors(
                                                focusedContainerColor = Color.Transparent,
                                                unfocusedContainerColor = Color.Transparent,
                                                focusedIndicatorColor = Color.Transparent,
                                                unfocusedIndicatorColor = Color.Transparent,
                                                focusedTextColor = TextPrimary,
                                                unfocusedTextColor = TextPrimary
                                            ),
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(if (isCloseFriendsOnly) Color(0xFF4CAF50).copy(0.12f) else Color.White.copy(0.03f))
                                                .clickable { isCloseFriendsOnly = !isCloseFriendsOnly }
                                                .padding(horizontal = 10.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Star,
                                                contentDescription = null,
                                                tint = if (isCloseFriendsOnly) Color(0xFF4CAF50) else TextSecondary,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = "Close Friends Only",
                                                color = if (isCloseFriendsOnly) Color(0xFF4CAF50) else TextSecondary,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.weight(1f)
                                            )
                                            Checkbox(
                                                checked = isCloseFriendsOnly,
                                                onCheckedChange = { isCloseFriendsOnly = it },
                                                colors = CheckboxDefaults.colors(
                                                    checkedColor = Color(0xFF4CAF50),
                                                    checkmarkColor = Color.White
                                                ),
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                } else {
                                    Column(
                                        verticalArrangement = Arrangement.spacedBy(12.dp),
                                        modifier = Modifier.padding(top = 8.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(if (isCloseFriendsOnly) Color(0xFF4CAF50).copy(0.15f) else Color.White.copy(0.05f))
                                                .border(1.dp, if (isCloseFriendsOnly) Color(0xFF4CAF50) else Color.White.copy(0.12f), RoundedCornerShape(12.dp))
                                                .clickable { isCloseFriendsOnly = !isCloseFriendsOnly }
                                                .padding(horizontal = 12.dp, vertical = 10.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Star,
                                                contentDescription = null,
                                                tint = if (isCloseFriendsOnly) Color(0xFF4CAF50) else TextSecondary,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Text(
                                                text = "Close Friends Only",
                                                color = if (isCloseFriendsOnly) Color(0xFF4CAF50) else TextPrimary,
                                                fontWeight = FontWeight.SemiBold,
                                                fontSize = 12.sp,
                                                modifier = Modifier.weight(1f)
                                            )
                                            Switch(
                                                checked = isCloseFriendsOnly,
                                                onCheckedChange = { isCloseFriendsOnly = it },
                                                colors = SwitchDefaults.colors(
                                                    checkedThumbColor = Color.White,
                                                    checkedTrackColor = Color(0xFF4CAF50),
                                                    uncheckedThumbColor = TextSecondary,
                                                    uncheckedTrackColor = Color.White.copy(alpha = 0.1f)
                                                ),
                                                modifier = Modifier.scale(0.8f)
                                            )
                                        }

                                        Button(
                                            onClick = { showStickerSelector = true },
                                            colors = ButtonDefaults.buttonColors(containerColor = BrandOrange),
                                            shape = RoundedCornerShape(12.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text(
                                                text = if (activeSticker != null) "Change Sticker 🎭" else "Add Sticker 🎭",
                                                color = Color.White,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp
                                            )
                                        }

                                        if (activeSticker != null) {
                                            Button(
                                                onClick = { activeSticker = null },
                                                colors = ButtonDefaults.buttonColors(containerColor = AccentPink),
                                                shape = RoundedCornerShape(12.dp),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Text("Remove Sticker ✗", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                            }
                                        }

                                        OutlinedTextField(
                                            value = postText,
                                            onValueChange = { postText = it },
                                            placeholder = { Text("Story caption...", color = TextSecondary, fontSize = 12.sp) },
                                            maxLines = 2,
                                            shape = RoundedCornerShape(12.dp),
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedTextColor = TextPrimary,
                                                unfocusedTextColor = TextPrimary,
                                                focusedBorderColor = BrandOrange,
                                                unfocusedBorderColor = BorderColor
                                            ),
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                }
                            }
                        }

                        // Grid Gallery header
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Recent Media", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            
                            // System picker fallback trigger
                            Text(
                                text = "Other Folders",
                                color = BrandOrange,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.clickable {
                                    filePickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
                                    )
                                }
                            )
                        }

                        // Grid Gallery List
                        val galleryItems = if (hasStoragePermission && deviceImages.isNotEmpty()) {
                            deviceImages
                        } else {
                            mockCampusImages.map { Uri.parse(it) }
                        }

                        LazyVerticalGrid(
                            columns = GridCells.Fixed(4),
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentPadding = PaddingValues(1.dp),
                            horizontalArrangement = Arrangement.spacedBy(1.dp),
                            verticalArrangement = Arrangement.spacedBy(1.dp)
                        ) {
                            // Item 0: Camera tile
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .aspectRatio(1f)
                                        .background(CardDark)
                                        .clickable { cameraLauncher.launch(null) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(Icons.Default.CameraAlt, contentDescription = "Camera", tint = Color.White)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text("Camera", color = Color.White, fontSize = 10.sp)
                                    }
                                }
                            }

                            // Items: Gallery photos
                            items(galleryItems) { mediaUri ->
                                val isSelected = selectedImageUri == mediaUri || selectedImageBase64 == mediaUri.toString()
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .aspectRatio(1f)
                                        .border(
                                            width = if (isSelected) 3.dp else 0.dp,
                                            color = if (isSelected) BrandOrange else Color.Transparent
                                        )
                                        .clickable {
                                            selectedImageUri = mediaUri
                                            selectedMediaType = "image"
                                            if (mediaUri.toString().startsWith("http")) {
                                                // mock image URL passed directly to cloud upload
                                                selectedImageBase64 = mediaUri.toString()
                                            } else {
                                                // compress local device photo
                                                selectedImageBase64 = ImageCompressor.compressImageFromUri(context, mediaUri)
                                            }
                                        }
                                ) {
                                    AsyncImage(
                                        model = mediaUri,
                                        contentDescription = "Gallery media",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // ─── Sticker Selector Dialog ───
    if (showStickerSelector) {
        AlertDialog(
            onDismissRequest = { showStickerSelector = false },
            title = { Text("Choose Sticker 🎭", fontWeight = FontWeight.Bold, color = TextPrimary) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    val stickerTypes = listOf(
                        Pair("Poll 📊", { showPollEditor = true }),
                        Pair("Quiz ❓", { showQuizEditor = true }),
                        Pair("Question Box 💬", { showQuestionEditor = true }),
                        Pair("Countdown ⏳", { showCountdownEditor = true }),
                        Pair("Add Yours 📸", { showAddYoursEditor = true }),
                        Pair("Link Sticker 🔗", { showLinkEditor = true })
                    )
                    stickerTypes.forEach { item ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.White.copy(alpha = 0.05f))
                                .clickable {
                                    showStickerSelector = false
                                    item.second()
                                }
                                .padding(vertical = 12.dp, horizontal = 16.dp)
                        ) {
                            Text(text = item.first, color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showStickerSelector = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            containerColor = CardDark,
            shape = RoundedCornerShape(24.dp)
        )
    }

    // ─── Poll Editor Dialog ───
    if (showPollEditor) {
        var question by remember { mutableStateOf("") }
        var opt1 by remember { mutableStateOf("") }
        var opt2 by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showPollEditor = false },
            title = { Text("Create Poll 📊", fontWeight = FontWeight.Bold, color = TextPrimary) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = question,
                        onValueChange = { question = it },
                        placeholder = { Text("Ask a question...", color = TextSecondary) },
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary, focusedBorderColor = BrandOrange, unfocusedBorderColor = BorderColor),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = opt1,
                        onValueChange = { opt1 = it },
                        placeholder = { Text("Option 1", color = TextSecondary) },
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary, focusedBorderColor = BrandOrange, unfocusedBorderColor = BorderColor),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = opt2,
                        onValueChange = { opt2 = it },
                        placeholder = { Text("Option 2", color = TextSecondary) },
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary, focusedBorderColor = BrandOrange, unfocusedBorderColor = BorderColor),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (question.isNotBlank() && opt1.isNotBlank() && opt2.isNotBlank()) {
                            activeSticker = com.example.diplomanexus.api.StorySticker.Poll(
                                question = question.trim(),
                                options = listOf(opt1.trim(), opt2.trim()),
                                votes = listOf(0, 0)
                            )
                            showPollEditor = false
                        }
                    },
                    enabled = question.isNotBlank() && opt1.isNotBlank() && opt2.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = BrandOrange)
                ) {
                    Text("Add", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showPollEditor = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            containerColor = CardDark,
            shape = RoundedCornerShape(24.dp)
        )
    }

    // ─── Quiz Editor Dialog ───
    if (showQuizEditor) {
        var question by remember { mutableStateOf("") }
        var opt1 by remember { mutableStateOf("") }
        var opt2 by remember { mutableStateOf("") }
        var opt3 by remember { mutableStateOf("") }
        var correctIndex by remember { mutableStateOf(0) }

        AlertDialog(
            onDismissRequest = { showQuizEditor = false },
            title = { Text("Create Quiz ❓", fontWeight = FontWeight.Bold, color = TextPrimary) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = question,
                        onValueChange = { question = it },
                        placeholder = { Text("Quiz Question...", color = TextSecondary) },
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary, focusedBorderColor = BrandOrange, unfocusedBorderColor = BorderColor),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = opt1,
                        onValueChange = { opt1 = it },
                        placeholder = { Text("Option A", color = TextSecondary) },
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary, focusedBorderColor = BrandOrange, unfocusedBorderColor = BorderColor),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = opt2,
                        onValueChange = { opt2 = it },
                        placeholder = { Text("Option B", color = TextSecondary) },
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary, focusedBorderColor = BrandOrange, unfocusedBorderColor = BorderColor),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = opt3,
                        onValueChange = { opt3 = it },
                        placeholder = { Text("Option C", color = TextSecondary) },
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary, focusedBorderColor = BrandOrange, unfocusedBorderColor = BorderColor),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text("Select Correct Option:", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("A", "B", "C").forEachIndexed { index, label ->
                            val isSel = correctIndex == index
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(if (isSel) BrandOrange else Color.White.copy(0.05f))
                                    .clickable { correctIndex = index },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(label, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (question.isNotBlank() && opt1.isNotBlank() && opt2.isNotBlank() && opt3.isNotBlank()) {
                            activeSticker = com.example.diplomanexus.api.StorySticker.Quiz(
                                question = question.trim(),
                                options = listOf(opt1.trim(), opt2.trim(), opt3.trim()),
                                correctIndex = correctIndex
                            )
                            showQuizEditor = false
                        }
                    },
                    enabled = question.isNotBlank() && opt1.isNotBlank() && opt2.isNotBlank() && opt3.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = BrandOrange)
                ) {
                    Text("Add", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showQuizEditor = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            containerColor = CardDark,
            shape = RoundedCornerShape(24.dp)
        )
    }

    // ─── Question Box Editor Dialog ───
    if (showQuestionEditor) {
        var prompt by remember { mutableStateOf("Ask me anything") }

        AlertDialog(
            onDismissRequest = { showQuestionEditor = false },
            title = { Text("Question Box 💬", fontWeight = FontWeight.Bold, color = TextPrimary) },
            text = {
                OutlinedTextField(
                    value = prompt,
                    onValueChange = { prompt = it },
                    placeholder = { Text("Prompt...", color = TextSecondary) },
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary, focusedBorderColor = BrandOrange, unfocusedBorderColor = BorderColor),
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (prompt.isNotBlank()) {
                            activeSticker = com.example.diplomanexus.api.StorySticker.Question(prompt.trim())
                            showQuestionEditor = false
                        }
                    },
                    enabled = prompt.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = BrandOrange)
                ) {
                    Text("Add", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showQuestionEditor = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            containerColor = CardDark,
            shape = RoundedCornerShape(24.dp)
        )
    }

    // ─── Countdown Editor Dialog ───
    if (showCountdownEditor) {
        var title by remember { mutableStateOf("") }
        var daysText by remember { mutableStateOf("1") }
        var hoursText by remember { mutableStateOf("0") }

        AlertDialog(
            onDismissRequest = { showCountdownEditor = false },
            title = { Text("Countdown Timer ⏳", fontWeight = FontWeight.Bold, color = TextPrimary) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        placeholder = { Text("Event Title...", color = TextSecondary) },
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary, focusedBorderColor = BrandOrange, unfocusedBorderColor = BorderColor),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = daysText,
                        onValueChange = { daysText = it },
                        placeholder = { Text("Days from now", color = TextSecondary) },
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary, focusedBorderColor = BrandOrange, unfocusedBorderColor = BorderColor),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = hoursText,
                        onValueChange = { hoursText = it },
                        placeholder = { Text("Hours from now", color = TextSecondary) },
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary, focusedBorderColor = BrandOrange, unfocusedBorderColor = BorderColor),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val d = daysText.toIntOrNull() ?: 0
                        val h = hoursText.toIntOrNull() ?: 0
                        if (title.isNotBlank() && (d > 0 || h > 0)) {
                            val durationEpoch = System.currentTimeMillis() + (d * 24 + h) * 3600 * 1000L
                            activeSticker = com.example.diplomanexus.api.StorySticker.Countdown(
                                title = title.trim(),
                                targetEpochMs = durationEpoch
                            )
                            showCountdownEditor = false
                        }
                    },
                    enabled = title.isNotBlank() && (daysText.toIntOrNull() != null || hoursText.toIntOrNull() != null),
                    colors = ButtonDefaults.buttonColors(containerColor = BrandOrange)
                ) {
                    Text("Add", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCountdownEditor = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            containerColor = CardDark,
            shape = RoundedCornerShape(24.dp)
        )
    }

    // ─── Add Yours Editor Dialog ───
    if (showAddYoursEditor) {
        var prompt by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showAddYoursEditor = false },
            title = { Text("Add Yours 📸", fontWeight = FontWeight.Bold, color = TextPrimary) },
            text = {
                OutlinedTextField(
                    value = prompt,
                    onValueChange = { prompt = it },
                    placeholder = { Text("Write a prompt (e.g. Desk setup)", color = TextSecondary) },
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary, focusedBorderColor = BrandOrange, unfocusedBorderColor = BorderColor),
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (prompt.isNotBlank()) {
                            activeSticker = com.example.diplomanexus.api.StorySticker.AddYours(prompt.trim())
                            showAddYoursEditor = false
                        }
                    },
                    enabled = prompt.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = BrandOrange)
                ) {
                    Text("Add", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddYoursEditor = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            containerColor = CardDark,
            shape = RoundedCornerShape(24.dp)
        )
    }

    // ─── Link Editor Dialog ───
    if (showLinkEditor) {
        var url by remember { mutableStateOf("") }
        var label by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showLinkEditor = false },
            title = { Text("Link Sticker 🔗", fontWeight = FontWeight.Bold, color = TextPrimary) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = url,
                        onValueChange = { url = it },
                        placeholder = { Text("URL (e.g. google.com)", color = TextSecondary) },
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary, focusedBorderColor = BrandOrange, unfocusedBorderColor = BorderColor),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = label,
                        onValueChange = { label = it },
                        placeholder = { Text("Link Label (e.g. My Website)", color = TextSecondary) },
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary, focusedBorderColor = BrandOrange, unfocusedBorderColor = BorderColor),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (url.isNotBlank() && label.isNotBlank()) {
                            activeSticker = com.example.diplomanexus.api.StorySticker.Link(
                                url = url.trim(),
                                label = label.trim()
                            )
                            showLinkEditor = false
                        }
                    },
                    enabled = url.isNotBlank() && label.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = BrandOrange)
                ) {
                    Text("Add", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLinkEditor = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            containerColor = CardDark,
            shape = RoundedCornerShape(24.dp)
        )
    }
}

@Composable
fun StickerPreviewComponent(sticker: com.example.diplomanexus.api.StorySticker) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.8f)),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f)),
        modifier = Modifier.width(140.dp)
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            when (sticker) {
                is com.example.diplomanexus.api.StorySticker.Poll -> {
                    Text("📊 POLL", color = BrandOrange, fontWeight = FontWeight.Bold, fontSize = 9.sp)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(sticker.question.ifBlank { "Poll Question" }, color = Color.White, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                is com.example.diplomanexus.api.StorySticker.Quiz -> {
                    Text("❓ QUIZ", color = AccentPink, fontWeight = FontWeight.Bold, fontSize = 9.sp)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(sticker.question.ifBlank { "Quiz Question" }, color = Color.White, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                is com.example.diplomanexus.api.StorySticker.Question -> {
                    Text("💬 QUESTION", color = BrandOrange, fontWeight = FontWeight.Bold, fontSize = 9.sp)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(sticker.prompt.ifBlank { "Ask me anything" }, color = Color.White, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                is com.example.diplomanexus.api.StorySticker.Countdown -> {
                    Text("⏳ COUNTDOWN", color = BrandOrange, fontWeight = FontWeight.Bold, fontSize = 9.sp)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(sticker.title.ifBlank { "Event Title" }, color = Color.White, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                is com.example.diplomanexus.api.StorySticker.AddYours -> {
                    Text("📸 ADD YOURS", color = BrandOrange, fontWeight = FontWeight.Bold, fontSize = 9.sp)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(sticker.prompt.ifBlank { "Show yours" }, color = Color.White, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                is com.example.diplomanexus.api.StorySticker.Link -> {
                    Text("🔗 LINK", color = BrandOrange, fontWeight = FontWeight.Bold, fontSize = 9.sp)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(sticker.label.ifBlank { "Visit Link" }, color = Color.White, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}
