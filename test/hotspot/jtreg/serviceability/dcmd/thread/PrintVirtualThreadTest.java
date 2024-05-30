/*
 * Copyright (c) 2015, 2024, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

import com.beust.ah.A;
import jdk.test.lib.dcmd.CommandExecutor;
import jdk.test.lib.dcmd.JMXExecutor;
import jdk.test.lib.process.OutputAnalyzer;
import org.testng.Assert;
import org.testng.SkipException;
import org.testng.annotations.Test;

import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;
import java.util.regex.Pattern;

/*
 * @test
 * @summary Test of diagnostic command Thread.print with virtual threads
 * @library /test/lib
 * @modules java.base/jdk.internal.misc
 *          java.compiler
 *          java.management
 *          jdk.internal.jvmstat/sun.jvmstat.monitor
 * @run testng PrintVirtualThreadTest
 */
public class PrintVirtualThreadTest {

    public void run(CommandExecutor executor) throws InterruptedException {
        var shouldStop = new AtomicBoolean();
        var started = new CountDownLatch(1);
        final Runnable runnable = new DummyRunnable(shouldStop, started);
        Thread.startVirtualThread(runnable);
        started.await();
        /* Execute */
        OutputAnalyzer output = executor.execute("Thread.print");
        output.shouldMatch(".*at " + Pattern.quote(DummyRunnable.class.getName()) + "\\.run.*");
        output.shouldMatch(".*at " + Pattern.quote(DummyRunnable.class.getName()) + "\\.compute.*");
    }

    @Test
    public void jmx() throws InterruptedException {
        run(new JMXExecutor());
    }

    static class DummyRunnable implements Runnable {

        private final AtomicBoolean shouldStop;
        private final CountDownLatch started;

        public DummyRunnable(AtomicBoolean shouldStop, CountDownLatch started) {
           this.shouldStop = shouldStop;
           this.started = started;
        }

        public void run() {
            compute();
        }

        void compute() {
            started.countDown();
            while(true) {
                if (shouldStop.get()) {
                    break;
                }
            }
        }
    }


}
