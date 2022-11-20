package com.example.sandboxsbcaching.infra

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table

typealias UserId = Long

@Table("users")
data class User(
    @Id val id: UserId? = null,
    val name: String,
)