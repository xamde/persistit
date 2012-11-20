/**
 * Copyright © 2011-2012 Akiban Technologies, Inc.  All rights reserved.
 * 
 * This program and the accompanying materials are made available
 * under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * This program may also be available under different license terms.
 * For more information, see www.akiban.com or contact licensing@akiban.com.
 * 
 * Contributors:
 * Akiban Technologies, Inc.
 */

package com.persistit;

import static org.junit.Assert.assertEquals;

import java.util.Properties;

import org.junit.Test;

import com.persistit.RecoveryManager.DefaultRecoveryListener;
import com.persistit.exception.MissingThreadException;
import com.persistit.exception.PersistitException;
import com.persistit.exception.TestException;
import com.persistit.unit.UnitTestProperties;
import com.persistit.util.Util;

/**
 * Found a way to reproduce this which matches some of the events others have
 * seen.
 * <p>
 * To reproduce, you need to be running some fairly intense activity - such as
 * dropping child tables. Then:
 * <ol>
 * <li>Terminate the process running persistit (e.g., kill -9)</li>
 * <li>Restart Persistit (e.g., the akiban-server).
 * <li>Fairly soon thereafter, shut down Persistit. "Fairly soon" means within
 * minutes. However, I believe that the huge transactions being generated by
 * dropping child table rows one-at-a-time within the scope of a single
 * transaction are extending "fairly soon" to "not-so-fairly-soon".
 * <li>Restart again. On this restart, there is a good chance of corruption.
 * </ol>
 * <p>
 * The bug is due to the mechanism called the "branch map" in the recovery
 * process. A checkpoint represents a timestamp T at which all pages updated
 * prior to T are present in the recovered B-Trees after recovery, and no pages
 * updated after T are present. Except the pages on the branch map. These are
 * required as a special accommodation for "long records" (blobs).
 * <p>
 * The issue is that on the first recovery, the post-checkpoint pages are
 * correctly segregated into the correct pageMap vs. branchMap bins. However,
 * due to some missing logic, this is not true on the second recovery. During
 * the second recovery, some pages that were updated after the checkpoint are
 * visible in the B-Trees; these, of course, are very wrong.
 * <p>
 * The case I've studied extensively was provoked by those huge drop
 * transactions, but this issue is theoretically possible in other scenarios.
 * 
 * @author Peter
 */
public class Bug777918Test extends PersistitUnitTestCase {

    
    @Override
    protected Properties getProperties(final boolean cleanup) {
        return UnitTestProperties.getBiggerProperties(cleanup);
    }

    @Test
    public void testDontMakeBranch() throws Exception {
        Exchange ex = _persistit.getExchange("persistit", "Bug777918Test", true);
        ex.getValue().put(RED_FOX);
        for (int i = 0; i < 100000; i++) {
            ex.to(i).store();
        }
        _persistit.checkpoint();
        for (int i = 100000; i < 200000; i++) {
            ex.to(i).store();
        }
        _persistit.getJournalManager().rollover();
        _persistit.close();

        _persistit = new Persistit(_config);
        _persistit.checkAllVolumes();
        ex = _persistit.getExchange("persistit", "Bug777918Test", false);
        // ensure updates after the checkpoint did make it, i.e.,
        // were not branched
        for (int i = 0; i < 200000; i++) {
            assertEquals(true, ex.to(i).isValueDefined());
        }
        _persistit.close();
        _persistit = new Persistit(_config);
        _persistit.checkAllVolumes();
        ex = _persistit.getExchange("persistit", "Bug777918Test", false);
        // ensure updates after the checkpoint did make it, i.e.,
        // were not branched
        for (int i = 0; i < 200000; i++) {
            assertEquals(true, ex.to(i).isValueDefined());
        }
    }

    @Test
    public void testMakeBranch() throws Exception {
        Exchange ex = _persistit.getExchange("persistit", "Bug777918Test", true);
        ex.getValue().put(RED_FOX);
        for (int i = 0; i < 100000; i++) {
            ex.to(i).store();
        }
        _persistit.checkpoint();
        for (int i = 100000; i < 200000; i++) {
            ex.to(i).store();
        }
        _persistit.flush();
        _persistit.crash();
        _persistit = new Persistit(_config);
        _persistit.checkAllVolumes();
        _persistit.close();

        _persistit = new Persistit(_config);
        _persistit.checkAllVolumes();
        ex = _persistit.getExchange("persistit", "Bug777918Test", false);
        // ensure updates after the checkpoint didn't make it
        for (int i = 0; i < 200000; i++) {
            assertEquals(i < 100000, ex.to(i).isValueDefined());
        }
    }

    @Test
    public void testMakeBranchTxn() throws Exception {
        Exchange ex = _persistit.getExchange("persistit", "Bug777918Test", true);
        ex.getValue().put(RED_FOX);
        for (int i = 0; i < 100000; i++) {
            ex.getTransaction().begin();
            ex.to(i).store();
            ex.getTransaction().commit();
            ex.getTransaction().end();

        }
        _persistit.checkpoint();
        for (int i = 100000; i < 200000; i++) {
            ex.getTransaction().begin();
            ex.to(i).store();
            ex.getTransaction().commit();
            ex.getTransaction().end();
        }
        _persistit.flush();
        _persistit.crash();
        _persistit = new Persistit(_config);
        _persistit.checkAllVolumes();
        _persistit.close();
        _persistit = new Persistit(_config);
        _persistit.checkAllVolumes();
        ex = _persistit.getExchange("persistit", "Bug777918Test", false);
        // ensure updates after the checkpoint didn't make it
        for (int i = 0; i < 200000; i++) {
            assertEquals(true, ex.to(i).isValueDefined());
        }
    }

    @Test
    public void testMakeBranchTxnLongRecord() throws Exception {
        final StringBuilder sb = new StringBuilder();

        while (sb.length() < 20000) {
            sb.append(RED_FOX);
        }
        Exchange ex = _persistit.getExchange("persistit", "Bug777918Test", true);
        ex.getValue().put(sb.toString());
        for (int i = 0; i < 10000; i++) {
            ex.getTransaction().begin();
            ex.to(i).store();
            ex.getTransaction().commit();
            ex.getTransaction().end();

        }
        _persistit.checkpoint();

        for (int i = 10000; i < 20000; i++) {
            ex.getTransaction().begin();
            ex.to(i).store();
            ex.getTransaction().commit();
            ex.getTransaction().end();
        }

        _persistit.flush();
        _persistit.crash();
        _persistit = new Persistit();
        _persistit.getRecoveryManager().setDefaultCommitListener(new TestCrashingRecoveryListener());
        _persistit.setConfiguration(_config);
        //
        // The recovery process deliberately crashes after applying some
        // transactions.
        //
        try {
            _persistit.initialize();
        } catch (final MissingThreadException e) {
            // expected
        } catch (final TestException e) {
            // expected
        }

        // This startup should divide the pages into page- and branch-map
        // and apply committed transactions using branch-map pages.
        //
        _persistit = new Persistit(_config);
        _persistit.checkAllVolumes();

        ex = _persistit.getExchange("persistit", "Bug777918Test", false);

        for (int i = 0; i < 20000; i++) {
            if (!ex.to(i).isValueDefined()) {
                System.out.println(i + " ");
            }
            assertEquals(true, ex.to(i).isValueDefined());
            assertEquals(sb.length(), ex.to(i).fetch().getValue().getString().length());
        }
    }

    @SuppressWarnings("serial")
    static class Bug777918Exception extends TestException {
        public Bug777918Exception(final long ts) {
            super("Purposely stop transaction recovery at ts=" + ts);
        }
    }

    class TestCrashingRecoveryListener extends DefaultRecoveryListener {
        boolean checkpointed = false;
        boolean crashed = false;

        @Override
        public void startTransaction(final long address, final long startTimestamp, final long commitTimestamp)
                throws PersistitException {
            if (startTimestamp > 50000 && !checkpointed) {
                _persistit.checkpoint();
                checkpointed = true;
            }
            if (startTimestamp > 100000 && !crashed) {
                _persistit.crash();
                crashed = true;
                /*
                 * Make sure the checkpoint manager thread has ended.
                 */
                Util.sleep(1000);
                throw new Bug777918Exception(startTimestamp);
            }
        }
    }

    @Override
    public void runAllTests() throws Exception {
        // TODO Auto-generated method stub

    }

}
