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

package net.daporkchop.fp2.core.mode.voxel.server.gen.exact;

import lombok.NonNull;
import net.daporkchop.fp2.api.world.FBlockWorld;
import net.daporkchop.fp2.core.server.world.IFarWorldServer;
import net.daporkchop.fp2.core.mode.voxel.VoxelPos;
import net.daporkchop.lib.math.vector.Vec2i;
import net.daporkchop.lib.math.vector.Vec3i;

import java.util.stream.Stream;

/**
 * @author DaPorkchop_
 */
public class CCVoxelGenerator extends AbstractExactVoxelGenerator {
    public CCVoxelGenerator(@NonNull IFarWorldServer world) {
        super(world);
    }

    @Override
    public Stream<Vec2i> neededColumns(@NonNull VoxelPos pos) {
        return Stream.empty();
    }

    @Override
    public Stream<Vec3i> neededCubes(@NonNull FBlockWorld world, @NonNull VoxelPos pos) {
        Vec3i[] arr = new Vec3i[8];
        for (int i = 0, dx = 0; dx < 2; dx++) {
            for (int dy = 0; dy < 2; dy++) {
                for (int dz = 0; dz < 2; dz++) {
                    arr[i++] = Vec3i.of(pos.x() + dx, pos.y() + dy, pos.z() + dz);
                }
            }
        }
        return Stream.of(arr);
    }
}
