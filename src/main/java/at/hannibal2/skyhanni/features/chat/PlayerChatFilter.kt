package at.hannibal2.skyhanni.features.chat

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.LorenzChatEvent
import at.hannibal2.skyhanni.events.PlayerSendChatEvent
import at.hannibal2.skyhanni.utils.LorenzLogger
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.LorenzUtils.removeColor
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class PlayerChatFilter {

    @SubscribeEvent
    fun onChatMessage(event: LorenzChatEvent) {
        if (!LorenzUtils.isOnHypixel) return
        if (!SkyHanniMod.feature.chat.playerMessages) return

        if (shouldBlock(event.message)) {
            event.blockedReason = "player_chat"
        }
    }

    val loggerPlayerChat = LorenzLogger("chat/player")

    fun shouldBlock(originalMessage: String): Boolean {
        val split: List<String> = if (originalMessage.contains("§7§r§7: ")) {
            originalMessage.split("§7§r§7: ")
        } else if (originalMessage.contains("§f: ")) {
            originalMessage.split("§f: ")
        } else {
            return false
        }

        var rawName = split[0]
        val message = split[1]

        val channel: PlayerMessageChannel
        if (rawName.startsWith("§9Party §8> ")) {
            channel = PlayerMessageChannel.PARTY
            rawName = rawName.substring(12)
        } else if (rawName.startsWith("§2Guild > ")) {
            channel = PlayerMessageChannel.GUILD
            rawName = rawName.substring(10)
        } else if (rawName.startsWith("§bCo-op > ")) {
            channel = PlayerMessageChannel.COOP
            rawName = rawName.substring(10)
        } else {
            channel = PlayerMessageChannel.ALL
        }

        val nameSplit = rawName.split(" ")
        val first = nameSplit[0]

        val last = nameSplit.last()
        val name = if (last.endsWith("]")) {
            nameSplit[nameSplit.size - 2]
        } else {
            last
        }

        if (first != name) {
            if (!first.contains("VIP") && !first.contains("MVP")) {
                //TODO support yt + admin
                return false
            }
        }

        send(channel, name.removeColor(), message.removeColor())
        return true
    }

    private fun send(channel: PlayerMessageChannel, name: String, message: String) {
        loggerPlayerChat.log("[$channel] $name: $message")
        val event = PlayerSendChatEvent(channel, name, message)
        event.postAndCatch()

        if (event.cancelledReason != "") {
            loggerPlayerChat.log("cancelled: " + event.cancelledReason)
        } else {
            val finalMessage = event.message
            if (finalMessage != message) {
                loggerPlayerChat.log("message changed: $finalMessage")
            }

            val prefix = channel.prefix
            LorenzUtils.chat("$prefix §b$name §f$finalMessage")
        }
    }

}