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

package net.daporkchop.fp2.impl.mc.forge1_16.compat.vanilla.exactfblockworld;

import lombok.NonNull;
import net.daporkchop.fp2.api.world.BlockWorldConstants;
import net.daporkchop.fp2.api.world.GenerationNotAllowedException;
import net.daporkchop.fp2.core.minecraft.world.chunks.AbstractChunksExactFBlockWorldHolder;
import net.daporkchop.fp2.core.minecraft.world.chunks.AbstractPrefetchedChunksExactFBlockWorld;
import net.daporkchop.lib.common.math.BinMath;
import net.minecraft.block.BlockState;
import net.minecraft.fluid.FluidState;
import net.minecraft.fluid.Fluids;

import java.util.List;

/**
 * @author DaPorkchop_
 */
public final class PrefetchedChunksFBlockWorld1_16 extends AbstractPrefetchedChunksExactFBlockWorld<OffThreadChunk1_16> {
    public PrefetchedChunksFBlockWorld1_16(@NonNull AbstractChunksExactFBlockWorldHolder<OffThreadChunk1_16> holder, boolean generationAllowed, @NonNull List<OffThreadChunk1_16> chunks) {
        super(holder, generationAllowed, chunks);
    }

    @Override
    protected long packedChunkPosition(@NonNull OffThreadChunk1_16 chunk) {
        return BinMath.packXY(chunk.x(), chunk.z());
    }

    @Override
    protected int getState(int x, int y, int z, OffThreadChunk1_16 chunk) throws GenerationNotAllowedException {
        BlockState blockState = chunk.getBlockState(x, y, z);
        FluidState fluidState = blockState.getFluidState();

        //gross hack for the fact that waterlogging isn't really supported yet
        //TODO: solve this for real
        if (fluidState.getType() != Fluids.EMPTY) {
            blockState = fluidState.createLegacyBlock();
        }

        return this.registry().state2id(blockState);
    }

    @Override
    protected int getBiome(int x, int y, int z, OffThreadChunk1_16 chunk) throws GenerationNotAllowedException {
        return this.registry().biome2id(chunk.biomes().getNoiseBiome(x >> 2, y >> 2, z >> 2)); //TODO: this doesn't do biome zooming (as would normally be done in BiomeManager)
    }

    @Override
    protected byte getLight(int x, int y, int z, OffThreadChunk1_16 chunk) throws GenerationNotAllowedException {
        return BlockWorldConstants.packLight(chunk.getSkyLight(x, y, z), chunk.getBlockLight(x, y, z));
    }
}
