package com.example.diplomanexus.api

import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*

interface DiplomaNexusApi {

    @POST("api/auth/send-sbtet-otp")
    suspend fun sendSbtetOtp(@Body request: SendSbtetOtpRequest): Response<SendSbtetOtpResponse>

    @POST("api/auth/verify-sbtet-otp")
    suspend fun verifySbtetOtp(@Body request: VerifySbtetOtpRequest): Response<VerifyPinResponse>

    @POST("api/auth/verify-pin")
    suspend fun verifyPin(@Body request: VerifyPinRequest): Response<VerifyPinResponse>

    @POST("api/auth/register")
    suspend fun register(@Body request: RegisterRequest): Response<AuthResponse>

    @POST("api/auth/login")
    suspend fun login(@Body request: AuthRequest): Response<AuthResponse>

    @POST("api/verify")
    suspend fun verifyStudent(
        @Header("Authorization") token: String,
        @Body request: VerificationRequest
    ): Response<VerificationResponse>

    @GET("api/profile")
    suspend fun getProfile(
        @Header("Authorization") token: String
    ): Response<UserDto>

    @GET("api/users/search")
    suspend fun searchUsers(
        @Header("Authorization") token: String,
        @Query("q") query: String
    ): Response<List<UserDto>>

    @POST("api/users/{id}/follow")
    suspend fun followUser(
        @Header("Authorization") token: String,
        @Path("id") targetUserId: Int
    ): Response<SimpleResponse>

    @POST("api/users/{id}/unfollow")
    suspend fun unfollowUser(
        @Header("Authorization") token: String,
        @Path("id") targetUserId: Int
    ): Response<SimpleResponse>

    @PUT("api/profile")
    suspend fun updateProfile(
        @Header("Authorization") token: String,
        @Body request: UpdateProfileRequest
    ): Response<VerificationResponse> // Reuse verification response format as it returns user

    @POST("api/profile/change-password")
    suspend fun changePassword(
        @Header("Authorization") token: String,
        @Body request: ChangePasswordRequest
    ): Response<SimpleResponse>

    @POST("api/profile/subscribe")
    suspend fun subscribe(
        @Header("Authorization") token: String,
        @Body request: SubscribeRequest
    ): Response<VerificationResponse>

    @GET("api/academic-info")
    suspend fun getAcademicInfo(
        @Header("Authorization") token: String
    ): Response<AcademicInfoDto>

    @GET("api/posts")
    suspend fun getPosts(
        @Header("Authorization") token: String
    ): Response<List<PostDto>>

    @POST("api/posts")
    suspend fun createPost(
        @Header("Authorization") token: String,
        @Body request: CreatePostRequest
    ): Response<PostDto>

    @POST("api/posts/{id}/like")
    suspend fun toggleLike(
        @Header("Authorization") token: String,
        @Path("id") postId: Int
    ): Response<LikeResponse>

    @POST("api/posts/{id}/seen")
    suspend fun markPostSeen(
        @Header("Authorization") token: String,
        @Path("id") postId: Int
    ): Response<SimpleResponse>

    @POST("api/posts/{id}/comment")
    suspend fun addComment(
        @Header("Authorization") token: String,
        @Path("id") postId: Int,
        @Body request: CreateCommentRequest
    ): Response<CommentDto>

    @GET("api/blogs")
    suspend fun getBlogs(
        @Header("Authorization") token: String
    ): Response<List<BlogDto>>

    @POST("api/blogs")
    suspend fun createBlog(
        @Header("Authorization") token: String,
        @Body request: CreateBlogRequest
    ): Response<BlogDto>

    // DM / Conversations
    @POST("api/conversations")
    suspend fun createConversation(
        @Header("Authorization") token: String,
        @Body request: CreateConversationRequest
    ): Response<ConversationDto>

    @GET("api/conversations")
    suspend fun getConversations(
        @Header("Authorization") token: String
    ): Response<List<ConversationDto>>

    @GET("api/conversations/{roomId}/messages")
    suspend fun getMessages(
        @Header("Authorization") token: String,
        @Path("roomId") roomId: Int,
        @Query("before") beforeId: Int? = null,
        @Query("limit") limit: Int = 30
    ): Response<List<MessageDto>>
    
    // SBTET Mobile verification via SMS OTP
    @POST("api/sbtet/otp/generate")
    suspend fun generateSbtetOtp(
        @Header("Authorization") token: String,
        @Body request: SbtetOtpRequest
    ): Response<SimpleResponse>

    @POST("api/sbtet/otp/verify")
    suspend fun verifySbtetOtp(
        @Header("Authorization") token: String,
        @Body request: SbtetOtpVerifyRequest
    ): Response<VerificationResponse>

    // Marketplace Endpoints
    @GET("api/marketplace")
    suspend fun getMarketplaceListings(
        @Header("Authorization") token: String
    ): Response<List<MarketplaceListingDto>>

    @POST("api/marketplace")
    suspend fun createMarketplaceListing(
        @Header("Authorization") token: String,
        @Body request: CreateMarketplaceRequest
    ): Response<MarketplaceListingDto>

    @PUT("api/marketplace/{id}/status")
    suspend fun updateMarketplaceStatus(
        @Header("Authorization") token: String,
        @Path("id") id: Int,
        @Body request: UpdateMarketplaceStatusRequest
    ): Response<MarketplaceListingDto>

    @GET("api/app-version")
    suspend fun getAppVersion(): Response<AppVersionDto>

    companion object {
        private const val BASE_URL = "https://diplomanexus-backend.onrender.com/"

        fun create(): DiplomaNexusApi {
            val okHttpClient = okhttp3.OkHttpClient.Builder()
                .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                .writeTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                .retryOnConnectionFailure(true)
                .addInterceptor { chain ->
                    val request = chain.request()
                    var response: okhttp3.Response? = null
                    var exception: Exception? = null
                    for (attempt in 1..3) {
                        try {
                            response = chain.proceed(request)
                            if (response.isSuccessful || attempt == 3) {
                                return@addInterceptor response
                            }
                        } catch (e: Exception) {
                            exception = e
                            if (attempt == 3) throw e
                            try { Thread.sleep(1000) } catch (_: Exception) {}
                        }
                    }
                    response ?: throw (exception ?: java.io.IOException("Network request failed after 3 retries"))
                }
                .build()

            return Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(okHttpClient)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(DiplomaNexusApi::class.java)
        }
    }
}
