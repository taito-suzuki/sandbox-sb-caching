package com.example.sandboxsbcaching.usecase

import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Repository

@Repository
class UserRepository {
    @Cacheable(cacheNames = ["user"])
    fun getName(id: String): String {
        /**
         * @Cacheableアノテーションを付与すると、そのメソッドのレスポンスをキャッシュできる。
         */
        println("Fetch $id's name from repository")
        return "$id's name"
    }

    // suspend funにkeyGeneratorを指定するのをめっちゃ忘れそうだなー
    @Cacheable(cacheNames = ["user"], keyGenerator = "coroutineKeyGenerator")
    suspend fun getNameAsync(id: String): String {
        /**
         *
         */
        println("Async fetch $id's name from repository")
        return "$id's name"
    }
}
