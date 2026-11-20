package io.github.mrbuggi.waterfallmist;

import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.fluid.FluidState;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;

public final class WaterfallManager {
    private static final int SEARCH_RADIUS_XZ = 24;
    private static final int SEARCH_RADIUS_Y = 14;
    private static final int SAMPLES_PER_TICK = 36;
    private static final int MIN_FALL_HEIGHT = 3;
    private static final int MAX_TRACKED_LIFETIME = 50;

    private final Map<BlockPos, WaterfallData> activeWaterfalls = new HashMap<>();
    private final Random random = new Random();
    private int tickCounter = 0;

    private static class WaterfallData {
        int height;
        int lifetime;

        WaterfallData(int height, int lifetime) {
            this.height = height;
            this.lifetime = lifetime;
        }
    }

    public void tick(MinecraftClient client) {
        ClientWorld world = client.world;
        if (world == null || client.player == null) {
            activeWaterfalls.clear();
            return;
        }

        BlockPos playerPos = client.player.getBlockPos();
        tickCounter++;

        scanForWaterfalls(world, playerPos);
        emitParticlesAndSound(world, playerPos);
    }

    private void scanForWaterfalls(ClientWorld world, BlockPos center) {
        BlockPos.Mutable mutable = new BlockPos.Mutable();

        for (int i = 0; i < SAMPLES_PER_TICK; i++) {
            int dx = random.nextInt(SEARCH_RADIUS_XZ * 2 + 1) - SEARCH_RADIUS_XZ;
            int dy = random.nextInt(SEARCH_RADIUS_Y * 2 + 1) - SEARCH_RADIUS_Y;
            int dz = random.nextInt(SEARCH_RADIUS_XZ * 2 + 1) - SEARCH_RADIUS_XZ;

            mutable.set(center.getX() + dx, center.getY() + dy, center.getZ() + dz);

            // Only proceed if this exact block is the true bottom impact point
            if (!isWaterImpactBase(world, mutable)) {
                continue;
            }

            // Measure how high the water fell to reach this impact point
            BlockPos.Mutable tracer = mutable.mutableCopy();
            int height = 0;
            while (height < 24) {
                tracer.move(Direction.UP);
                FluidState stepFluid = world.getFluidState(tracer);
                if (stepFluid.isEmpty() || !stepFluid.isIn(FluidTags.WATER)) {
                    break;
                }
                height++;
            }

            if (height >= MIN_FALL_HEIGHT) {
                BlockPos impactPos = mutable.toImmutable();
                WaterfallData existing = activeWaterfalls.get(impactPos);
                if (existing != null) {
                    existing.lifetime = MAX_TRACKED_LIFETIME;
                    existing.height = Math.max(existing.height, height);
                } else {
                    activeWaterfalls.put(impactPos, new WaterfallData(height, MAX_TRACKED_LIFETIME));
                }
            }
        }
    }

    private boolean isWaterImpactBase(ClientWorld world, BlockPos pos) {
        FluidState currentFluid = world.getFluidState(pos);
        if (!currentFluid.isIn(FluidTags.WATER)) {
            return false;
        }

        // Must have falling water directly above
        FluidState aboveFluid = world.getFluidState(pos.up());
        if (aboveFluid.isEmpty() || !aboveFluid.isIn(FluidTags.WATER) || aboveFluid.isStill()) {
            return false;
        }

        // The vertical fall MUST terminate here — check below
        BlockPos downPos = pos.down();
        BlockState belowState = world.getBlockState(downPos);

        // 1. Water hits solid ground (stone, dirt, floor)
        if (!belowState.getCollisionShape(world, downPos).isEmpty() || belowState.isSolid()) {
            return true;
        }

        // 2. If below is air, water is still falling mid-air
        if (belowState.isAir()) {
            return false;
        }

        // 3. Water hits a pool or lake
        FluidState belowFluid = world.getFluidState(downPos);
        if (belowFluid.isIn(FluidTags.WATER)) {
            // Pool is still water
            if (belowFluid.isStill() || currentFluid.isStill()) {
                return true;
            }
            // Shallow water on top of solid block
            BlockState floorState = world.getBlockState(downPos.down());
            if (!floorState.getCollisionShape(world, downPos.down()).isEmpty() || floorState.isSolid()) {
                return true;
            }
        }

        // Otherwise it's still dropping down through open space
        return false;
    }

    private void emitParticlesAndSound(ClientWorld world, BlockPos playerPos) {
        if (activeWaterfalls.isEmpty()) {
            return;
        }

        Iterator<Map.Entry<BlockPos, WaterfallData>> it = activeWaterfalls.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<BlockPos, WaterfallData> entry = it.next();
            BlockPos pos = entry.getKey();
            WaterfallData data = entry.getValue();

            data.lifetime--;

            double distSq = pos.getSquaredDistance(playerPos);
            if (distSq > (SEARCH_RADIUS_XZ + 8) * (SEARCH_RADIUS_XZ + 8) || data.lifetime <= 0) {
                if (!isWaterImpactBase(world, pos)) {
                    it.remove();
                    continue;
                }
                data.lifetime = MAX_TRACKED_LIFETIME;
            }

            spawnWaterfallParticles(world, pos, data.height);

            if (distSq < 144.0 && (tickCounter % 25 == 0) && random.nextFloat() < 0.45F) {
                float pitch = 0.35F + random.nextFloat() * 0.15F;
                float volume = 0.12F + Math.min(data.height * 0.015F, 0.18F);
                world.playSound(
                    pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                    SoundEvents.BLOCK_WATER_AMBIENT,
                    SoundCategory.AMBIENT,
                    volume, pitch, false
                );
            }
        }
    }

    private void spawnWaterfallParticles(ClientWorld world, BlockPos pos, int height) {
        // Particles radiate in a ring OUTWARD from the stream impact center
        double angle = random.nextDouble() * Math.PI * 2.0;
        double mistDist = 0.45 + random.nextDouble() * 0.55;
        double px = pos.getX() + 0.5 + Math.cos(angle) * mistDist;
        double pz = pos.getZ() + 0.5 + Math.sin(angle) * mistDist;
        double py = pos.getY() + 0.95 + random.nextDouble() * 0.15;

        double mvx = Math.cos(angle) * (0.015 + random.nextDouble() * 0.025);
        double mvy = 0.025 + random.nextDouble() * 0.035;
        double mvz = Math.sin(angle) * (0.015 + random.nextDouble() * 0.025);

        // 1. Pure white mist
        if (random.nextFloat() < (height >= 8 ? 0.70F : 0.45F)) {
            world.addParticle(WaterfallMistClient.MIST, px, py, pz, mvx, mvy, mvz);
        }

        // 2. Soft white cloud puff
        if (random.nextFloat() < 0.30F) {
            double cx = pos.getX() + 0.5 + Math.cos(angle) * (0.40 + random.nextDouble() * 0.40);
            double cz = pos.getZ() + 0.5 + Math.sin(angle) * (0.40 + random.nextDouble() * 0.40);
            world.addParticle(ParticleTypes.CLOUD, cx, py + 0.05, cz, mvx * 0.5, 0.015, mvz * 0.5);
        }

        // 3. Water splashes jumping outward from impact
        int splashCount = 1 + (height >= 6 ? 2 : 1);
        for (int i = 0; i < splashCount; i++) {
            double sAngle = random.nextDouble() * Math.PI * 2.0;
            double sDist = 0.30 + random.nextDouble() * 0.35;
            double sx = pos.getX() + 0.5 + Math.cos(sAngle) * sDist;
            double sz = pos.getZ() + 0.5 + Math.sin(sAngle) * sDist;
            double svx = Math.cos(sAngle) * (0.04 + random.nextDouble() * 0.08);
            double svy = 0.12 + random.nextDouble() * 0.14;
            double svz = Math.sin(sAngle) * (0.04 + random.nextDouble() * 0.08);
            world.addParticle(ParticleTypes.SPLASH, sx, py, sz, svx, svy, svz);
        }

        // 4. Surface foam bubbles
        if (random.nextFloat() < 0.55F) {
            double fAngle = random.nextDouble() * Math.PI * 2.0;
            double fDist = 0.35 + random.nextDouble() * 0.45;
            double bx = pos.getX() + 0.5 + Math.cos(fAngle) * fDist;
            double bz = pos.getZ() + 0.5 + Math.sin(fAngle) * fDist;
            world.addParticle(ParticleTypes.BUBBLE_POP, bx, py, bz, 0.0, 0.015, 0.0);
        }
    }
}
