package com.panonit.sturdyoctopotato.controllers

import com.panonit.sturdyoctopotato.security.AuthService
import jakarta.validation.Valid
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.Pattern
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/auth")
class AuthController(
    private val authService: AuthService,
) {
    data class AuthRequest(
        @field:Email(message = "Invalid email address")
        val email: String,
        @field:Pattern(
            regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).{9,}$",
            message = "Password must be at least 9 characters long and contain at least one digit, uppercase and lowercase character."
        )
        val password: String
    )

    data class RefreshTokenRequest(val refreshToken: String)

    @PostMapping("/register")
    fun register(
        @Valid @RequestBody body: AuthRequest,
    ) {
        return authService.register(body.email, body.password)
    }

    @PostMapping("/login")
    fun login(
        @Valid @RequestBody body: AuthRequest,
    ): AuthService.TokenPair {
        return authService.login(body.email, body.password)
    }

    @PostMapping("/refresh")
    fun refresh(
        @RequestBody body: RefreshTokenRequest,
    ): AuthService.TokenPair {
        return authService.refresh(body.refreshToken)
    }
}