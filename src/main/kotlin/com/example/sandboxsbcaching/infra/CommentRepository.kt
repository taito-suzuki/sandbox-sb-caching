package com.example.sandboxsbcaching.infra

import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Repository

@Repository
class CommentRepository {
    @Cacheable(cacheNames = ["comment"])
    fun getComment(id: CommentId): Comment {
        println("Fetch comment($id) from repository")
        return Comment(
            id = id,
            body = "$id comment",
            userId = 1,
        )
    }

    @Cacheable(cacheNames = ["comment"], keyGenerator = "coroutineKeyGenerator")
    suspend fun getCommentAsync(id: CommentId): Comment {
        return getComment(id)
    }
}