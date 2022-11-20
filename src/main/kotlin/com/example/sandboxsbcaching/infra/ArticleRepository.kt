package com.example.sandboxsbcaching.infra

import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Repository

@Repository
class ArticleRepository(
    val articleTable: ArticleTable,
) {
    @Cacheable(cacheNames = ["article"])
    fun getArticle(id: Long): Article {
        /**
         * @Cacheableアノテーションを付与すると、そのメソッドのレスポンスをキャッシュできる。
         */
        println("Fetch article($id) from repository")
        val user = articleTable.findById(id)
        if (user.isEmpty) {
            throw NotFoundException("Not found entry article:$id")
        }
        return user.get()
    }

    // suspend funにkeyGeneratorを指定するのをめっちゃ忘れそうだなー
    @Cacheable(cacheNames = ["article"], keyGenerator = "coroutineKeyGenerator")
    suspend fun getArticleAsync(id: Long): Article = getArticle(id)
}
