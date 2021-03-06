package com.blitzoffline.bountysystem.command

import com.blitzoffline.bountysystem.BountySystem
import com.blitzoffline.bountysystem.config.holder.Messages
import com.blitzoffline.bountysystem.util.sendMessage
import me.mattstudios.mf.annotations.Alias
import me.mattstudios.mf.annotations.Command
import me.mattstudios.mf.annotations.Permission
import me.mattstudios.mf.annotations.SubCommand
import me.mattstudios.mf.base.CommandBase
import org.bukkit.command.CommandSender

@Alias("badmin")
@Command("bountyadmin")
class CommandAdminReload(private val plugin: BountySystem) : CommandBase() {

    @SubCommand("reload")
    @Permission("bountysystem.admin")
    fun reload(sender: CommandSender) {
        plugin.database.save()
        plugin.settings.reload()
        plugin.messages.reload()
        plugin.messages[Messages.CONFIG_RELOADED].sendMessage(sender)
    }
}