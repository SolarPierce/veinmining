/*
 * Copyright (C) 2020-2022 Illusive Soulworks
 *
 * Vein Mining is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * any later version.
 *
 * Vein Mining is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Vein Mining.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.illusivesoulworks.veinmining;

import com.illusivesoulworks.veinmining.common.data.BlockGroupsResourceListener;
import com.illusivesoulworks.veinmining.common.network.CPacketState;
import com.illusivesoulworks.veinmining.common.veinmining.VeinMiningEvents;
import com.illusivesoulworks.veinmining.data.FabricBlockGroupsResourceListener;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.PackType;

public class VeinMiningFabricMod implements ModInitializer {

  public static final ResourceLocation STATE_PACKET =
      new ResourceLocation(VeinMiningConstants.MOD_ID, "state");
  public static final ResourceLocation NOTIFY_PACKET =
      new ResourceLocation(VeinMiningConstants.MOD_ID, "notify");

  @Override
  public void onInitialize() {
    Registry.register(BuiltInRegistries.ENCHANTMENT, VeinMiningConstants.ENCHANTMENT_ID,
        VeinMiningMod.ENCHANTMENT);
    ServerLifecycleEvents.SERVER_STARTED.register(server -> VeinMiningEvents.reloadDatapack());
    ServerTickEvents.END_WORLD_TICK.register(VeinMiningEvents::tick);
    ResourceManagerHelper resourceManagerHelper = ResourceManagerHelper.get(PackType.SERVER_DATA);
    FabricBlockGroupsResourceListener fabricBlockGroupsResourceListener =
        new FabricBlockGroupsResourceListener();
    BlockGroupsResourceListener.INSTANCE = fabricBlockGroupsResourceListener;
    resourceManagerHelper.registerReloadListener(fabricBlockGroupsResourceListener);
    ServerPlayConnectionEvents.DISCONNECT.register(
        (handler, server) -> VeinMiningEvents.playerLoggedOut(handler.getPlayer()));
    PlayerBlockBreakEvents.AFTER.register((world, player, pos, state, blockEntity) -> {
      if (player instanceof ServerPlayer serverPlayer) {
        VeinMiningEvents.blockBreak(serverPlayer, pos, state);
      }
    });
    ServerEntityEvents.EQUIPMENT_CHANGE.register(
        (livingEntity, equipmentSlot, previousStack, currentStack) -> VeinMiningEvents.toolEquip(
            currentStack, previousStack, equipmentSlot, livingEntity));
    ServerPlayNetworking.registerGlobalReceiver(STATE_PACKET,
        (server, player, handler, buf, responseSender) -> {
          CPacketState msg = CPacketState.decode(buf);
          server.execute(() -> CPacketState.handle(msg, player));
        });
  }
}
