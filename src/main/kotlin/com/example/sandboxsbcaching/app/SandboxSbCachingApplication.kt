package com.example.sandboxsbcaching

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.cache.CacheManager
import org.springframework.cache.annotation.Cacheable
import org.springframework.cache.annotation.EnableCaching
import org.springframework.cache.caffeine.CaffeineCache
import org.springframework.stereotype.Controller
import org.springframework.stereotype.Repository
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable

@SpringBootApplication
// Spring FrameworkのCache abstraction機能を有効化するオプション。
// このオプションを付与すると、キャッシュされる。
// このオプションを外すだけで、コードを修正することなく、キャッシュが無効化される実装にしておくと良い。
@EnableCaching
class SandboxSbCachingApplication

// Spring FrameworkのCache abstractionは、アノテーションを関数に付与するだけでキャッシュできてしまう。
//
// とても便利なものだが、その反面、以下の点に気をつける必要がある。
// (1) 便利だからといってキャッシュをホイホイと追加してゆき、どこで何をキャッシュしているのかわからなくなってしまうということが起こりうる。
// (2) キャッシュ名が意図せず被る。
//
// 使用可能なキャッシュをあらかじめ定義しておき、キャッシュをホイホイと追加できなくし、キャッシュ使用箇所を辿りやすくしておいた方が良い。
// ということでenum使う。
// TODO ↑定数化する。

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

@Controller
class HelloController(
    val userRepository: UserRepository,
    val cacheManager: CacheManager,
) {
    @GetMapping("/users/{id}")
    fun getUserById(
        model: Model,
        @PathVariable("id") id: String,
    ): String {
        model.addAttribute("name", userRepository.getName(id))
        return "index"
    }

    @GetMapping("/users/{id}/async")
    suspend fun getUserByIdAsync(
        model: Model,
        @PathVariable("id") id: String,
    ): String {
        model.addAttribute("name", userRepository.getNameAsync(id))
        return "index"
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
        val cache = cacheManager.getCache(cacheName) ?: return "error/404"
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


fun main(args: Array<String>) {
    runApplication<SandboxSbCachingApplication>(*args)
}
