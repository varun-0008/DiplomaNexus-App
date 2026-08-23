package com.example.diplomanexus.ui.screens

import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.PickVisualMediaRequest
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.diplomanexus.api.MarketplaceListingDto
import com.example.diplomanexus.theme.*
import com.example.diplomanexus.utils.ImageCompressor
import com.example.diplomanexus.viewmodel.AppViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MarketplaceScreen(
    viewModel: AppViewModel,
    onNavigateToChats: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    androidx.activity.compose.BackHandler(onBack = onBack)

    val listings by viewModel.marketplaceListings.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    var selectedTab by remember { mutableStateOf("product") } // "product" (Classifieds) or "gig" (Freelance)
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("All") }
    
    var showCreateSheet by remember { mutableStateOf(false) }
    var activeDetailsListing by remember { mutableStateOf<MarketplaceListingDto?>(null) }

    LaunchedEffect(Unit) {
        viewModel.fetchMarketplaceListings()
    }

    val categories = if (selectedTab == "product") {
        listOf("All", "Textbooks", "Electronics", "Dorm Gear", "Others")
    } else {
        listOf("All", "Writing", "Researching", "Copying", "Project Completion", "A Task")
    }

    val filteredListings = remember(listings, selectedTab, searchQuery, selectedCategory) {
        listings.filter {
            it.listing_type == selectedTab &&
            (selectedCategory == "All" || it.category == selectedCategory) &&
            (it.title.contains(searchQuery, ignoreCase = true) || 
             (it.description?.contains(searchQuery, ignoreCase = true) == true))
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Campus Marketplace", fontWeight = FontWeight.Bold, color = TextPrimary) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DeepDark),
                actions = {
                    IconButton(onClick = { viewModel.fetchMarketplaceListings() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = TextPrimary)
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showCreateSheet = true },
                containerColor = BrandOrange,
                contentColor = Color.White,
                shape = CircleShape
            ) {
                Icon(Icons.Default.Add, contentDescription = "List Item")
            }
        },
        containerColor = DeepDark,
        modifier = modifier.fillMaxSize()
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Segmented Tab Switcher (Products vs Gigs)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .height(44.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(CardDark)
                    .padding(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                listOf(
                    Pair("product", "Books & Goods 📦"),
                    Pair("gig", "Freelance Gigs 💼")
                ).forEach { tab ->
                    val isSel = selectedTab == tab.first
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(18.dp))
                            .background(if (isSel) Color.White.copy(alpha = 0.1f) else Color.Transparent)
                            .border(if (isSel) BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)) else BorderStroke(0.dp, Color.Transparent), RoundedCornerShape(18.dp))
                            .clickable {
                                selectedTab = tab.first
                                selectedCategory = "All"
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = tab.second,
                            color = if (isSel) Color.White else TextSecondary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }
            }

            // Search input field
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search listings...", color = TextSecondary, fontSize = 13.sp) },
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
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            )

            // Categories horizontal picker list
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                categories.forEach { cat ->
                    val isSel = selectedCategory == cat
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (isSel) BrandOrange.copy(0.15f) else Color.White.copy(alpha = 0.03f))
                            .border(1.dp, if (isSel) BrandOrange else Color.White.copy(alpha = 0.1f), RoundedCornerShape(14.dp))
                            .clickable { selectedCategory = cat }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = cat,
                            color = if (isSel) BrandOrange else TextSecondary,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp
                        )
                    }
                }
            }

            // Grid content
            if (filteredListings.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Storefront, contentDescription = null, tint = TextTertiary, modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = if (selectedTab == "product") "No books or goods listed yet." else "No freelance gigs posted yet.",
                            color = TextSecondary,
                            fontSize = 14.sp
                        )
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(filteredListings) { listing ->
                        ListingCard(listing = listing, onClick = { activeDetailsListing = listing })
                    }
                }
            }
        }
    }

    // Listing Details Modal/Dialog
    if (activeDetailsListing != null) {
        ListingDetailsDialog(
            listing = activeDetailsListing!!,
            currentUser = currentUser,
            onDismiss = { activeDetailsListing = null },
            onUpdateStatus = { status ->
                viewModel.updateMarketplaceStatus(activeDetailsListing!!.id, status)
                activeDetailsListing = activeDetailsListing!!.copy(status = status)
            },
            onMessageClick = {
                val introMsg = if (activeDetailsListing!!.listing_type == "product") {
                    "Hi! I'm interested in buying your product: '${activeDetailsListing!!.title}' listed for ${activeDetailsListing!!.price ?: "Free"}!"
                } else {
                    "Hi! I'd like to apply/assist with your freelance gig: '${activeDetailsListing!!.title}' (Budget: ${activeDetailsListing!!.price ?: "TBD"})!"
                }
                viewModel.contactSellerOrPoster(activeDetailsListing!!.user_id, introMsg) {
                    activeDetailsListing = null
                    onNavigateToChats()
                }
            }
        )
    }

    // Create Sheet
    if (showCreateSheet) {
        CreateListingSheet(
            selectedType = selectedTab,
            onDismiss = { showCreateSheet = false },
            onSubmit = { title, desc, price, cat, type, base64 ->
                viewModel.createMarketplaceListing(title, desc, price, cat, base64, type) {
                    showCreateSheet = false
                }
            },
            categories = categories.filter { it != "All" },
            isLoading = isLoading
        )
    }
}

@Composable
fun ListingCard(
    listing: MarketplaceListingDto,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardDark),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .background(Color.White.copy(0.02f)),
                contentAlignment = Alignment.Center
            ) {
                if (listing.image_base64 != null) {
                    val clean = listing.image_base64.substringAfter("base64,")
                    val bytes = Base64.decode(clean, Base64.DEFAULT)
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
                    // Gig/Icon Placeholder
                    Icon(
                        imageVector = if (listing.listing_type == "gig") Icons.Default.WorkOutline else Icons.Default.Inventory2,
                        contentDescription = null,
                        tint = TextTertiary,
                        modifier = Modifier.size(36.dp)
                    )
                }
                
                // Status Badge overlay
                val statusColor = when (listing.status.lowercase()) {
                    "available", "open" -> Color(0xFF2E7D32)
                    "pending" -> Color(0xFFE65100)
                    else -> Color(0xFFC62828)
                }
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(statusColor)
                        .padding(horizontal = 6.dp, vertical = 2.5.dp)
                ) {
                    Text(
                        text = listing.status.uppercase(),
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 8.sp
                    )
                }
            }

            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = listing.title,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = listing.price?.ifBlank { "TBD" } ?: "TBD",
                        color = BrandOrange,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 12.sp
                    )
                    Text(
                        text = listing.category ?: "Others",
                        color = TextSecondary,
                        fontSize = 10.sp
                    )
                }
            }
        }
    }
}

@Composable
fun ListingDetailsDialog(
    listing: MarketplaceListingDto,
    currentUser: com.example.diplomanexus.api.UserDto?,
    onDismiss: () -> Unit,
    onUpdateStatus: (String) -> Unit,
    onMessageClick: () -> Unit
) {
    val isMyListing = currentUser?.id == listing.user_id

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = listing.title, fontWeight = FontWeight.Bold, color = TextPrimary) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Listing Media / Banner
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White.copy(0.03f)),
                    contentAlignment = Alignment.Center
                ) {
                    if (listing.image_base64 != null) {
                        val clean = listing.image_base64.substringAfter("base64,")
                        val bytes = Base64.decode(clean, Base64.DEFAULT)
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
                        Icon(
                            imageVector = if (listing.listing_type == "gig") Icons.Default.WorkOutline else Icons.Default.Inventory2,
                            contentDescription = null,
                            tint = TextTertiary,
                            modifier = Modifier.size(48.dp)
                        )
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        Text("Price / Budget", color = TextSecondary, fontSize = 11.sp)
                        Text(listing.price?.ifBlank { "TBD" } ?: "TBD", color = BrandOrange, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Category", color = TextSecondary, fontSize = 11.sp)
                        Text(listing.category ?: "Others", color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    }
                }

                HorizontalDivider(color = BorderColor.copy(alpha = 0.4f))

                Column {
                    Text("Description", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = listing.description?.ifBlank { "No description provided." } ?: "No description provided.",
                        color = TextPrimary,
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )
                }

                HorizontalDivider(color = BorderColor.copy(alpha = 0.4f))

                // User row
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AvatarView(base64 = listing.profile_pic_base64, name = listing.student_name ?: listing.username, size = 32.dp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(listing.student_name ?: listing.username, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Text("Listing Owner", color = TextSecondary, fontSize = 10.sp)
                    }
                }

                // If owner, show Status Toggles!
                if (isMyListing) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("Update Listing Status:", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    val options = if (listing.listing_type == "product") {
                        listOf("available", "pending", "sold")
                    } else {
                        listOf("open", "filled")
                    }
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        options.forEach { opt ->
                            val isSel = listing.status.lowercase() == opt
                            val btnBg = when {
                                !isSel -> Color.White.copy(0.04f)
                                opt == "available" || opt == "open" -> Color(0xFF2E7D32).copy(0.2f)
                                opt == "pending" -> Color(0xFFE65100).copy(0.2f)
                                else -> Color(0xFFC62828).copy(0.2f)
                            }
                            val btnBorder = when {
                                !isSel -> Color.White.copy(0.1f)
                                opt == "available" || opt == "open" -> Color(0xFF2E7D32)
                                opt == "pending" -> Color(0xFFE65100)
                                else -> Color(0xFFC62828)
                            }

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(btnBg)
                                    .border(1.dp, btnBorder, RoundedCornerShape(8.dp))
                                    .clickable { onUpdateStatus(opt) }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(opt.uppercase(), color = if (isSel) Color.White else TextSecondary, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (!isMyListing) {
                Button(
                    onClick = onMessageClick,
                    colors = ButtonDefaults.buttonColors(containerColor = BrandOrange)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Chat, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (listing.listing_type == "product") "Message Seller" else "Apply for Gig",
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = TextSecondary)
            }
        },
        containerColor = CardDark,
        shape = RoundedCornerShape(24.dp)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateListingSheet(
    selectedType: String,
    onDismiss: () -> Unit,
    onSubmit: (title: String, desc: String, price: String, category: String, type: String, base64: String?) -> Unit,
    categories: List<String>,
    isLoading: Boolean
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }
    var category by remember { mutableStateOf(categories.firstOrNull() ?: "Others") }
    var listingType by remember { mutableStateOf(selectedType) } // "product" or "gig"
    
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var imageBase64 by remember { mutableStateOf<String?>(null) }

    val context = LocalContext.current
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            imageUri = uri
            imageBase64 = ImageCompressor.compressImageFromUri(context, uri)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (listingType == "product") "New Item Listing 📦" else "Post Freelance Gig 💼",
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(380.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Type Switcher inside sheet
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(36.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(Color.White.copy(0.04f))
                        .padding(2.dp)
                ) {
                    listOf("product" to "Books & Goods", "gig" to "Freelance Gig").forEach { t ->
                        val isSel = listingType == t.first
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(16.dp))
                                .background(if (isSel) Color.White.copy(0.08f) else Color.Transparent)
                                .clickable {
                                    listingType = t.first
                                    category = categories.firstOrNull() ?: "Others"
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(t.second, color = if (isSel) Color.White else TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    placeholder = { Text("Title (e.g. Calculus Book)", color = TextSecondary) },
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary, focusedBorderColor = BrandOrange, unfocusedBorderColor = BorderColor),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = price,
                    onValueChange = { price = it },
                    placeholder = { Text(if (listingType == "product") "Price (e.g. $15)" else "Budget (e.g. $25/hr)", color = TextSecondary) },
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary, focusedBorderColor = BrandOrange, unfocusedBorderColor = BorderColor),
                    modifier = Modifier.fillMaxWidth()
                )

                // Category dropdown simulation
                var expanded by remember { mutableStateOf(false) }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White.copy(0.03f))
                        .border(1.dp, BorderColor, RoundedCornerShape(12.dp))
                        .clickable { expanded = true }
                        .padding(12.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Category: $category", color = TextPrimary, fontSize = 13.sp)
                        Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = TextSecondary)
                    }
                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false },
                        modifier = Modifier.background(CardDark)
                    ) {
                        categories.forEach { cat ->
                            DropdownMenuItem(
                                text = { Text(cat, color = TextPrimary) },
                                onClick = {
                                    category = cat
                                    expanded = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    placeholder = { Text("Description details...", color = TextSecondary) },
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary, focusedBorderColor = BrandOrange, unfocusedBorderColor = BorderColor),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                )

                // Image preview / upload row (Only relevant for products, optional for Gigs)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Button(
                        onClick = {
                            photoPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(0.06f)),
                        modifier = Modifier.height(36.dp)
                    ) {
                        Icon(Icons.Default.AddAPhoto, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Photo", fontSize = 11.sp)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    if (imageUri != null) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(6.dp))
                        ) {
                            AsyncImage(model = imageUri, contentDescription = null, contentScale = ContentScale.Crop)
                        }
                    } else {
                        Text("No photo attached", color = TextSecondary, fontSize = 11.sp)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank()) {
                        onSubmit(title, description, price, category, listingType, imageBase64)
                    }
                },
                enabled = title.isNotBlank() && !isLoading,
                colors = ButtonDefaults.buttonColors(containerColor = BrandOrange)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(18.dp))
                } else {
                    Text("Publish", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isLoading) {
                Text("Cancel", color = TextSecondary)
            }
        },
        containerColor = CardDark,
        shape = RoundedCornerShape(24.dp)
    )
}
