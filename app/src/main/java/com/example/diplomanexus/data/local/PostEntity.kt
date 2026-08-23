package com.example.diplomanexus.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import com.example.diplomanexus.api.CommentDto
import com.example.diplomanexus.api.PostDto
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

@Entity(tableName = "posts")
data class PostEntity(
    @PrimaryKey val id: Int,
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
    val likes_count: Int,
    val is_liked_by_me: Boolean,
    val comments: String, // Stored as JSON
    val is_seen: Boolean,
    val branch: String? = null
) {
    fun toDto(): PostDto {
        val listType = object : TypeToken<List<CommentDto>>() {}.type
        val commentsList: List<CommentDto> = Gson().fromJson(comments, listType) ?: emptyList()
        return PostDto(
            id = id,
            content = content,
            image_base64 = image_base64,
            media_url = media_url,
            media_type = media_type,
            upload_status = upload_status,
            created_at = created_at,
            username = username,
            student_name = student_name,
            is_verified = is_verified,
            profile_pic_base64 = profile_pic_base64,
            likes_count = likes_count,
            is_liked_by_me = is_liked_by_me,
            comments = commentsList,
            is_seen = is_seen,
            branch = branch
        )
    }
}

class Converters {
    @TypeConverter
    fun fromCommentsList(comments: List<CommentDto>?): String {
        return Gson().toJson(comments ?: emptyList<CommentDto>())
    }

    @TypeConverter
    fun toCommentsList(data: String?): List<CommentDto> {
        if (data.isNullOrEmpty()) return emptyList()
        val listType = object : TypeToken<List<CommentDto>>() {}.type
        return Gson().fromJson(data, listType) ?: emptyList()
    }
}

fun PostDto.toEntity(): PostEntity {
    return PostEntity(
        id = id,
        content = content,
        image_base64 = image_base64,
        media_url = media_url,
        media_type = media_type,
        upload_status = upload_status,
        created_at = created_at,
        username = username,
        student_name = student_name,
        is_verified = is_verified,
        profile_pic_base64 = profile_pic_base64,
        likes_count = likes_count,
        is_liked_by_me = is_liked_by_me,
        comments = Gson().toJson(comments),
        is_seen = is_seen,
        branch = branch
    )
}
