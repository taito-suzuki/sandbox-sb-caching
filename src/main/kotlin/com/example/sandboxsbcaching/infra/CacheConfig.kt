package com.example.sandboxsbcaching.infra

import org.springframework.cache.interceptor.KeyGenerator
import org.springframework.cache.interceptor.SimpleKeyGenerator
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.lang.reflect.Method
import kotlin.reflect.jvm.kotlinFunction

@Configuration
class CacheConfig {

    /**
     * Kotlinのsuspend funcにCacheableアノテーションを付与しても、キャッシュされない。
     * 理由は、suspend funcのjavaのコード表現において、関数の引数にContinueationオブジェクトが含まれているため。
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
