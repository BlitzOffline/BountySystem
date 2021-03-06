package com.blitzoffline.bountysystem.util

import me.clip.placeholderapi.PlaceholderAPI
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import org.bukkit.OfflinePlayer
import org.bukkit.entity.Player


val specialSerializer = LegacyComponentSerializer.builder().hexColors().useUnusualXRepeatedCharacterHexFormat().build()
val legacySerializer = LegacyComponentSerializer.legacyAmpersand()

fun String.color() = specialSerializer.serialize(legacySerializer.deserialize(this))
fun List<String>.color() = map { it.color() }

fun String.parsePAPI(player: Player) = PlaceholderAPI.setPlaceholders(player, this)
fun String.parsePAPI(player: OfflinePlayer) = PlaceholderAPI.setPlaceholders(player, this)

fun List<String>.containsIgnoreCase(s: String) = this.map { it.uppercase() }.contains(s.uppercase())
fun List<String>.containsAnyIgnoreCase(l: List<String>): Boolean {
    l.forEach { if (this.containsIgnoreCase(it)) return true }
    return false
}