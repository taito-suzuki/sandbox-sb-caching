package com.example.sandboxsbcaching

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.cache.annotation.EnableCaching
import org.springframework.data.jdbc.repository.config.EnableJdbcRepositories

@SpringBootApplication
@EnableJdbcRepositories
// Spring FrameworkのCache abstraction機能を有効化するアノテーション。
// このオプションを外すだけで、コードを修正することなく、キャッシュが無効化される実装にしておくと良い。
@EnableCaching
class SandboxSbCachingApplication


fun main(args: Array<String>) {
    runApplication<SandboxSbCachingApplication>(*args)
}
