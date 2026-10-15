package com.boyka.dildo;

import com.boyka.dildo.gui.BoykaScreen;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.block.TrapdoorBlock;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.GlowItemFrameEntity;
import net.minecraft.entity.decoration.ItemFrameEntity;
import net.minecraft.entity.decoration.painting.PaintingEntity;
import net.minecraft.entity.vehicle.AbstractMinecartEntity;
import net.minecraft.entity.vehicle.ChestMinecartEntity;
import net.minecraft.entity.vehicle.FurnaceMinecartEntity;
import net.minecraft.entity.vehicle.HopperMinecartEntity;
import net.minecraft.entity.vehicle.MinecartEntity;
import net.minecraft.entity.vehicle.TntMinecartEntity;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.world.RaycastContext;
import org.lwjgl.glfw.GLFW;

import java.util.List;
import java.util.OptionalInt;

public class BoykaDildoMod implements ClientModInitializer {

    private int trapTickCounter = 0;
    private int dripTickCounter = 0;

    private static boolean menuKeyWasDown = false;
    private static boolean dripKeyWasDown = false;
    private static boolean trapKeyWasDown = false;

    @Override
    public void onInitializeClient() {
        BoykaConfig.load();

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            handleKeybinds(client);

            if (client.player == null || client.world == null) {
                return;
            }

            if (BoykaConfig.autoDripEnabled) {
                handleAutoDrip(client);
            }

            if (BoykaConfig.trapHelperEnabled) {
                handleTrapHelper(client);
            }
        });
    }

    private void handleKeybinds(MinecraftClient client) {
        long windowHandle = client.getWindow().getHandle();

        boolean menuDown = GLFW.glfwGetKey(
                windowHandle,
                BoykaConfig.keyMenu
        ) == GLFW.GLFW_PRESS;

        if (menuDown && !menuKeyWasDown && client.currentScreen == null) {
            client.setScreen(new BoykaScreen());
        }

        menuKeyWasDown = menuDown;

        boolean dripDown = GLFW.glfwGetKey(
                windowHandle,
                BoykaConfig.keyAutoDrip
        ) == GLFW.GLFW_PRESS;

        if (dripDown && !dripKeyWasDown) {
            BoykaConfig.autoDripEnabled = !BoykaConfig.autoDripEnabled;

            notifyPlayer(
                    client,
                    "Auto Drip: " + (BoykaConfig.autoDripEnabled ? "ON" : "OFF")
            );
        }

        dripKeyWasDown = dripDown;

        boolean trapDown = GLFW.glfwGetKey(
                windowHandle,
                BoykaConfig.keyTrapHelper
        ) == GLFW.GLFW_PRESS;

        if (trapDown && !trapKeyWasDown) {
            BoykaConfig.trapHelperEnabled = !BoykaConfig.trapHelperEnabled;

            notifyPlayer(
                    client,
                    "Trap Helper: " + (BoykaConfig.trapHelperEnabled ? "ON" : "OFF")
            );
        }

        trapKeyWasDown = trapDown;
    }

    private void notifyPlayer(MinecraftClient client, String message) {
        if (client.player != null) {
            client.player.sendMessage(
                    net.minecraft.text.Text.literal("[BoykaDILDO] " + message),
                    true
            );
        }
    }

    // AUTO DRIP:
    // Trzymaj pointed dripstone i patrz na klapę.
    private void handleAutoDrip(MinecraftClient client) {
        ClientPlayerEntity player = client.player;

        if (player == null || client.interactionManager == null) {
            return;
        }

        boolean holdingDripstone =
                player.getMainHandStack().getItem() == Items.POINTED_DRIPSTONE
                        || player.getOffHandStack().getItem() == Items.POINTED_DRIPSTONE;

        if (!holdingDripstone) {
            return;
        }

        dripTickCounter++;

        int dripDelay = Math.max(1, BoykaConfig.autoDripDelayTicks);

        if (dripTickCounter < dripDelay) {
            return;
        }

        dripTickCounter = 0;

        HitResult hitResult = client.crosshairTarget;

        if (hitResult == null || hitResult.getType() != HitResult.Type.BLOCK) {
            return;
        }

        BlockHitResult blockHit = (BlockHitResult) hitResult;
        BlockPos blockPos = blockHit.getBlockPos();

        if (!(client.world.getBlockState(blockPos).getBlock() instanceof TrapdoorBlock)) {
            return;
        }

        client.interactionManager.interactBlock(
                player,
                Hand.MAIN_HAND,
                blockHit
        );
    }

    // TRAP HELPER:
    // Normalny zasięg, maksymalnie 3 bloki oraz wymagana widoczność celu.
    private void handleTrapHelper(MinecraftClient client) {
        ClientPlayerEntity player = client.player;

        if (player == null || client.interactionManager == null) {
            return;
        }

        trapTickCounter++;

        int configuredDelay = BoykaConfig.trapHelperDelayTicks;
        int delayTicks = configuredDelay == 0
                ? 1
                : Math.max(1, configuredDelay * 2);

        if (trapTickCounter < delayTicks) {
            return;
        }

        trapTickCounter = 0;

        double reach = Math.min(3.0D, Math.max(1.0D, BoykaConfig.trapHelperReach));

        Box searchBox = player.getBoundingBox().expand(reach);

        List<Entity> targets = client.world.getOtherEntities(
                player,
                searchBox,
                this::isTargetEntity
        );

        boolean attackedTarget = false;

        for (Entity target : targets) {
            if (player.distanceTo(target) > reach) {
                continue;
            }

            if (!hasLineOfSight(client, player, target)) {
                continue;
            }

            if (!isCategoryEnabled(target)) {
                continue;
            }

            client.interactionManager.attackEntity(player, target);
            player.swingHand(Hand.MAIN_HAND);

            attackedTarget = true;
        }

        if (attackedTarget && BoykaConfig.autoCobweb) {
            placeCobwebAtFeet(client, player);
        }
    }

    private boolean isTargetEntity(Entity entity) {
        return entity instanceof AbstractMinecartEntity
                || entity instanceof ItemFrameEntity
                || entity instanceof GlowItemFrameEntity
                || entity instanceof PaintingEntity;
    }

    private boolean isCategoryEnabled(Entity entity) {
        if (entity instanceof TntMinecartEntity) {
            return BoykaConfig.destroyMinecartTnt;
        }

        if (entity instanceof HopperMinecartEntity) {
            return BoykaConfig.destroyMinecartHopper;
        }

        if (entity instanceof FurnaceMinecartEntity) {
            return BoykaConfig.destroyMinecartFurnace;
        }

        if (entity instanceof ChestMinecartEntity) {
            return BoykaConfig.destroyMinecartChest;
        }

        if (entity instanceof MinecartEntity) {
            return BoykaConfig.destroyMinecartNormal;
        }

        if (entity instanceof PaintingEntity) {
            return BoykaConfig.destroyPaintings;
        }

        if (entity instanceof ItemFrameEntity
                || entity instanceof GlowItemFrameEntity) {
            return BoykaConfig.destroyBanners;
        }

        return false;
    }

    private boolean hasLineOfSight(
            MinecraftClient client,
            ClientPlayerEntity player,
            Entity target
    ) {
        HitResult raycastResult = client.world.raycast(
                new RaycastContext(
                        player.getCameraPosVec(1.0F),
                        target.getPos().add(
                                0.0D,
                                target.getHeight() / 2.0D,
                                0.0D
                        ),
                        RaycastContext.ShapeType.COLLIDER,
                        RaycastContext.FluidHandling.NONE,
                        player
                )
        );

        return raycastResult.getType() == HitResult.Type.MISS;
    }

    private void placeCobwebAtFeet(
            MinecraftClient client,
            ClientPlayerEntity player
    ) {
        if (client.interactionManager == null) {
            return;
        }

        OptionalInt cobwebSlot = findCobwebSlot(player);

        if (cobwebSlot.isEmpty()) {
            return;
        }

        int previousSlot = player.getInventory().selectedSlot;
        player.getInventory().selectedSlot = cobwebSlot.getAsInt();

        BlockPos feetPosition = player.getBlockPos();

        BlockHitResult cobwebPlacementHit = new BlockHitResult(
                player.getPos(),
                Direction.UP,
                feetPosition.down(),
                false
        );

        client.interactionManager.interactBlock(
                player,
                Hand.MAIN_HAND,
                cobwebPlacementHit
        );

        player.getInventory().selectedSlot = previousSlot;
    }

    private OptionalInt findCobwebSlot(ClientPlayerEntity player) {
        for (int slot = 0; slot < 9; slot++) {
            if (player.getInventory().getStack(slot).getItem() == Items.COBWEB) {
                return OptionalInt.of(slot);
            }
        }

        return OptionalInt.empty();
    }
}
