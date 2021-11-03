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

package net.daporkchop.fp2.gl.opengl.attribute.uniform;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NonNull;
import net.daporkchop.fp2.common.util.alloc.Allocator;
import net.daporkchop.fp2.common.util.alloc.DirectMemoryAllocator;
import net.daporkchop.fp2.gl.attribute.Attribute;
import net.daporkchop.fp2.gl.attribute.instanced.InstancedAttributeWriter;
import net.daporkchop.fp2.gl.attribute.uniform.UniformAttributeBuffer;
import net.daporkchop.fp2.gl.buffer.BufferUsage;
import net.daporkchop.fp2.gl.opengl.attribute.AttributeFormatImpl;
import net.daporkchop.fp2.gl.opengl.attribute.AttributeImpl;
import net.daporkchop.fp2.gl.opengl.buffer.GLBufferImpl;

/**
 * @author DaPorkchop_
 */
//TODO: this currently does nothing
@Getter
public class UniformAttributeBufferImpl implements UniformAttributeBuffer {
    protected final AttributeFormatImpl format;
    protected final GLBufferImpl buffer;

    @Getter(AccessLevel.NONE)
    protected final Allocator alloc = new DirectMemoryAllocator();

    protected final int[] offsets;
    protected final int totalSize;
    protected long addr;

    public UniformAttributeBufferImpl(@NonNull AttributeFormatImpl format, @NonNull BufferUsage usage) {
        this.format = format;
        this.buffer = format.gl().createBuffer(usage);

        this.offsets = format.offsetsUniform();
        this.totalSize = format.totalSizeUniform();
        this.addr = this.alloc.alloc(format.totalSizeUniform());
    }

    @Override
    public void close() {
        this.alloc.free(this.addr);
    }

    @Override
    public UniformAttributeBuffer set(@NonNull Attribute.Int1 attribIn, int v0) {
        AttributeImpl.Int1 attrib = (AttributeImpl.Int1) attribIn;
        attrib.set(null, this.addr + this.offsets[attrib.index()], v0);
        return this;
    }

    @Override
    public UniformAttributeBuffer set(@NonNull Attribute.Int2 attribIn, int v0, int v1) {
        AttributeImpl.Int2 attrib = (AttributeImpl.Int2) attribIn;
        attrib.set(null, this.addr + this.offsets[attrib.index()], v0, v1);
        return this;
    }

    @Override
    public UniformAttributeBuffer set(@NonNull Attribute.Int3 attribIn, int v0, int v1, int v2) {
        AttributeImpl.Int3 attrib = (AttributeImpl.Int3) attribIn;
        attrib.set(null, this.addr + this.offsets[attrib.index()], v0, v1, v2);
        return this;
    }

    @Override
    public UniformAttributeBuffer setARGB(@NonNull Attribute.Int3 attribIn, int argb) {
        AttributeImpl.Int3 attrib = (AttributeImpl.Int3) attribIn;
        attrib.setARGB(null, this.addr + this.offsets[attrib.index()], argb);
        return this;
    }

    @Override
    public UniformAttributeBuffer set(@NonNull Attribute.Int4 attribIn, int v0, int v1, int v2, int v3) {
        AttributeImpl.Int4 attrib = (AttributeImpl.Int4) attribIn;
        attrib.set(null, this.addr + this.offsets[attrib.index()], v0, v1, v2, v3);
        return this;
    }

    @Override
    public UniformAttributeBuffer setARGB(@NonNull Attribute.Int4 attribIn, int argb) {
        AttributeImpl.Int4 attrib = (AttributeImpl.Int4) attribIn;
        attrib.setARGB(null, this.addr + this.offsets[attrib.index()], argb);
        return this;
    }
}
