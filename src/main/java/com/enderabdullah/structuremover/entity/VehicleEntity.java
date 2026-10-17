package com.enderabdullah.structuremover.entity;

import com.enderabdullah.structuremover.StructureMoverMod;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.entity.IEntityWithComplexSpawnPacket;

import java.util.ArrayList;
import java.util.List;

/**
 * A simple drivable vehicle made of blocks. It is not a rigid-body physics engine:
 * the vehicle stays level, uses gravity, vanilla block collision and a bicycle-style steering model.
 */
public class VehicleEntity extends Entity implements IEntityWithComplexSpawnPacket {

    /** One block of the vehicle. The offset is relative to the vehicle origin (an integer corner). */
    public record Part(BlockPos offset, BlockState state) {
    }

    private final List<Part> parts = new ArrayList<>();
    private float baseYaw;
    private int wheelCount;
    private BlockPos seatOffset;
    private int boxSide = 1;
    private int boxHeight = 1;
    private double speed;

    private int lerpSteps;
    private double lerpX, lerpY, lerpZ;
    private float lerpYaw;

    public VehicleEntity(EntityType<? extends VehicleEntity> type, Level level) {
        super(type, level);
    }

    // ---------- data ----------

    public List<Part> getParts() {
        return parts;
    }

    public float getBaseYaw() {
        return baseYaw;
    }

    public void setParts(List<Part> newParts, float baseYaw) {
        this.parts.clear();
        this.parts.addAll(newParts);
        this.baseYaw = baseYaw;
        recompute();
    }

    private void recompute() {
        wheelCount = 0;
        seatOffset = null;
        int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE, maxZ = Integer.MIN_VALUE;
        for (Part p : parts) {
            BlockPos o = p.offset();
            if (p.state().is(StructureMoverMod.VEHICLE_WHEEL.get())) wheelCount++;
            if (seatOffset == null && p.state().is(StructureMoverMod.STEERING_WHEEL.get())) seatOffset = o;
            minX = Math.min(minX, o.getX());
            minY = Math.min(minY, o.getY());
            minZ = Math.min(minZ, o.getZ());
            maxX = Math.max(maxX, o.getX());
            maxY = Math.max(maxY, o.getY());
            maxZ = Math.max(maxZ, o.getZ());
        }
        if (parts.isEmpty()) {
            boxSide = 1;
            boxHeight = 1;
        } else {
            boxSide = Math.max(maxX - minX + 1, maxZ - minZ + 1);
            boxHeight = maxY - Math.min(minY, 0) + 1;
        }
        refreshDimensions();
    }

    @Override
    public EntityDimensions getDimensions(Pose pose) {
        return EntityDimensions.fixed(Math.max(1, boxSide), Math.max(1, boxHeight));
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
    }

    // ---------- saving ----------

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putFloat("BaseYaw", baseYaw);
        ListTag list = new ListTag();
        for (Part p : parts) {
            CompoundTag t = new CompoundTag();
            t.putInt("x", p.offset().getX());
            t.putInt("y", p.offset().getY());
            t.putInt("z", p.offset().getZ());
            t.put("s", NbtUtils.writeBlockState(p.state()));
            list.add(t);
        }
        tag.put("Parts", list);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        HolderGetter<Block> blocks = level().holderLookup(Registries.BLOCK);
        ListTag list = tag.getList("Parts", Tag.TAG_COMPOUND);
        List<Part> loaded = new ArrayList<>();
        for (int i = 0; i < list.size(); i++) {
            CompoundTag t = list.getCompound(i);
            BlockState state = NbtUtils.readBlockState(blocks, t.getCompound("s"));
            loaded.add(new Part(new BlockPos(t.getInt("x"), t.getInt("y"), t.getInt("z")), state));
        }
        setParts(loaded, tag.getFloat("BaseYaw"));
    }

    @Override
    public void writeSpawnData(RegistryFriendlyByteBuf buf) {
        buf.writeFloat(baseYaw);
        buf.writeVarInt(parts.size());
        for (Part p : parts) {
            buf.writeVarInt(p.offset().getX());
            buf.writeVarInt(p.offset().getY());
            buf.writeVarInt(p.offset().getZ());
            buf.writeVarInt(Block.getId(p.state()));
        }
    }

    @Override
    public void readSpawnData(RegistryFriendlyByteBuf buf) {
        float yaw = buf.readFloat();
        int n = buf.readVarInt();
        List<Part> loaded = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            BlockPos offset = new BlockPos(buf.readVarInt(), buf.readVarInt(), buf.readVarInt());
            loaded.add(new Part(offset, Block.stateById(buf.readVarInt())));
        }
        setParts(loaded, yaw);
    }

    // ---------- behaviour ----------

    @Override
    public boolean isPickable() {
        return !isRemoved();
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        return false;
    }

    @Override
    public float maxUpStep() {
        return 1.0f;
    }

    @Override
    protected boolean canAddPassenger(Entity passenger) {
        return getPassengers().isEmpty();
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {
        if (player.isSecondaryUseActive()) {
            if (!level().isClientSide) {
                disassemble();
            }
            return InteractionResult.sidedSuccess(level().isClientSide);
        }
        if (!level().isClientSide) {
            player.startRiding(this);
        }
        return InteractionResult.sidedSuccess(level().isClientSide);
    }

    private Vec3 seatWorld() {
        double lx = 0, ly = 0, lz = 0;
        if (seatOffset != null) {
            lx = seatOffset.getX() + 0.5;
            ly = seatOffset.getY();
            lz = seatOffset.getZ() + 0.5;
        }
        double th = Math.toRadians(getYRot() - baseYaw);
        double rx = lx * Math.cos(th) - lz * Math.sin(th);
        double rz = lx * Math.sin(th) + lz * Math.cos(th);
        return new Vec3(getX() + rx, getY() + ly, getZ() + rz);
    }

    @Override
    protected void positionRider(Entity passenger, Entity.MoveFunction callback) {
        if (!hasPassenger(passenger)) return;
        Vec3 seat = seatWorld();
        callback.accept(passenger, seat.x, seat.y, seat.z);
    }

    @Override
    public void lerpTo(double x, double y, double z, float yRot, float xRot, int steps) {
        this.lerpX = x;
        this.lerpY = y;
        this.lerpZ = z;
        this.lerpYaw = yRot;
        this.lerpSteps = Math.max(steps, 2);
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            if (lerpSteps > 0) {
                double nx = getX() + (lerpX - getX()) / lerpSteps;
                double ny = getY() + (lerpY - getY()) / lerpSteps;
                double nz = getZ() + (lerpZ - getZ()) / lerpSteps;
                float nyaw = getYRot() + (float) Mth.wrapDegrees(lerpYaw - getYRot()) / lerpSteps;
                lerpSteps--;
                setPos(nx, ny, nz);
                setRot(nyaw, 0.0f);
            }
            return;
        }
        tickServer();
    }

    private void tickServer() {
        float forward = 0, strafe = 0;
        Entity rider = getFirstPassenger();
        if (rider instanceof LivingEntity driver) {
            forward = driver.zza;
            strafe = driver.xxa;
        }

        int blocks = Math.max(1, parts.size());
        double power = wheelCount >= 2 ? Math.min(1.0, wheelCount * 12.0 / blocks) : 0.0;
        double maxSpeed = 0.6 * power;
        double accel = 0.05 * power;

        if (onGround()) {
            if (forward != 0 && power > 0) {
                speed += forward * accel;
            } else {
                speed *= 0.88;
            }
            if (Math.abs(speed) < 0.005) speed = 0;
        } else {
            speed *= 0.99;
        }
        speed = Mth.clamp(speed, -maxSpeed * 0.4, maxSpeed);

        if (strafe != 0 && Math.abs(speed) > 0.02) {
            float turn = (float) Mth.clamp(speed / 0.25, -1.0, 1.0);
            setYRot(getYRot() - strafe * 4.0f * turn);
        }

        double rad = Math.toRadians(getYRot());
        double vx = -Math.sin(rad) * speed;
        double vz = Math.cos(rad) * speed;
        double vy = getDeltaMovement().y;
        if (!isNoGravity()) vy -= 0.08;
        vy *= 0.98;

        move(MoverType.SELF, new Vec3(vx, vy, vz));
        if (horizontalCollision) speed *= 0.5;
        setDeltaMovement(vx, onGround() ? 0.0 : vy, vz);

        if (getY() < level().getMinBuildHeight() - 64) {
            discard();
        }
    }

    // ---------- disassembly ----------

    public void disassemble() {
        if (!(level() instanceof ServerLevel serverLevel)) return;
        ejectPassengers();

        int quarter = Math.floorMod(Math.round((getYRot() - baseYaw) / 90.0f), 4);
        Rotation rot = switch (quarter) {
            case 1 -> Rotation.CLOCKWISE_90;
            case 2 -> Rotation.CLOCKWISE_180;
            case 3 -> Rotation.COUNTERCLOCKWISE_90;
            default -> Rotation.NONE;
        };
        BlockPos origin = new BlockPos((int) Math.round(getX()), (int) Math.round(getY()), (int) Math.round(getZ()));

        for (Part p : parts) {
            BlockPos target = origin.offset(p.offset().rotate(rot));
            BlockState placed = p.state().rotate(rot);
            BlockState existing = serverLevel.getBlockState(target);
            if (existing.isAir() || existing.canBeReplaced()) {
                serverLevel.setBlock(target, placed, Block.UPDATE_ALL);
            } else {
                Block.dropResources(placed, serverLevel, target);
            }
        }
        discard();
    }
}
