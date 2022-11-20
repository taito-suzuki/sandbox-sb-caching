# Spring Cache勉強用

https://docs.spring.io/spring-framework/docs/current/reference/html/integration.html#cache

Spring Frameworkには、Spring Cache Abstractionという、抽象化されたキャッシュ機構がある。
「抽象化された」という言葉の意味は、キャッシュの実装を容易に変えられるということ。

どんな機能がある？（ざっくりと）

- 関数に対するキャッシュ機構。関数の引数からキャッシュキーを抽出し、抽出されたキャッシュキーに紐付けて、関数の 返り値をキャッシュする。
- キャッシュキーは、基本的には関数の引数となるが、KeyGeneratorをカスタム実装することで複雑なキャッシュキーの抽出も可能。
- キャッシュ条件も指定可能（この引数がXXX以上だったらキャッシュする等）。
- キャッシュ機構の実装を、CacheManagerをBeanすることで容易に切り替えられる。
    - Redis実装
    - Caffeine実装
    - Ehcache実装
    - etc...

サンプルプログラムの実行方法。

```shell
./gradlew bootRun
```