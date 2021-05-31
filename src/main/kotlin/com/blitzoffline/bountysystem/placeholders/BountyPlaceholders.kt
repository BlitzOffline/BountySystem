package com.blitzoffline.bountysystem.placeholders

import com.blitzoffline.bountysystem.BountySystem
import com.blitzoffline.bountysystem.bounty.BOUNTIES_LIST
import me.clip.placeholderapi.expansion.PlaceholderExpansion
import org.bukkit.OfflinePlayer

class BountyPlaceholders(private val plugin: BountySystem) : PlaceholderExpansion() {
    override fun getIdentifier() = plugin.description.name.lowercase()

    override fun getAuthor() = plugin.description.authors[0] ?: "BlitzOffline"

    override fun getVersion() = plugin.description.version

    override fun canRegister() = true

    override fun persist() = true

    override fun onRequest(player: OfflinePlayer?, params: String): String? {
        if (params.split("_").size != 2) return null

        when {

            params.startsWith("target_", true) -> {
                val id = params.substringAfter("target_")
                val bounty = BOUNTIES_LIST.firstOrNull { it.id.toString() == id && !it.expired() } ?: return ""
                return bounty.target().name ?: ""
            }

            params.startsWith("payer_", true) -> {
                val id = params.substringAfter("payer_")
                val bounty = BOUNTIES_LIST.firstOrNull { it.id.toString() == id && !it.expired() } ?: return ""
                return bounty.payer().name ?: ""
            }

            params.startsWith("amount_", true) -> {
                val id = params.substringAfter("amount_")
                val bounty = BOUNTIES_LIST.firstOrNull { it.id.toString() == id && !it.expired() } ?: return ""
                return bounty.amount.toString()
            }

            params.startsWith("ids_", true) -> {
                val p = plugin.server.getOfflinePlayerIfCached(params.substringAfter("ids_")) ?: return ""
                val ids = mutableListOf<String>()
                BOUNTIES_LIST.forEach { bounty ->
                    if (bounty.expired()) return@forEach
                    if (bounty.payer() != p) return@forEach
                    ids.add(bounty.id.toString())
                }
                if (ids.isEmpty()) return ""
                return ids.joinToString(", ")
            }
        }
        return null
    }
}