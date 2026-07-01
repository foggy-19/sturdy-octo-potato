package com.panonit.sturdyoctopotato.security

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatusCode
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import org.springframework.web.server.ResponseStatusException

@Component
class JwtAuthFilter(
    private val jwtService: JwtService
) : OncePerRequestFilter() {

    companion object {
        private const val AUTHORIZATION = "Authorization"
        private const val BEARER = "Bearer "
    }

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        val header = request.getHeader(AUTHORIZATION)
        if (header?.startsWith(BEARER) == true) {
            val token = header.removePrefix(BEARER)
            if (jwtService.validateAccessToken(token)) {
                val userId = jwtService.getUserIdFromToken(token)
                    ?: throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "invalid access token")

                val auth = UsernamePasswordAuthenticationToken(userId, null, null)
                SecurityContextHolder.getContext().authentication = auth
            }
        }

        filterChain.doFilter(request, response)
    }
}