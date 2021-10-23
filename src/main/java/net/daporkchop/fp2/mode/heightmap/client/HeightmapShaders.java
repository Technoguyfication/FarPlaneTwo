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

package net.daporkchop.fp2.mode.heightmap.client;

import lombok.experimental.UtilityClass;
import net.daporkchop.fp2.client.gl.shader.ComputeShaderBuilder;
import net.daporkchop.fp2.client.gl.shader.RenderShaderProgram;
import net.daporkchop.fp2.client.gl.shader.ShaderManager;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;

import java.util.EnumSet;

import static net.daporkchop.fp2.FP2.*;

/**
 * @author DaPorkchop_
 */
@UtilityClass
public class HeightmapShaders {
    public static final RenderShaderProgram BLOCK_SHADER = ShaderManager.renderShaderBuilder("heightmap/block")
            .withVertexShader(new ResourceLocation(MODID, "shaders/vert/heightmap/heightmap.vert"))
            .withFragmentShader(new ResourceLocation(MODID, "shaders/frag/block.frag"))
            .link();

    //public final RenderShaderProgram BLOCK_SHADER_TRANSFORM_FEEDBACK = ShaderManager.get("heightmap/xfb/block");

    public final RenderShaderProgram STENCIL_SHADER = ShaderManager.renderShaderBuilder("heightmap/stencil")
            .withVertexShader(new ResourceLocation(MODID, "shaders/vert/heightmap/heightmap.vert"))
            .withFragmentShader(new ResourceLocation(MODID, "shaders/frag/stencil.frag"))
            .link();

    public static final ComputeShaderBuilder CULL_SHADER = ShaderManager.computeShaderBuilder("heightmap/cull")
            .withComputeShader(new ResourceLocation(MODID, "shaders/comp/heightmap/heightmap_frustum_culling.comp"))
            .withGlobalEnableAxes(EnumSet.of(EnumFacing.Axis.X, EnumFacing.Axis.Y));
}
