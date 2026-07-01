package com.panonit.sturdyoctopotato.security

import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.util.*
import kotlin.io.encoding.Base64

@Service
class JwtService(
    @Value($$"${jwt.secret}") private val secret: String,
    @Value($$"${jwt.expiration}") private val expiration: Long,
) {

    companion object {
        private const val BEARER = "Bearer "
        private const val TOKEN_TYPE = "type"
        private const val ACCESS_TOKEN = "access_token"
        private const val REFRESH_TOKEN = "refresh_token"
    }

    private val secretKey = Keys.hmacShaKeyFor(Base64.decode(secret))
    private val accessTokenValidityMs = expiration * 60 * 1000L // expiration * minutes
    val refreshTokenValidityMs = expiration * 24 * 60 * 60 * 1000L // expiration * days

    fun generateAccessToken(userId: String): String {
        return generateToken(userId, type = ACCESS_TOKEN, expiry = accessTokenValidityMs)
    }

    fun validateAccessToken(accessToken: String): Boolean {
        val claims = parseAllClaims(accessToken) ?: return false
        val type = claims[TOKEN_TYPE] as? String ?: return false

        return type == ACCESS_TOKEN
    }

    fun generateRefreshToken(userId: String): String {
        return generateToken(userId, type = REFRESH_TOKEN, expiry = refreshTokenValidityMs)
    }

    fun validateRefreshToken(refreshToken: String): Boolean {
        val claims = parseAllClaims(refreshToken) ?: return false
        val type = claims[TOKEN_TYPE] as? String ?: return false

        return type == REFRESH_TOKEN
    }


    fun getUserIdFromToken(token: String): String? {
        return parseAllClaims(token)?.subject
    }

    private fun parseAllClaims(token: String): Claims? {
        val jws = if (token.startsWith(prefix = BEARER)) token.removePrefix(BEARER) else token

        return try {
            val parser = Jwts.parser().verifyWith(secretKey).build()
            parser.parseSignedClaims(jws).payload
        } catch (_: Exception) {
            null
        }
    }

    private fun generateToken(userId: String, type: String, expiry: Long): String {
        val now = Date()
        val expiryDate = Date(now.time + expiry)

        return Jwts.builder()
            .subject(userId)
            .claim("type", type)
            .issuedAt(now)
            .expiration(expiryDate)
            .signWith(secretKey, Jwts.SIG.HS256)
            .compact()
    }
}