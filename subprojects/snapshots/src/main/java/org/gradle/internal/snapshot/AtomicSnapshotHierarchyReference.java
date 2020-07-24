/*
 * Copyright 2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gradle.internal.snapshot;

import org.gradle.internal.snapshot.impl.DefaultVfsRoot;

import java.util.concurrent.locks.ReentrantLock;

public class AtomicSnapshotHierarchyReference implements VfsRootReference {
    private volatile SnapshotHierarchy root;
    private final SnapshotHierarchy.UpdateFunctionRunner updateFunctionRunner;
    private final ReentrantLock updateLock = new ReentrantLock();

    public AtomicSnapshotHierarchyReference(SnapshotHierarchy root, SnapshotHierarchy.UpdateFunctionRunner updateFunctionRunner) {
        this.root = root;
        this.updateFunctionRunner = updateFunctionRunner;
    }

    @Override
    public ReadOnlyVfsRoot get() {
        return root;
    }

    @Override
    public void update(VfsUpdateFunction updateFunction) {
        updateLock.lock();
        try {
            DefaultVfsRoot vfsRoot = new DefaultVfsRoot(root, updateFunctionRunner);
            updateFunction.update(vfsRoot);
            root = vfsRoot.getDelegate();
        } finally {
            updateLock.unlock();
        }
    }
}
