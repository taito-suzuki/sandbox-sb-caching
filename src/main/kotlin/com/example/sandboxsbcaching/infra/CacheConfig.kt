package com.example.sandboxsbcaching.app

import com.github.benmanes.caffeine.cache.Caffeine
import org.springframework.cache.CacheManager
import org.springframework.cache.caffeine.CaffeineCacheManager
import org.springframework.cache.interceptor.KeyGenerator
import org.springframework.cache.interceptor.SimpleKeyGenerator
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.lang.reflect.Method
import kotlin.reflect.jvm.kotlinFunction

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
     * 参考 https://stackoverflow.com/questions/70801728/spring-boot-cachable-ehcache-with-kotlin-coroutines-best-practises
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
