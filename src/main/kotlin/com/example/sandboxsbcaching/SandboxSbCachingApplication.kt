package com.example.sandboxsbcaching

import com.github.benmanes.caffeine.cache.Caffeine
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.cache.CacheManager
import org.springframework.cache.annotation.Cacheable
import org.springframework.cache.annotation.EnableCaching
import org.springframework.cache.caffeine.CaffeineCache
import org.springframework.cache.caffeine.CaffeineCacheManager
import org.springframework.cache.interceptor.KeyGenerator
import org.springframework.cache.interceptor.SimpleKeyGenerator
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.stereotype.Controller
import org.springframework.stereotype.Repository
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import java.lang.reflect.Method
import kotlin.reflect.jvm.kotlinFunction

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

@Configuration
class AppConfig {

    /**
     * ここでキャッシュの実装を指定する。
     * CacheManagerの実装を切り替えるだけで、コードを修正することなく、実装を切り替えられるようにしておくと良い。
     */

    /**
     * SimpleCacheManager実装を使用する場合
     * SimpleCacheManaager は、Springが用意している単純なCacheManager
     * これ単体は極めてシンプルな仕様なため、プロダクションでの利用は難しいかも
     */
    /*
    @Bean
    fun cacheManager(): CacheManager {
        val scm = SimpleCacheManager()
        scm.setCaches(
            listOf(
                ConcurrentMapCache(
                    "user",
                ),
            )
        )
        return scm
    }
    */

    /**
     * CaffeineCacheManager
     * 割と新しめで人気の高いキャッシュ実装。
     * 高機能。保守性も問題なし（GHのstarが多く、ソースコードもどんどんコミットされてる）。
     */

    @Bean
    fun caffeineConfig(): Caffeine<Any, Any> = Caffeine.newBuilder()
        .recordStats() // 統計情報を記録しておく

    @Bean
    fun caffeineCacheManager(caffeine: Caffeine<Any, Any>): CacheManager {
        val ccm = CaffeineCacheManager()
        ccm.setCaffeine(caffeine)
        return ccm
    }

    /**
     * Kotlinのsuspend funcにCacheableアノテーションを付与しても、キャッシュされない。
     * 理由は、suspend funcのjavaのコード表現においては、関数の引数にContinueationオブジェクトが含まれているため。
     * kotlinのコードにはない引数が、コンパイルの過程で追加されるとのこと。
     * https://stackoverflow.com/questions/70801728/spring-boot-cachable-ehcache-with-kotlin-coroutines-best-practises
     * このContinuationオブジェクトをキャッシュキーとしてしまうため、キャッシュが正しく（開発者が意図した通りに）動かない。
     *
     * 適切にキャッシュするためには、suspend funcには、以下のようなCustomなKeyGeneratorを指定してやる必要がある。
     *
     * 結構怖い落とし穴である。
     */
    private class CoroutineKeyGenerator : SimpleKeyGenerator() {
        // ↑SimpleKeyGeneratorはSpring Cache Abstractionのデフォルトで使用されているKeyGenerator。
        override fun generate(target: Any, method: Method, vararg params: Any?): Any {
            val isSuspendFunction = method.kotlinFunction?.isSuspend ?: false
            if (isSuspendFunction) {
                // Suspend関数だったら、paramsの末尾（Continuationオブジェクト）を除去する
                val nextParams = arrayOfNulls<Any?>(params.size - 1)
                params.forEachIndexed { i, v ->
                    if (i < params.size - 1) {
                        nextParams[i] = v
                    }
                }
                return super.generate(target, method, *nextParams)
            }
            return super.generate(target, method, *params)
        }
    }

    @Bean
    fun coroutineKeyGenerator(): KeyGenerator {
        return CoroutineKeyGenerator()
    }
}

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
