package com.panonit.sturdyoctopotato.database.repository

import com.panonit.sturdyoctopotato.database.model.User
import org.bson.types.ObjectId
import org.springframework.data.mongodb.repository.MongoRepository

interface UserRepository : MongoRepository<User, ObjectId> {
    fun findByEmail(email: String): User?
}