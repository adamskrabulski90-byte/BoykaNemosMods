package com.boyka.dildo;

import com.boyka.dildo.gui.BoykaScreen;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.block.SlabBlock;
import net.minecraft.block.TrapdoorBlock;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.vehicle.*;
import net.minecraft.entity.decoration.*;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.RaycastContext.FluidHandling;
import net.minecraft.world.RaycastContext.ShapeType;
import org.lwjgl.glfw.GLFW;

import java.util.List;
import java.util.OptionalInt;

public class BoykaDildoMod implements ClientModInitializer {

    private int tickCounter = 0;
    private static boolean menuKeyWasDown = false;
    private static boolean dripKeyWasDown = false;
    private static boolean trapKeyWasDown = false;

    @Override
    public void onInitializeClient() {
        BoykaConfig.load();

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            handleKeybinds(client);

            if (client.player == null || client.world == null) return;

            tickCounter++;

            if (BoykaConfig.autoDripEnabled) {
                handleAutoDrip(client);
            }

            if (BoykaConfig.trapHelperEnabled) {
                handleTrapHelper(client);
            }
        });
    }

    private void handleKeybinds(MinecraftClient client) {
        long handle = client.getWindow().getHandle();

        boolean menuDown = GLFW.glfwGetKey(handle, BoykaConfig.keyMenu) == GLFW.GLFW_PRESS;
        if (menuDown && !menuKeyWasDown && client.currentScreen == null) {
            client.setScreen(new BoykaScreen());
        }
        menuKeyWasDown = menuDown;

        boolean dripDown = GLFW.glfwGetKey(handle, BoykaConfig.keyAutoDrip) == GLFW.GLFW_PRESS;
        if (dripDown && !dripKeyWasDown) {
            BoykaConfig.autoDripEnabled = !BoykaConfig.autoDripEnabled;
            notify(client, "Auto Drip: " + (BoykaConfig.autoDripEnabled ? "ON" : "OFF"));
        }
        dripKeyWasDown = dripDown;

        boolean trapDown = GLFW.glfwGetKey(handle, BoykaConfig.keyTrapHelper) == GLFW.GLFW_PRESS;
        if (trapDown && !trapKeyWasDown) {
            BoykaConfig.trapHelperEnabled = !BoykaConfig.trapHelperEnabled;
            notify(client, "Trap Helper: " + (BoykaConfig.trapHelperEnabled ? "ON" : "OFF"));
        }
        trapKeyWasDown = trapDown;
    }

    private void notify(MinecraftClient client, String msg) {
        if (client.player != null) {
            client.player.sendMessage(net.minecraft.text.Text.literal("[BoykaDILDO] " + msg), true);
        }
    }

    // ---------------- AUTO DRIP ----------------
    // Wymaga: trzymasz Pointed Dripstone, patrzysz na klapę (trapdoor) w normalnym zasięgu.
    private int dripTickCounter = 0;

    private void handleAutoDrip(MinecraftClient client) {
        ClientPlayerEntity player = client.player;
        if (player == null) return;

        boolean holdingDripstone = player.getMainHandStack().getItem() == Items.POINTED_DRIPSTONE
                || player.getOffHandStack().getItem() == Items.POINTED_DRIPSTONE;
        if (!holdingDripstone) return;

        dripTickCounter++;
        if (dripTickCounter < Math.max(1, BoykaConfig.autoDripDelayTicks)) return;
        dripTickCounter = 0;

        HitResult hit = client.crosshairTarget;
        if (hit == null || hit.getType() != HitResult.Type.BLOCK) return;

        BlockHitResult blockHit = (BlockHitResult) hit;
        BlockPos pos = blockHit.getBlockPos();

        if (!(client.world.getBlockState(pos).getBlock() instanceof TrapdoorBlock)) return;

        client.interactionManager.interactBlock(player, Hand.MAIN_HAND, blockHit);
    }

    // ---------------- TRAP HELPER ----------------
    private void handleTrapHelper(MinecraftClient client) {
        ClientPlayerEntity player = client.player;
        if (player == null) return;

        tickCounter++;
        int delay = Math.max(1, BoykaConfig.trapHelperDelayTicks == 0 ? 1 : BoykaConfig.trapHelperDelayTicks * 2);
        if (tickCounter % delay != 0) return;

        double reach = BoykaConfig.trapHelperReach;
        Box box = player.getBoundingBox().expand(reach);

        List<Entity> targets = client.world.getOtherEntities(player, box, this::isTargetEntity);

        boolean destroyedSomething = false;

        for (Entity target : targets) {
            if (player.distanceTo(target) > reach) continue;
            if (!hasLineOfSight(client, player, target)) continue;
            if (!isCategoryEnabled(target)) continue;

            client.interactionManager.attackEntity(player, target);
            player.swingHand(Hand.MAIN_HAND);
            destroyedSomething = true;
        }

        if (destroyedSomething && BoykaConfig.autoCobweb) {
            placeCobwebAtFeet(client, player);
        }
    }

    private boolean isTargetEntity(Entity e) {
        return e instanceof AbstractMinecartEntity
                || e instanceof ItemFrameEntity
                || e instanceof GlowItemFrameEntity
                || e instanceof PaintingEntity;
    }

    private boolean isCategoryEnabled(Entity e) {
        if (e instanceof TntMinecartEntity) return BoykaConfig.destroyMinecartTnt;
        if (e instanceof HopperMinecartEntity) return BoykaConfig.destroyMinecartHopper;
        if (e instanceof FurnaceMinecartEntity) return BoykaConfig.destroyMinecartFurnace;
        if (e instanceof ChestMinecartEntity) return BoykaConfig.destroyMinecartChest;
        if (e instanceof MinecartEntity) return BoykaConfig.destroyMinecartNormal;
        if (e instanceof PaintingEntity) return BoykaConfig.destroyPaintings;
        if (e instanceof ItemFrameEntity || e instanceof GlowItemFrameEntity) return BoykaConfig.destroyBanners;
        return false;
    }

    private boolean hasLineOfSight(MinecraftClient client, ClientPlayerEntity player, Entity target) {
        HitResult hit = client.world.raycast(new RaycastContext(
                player.getCameraPosVec(1.0f),
                target.getPos().add(0, target.getHeight() / 2.0, 0),
                ShapeType.COLLIDER,
                FluidHandling.NONE,
                player
        ));
        return hit.getType() == HitResult.Type.MISS;
    }

    private void placeCobwebAtFeet(MinecraftClient client, ClientPlayerEntity player) {
        OptionalInt slot = findCobwebSlot(player);
        if (slot.isEmpty()) return;

        int prevSlot = player.getInventory().selectedSlot;
        player.getInventory().selectedSlot = slot.getAsInt();

        BlockPos feet = player.getBlockPos();
        BlockHitResult fakeHit = new BlockHitResult(
                player.getPos(), net.minecraft.util.math.Direction.UP, feet.down(), false
        );
        client.interactionManager.interactBlock(player, Hand.MAIN_HAND, fakeHit);

        player.getInventory().selectedSlot = prevSlot;
    }

    private OptionalInt findCobwebSlot(ClientPlayerEntity player) {
        for (int i = 0; i < 9; i++) {
            if (player.getInventory().getStack(i).getItem() == Items.COBWEB) {
                return OptionalInt.of(i);
            }
        }
        return OptionalInt.empty();
    }
}
