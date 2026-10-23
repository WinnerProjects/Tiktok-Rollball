package com.github.tacowasa059.katamariio.mixin.common;

import com.github.tacowasa059.katamariio.KatamariIO;
import com.github.tacowasa059.katamariio.common.KatamariAbsorb;
import com.github.tacowasa059.katamariio.common.KatamariMovement;
import com.github.tacowasa059.katamariio.common.KatamariShell;
import com.github.tacowasa059.katamariio.common.KatamariWin;
import net.minecraft.server.level.ServerPlayer;
import com.github.tacowasa059.katamariio.common.accessors.ICustomPlayerData;
import com.github.tacowasa059.katamariio.common.networks.S2CPlayerPacket;
import com.github.tacowasa059.katamariio.common.serializers.ModDataSerializers;
import com.github.tacowasa059.katamariio.common.utils.QuaternionUtils;
import com.mojang.serialization.Codec;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.Identifier;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Mixin(Player.class)
public abstract class PlayerMixin implements ICustomPlayerData {
    @Unique
    private static final EntityDataAccessor<Float> sphericalPlayerMod$SIZE = SynchedEntityData.defineId(Player.class, EntityDataSerializers.FLOAT);
    @Unique
    private static final EntityDataAccessor<Float> sphericalPlayerMod$RENDER_SIZE = SynchedEntityData.defineId(Player.class, EntityDataSerializers.FLOAT);
    @Unique
    private static final EntityDataAccessor<Boolean> sphericalPlayerMod$FLAG = SynchedEntityData.defineId(Player.class, EntityDataSerializers.BOOLEAN);
    @Unique
    private static final EntityDataAccessor<Float> RESTITUTION_COEFFICIENT = SynchedEntityData.defineId(Player.class, EntityDataSerializers.FLOAT);
    @Unique
    private static final EntityDataAccessor<CompoundTag> sphericalPlayerMod$QUATERNION = SynchedEntityData.defineId(Player.class, ModDataSerializers.FLOAT_COMPOUND);
    @Unique
    private static final EntityDataAccessor<CompoundTag> CURRENT_POSITION = SynchedEntityData.defineId(Player.class, ModDataSerializers.FLOAT_COMPOUND);
    @Unique
    private static final EntityDataAccessor<List<Vec3>> SPHERICAL_PLAYER_POSITION_LIST = SynchedEntityData.defineId(Player.class, ModDataSerializers.VEC3_LIST);
    @Unique
    private static final EntityDataAccessor<List<Quaternionf>> SPHERICAL_PLAYER_QUATERNION_LIST = SynchedEntityData.defineId(Player.class, ModDataSerializers.QUATERNION_LIST);
    @Unique
    private static final EntityDataAccessor<List<Block>> SPHERICAL_PLAYER_BLOCK_LIST = SynchedEntityData.defineId(Player.class, ModDataSerializers.BLOCK_LIST);
    @Unique
    private static final EntityDataAccessor<Integer> SPHERICAL_PLAYER_BLOCK_COUNT = SynchedEntityData.defineId(Player.class, EntityDataSerializers.INT);

    @Unique
    private boolean sphericalPlayerMod$initialized = false;
    @Unique
    private Quaternionf sphericalPlayerMod$quaternion = new Quaternionf(0, 0, 0, 1);
    @Unique
    private Quaternionf sphericalPlayerMod$prevQuaternion = new Quaternionf(0, 0, 0, 1);
    @Unique
    private static final int KATAMARI_COLLISION_LAT_BINS = 6;
    @Unique
    private static final int KATAMARI_COLLISION_LON_BINS = 12;
    @Unique
    private static final float KATAMARI_BLOCK_EXTENT = 0.5f;
    /** Volume every eaten block adds to the ball (1.0 = a full block). Lower = slower growth. */
    @Unique
    private static final float KATAMARI_GROWTH_VOLUME_PER_BLOCK = 0.012f;
    @Unique
    private static final float KATAMARI_MAX_SIZE = 20.0f;
    @Unique
    private float katamariIO$exactSize = -1.0f;
    @Unique
    private static final String SPM_ATTACHED_BLOCKS = "SPM_AttachedBlocks";
    @Unique
    private float[] katamariIO$binMaxRadii = null;
    @Unique
    private boolean katamariIO$binRadiiDirty = true;
    /** Server-side master copy of the block shell (one block per slot); synced to clients in throttled batches. */
    @Unique
    private List<Block> katamariIO$shell = null;
    @Unique
    private boolean katamariIO$shellDirty = false;

    @Inject(method = "tick", at = @At("HEAD"))
    protected void tick(CallbackInfo ci) {
        if (!sphericalPlayerMod$initialized) {
            sphericalPlayerMod$quaternion = katamariIO$getValidQuaternion(katamariIO$getQuaternion());
            sphericalPlayerMod$prevQuaternion = katamariIO$getValidQuaternion(katamariIO$getQuaternion());
            sphericalPlayerMod$initialized = true;
        }

        Player player = (Player) (Object) this;
        if (player.level().isClientSide()) {
            sphericalPlayerMod$prevQuaternion = katamariIO$getValidQuaternion(new Quaternionf(sphericalPlayerMod$quaternion));
            Quaternionf quaternion = QuaternionUtils.getUpdatedQuaternion(player.position(),
                    katamariIO$getCurrentPosition(), (ICustomPlayerData) player);
            if (quaternion == null) {
                quaternion = katamariIO$getQuaternion();
            }
            sphericalPlayerMod$quaternion = katamariIO$getValidQuaternion(quaternion);
            EntityDimensions expectedDimensions = player.getDimensions(player.getPose());
            if (player.tickCount % 5 == 0
                    && (Math.abs(expectedDimensions.width() - (float) player.getBoundingBox().getXsize()) > 1.0E-4f
                    || Math.abs(expectedDimensions.height() - (float) player.getBoundingBox().getYsize()) > 1.0E-4f)) {
                player.refreshDimensions();
            }
        } else {
            KatamariAbsorb.tick(player);
            katamariIO$serverShellTick(player);
            if (player instanceof ServerPlayer serverPlayer) {
                KatamariWin.tick(serverPlayer);
            }
        }
    }

    @Override
    public Quaternionf katamariIO$getInterpolatedQuaternion(float partialTicks) {
        return QuaternionUtils.slerp(sphericalPlayerMod$prevQuaternion, sphericalPlayerMod$quaternion, partialTicks);
    }

    @Inject(method = "defineSynchedData", at = @At("TAIL"))
    protected void defineSynchedData(SynchedEntityData.Builder builder, CallbackInfo ci) {
        builder.define(sphericalPlayerMod$SIZE, KatamariIO.DEFAULT_BALL_SIZE);
        builder.define(sphericalPlayerMod$RENDER_SIZE, KatamariIO.DEFAULT_BALL_SIZE);
        builder.define(sphericalPlayerMod$FLAG, true);
        CompoundTag quaternionTag = new CompoundTag();
        quaternionTag.putFloat("x", 0f);
        quaternionTag.putFloat("y", 0f);
        quaternionTag.putFloat("z", 0f);
        quaternionTag.putFloat("w", 1f);
        builder.define(sphericalPlayerMod$QUATERNION, quaternionTag);
        builder.define(RESTITUTION_COEFFICIENT, 0.55f);

        CompoundTag positionTag = new CompoundTag();
        positionTag.putFloat("x", 0);
        positionTag.putFloat("y", 0);
        positionTag.putFloat("z", 0);
        builder.define(CURRENT_POSITION, positionTag);

        builder.define(SPHERICAL_PLAYER_POSITION_LIST, new ArrayList<>());
        builder.define(SPHERICAL_PLAYER_QUATERNION_LIST, new ArrayList<>());
        builder.define(SPHERICAL_PLAYER_BLOCK_LIST, new ArrayList<>());
        builder.define(SPHERICAL_PLAYER_BLOCK_COUNT, 0);
    }

    @Inject(method = "addAdditionalSaveData", at = @At("TAIL"))
    private void writeAdditional(ValueOutput output, CallbackInfo ci) {
        Player entity = (Player) (Object) this;
        SynchedEntityData dataManager = entity.getEntityData();
        output.store("SPM_Size", Codec.FLOAT, dataManager.get(sphericalPlayerMod$SIZE));
        output.store("SPM_RenderSize", Codec.FLOAT, dataManager.get(sphericalPlayerMod$RENDER_SIZE));
        output.store("SPM_isBall", Codec.BOOL, dataManager.get(sphericalPlayerMod$FLAG));
        output.store("SPM_Quaternion", CompoundTag.CODEC, dataManager.get(sphericalPlayerMod$QUATERNION));
        output.store("SPM_RESTITUTION", Codec.FLOAT, dataManager.get(RESTITUTION_COEFFICIENT));
        output.store("SPM_POSITION", CompoundTag.CODEC, dataManager.get(CURRENT_POSITION));
        output.store("SPM_BlockCount", Codec.INT, dataManager.get(SPHERICAL_PLAYER_BLOCK_COUNT));

        List<Block> blocks = katamariIO$shell != null ? katamariIO$shell : dataManager.get(SPHERICAL_PLAYER_BLOCK_LIST);
        ValueOutput.ValueOutputList attachedList = output.childrenList(SPM_ATTACHED_BLOCKS);
        for (Block block : blocks) {
            Identifier key = BuiltInRegistries.BLOCK.getKey(block);
            if (key == null) {
                continue;
            }
            ValueOutput entry = attachedList.addChild();
            entry.store("block", Codec.STRING, key.toString());
        }
    }

    @Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
    private void readAdditional(ValueInput input, CallbackInfo ci) {
        Player entity = (Player) (Object) this;
        SynchedEntityData dataManager = entity.getEntityData();

        input.read("SPM_Size", Codec.FLOAT).ifPresent(this::katamariIO$setSize);
        Optional<Float> renderSize = input.read("SPM_RenderSize", Codec.FLOAT);
        if (renderSize.isPresent()) {
            katamariIO$setRenderSize(renderSize.get());
        } else {
            input.read("SPM_Size", Codec.FLOAT).ifPresent(this::katamariIO$setRenderSize);
        }
        input.read("SPM_RESTITUTION", Codec.FLOAT).ifPresent(this::katamariIO$setRestitutionCoefficient);
        input.read("SPM_isBall", Codec.BOOL).ifPresent(this::katamariIO$setFlag);
        input.read("SPM_Quaternion", CompoundTag.CODEC)
                .ifPresent(tag -> dataManager.set(sphericalPlayerMod$QUATERNION, tag));
        input.read("SPM_POSITION", CompoundTag.CODEC)
                .ifPresent(tag -> dataManager.set(CURRENT_POSITION, tag));
        Optional<Integer> blockCount = input.read("SPM_BlockCount", Codec.INT);
        if (blockCount.isPresent()) {
            dataManager.set(SPHERICAL_PLAYER_BLOCK_COUNT, blockCount.get());
        } else {
            dataManager.set(SPHERICAL_PLAYER_BLOCK_COUNT, dataManager.get(SPHERICAL_PLAYER_BLOCK_LIST).size());
        }

        Optional<ValueInput.ValueInputList> attachedOpt = input.childrenList(SPM_ATTACHED_BLOCKS);
        if (attachedOpt.isPresent()) {
            // Only the block types are stored; their place on the ball follows from the slot index and the size.
            List<Block> blocks = new ArrayList<>();
            for (ValueInput entry : attachedOpt.get()) {
                Identifier key = Identifier.tryParse(entry.read("block", Codec.STRING).orElse(""));
                if (key == null || !BuiltInRegistries.BLOCK.containsKey(key)) {
                    continue;
                }
                blocks.add(BuiltInRegistries.BLOCK.getValue(key));
            }
            dataManager.set(SPHERICAL_PLAYER_BLOCK_LIST, blocks, true);
            katamariIO$shell = null;
            katamariIO$shellDirty = false;
        }
    }

    @Override
    public void katamariIO$setSize(float size) {
        Player entity = (Player) (Object) this;
        entity.getEntityData().set(sphericalPlayerMod$SIZE, size);
        katamariIO$binRadiiDirty = true;
        entity.refreshDimensions();
    }

    @Override
    public float katamariIO$getSize() {
        return ((Player) (Object) this).getEntityData().get(sphericalPlayerMod$SIZE);
    }

    @Override
    public void katamariIO$setCollisionSize(float size) {
        katamariIO$setSize(size);
    }

    @Override
    public float katamariIO$getCollisionSize() {
        return katamariIO$getSize();
    }

    @Override
    public void katamariIO$setRenderSize(float size) {
        ((Player) (Object) this).getEntityData().set(sphericalPlayerMod$RENDER_SIZE, size);
    }

    @Override
    public float katamariIO$getRenderSize() {
        return ((Player) (Object) this).getEntityData().get(sphericalPlayerMod$RENDER_SIZE);
    }

    @Override
    public void katamariIO$setRestitutionCoefficient(float value) {
        ((Player) (Object) this).getEntityData().set(RESTITUTION_COEFFICIENT, value);
    }

    @Override
    public float katamariIO$getRestitutionCoefficient() {
        return ((Player) (Object) this).getEntityData().get(RESTITUTION_COEFFICIENT);
    }

    @Override
    public void katamariIO$setFlag(boolean flag) {
        Player entity = (Player) (Object) this;
        entity.getEntityData().set(sphericalPlayerMod$FLAG, flag);
        katamariIO$binRadiiDirty = true;
        entity.refreshDimensions();
    }

    @Override
    public void katamariIO$setFlagAndSizeAndRestitution(boolean flag, float collisionSize, float renderSize, float value) {
        Player entity = (Player) (Object) this;
        entity.getEntityData().set(sphericalPlayerMod$FLAG, flag);
        entity.getEntityData().set(sphericalPlayerMod$SIZE, collisionSize);
        entity.getEntityData().set(sphericalPlayerMod$RENDER_SIZE, renderSize);
        entity.getEntityData().set(RESTITUTION_COEFFICIENT, value);
        katamariIO$binRadiiDirty = true;
        entity.refreshDimensions();
    }

    @Override
    public boolean katamariIO$getFlag() {
        return ((Player) (Object) this).getEntityData().get(sphericalPlayerMod$FLAG);
    }

    @Override
    public void katamariIO$setQuaternion(Quaternionf quaternion) {
        Player entity = (Player) (Object) this;
        CompoundTag nbt = new CompoundTag();
        nbt.putFloat("x", quaternion.x());
        nbt.putFloat("y", quaternion.y());
        nbt.putFloat("z", quaternion.z());
        nbt.putFloat("w", quaternion.w());
        entity.getEntityData().set(sphericalPlayerMod$QUATERNION, nbt);
    }

    @Override
    public Quaternionf katamariIO$getQuaternion() {
        CompoundTag quaternionNbt = ((Player) (Object) this).getEntityData().get(sphericalPlayerMod$QUATERNION);
        return katamariIO$getValidQuaternion(new Quaternionf(
                quaternionNbt.getFloatOr("x", 0f),
                quaternionNbt.getFloatOr("y", 0f),
                quaternionNbt.getFloatOr("z", 0f),
                quaternionNbt.getFloatOr("w", 1f)
        ));
    }

    @Unique
    private static Quaternionf katamariIO$getValidQuaternion(Quaternionf quaternion) {
        if (quaternion.x() == 0 && quaternion.y() == 0 && quaternion.z() == 0 && quaternion.w() == 0) {
            return new Quaternionf(0, 0, 0, 1);
        }
        return quaternion;
    }

    @Override
    public void katamariIO$setCurrentPosition(Vec3 pos) {
        Player entity = (Player) (Object) this;
        CompoundTag nbt = new CompoundTag();
        nbt.putFloat("x", (float) pos.x());
        nbt.putFloat("y", (float) pos.y());
        nbt.putFloat("z", (float) pos.z());
        entity.getEntityData().set(CURRENT_POSITION, nbt);
    }

    @Override
    public Vec3 katamariIO$getCurrentPosition() {
        CompoundTag compoundNbt = ((Player) (Object) this).getEntityData().get(CURRENT_POSITION);
        return new Vec3(
                compoundNbt.getFloatOr("x", 0f),
                compoundNbt.getFloatOr("y", 0f),
                compoundNbt.getFloatOr("z", 0f)
        );
    }

    // Dimensions moved to Avatar#getDefaultDimensions in 1.21.9 (see AvatarMixin); fall damage is handled in
    // LivingEntityMixin so it keeps working wherever Player/Avatar declare causeFallDamage.
    @Inject(method = "maybeBackOffFromEdge", at = @At("HEAD"), cancellable = true, require = 0)
    protected void maybeBackOffFromEdge(Vec3 movement, MoverType moverType, CallbackInfoReturnable<Vec3> cir) {
        if (katamariIO$getFlag()) {
            cir.setReturnValue(movement);
        }
    }

    @Override
    public void katamariIO$addBlock(Block block, Quaternionf quaternionf, Vec3 vec3) {
        katamariIO$absorbBlocks(List.of(block), List.of(vec3));
    }

    /**
     * Batch absorb: grows the ball once, then every eaten block replaces the shell slot that lies closest to the
     * direction it was hit from. The shell is always ONE complete layer of blocks on an evenly spaced sphere
     * (see KatamariShell), so the ball is perfectly round and never has holes. The block list is synced to
     * clients in throttled batches (see katamariIO$serverShellTick) instead of on every scan.
     */
    @Override
    public void katamariIO$absorbBlocks(List<Block> absorbed, List<Vec3> localHits) {
        int count = Math.min(absorbed.size(), localHits.size());
        if (count <= 0) {
            return;
        }
        Player entity = (Player) (Object) this;
        SynchedEntityData data = entity.getEntityData();

        katamariIO$growBy(entity, count);
        data.set(SPHERICAL_PLAYER_BLOCK_COUNT, data.get(SPHERICAL_PLAYER_BLOCK_COUNT) + count);

        int slots = KatamariShell.slotCount(data.get(sphericalPlayerMod$SIZE));
        List<Block> shell = katamariIO$getShell(data);
        if (shell.size() != slots) {
            shell = katamariIO$resampleShell(shell, slots, absorbed);
            katamariIO$shell = shell;
        }

        float[] dirs = KatamariShell.directions(slots);
        boolean[] taken = new boolean[slots];
        int takenCount = 0;
        for (int i = 0; i < count; i++) {
            Vec3 hit = localHits.get(i);
            double len = hit.length();
            float hx = 0.0f;
            float hy = 1.0f;
            float hz = 0.0f;
            if (len > 1.0E-4) {
                hx = (float) (hit.x / len);
                hy = (float) (hit.y / len);
                hz = (float) (hit.z / len);
            }
            if (takenCount >= slots) {
                java.util.Arrays.fill(taken, false);
                takenCount = 0;
            }
            int best = 0;
            float bestDot = -2.0f;
            for (int s = 0; s < slots; s++) {
                if (taken[s]) {
                    continue;
                }
                float dot = dirs[3 * s] * hx + dirs[3 * s + 1] * hy + dirs[3 * s + 2] * hz;
                if (dot > bestDot) {
                    bestDot = dot;
                    best = s;
                }
            }
            taken[best] = true;
            takenCount++;
            shell.set(best, absorbed.get(i));
        }
        katamariIO$shellDirty = true;
    }

    @Unique
    private List<Block> katamariIO$getShell(SynchedEntityData data) {
        if (katamariIO$shell == null) {
            katamariIO$shell = new ArrayList<>(data.get(SPHERICAL_PLAYER_BLOCK_LIST));
        }
        return katamariIO$shell;
    }

    /** New shell with {@code slots} blocks; every new slot takes the block of the nearest old slot (colours stay put). */
    @Unique
    private static List<Block> katamariIO$resampleShell(List<Block> old, int slots, List<Block> fallback) {
        List<Block> result = new ArrayList<>(slots);
        int oldCount = old.size();
        if (oldCount > 0) {
            float[] oldDirs = KatamariShell.directions(oldCount);
            float[] newDirs = KatamariShell.directions(slots);
            for (int s = 0; s < slots; s++) {
                float nx = newDirs[3 * s];
                float ny = newDirs[3 * s + 1];
                float nz = newDirs[3 * s + 2];
                int best = 0;
                float bestDot = -2.0f;
                for (int o = 0; o < oldCount; o++) {
                    float dot = oldDirs[3 * o] * nx + oldDirs[3 * o + 1] * ny + oldDirs[3 * o + 2] * nz;
                    if (dot > bestDot) {
                        bestDot = dot;
                        best = o;
                    }
                }
                result.add(old.get(best));
            }
        } else {
            int pool = fallback.size();
            for (int s = 0; s < slots; s++) {
                result.add(fallback.get(Math.floorMod(s * 0x9E3779B1, pool)));
            }
        }
        return result;
    }

    /** Server only: keeps the slot count in line with the size and syncs the shell at most every 4 ticks. */
    @Unique
    private void katamariIO$serverShellTick(Player entity) {
        if (entity.tickCount % 4 != 0) {
            return;
        }
        SynchedEntityData data = entity.getEntityData();
        List<Block> shell = katamariIO$getShell(data);
        if (!shell.isEmpty()) {
            int slots = KatamariShell.slotCount(data.get(sphericalPlayerMod$SIZE));
            if (shell.size() != slots) {
                katamariIO$shell = katamariIO$resampleShell(shell, slots, shell);
                katamariIO$shellDirty = true;
            }
        }
        if (katamariIO$shellDirty) {
            katamariIO$shellDirty = false;
            data.set(SPHERICAL_PLAYER_BLOCK_LIST, new ArrayList<>(katamariIO$shell), true);
        }
    }

    @Override
    public int katamariIO$getAttachedBlockCount() {
        return ((Player) (Object) this).getEntityData().get(SPHERICAL_PLAYER_BLOCK_COUNT);
    }

    @Override
    public void katamariIO$clearAttachedBlocks() {
        Player entity = (Player) (Object) this;
        entity.getEntityData().set(SPHERICAL_PLAYER_BLOCK_LIST, new ArrayList<>(), true);
        entity.getEntityData().set(SPHERICAL_PLAYER_POSITION_LIST, new ArrayList<>(), true);
        entity.getEntityData().set(SPHERICAL_PLAYER_QUATERNION_LIST, new ArrayList<>(), true);
        entity.getEntityData().set(SPHERICAL_PLAYER_BLOCK_COUNT, 0);
        katamariIO$shell = null;
        katamariIO$shellDirty = false;
        katamariIO$binRadiiDirty = true;
    }

    @Override
    public List<Vec3> katamariIO$getSphericalPlayerPositions() {
        return ((Player) (Object) this).getEntityData().get(SPHERICAL_PLAYER_POSITION_LIST);
    }

    @Override
    public List<Quaternionf> katamariIO$getSphericalPlayerQuaternions() {
        return ((Player) (Object) this).getEntityData().get(SPHERICAL_PLAYER_QUATERNION_LIST);
    }

    @Override
    public List<Block> katamariIO$getSphericalPlayerBlocks() {
        return ((Player) (Object) this).getEntityData().get(SPHERICAL_PLAYER_BLOCK_LIST);
    }

    /**
     * Volume based growth: every eaten block adds a little volume to the ball. The exact size is kept in a
     * float so tiny increments are not lost, the synced size moves in 0.1 steps. The visible (render) size is
     * kept equal to the hitbox size so the drawn core never lags behind the block shell.
     *
     * @return true if the synced size changed
     */
    @Unique
    private boolean katamariIO$growBy(Player entity, int blockCount) {
        float current = entity.getEntityData().get(sphericalPlayerMod$SIZE);
        if (katamariIO$exactSize <= 0.0f || Math.abs(Math.round(katamariIO$exactSize * 10.0f) / 10.0f - current) > 0.051f) {
            katamariIO$exactSize = current;
        }
        if (katamariIO$exactSize >= KATAMARI_MAX_SIZE - 1.0E-3f) {
            return false;
        }

        double radius = katamariIO$exactSize / 2.0;
        double addedRadiusCube = KATAMARI_GROWTH_VOLUME_PER_BLOCK * blockCount * 3.0 / (4.0 * Math.PI);
        double newRadius = Math.cbrt(radius * radius * radius + addedRadiusCube);
        katamariIO$exactSize = (float) Math.min(KATAMARI_MAX_SIZE, newRadius * 2.0);

        float newSize = Math.round(katamariIO$exactSize * 10.0f) / 10.0f;
        if (Math.abs(newSize - current) > 1.0E-4f) {
            entity.getEntityData().set(sphericalPlayerMod$SIZE, newSize);
            entity.getEntityData().set(sphericalPlayerMod$RENDER_SIZE, newSize);
            entity.refreshDimensions();
            return true;
        }
        return false;
    }

    @Unique
    private void katamariIO$ensureBinRadiiInitialized(Player entity) {
        if (!katamariIO$binRadiiDirty && katamariIO$binMaxRadii != null) {
            return;
        }

        float baseCollisionRadius = Math.max(0.01f, entity.getEntityData().get(sphericalPlayerMod$SIZE) / 2.0f);
        int binCount = KATAMARI_COLLISION_LAT_BINS * KATAMARI_COLLISION_LON_BINS;
        katamariIO$binMaxRadii = new float[binCount];
        for (int i = 0; i < binCount; i++) {
            katamariIO$binMaxRadii[i] = baseCollisionRadius;
        }

        List<Vec3> vec3s = entity.getEntityData().get(SPHERICAL_PLAYER_POSITION_LIST);
        for (Vec3 offset : vec3s) {
            float distance = (float) offset.length();
            if (distance < 1.0E-6f) {
                continue;
            }

            float blockOuterRadius = distance + KATAMARI_BLOCK_EXTENT;
            float nx = (float) (offset.x / distance);
            float ny = (float) (offset.y / distance);
            float nz = (float) (offset.z / distance);
            int index = katamariIO$getAngularBinIndex(nx, ny, nz);
            if (blockOuterRadius > katamariIO$binMaxRadii[index]) {
                katamariIO$binMaxRadii[index] = blockOuterRadius;
            }
        }

        katamariIO$binRadiiDirty = false;
    }

    @Unique
    private static int katamariIO$getAngularBinIndex(float nx, float ny, float nz) {
        float clampedY = Math.max(-1.0f, Math.min(1.0f, ny));
        double lat = (Math.asin(clampedY) + Math.PI / 2.0) / Math.PI;
        int latIndex = (int) Math.floor(lat * KATAMARI_COLLISION_LAT_BINS);
        if (latIndex < 0) {
            latIndex = 0;
        } else if (latIndex >= KATAMARI_COLLISION_LAT_BINS) {
            latIndex = KATAMARI_COLLISION_LAT_BINS - 1;
        }

        double lon = (Math.atan2(nz, nx) + Math.PI) / (2.0 * Math.PI);
        int lonIndex = (int) Math.floor(lon * KATAMARI_COLLISION_LON_BINS);
        if (lonIndex < 0) {
            lonIndex = 0;
        } else if (lonIndex >= KATAMARI_COLLISION_LON_BINS) {
            lonIndex = KATAMARI_COLLISION_LON_BINS - 1;
        }

        return latIndex * KATAMARI_COLLISION_LON_BINS + lonIndex;
    }
}
