package com.panonit.sturdyoctopotato.security

import com.panonit.sturdyoctopotato.database.model.RefreshToken
import com.panonit.sturdyoctopotato.database.model.User
import com.panonit.sturdyoctopotato.database.repository.RefreshTokenRepository
import com.panonit.sturdyoctopotato.database.repository.UserRepository
import org.bson.types.ObjectId
import org.springframework.http.HttpStatus
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException
import java.security.MessageDigest
import kotlin.io.encoding.Base64
import kotlin.jvm.optionals.getOrNull
import kotlin.time.Clock
import kotlin.time.Duration.Companion.milliseconds

@Service
class AuthService(
    private val jwtService: JwtService,
    private val hashEncoder: HashEncoder,
    private val userRepository: UserRepository,
    private val refreshTokenRepository: RefreshTokenRepository,
) {
    data class TokenPair(val access: String, val refresh: String)

    fun register(email: String, password: String) {
        if (userRepository.findByEmail(email) != null) {
            throw ResponseStatusException(HttpStatus.CONFLICT, "user already exists")
        }

        userRepository.save(User(email, hashEncoder.encode(password)))
    }

    fun login(email: String, password: String): TokenPair {
        val user = userRepository.findByEmail(email) ?: throw BadCredentialsException("invalid credentials")

        if (!hashEncoder.matches(raw = password, hashed = user.hashedPassword)) {
            throw BadCredentialsException("invalid credentials")
        }

        val tokenPair = generateTokenPair(userId = user.id)
        storeRefreshToken(userId = user.id, rawRefreshToken = tokenPair.refresh)

        return tokenPair
    }

    @Transactional
    fun refresh(refreshToken: String): TokenPair {
        if (!jwtService.validateRefreshToken(refreshToken)) {
            throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "invalid refresh token")
        }

        val userId = jwtService.getUserIdFromToken(refreshToken)
            ?: throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "invalid refresh token")

        val user = userRepository.findById(ObjectId(userId)).getOrNull()
            ?: throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "invalid refresh token")

        val hashed = hashToken(refreshToken)
        refreshTokenRepository.findByUserIdAndHashedToken(userId = user.id, hashedToken = hashed)
            ?: throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "invalid refresh token")

        refreshTokenRepository.deleteByUserIdAndHashedToken(userId = user.id, hashedToken = hashed)

        val tokenPair = generateTokenPair(userId = user.id)
        storeRefreshToken(userId = user.id, rawRefreshToken = tokenPair.refresh)

        return tokenPair
    }

    private fun storeRefreshToken(userId: ObjectId, rawRefreshToken: String) {
        val hashed = hashToken(rawRefreshToken)
        val expiresAt = Clock.System.now().plus(duration = jwtService.refreshTokenValidityMs.milliseconds)

        refreshTokenRepository.save(
            RefreshToken(
                userId = userId,
                hashedToken = hashed,
                expiresAt = expiresAt,
            )
        )
    }

    private fun generateTokenPair(userId: ObjectId): TokenPair {
        return TokenPair(
            access = jwtService.generateAccessToken(userId.toHexString()),
            refresh = jwtService.generateRefreshToken(userId.toHexString())
        )
    }

    private fun hashToken(token: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(token.encodeToByteArray())

        return Base64.encode(hash)
    }
}