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

package net.daporkchop.fp2.gl.opengl.binding;

import com.google.common.collect.ImmutableList;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import net.daporkchop.fp2.gl.binding.DrawBinding;
import net.daporkchop.fp2.gl.opengl.GLAPI;
import net.daporkchop.fp2.gl.opengl.OpenGL;
import net.daporkchop.fp2.gl.opengl.attribute.AttributeFormatImpl;
import net.daporkchop.fp2.gl.opengl.attribute.BaseAttributeBufferImpl;
import net.daporkchop.fp2.gl.opengl.attribute.common.VertexAttributeBuffer;
import net.daporkchop.fp2.gl.opengl.attribute.global.GlobalAttributeBufferTexture;
import net.daporkchop.fp2.gl.opengl.attribute.local.LocalAttributeBufferImpl;
import net.daporkchop.fp2.gl.opengl.attribute.local.LocalAttributeFormatImpl;
import net.daporkchop.fp2.gl.opengl.attribute.uniform.UniformAttributeBufferImpl;
import net.daporkchop.fp2.gl.opengl.layout.DrawLayoutImpl;
import net.daporkchop.fp2.gl.opengl.texture.TextureImpl;
import net.daporkchop.fp2.gl.opengl.texture.TextureTarget;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static net.daporkchop.fp2.gl.opengl.OpenGLConstants.*;
import static net.daporkchop.lib.common.util.PValidation.*;

/**
 * @author DaPorkchop_
 */
public class DrawBindingImpl implements DrawBinding {
    protected final OpenGL gl;
    protected final GLAPI api;

    protected final DrawLayoutImpl layout;

    protected final int vao;
    protected final List<UniformBufferBinding> uniformBuffers;
    protected final List<TextureBinding> textures;

    public DrawBindingImpl(@NonNull DrawBindingBuilderImpl builder) {
        this.layout = builder.layout;
        this.gl = this.layout.gl();
        this.api = this.gl.api();

        //create a VAO
        this.vao = this.api.glGenVertexArray();
        this.gl.resourceArena().register(this, this.vao, this.api::glDeleteVertexArray);

        //group attribute buffers by attribute format
        Map<AttributeFormatImpl, BaseAttributeBufferImpl> buffersByFormatOld = Stream.of(builder.uniforms, builder.globals)
                .flatMap(List::stream)
                .collect(Collectors.toMap(BaseAttributeBufferImpl::format, Function.identity()));

        Map<LocalAttributeFormatImpl<?>, LocalAttributeBufferImpl<?>> buffersByFormat = Stream.of(builder.locals)
                .flatMap(List::stream)
                .collect(Collectors.toMap(LocalAttributeBufferImpl::format, Function.identity()));

        //configure all vertex attributes in the VAO
        int oldVao = this.api.glGetInteger(GL_VERTEX_ARRAY_BINDING);
        try {
            this.api.glBindVertexArray(this.vao);

            this.layout.vertexBindingsByFormat().forEach((format, bindings) -> {
                BaseAttributeBufferImpl buffer = buffersByFormatOld.remove(format);
                checkArg(buffer != null, format);

                bindings.forEach(binding -> binding.enableAndBind(this.api, buffer));
            });

            this.layout.vertexAttributeBindingsByFormat().forEach((format, binding) -> {
                VertexAttributeBuffer buffer = buffersByFormat.remove(format);
                checkArg(buffer != null, format);

                binding.enableAndBind(this.api, buffer);
            });
        } finally {
            this.api.glBindVertexArray(oldVao);
        }

        //configure uniform buffers
        this.uniformBuffers = this.layout.uniformBlockBindings().stream()
                .map(blockBinding -> {
                    UniformAttributeBufferImpl buffer = (UniformAttributeBufferImpl) buffersByFormatOld.remove(blockBinding.format());
                    checkArg(buffer != null, blockBinding.format());

                    return new UniformBufferBinding(buffer, blockBinding.bindingIndex());
                })
                .collect(Collectors.collectingAndThen(Collectors.toList(), ImmutableList::copyOf));

        //configure textures
        this.textures = this.layout.textureBindings().stream()
                .flatMap(textureBinding -> {
                    GlobalAttributeBufferTexture buffer = (GlobalAttributeBufferTexture) buffersByFormatOld.remove(textureBinding.format());
                    checkArg(buffer != null, textureBinding.format());

                    return Stream.of(textureBinding.format().attribsArray())
                            .map(attrib -> new TextureBinding(buffer.textures()[attrib.index()], textureBinding.unit() + attrib.index(), textureBinding.target()));
                })
                .collect(Collectors.collectingAndThen(Collectors.toList(), ImmutableList::copyOf));

        //ensure every attribute has been used
        checkArg(buffersByFormatOld.isEmpty(), "some buffers have not been bound to anything!", buffersByFormatOld.keySet());
        checkArg(buffersByFormat.isEmpty(), "some buffers have not been bound to anything!", buffersByFormat.keySet());
    }

    @Override
    public void close() {
        this.gl.resourceArena().delete(this);
    }

    public void bind(@NonNull Runnable callback) {
        int oldVao = this.api.glGetInteger(GL_VERTEX_ARRAY_BINDING);

        try {
            this.api.glBindVertexArray(this.vao);
            this.uniformBuffers.forEach(binding -> this.api.glBindBufferBase(GL_UNIFORM_BUFFER, binding.bindingIndex, binding.buffer.buffer().id()));
            this.textures.forEach(binding -> {
                this.api.glActiveTexture(GL_TEXTURE0 + binding.unit);
                this.api.glBindTexture(binding.target.target(), binding.texture.id());
            });

            callback.run();
        } finally {
            this.textures.forEach(binding -> { //this doesn't actually restore the old binding ID...
                this.api.glActiveTexture(GL_TEXTURE0 + binding.unit);
                this.api.glBindTexture(binding.target.target(), 0);
            });
            this.uniformBuffers.forEach(binding -> this.api.glBindBufferBase(GL_UNIFORM_BUFFER, binding.bindingIndex, 0)); //this doesn't actually restore the old binding ID...
            this.api.glBindVertexArray(oldVao);
        }
    }

    /**
     * @author DaPorkchop_
     */
    @RequiredArgsConstructor
    protected static class UniformBufferBinding {
        @NonNull
        protected final UniformAttributeBufferImpl buffer;
        protected final int bindingIndex;
    }

    /**
     * @author DaPorkchop_
     */
    @RequiredArgsConstructor
    protected static class TextureBinding {
        @NonNull
        protected final TextureImpl texture;
        protected final int unit;
        @NonNull
        protected final TextureTarget target;
    }
}
