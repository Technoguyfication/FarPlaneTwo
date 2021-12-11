/*
 * Adapted from The MIT License (MIT)
 *
 * Copyright (c) 2020-2021 DaPorkchop_
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

package net.daporkchop.fp2.impl.mc.forge1_12_2.client.render;

import lombok.Getter;
import lombok.NonNull;
import net.daporkchop.fp2.core.client.render.WorldRenderer;
import net.daporkchop.fp2.gl.GL;
import net.daporkchop.fp2.impl.mc.forge1_12_2.FakeFarWorldClient;
import net.daporkchop.fp2.impl.mc.forge1_12_2.GameRegistry1_12_2;
import net.daporkchop.fp2.impl.mc.forge1_12_2.ResourceProvider1_12_2;
import net.daporkchop.fp2.util.SingleBiomeBlockAccess;
import net.minecraft.client.Minecraft;
import net.minecraft.util.math.BlockPos;

import static net.daporkchop.fp2.util.BlockType.*;

/**
 * @author DaPorkchop_
 */
@Getter
public class WorldRenderer1_12_2 implements WorldRenderer, AutoCloseable {
    protected final Minecraft mc;
    protected final GL gl;

    protected final FakeFarWorldClient world;

    protected final GameRegistry1_12_2 registry;
    protected final byte[] renderTypeLookup;

    public WorldRenderer1_12_2(@NonNull Minecraft mc, @NonNull FakeFarWorldClient world) {
        this.mc = mc;
        this.world = world;

        this.registry = world.fp2_IFarWorld_registry();

        //look up and cache the render type for each block state
        this.renderTypeLookup = new byte[this.registry.states().max().getAsInt() + 1];
        this.registry.states().forEach(state -> {
            int typeIndex;
            switch (this.registry.id2state(state).getBlock().getRenderLayer()) {
                default:
                    typeIndex = RENDER_TYPE_OPAQUE;
                    break;
                case CUTOUT:
                case CUTOUT_MIPPED:
                    typeIndex = RENDER_TYPE_CUTOUT;
                    break;
                case TRANSLUCENT:
                    typeIndex = RENDER_TYPE_TRANSLUCENT;
                    break;
            }
            this.renderTypeLookup[state] = (byte) typeIndex;
        });

        this.gl = GL.builder()
                .withResourceProvider(new ResourceProvider1_12_2(this.mc))
                .wrapCurrent();
    }

    @Override
    public int renderTypeForState(int state) {
        return this.renderTypeLookup[state];
    }

    @Override
    public int tintFactorForStateInBiomeAtPos(int state, int biome, int x, int y, int z) {
        return this.mc.getBlockColors().colorMultiplier(this.registry.id2state(state), new SingleBiomeBlockAccess().biome(this.registry.id2biome(biome)), new BlockPos(x, y, z), 0);
    }

    @Override
    public Object terrainTextureId() {
        return this.mc.getTextureMapBlocks().getGlTextureId();
    }

    @Override
    public Object lightmapTextureId() {
        return this.mc.entityRenderer.lightmapTexture.getGlTextureId();
    }

    @Override
    public void close() {
        this.gl.close();
    }
}
