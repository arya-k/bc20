package prelim;
import battlecode.common.*;

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
     * Computes the absolute value between two integers
     * @param a first number
     * @param b second number
     * @return abs(a-b)
     */
    static int abs(int a, int b) {
        return a < b ? b-a : a-b;
    }
}
