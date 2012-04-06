/**
 * END USER LICENSE AGREEMENT (“EULA”)
 *
 * READ THIS AGREEMENT CAREFULLY (date: 9/13/2011):
 * http://www.akiban.com/licensing/20110913
 *
 * BY INSTALLING OR USING ALL OR ANY PORTION OF THE SOFTWARE, YOU ARE ACCEPTING
 * ALL OF THE TERMS AND CONDITIONS OF THIS AGREEMENT. YOU AGREE THAT THIS
 * AGREEMENT IS ENFORCEABLE LIKE ANY WRITTEN AGREEMENT SIGNED BY YOU.
 *
 * IF YOU HAVE PAID A LICENSE FEE FOR USE OF THE SOFTWARE AND DO NOT AGREE TO
 * THESE TERMS, YOU MAY RETURN THE SOFTWARE FOR A FULL REFUND PROVIDED YOU (A) DO
 * NOT USE THE SOFTWARE AND (B) RETURN THE SOFTWARE WITHIN THIRTY (30) DAYS OF
 * YOUR INITIAL PURCHASE.
 *
 * IF YOU WISH TO USE THE SOFTWARE AS AN EMPLOYEE, CONTRACTOR, OR AGENT OF A
 * CORPORATION, PARTNERSHIP OR SIMILAR ENTITY, THEN YOU MUST BE AUTHORIZED TO SIGN
 * FOR AND BIND THE ENTITY IN ORDER TO ACCEPT THE TERMS OF THIS AGREEMENT. THE
 * LICENSES GRANTED UNDER THIS AGREEMENT ARE EXPRESSLY CONDITIONED UPON ACCEPTANCE
 * BY SUCH AUTHORIZED PERSONNEL.
 *
 * IF YOU HAVE ENTERED INTO A SEPARATE WRITTEN LICENSE AGREEMENT WITH AKIBAN FOR
 * USE OF THE SOFTWARE, THE TERMS AND CONDITIONS OF SUCH OTHER AGREEMENT SHALL
 * PREVAIL OVER ANY CONFLICTING TERMS OR CONDITIONS IN THIS AGREEMENT.
 */

package com.persistit.mxbeans;

import com.persistit.AlertMonitor;
import com.persistit.AlertMonitor.AlertLevel;

/**
 * Methods of {@link AlertMonitor intended to be exposed in an MXBean. This
 * interface should be extended by the MXBean interface for a concrete
 * implementation such as {@link IOAlertMonitorMXBean}.
 */
public interface AlertMonitorMXBean {

    public final static String MXBEAN_NAME = "com.persistit:type=Persistit,class=AlertMonitor";

    final static String MANY_JOURNAL_FILES = "JournalFiles";
    final static String JOURNAL_CATEGORY = "Journal";
    final static String WRITE_PAGE_CATEGORY = "WritePage";
    final static String READ_PAGE_CATEGORY = "ReadPage";
    final static String EXTEND_VOLUME_CATEGORY = "ExtendVolume";
    final static String FLUSH_STATISTICS_CATEGORY = "FlushStatistics";
    final static String CLEANUP_CATEGORY = "Cleanup";

    /**
     * Current maximum AlertLevel in this monitor as a String: one of NORMAL,
     * WARN or ERROR
     */
    @Description("Current maximum AlertLevel in this monitor as a String: one of NORMAL, WARN or ERROR.")
    String getAlertLevel();

    /**
     * Restore this alert monitor to its initial state at level
     * {@link AlertLevel#NORMAL} with no history.
     */
    @Description("Restore this alert monitor to its initial state.")
    void reset();

    /**
     * @return the interval in milliseconds between successive notifications for
     *         this monitor when its level is {@link AlertLevel#WARN}.
     */
    @Description("The interval in milliseconds between successive notifications"
            + " for this monitor when its level is WARN.")
    long getWarnLogTimeInterval();

    /**
     * Set the interval between successive notifications for this monitor when
     * its {@link AlertLevel#WARN}.
     * 
     * @param warnLogTimeInterval
     *            the interval in milliseconds
     */
    @Description("The interval in milliseconds between successive notifications"
            + " for this monitor when its level is WARN.")
    void setWarnLogTimeInterval(long warnLogTimeInterval);

    /**
     * @return the interval in milliseconds between successive notifications for
     *         this monitor when its {@link AlertLevel#ERROR}.
     */
    @Description("The interval in milliseconds between successive notifications"
            + " for this monitor when its level is ERROR.")
    long getErrorLogTimeInterval();

    /**
     * Set the interval between successive notifications for this monitor when
     * its {@link AlertLevel#ERROR}.
     * 
     * @param _warnLogTimeInterval
     *            the interval in milliseconds
     */
    @Description("The interval in milliseconds between successive notifications"
            + " for this monitor when its level is ERROR.")
    void setErrorLogTimeInterval(long errorLogTimeInterval);

    /**
     * @return The number of events per category for which to keep individual
     *         Events.
     */
    @Description("The number of events per category for which to keep individual Events.")
    int getHistoryLength();

    /**
     * Set the number of events per category on which to keep a complete
     * history. Once the number of events exceeds this count, the events
     * aggregated.
     * 
     * @param historyLength
     *            the historyLength to set
     */
    @Description("The number of events per category for which to keep individual Events.")
    void setHistoryLength(@PName("historyLength") int historyLength);

    /**
     * Return a summary of this AlertMonitor's current state.
     * 
     * @return a summary report
     */
    @Description("A summary of this AlertMonitor's current state")
    String getSummary();

    /**
     * Return a detailed description, including first and recent occurrences of
     * events within the History for the specified category. If there is no
     * history for the specified category, this method returns <code>null</code>
     * .
     * 
     * @param category
     *            the category name
     * @return the detailed report
     */
    @Description("A detailed description, including first and recent occurrences of"
            + " events within the History for the specified category")
    String getDetailedHistory(final String category);

    /**
     * Called periodically to emit log messages
     * 
     * @param force
     *            Whether to force notifications to be issued immediatel
     */
    @Description("Operation called periodically to emit notifications and log messages.")
    void poll(
            @PName("force") @Description("Whether to force notifications to be issued immediately") final boolean force);

}
