/*
 * The MIT License
 *
 * Copyright 2022 zzambers.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package resultsview.storage;

import java.util.Collection;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class ConcurrentStorage extends Storage {

    final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    @Override
    public Job getJob(String name) {
        Lock rlock = lock.readLock();
        rlock.lock();
        try {
            return super.getJob(name);
        } finally {
            rlock.unlock();
        }
    }

    @Override
    public Pkg getPkg(String name) {
        Lock rlock = lock.readLock();
        rlock.lock();
        try {
            return super.getPkg(name);
        } finally {
            rlock.unlock();
        }
    }

    @Override
    public Collection<Job> getJobs() {
        Lock rlock = lock.readLock();
        rlock.lock();
        try {
            return super.getJobs();
        } finally {
            rlock.unlock();
        }
    }

    @Override
    public Collection<Pkg> getPkgs() {
        Lock rlock = lock.readLock();
        rlock.lock();
        try {
            return super.getPkgs();
        } finally {
            rlock.unlock();
        }
    }

    @Override
    public Collection<Run> getJobRuns(Job job) {
        Lock rlock = lock.readLock();
        rlock.lock();
        try {
            return super.getJobRuns(job);
        } finally {
            rlock.unlock();
        }
    }

    @Override
    public Collection<Run> getPkgRuns(Pkg pkg) {
        Lock rlock = lock.readLock();
        rlock.lock();
        try {
            return super.getPkgRuns(pkg);
        } finally {
            rlock.unlock();
        }
    }

    @Override
    public int getPkgRunsCount(Pkg pkg) {
        Lock rlock = lock.readLock();
        rlock.lock();
        try {
            return super.getPkgRunsCount(pkg);
        } finally {
            rlock.unlock();
        }
    }

    @Override
    public void removeJob(String name) {
        Lock wlock = lock.writeLock();
        wlock.lock();
        try {
            super.removeJob(name);
        } finally {
            wlock.unlock();
        }
    }

    @Override
    public void storeJob(Job job) {
        Lock wlock = lock.writeLock();
        wlock.lock();
        try {
            super.storeJob(job);
        } finally {
            wlock.unlock();
        }
    }

    @Override
    public void storePkg(Pkg pkg) {
        Lock wlock = lock.writeLock();
        wlock.lock();
        try {
            super.storePkg(pkg);
        } finally {
            wlock.unlock();
        }
    }

    @Override
    public void storeRun(Run run) {
        Lock wlock = lock.writeLock();
        wlock.lock();
        try {
            super.storeRun(run);
        } finally {
            wlock.unlock();
        }
    }

    @Override
    public void addPkgRun(Pkg pkg, Run run) {
        Lock wlock = lock.writeLock();
        wlock.lock();
        try {
            super.addPkgRun(pkg, run);
        } finally {
            wlock.unlock();
        }
    }

    @Override
    public Run getJobLatestRun(Job job) {
        Lock rlock = lock.readLock();
        rlock.lock();
        try {
            return super.getJobLatestRun(job);
        } finally {
            rlock.unlock();
        }
    }

    @Override
    public void setJobLatestRun(Job job, Run run) {
        Lock wlock = lock.writeLock();
        wlock.lock();
        try {
            super.setJobLatestRun(job, run);
        } finally {
            wlock.unlock();
        }
    }

    @Override
    public void addUnfinishedRun(Run run) {
        Lock wlock = lock.writeLock();
        wlock.lock();
        try {
            super.addUnfinishedRun(run);
        } finally {
            wlock.unlock();
        }
    }

    @Override
    public void removeUnfinishedRun(Run run) {
        Lock wlock = lock.writeLock();
        wlock.lock();
        try {
            super.removeUnfinishedRun(run);
        } finally {
            wlock.unlock();
        }
    }

    @Override
    public Collection<Run> getUnfinishedRuns() {
        Lock rlock = lock.readLock();
        rlock.lock();
        try {
            return super.getUnfinishedRuns();
        } finally {
            rlock.unlock();
        }
    }

}
