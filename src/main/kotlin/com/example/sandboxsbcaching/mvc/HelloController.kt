package com.example.sandboxsbcaching.mvc

import com.example.sandboxsbcaching.infra.*
import org.springframework.cache.CacheManager
import org.springframework.cache.caffeine.CaffeineCache
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.ResponseStatus

@Controller
class HelloController(
    val userRepository: UserRepository,
    val articleRepository: ArticleRepository,
    val commentRepository: CommentRepository,
    val cacheManager: CacheManager,
) {
    @GetMapping("/users/{id}")
    fun getUserById(
        model: Model,
        @PathVariable("id") id: Long,
    ): String {
        model.addAttribute("user", userRepository.getUser(id))
        return "user"
    }

    @GetMapping("/users/{id}/async")
    suspend fun getUserByIdAsync(
        model: Model,
        @PathVariable("id") id: Long,
    ): String {
        model.addAttribute("user", userRepository.getUserAsync(id))
        return "user"
    }

    @GetMapping("/articles/{id}")
    fun getArticleById(
        model: Model,
        @PathVariable("id") id: ArticleId,
    ): String {
        model.addAttribute("article", articleRepository.getArticle(id))
        return "article"
    }

    @GetMapping("/articles/{id}/async")
    suspend fun getArticleByIdAsync(
        model: Model,
        @PathVariable("id") id: ArticleId,
    ): String {
        model.addAttribute("article", articleRepository.getArticleAsync(id))
        return "article"
    }

    @GetMapping("/comments/{id}")
    fun getCommentById(
        model: Model,
        @PathVariable("id") id: CommentId,
    ): String {
        model.addAttribute("comment", commentRepository.getComment(id))
        return "comment"
    }

    @GetMapping("/comments/{id}/async")
    suspend fun getCommentByIdAsync(
        model: Model,
        @PathVariable("id") id: CommentId,
    ): String {
        model.addAttribute("comment", commentRepository.getCommentAsync(id))
        return "comment"
    }

    @ExceptionHandler(NotFoundException::class)
    @ResponseStatus(
        value = HttpStatus.NOT_FOUND,
        reason = "Not Found"
    )
    fun error404(
        model: Model,
        e: Exception,
    ): String {
        println(e.message)
        return "error/404"
    }

    /**
     * CaffeineCacheの統計情報を返す。
     * 統計はキャッシュがシステムに対してかける負荷を可視化する上で必要な情報。
     * 本番運用する場合、統計はサクッとチェックできるようにしておきたい。
     */
    data class CaffeineCacheStatsEntry<T>(
        val name: String,
        val description: String,
        val value: T,
    )

    @GetMapping("/caffeine/stats/{cacheName}")
    fun getCaffeineStats(
        @PathVariable("cacheName") cacheName: String,
        model: Model,
    ): String {
        val cache = cacheManager.getCache(cacheName) ?: throw NotFoundException("cache $cacheName is not found")
        if (cache !is CaffeineCache) return "error/500"
        val nativeCache = cache.nativeCache
        val stats = nativeCache.stats()

        /**
         * Runtimeにおけるキャッシュのサイズ。
         */
        val cacheEntries = listOf(
            CaffeineCacheStatsEntry(
                "estimatedSize",
                "The value returned is an estimate; the actual count may differ if there are concurrent insertions or removals, or if some entries are pending removal due to expiration or weak/soft reference collection. In the case of stale entries this inaccuracy can be mitigated by performing a cleanUp() first.",
                nativeCache.estimatedSize(),
            ),
        )

        /**
         * Runtimeのメモリ使用量に関すること。
         *
         * 残念ながら、RuntimeにおけるCaffeineCacheのメモリ使用量を推定することは困難。
         * JAMMというツールを用いれば可能だが、このツールはプロダクションでは利用できない。
         * https://stackoverflow.com/questions/63672666/how-to-calculate-the-memory-occupied-by-caffeine-cache
         *
         * Runtimeにおけるメモリ使用量を推定することが困難である
         * というのは、Caffeineライブラリ固有の問題ではなく、JVM上で動くアプリケーション全般に言えることっぽい。
         * なので、メモリ使用量の推定は、諦めた方が良いかもしれない。
         */

        /**
         * その他、取得することのできるキャッシュに関する統計値。
         */
        val statsEntries = listOf(
            CaffeineCacheStatsEntry(
                "averageLoadPenalty",
                "The average number of nanoseconds spent loading new values.",
                stats.averageLoadPenalty(),
            ),
            CaffeineCacheStatsEntry(
                "evictionCount",
                "The number of times an entry has been evicted.",
                stats.evictionCount(),
            ),
            CaffeineCacheStatsEntry(
                "evictionWeight",
                "The sum of weights of evicted entries.",
                stats.evictionWeight(),
            ),
            CaffeineCacheStatsEntry(
                "hitCount",
                "The number of times Cache lookup methods have returned a cached value.",
                stats.hitCount(),
            ),
            CaffeineCacheStatsEntry(
                "hitRate",
                "The ratio of cache requests which were hits.",
                stats.hitRate(),
            ),
            CaffeineCacheStatsEntry(
                "loadCount",
                "The total number of times that Cache lookup methods attempted to load new values.",
                stats.loadCount(),
            ),
            CaffeineCacheStatsEntry(
                "loadFailureCount",
                "The number of times Cache lookup methods failed to load a new value, either because no value was found or an exception was thrown while loading.",
                stats.loadFailureCount(),
            ),
            CaffeineCacheStatsEntry(
                "loadFailureRate",
                "The ratio of cache loading attempts which threw exceptions.",
                stats.loadFailureRate(),
            ),
            CaffeineCacheStatsEntry(
                "loadSuccessCount",
                "The number of times Cache lookup methods have successfully loaded a new value.",
                stats.loadSuccessCount(),
            ),
            CaffeineCacheStatsEntry(
                "missCount",
                "The number of times Cache lookup methods have returned an uncached (newly loaded) value, or null.",
                stats.missCount(),
            ),
            CaffeineCacheStatsEntry(
                "missCount",
                "The ratio of cache requests which were misses.",
                stats.missRate(),
            ),
            CaffeineCacheStatsEntry(
                "requestCount",
                "The number of times Cache lookup methods have returned either a cached or uncached value.",
                stats.requestCount(),
            ),
            CaffeineCacheStatsEntry(
                "totalLoadTime",
                "The total number of nanoseconds the cache has spent loading new values.",
                stats.totalLoadTime(),
            ),
        )
        model.addAttribute("statsEntries", statsEntries)
        model.addAttribute("cacheEntries", cacheEntries)
        return "caffeine/stats"
    }

}