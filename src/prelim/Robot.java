package prelim;
import battlecode.common.*;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Map;

abstract class Robot {
    static RobotController rc = null;
    static RobotType type = null;
    static Team ally, enemy;

    static MapLocation spawnPos;

    /* CONSTANTS */
    static final int INFO_AMT = 14;
    static final int MINER_SOUP_THRESHOLD = 100;
    static final int MINER_RATE = 50;
    static final int LANDSCAPER_SOUP_THRESHOLD = 100;
    static final int LANDSCAPER_RATE = 50;
    static final int DRONE_SOUP_THRESHOLD = 100;
    static final int DRONE_RATE = 50;


    public static void init(RobotController rc) {
        Robot.rc = rc;
        type = rc.getType();
        spawnPos = rc.getLocation();

        ally = rc.getTeam();
        enemy = ally.opponent();
    }


    /**
     * Called once when the unit first starts to run code.
     * Executed just before the first call to onUpdate.
     */
    abstract void onAwake() throws GameActionException;

    /**
     * Called repeatedly until the unit dies or the game ends.
     * A single invocation may take longer than one tick.
     */
    abstract void onUpdate() throws GameActionException;

    /**
     * Convert from direction to integer, in ccw direction.
     * @param dir direction input
     * @return direction to number
     */
    static int directionToInt(Direction dir) {
        switch(dir) {
            case EAST: return 0;
            case NORTHEAST: return 1;
            case NORTH: return 2;
            case NORTHWEST: return 3;
            case WEST: return 4;
            case SOUTHWEST: return 5;
            case SOUTH: return 6;
            case SOUTHEAST: return 7;
            default: return -1;
        }
    }





    // Communication Code

    // HIGH LEVEL COMMUNICATION


    final static byte globalStatusHeader = 0b00000000;

//    final static byte soupLocationHeader = 0b00000001;
//
//    public static void postSoupLocation(MapLocation ml, int amount) {
//
//    }
//
//    public static void updateSoupLocations() {
//
//    }
//
//
//    public static class SoupLocation {
//        byte amount;
//        MapLocation location;
//    }

    public static class Symmetry {
        byte Unknown = 0;
        byte Horizontal = 0b00000001;
        byte Vertical =   0b00000010;
        byte Rotational = 0b00000100;
    }

    public static class GlobalStatus {

        // Assuming 2 bytes at most, max of 65536
        int roundUpdated = 0;

        byte symmetry = 0;

        // 12 bits each, 3 bytes together
        MapLocation ourHQ = null;
        MapLocation enemyHQ = null;


        int otherInformationAbbyMightWant = 0;
        int otherInformationAryaMightWant = 0;
        int otherInformationEvanMightWant = 0;

    }


    public static GlobalStatus globalStatus = new GlobalStatus();
    public static final int ROUNDS_VALID = 10;

    private static void updateGlobalStatus(int[] message) {

        if (message[5] + ROUNDS_VALID < rc.getRoundNum()) {
            return;
        }

        globalStatus.roundUpdated = message[5];

        if (globalStatus.ourHQ == null && (message[0] & 0x10000000) != 0) {
            globalStatus.ourHQ = new MapLocation(message[0] & 0b111111, (message[0]>>6) & 0b111111);
        }

        if (globalStatus.enemyHQ == null && (message[0] & 0x20000000) != 0) {
            globalStatus.enemyHQ = new MapLocation((message[0]>>12) & 0b111111, (message[0]>>18) & 0b111111);
        }

        globalStatus.otherInformationAbbyMightWant = message[1];
        globalStatus.otherInformationAryaMightWant = message[2];
        globalStatus.otherInformationEvanMightWant = message[3];

        globalStatus.symmetry = (byte) (message[6] & 0xFF);

    }

    public static void postGlobalStatus() throws GameActionException {
        int locationData = 0;
        if (globalStatus.ourHQ != null) {
            locationData |= 0x10000000;
            locationData |= globalStatus.ourHQ.x & 0b111111;
            locationData |= (globalStatus.ourHQ.y & 0b111111) << 6;
        }

        if (globalStatus.enemyHQ != null) {
            locationData |= 0x20000000;
            locationData |= (globalStatus.enemyHQ.x & 0b111111) << 12;
            locationData |= (globalStatus.enemyHQ.y & 0b111111) << 18;
        }


        int[] message = {locationData,
                globalStatus.otherInformationAbbyMightWant,
                globalStatus.otherInformationAryaMightWant,
                globalStatus.otherInformationEvanMightWant,
                globalStatus.roundUpdated,
                ((int) globalStatus.symmetry) & 0xFF};

        Robot.post(globalStatusHeader, message, false);

    }

    // MID LEVEL COMMUNICATION

    /**
     * Post data to the block chain
     *
     * @param header header
     * @param data byte array of data to encode. Max length of MAX_DATA_BYTES (currently 23)
     * @param isHighPriority true if high priority
     */
    public static void post(byte header, byte[] data, boolean isHighPriority) throws GameActionException {
        int cost = determineCost(isHighPriority);
        int[] encoded = encode(header, data, cost);


        if (rc.canSubmitTransaction(encoded, cost)) {
            rc.submitTransaction(encoded, cost);
        }

        //TODO: Currently fails silently. probably not ideal behavior

    }

    /**
     * Post data to the block chain
     *
     * @param header header
     * @param data int array of data to encode. Max length of MAX_DATA_INTS (currently 6), and most significant byte of data must be zeros.
     * @param isHighPriority true if high priority
     */
    public static void post(byte header, int[] data, boolean isHighPriority) throws GameActionException {
        int cost = determineCost(isHighPriority);

        int[] encoded = encode(header, data, cost);

        if (rc.canSubmitTransaction(encoded, cost)) {
            rc.submitTransaction(encoded, cost);
        }

        //TODO: Currently fails silently. probably not ideal behavior
    }

    // TODO: Find good default here to determine cost for first round
    private static int previousRoundTotalCost = 0;
    private static int lastUpdatedPreviousRoundBlockSet = 0;
    private static Transaction[] previousRoundBlock = new Transaction[0];

    /**
     * Update the previous round block and global status
     */
    public static void blockChainSync() throws GameActionException {

        // In round zero this if statement is purposefully skipped because no block has been released yet
        if (lastUpdatedPreviousRoundBlockSet != rc.getRoundNum()) {
            previousRoundBlock = rc.getBlock(rc.getRoundNum() - 1);
            lastUpdatedPreviousRoundBlockSet = rc.getRoundNum();

            //TODO: Should this code be here or in determineCost – byte code savings depend on how frequently data is posted
            previousRoundTotalCost = 0;
            for (Transaction t : previousRoundBlock) {
                previousRoundTotalCost += t.getCost();

                if (verifyChecksum(t.getMessage(), t.getCost())) {
                    if (decodeHeader(t.getMessage()) == globalStatusHeader) {
                        updateGlobalStatus(t.getMessage());
                    }
                }

            }


        }
    }

    /**
     * Estimate the cost for a transaction to be get in the block chain
     *
     * @param isHighPriority true if high priority
     * @return the approximate cost to get in the block chain
     */
    private static int determineCost(boolean isHighPriority) {
        //TODO: Fix? logic here
        if (previousRoundBlock.length < 7) {
            return 1;
        }

        return isHighPriority ? 1 + (previousRoundTotalCost / 7) : 1 + (previousRoundTotalCost / 7);
    }


    // LOW LEVEL COMMUNICATION

    // The communication code below is not intended to be used by other people. It is low level communication code and I will
    // develop higher level abstractions over next few days.

    // TODO: Meet with group to discuss messaging requirements
    // TODO: Determine standardized header system

    // Int width in bytes
    static final int INT_WIDTH = 4;

    // Byte width in bits
    static final int BYTE_WIDTH = 8;

    // Max number of bytes in data: (7 * 4) - 4 - 1 = number of bytes per transaction minus 4 (checksum) and 1 (header)
    static final int MAX_DATA_BYTES = 23;

    // Max number of ints in data: 7 - 1 = number of ints per transaction minus 1 (checksum)
    // Note that the most significant byte of the 6th int must be left blank for the header
    static final int MAX_DATA_INTS = 6;

    // Magic number used to verify messages are from our team. Change between each tournament.
    static final int COMM_CHECK = 0x38ede937;
    static final int[] COMM_XOR = {0xd13ed2a2, 0x66e21209, 0x1a218286, 0x7d37cd6b, 0x1a2a127c, 0x05f4bd5b};

    /**
     * Securely encode byte data and a header to fit in a transaction message.
     *
     * Note that header use should be standardized by the team. Format is detailed in implementation.
     *
     * @param header data header. Could include length, intended receiver, etc.
     * @param data byte array of data to encode. Max length of MAX_DATA_BYTES (currently 23)
     * @return transaction message: array of 7 ints
     */
    public static int[] encode(byte header, byte[] data, int cost) {
        int[] message = {0, 0, 0, 0, 0, 0, 0};

        // Data format: 4321 8765 ... HMMM CCCC
        // Where numbers indicate bytes, MMM represents the last possible data bytes, H is the header, and CCCC is the checksum

        // Add message bytes
        for (int i = 0; i < data.length; i++) {
            // Need to do & 0xFF because Java tries to do a signed conversion, setting the leader 3 bytes of the int to FF if the byte is negative
            message[i / INT_WIDTH] |= (((int) data[i]) & 0xFF) << (BYTE_WIDTH * (i % INT_WIDTH));
        }

        // Add header
        message[5] |= header << (BYTE_WIDTH * (INT_WIDTH - 1));

        // Add security features to message
        encrypt(message);
        message[6] = calculateChecksum(message, cost);

        return message;
    }

    /**
     * Securely encode int data to fit in a transaction message. See note!
     * !! The most significant byte of the 6th int must be left blank for the header. !!
     *
     * @param header data header. Could include length, intended receiver, etc.
     * @param data int array of data to encode. Max length of MAX_DATA_INTS (currently 6)
     * @return transaction message: array of 7 ints
     */
    public static int[] encode(byte header, int[] data, int cost) {
        int[] message = {0, 0, 0, 0, 0, 0, 0};

        // Add message ints
        // TODO: see if System.arrayCopy is cheaper
        for (int i = 0; i < data.length; i++) {
            message[i] = data[i];
        }

        // Add header
        message[5] |= header << (BYTE_WIDTH * (INT_WIDTH - 1));

        // Add security features to message
        encrypt(message);
        message[6] = calculateChecksum(message, cost);

        return message;
    }


    private static void encrypt(int[] message) {
        message[0] ^= COMM_XOR[0];
        message[1] ^= COMM_XOR[1];
        message[2] ^= COMM_XOR[2];
        message[3] ^= COMM_XOR[3];
        message[4] ^= COMM_XOR[4];
        message[5] ^= COMM_XOR[5];
    }

    private static void decrypt(int[] message) {
        message[0] ^= COMM_XOR[0];
        message[1] ^= COMM_XOR[1];
        message[2] ^= COMM_XOR[2];
        message[3] ^= COMM_XOR[3];
        message[4] ^= COMM_XOR[4];
        message[5] ^= COMM_XOR[5];
    }

    private static int calculateChecksum(int[] message, int cost) {
        // Have the comm check so that a message of all zeros does not satisfy checksum
        // Multiply so that changing the order of blocks changes the checksum
        int sum = message[0]*3 ^ message[1]*5 ^ message[2]*7 ^ message[3]*11 ^ message[4]*13 ^ message[5]*17 ^ COMM_CHECK;

        // Logic: changing any bit in the message will change two bits in the checksum
        return sum ^ (sum >>> 16) ^ (sum << 16) ^ cost;
    }

    /**
     * Verify that a transaction message is from our team.
     *
     * @param encryptedMessage transaction message (Still encrypted)
     * @return true if message is from our team, false otherwise
     */
    public static boolean verifyChecksum(int[] encryptedMessage, int cost) {
        return encryptedMessage[6] != calculateChecksum(encryptedMessage, cost);
    }

    /**
     * Decode the header from an encrypted transaction message
     *
     * If the message is not from our team, this method won't fail but will be meaningless
     *
     * The intended use case is to look at the header to determine if the message is worth completely decrypting
     *
     * @param encryptedMessage transaction message from our team (MUST STILL BE ENCRYPTED)
     * @return the header
     */
    public static byte decodeHeader(int[] encryptedMessage) {
        return (byte) ((encryptedMessage[5] ^ COMM_XOR[5]) >> (BYTE_WIDTH * (INT_WIDTH - 1)));
    }

    /**
     * Decode the data from a transaction message (must be a message from our team), assuming max length.
     * Message is irrevocably modified.
     * If the message encoded was shorter than MAX_DATA_BYTES, the remaining bytes will be 0.
     *
     * @param message encrypted transaction message from our team. Message is irrevocably modified.
     * @return the decoded data
     */
    public static byte[] decodeByteData(int[] message) {
        return decodeByteData(message, MAX_DATA_BYTES);
    }

    /**
     * Decode the data from a transaction message (must be a message from our team), of a known length.
     * Message is irrevocably modified.
     * If the message encoded was shorter than length, the remaining bytes will be 0.
     *
     * Length could be known from (e.g.) already decoded header information.
     *
     * @param message encrypted transaction message from our team. Message is irrevocably modified.
     * @param length known length of the data to be decoded.
     * @return the decoded data
     */
    public static byte[] decodeByteData(int[] message, int length) {
        byte[] decoded = new byte[length];

        decrypt(message);

        // Decode data encoded with format detailed in encode()
        for (int i = 0; i < length; i++) {
            decoded[i] = (byte) message[i / 4];
            message[i / INT_WIDTH] >>= BYTE_WIDTH;
        }

        return decoded;
    }

    /**
     * Decode the data from a transaction message. Must be a message from our team.
     * If the message encoded was shorter than 6, the remaining ints will be 0.
     *
     * @param message encrypted transaction message from our team
     * @return the decoded data
     */
    public static int[] decodeIntData(int[] message) {
        int[] decoded = new int[MAX_DATA_INTS];

        decrypt(message);

        // TODO: see if System.arrayCopy is cheaper
        for (int i = 0; i < MAX_DATA_INTS; i++) {
            decoded[i] = message[i];
        }

        // Remove header
        decoded[5] &= 0x00FFFFFF;

        return decoded;
    }

}
