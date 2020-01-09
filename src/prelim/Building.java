package prelim;

import battlecode.common.*;

public abstract class Building extends Robot {
    private static int[][] info;
    static int[] spawnWeights;

    /**
     * Determines the robot to shoot
     * @return the id of the robot to shoot or -1 if none is possible
     */
    public static int robotToShoot() {
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

    private static int directionToInt(Direction dir) {
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

    private static int getAccessible(MapLocation loc) throws GameActionException {
        int transfers = 0;
        int elev = rc.senseElevation(loc);
        for(Direction dir: Direction.allDirections()) {
            MapLocation cur = loc.add(dir);
            if(rc.canSenseLocation(cur) && abs(elev, rc.senseElevation(cur)) <= GameConstants.MAX_DIRT_DIFFERENCE) {
                transfers += 1;
            }
        }
        return transfers;
    }

    private static int getIndexOfRobot(RobotInfo robot) {
        int i = rc.getTeam() == ally ? 0 : 4; // skip by 4 in array if enemy
        switch (robot.getType()) {
            case MINER: return i + 5;
            case LANDSCAPER: return i + 6;
            case DELIVERY_DRONE: return i + 7;
            case COW: return 13;
            default: return i + 8;
        }
    }

    private static boolean checkNetGunDanger(RobotInfo robot, Direction dir, MapLocation loc) throws GameActionException {
        MapLocation check = robot.getLocation().add(dir);
        int dist = check.distanceSquaredTo(loc);
        int pollution = rc.sensePollution(loc);
        int maxDist = GameConstants.NET_GUN_SHOOT_RADIUS_SQUARED + 1/(1 + pollution)/(1 + pollution);
        return dist <= maxDist;
    }

    public static void scanArea() throws GameActionException {
        // Each of the 8 directions has 14 data points associated
        info = new int[8][INFO_AMT];
        int r = (int)Math.sqrt(rc.getCurrentSensorRadiusSquared());
        // Iterate over the entire square, not just the circle
        for(int x = -r; x <= r; x++) {
            for(int y = -r; y <= r; y++) {
                MapLocation loc = spawnPos.translate(x, y);
                if(rc.canSenseLocation(loc)) {
                    Direction dir = spawnPos.directionTo(loc);
                    int d = directionToInt(dir);
                    // Collect basic data
                    info[d][0] += rc.senseSoup(loc);
                    info[d][1] += rc.sensePollution(loc);
                    info[d][2] += rc.senseFlooding(loc) ? 1 : 0; // java is dumb
                    info[d][3] += rc.senseElevation(loc);
                    info[d][4] += getAccessible(loc);
                    RobotInfo robot = rc.senseRobotAtLocation(loc);
                    // Increment the correct robot count
                    if(robot != null) info[d][getIndexOfRobot(robot)] += 1;
                    // Check 8 spawn possibilities to see if they're in range of the possible net gun here
                    if(robot != null && robot.getType() == RobotType.NET_GUN) {
                        for(Direction check: Direction.allDirections()) {
                            info[directionToInt(check)][13] += checkNetGunDanger(robot, check, loc) ? 1 : 0;
                        }
                    }
                }
            }
        }
    }

    /**
     * Determines the direction to spawn a new unit
     * @return the direction to spawn or null if none is possible
     */
    public static Direction getSpawnDirection(RobotType spawnType) {
        int score = -1 << 30;
        Direction[] all = Direction.allDirections();
        Direction dir = null;
        for(int i = 0; i<8; i++) {
            int cur = 0;
            for(int j = 0; j<INFO_AMT; j++) {
                cur += spawnWeights[j] * info[i][j];
            }
            if(cur > score && rc.canBuildRobot(type, all[i])) {
                dir = all[i];
                score = cur;
            }
        }
        return dir;
    }
}
