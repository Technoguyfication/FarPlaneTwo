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

package net.daporkchop.fp2.core.mode.common.server;

import com.google.common.collect.ImmutableList;
import it.unimi.dsi.fastutil.objects.ObjectRBTreeSet;
import lombok.Getter;
import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.Synchronized;
import net.daporkchop.fp2.api.event.FEventHandler;
import net.daporkchop.fp2.core.mode.api.IFarCoordLimits;
import net.daporkchop.fp2.core.mode.api.IFarPos;
import net.daporkchop.fp2.core.mode.api.IFarRenderMode;
import net.daporkchop.fp2.core.mode.api.IFarTile;
import net.daporkchop.fp2.core.mode.api.server.IFarTileProvider;
import net.daporkchop.fp2.core.mode.api.server.gen.IFarGeneratorExact;
import net.daporkchop.fp2.core.mode.api.server.gen.IFarGeneratorRough;
import net.daporkchop.fp2.core.mode.api.server.gen.IFarScaler;
import net.daporkchop.fp2.core.mode.api.server.storage.IFarTileStorage;
import net.daporkchop.fp2.core.mode.api.server.tracking.IFarTrackerManager;
import net.daporkchop.fp2.core.mode.api.tile.ITileHandle;
import net.daporkchop.fp2.core.mode.common.server.storage.rocksdb.RocksTileStorage;
import net.daporkchop.fp2.core.server.event.ColumnSavedEvent;
import net.daporkchop.fp2.core.server.event.CubeSavedEvent;
import net.daporkchop.fp2.core.server.event.TickEndEvent;
import net.daporkchop.fp2.core.server.world.IFarWorldServer;
import net.daporkchop.fp2.core.util.threading.scheduler.ApproximatelyPrioritizedSharedFutureScheduler;
import net.daporkchop.fp2.core.util.threading.scheduler.Scheduler;
import net.daporkchop.lib.common.misc.string.PStrings;
import net.daporkchop.lib.common.misc.threadfactory.PThreadFactories;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import static net.daporkchop.fp2.core.FP2Core.*;
import static net.daporkchop.lib.common.util.PValidation.*;

/**
 * @author DaPorkchop_
 */
@Getter
public abstract class AbstractFarTileProvider<POS extends IFarPos, T extends IFarTile> implements IFarTileProvider<POS, T> {
    protected final IFarWorldServer world;
    protected final IFarRenderMode<POS, T> mode;
    protected final Path root;

    protected final IFarGeneratorRough<POS, T> generatorRough;
    protected final IFarGeneratorExact<POS, T> generatorExact;
    protected final IFarScaler<POS, T> scaler;

    protected final IFarTileStorage<POS, T> storage;

    protected final IFarCoordLimits<POS> coordLimits;

    protected final IFarTrackerManager<POS, T> trackerManager;

    protected final Scheduler<PriorityTask<POS>, ITileHandle<POS, T>> scheduler; //TODO: make this global rather than per-mode and per-dimension

    protected Set<POS> updatesPending = new ObjectRBTreeSet<>();
    protected long lastCompletedTick = -1L;

    public AbstractFarTileProvider(@NonNull IFarWorldServer world, @NonNull IFarRenderMode<POS, T> mode) {
        this.world = world;
        this.mode = mode;

        this.coordLimits = mode.tileCoordLimits(world.fp2_IFarWorld_coordLimits());

        this.generatorRough = this.mode().roughGenerator(world, this);
        this.generatorExact = this.mode().exactGenerator(world, this);

        if (this.generatorRough == null) {
            fp2().log().warn("no rough %s generator exists for world '%s' (generator=%s)! Falling back to exact generator, this will have serious performance implications.", mode.name(), world.fp2_IFarWorld_dimensionId(), world.fp2_IFarWorldServer_terrainGeneratorInfo().implGenerator());
            //TODO: make the fallback generator smart! rather than simply getting the chunks from the world, do generation and population in
            // a volatile, in-memory world clone to prevent huge numbers of chunks/cubes from potentially being generated (and therefore saved)
        }

        this.scaler = mode.scaler(world, this);

        this.root = world.fp2_IFarWorldServer_worldDirectory().resolve(MODID).resolve(this.mode().name().toLowerCase());
        this.storage = new RocksTileStorage<>(this, this.root);

        this.scheduler = new ApproximatelyPrioritizedSharedFutureScheduler<>(
                scheduler -> new TileWorker<>(this, scheduler),
                this.world.fp2_IFarWorld_workerManager().createChildWorkerGroup()
                        .threads(fp2().globalConfig().performance().terrainThreads())
                        .threadFactory(PThreadFactories.builder().daemon().minPriority().collapsingId()
                                .name(PStrings.fastFormat("FP2 %s %s Worker #%%d", mode.name(), world.fp2_IFarWorld_dimensionId())).build()),
                PriorityTask.approxComparator());

        this.trackerManager = this.createTracker();

        //TODO: figure out why i was registering this here?
        // fp2().eventBus().registerWeak(this);

        world.fp2_IFarWorldServer_eventBus().registerWeak(this);
    }

    protected abstract IFarTrackerManager<POS, T> createTracker();

    protected abstract boolean anyVanillaTerrainExistsAt(@NonNull POS pos);

    protected boolean anyVanillaTerrainExistsAt(@NonNull List<POS> positions) {
        return positions.stream().anyMatch(this::anyVanillaTerrainExistsAt);
    }

    @Override
    public CompletableFuture<ITileHandle<POS, T>> requestLoad(@NonNull POS pos) {
        return this.scheduler.schedule(TaskStage.LOAD.taskForPosition(pos));
    }

    @Override
    public CompletableFuture<ITileHandle<POS, T>> requestUpdate(@NonNull POS pos) {
        return this.scheduler.schedule(TaskStage.UPDATE.taskForPosition(pos));
    }

    @Override
    public long currentTimestamp() {
        checkState(this.lastCompletedTick >= 0L, "no game ticks have been completed?!?");
        return this.lastCompletedTick;
    }

    public boolean canGenerateRough(@NonNull POS pos) {
        return this.generatorRough != null && this.generatorRough.canGenerate(pos);
    }

    protected void scheduleForUpdate(@NonNull POS pos) {
        this.scheduleForUpdate(ImmutableList.of(pos));
    }

    protected void scheduleForUpdate(@NonNull POS... positions) {
        this.scheduleForUpdate(Arrays.asList(positions));
    }

    protected void scheduleForUpdate(@NonNull Collection<POS> positions) {
        positions.forEach(pos -> checkArg(pos.level() == 0, "position must be at level 0! %s", pos));

        //noinspection SynchronizeOnNonFinalField
        synchronized (this.updatesPending) {
            this.updatesPending.addAll(positions);
        }
    }

    @FEventHandler
    protected abstract void onColumnSaved(ColumnSavedEvent event);

    @FEventHandler
    protected abstract void onCubeSaved(CubeSavedEvent event);

    @FEventHandler
    private void onTickEnd(TickEndEvent event) {
        this.lastCompletedTick = this.world.fp2_IFarWorld_timestamp();
        checkState(this.lastCompletedTick >= 0L, "lastCompletedTick (%d) < 0?!?", this.lastCompletedTick);

        this.flushUpdateQueue();
    }

    @Synchronized("updatesPending")
    protected void flushUpdateQueue() {
        checkState(this.lastCompletedTick >= 0L, "flushed update queue before any game ticks were completed?!?");

        if (!this.updatesPending.isEmpty()) {
            //iterate up through all of the scaler outputs and enqueue them all for marking as dirty
            Collection<POS> last = this.updatesPending;
            for (int level = 0; level + 1 < this.mode.maxLevels(); level++) {
                Collection<POS> next = this.scaler.uniqueOutputs(last);
                this.updatesPending.addAll(next);
                last = next;
            }

            //actually mark all of the queued tiles as dirty
            this.storage.multiMarkDirty(new ArrayList<>(this.updatesPending), this.lastCompletedTick);

            //clear the pending updates queue
            this.updatesPending.clear();
        }
    }

    @Synchronized("updatesPending")
    protected void shutdownUpdateQueue() {
        this.flushUpdateQueue();
        this.updatesPending = null;
    }

    @Override
    @SneakyThrows(IOException.class)
    public void close() {
        this.trackerManager.close();

        fp2().eventBus().unregister(this);

        this.scheduler.close();

        this.onTickEnd(null);
        this.shutdownUpdateQueue();

        fp2().log().trace("Shutting down storage in world '%s'", this.world.fp2_IFarWorld_dimensionId());
        this.storage.close();
    }
}
