/*
 * Adapted from The MIT License (MIT)
 *
 * Copyright (c) 2020-2022 DaPorkchop_
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software
 * is furnished to do so, subject to the following conditions:
 *
 * Any persons and/or organizations using this software must include the above copyright notice and this permission notice,
 * provide sufficient credit to the original authors of the project (IE: DaPorkchop_), as well as provide a link to the original project.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS
 * BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 */

package net.daporkchop.fp2.impl.mc.forge1_12_2.compat.cwg.generator.voxel;

import io.github.opencubicchunks.cubicchunks.cubicgen.common.biome.IBiomeBlockReplacer;
import lombok.NonNull;
import net.daporkchop.fp2.api.world.BlockWorldConstants;
import net.daporkchop.fp2.core.mode.api.server.IFarTileProvider;
import net.daporkchop.fp2.core.mode.voxel.VoxelData;
import net.daporkchop.fp2.core.mode.voxel.VoxelPos;
import net.daporkchop.fp2.core.mode.voxel.VoxelTile;
import net.daporkchop.fp2.core.mode.voxel.server.gen.rough.AbstractRoughVoxelGenerator;
import net.daporkchop.fp2.core.server.world.IFarWorldServer;
import net.daporkchop.fp2.impl.mc.forge1_12_2.compat.cwg.CWGContext;
import net.daporkchop.lib.common.reference.ReferenceStrength;
import net.daporkchop.lib.common.reference.cache.Cached;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.world.WorldServer;

import java.util.Arrays;

import static java.lang.Math.*;
import static net.daporkchop.fp2.core.mode.voxel.VoxelConstants.*;

/**
 * @author DaPorkchop_
 */
public class CWGVoxelGenerator extends AbstractRoughVoxelGenerator<CWGContext> {
    protected final Cached<CWGContext> ctx;

    public CWGVoxelGenerator(@NonNull IFarWorldServer world, @NonNull IFarTileProvider<VoxelPos, VoxelTile> provider) {
        super(world, provider);

        this.ctx = Cached.threadLocal(() -> new CWGContext(this.registry, (WorldServer) world.fp2_IFarWorld_implWorld(), CACHE_SIZE, 2, VT_SHIFT), ReferenceStrength.WEAK);
    }

    @Override
    public void generate(@NonNull VoxelPos pos, @NonNull VoxelTile tile) {
        int level = pos.level();
        int baseX = pos.blockX();
        int baseY = pos.blockY();
        int baseZ = pos.blockZ();

        CWGContext ctx = this.ctx.get();
        ctx.init(baseX + (CACHE_MIN << level), baseZ + (CACHE_MIN << level), level);
        double[][] densityMap = this.densityMapCache.get();

        //water
        assert cacheIndex(0, 0, 1) - cacheIndex(0, 0, 0) == 1 : "cache coordinate order must be z-minor";

        double scaleFactor = 1.0d / (1 << level);
        for (int x = CACHE_MIN; x < CACHE_MAX; x++) {
            for (int y = CACHE_MIN; y < CACHE_MAX; y++) {
                final int idx = cacheIndex(x, y, CACHE_MIN);
                Arrays.fill(densityMap[0], idx, idx + CACHE_MAX, ((this.seaLevel - 0.125d) - (baseY + (y << level))) * scaleFactor);
            }
        }

        //blocks
        ctx.get3d(densityMap[1], baseY + (CACHE_MIN << level));

        //actually create the mesh (using dual contouring)
        this.dualContour(baseX, baseY, baseZ, level, tile, densityMap, ctx);
    }

    @Override
    protected int getFaceState(int blockX, int blockY, int blockZ, int level, double nx, double ny, double nz, double density0, double density1, int edge, int layer, CWGContext ctx) {
        if (layer == 0) { //layer 0 is always water lol
            return this.registry.state2id(Blocks.WATER.getDefaultState());
        }

        double density = max(density0, density1);

        IBlockState state = Blocks.AIR.getDefaultState();
        for (IBiomeBlockReplacer replacer : ctx.replacersForBiome(ctx.getBiome(blockX, blockZ))) {
            state = replacer.getReplacedBlock(state, blockX, blockY, blockZ, nx, ny, nz, density);
        }

        return this.registry.state2id(state);
    }

    @Override
    protected void populateVoxelBlockData(int blockX, int blockY, int blockZ, int level, double nx, double ny, double nz, VoxelData data, CWGContext ctx) {
        blockY++;

        int seaLevel = this.seaLevel >> level << level; //truncate lower bits in order to scale the sea level to the current zoom level
        data.light = BlockWorldConstants.packLight(blockY < seaLevel ? max(15 - (seaLevel - blockY) * 3, 0) : 15, 0);
        data.biome = ctx.getBiome(blockX, blockZ);
    }

    @Override
    public boolean supportsLowResolution() {
        return true;
    }
}
