package com.example.sandboxsbcaching.infra

import com.github.benmanes.caffeine.cache.Caffeine
import org.springframework.cache.CacheManager
import org.springframework.cache.caffeine.CaffeineCacheManager
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.util.concurrent.TimeUnit

@Configuration
class CacheConfigCaffeine {
    /**
     * CaffeineCacheManager
     * https://github.com/ben-manes/caffeine
     *
     * Caffeineは、javaのキャッシュライブラリ。
     * 割と新しめで人気の高いキャッシュ実装。
     * 高機能。保守性も問題なし（GHのstarが多く、ソースコードもどんどんコミットされてる）。
     * パフォーマンスに定評がある（ネット上調べ）。
     *
     * ライブラリが公式にSpring Cache Abstractionへ対応しているため、
     * CaffeineをSpring Cache Abstractionに対応させるためのコードを自前で書く必要はない。
     */

    @Bean
    fun caffeineCacheManager(): CacheManager {
        /**
         * user, articleという名前のキャッシュの設定を、それぞれ定義する。
         */
        val mng = CaffeineCacheManager(
            // 以下の2つのキャッシュだけを利用可能とする。
            // これら以外のキャッシュを利用しようとした場合、例外がスローされる。
            "user",
            "article",
            "comment",
        )
        /**
         * キャッシュの細かい設定を指定できる。
         */
        mng.registerCustomCache(
            "user",
            Caffeine.newBuilder()
                // キャッシュデータ更新ルールを指定する。
                // https://github.com/ben-manes/caffeine/wiki/Eviction
                // キャッシュデータの更新ルールは2種類ある。
                // (1) サイズによる更新ルール
                // (2) 機関による更新ルール
                // 更新ルールを指定していない場合、どうなるのか？
                // 公式ドキュメントでは特に記述はないが、実際に試してみた挙動を見る限り、キャッシュに一度データが保持された後は更新されない。
                /**
                 * (1) サイズによる更新ルール
                 * このキャッシュが保持できるデータ数の上限値
                 * キャッシュが保持しているデータの数が、この上限値を超えると、
                 * 利用頻度の小さいデータから消してゆく。
                 */
                .maximumSize(1000)
                // 続いて、キャッシュの生存期間による更新ルール。
                // キャッシュの生存期間は3つある
                // それぞれ微妙に振る舞いが異なるので注意が必要。
                /**
                 * (2) 期間による更新ルール
                 * とあるキーがキャッシュから読まれてから一定時間の間、そのキーの値を保持する。という設定。
                 * 注意点
                 * とあるキーがアクセスされた時刻から一定時間アクセスがない状態が続くと、そのキーはキャッシュから削除される。
                 * しかしながら、頻繁にアクセスされるキーは、古い状態のまま更新されずにずっとキャッシュに残り続けてしまう可能性があることに注意。
                 */
                // .expireAfterAccess(10, TimeUnit.SECONDS)
                /**
                 * (2) 期間による更新ルール
                 * キーがキャッシュへ書き込まれた時刻から一定時間はキーを保持する。という設定。
                 * 注意点
                 * .expireAfterAccessと異なる点は、そのキーがキャッシュからどれだけ読み込まれているかは考慮されない点。
                 * キーが頻繁にアクセスされているかどうかにかかわらず、
                 * そのキーが作られた時刻から一定時間が経過するとそのデータは削除される。
                 */
                .expireAfterWrite(10, TimeUnit.SECONDS)
                // キーがキャッシュから削除された後に呼ばれるコールバック関数を作ったりもできる。
                .evictionListener<Any, Any> { key, value, cause ->
                    println("Evicted $key because $cause")
                }
                /**
                 * 統計情報を記録する。
                 * 統計情報は、cache.statsメソッドにより取得されることができる。
                 */
                .recordStats()
                .build(),
        )
        mng.registerCustomCache(
            "article",
            Caffeine.newBuilder()
                .maximumSize(1000)
                .expireAfterWrite(10, TimeUnit.SECONDS)
                .recordStats()
                .build(),
        )
        mng.registerCustomCache(
            "comment",
            Caffeine.newBuilder()
                .evictionListener<Any, Any> { key, value, cause ->
                    println("Evicted $key because $cause")
                }
                .maximumSize(1000)
                .recordStats()
                .build(),
        )
        return mng
    }
}