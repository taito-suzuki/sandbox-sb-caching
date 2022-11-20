package com.example.sandboxsbcaching.infra

import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Repository

@Repository
class UserRepository(
    val userTable: UserTable,
) {
    @Cacheable(cacheNames = ["user"])
    fun getUser(id: Long): User {
        /**
         * @Cacheableアノテーションを付与すると、そのメソッドのレスポンスをキャッシュできる。
         */
        println("Fetch user($id) from repository")
        val user = userTable.findById(id)
        if (user.isEmpty) {
            throw NotFoundException("Not found entry user:$id")
        }
        return user.get()
    }

    // suspend funにkeyGeneratorを指定するのをめっちゃ忘れそうだなー
    @Cacheable(cacheNames = ["user"], keyGenerator = "coroutineKeyGenerator")
    suspend fun getUserAsync(id: Long): User = getUser(id)
}
