package com.example.diplomanexus.api

// Auth DTOs
data class AuthRequest(
    val username: String,
    val password: String
)

data class RegisterRequest(
    val username: String,
    val password: String,
    val pin: String? = null,
    val student_name: String? = null,
    val branch: String? = null,
    val college_name: String? = null,
    val mobile_number: String? = null
)

data class VerifyPinRequest(
    val pin: String
)

data class SendSbtetOtpRequest(
    val pin: String,
    val mobile: String
)

data class SendSbtetOtpResponse(
    val success: Boolean,
    val message: String
)

data class VerifySbtetOtpRequest(
    val pin: String,
    val mobile: String,
    val otp: String
)

data class VerifiedStudentDto(
    val pin: String,
    val name: String,
    val branch: String,
    val college: String,
    val mobile: String? = ""
)

data class VerifyPinResponse(
    val success: Boolean,
    val student: VerifiedStudentDto
)

data class AuthResponse(
    val token: String,
    val user: UserDto
)

data class UserDto(
    val id: Int,
    val username: String,
    val pin: String?,
    val student_name: String?,
    val branch: String?,
    val college_name: String?,
    val mobile_number: String?,
    val is_verified: Boolean,
    val about_me: String?,
    val profile_pic_base64: String?,
    val subscription_tier: String,
    val followers_count: Int = 0,
    val following_count: Int = 0,
    val friends_count: Int = 0,
    val is_following: Boolean = false
)

// Verification DTO
data class VerificationRequest(
    val pin: String,
    val student_name: String,
    val branch: String,
    val college_name: String,
    val mobile_number: String,
    val screenshot: String? = null
)

data class VerificationResponse(
    val message: String,
    val user: UserDto
)

// Feed DTOs
data class PostDto(
    val id: Int,
    val content: String,
    val image_base64: String?,
    val media_url: String?,
    val media_type: String?,
    val upload_status: String?,
    val created_at: String,
    val username: String,
    val student_name: String?,
    val is_verified: Boolean,
    val profile_pic_base64: String?,
    var likes_count: Int,
    var is_liked_by_me: Boolean,
    val comments: List<CommentDto>,
    val is_seen: Boolean = false,
    val branch: String? = null
)

data class CreatePostRequest(
    val content: String,
    val image_base64: String?,
    val media_type: String?
)

data class LikeResponse(
    val liked: Boolean,
    val likes_count: Int
)

data class CommentDto(
    val id: Int,
    val content: String,
    val created_at: String,
    val username: String,
    val student_name: String?,
    val is_verified: Boolean,
    val branch: String? = null
)

data class CreateCommentRequest(
    val content: String
)

// Blog DTOs
data class BlogDto(
    val id: Int,
    val title: String,
    val content: String,
    val created_at: String,
    val username: String,
    val student_name: String?,
    val is_verified: Boolean,
    val profile_pic_base64: String?
)

data class CreateBlogRequest(
    val title: String,
    val content: String
)

// Profile Customization
data class UpdateProfileRequest(
    val about_me: String?,
    val profile_pic_base64: String?
)

data class SubscribeRequest(
    val tier: String
)

// Academic DTO
data class SubjectMarksDto(
    val code: String,
    val name: String,
    val grade: String?,
    val mid1: Double? = null,
    val mid2: Double? = null,
    val internal: Double? = null,
    val end_sem: Double? = null,
    val total: Double? = null
)

data class SemesterInfoDto(
    val semester_number: Int,
    val attendance_percentage: Int,
    val sgpa: Double,
    val backlogs: Int,
    val cgpa: Double = 0.0,
    val subjects: List<SubjectMarksDto> = emptyList()
)

data class AttendanceSummaryDto(
    val percentage: Double,
    val workingDays: Int,
    val presentDays: Double,
    val semester: String
)

data class AttendanceLogDto(
    val date: String,
    val status: String,
    val month: String,
    val monthNum: Int,
    val day: String,
    val year: Int
)

data class AcademicInfoDto(
    val pin: String,
    val student_name: String,
    val branch: String,
    val college_name: String,
    val mobile_number: String,
    val semesters: List<SemesterInfoDto>,
    val attendance_summary: AttendanceSummaryDto?,
    val attendance_logs: List<AttendanceLogDto>?
)

data class ChangePasswordRequest(
    val old_password: String,
    val new_password: String
)

data class SimpleResponse(
    val message: String
)

// DM / Messaging DTOs
data class ConversationDto(
    val id: Int,
    val other_user_id: Int,
    val other_username: String,
    val other_student_name: String?,
    val other_profile_pic_base64: String?,
    val other_is_verified: Boolean,
    val last_message: String?,
    val last_message_time: String?,
    val last_message_type: String?,
    val unread_count: Int = 0
)

data class MessageDto(
    val id: Int,
    val room_id: Int,
    val sender_id: Int,
    val message_type: String,
    val text_content: String?,
    val media_url: String?,
    val is_read: Boolean,
    val created_at: String
)

data class CreateConversationRequest(val target_user_id: Int)
data class SendMessageRequest(val content: String, val message_type: String = "text")

data class SbtetOtpRequest(
    val pin: String,
    val phone: String
)

data class SbtetOtpVerifyRequest(
    val pin: String,
    val phone: String,
    val otp: String
)

// Stories, Highlights, & Stickers DTOs
data class HighlightDto(
    val id: String,
    val name: String,
    val coverImage: String?,
    val storyIds: List<Int>
)

sealed class StorySticker {
    data class Poll(
        val question: String,
        val options: List<String>,
        var votes: List<Int>,
        var votedIndex: Int = -1
    ) : StorySticker()

    data class Quiz(
        val question: String,
        val options: List<String>,
        val correctIndex: Int,
        var selectedIndex: Int = -1
    ) : StorySticker()

    data class Question(
        val prompt: String
    ) : StorySticker()

    data class Countdown(
        val title: String,
        val targetEpochMs: Long
    ) : StorySticker()

    data class AddYours(
        val prompt: String
    ) : StorySticker()

    data class Link(
        val url: String,
        val label: String
    ) : StorySticker()
}

data class MarketplaceListingDto(
    val id: Int,
    val user_id: Int,
    val title: String,
    val description: String?,
    val price: String?,
    val category: String?,
    val status: String,
    val image_base64: String?,
    val listing_type: String,
    val created_at: String,
    val username: String,
    val student_name: String?,
    val college_name: String? = null,
    val branch: String? = null,
    val is_verified: Boolean,
    val profile_pic_base64: String?
)

data class CreateMarketplaceRequest(
    val title: String,
    val description: String?,
    val price: String?,
    val category: String?,
    val image_base64: String?,
    val listing_type: String
)

data class UpdateMarketplaceStatusRequest(
    val status: String
)

data class NotificationDto(
    val id: String,
    val type: String, // "like", "comment", "message", "follow"
    val senderName: String,
    val senderAvatar: String?,
    val extraText: String?,
    val timestamp: Long,
    var isRead: Boolean = false
)

data class AppVersionDto(
    val latestVersionCode: Int,
    val latestVersionName: String,
    val downloadUrl: String,
    val releaseNotes: String,
    val forceUpdate: Boolean = false
)

