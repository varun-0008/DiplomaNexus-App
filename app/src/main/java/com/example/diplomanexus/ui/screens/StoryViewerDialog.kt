package com.example.diplomanexus.ui.screens

import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import android.util.Log
import android.widget.Toast
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import com.example.diplomanexus.api.PostDto
import com.example.diplomanexus.api.StorySticker
import com.example.diplomanexus.theme.*
import com.example.diplomanexus.utils.MediaCacheManager
import com.google.gson.Gson
import java.text.SimpleDateFormat
import java.util.*

fun parseStoryContent(rawContent: String): Pair<String, StorySticker?> {
    var clean = rawContent
    // Strip close friends tag if any
    if (clean.startsWith("[CF]")) {
        clean = clean.substring(4)
    }
    if (clean.startsWith("[STICKER:")) {
        val endIndex = clean.indexOf("]")
        if (endIndex != -1) {
            val json = clean.substring(9, endIndex)
            val remaining = clean.substring(endIndex + 1)
            try {
                val gson = Gson()
                val map = gson.fromJson(json, Map::class.java)
                val type = map["type"] as? String
                val sticker = when (type) {
                    "poll" -> {
                        val question = map["question"] as? String ?: ""
                        val options = (map["options"] as? List<*>)?.map { it.toString() } ?: emptyList()
                        val votes = (map["votes"] as? List<*>)?.map { (it as? Number)?.toInt() ?: 0 } ?: listOf(0, 0)
                        val votedIndex = (map["votedIndex"] as? Number)?.toInt() ?: -1
                        StorySticker.Poll(question, options, votes, votedIndex)
                    }
                    "quiz" -> {
                        val question = map["question"] as? String ?: ""
                        val options = (map["options"] as? List<*>)?.map { it.toString() } ?: emptyList()
                        val correctIndex = (map["correctIndex"] as? Number)?.toInt() ?: 0
                        val selectedIndex = (map["selectedIndex"] as? Number)?.toInt() ?: -1
                        StorySticker.Quiz(question, options, correctIndex, selectedIndex)
                    }
                    "question" -> {
                        val prompt = map["prompt"] as? String ?: ""
                        StorySticker.Question(prompt)
                    }
                    "countdown" -> {
                        val title = map["title"] as? String ?: ""
                        val targetEpochMs = (map["targetEpochMs"] as? Number)?.toLong() ?: 0L
                        StorySticker.Countdown(title, targetEpochMs)
                    }
                    "add_yours" -> {
                        val prompt = map["prompt"] as? String ?: ""
                        StorySticker.AddYours(prompt)
                    }
                    "link" -> {
                        val url = map["url"] as? String ?: ""
                        val label = map["label"] as? String ?: ""
                        StorySticker.Link(url, label)
                    }
                    else -> null
                }
                return Pair(remaining, sticker)
            } catch (e: Exception) {
                Log.e("StoryViewerDialog", "Error parsing sticker json", e)
            }
        }
    }
    return Pair(clean, null)
}

@OptIn(UnstableApi::class)
@Composable
fun StoryViewerDialog(
    username: String,
    stories: List<PostDto>,
    onDismiss: () -> Unit,
    onAddYoursClick: (String) -> Unit = {}
) {
    if (stories.isEmpty()) {
        onDismiss()
        return
    }
    var currentStoryIndex by remember { mutableStateOf(0) }
    var progress by remember { mutableStateOf(0f) }
    var isPaused by remember { mutableStateOf(false) }
    var isChromeHidden by remember { mutableStateOf(false) }
    
    val story = stories[currentStoryIndex]
    
    // Parse sticker & caption
    val parsedData = remember(story.content) { parseStoryContent(story.content) }
    val caption = parsedData.first
    var activeSticker by remember(story.id) { mutableStateOf(parsedData.second) }

    val isCloseFriends = remember(story.content) { story.content.startsWith("[CF]") }

    LaunchedEffect(username, currentStoryIndex, isPaused) {
        if (isPaused) return@LaunchedEffect
        val durationMs = 5000f
        val stepMs = 50L
        while (progress < 1f) {
            kotlinx.coroutines.delay(stepMs)
            progress += stepMs / durationMs
        }
        if (currentStoryIndex < stories.size - 1) {
            progress = 0f
            currentStoryIndex++
        } else {
            onDismiss()
        }
    }
    
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            // Background Tap Detector & Story Media Container
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(username, currentStoryIndex) {
                        detectTapGestures(
                            onPress = {
                                isPaused = true
                                isChromeHidden = true
                                tryAwaitRelease()
                                isPaused = false
                                isChromeHidden = false
                            },
                            onTap = { offset ->
                                val isRightSide = offset.x > size.width / 2
                                if (isRightSide) {
                                    if (currentStoryIndex < stories.size - 1) {
                                        progress = 0f
                                        currentStoryIndex++
                                    } else {
                                        onDismiss()
                                    }
                                } else {
                                    if (currentStoryIndex > 0) {
                                        progress = 0f
                                        currentStoryIndex--
                                    }
                                }
                            }
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                if (story.media_type == "video" && story.media_url != null) {
                    val context = LocalContext.current
                    val exoPlayer = remember(story.id) {
                        ExoPlayer.Builder(context).build().apply {
                            val dataSourceFactory = MediaCacheManager.getCacheDataSourceFactory(context)
                            val mediaItem = MediaItem.fromUri(Uri.parse(story.media_url))
                            val mediaSource = androidx.media3.exoplayer.source.DefaultMediaSourceFactory(dataSourceFactory).createMediaSource(mediaItem)
                            setMediaSource(mediaSource)
                            repeatMode = ExoPlayer.REPEAT_MODE_OFF
                            playWhenReady = !isPaused
                            prepare()
                        }
                    }
                    
                    LaunchedEffect(isPaused) {
                        if (isPaused) exoPlayer.pause() else exoPlayer.play()
                    }
                    
                    DisposableEffect(story.id) {
                        onDispose { exoPlayer.release() }
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
                } else {
                    if (story.media_url != null) {
                        AsyncImage(
                            model = story.media_url,
                            contentDescription = "Story Image",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else if (story.image_base64 != null) {
                        val b64Data = story.image_base64.substringAfter("base64,")
                        val bytes = Base64.decode(b64Data, Base64.DEFAULT)
                        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                        if (bitmap != null) {
                            Image(
                                bitmap = bitmap.asImageBitmap(),
                                contentDescription = "Story Image",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }

                // Interactive Sticker Overlay (If Present)
                if (activeSticker != null) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(horizontal = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        StickerView(
                            sticker = activeSticker!!,
                            onUpdate = { updated ->
                                activeSticker = updated
                            },
                            onAddYoursClick = { prompt ->
                                onDismiss()
                                onAddYoursClick(prompt)
                            },
                            onModalActive = { active ->
                                isPaused = active
                            }
                        )
                    }
                }

                // Bottom Story Caption
                if (caption.isNotBlank() && !isChromeHidden) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(0.7f))))
                            .padding(horizontal = 20.dp, vertical = 40.dp)
                    ) {
                        Text(
                            text = caption,
                            color = Color.White,
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
            
            // Top Controls Chrome Overlay
            AnimatedVisibility(
                visible = !isChromeHidden,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                ) {
                    // Segmented Progress Bars
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        stories.forEachIndexed { index, _ ->
                            val segmentProgress = when {
                                index < currentStoryIndex -> 1f
                                index > currentStoryIndex -> 0f
                                else -> progress
                            }
                            LinearProgressIndicator(
                                progress = { segmentProgress },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(3.dp)
                                    .clip(RoundedCornerShape(2.dp)),
                                color = Color.White,
                                trackColor = Color.White.copy(alpha = 0.3f)
                            )
                        }
                    }
                    
                    // User Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AvatarView(
                            base64 = story.profile_pic_base64,
                            name = story.student_name ?: story.username,
                            size = 36.dp
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = story.student_name ?: story.username,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                                if (isCloseFriends) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(Color(0xFF4CAF50))
                                            .padding(horizontal = 4.dp, vertical = 1.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Star,
                                            contentDescription = "Close Friends",
                                            tint = Color.White,
                                            modifier = Modifier.size(8.dp)
                                        )
                                    }
                                }
                            }
                            Text(
                                text = getRelativeTimeString(story.created_at),
                                color = Color.White.copy(alpha = 0.7f),
                                fontSize = 11.sp
                            )
                        }
                        Spacer(modifier = Modifier.weight(1f))
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "Close Story", tint = Color.White)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StickerView(
    sticker: StorySticker,
    onUpdate: (StorySticker) -> Unit,
    onAddYoursClick: (String) -> Unit,
    onModalActive: (Boolean) -> Unit
) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.75f)),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f)),
        modifier = Modifier.width(260.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            when (sticker) {
                is StorySticker.Poll -> {
                    Text(
                        text = sticker.question.ifBlank { "Poll" },
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    val totalVotes = sticker.votes.sum().coerceAtLeast(1)
                    val hasVoted = sticker.votedIndex != -1

                    sticker.options.forEachIndexed { index, option ->
                        val pct = (sticker.votes[index] * 100) / totalVotes
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .height(40.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    if (hasVoted && sticker.votedIndex == index) BrandOrange.copy(0.2f)
                                    else Color.White.copy(alpha = 0.08f)
                                )
                                .border(
                                    width = 1.dp,
                                    color = if (hasVoted && sticker.votedIndex == index) BrandOrange else Color.Transparent,
                                    shape = RoundedCornerShape(10.dp)
                                )
                                .clickable {
                                    if (!hasVoted) {
                                        val newVotes = sticker.votes.toMutableList()
                                        newVotes[index] = newVotes[index] + 1
                                        sticker.votes = newVotes
                                        sticker.votedIndex = index
                                        onUpdate(sticker)
                                    }
                                }
                        ) {
                            if (hasVoted) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .fillMaxWidth(pct.toFloat() / 100f)
                                        .background(BrandOrange.copy(alpha = 0.35f))
                                )
                            }
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(text = option, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                if (hasVoted) {
                                    Text(text = "$pct%", color = Color.White.copy(0.9f), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
                is StorySticker.Quiz -> {
                    Text(
                        text = sticker.question.ifBlank { "Quiz" },
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    val hasAnswered = sticker.selectedIndex != -1

                    sticker.options.forEachIndexed { index, option ->
                        val isSelected = sticker.selectedIndex == index
                        val isCorrect = sticker.correctIndex == index

                        val containerColor = when {
                            !hasAnswered -> Color.White.copy(alpha = 0.08f)
                            isCorrect -> Color(0xFF2E7D32).copy(alpha = 0.3f)
                            isSelected -> AccentPink.copy(alpha = 0.3f)
                            else -> Color.White.copy(alpha = 0.04f)
                        }

                        val borderColor = when {
                            !hasAnswered -> Color.Transparent
                            isCorrect -> Color(0xFF2E7D32)
                            isSelected -> AccentPink
                            else -> Color.Transparent
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .height(40.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(containerColor)
                                .border(1.dp, borderColor, RoundedCornerShape(10.dp))
                                .clickable {
                                    if (!hasAnswered) {
                                        sticker.selectedIndex = index
                                        onUpdate(sticker)
                                    }
                                },
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = option,
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                if (hasAnswered && isCorrect) {
                                    Text("✓", color = Color(0xFF4CAF50), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                } else if (hasAnswered && isSelected && !isCorrect) {
                                    Text("✗", color = AccentPink, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                }
                            }
                        }
                    }
                }
                is StorySticker.Question -> {
                    Text(
                        text = sticker.prompt.ifBlank { "Ask me a question" },
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    var responseText by remember { mutableStateOf("") }
                    var showDialog by remember { mutableStateOf(false) }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(42.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color.White.copy(alpha = 0.08f))
                            .clickable {
                                onModalActive(true)
                                showDialog = true
                            }
                            .padding(horizontal = 12.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Text("Type something...", color = Color.White.copy(alpha = 0.4f), fontSize = 13.sp)
                    }

                    if (showDialog) {
                        AlertDialog(
                            onDismissRequest = {
                                showDialog = false
                                onModalActive(false)
                            },
                            title = { Text("Send Response", fontWeight = FontWeight.Bold, color = TextPrimary) },
                            text = {
                                OutlinedTextField(
                                    value = responseText,
                                    onValueChange = { responseText = it },
                                    placeholder = { Text("Your answer...", color = TextSecondary) },
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = TextPrimary,
                                        unfocusedTextColor = TextPrimary,
                                        focusedBorderColor = BrandOrange,
                                        unfocusedBorderColor = BorderColor
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            },
                            confirmButton = {
                                Button(
                                    onClick = {
                                        if (responseText.isNotBlank()) {
                                            Toast.makeText(context, "Response sent!", Toast.LENGTH_SHORT).show()
                                            responseText = ""
                                            showDialog = false
                                            onModalActive(false)
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = BrandOrange)
                                ) {
                                    Text("Send", color = Color.White)
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = {
                                    showDialog = false
                                    onModalActive(false)
                                }) {
                                    Text("Cancel", color = TextSecondary)
                                }
                            },
                            containerColor = CardDark,
                            shape = RoundedCornerShape(24.dp)
                        )
                    }
                }
                is StorySticker.Countdown -> {
                    Text(
                        text = sticker.title.ifBlank { "Countdown" },
                        color = Color.White.copy(alpha = 0.7f),
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    var timeLeft by remember { mutableStateOf("") }
                    LaunchedEffect(sticker.targetEpochMs) {
                        while (true) {
                            val diff = sticker.targetEpochMs - System.currentTimeMillis()
                            if (diff <= 0) {
                                timeLeft = "Ended!"
                                break
                            }
                            val days = diff / (1000 * 60 * 60 * 24)
                            val hours = (diff / (1000 * 60 * 60)) % 24
                            val mins = (diff / (1000 * 60)) % 60
                            val secs = (diff / 1000) % 60
                            timeLeft = "${days}d ${hours}h ${mins}m ${secs}s"
                            kotlinx.coroutines.delay(1000L)
                        }
                    }

                    Text(
                        text = timeLeft,
                        color = BrandOrange,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        textAlign = TextAlign.Center
                    )
                }
                is StorySticker.AddYours -> {
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(BrandOrange.copy(alpha = 0.2f))
                            .padding(8.dp)
                    ) {
                        Text("📸", fontSize = 24.sp)
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = sticker.prompt.ifBlank { "Add Yours" },
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = { onAddYoursClick(sticker.prompt) },
                        colors = ButtonDefaults.buttonColors(containerColor = BrandOrange),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Add Yours", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
                is StorySticker.Link -> {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White.copy(alpha = 0.08f))
                            .clickable {
                                try {
                                    val cleanUrl = if (!sticker.url.startsWith("http://") && !sticker.url.startsWith("https://")) {
                                        "https://" + sticker.url
                                    } else sticker.url
                                    uriHandler.openUri(cleanUrl)
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Invalid link", Toast.LENGTH_SHORT).show()
                                }
                            }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(Icons.Default.Link, contentDescription = "Link", tint = BrandOrange, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = sticker.label.ifBlank { "Visit Link" },
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }
    }
}

private fun getRelativeTimeString(createdAtStr: String): String {
    return try {
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
        sdf.timeZone = TimeZone.getTimeZone("UTC")
        val date = sdf.parse(createdAtStr) ?: Date()
        val diff = System.currentTimeMillis() - date.time
        val mins = diff / (1000 * 60)
        val hours = mins / 60
        when {
            mins < 1 -> "Just now"
            mins < 60 -> "${mins}m ago"
            hours < 24 -> "${hours}h ago"
            else -> SimpleDateFormat("MMM d, h:mm a", Locale.US).format(date)
        }
    } catch (e: Exception) {
        "Active"
    }
}
