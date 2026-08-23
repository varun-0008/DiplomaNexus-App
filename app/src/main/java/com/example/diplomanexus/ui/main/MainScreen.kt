package com.example.diplomanexus.ui.main

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import com.example.diplomanexus.theme.*
import com.example.diplomanexus.ui.screens.AcademicsScreen
import com.example.diplomanexus.ui.screens.FeedScreen
import com.example.diplomanexus.ui.screens.CreatePostScreen
import com.example.diplomanexus.ui.screens.ProfileScreen
import com.example.diplomanexus.ui.screens.SearchScreen
import com.example.diplomanexus.ui.screens.MessagesScreen
import com.example.diplomanexus.ui.screens.AvatarView
import com.example.diplomanexus.ui.dialogs.UpdateDialog
import com.example.diplomanexus.viewmodel.AppViewModel
import kotlinx.coroutines.launch

@Composable
fun MainScreen(
    viewModel: AppViewModel,
    onNavigateToVerify: () -> Unit,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableStateOf(0) }
    val pagerState = rememberPagerState(initialPage = 1) { 6 }
    val coroutineScope = rememberCoroutineScope()
    val currentUser by viewModel.currentUser.collectAsState()
    val activeConversationId by viewModel.activeConversationId.collectAsState()
    var currentScreenOverlay by remember { mutableStateOf<String?>(null) }

    // Auto update states
    val availableUpdate by viewModel.availableUpdate.collectAsState()
    val downloadProgress by viewModel.downloadProgress.collectAsState()
    val isDownloading by viewModel.isDownloading.collectAsState()
    val downloadError by viewModel.downloadError.collectAsState()

    val context = LocalContext.current
    val activity = remember(context) {
        var ctx = context
        while (ctx is android.content.ContextWrapper) {
            if (ctx is android.app.Activity) {
                return@remember ctx
            }
            ctx = ctx.baseContext
        }
        null
    }

    androidx.activity.compose.BackHandler(
        enabled = currentScreenOverlay == null
    ) {
        if (selectedTab == 3 && activeConversationId != null) {
            viewModel.setActiveConversation(null)
        } else if (pagerState.currentPage != 1) {
            coroutineScope.launch { pagerState.animateScrollToPage(1) }
        } else {
            activity?.moveTaskToBack(true)
        }
    }

    // Sync pager and selectedTab
    LaunchedEffect(pagerState.settledPage) {
        if (pagerState.settledPage >= 1) {
            selectedTab = pagerState.settledPage - 1
        }
    }

    LaunchedEffect(selectedTab) {
        if (pagerState.currentPage != selectedTab + 1) {
            pagerState.animateScrollToPage(selectedTab + 1)
        }
    }

    val tabs = listOf(
        Pair("Feed", Icons.Default.Home),
        Pair("Academics", Icons.Default.School),
        Pair("Search", Icons.Default.Search),
        Pair("Messages", Icons.Default.Send),
        Pair("Profile", Icons.Default.Person)
    )

    val showBottomBar = pagerState.currentPage > 0 && 
            !(selectedTab == 3 && activeConversationId != null)

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = DeepDark
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(DeepDark)
                .padding(top = paddingValues.calculateTopPadding())
        ) {
            // Screen contents
            HorizontalPager(
                state = pagerState,
                userScrollEnabled = pagerState.currentPage != 4 || activeConversationId == null,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                when (page) {
                    0 -> CreatePostScreen(
                        viewModel = viewModel,
                        onClose = {
                            coroutineScope.launch { pagerState.animateScrollToPage(1) }
                        }
                    )
                    1 -> FeedScreen(
                        viewModel = viewModel,
                        onNavigateToVerify = onNavigateToVerify,
                        onOpenCreatePost = {
                            coroutineScope.launch { pagerState.animateScrollToPage(0) }
                        },
                        onOpenNotifications = { currentScreenOverlay = "notifications" },
                        onOpenMarketplace = { currentScreenOverlay = "marketplace" }
                    )
                    2 -> AcademicsScreen(
                        viewModel = viewModel,
                        onNavigateToVerify = onNavigateToVerify,
                        onOpenNotifications = { currentScreenOverlay = "notifications" },
                        onOpenMarketplace = { currentScreenOverlay = "marketplace" }
                    )
                    3 -> SearchScreen(
                        viewModel = viewModel,
                        onStartChat = { userId ->
                            viewModel.requestOpenConversation(userId)
                            selectedTab = 3
                        },
                        onOpenNotifications = { currentScreenOverlay = "notifications" },
                        onOpenMarketplace = { currentScreenOverlay = "marketplace" }
                    )
                    4 -> MessagesScreen(
                        viewModel = viewModel
                    )
                    5 -> ProfileScreen(
                        viewModel = viewModel,
                        onOpenSettings = { currentScreenOverlay = "settings" },
                        onNavigateToVerify = onNavigateToVerify,
                        onOpenNotifications = { currentScreenOverlay = "notifications" },
                        onOpenMarketplace = { currentScreenOverlay = "marketplace" }
                    )
                }
            }

            // Liquid Glass Floating Island Navbar
            if (showBottomBar) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 20.dp, vertical = 20.dp)
                ) {
                    LiquidGlassNavBar(
                        tabs = tabs,
                        selectedTab = selectedTab,
                        currentUser = currentUser,
                        viewModel = viewModel,
                        onTabSelected = { index ->
                            selectedTab = index
                        }
                    )
                }
            }

            // Overlays
            if (currentScreenOverlay == "notifications") {
                com.example.diplomanexus.ui.screens.NotificationsScreen(
                    viewModel = viewModel,
                    onBack = { currentScreenOverlay = null }
                )
            } else if (currentScreenOverlay == "marketplace") {
                com.example.diplomanexus.ui.screens.MarketplaceScreen(
                    viewModel = viewModel,
                    onNavigateToChats = {
                        currentScreenOverlay = null
                        selectedTab = 3
                    },
                    onBack = { currentScreenOverlay = null }
                )
            } else if (currentScreenOverlay == "settings") {
                com.example.diplomanexus.ui.screens.SettingsScreen(
                    viewModel = viewModel,
                    onBack = { currentScreenOverlay = null },
                    onOpenCloseFriends = { currentScreenOverlay = "close_friends" },
                    onNavigateToVerify = onNavigateToVerify,
                    onLogout = onLogout
                )
            } else if (currentScreenOverlay == "close_friends") {
                com.example.diplomanexus.ui.screens.CloseFriendsScreen(
                    viewModel = viewModel,
                    onBack = { currentScreenOverlay = "settings" }
                )
            }

            // Auto-Update Prompt Dialog
            if (availableUpdate != null) {
                UpdateDialog(
                    updateInfo = availableUpdate!!,
                    downloadProgress = downloadProgress,
                    isDownloading = isDownloading,
                    downloadError = downloadError,
                    onUpdateClick = { viewModel.startAppUpdate(context) },
                    onDismissRequest = { viewModel.dismissUpdateDialog() }
                )
            }
        }
    }
}

@Composable
fun LiquidGlassNavBar(
    tabs: List<Pair<String, androidx.compose.ui.graphics.vector.ImageVector>>,
    selectedTab: Int,
    currentUser: com.example.diplomanexus.api.UserDto?,
    viewModel: AppViewModel,
    onTabSelected: (Int) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp),
        shape = RoundedCornerShape(36.dp),
        colors = CardDefaults.cardColors(containerColor = CardDark.copy(alpha = 0.95f)),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                tabs.forEachIndexed { index, tab ->
                    val isSelected = selectedTab == index
                    val targetId = when (index) {
                        0 -> "feed_tab"
                        1 -> "academics_tab"
                        2 -> "search_tab"
                        3 -> "messages_tab"
                        4 -> "profile_tab"
                        else -> null
                    }
                    
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clickable(
                                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                                indication = null
                            ) {
                                onTabSelected(index)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        if (isSelected) {
                            // Active Tab Highlight: Solid Orange Circle
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(BrandOrange),
                                contentAlignment = Alignment.Center
                            ) {
                                if (index == 4) {
                                    AvatarView(
                                        base64 = currentUser?.profile_pic_base64,
                                        name = currentUser?.student_name ?: currentUser?.username ?: "P",
                                        size = 28.dp
                                    )
                                } else {
                                    Icon(
                                        imageVector = tab.second,
                                        contentDescription = tab.first,
                                        tint = Color.White,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                        } else {
                            // Inactive Tab: Gray Outline
                            if (index == 4) {
                                AvatarView(
                                    base64 = currentUser?.profile_pic_base64,
                                    name = currentUser?.student_name ?: currentUser?.username ?: "P",
                                    size = 24.dp
                                )
                            } else {
                                Icon(
                                    imageVector = tab.second,
                                    contentDescription = tab.first,
                                    tint = TextSecondary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
