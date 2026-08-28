package com.example.diplomanexus.viewmodel

import android.app.Application
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.LinearGradient
import android.graphics.Shader
import android.util.Base64
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.diplomanexus.api.*
import com.example.diplomanexus.data.ChatSocketManager
import com.example.diplomanexus.data.SessionManager
import com.example.diplomanexus.data.local.toEntity
import java.io.ByteArrayOutputStream
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.UUID

class AppViewModel(application: Application) : AndroidViewModel(application) {

    private val api = DiplomaNexusApi.create()
    private val sessionManager = SessionManager(application)
    private val database = com.example.diplomanexus.data.local.AppDatabase.getDatabase(application)
    private val postDao = database.postDao()

    private val _currentUser = MutableStateFlow<UserDto?>(null)
    val currentUser: StateFlow<UserDto?> = _currentUser.asStateFlow()

    private val _posts = MutableStateFlow<List<PostDto>>(emptyList())
    val posts: StateFlow<List<PostDto>> = _posts.asStateFlow()

    private val _blogs = MutableStateFlow<List<BlogDto>>(emptyList())
    val blogs: StateFlow<List<BlogDto>> = _blogs.asStateFlow()

    private val _academicInfo = MutableStateFlow<AcademicInfoDto?>(null)
    val academicInfo: StateFlow<AcademicInfoDto?> = _academicInfo.asStateFlow()

    private val _searchResults = MutableStateFlow<List<UserDto>>(emptyList())
    val searchResults: StateFlow<List<UserDto>> = _searchResults.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()



    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()



    // ── DM / Messaging State ──
    private val _conversations = MutableStateFlow<List<ConversationDto>>(emptyList())
    val conversations: StateFlow<List<ConversationDto>> = _conversations.asStateFlow()

    private val _activeMessages = MutableStateFlow<List<MessageDto>>(emptyList())
    val activeMessages: StateFlow<List<MessageDto>> = _activeMessages.asStateFlow()

    private val _activeConversationId = MutableStateFlow<Int?>(null)
    val activeConversationId: StateFlow<Int?> = _activeConversationId.asStateFlow()

    private val _onlineUsers = MutableStateFlow<Set<Int>>(emptySet())
    val onlineUsers: StateFlow<Set<Int>> = _onlineUsers.asStateFlow()

    private val _typingUsers = MutableStateFlow<Map<Int, Set<Int>>>(emptyMap())
    val typingUsers: StateFlow<Map<Int, Set<Int>>> = _typingUsers.asStateFlow()

    // Target conversation to open (set by SearchScreen Message button)
    private val _pendingOpenConversationUserId = MutableStateFlow<Int?>(null)
    val pendingOpenConversationUserId: StateFlow<Int?> = _pendingOpenConversationUserId.asStateFlow()

    private val prefs = application.getSharedPreferences("diplomanexus_stories_prefs", Context.MODE_PRIVATE)
    private val gson = Gson()

    private val _closeFriends = MutableStateFlow<Set<Int>>(emptySet())
    val closeFriends: StateFlow<Set<Int>> = _closeFriends.asStateFlow()

    private val _highlights = MutableStateFlow<List<HighlightDto>>(emptyList())
    val highlights: StateFlow<List<HighlightDto>> = _highlights.asStateFlow()

    private val _marketplaceListings = MutableStateFlow<List<com.example.diplomanexus.api.MarketplaceListingDto>>(emptyList())
    val marketplaceListings: StateFlow<List<com.example.diplomanexus.api.MarketplaceListingDto>> = _marketplaceListings.asStateFlow()

    private val _notifications = MutableStateFlow<List<com.example.diplomanexus.api.NotificationDto>>(emptyList())
    val notifications: StateFlow<List<com.example.diplomanexus.api.NotificationDto>> = _notifications.asStateFlow()

    // ── Auto-Update State ──
    private val _availableUpdate = MutableStateFlow<AppVersionDto?>(null)
    val availableUpdate: StateFlow<AppVersionDto?> = _availableUpdate.asStateFlow()

    private val _downloadProgress = MutableStateFlow<Float?>(null)
    val downloadProgress: StateFlow<Float?> = _downloadProgress.asStateFlow()

    private val _isDownloading = MutableStateFlow(false)
    val isDownloading: StateFlow<Boolean> = _isDownloading.asStateFlow()

    private val _downloadError = MutableStateFlow<String?>(null)
    val downloadError: StateFlow<String?> = _downloadError.asStateFlow()

    init {
        // Load Close Friends
        val cfJson = prefs.getString("close_friends_list", "[]")
        try {
            val type = object : TypeToken<Set<Int>>() {}.type
            _closeFriends.value = gson.fromJson(cfJson, type) ?: emptySet()
        } catch (e: Exception) {
            _closeFriends.value = emptySet()
        }

        // Load Highlights
        val hlJson = prefs.getString("highlights_list", "[]")
        try {
            val type = object : TypeToken<List<HighlightDto>>() {}.type
            _highlights.value = gson.fromJson(hlJson, type) ?: emptyList()
        } catch (e: Exception) {
            _highlights.value = emptyList()
        }

        // Initial mock notifications
        _notifications.value = listOf(
            com.example.diplomanexus.api.NotificationDto("1", "like", "Rahul Kumar", null, "liked your recent project showcase.", System.currentTimeMillis() - 3600000),
            com.example.diplomanexus.api.NotificationDto("2", "comment", "Sneha Reddy", null, "commented: 'Looks amazing, can we collaborate on the codebase?'", System.currentTimeMillis() - 7200000),
            com.example.diplomanexus.api.NotificationDto("3", "follow", "Karthik Rao", null, "started following you.", System.currentTimeMillis() - 14400000)
        )

        // Check for App Updates
        checkForUpdates()

        // Auto-login if token exists
        val token = sessionManager.fetchAuthToken()
        if (token != null) {
            fetchProfile()
            fetchMarketplaceListings()
            connectChat()
        }
        
        // Observe local database for offline posts support
        viewModelScope.launch {
            postDao.getPosts().collect { localPosts ->
                _posts.value = combineWithMocks(localPosts.map { it.toDto() })
            }
        }
    }

    private fun getBearerToken(): String {
        return "Bearer ${sessionManager.fetchAuthToken() ?: ""}"
    }


    fun clearError() {
        _errorMessage.value = null
    }

    // ------------------- AUTO UPDATE -------------------

    fun checkForUpdates(context: android.content.Context? = null) {
        viewModelScope.launch {
            try {
                val response = api.getAppVersion()
                if (response.isSuccessful && response.body() != null) {
                    val update = response.body()!!
                    var currentVersionCode = 1
                    if (context != null) {
                        try {
                            val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
                            @Suppress("DEPRECATION")
                            currentVersionCode = pInfo.versionCode
                        } catch (e: Exception) {
                            Log.e("AppViewModel", "Package info error", e)
                        }
                    }
                    if (update.latestVersionCode > currentVersionCode) {
                        _availableUpdate.value = update
                    } else {
                        _availableUpdate.value = null
                    }
                }
            } catch (e: Exception) {
                Log.e("AppViewModel", "Check update error", e)
            }
        }
    }

    fun startAppUpdate(context: android.content.Context) {
        val update = _availableUpdate.value ?: return
        if (_isDownloading.value) return

        viewModelScope.launch {
            _isDownloading.value = true
            _downloadError.value = null
            _downloadProgress.value = 0f

            if (!com.example.diplomanexus.util.UpdateManager.canInstallUnknownApps(context)) {
                com.example.diplomanexus.util.UpdateManager.openInstallPermissionSettings(context)
                _isDownloading.value = false
                _downloadError.value = "Please allow installation from unknown sources in Settings and tap Update Now again."
                return@launch
            }

            val apkFile = com.example.diplomanexus.util.UpdateManager.downloadApk(
                context = context,
                downloadUrl = update.downloadUrl,
                onProgress = { progress ->
                    _downloadProgress.value = progress
                }
            )

            _isDownloading.value = false

            if (apkFile != null && apkFile.exists()) {
                com.example.diplomanexus.util.UpdateManager.installApk(context, apkFile)
            } else {
                _downloadError.value = "Failed to download update file. Please check server connection."
            }
        }
    }

    fun dismissUpdateDialog() {
        _availableUpdate.value = null
    }

    // ------------------- AUTHENTICATION -------------------

    fun sendSbtetOtp(pin: String, mobile: String, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            try {
                // Client-side direct verification straight to SBTET government servers!
                val (success, message) = com.example.diplomanexus.data.SbtetClientFetcher.sendSbtetOtp(pin, mobile)
                if (success) {
                    onResult(true, message)
                } else {
                    _errorMessage.value = message
                    onResult(false, message)
                }
            } catch (e: Exception) {
                Log.e("AppViewModel", "Send OTP error", e)
                val err = "Error contacting SBTET: ${e.localizedMessage}"
                _errorMessage.value = err
                onResult(false, err)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun verifySbtetOtpForSignUp(pin: String, mobile: String, otp: String, onResult: (VerifiedStudentDto?) -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            try {
                // Client-side direct student verification & profile extraction!
                val student = com.example.diplomanexus.data.SbtetClientFetcher.verifySbtetOtp(pin, mobile, otp)
                if (student != null) {
                    val dto = VerifiedStudentDto(
                        pin = student.pin,
                        name = student.name,
                        branch = student.branchName ?: "Diploma",
                        college = student.collegeName ?: "Polytechnic College",
                        mobile = student.phoneNumber ?: mobile.trim()
                    )
                    onResult(dto)
                } else {
                    val errorMsg = "Invalid OTP or SBTET student record not found."
                    _errorMessage.value = errorMsg
                    onResult(null)
                }
            } catch (e: Exception) {
                Log.e("AppViewModel", "Verify OTP error", e)
                _errorMessage.value = "Error contacting SBTET: ${e.localizedMessage}"
                onResult(null)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun verifyPinWithSbtet(pin: String, onResult: (VerifiedStudentDto?) -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            try {
                val response = api.verifyPin(VerifyPinRequest(pin.trim()))
                if (response.isSuccessful && response.body()?.success == true && response.body()?.student != null) {
                    onResult(response.body()!!.student)
                } else {
                    val errorMsg = response.errorBody()?.string() ?: "Unable to verify PIN with SBTET portal"
                    _errorMessage.value = parseError(errorMsg)
                    onResult(null)
                }
            } catch (e: Exception) {
                Log.e("AppViewModel", "PIN verification error", e)
                _errorMessage.value = "Network error: ${e.localizedMessage}"
                onResult(null)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun register(
        username: String,
        password: String,
        pin: String? = null,
        studentName: String? = null,
        branch: String? = null,
        collegeName: String? = null,
        mobileNumber: String? = null,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            try {
                val req = RegisterRequest(
                    username = username.trim().lowercase(),
                    password = password,
                    pin = pin?.trim(),
                    student_name = studentName,
                    branch = branch,
                    college_name = collegeName,
                    mobile_number = mobileNumber
                )
                val response = api.register(req)
                if (response.isSuccessful && response.body() != null) {
                    val authResponse = response.body()!!
                    sessionManager.saveAuthToken(authResponse.token)
                    _currentUser.value = authResponse.user
                    onSuccess()
                    fetchPosts()
                    fetchBlogs()
                    if (authResponse.user.is_verified) {
                        fetchAcademicInfo()
                    }
                } else {
                    val errorMsg = response.errorBody()?.string() ?: "Registration failed"
                    _errorMessage.value = parseError(errorMsg)
                }
            } catch (e: Exception) {
                Log.e("AppViewModel", "Registration error", e)
                _errorMessage.value = "Network error: ${e.localizedMessage}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun login(username: String, password: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            try {
                val response = api.login(AuthRequest(username, password))
                if (response.isSuccessful && response.body() != null) {
                    val authResponse = response.body()!!
                    sessionManager.saveAuthToken(authResponse.token)
                    _currentUser.value = authResponse.user
                    onSuccess()
                    // Load active data
                    fetchPosts()
                    fetchBlogs()
                    if (authResponse.user.is_verified) {
                        fetchAcademicInfo()
                    }
                } else {
                    val errorMsg = response.errorBody()?.string() ?: "Login failed"
                    _errorMessage.value = parseError(errorMsg)
                }
            } catch (e: Exception) {
                Log.e("AppViewModel", "Login error", e)
                _errorMessage.value = "Network error: ${e.localizedMessage}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun logout(onSuccess: () -> Unit) {
        disconnectChat()
        sessionManager.clearSession()
        _currentUser.value = null
        _posts.value = emptyList()
        _blogs.value = emptyList()
        _academicInfo.value = null
        _conversations.value = emptyList()
        _activeMessages.value = emptyList()
        _activeConversationId.value = null
        onSuccess()
    }

    private fun fetchProfile() {
        viewModelScope.launch {
            try {
                val response = api.getProfile(getBearerToken())
                if (response.isSuccessful && response.body() != null) {
                    val user = response.body()!!
                    _currentUser.value = user
                    // Load relevant feeds
                    fetchPosts()
                    fetchBlogs()
                    if (user.is_verified) {
                        fetchAcademicInfo()
                    }
                } else {
                    // Token expired or invalid
                    sessionManager.clearSession()
                }
            } catch (e: Exception) {
                Log.e("AppViewModel", "Fetch profile error", e)
            }
        }
    }

    // ------------------- STUDENT VERIFICATION -------------------

    fun verifyStudent(pin: String, name: String, branch: String, college: String, mobile: String, screenshot: String?, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            try {
                val request = VerificationRequest(pin, name, branch, college, mobile, screenshot)
                val response = api.verifyStudent(getBearerToken(), request)
                if (response.isSuccessful && response.body() != null) {
                    _currentUser.value = response.body()!!.user
                    fetchAcademicInfo()
                    fetchPosts() // reload posts to enable creation
                    onSuccess()
                } else {
                    val errorMsg = response.errorBody()?.string() ?: "Verification failed"
                    _errorMessage.value = parseError(errorMsg)
                }
            } catch (e: Exception) {
                Log.e("AppViewModel", "Verification error", e)
                _errorMessage.value = "Network error: ${e.localizedMessage}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun generateSbtetOtp(pin: String, phone: String, onSuccess: (String) -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            try {
                val response = api.generateSbtetOtp(getBearerToken(), SbtetOtpRequest(pin, phone))
                if (response.isSuccessful && response.body() != null) {
                    onSuccess(response.body()!!.message)
                } else {
                    val errorMsg = response.errorBody()?.string() ?: "Failed to generate OTP"
                    _errorMessage.value = parseError(errorMsg)
                }
            } catch (e: Exception) {
                Log.e("AppViewModel", "Generate OTP error", e)
                _errorMessage.value = "Network error: ${e.localizedMessage}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun verifySbtetOtp(pin: String, phone: String, otp: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            try {
                val response = api.verifySbtetOtp(getBearerToken(), SbtetOtpVerifyRequest(pin, phone, otp))
                if (response.isSuccessful && response.body() != null) {
                    _currentUser.value = response.body()!!.user
                    fetchAcademicInfo()
                    fetchPosts()
                    onSuccess()
                } else {
                    val errorMsg = response.errorBody()?.string() ?: "OTP verification failed"
                    _errorMessage.value = parseError(errorMsg)
                }
            } catch (e: Exception) {
                Log.e("AppViewModel", "Verify OTP error", e)
                _errorMessage.value = "Network error: ${e.localizedMessage}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    // ------------------- SEARCH USERS -------------------

    fun searchUsers(query: String) {
        viewModelScope.launch {
            try {
                val response = api.searchUsers(getBearerToken(), query)
                if (response.isSuccessful && response.body() != null) {
                    _searchResults.value = response.body()!!
                }
            } catch (e: Exception) {
                Log.e("AppViewModel", "Search users error", e)
            }
        }
    }

    fun followUser(targetUserId: Int, onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            try {
                val response = api.followUser(getBearerToken(), targetUserId)
                if (response.isSuccessful) {
                    fetchProfile()
                    // Update local search results state immediately to sync UI
                    _searchResults.value = _searchResults.value.map {
                        if (it.id == targetUserId) {
                            it.copy(
                                is_following = true,
                                followers_count = it.followers_count + 1
                            )
                        } else it
                    }
                    onComplete()
                }
            } catch (e: Exception) {
                Log.e("AppViewModel", "Follow user error", e)
            }
        }
    }

    fun unfollowUser(targetUserId: Int, onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            try {
                val response = api.unfollowUser(getBearerToken(), targetUserId)
                if (response.isSuccessful) {
                    fetchProfile()
                    // Update local search results state immediately to sync UI
                    _searchResults.value = _searchResults.value.map {
                        if (it.id == targetUserId) {
                            it.copy(
                                is_following = false,
                                followers_count = (it.followers_count - 1).coerceAtLeast(0)
                            )
                        } else it
                    }
                    onComplete()
                }
            } catch (e: Exception) {
                Log.e("AppViewModel", "Unfollow user error", e)
            }
        }
    }

    // ------------------- SOCIAL FEED (POSTS) -------------------

    private val seenPostsSent = mutableSetOf<Int>()

    fun fetchPosts() {
        viewModelScope.launch {
            try {
                seenPostsSent.clear()
                val response = api.getPosts(getBearerToken())
                if (response.isSuccessful && response.body() != null) {
                    val dtoList = response.body()!!
                    _posts.value = dtoList
                    // Save to local database for offline caching
                    postDao.insertPosts(dtoList.map { it.toEntity() })
                } else {
                    _posts.value = emptyList()
                }
            } catch (e: Exception) {
                Log.e("AppViewModel", "Fetch posts error", e)
                _posts.value = emptyList()
            }
        }
    }

    private fun combineWithMocks(fetchedList: List<PostDto>): List<PostDto> {
        return fetchedList
    }

    fun getMockPosts(): List<PostDto> {
        val user = _currentUser.value
        val myUsername = user?.username ?: "dev_user"
        val myStudentName = user?.student_name ?: "Diploma Student"
        val myIsVerified = user?.is_verified ?: true
        val myProfilePic = user?.profile_pic_base64

        // 1. My Mock Post (Image)
        val myMockPost = PostDto(
            id = -100,
            content = "Just finished building the new premium glassmorphic dashboard for DiplomaNexus! Everything looks so clean in dark mode. 💻🔥",
            image_base64 = generateMockImageBase64("#00D2FF", "#9D4EDD", "DiplomaNexus Dev"),
            media_url = null,
            media_type = "image",
            upload_status = "success",
            created_at = "2026-08-10T12:00:00.000Z",
            username = myUsername,
            student_name = myStudentName,
            is_verified = myIsVerified,
            profile_pic_base64 = myProfilePic,
            likes_count = 42,
            is_liked_by_me = true,
            comments = listOf(
                CommentDto(
                    id = -200,
                    content = "Looks amazing! The blur effects are super smooth.",
                    created_at = "2026-08-10T12:15:00.000Z",
                    username = "sneha_reddy",
                    student_name = "Sneha Reddy",
                    is_verified = true
                ),
                CommentDto(
                    id = -201,
                    content = "Awesome work! Can't wait to test it on my device.",
                    created_at = "2026-08-10T12:30:00.000Z",
                    username = "raj_kumar",
                    student_name = "Raj Kumar",
                    is_verified = false
                )
            )
        )

        // 2. My Mock Tweet
        val myMockTweet = PostDto(
            id = -101,
            content = "Late night coding sessions hitting different. Android + Jetpack Compose is a powerful combo for building slick UIs. 📱 #DiplomaNexus #DevLife",
            image_base64 = null,
            media_url = null,
            media_type = "tweet",
            upload_status = "success",
            created_at = "2026-08-10T11:30:00.000Z",
            username = myUsername,
            student_name = myStudentName,
            is_verified = myIsVerified,
            profile_pic_base64 = myProfilePic,
            likes_count = 12,
            is_liked_by_me = false,
            comments = emptyList()
        )

        // 3. Other user's mock post
        val otherMockPost = PostDto(
            id = -102,
            content = "Excited to share that I just completed my 5th semester internship at TCS! 🚀 Grateful for all the learnings and the amazing team support.",
            image_base64 = generateMockImageBase64("#FF54B0", "#FFD700", "Internship Complete!"),
            media_url = null,
            media_type = "image",
            upload_status = "success",
            created_at = "2026-08-10T10:00:00.000Z",
            username = "sneha_reddy",
            student_name = "Sneha Reddy",
            is_verified = true,
            profile_pic_base64 = null,
            likes_count = 156,
            is_liked_by_me = false,
            comments = listOf(
                CommentDto(
                    id = -202,
                    content = "Congratulations Sneha! Proud of you!",
                    created_at = "2026-08-10T10:15:00.000Z",
                    username = "raj_kumar",
                    student_name = "Raj Kumar",
                    is_verified = false
                )
            )
        )

        // 4. Other user's mock tweet
        val otherMockTweet = PostDto(
            id = -103,
            content = "Can someone help with C21 Schema 4th Semester Microprocessors previous year question papers? Need them urgently for exam prep! 📚",
            image_base64 = null,
            media_url = null,
            media_type = "tweet",
            upload_status = "success",
            created_at = "2026-08-10T09:00:00.000Z",
            username = "raj_kumar",
            student_name = "Raj Kumar",
            is_verified = false,
            profile_pic_base64 = null,
            likes_count = 8,
            is_liked_by_me = false,
            comments = listOf(
                CommentDto(
                    id = -203,
                    content = "Check the Academics section! I uploaded them there yesterday.",
                    created_at = "2026-08-10T09:10:00.000Z",
                    username = "sneha_reddy",
                    student_name = "Sneha Reddy",
                    is_verified = true
                )
            )
        )

        // 5. Mock active story for the story row
        val mockStory = PostDto(
            id = -104,
            content = "Morning walk at the college campus! 🏫☀️",
            image_base64 = generateMockImageBase64("#00FF87", "#60EFFF", "Campus Vibes"),
            media_url = null,
            media_type = "story",
            upload_status = "success",
            created_at = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.US).apply {
                timeZone = java.util.TimeZone.getTimeZone("UTC")
            }.format(java.util.Date()),
            username = "sneha_reddy",
            student_name = "Sneha Reddy",
            is_verified = true,
            profile_pic_base64 = null,
            likes_count = 5,
            is_liked_by_me = false,
            comments = emptyList()
        )

        return listOf(myMockPost, myMockTweet, otherMockPost, otherMockTweet, mockStory)
    }

    private fun generateMockImageBase64(color1: String, color2: String, text: String): String {
        try {
            val c1 = android.graphics.Color.parseColor(color1)
            val c2 = android.graphics.Color.parseColor(color2)
            
            val bitmap = Bitmap.createBitmap(400, 300, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            val paint = Paint()
            val gradient = LinearGradient(
                0f, 0f, 400f, 300f,
                c1, c2,
                Shader.TileMode.CLAMP
            )
            paint.shader = gradient
            canvas.drawRect(0f, 0f, 400f, 300f, paint)

            // Draw text overlay
            paint.shader = null
            paint.color = android.graphics.Color.WHITE
            paint.textSize = 28f
            paint.textAlign = Paint.Align.CENTER
            paint.isAntiAlias = true
            val xPos = canvas.width / 2f
            val yPos = (canvas.height / 2f) - ((paint.descent() + paint.ascent()) / 2f)
            canvas.drawText(text, xPos, yPos, paint)

            val stream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 85, stream)
            val bytes = stream.toByteArray()
            return "data:image/jpeg;base64," + Base64.encodeToString(bytes, Base64.DEFAULT).trim().replace("\n", "").replace("\r", "")
        } catch (e: Exception) {
            Log.e("AppViewModel", "Mock image generation failed", e)
            return ""
        }
    }

    fun createPost(content: String, imageBase64: String? = null, mediaType: String? = "image", onSuccess: () -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            try {
                val response = api.createPost(getBearerToken(), CreatePostRequest(content, imageBase64, mediaType))
                if (response.isSuccessful && response.body() != null) {
                    // Prepend new post to list
                    _posts.value = listOf(response.body()!!) + _posts.value
                    onSuccess()
                } else {
                    val errorMsg = response.errorBody()?.string() ?: "Failed to create post"
                    _errorMessage.value = parseError(errorMsg)
                }
            } catch (e: Exception) {
                Log.e("AppViewModel", "Create post error", e)
                _errorMessage.value = "Network error: ${e.localizedMessage}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun toggleLike(postId: Int) {
        if (postId < 0) {
            _posts.value = _posts.value.map { post ->
                if (post.id == postId) {
                    val newLiked = !post.is_liked_by_me
                    val newCount = if (newLiked) post.likes_count + 1 else (post.likes_count - 1).coerceAtLeast(0)
                    post.copy(
                        is_liked_by_me = newLiked,
                        likes_count = newCount
                    )
                } else post
            }
            return
        }

        viewModelScope.launch {
            try {
                val response = api.toggleLike(getBearerToken(), postId)
                if (response.isSuccessful && response.body() != null) {
                    val likeResponse = response.body()!!
                    // Update post list in-place
                    _posts.value = _posts.value.map { post ->
                        if (post.id == postId) {
                            post.copy(
                                is_liked_by_me = likeResponse.liked,
                                likes_count = likeResponse.likes_count
                            )
                        } else post
                    }
                }
            } catch (e: Exception) {
                Log.e("AppViewModel", "Like post error", e)
            }
        }
    }

    fun markPostSeen(postId: Int) {
        if (seenPostsSent.contains(postId)) return
        seenPostsSent.add(postId)
        viewModelScope.launch {
            try {
                val response = api.markPostSeen(getBearerToken(), postId)
                if (response.isSuccessful) {
                    _posts.value = _posts.value.map { post ->
                        if (post.id == postId) {
                            post.copy(is_seen = true)
                        } else post
                    }
                }
            } catch (e: Exception) {
                Log.e("AppViewModel", "Mark post seen error", e)
            }
        }
    }

    fun addComment(postId: Int, content: String) {
        if (postId < 0) {
            val user = _currentUser.value
            val newComment = CommentDto(
                id = (Math.random() * -10000).toInt(),
                content = content,
                created_at = "Just now",
                username = user?.username ?: "anonymous",
                student_name = user?.student_name ?: user?.username ?: "Anonymous",
                is_verified = user?.is_verified == true
            )
            _posts.value = _posts.value.map { post ->
                if (post.id == postId) {
                    post.copy(comments = post.comments + newComment)
                } else post
            }
            return
        }

        viewModelScope.launch {
            try {
                val response = api.addComment(getBearerToken(), postId, CreateCommentRequest(content))
                if (response.isSuccessful && response.body() != null) {
                    val newComment = response.body()!!
                    // Add comment to post
                    _posts.value = _posts.value.map { post ->
                        if (post.id == postId) {
                            post.copy(comments = post.comments + newComment)
                        } else post
                    }
                }
            } catch (e: Exception) {
                Log.e("AppViewModel", "Comment post error", e)
            }
        }
    }

    // ------------------- BLOGS -------------------

    fun fetchBlogs() {
        viewModelScope.launch {
            try {
                val response = api.getBlogs(getBearerToken())
                if (response.isSuccessful && response.body() != null) {
                    _blogs.value = response.body()!!
                }
            } catch (e: Exception) {
                Log.e("AppViewModel", "Fetch blogs error", e)
            }
        }
    }

    fun createBlog(title: String, content: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            try {
                val response = api.createBlog(getBearerToken(), CreateBlogRequest(title, content))
                if (response.isSuccessful && response.body() != null) {
                    _blogs.value = listOf(response.body()!!) + _blogs.value
                    onSuccess()
                } else {
                    val errorMsg = response.errorBody()?.string() ?: "Failed to create blog"
                    _errorMessage.value = parseError(errorMsg)
                }
            } catch (e: Exception) {
                Log.e("AppViewModel", "Create blog error", e)
                _errorMessage.value = "Network error: ${e.localizedMessage}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    // ------------------- ACADEMICS -------------------

    fun fetchAcademicInfo() {
        viewModelScope.launch {
            try {
                val response = api.getAcademicInfo(getBearerToken())
                if (response.isSuccessful && response.body() != null) {
                    _academicInfo.value = response.body()!!
                } else {
                    _academicInfo.value = AcademicInfoDto("", "", "", "", "", emptyList(), null, null)
                }
            } catch (e: Exception) {
                Log.e("AppViewModel", "Fetch academics error", e)
                _academicInfo.value = AcademicInfoDto("", "", "", "", "", emptyList(), null, null)
            }
        }
    }

    // ------------------- PROFILE MANAGEMENT -------------------

    fun updateProfile(aboutMe: String?, profilePicBase64: String?, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            try {
                val response = api.updateProfile(getBearerToken(), UpdateProfileRequest(aboutMe, profilePicBase64))
                if (response.isSuccessful && response.body() != null) {
                    _currentUser.value = response.body()!!.user
                    onSuccess()
                } else {
                    val errorMsg = response.errorBody()?.string() ?: "Failed to update profile"
                    _errorMessage.value = parseError(errorMsg)
                }
            } catch (e: Exception) {
                Log.e("AppViewModel", "Update profile error", e)
                _errorMessage.value = "Network error: ${e.localizedMessage}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun changePassword(oldPass: String, newPass: String, onSuccess: (String) -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val response = api.changePassword(getBearerToken(), ChangePasswordRequest(oldPass, newPass))
                if (response.isSuccessful && response.body() != null) {
                    onSuccess(response.body()!!.message)
                } else {
                    val errorMsg = response.errorBody()?.string() ?: "Failed to change password"
                    onError(parseError(errorMsg))
                }
            } catch (e: Exception) {
                Log.e("AppViewModel", "Change password error", e)
                onError("Network error: ${e.localizedMessage}")
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun subscribe(tier: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            try {
                val response = api.subscribe(getBearerToken(), SubscribeRequest(tier))
                if (response.isSuccessful && response.body() != null) {
                    _currentUser.value = response.body()!!.user
                    onSuccess()
                } else {
                    val errorMsg = response.errorBody()?.string() ?: "Failed to subscribe"
                    _errorMessage.value = parseError(errorMsg)
                }
            } catch (e: Exception) {
                Log.e("AppViewModel", "Subscribe error", e)
                _errorMessage.value = "Network error: ${e.localizedMessage}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    // Helper to parse express-validator or generic errors
    private fun parseError(rawJson: String): String {
        return try {
            val parser = com.google.gson.JsonParser.parseString(rawJson)
            if (parser.isJsonObject) {
                val obj = parser.asJsonObject
                if (obj.has("error")) {
                    obj.get("error").asString
                } else if (obj.has("message")) {
                    obj.get("message").asString
                } else {
                    rawJson
                }
            } else {
                rawJson
            }
        } catch (e: Exception) {
            rawJson
        }
    }

    // ------------------- DM / MESSAGING -------------------

    private fun connectChat() {
        val token = sessionManager.fetchAuthToken() ?: return
        ChatSocketManager.connect(token)

        // Collect new messages
        viewModelScope.launch {
            ChatSocketManager.newMessageFlow().collect { message ->
                // If the message belongs to the currently open conversation, append it
                if (message.room_id == _activeConversationId.value) {
                    _activeMessages.value = _activeMessages.value + message
                    // Mark as read since we're viewing it
                    ChatSocketManager.markRead(message.room_id)
                } else {
                    val senderName = _conversations.value.find { it.id == message.room_id }?.other_student_name ?: _conversations.value.find { it.id == message.room_id }?.other_username ?: "Someone"
                    val senderAvatar = _conversations.value.find { it.id == message.room_id }?.other_profile_pic_base64
                    addNotification("message", senderName, senderAvatar, message.text_content)
                }
                // Update conversation list with latest message
                updateConversationLastMessage(message)
            }
        }

        // Collect online/offline events
        viewModelScope.launch {
            ChatSocketManager.userOnlineFlow().collect { userId ->
                _onlineUsers.value = _onlineUsers.value + userId
            }
        }
        viewModelScope.launch {
            ChatSocketManager.userOfflineFlow().collect { userId ->
                _onlineUsers.value = _onlineUsers.value - userId
            }
        }

        // Collect typing events
        viewModelScope.launch {
            ChatSocketManager.typingFlow().collect { event ->
                val current = _typingUsers.value.toMutableMap()
                val roomTypers = current.getOrDefault(event.room_id, emptySet()).toMutableSet()
                roomTypers.add(event.user_id)
                current[event.room_id] = roomTypers
                _typingUsers.value = current
            }
        }
        viewModelScope.launch {
            ChatSocketManager.stopTypingFlow().collect { event ->
                val current = _typingUsers.value.toMutableMap()
                val roomTypers = current.getOrDefault(event.room_id, emptySet()).toMutableSet()
                roomTypers.remove(event.user_id)
                current[event.room_id] = roomTypers
                _typingUsers.value = current
            }
        }

        // Collect read receipts
        viewModelScope.launch {
            ChatSocketManager.messagesReadFlow().collect { event ->
                if (event.room_id == _activeConversationId.value) {
                    _activeMessages.value = _activeMessages.value.map {
                        if (it.sender_id == _currentUser.value?.id) it.copy(is_read = true) else it
                    }
                }
            }
        }
    }

    private fun disconnectChat() {
        ChatSocketManager.disconnect()
    }

    fun fetchConversations() {
        viewModelScope.launch {
            try {
                val response = api.getConversations(getBearerToken())
                if (response.isSuccessful && response.body() != null) {
                    _conversations.value = response.body()!!
                }
            } catch (e: Exception) {
                Log.e("AppViewModel", "Fetch conversations error", e)
            }
        }
    }

    fun openConversation(targetUserId: Int, onRoomReady: (Int) -> Unit = {}) {
        viewModelScope.launch {
            try {
                val response = api.createConversation(getBearerToken(), CreateConversationRequest(targetUserId))
                if (response.isSuccessful && response.body() != null) {
                    val conversation = response.body()!!
                    _activeConversationId.value = conversation.id
                    ChatSocketManager.joinRoom(conversation.id)
                    fetchMessages(conversation.id)
                    // Add to conversations list if not already there
                    if (_conversations.value.none { it.id == conversation.id }) {
                        _conversations.value = listOf(conversation) + _conversations.value
                    }
                    onRoomReady(conversation.id)
                }
            } catch (e: Exception) {
                Log.e("AppViewModel", "Open conversation error", e)
            }
        }
    }

    fun fetchMessages(roomId: Int, beforeId: Int? = null) {
        viewModelScope.launch {
            try {
                val response = api.getMessages(getBearerToken(), roomId, beforeId)
                if (response.isSuccessful && response.body() != null) {
                    val messages = response.body()!!
                    if (beforeId != null) {
                        // Prepend older messages
                        _activeMessages.value = messages + _activeMessages.value
                    } else {
                        _activeMessages.value = messages
                    }
                    // Mark as read
                    ChatSocketManager.markRead(roomId)
                }
            } catch (e: Exception) {
                Log.e("AppViewModel", "Fetch messages error", e)
            }
        }
    }

    fun sendMessage(roomId: Int, content: String, messageType: String = "text") {
        ChatSocketManager.sendMessage(roomId, content, messageType)
        ChatSocketManager.emitStopTyping(roomId)
    }

    fun setActiveConversation(roomId: Int?) {
        _activeConversationId.value = roomId
        if (roomId != null) {
            fetchMessages(roomId)
        } else {
            _activeMessages.value = emptyList()
        }
    }

    fun handleTyping(roomId: Int) {
        ChatSocketManager.emitTyping(roomId)
    }

    fun handleStopTyping(roomId: Int) {
        ChatSocketManager.emitStopTyping(roomId)
    }

    fun requestOpenConversation(userId: Int) {
        _pendingOpenConversationUserId.value = userId
    }

    fun clearPendingConversation() {
        _pendingOpenConversationUserId.value = null
    }

    fun toggleCloseFriend(userId: Int) {
        val current = _closeFriends.value.toMutableSet()
        if (current.contains(userId)) {
            current.remove(userId)
        } else {
            current.add(userId)
        }
        _closeFriends.value = current
        prefs.edit().putString("close_friends_list", gson.toJson(current)).apply()
    }

    fun isCloseFriend(userId: Int): Boolean {
        return _closeFriends.value.contains(userId)
    }

    fun addHighlight(name: String, storyIds: List<Int>) {
        val current = _highlights.value.toMutableList()
        val coverImage = _posts.value.find { storyIds.contains(it.id) }?.image_base64 ?: _posts.value.find { storyIds.contains(it.id) }?.media_url
        val newHl = HighlightDto(
            id = UUID.randomUUID().toString(),
            name = name,
            coverImage = coverImage,
            storyIds = storyIds
        )
        current.add(newHl)
        _highlights.value = current
        prefs.edit().putString("highlights_list", gson.toJson(current)).apply()
    }

    fun removeHighlight(highlightId: String) {
        val current = _highlights.value.filter { it.id != highlightId }
        _highlights.value = current
        prefs.edit().putString("highlights_list", gson.toJson(current)).apply()
    }

    // ─── Marketplace API Controllers ─────────────────────────────────────────

    fun fetchMarketplaceListings() {
        viewModelScope.launch {
            try {
                val response = api.getMarketplaceListings(getBearerToken())
                if (response.isSuccessful && response.body() != null) {
                    _marketplaceListings.value = response.body()!!
                }
            } catch (e: Exception) {
                Log.e("AppViewModel", "Fetch marketplace listings error", e)
            }
        }
    }

    fun createMarketplaceListing(
        title: String,
        description: String?,
        price: String?,
        category: String?,
        image_base64: String?,
        listing_type: String,
        onComplete: () -> Unit
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val req = com.example.diplomanexus.api.CreateMarketplaceRequest(
                    title = title,
                    description = description,
                    price = price,
                    category = category,
                    image_base64 = image_base64,
                    listing_type = listing_type
                )
                val response = api.createMarketplaceListing(getBearerToken(), req)
                if (response.isSuccessful && response.body() != null) {
                    _marketplaceListings.value = listOf(response.body()!!) + _marketplaceListings.value
                    onComplete()
                }
            } catch (e: Exception) {
                Log.e("AppViewModel", "Create marketplace listing error", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun updateMarketplaceStatus(listingId: Int, status: String) {
        viewModelScope.launch {
            try {
                val req = com.example.diplomanexus.api.UpdateMarketplaceStatusRequest(status)
                val response = api.updateMarketplaceStatus(getBearerToken(), listingId, req)
                if (response.isSuccessful && response.body() != null) {
                    _marketplaceListings.value = _marketplaceListings.value.map {
                        if (it.id == listingId) response.body()!! else it
                    }
                }
            } catch (e: Exception) {
                Log.e("AppViewModel", "Update marketplace status error", e)
            }
        }
    }

    fun contactSellerOrPoster(targetUserId: Int, starterText: String, onComplete: () -> Unit) {
        viewModelScope.launch {
            try {
                val response = api.createConversation(getBearerToken(), com.example.diplomanexus.api.CreateConversationRequest(targetUserId))
                if (response.isSuccessful && response.body() != null) {
                    val room = response.body()!!
                    if (!_conversations.value.any { it.id == room.id }) {
                        _conversations.value = listOf(room) + _conversations.value
                    }
                    sendMessage(room.id, starterText, "text")
                    _activeConversationId.value = room.id
                    onComplete()
                }
            } catch (e: Exception) {
                Log.e("AppViewModel", "Failed to contact listing creator", e)
            }
        }
    }

    // ─── Notification Engine ──────────────────────────────────────────────────

    fun addNotification(type: String, senderName: String, senderAvatar: String?, extraText: String?) {
        val list = _notifications.value.toMutableList()
        val newNotif = com.example.diplomanexus.api.NotificationDto(
            id = UUID.randomUUID().toString(),
            type = type,
            senderName = senderName,
            senderAvatar = senderAvatar,
            extraText = extraText,
            timestamp = System.currentTimeMillis()
        )
        list.add(0, newNotif)
        _notifications.value = list
    }

    fun markAllNotificationsRead() {
        _notifications.value = _notifications.value.map { it.copy(isRead = true) }
    }

    private fun updateConversationLastMessage(message: MessageDto) {
        _conversations.value = _conversations.value.map { conv ->
            if (conv.id == message.room_id) {
                conv.copy(
                    last_message = message.text_content,
                    last_message_time = message.created_at,
                    last_message_type = message.message_type,
                    unread_count = if (message.room_id == _activeConversationId.value) 0
                                  else conv.unread_count + 1
                )
            } else conv
        }.sortedByDescending { it.last_message_time }
    }
}
