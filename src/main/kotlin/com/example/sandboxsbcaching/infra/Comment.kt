package com.example.sandboxsbcaching.infra

typealias CommentId = String

data class Comment(
    val id: CommentId,
    val body: String,
    val userId: UserId,
)