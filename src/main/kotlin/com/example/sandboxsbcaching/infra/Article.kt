package com.example.sandboxsbcaching.infra

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table

typealias ArticleId = Long

@Table("articles")
data class Article(
    @Id val id: ArticleId? = null,
    val title: String,
    val authorId: UserId,
)