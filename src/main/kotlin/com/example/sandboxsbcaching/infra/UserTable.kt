package com.example.sandboxsbcaching.infra

import org.springframework.data.repository.CrudRepository

interface UserTable : CrudRepository<User, UserId>