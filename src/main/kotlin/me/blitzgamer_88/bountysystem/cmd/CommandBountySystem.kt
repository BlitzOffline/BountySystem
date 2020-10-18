package me.blitzgamer_88.bountysystem.cmd

import me.blitzgamer_88.bountysystem.BountySystem
import me.blitzgamer_88.bountysystem.conf.Config
import me.blitzgamer_88.bountysystem.util.*
import me.mattstudios.mf.annotations.Command
import me.mattstudios.mf.annotations.Completion
import me.mattstudios.mf.annotations.Default
import me.mattstudios.mf.annotations.SubCommand
import me.mattstudios.mf.base.CommandBase
import me.mattstudios.mfgui.gui.components.ItemBuilder
import me.mattstudios.mfgui.gui.guis.GuiItem
import me.mattstudios.mfgui.gui.guis.PaginatedGui
import org.bukkit.Bukkit.getOfflinePlayer
import org.bukkit.Bukkit.getServer
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.SkullMeta
import java.util.*


@Command("bounty")
class CommandBountySystem(private val plugin: BountySystem) : CommandBase() {

    @Default
    fun mainCommand(sender: Player) {

        // Perms
        val bountyOpenPermission = conf().getProperty(Config.bountyOpenPermission)
        // Messages
        val noPermission = conf().getProperty(Config.noPermission)
        val noBountiesFound = conf().getProperty(Config.noBountiesFound)
        val guiTitle = conf().getProperty(Config.guiTitle).color()
        val itemTitle = conf().getProperty(Config.itemTitle)
        val itemLore = conf().getProperty(Config.itemLore)
        // Others
        val ids = plugin.getBounties().getKeys(false)
        val bountyTax = conf().getProperty(Config.bountyTax)
        val bountyExpiryTime = conf().getProperty(Config.bountyExpiryTime)

        if (!sender.hasPermission(bountyOpenPermission)) {
            noPermission.msg(sender)
            return
        }

        // CREATE AND OPEN A MENU WITH ALL BOUNTIES LISTED INSIDE

        val idsSize = ids.size
        if (idsSize < 1) {
            noBountiesFound.msg(sender)
            return
        }

        val bounties = plugin.getBounties()
        val bountyGui = PaginatedGui(6, 45, guiTitle)

        bountyGui.setItem(6, 3, ItemBuilder.from(Material.PAPER).setName("&6Previous".color()).asGuiItem { bountyGui.previous() })
        bountyGui.setItem(6, 7, ItemBuilder.from(Material.PAPER).setName("&6Next".color()).asGuiItem { bountyGui.next() })

        val fillerGlass = ItemStack(Material.BLACK_STAINED_GLASS_PANE, 1)
        val fillerMeta = fillerGlass.itemMeta
        if (fillerMeta != null) {
            fillerMeta.setDisplayName(" ")
            fillerGlass.itemMeta = fillerMeta
        }
        val fillerItem = GuiItem(fillerGlass)
        bountyGui.filler.fillBottom(fillerItem)

        var i = 0
        for (id in ids) {
            if (id.toIntOrNull() == null) continue
            if (id.toInt() < minId || id.toInt() > maxId) continue

            val targetUniqueIdString = bounties.getString("$id.target") ?: continue
            val targetUniqueId = UUID.fromString(targetUniqueIdString)
            val targetOfflinePlayer = getOfflinePlayer(targetUniqueId)

            val payerUniqueIdString = bounties.getString("$id.placer") ?: continue
            val payerUniqueId = UUID.fromString(payerUniqueIdString)
            val payerOfflinePlayer = getOfflinePlayer(payerUniqueId)
            val payerName = payerOfflinePlayer.name ?: continue

            val amount = bounties.getString("$id.amount") ?: continue
            val newAmount = amount.toInt() - ((bountyTax/100)*amount.toInt())
            val placedTime = bounties.getLong("$id.placedTime")
            val currentTime = System.currentTimeMillis() / 1000
            val expiryTime = formatTime(bountyExpiryTime-(currentTime-placedTime))

            val newItemLore: MutableList<String> = mutableListOf()

            for (lore in itemLore) {
                newItemLore.add(
                    lore.color().replace("%amount%", newAmount.toString()).replace("%payer%", payerName).replace("%bountyId%", id).replace("%expiryTime%", expiryTime)
                )
            }

            val head = ItemStack(Material.PLAYER_HEAD, 1)

            val meta = head.itemMeta as SkullMeta
            meta.owningPlayer = targetOfflinePlayer
            meta.setDisplayName(itemTitle.parsePAPI(targetOfflinePlayer))
            meta.lore = newItemLore
            head.itemMeta = meta

            val guiItem = GuiItem(head)
            bountyGui.setItem(i, guiItem)

            i++
            if (i > 45) i=0
        }

        bountyGui.setDefaultClickAction { it.isCancelled = true }

        bountyGui.open(sender)
    }


    @SubCommand("place")
    fun bountyPlaceCommand(sender: Player, @Completion("#players") targetName: String, @Completion("#amount") amt: Int?) {

        // Perms
        val bountyPlacePermission = conf().getProperty(Config.bountyPlacePermission)
        val bountyByPassPermission = conf().getProperty(Config.bountyByPassPermission)
        // Messages
        val noPermission = conf().getProperty(Config.noPermission)
        val wrongUsage = conf().getProperty(Config.wrongUsage)
        val playerNotFound = conf().getProperty(Config.playerNotFound)
        val targetHasBounty = conf().getProperty(Config.targetHasBounty)
        val notEnoughMoney = conf().getProperty(Config.notEnoughMoney)
        val targetWhitelisted = conf().getProperty(Config.targetWhitelisted)
        val bountyPlacedSelf = conf().getProperty(Config.bountyPlacedSelf)
        val maxBounties = conf().getProperty(Config.maxBounties)
        val bountyOnYourself = conf().getProperty(Config.bountyOnYourself)
        val bountyPlacedEveryone = conf().getProperty(Config.bountyPlacedEveryone).parsePAPI(sender)
        // Others
        val maxBountiesPerPlayer = conf().getProperty(Config.maxBountiesPerPlayer)
        val bountyTax = conf().getProperty(Config.bountyTax)
        val ids = plugin.getBounties().getKeys(false)

        if (!sender.hasPermission(bountyPlacePermission)) {
            noPermission.msg(sender)
            return
        }

        val amount = amt.toString().toIntOrNull()
        if (amount == null) {
            wrongUsage.msg(sender)
            return
        }

        val targetPlayer = getServer().getPlayer(targetName)
        if (targetPlayer == null) {
            playerNotFound.msg(sender)
            return
        }

        if (targetPlayer == sender) {
            bountyOnYourself.msg(sender)
            return
        }

        val balance = econ?.getBalance(sender)
        if (balance == null || balance < amount.toDouble()) {
            notEnoughMoney.msg(sender)
            return
        }

        // Check if the player is whitelisted first
        if (sender.hasPermission(bountyByPassPermission)) {
            targetWhitelisted.msg(sender)
            return
        }

        val bounties = plugin.getBounties()

        var bountiesCounter = 0
        for (id in ids) {
            val placerUniqueIdString = bounties.getString("$id.placer")
            val placerUniqueId = UUID.fromString(placerUniqueIdString)
            if (sender.uniqueId == placerUniqueId) bountiesCounter++
        }

        if (bountiesCounter >= maxBountiesPerPlayer) {
            maxBounties.msg(sender)
            return
        }

        // CHECK IF THERE IS A BOUNTY ON THAT PLAYER ALREADY AND RETURN IF THERE IS
        var exists = false
        for (id in ids) {
            val newId = id.toIntOrNull() ?: continue
            if (newId < minId || newId > maxId) continue
            val targetUniqueIdString = bounties.getString("$id.target") ?: continue
            val targetUniqueId = UUID.fromString(targetUniqueIdString)
            val targetOfflinePlayer = getOfflinePlayer(targetUniqueId)
            val targetOfflinePlayerName = targetOfflinePlayer.name ?: continue
            if (targetOfflinePlayerName == targetName) {
                exists = true
                break
            }
        }
        if (exists) {
            targetHasBounty.msg(sender)
            return
        }

        // IF THERE IS NO BOUNTY ON THAT PLAYER, PLACE ONE. FIRST CREATE A NEW BOUNTY ID
        var bountyId = (minId..maxId).random()
        var idExists = true
        while (idExists) {
            idExists = false
            bountyId = (minId..maxId).random()
            for (id in ids) {
                if (id.toIntOrNull() == null) continue
                if (bountyId == id.toInt()) {
                    idExists = true
                    continue
                }
            }
        }

        val newId = bountyId.toString()
        val currentTimeInSeconds = System.currentTimeMillis() / 1000

        // NOW CREATE THE BOUNTY
        econ?.withdrawPlayer(sender, amount.toDouble())
        bounties.set("$newId.target", targetPlayer.uniqueId.toString())
        bounties.set("$newId.placer", sender.uniqueId.toString())
        bounties.set("$newId.amount", amount)
        bounties.set("$newId.placedTime", currentTimeInSeconds)
        plugin.saveBounties()

        val newAmount = amount - ((bountyTax/100)*amount)

        bountyPlacedSelf.replace("%target%", targetName).replace("%amount%", newAmount.toString()).replace("%bountyId%", newId).msg(sender)
        bountyPlacedEveryone.replace("%target%", targetName).replace("%amount%", newAmount.toString()).replace("%bountyId%", newId).broadcast()
    }

    @SubCommand("add")
    fun bountyAddCommand(sender: Player, bountyId: String, @Completion("#amount") amt: Int?) {

        // Perms
        val bountyAddPermission = conf().getProperty(Config.bountyAddPermission)
        // Messages
        val noPermission = conf().getProperty(Config.noPermission)
        val wrongUsage = conf().getProperty(Config.wrongUsage)
        val bountyNotFound = conf().getProperty(Config.bountyNotFound)
        val notYourBounty = conf().getProperty(Config.notYourBounty)
        val amountUpdated = conf().getProperty(Config.amountUpdated)
        // Others
        val ids = plugin.getBounties().getKeys(false)

        if (!sender.hasPermission(bountyAddPermission)) {
            noPermission.msg(sender)
            return
        }

        if (bountyId.toIntOrNull() == null) {
            wrongUsage.msg(sender)
            return
        }

        val amount = amt.toString().toIntOrNull()
        if (amount == null) {
            wrongUsage.msg(sender)
            return
        }

        if (!ids.contains(bountyId)) {
            bountyNotFound.replace("%bountyId%", bountyId).msg(sender)
            return
        }

        // CHECK IF BOUNTY IS PLACED BY SENDER
        val bounties = plugin.getBounties()
        val placerUniqueIdString = bounties.getString("$bountyId.placer") ?: return
        val placerUniqueId = UUID.fromString(placerUniqueIdString)
        val senderUUID = sender.uniqueId

        if (placerUniqueId != senderUUID) {
            notYourBounty.replace("%bountyId%", bountyId).msg(sender)
            return
        }

        val savedAmount = bounties.getInt("$bountyId.amount")
        val newAmount = savedAmount+amount
        econ?.withdrawPlayer(sender, amount.toDouble())
        bounties.set("$bountyId.amount", newAmount)
        plugin.saveBounties()
        amountUpdated.replace("%newAmount%", amount.toString()).replace("%oldAmount%", savedAmount.toString()).msg(sender)
    }

    @SubCommand("cancel")
    fun bountyCancelCommand(sender: Player, bId: Int?) {

        // Perms
        val bountyCancelPermission = conf().getProperty(Config.bountyCancelPermission)
        // Messages
        val noPermission = conf().getProperty(Config.noPermission)
        val wrongUsage = conf().getProperty(Config.wrongUsage)
        val bountyNotFound = conf().getProperty(Config.bountyNotFound)
        val notYourBounty = conf().getProperty(Config.notYourBounty)
        val bountyCanceled = conf().getProperty(Config.bountyCanceled)
        // Others
        val ids = plugin.getBounties().getKeys(false)

        if (!sender.hasPermission(bountyCancelPermission)) {
            noPermission.msg(sender)
            return
        }

        val bountyId = bId.toString()
        if (bountyId.toIntOrNull() == null) {
            wrongUsage.msg(sender)
            return
        }

        if (!ids.contains(bountyId)) {
            bountyNotFound.replace("%bountyId%", bountyId).msg(sender)
            return
        }

        // CHECK IF BOUNTY IS PLACED BY SENDER
        val bounties = plugin.getBounties()
        val placerUniqueIdString = bounties.getString("$bountyId.placer") ?: return
        val placerUniqueId = UUID.fromString(placerUniqueIdString)
        val senderUUID = sender.uniqueId

        if (placerUniqueId != senderUUID) {
            notYourBounty.replace("%bountyId%", bountyId).msg(sender)
            return
        }

        // REMOVE BOUNTY
        val amount = bounties.getInt("$bountyId.amount")
        econ?.depositPlayer(sender, amount.toDouble())
        bounties.set(bountyId, null)
        plugin.saveBounties()
        bountyCanceled.replace("%bountyId%", bountyId).msg(sender)
    }
}
