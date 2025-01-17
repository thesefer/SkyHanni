package at.hannibal2.skyhanni.data

import at.hannibal2.skyhanni.events.EntityHealthUpdateEvent
import at.hannibal2.skyhanni.events.EntityMaxHealthUpdateEvent
import at.hannibal2.skyhanni.events.PacketEvent
import at.hannibal2.skyhanni.utils.LorenzUtils.baseMaxHealth
import net.minecraft.client.Minecraft
import net.minecraft.client.entity.EntityOtherPlayerMP
import net.minecraft.client.entity.EntityPlayerSP
import net.minecraft.entity.EntityLivingBase
import net.minecraft.entity.boss.EntityWither
import net.minecraft.entity.item.EntityArmorStand
import net.minecraft.entity.item.EntityItem
import net.minecraft.entity.item.EntityItemFrame
import net.minecraft.entity.item.EntityXPOrb
import net.minecraft.network.play.server.S1CPacketEntityMetadata
import net.minecraftforge.event.world.WorldEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.TickEvent

class EntityData {

    private val maxHealthMap = mutableMapOf<EntityLivingBase, Int>()

    @SubscribeEvent
    fun onTick(event: TickEvent.ClientTickEvent) {
        if (event.phase != TickEvent.Phase.START) return

        val minecraft = Minecraft.getMinecraft() ?: return
        val theWorld = minecraft.theWorld ?: return
        for (entity in theWorld.loadedEntityList) {
            if (entity !is EntityLivingBase) continue

            val maxHealth = entity.baseMaxHealth
            val oldMaxHealth = maxHealthMap.getOrDefault(entity, -1)
            if (oldMaxHealth != maxHealth) {
                maxHealthMap[entity] = maxHealth
                EntityMaxHealthUpdateEvent(entity, maxHealth).postAndCatch()
            }
        }
    }

    @SubscribeEvent
    fun onWorldChange(event: WorldEvent.Load) {
        maxHealthMap.clear()
    }

    @SubscribeEvent
    fun onHealthUpdatePacket(event: PacketEvent.ReceiveEvent) {
        val packet = event.packet

        if (packet !is S1CPacketEntityMetadata) return

        val watchableObjects = packet.func_149376_c() ?: return
        val entityId = packet.entityId

        val theWorld = Minecraft.getMinecraft().theWorld ?: return
        val entity = theWorld.getEntityByID(entityId) ?: return
        if (entity is EntityArmorStand) return
        if (entity is EntityXPOrb) return
        if (entity is EntityItem) return
        if (entity is EntityItemFrame) return

        if (entity is EntityOtherPlayerMP) return
        if (entity is EntityPlayerSP) return

        for (watchableObject in watchableObjects) {

            val dataValueId = watchableObject.dataValueId
            val any = watchableObject.`object`
            if (dataValueId != 6) continue

            val health = (any as Float).toInt()

            if (entity is EntityWither) {
                if (health == 300) {
                    if (entityId < 0) return
                }
            }

            if (entity is EntityLivingBase) {
                EntityHealthUpdateEvent(entity, health).postAndCatch()
            }
        }
    }
}