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

package net.daporkchop.fp2.core.mode.api.server.gen;

import lombok.NonNull;
import net.daporkchop.fp2.api.world.FBlockWorld;
import net.daporkchop.fp2.api.world.GenerationNotAllowedException;
import net.daporkchop.fp2.core.mode.api.IFarPos;
import net.daporkchop.fp2.core.mode.api.IFarTile;
import net.daporkchop.fp2.core.mode.api.server.IFarServerResourceCreationEvent;
import net.daporkchop.fp2.core.mode.api.server.IFarTileProvider;

import java.util.List;
import java.util.Optional;

import static net.daporkchop.lib.common.util.PValidation.*;

/**
 * Type of {@link IFarGenerator} which generates tile data based on block data in the world.
 * <p>
 * Exact generators only operate on tiles at level 0.
 *
 * @author DaPorkchop_
 */
public interface IFarGeneratorExact<POS extends IFarPos, T extends IFarTile> extends IFarGenerator {
    /**
     * Gets an {@link Optional} {@link List} of all the tile positions which may be generated at the same time as the tile at the given position to potentially achieve better performance.
     * <p>
     * If present, the input position must be included in the resulting {@link List}.
     *
     * @param world the {@link FBlockWorld} instance providing access to block data in the world
     * @param pos   the position of the tile to generate
     * @return an {@link Optional} {@link List} of all the tile positions which may be generated at the same time
     */
    default Optional<List<POS>> batchGenerationGroup(@NonNull FBlockWorld world, @NonNull POS pos) {
        return Optional.empty(); //don't do batch generation by default
    }

    /**
     * Generates the terrain in the given tile.
     *
     * @param world the {@link FBlockWorld} instance providing access to block data in the world
     * @param pos   the position of the tile to generate
     * @param tile  the tile to generate
     * @throws GenerationNotAllowedException if the generator attempts to access terrain which is not generated, and the given {@link FBlockWorld} does not allow generation
     */
    void generate(@NonNull FBlockWorld world, @NonNull POS pos, @NonNull T tile) throws GenerationNotAllowedException;

    /**
     * Generates the terrain in the given tiles.
     *
     * @param world     the {@link FBlockWorld} instance providing access to block data in the world
     * @param positions the positions of the tiles to generate
     * @param tiles     the tiles to generate
     * @throws GenerationNotAllowedException if the generator attempts to access terrain which is not generated, and the given {@link FBlockWorld} does not allow generation
     */
    default void generate(@NonNull FBlockWorld world, @NonNull POS[] positions, @NonNull T[] tiles) throws GenerationNotAllowedException {
        checkArg(positions.length == tiles.length, "positions (%d) and tiles (%d) must have same number of elements!", positions.length, tiles.length);

        for (int i = 0; i < positions.length; i++) {
            this.generate(world, positions[i], tiles[i]);
        }
    }

    /**
     * Fired to create a new {@link IFarGeneratorExact}.
     *
     * @author DaPorkchop_
     */
    interface CreationEvent<POS extends IFarPos, T extends IFarTile> extends IFarServerResourceCreationEvent<POS, T, IFarGeneratorExact<POS, T>> {
        /**
         * @return the {@link IFarTileProvider} which the {@link IFarGeneratorExact} will be created for
         */
        IFarTileProvider<POS, T> provider();
    }
}
