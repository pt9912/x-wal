package com.xwal.adapter.identity

import io.micronaut.security.token.RolesFinder
import jakarta.inject.Singleton
import org.slf4j.LoggerFactory

@Singleton
class KeycloakRolesMapper : RolesFinder {

    private val log = LoggerFactory.getLogger(KeycloakRolesMapper::class.java)

    override fun resolveRoles(claims: Map<String, Any>?): List<String> {
        if (claims == null) return emptyList()

        val keycloakRoles = extractKeycloakRoles(claims)
        val xwalScopes = keycloakRoles.flatMap { mapToXwalScopes(it) }.distinct()

        log.debug("Mapped Keycloak roles {} to x-wal scopes {}", keycloakRoles, xwalScopes)
        return xwalScopes
    }

    private fun extractKeycloakRoles(claims: Map<String, Any>): Set<String> {
        val roles = mutableSetOf<String>()

        // Realm roles
        @Suppress("UNCHECKED_CAST")
        val realmAccess = claims["realm_access"] as? Map<String, Any>
        val realmRoles = realmAccess?.get("roles") as? List<String>
        realmRoles?.let { roles.addAll(it) }

        // Client roles (xwal-api)
        @Suppress("UNCHECKED_CAST")
        val resourceAccess = claims["resource_access"] as? Map<String, Any>
        @Suppress("UNCHECKED_CAST")
        val clientAccess = resourceAccess?.get("xwal-api") as? Map<String, Any>
        val clientRoles = clientAccess?.get("roles") as? List<String>
        clientRoles?.let { roles.addAll(it) }

        return roles
    }

    private fun mapToXwalScopes(role: String): List<String> = when (role) {
        "admin" -> listOf("workflow.admin", "workflow.write", "workflow.read")
        "workflow-designer" -> listOf("workflow.write", "workflow.read")
        "workflow-executor" -> listOf("workflow.write", "workflow.read")
        "viewer" -> listOf("workflow.read")
        else -> emptyList()
    }
}
