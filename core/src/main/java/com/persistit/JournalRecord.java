/**
 * Copyright (C) 2011 Akiban Technologies Inc.
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License, version 3,
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see http://www.gnu.org/licenses.
 */

package com.persistit;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;

/**
 * This class encapsulates the formats of Persistit journal records. There is
 * one inner class per record type. The following describes the byte layout
 * implemented by these classes.
 * 
 * All multi-byte integers are stored in big-endian form. General record format:
 * 
 * <table border="1">
 * <tr valign="top">
 * <td>+0</td>
 * <td>length</td>
 * </tr>
 * <tr valign="top">
 * <td>+4</td>
 * <td>type</td>
 * </tr>
 * <tr valign="top">
 * <td>+8</td>
 * <td>timestamp</td>
 * </tr>
 * <tr valign="top">
 * <td>+16</td>
 * <td>payload</td>
 * </tr>
 * </table>
 * <p />
 * Type: two ASCII bytes:
 * <p />
 * <table border="1">
 * 
 * <tr valign="top">
 * <td>JH</td>
 * <td>Journal Header: written as the first record of each journal file.
 * Identifies the version, creation timestamp of the journal, created timestamp
 * of the journal file and path to which journal file was originally written.
 * <table>
 * <tr valign="top">
 * <td>+16</td>
 * <td>Version (long)</td>
 * </tr>
 * <tr valign="top">
 * <td>+24</td>
 * <td>Journal file size (long)</td>
 * </tr>
 * <tr valign="top">
 * <td>+32</td>
 * <td>Current journal address (long)</td>
 * </tr>
 * <tr valign="top">
 * <td>+40</td>
 * <td>Base journal address (long)</td>
 * </tr>
 * <tr valign="top">
 * <td>+48</td>
 * <td>Journal created timestamp (long)</td>
 * </tr>
 * <tr valign="top">
 * <td>+56</td>
 * <td>Journal file created timestamp (long)</td>
 * </tr>
 * <tr valign="top">
 * <td>+64</td>
 * <td>Last valid checkpoint timestamp (long)</td>
 * </tr>
 * <tr valign="top">
 * <td>+72</td>
 * <td>Reserved (8 bytes)</td>
 * </tr>
 * <tr valign="top">
 * <td>+80</td>
 * <td>Journal File Path (variable - length determined by record length)</td>
 * </tr>
 * </table>
 * </td>
 * </tr>
 * 
 * <tr valign="top">
 * <td>JE</td>
 * <td>Journal End: written as the last record of each journal file. Indicates
 * that the next valid record is in the next journal file, and confirms that the
 * journal file is complete. Lack of a JE record at the end of a journal file
 * indicates the system did not shut down normally.
 * <table>
 * <tr valign="top">
 * <td>+16</td>
 * <td>Current journal address (long)</td>
 * </tr>
 * <tr valign="top">
 * <td>+24</td>
 * <td>Base journal address (long)</td>
 * </tr>
 * <tr valign="top">
 * <td>+32</td>
 * <td>Journal created timestamp (long)</td>
 * </tr>
 * </table>
 * </td>
 * </tr>
 * 
 * <tr valign="top">
 * <td>IV</td>
 * <td>Identify Volume: associates an integer handle to a Volume. This handle is
 * referenced by subsequent log records to identify this Volume. The handle has
 * no meaning beyond the scope of one log file; every new log generation gets
 * new IV records.
 * <table>
 * <tr valign="top">
 * <td>+16</td>
 * <td>Volume handle (int)</td>
 * </tr>
 * <tr valign="top">
 * <td>+20</td>
 * <td>Volume Id (long)</td>
 * </tr>
 * <tr valign="top">
 * <td>+28</td>
 * <td>Volume Path (variable - length determined by record length)</td>
 * </tr>
 * </table>
 * </td>
 * </tr>
 * 
 * <tr valign="top">
 * <td>IT</td>
 * <td>Identify Tree: associates an integer handle to a Tree. This handle is
 * referenced by subsequent log records to identify this Tree. The handle has no
 * meaning beyond the scope of one log file; every new log generation gets new
 * IT records.
 * <table>
 * <tr valign="top">
 * <td>+16</td>
 * <td>Tree handle (int)</td>
 * </tr>
 * <tr valign="top">
 * <td>+20</td>
 * <td>Volume handle (int)</td>
 * </tr>
 * <tr valign="top">
 * <td>+24</td>
 * <td>Tree Name (variable - length determined by record length)</td>
 * </tr>
 * </table>
 * </td>
 * </tr>
 * 
 * <tr valign="top">
 * <td>PA</td>
 * <td>Page Image
 * <table>
 * <tr valign="top">
 * <td>+16</td>
 * <td>Volume handle (int) - refers to a volume defined in a preceding IV record
 * </td>
 * </tr>
 * <tr valign="top">
 * <td>+20</td>
 * <td>page address (long)</td>
 * </tr>
 * <tr valign="top">
 * <td>+28</td>
 * <td>leftSize (int)</td>
 * </tr>
 * <tr valign="top">
 * <td>+32</td>
 * <td>bytes: the first leftSize bytes will go into the page at offset 0 the
 * remaining bytes will go to the end of the page; the middle of the page will
 * be cleared</td>
 * </tr>
 * </table>
 * </td>
 * </tr>
 * 
 * <tr valign="top">
 * <td>PM</td>
 * <td>Page Map: written once near the top of each journal file. Represents the
 * state of the page map at the time the journal rolled over.
 * <table>
 * <tr valign="top">
 * <td>+16</td>
 * <td></td>
 * <td>Instances of the following fixed-length structure (28 bytes), number
 * determined by overall record size
 * <table>
 * <tr valign="top">
 * <td>+0</td>
 * <td>Transaction timestamp (long)</td>
 * </tr>
 * <tr valign="top">
 * <td>+8</td>
 * <td>Journal address (long)</td>
 * </tr>
 * <tr valign="top">
 * <td>+16</td>
 * <td>Volume handle (int) - refers to a volume defined in a preceding IV record
 * </td>
 * </tr>
 * <tr valign="top">
 * <td>+20</td>
 * <td>Page address (long)</td>
 * </tr>
 * </table>
 * </td>
 * </tr>
 * </table>
 * </td>
 * </tr>
 * 
 * <tr valign="top">
 * <td>TM</td>
 * <td>Transaction Map: written once near the top of each journal file.
 * Represents map of transactions still open (started, but neither rolled back
 * nor committed) at the time the journal rolled over.
 * <table>
 * <tr valign="top">
 * <td>+16</td>
 * <td></td>
 * <td>Instances of the following fixed-length structure (24 bytes) number
 * determined by overall record size
 * <table>
 * <tr valign="top">
 * <td>+0</td>
 * <td>Transaction timestamp (long)</td>
 * </tr>
 * <tr valign="top">
 * <td>+8</td>
 * <td>Journal address (long)</td>
 * </tr>
 * <tr valign="top">
 * <td>+16</td>
 * <td>isCommitted (byte) - indicates whether the journal already contains a TC
 * record for this transaction</td>
 * </tr>
 * </table>
 * </td>
 * </tr>
 * </table>
 * </td>
 * </tr>
 * 
 * <tr valign="top">
 * <td>CP</td>
 * <td>Checkpoint. Specifies a timestamp and a system time in millis at which
 * all pages modified prior to that timestamp are present in the log.
 * <table>
 * <tr valign="top">
 * <td>+16</td>
 * <td>System time in milliseconds (long)</td>
 * </tr>
 * <tr valign="top">
 * <td>+24</td>
 * <td>Base journal address (long)</td>
 * </tr>
 * </table>
 * </td>
 * </tr>
 * 
 * <tr valign="top">
 * <td>TS</td>
 * <td>Transaction Start: binds subsequent records having same timestamp to a
 * commit
 * <table>
 * <tr valign="top">
 * <td>+16</td>
 * <td>Starting timestamp (long)- the timestamp assigned when the transaction
 * started. The base timestamp is the later commit timestamp.</td>
 * </tr>
 * </table>
 * </td>
 * </tr>
 * 
 * <tr valign="top">
 * <td>TC</td>
 * <td>Transaction Commit: all records having same or linked time-stamps should
 * be applied
 * <table>
 * <tr valign="top">
 * <td>+16</td>
 * <td>Transaction ID (long).</td>
 * </tr>
 * </table>
 * </td>
 * </tr>
 * 
 * <tr valign="top">
 * <td>SR</td>
 * <td>Store Record - specifies a Tree into which a key/value pair should be
 * inserted
 * <table>
 * <tr valign="top">
 * <td>+16</td>
 * <td>Tree handle (int) - matches a tree identified in a preceding IT record</td>
 * </tr>
 * <tr valign="top">
 * <td>+20</td>
 * <td>Key size (short)</td>
 * </tr>
 * <tr valign="top">
 * <td>+22</td>
 * <td>Key bytes immediately followed by Value bytes (variable).</td>
 * </tr>
 * </table>
 * 
 * </tr>
 * <tr valign="top">
 * <td>DV</td>
 * <td>Delete Volume - specifies a Volume to be deleted.
 * <table>
 * <tr valign="top">
 * <td>+16</td>
 * <td>Volume handle (int) - matches a volume identified in a preceding IV
 * record</td>
 * </tr>
 * </table>
 * </tr>
 * 
 * <tr valign="top">
 * <td>DT</td>
 * <td>Delete Tree - specifies a Tree to be deleted.
 * <table>
 * <tr valign="top">
 * <td>+16</td>
 * <td>Tree handle (int) - matches a tree identified in a preceding IT record</td>
 * </tr>
 * </table>
 * </tr>
 * 
 * <tr valign="top">
 * <td>DR</td>
 * <td>Delete Record - specifies a Tree and two Keys: all key/value pairs
 * between these two keys (inclusive) are deleted. The Key bytes field defines
 * two keys, key1 and key2. These delimit the range to be deleted. The first
 * Key1_size bytes of this field contain the encoded key1 value. The remaining
 * bytes define key2. The first Elision_count bytes of Key2 are the same as
 * Key1; only the remaining unique bytes are stored in the record.
 * <table>
 * <tr valign="top">
 * <td>+16</td>
 * <td>Tree handle (int) - matches a tree identified in a preceding IT record</td>
 * </tr>
 * <tr valign="top">
 * <td>+20</td>
 * <td>Key1_size (short)</td>
 * </tr>
 * <tr valign="top">
 * <td>+22</td>
 * <td>Key2 Elision_count (short)</td>
 * </tr>
 * <tr valign="top">
 * <td>+24</td>
 * <td>Key bytes</td>
 * </tr>
 * </table>
 * </tr>
 * 
 * </table>
 * 
 * @author peter
 * 
 */
public class JournalRecord {

    private final static Charset UTF8 = Charset.forName("UTF-8");

    public final static int OVERHEAD = 16;

    public final static int[] TYPES = new int[] { JE.TYPE, JH.TYPE, PA.TYPE,
            PM.TYPE, SR.TYPE, DR.TYPE, DT.TYPE, TM.TYPE, TS.TYPE, TC.TYPE,
            CP.TYPE, IV.TYPE, IT.TYPE };

    public static boolean isValidType(final int t) {
        for (int type : TYPES) {
            if (t == type) {
                return true;
            }
        }
        return false;
    }

    public static String str(final int t) {
        if (isValidType(t)) {
            return new String(new char[] { (char) ((t >>> 8) & 0xFF),
                    (char) (t & 0xFF) });
        } else {
            return "??";
        }
    }

    private static void putByte(final ByteBuffer bb, final int offset,
            final int value) {
        Util.putByte(bb.array(), bb.position() + offset, value);
    }

    static int getByte(final ByteBuffer bb, final int offset) {
        return Util.getByte(bb.array(), bb.position() + offset);
    }

    static void putChar(final ByteBuffer bb, final int offset, final int value) {
        Util.putChar(bb.array(), bb.position() + offset, value);
    }

    static int getChar(final ByteBuffer bb, final int offset) {
        return Util.getChar(bb.array(), bb.position() + offset);
    }

    static void putInt(final ByteBuffer bb, final int offset, final int value) {
        Util.putInt(bb.array(), bb.position() + offset, value);
    }

    static int getInt(final ByteBuffer bb, final int offset) {
        return Util.getInt(bb.array(), bb.position() + offset);
    }

    static void putLong(final ByteBuffer bb, final int offset, final long value) {
        Util.putLong(bb.array(), bb.position() + offset, value);
    }

    static long getLong(final ByteBuffer bb, final int offset) {
        return Util.getLong(bb.array(), bb.position() + offset);
    }

    static int getLength(final ByteBuffer bb) {
        return getInt(bb, 0);
    }

    static void putLength(final ByteBuffer bb, int length) {
        putInt(bb, 0, length);
    }

    public static int getType(final ByteBuffer bb) {
        return getChar(bb, 4);
    }

    static void putType(final ByteBuffer bb, int type) {
        putChar(bb, 4, type);
    }

    public static long getTimestamp(final ByteBuffer bb) {
        return getLong(bb, 8);
    }

    public static void putTimestamp(final ByteBuffer bb, long timestamp) {
        putLong(bb, 8, timestamp);
    }

    /**
     * Journal End
     */
    public static class JE extends JournalRecord {

        public final static int TYPE = ('J' << 8) | 'E';

        public static int OVERHEAD = 40;

        public static void putType(final ByteBuffer bb) {
            putType(bb, TYPE);
        }

        public static long getCurrentJournalAddress(final ByteBuffer bb) {
            return getLong(bb, 16);
        }

        public static void putCurrentJournalAddress(final ByteBuffer bb,
                final long address) {
            putLong(bb, 16, address);
        }

        public static long getBaseAddress(final ByteBuffer bb) {
            return getLong(bb, 24);
        }

        public static void putBaseAddress(final ByteBuffer bb,
                final long address) {
            putLong(bb, 24, address);
        }

        public static long getJournalCreatedTime(final ByteBuffer bb) {
            return getLong(bb, 32);
        }

        public static void putJournalCreatedTime(final ByteBuffer bb,
                final long time) {
            putLong(bb, 32, time);
        }
    }

    /**
     * Journal header
     */
    public static class JH extends JournalRecord {

        public final static int TYPE = ('J' << 8) | 'H';

        public static int OVERHEAD = 64;

        public static int MAX_LENGTH = OVERHEAD + 2048;

        public static void putType(final ByteBuffer bb) {
            putType(bb, TYPE);
        }

        public static long getVersion(final ByteBuffer bb) {
            return getLong(bb, 16);
        }

        public static void putVersion(final ByteBuffer bb, final long version) {
            putLong(bb, 16, version);
        }

        public static long getBaseJournalAddress(final ByteBuffer bb) {
            return getLong(bb, 24);
        }

        public static void putBaseJournalAddress(final ByteBuffer bb,
                final long address) {
            putLong(bb, 24, address);
        }

        public static long getCurrentJournalAddress(final ByteBuffer bb) {
            return getLong(bb, 32);
        }

        public static void putCurrentJournalAddress(final ByteBuffer bb,
                final long address) {
            putLong(bb, 32, address);
        }

        public static long getJournalCreatedTime(final ByteBuffer bb) {
            return getLong(bb, 40);
        }

        public static void putJournalCreatedTime(final ByteBuffer bb,
                final long time) {
            putLong(bb, 40, time);
        }

        public static long getFileCreatedTime(final ByteBuffer bb) {
            return getLong(bb, 48);
        }

        public static void putFileCreatedTime(final ByteBuffer bb,
                final long time) {
            putLong(bb, 48, time);
        }

        public static long getBlockSize(final ByteBuffer bb) {
            return getLong(bb, 56);
        }

        public static void putBlockSize(final ByteBuffer bb, final long size) {
            putLong(bb, 56, size);
        }

        public static String getPath(final ByteBuffer bb) {
            final int length = getLength(bb) - OVERHEAD;
            return new String(bb.array(), bb.position() + OVERHEAD, length,
                    UTF8);
        }

        public static void putPath(final ByteBuffer bb, final String path) {
            final byte[] stringBytes = path.getBytes(UTF8);
            System.arraycopy(stringBytes, 0, bb.array(), bb.position()
                    + OVERHEAD, stringBytes.length);
            putLength(bb, OVERHEAD + stringBytes.length);
        }
    }

    /**
     * Page Map
     */
    public static class PM extends JournalRecord {

        public final static int TYPE = ('P' << 8) | 'M';

        public static int OVERHEAD = 16;

        public static int ENTRY_SIZE = 28;

        public static void putType(final ByteBuffer bb) {
            putType(bb, TYPE);
        }

        public static int getEntryCount(final ByteBuffer bb) {
            final int length = getLength(bb) - OVERHEAD;
            return length / ENTRY_SIZE;
        }

        public static long getEntryTimestamp(final ByteBuffer bb,
                final int index) {
            return getLong(bb, (index * ENTRY_SIZE));
        }

        public static long getEntryJournalAddress(final ByteBuffer bb,
                final int index) {
            return getLong(bb, 8 + (index * ENTRY_SIZE));
        }

        public static int getEntryVolumeHandle(final ByteBuffer bb,
                final int index) {
            return getInt(bb, 16 + (index * ENTRY_SIZE));
        }

        public static long getEntryPageAddress(final ByteBuffer bb,
                final int index) {
            return getLong(bb, 20 + (index * ENTRY_SIZE));
        }

        public static void putEntry(final ByteBuffer bb, final int index,
                final long timestamp, final long journalAddress,
                final int volumeHandle, final long pageAddress) {
            putLong(bb, 0 + (index * ENTRY_SIZE), timestamp);
            putLong(bb, 8 + (index * ENTRY_SIZE), journalAddress);
            putInt(bb, 16 + (index * ENTRY_SIZE), volumeHandle);
            putLong(bb, 20 + (index * ENTRY_SIZE), pageAddress);
        }
    }

    /**
     * Transaction Map
     */
    public static class TM extends JournalRecord {

        public final static int TYPE = ('T' << 8) | 'M';

        public static int OVERHEAD = 16;

        public static int ENTRY_SIZE = 25;

        public static void putType(final ByteBuffer bb) {
            putType(bb, TYPE);
        }

        public static int getEntryCount(final ByteBuffer bb) {
            final int length = getLength(bb) - OVERHEAD;
            return length / ENTRY_SIZE;
        }

        public static long getEntryStartTimestamp(final ByteBuffer bb,
                final int index) {
            return getLong(bb, (index * ENTRY_SIZE));
        }

        public static long getEntryCommitTimestamp(final ByteBuffer bb,
                final int index) {
            return getLong(bb, 8 + (index * ENTRY_SIZE));
        }

        public static long getEntryJournalAddress(final ByteBuffer bb,
                final int index) {
            return getLong(bb, 16 + (index * ENTRY_SIZE));
        }

        public static boolean getEntryCommitted(final ByteBuffer bb,
                final int index) {
            return getByte(bb, 24 + (index * ENTRY_SIZE)) != 0;
        }

        public static void putEntry(final ByteBuffer bb, final int index,
                final long startTimestamp, final long commitTimestamp,
                final long journalAddress, final boolean committed) {
            putLong(bb, 0 + (index * ENTRY_SIZE), startTimestamp);
            putLong(bb, 8 + (index * ENTRY_SIZE), commitTimestamp);
            putLong(bb, 16 + (index * ENTRY_SIZE), journalAddress);
            putByte(bb, 24 + (index * ENTRY_SIZE), committed ? 0xFF : 0);
        }

    }

    /**
     * Identify Volume
     */
    public static class IV extends JournalRecord {

        public final static int TYPE = ('I' << 8) | 'V';

        public static int OVERHEAD = 28;

        public static int MAX_LENGTH = OVERHEAD + 2048;

        public static void putType(final ByteBuffer bb) {
            putType(bb, TYPE);
        }

        public static int getHandle(final ByteBuffer bb) {
            return getInt(bb, 16);
        }

        public static void putHandle(final ByteBuffer bb, final int handle) {
            putInt(bb, 16, handle);
        }

        public static long getVolumeId(final ByteBuffer bb) {
            return getLong(bb, 20);
        }

        public static void putVolumeId(final ByteBuffer bb, final long volumeId) {
            putLong(bb, 20, volumeId);
        }

        public static String getVolumeName(final ByteBuffer bb) {
            final int length = getLength(bb) - OVERHEAD;
            return new String(bb.array(), bb.position() + OVERHEAD, length,
                    UTF8);
        }

        public static void putVolumeName(final ByteBuffer bb,
                final String volumeName) {
            final byte[] stringBytes = volumeName.getBytes(UTF8);
            System.arraycopy(stringBytes, 0, bb.array(), bb.position()
                    + OVERHEAD, stringBytes.length);
            putLength(bb, OVERHEAD + stringBytes.length);
        }
    }

    /**
     * Identify Tree
     */
    public static class IT extends JournalRecord {

        public final static int TYPE = ('I' << 8) | 'T';

        public static int OVERHEAD = 24;

        public static int MAX_LENGTH = OVERHEAD + 1024;

        public static void putType(final ByteBuffer bb) {
            putType(bb, TYPE);
        }

        public static int getHandle(final ByteBuffer bb) {
            return getInt(bb, 16);
        }

        public static void putHandle(final ByteBuffer bb, final int handle) {
            putInt(bb, 16, handle);
        }

        public static int getVolumeHandle(final ByteBuffer bb) {
            return getInt(bb, 20);
        }

        public static void putVolumeHandle(final ByteBuffer bb,
                final int volumeHandle) {
            putInt(bb, 20, volumeHandle);
        }

        public static String getTreeName(final ByteBuffer bb) {
            final int length = getLength(bb) - OVERHEAD;
            return new String(bb.array(), bb.position() + OVERHEAD, length,
                    UTF8);
        }

        public static void putTreeName(final ByteBuffer bb,
                final String treeName) {
            final byte[] stringBytes = treeName.getBytes(UTF8);
            System.arraycopy(stringBytes, 0, bb.array(), bb.position()
                    + OVERHEAD, stringBytes.length);
            putLength(bb, OVERHEAD + stringBytes.length);
        }
    }

    /**
     * Page
     */
    public static class PA extends JournalRecord {

        public final static int TYPE = ('P' << 8) | 'A';

        public final static int OVERHEAD = 36;

        public static void putType(final ByteBuffer bb) {
            putType(bb, TYPE);
        }

        public static int getVolumeHandle(final ByteBuffer bb) {
            return getInt(bb, 16);
        }

        public static void putVolumeHandle(final ByteBuffer bb,
                final int volumeHandle) {
            putInt(bb, 16, volumeHandle);
        }

        public static long getPageAddress(final ByteBuffer bb) {
            return getLong(bb, 20);
        }

        public static void putPageAddress(final ByteBuffer bb,
                final long pageAddress) {
            putLong(bb, 20, pageAddress);
        }

        public static int getLeftSize(final ByteBuffer bb) {
            return getInt(bb, 28);
        }

        public static void putLeftSize(final ByteBuffer bb, final int leftSize) {
            putInt(bb, 28, leftSize);
        }

        public static int getBufferSize(final ByteBuffer bb) {
            return getInt(bb, 32);
        }

        public static void putBufferSize(final ByteBuffer bb,
                final int bufferSize) {
            putInt(bb, 32, (char) bufferSize);
        }

    }

    /**
     * Checkpoint
     */
    public static class CP extends JournalRecord {

        public final static int TYPE = ('C' << 8) | 'P';

        public final static int OVERHEAD = 32;

        public static void putType(final ByteBuffer bb) {
            putType(bb, TYPE);
        }

        public static long getSystemTimeMillis(final ByteBuffer bb) {
            return getLong(bb, 16);
        }

        public static void putSystemTimeMillis(final ByteBuffer bb,
                final long systemTimeMillis) {
            putLong(bb, 16, systemTimeMillis);
        }

        public static long getBaseAddress(final ByteBuffer bb) {
            return getLong(bb, 24);
        }

        public static void putBaseAddress(final ByteBuffer bb, final long base) {
            putLong(bb, 24, base);
        }

    }

    /**
     * Transaction Start
     */
    public static class TS extends JournalRecord {

        public final static int TYPE = ('T' << 8) | 'S';

        public final static int OVERHEAD = 24;

        public static void putType(final ByteBuffer bb) {
            putType(bb, TYPE);
        }

        public static void putStartTimestamp(final ByteBuffer bb,
                final long startTimestamp) {
            putLong(bb, 16, startTimestamp);
        }

        public static long getStartTimestamp(final ByteBuffer bb) {
            return getLong(bb, 16);
        }
    }

    /**
     * Transaction Commit
     */
    public static class TC extends JournalRecord {

        public final static int TYPE = ('T' << 8) | 'C';

        public final static int OVERHEAD = 16;

        public static void putType(final ByteBuffer bb) {
            putType(bb, TYPE);
        }
    }

    /**
     * Store Record
     */
    public static class SR extends JournalRecord {

        public final static int TYPE = ('S' << 8) | 'R';

        public final static int OVERHEAD = 22;

        public static void putType(final ByteBuffer bb) {
            putType(bb, TYPE);
        }

        public static void putTreeHandle(final ByteBuffer bb, final int handle) {
            putInt(bb, 16, handle);
        }

        public static int getTreeHandle(final ByteBuffer bb) {
            return getInt(bb, 16);
        }

        public static void putKeySize(final ByteBuffer bb, final int size) {
            putChar(bb, 20, size);
        }

        public static int getKeySize(final ByteBuffer bb) {
            return getChar(bb, 20);
        }
    }

    /**
     * Delete Record
     */
    public static class DR extends JournalRecord {

        public final static int TYPE = ('D' << 8) | 'R';

        public final static int OVERHEAD = 22;

        public static void putType(final ByteBuffer bb) {
            putType(bb, TYPE);
        }

        public static void putTreeHandle(final ByteBuffer bb, final int handle) {
            putInt(bb, 16, handle);
        }

        public static int getTreeHandle(final ByteBuffer bb) {
            return getInt(bb, 16);
        }

        public static void putKey1Size(final ByteBuffer bb, final int size) {
            putChar(bb, 20, size);
        }

        public static int getKey1Size(final ByteBuffer bb) {
            return getChar(bb, 20);
        }

    }

    /**
     * Delete Tree
     */
    public static class DT extends JournalRecord {

        public final static int TYPE = ('D' << 8) | 'T';

        public final static int OVERHEAD = 20;

        public static void putType(final ByteBuffer bb) {
            putType(bb, TYPE);
        }

        public static void putTreeHandle(final ByteBuffer bb, final int handle) {
            putInt(bb, 16, handle);
        }

        public static int getTreeHandle(final ByteBuffer bb) {
            return getInt(bb, 16);
        }
    }
}
