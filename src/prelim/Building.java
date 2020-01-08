package prelim;

import battlecode.common.*;

public abstract class Building extends Robot {
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
     * Determines the robot to shoot
     * @return the id of the robot to shoot or -1 if none is possible
     * @throws GameActionException
     */
    public static int robotToShoot() throws GameActionException {
        // -1 means use full sense radius (do not limit)
        RobotInfo[] robots = rc.senseNearbyRobots(-1, enemy);

        int closest = -1;
        int closestDist = 1 << 30;
        for(int i = 0; i<robots.length; i++) {
            int dist = spawnPos.distanceSquaredTo(robots[i].getLocation());
            if(dist < closestDist) {
                closest = i;
                closestDist = dist;
            }
        }
        return closest;
    }

    /**
     * Determines the direction to spawn a new unit
     * @return the direction to spawn or null if none is possible
     * @throws GameActionException
     */
    public static Direction getSpawnDirection(RobotType spawnType) throws GameActionException {
        Direction[] all = Direction.allDirections();
        for(Direction dir: all) {
            if(rc.canBuildRobot(spawnType, dir)) return dir;
        }
        return null;
    }
}
