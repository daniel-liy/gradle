/*
 * Copyright 2015 the original author or authors.
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
package org.gradle.api.internal.project.antbuilder;

import com.google.common.collect.Maps;
import org.gradle.api.logging.Logger;
import org.gradle.api.logging.Logging;
import org.gradle.internal.classpath.ClassPath;

import java.lang.ref.ReferenceQueue;
import java.util.Map;
import java.util.concurrent.locks.ReentrantReadWriteLock;

class FinalizerThread extends Thread {
    private final static Logger LOG = Logging.getLogger(MemoryLeakPrevention.class);

    private final ReentrantReadWriteLock lock;
    private final Map<ClassPath, Cleanup> cleanups;
    private final ReferenceQueue<CachedClassLoader> referenceQueue;
    private final Map<ClassPath, CacheEntry> cacheEntries;

    private boolean stopped;

    public FinalizerThread(Map<ClassPath, CacheEntry> cacheEntries, ReentrantReadWriteLock lock) {
        this.setName("Classloader cache reference queue poller");
        this.setDaemon(true);
        this.referenceQueue = new ReferenceQueue<CachedClassLoader>();
        this.cacheEntries = cacheEntries;
        this.cleanups = Maps.newConcurrentMap();
        this.lock = lock;
    }

    public void run() {

        try {
            while (!stopped) {
                Cleanup entry = (Cleanup) referenceQueue.remove();
                ClassPath key = entry.getKey();
                removeCacheEntry(key, entry);
            }
        } catch (InterruptedException ex) {
            LOG.debug("Shutdown of classloader cache in progress");
        }
    }

    private void removeCacheEntry(ClassPath key, Cleanup entry) {
        if (LOG.isDebugEnabled()) {
            LOG.debug(String.format("Removing classloader from cache, classpath = %s", key.getAsURIs()));
        }
        lock.writeLock().lock();
        try {
            cacheEntries.remove(key);
            cleanups.remove(key);
        } finally {
            lock.writeLock().unlock();
        }
        try {
            entry.clear();
            entry.cleanup();
        } catch (Exception ex) {
            LOG.error("Unable to perform cleanup of classloader for classpath: "+key, ex);
        }
    }

    public ReferenceQueue<CachedClassLoader> getReferenceQueue() {
        return referenceQueue;
    }

    public void exit() {
        lock.writeLock().lock();
        try {
            stopped = true;
            interrupt();
            while (!cleanups.isEmpty()) {
                Map.Entry<ClassPath, Cleanup> entry = cleanups.entrySet().iterator().next();
                removeCacheEntry(entry.getKey(), entry.getValue());
            }
            LOG.debug("Completed shutdown");
        } finally {
            lock.writeLock().unlock();
        }
    }

    public void putCleanup(ClassPath key, Cleanup cleanup) {
        cleanups.put(key, cleanup);
    }
}
