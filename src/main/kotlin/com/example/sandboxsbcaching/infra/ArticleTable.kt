package com.example.sandboxsbcaching.infra

import org.springframework.data.repository.CrudRepository

interface ArticleTable : CrudRepository<Article, ArticleId>