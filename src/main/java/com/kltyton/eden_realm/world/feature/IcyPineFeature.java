package com.kltyton.eden_realm.world.feature;

import com.kltyton.eden_realm.common.block.tree.ERWoodSet;
import com.kltyton.eden_realm.registry.ERBlocks;
import com.kltyton.eden_realm.world.terrain.TerrainWorldProfiles;
import com.kltyton.eden_realm.world.tree.IceCrystalPineShape;
import com.mojang.serialization.Codec;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.TreeFeature;
import net.minecraft.world.level.levelgen.feature.configurations.TreeConfiguration;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

/** Places procedural pine geometry after checking the complete trunk and crown footprint. */
public final class IcyPineFeature extends Feature<TreeConfiguration> {
    private final boolean large;
    private final boolean biomeRestricted;

    public IcyPineFeature(Codec<TreeConfiguration> codec) { this(codec, false, true); }

    public IcyPineFeature(Codec<TreeConfiguration> codec, boolean large, boolean biomeRestricted) {
        super(codec);
        this.large = large;
        this.biomeRestricted = biomeRestricted;
    }

    @Override
    public boolean place(FeaturePlaceContext<TreeConfiguration> context) {
        WorldGenLevel level = context.level();
        BlockPos origin = context.origin();
        if (biomeRestricted) {
            var biome = level.getBiome(origin);
            String id = biome.unwrapKey().orElseThrow().identifier().getPath();
            int elevation = origin.getY() - TerrainWorldProfiles.profile(level, biome).elevationOffset();
            if (id.equals("ice_crystal_basin") && elevation < 82
                    || id.equals("ice_ridge_valley") && elevation > 125) return false;
        }
        var pine = ERBlocks.woodBlocks(ERWoodSet.ICE_CRYSTAL_PINE);
        var sapling = pine.sapling().get();
        if (!sapling.defaultBlockState().canSurvive(level, origin)) return false;
        RandomSource random = context.random();
        TreeConfiguration config = context.config();
        int height = config.trunkPlacer.getTreeHeight(random);
        Set<BlockPos> consumedSaplings = new HashSet<>();
        consumedSaplings.add(origin);
        Set<BlockPos> soil = new HashSet<>();
        soil.add(origin.below());
        Direction pair = Direction.EAST;
        if (large) {
            for (Direction direction : Direction.Plane.HORIZONTAL) {
                if (level.getBlockState(origin.relative(direction)).is(sapling)) {
                    pair = direction;
                    break;
                }
            }
            BlockPos second = origin.relative(pair);
            if (!sapling.defaultBlockState().canSurvive(level, second)) return false;
            consumedSaplings.add(second);
            soil.add(second.below());
        }
        var shape = IceCrystalPineShape.create(height, large, pair.getUnitVec3i().getX(), pair.getUnitVec3i().getZ());
        Map<BlockPos, BlockState> blocks = new LinkedHashMap<>();
        Set<BlockPos> logs = new HashSet<>();
        for (var entry : shape.entrySet()) {
            var point = entry.getKey();
            var part = entry.getValue();
            BlockPos pos = origin.offset(point.x(), point.y(), point.z());
            BlockPos bridge = null;
            boolean buried = part.root && point.y() < 0;
            if (buried) {
                int top = Math.min(origin.getY(), level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                        pos.getX(), pos.getZ()) - 1);
                int bottom = origin.getY() - 2;
                pos = new BlockPos(pos.getX(), top, pos.getZ());
                while (pos.getY() >= bottom && !level.getBlockState(pos).is(BlockTags.DIRT)) {
                    BlockState surface = level.getBlockState(pos);
                    if (!surface.isAir() && !surface.is(Blocks.SNOW)) break;
                    pos = pos.below();
                }
                if (pos.getY() < bottom || !level.getBlockState(pos).is(BlockTags.DIRT)
                        || !level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), Direction.UP)) continue;
                boolean connected = blocks.containsKey(pos.above()) || blocks.containsKey(pos.below());
                for (Direction direction : Direction.Plane.HORIZONTAL)
                    connected |= blocks.containsKey(pos.relative(direction));
                if (!connected && blocks.containsKey(pos.above(2))) bridge = pos.above();
                if (!connected && bridge == null) {
                    for (Direction direction : Direction.Plane.HORIZONTAL) {
                        BlockPos neighbor = pos.relative(direction);
                        if (blocks.containsKey(neighbor.above())) { bridge = pos.above(); break; }
                        if (blocks.containsKey(neighbor.below())) { bridge = neighbor; break; }
                    }
                }
                if (!connected && bridge == null) continue;
                if (bridge != null && (!level.ensureCanWrite(bridge) || !TreeFeature.validTreePos(level, bridge))) continue;
            }
            boolean replaceable = level.getBlockState(pos).is(sapling)
                    ? consumedSaplings.contains(pos) : TreeFeature.validTreePos(level, pos);
            boolean writable = level.isInsideBuildHeight(pos) && level.ensureCanWrite(pos);
            if (buried) {
                if (!writable) continue;
            } else if (part.root) {
                BlockPos base = origin.offset(point.x(), 0, point.z());
                if (!replaceable || !writable || !sapling.defaultBlockState().canSurvive(level, base)) continue;
                BlockPos below = pos.below();
                if (point.y() > 0 && !blocks.containsKey(below)
                        && !level.getBlockState(below).isFaceSturdy(level, below, Direction.UP)) continue;
            } else if (!replaceable || !writable) return false;
            BlockState state;
            if (part == IceCrystalPineShape.Part.LEAF) {
                state = config.foliageProvider.getState(level, random, pos);
            } else {
                logs.add(pos);
                state = part == IceCrystalPineShape.Part.ROOT_BARK ? pine.wood().get().defaultBlockState()
                        : config.trunkProvider.getState(level, random, pos);
                Direction.Axis axis = switch (part) {
                    case LOG_X, ROOT_X -> Direction.Axis.X;
                    case LOG_Z, ROOT_Z -> Direction.Axis.Z;
                    default -> Direction.Axis.Y;
                };
                state = state.setValue(BlockStateProperties.AXIS, axis);
            }
            if (bridge != null) {
                blocks.put(bridge, pine.wood().get().defaultBlockState());
                logs.add(bridge);
            }
            blocks.put(pos, state);
        }
        for (BlockPos pos : soil) if (!level.ensureCanWrite(pos)) return false;
        for (BlockPos pos : soil) {
            if (!level.getBlockState(pos).onTreeGrow(level, (p, state) -> level.setBlock(p, state, 19), random, pos, config)) {
                BlockState below = config.belowTrunkProvider.getOptionalState(level, random, pos);
                if (below != null) level.setBlock(pos, below, 19);
            }
        }
        blocks.forEach((pos, state) -> level.setBlock(pos, state, 19));
        BoundingBox bounds = BoundingBox.encapsulatingPositions(blocks.keySet()).orElseThrow();
        var filled = TreeFeature.updateLeaves(level, bounds, logs, Set.of(), soil);
        StructureTemplate.updateShapeAtEdge(level, 3, filled, bounds.minX(), bounds.minY(), bounds.minZ());
        return true;
    }
}
