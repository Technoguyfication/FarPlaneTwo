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

package net.daporkchop.fp2.api.storage.internal;

import lombok.NonNull;
import net.daporkchop.fp2.api.storage.FStorageException;

import java.util.List;

/**
 * Interface defining a set of read operations which span an entire {@link FStorageInternal}.
 *
 * @author DaPorkchop_
 */
public interface FStorageReadOperationsInternal {
    /**
     * Gets the value associated with the given key in the given storage column.
     * <p>
     * This is a read operation.
     *
     * @param column the storage column
     * @param key    the key
     * @return the value associated with the given key, or {@code null} if it could not be found
     * @throws FStorageException if the operation fails
     */
    byte[] get(@NonNull FStorageColumnInternal column, @NonNull byte[] key) throws FStorageException;

    /**
     * Gets the values associated with the given keys in the given storage columns.
     * <p>
     * This is a read operation.
     *
     * @param columns the storage columns
     * @param keys    the keys
     * @return an array of results. For each column-entry pair, the corresponding element in the returned array is the value associated with the given key, or {@code null} if it could not be found
     * @throws FStorageException if the operation fails
     */
    byte[][] multiGet(@NonNull List<FStorageColumnInternal> columns, @NonNull List<byte[]> keys) throws FStorageException;
}
