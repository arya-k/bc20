package prelim;

import battlecode.common.*;

/**
 * Required to use BugNav.goTo() or Explore.go()
 * Defines a function that tells the pathfinding
 * whether it is safe to visit a certain location.
 */
interface NavSafetyPolicy {
    public boolean isSafeToMoveTo(MapLocation loc);
}

/**
 *  Implements Bug Pathfinding for both air and ground units.
 *  Must be instantiated with some form of avoidance policy,
 *  which will then be taken into account.
 *
 *  Heavily inspired by https://github.com/TheDuck314/battlecode2015/blob/master/teams/zephyr26_final/Nav.java
 *
 *  Example Usage:
 *  MapLocation target = new MapLocation(0,0);
 *  NavSafetyPolicy nsp = new AvoidAllUnitsSafetyPolicy(enemy_hq_loc);
 *  while (BugNav.goTo(target, nsp)) {
 *      rc.yield(); // BugNav.goTo returns true when an action was taken.
 *  }
 *  /* you will either be at the destination, or bug will have failed
 */
public class BugNav {
    private static MapLocation dest;
    private static NavSafetyPolicy safety;
    private static RobotController rc;


    private enum BugState { DIRECT, BUG }
    private enum WallSide { LEFT, RIGHT }

    private static BugState bugState; // whether bugging or moving directly
    private static WallSide bugWallSide; // whether trying right or left of wall
    private static int bugStartDistSq; // starting distance to target
    private static Direction bugLastMoveDir; // direction last moved in
    private static Direction bugLookStartDir; // direction from start to target
    private static int bugRotationCount; // TODO: what is this
    private static int bugMovesSinceSeenObstacle; // to determine when to stop bugging

    public BugNav(RobotController rc) {
        BugNav.rc = rc;
    }

    /**
     * Try to move towards a target destination.
     * @param destIn target destination
     * @param safetyIn safety policy to take
     * @throws GameActionException
     */
    public static void goTo(MapLocation destIn, NavSafetyPolicy safetyIn) throws GameActionException {
        if (!rc.isReady()) {
            return; // Robot can't do any actions this turn.
        }

        if (!destIn.equals(dest)) {
            dest = destIn; // new target
            bugState = BugState.DIRECT;
        }

        if (rc.getLocation().equals(destIn)) {
            return; // we have arrived- no more actions need to be taken.
        }

        safety = safetyIn;

        /* TODO: SMARTER BUGGING ENDING HERE */
        bugMove();
    }

    /**
     * Larger logic for bugfinding algorithm- move towards target
     * bugging if necessary
     * @return whether the action was successful.
     * @throws GameActionException
     */
    private static void bugMove() throws GameActionException {
        // try to stop bugging if you can
        if (bugState == BugState.BUG) {
            if (canEndBug()) {
                bugState = BugState.DIRECT;
            }
        }

        // try to move towards target
        if (bugState == BugState.DIRECT) {
            if (!tryMoveDirect()) { // we have to bug
                bugState = BugState.BUG;
                startBug(); // we will have to bug around the obstacle
            } else {
                return; // we moved towards the target
            }
        }

        // try to bug
        if (bugState == BugState.BUG) {
            BugTurn();
        }
    }

    /**
     * Determines when we no longer need to bug around an object
     * @return whether to stop bugging
     */
    private static boolean canEndBug() {
        if (bugMovesSinceSeenObstacle >= 4) return true;
        return (bugRotationCount <= 0 || bugRotationCount >=8) &&
                rc.getLocation().distanceSquaredTo(dest) <= bugStartDistSq;
    }

    /**
     * Moving directly towards the target, or one to the left and right
     * if necessary
     * @return whether a move was successfully made.
     * @throws GameActionException hopefully never
     */
    private static boolean tryMoveDirect() throws GameActionException{
        Direction toDest = rc.getLocation().directionTo(dest);

        if (canMove(toDest)) {
            rc.move(toDest);
            return true;
        }

        Direction[] dirs = new Direction[2];
        Direction dirLeft = toDest.rotateLeft();
        Direction dirRight = toDest.rotateRight();

        if (rc.getLocation().add(dirLeft).distanceSquaredTo(dest) <
                rc.getLocation().add(dirRight).distanceSquaredTo(dest)) {
            dirs[0] = dirLeft;
            dirs[1] = dirRight;
        } else {
            dirs[0] = dirRight;
            dirs[1] = dirLeft;
        }

        for (Direction dir : dirs) {
            if (canMove(dir)) {
                rc.move(dir);
                return true;
            }
        }
        return false;
    }

    /**
     * Whether it is safe to move in a given direction
     * @param dir the direction to move in
     * @return safe (yes/no)
     */
    private static boolean canMove(Direction dir) {
        return rc.canMove(dir) && safety.isSafeToMoveTo(rc.getLocation().add(dir));
    }

    /**
     * Sets up the useful information for the bug pathfinding-
     * keeps track of distance to target, direction we've moved
     * in, whether we've rotated multiple times, etc...
     */
    private static void startBug() {
        bugStartDistSq = rc.getLocation().distanceSquaredTo(dest);
        bugLastMoveDir = rc.getLocation().directionTo(dest);
        bugLookStartDir = rc.getLocation().directionTo(dest);
        bugRotationCount = 0;
        bugMovesSinceSeenObstacle = 0;

        // try to intelligently choose on which side we will keep the wall
        Direction leftTryDir = bugLastMoveDir.rotateLeft();
        for (int i = 0; i < 3; i++) {
            if (!canMove(leftTryDir)) {
                leftTryDir.rotateLeft();
            } else {
                break;
            }
        }

        Direction rightTryDir = bugLastMoveDir.rotateRight();
        for (int i = 0; i < 3; i++) {
            if (!canMove(rightTryDir)) {
                rightTryDir.rotateRight();
            } else {
                break;
            }
        }

        if (dest.distanceSquaredTo(rc.getLocation().add(leftTryDir)) <
            dest.distanceSquaredTo(rc.getLocation().add(rightTryDir))) {
            bugWallSide = WallSide.RIGHT;
        } else {
            bugWallSide = WallSide.LEFT;
        }
    }

    private static void BugTurn() throws GameActionException {
        if (detectBugIntoEdge()) {
            reverseBugWallFollowDir();
        }
        Direction dir = findBugMoveDir();
        if (dir != null) {
            bugMoveInDir(dir);
        }
    }

    /**
     * Determines when we have hit the edge of the map
     * @return whether we will hit the side of the map
     * @throws GameActionException hopefully never
     */
    private static boolean detectBugIntoEdge() throws GameActionException {
        if (bugWallSide == WallSide.LEFT) {
            return rc.onTheMap(rc.getLocation().add(bugLastMoveDir.rotateLeft()));
        } else {
            return rc.onTheMap(rc.getLocation().add(bugLastMoveDir.rotateRight()));
        }
    }

    /**
     * Reverse directions because we have hit the map
     * involves resetting the bug distances.
     */
    private static void reverseBugWallFollowDir() {
        bugWallSide = (bugWallSide == WallSide.LEFT ? WallSide.RIGHT : WallSide.LEFT);
        startBug();
    }

    /**
     * Determines a direction the bug can move in to continue following
     * the wall in the proper direction
     * @return the direction to move in
     */
    public static Direction findBugMoveDir() {
        bugMovesSinceSeenObstacle++;
        Direction dir = bugLookStartDir;
        for (int i = 8; i-- > 0;) { // #bytecodeOptimized :)
            if (canMove(dir)) {
                return dir;
            }
            dir = (bugWallSide == WallSide.LEFT ? dir.rotateLeft() : dir.rotateRight());
            bugMovesSinceSeenObstacle = 0;
        }
        return null;
    }

    public static void bugMoveInDir(Direction dir) throws GameActionException{
        rc.move(dir);
        bugRotationCount += calculateBugRotation(dir);
        bugLastMoveDir = dir;
        if (bugWallSide == WallSide.LEFT) {
            bugLookStartDir = dir.rotateLeft().rotateLeft();
        } else {
            bugLookStartDir.rotateRight().rotateRight();
        }
    }

    public static int calculateBugRotation(Direction moveDir) {
        if (bugWallSide == WallSide.LEFT) {
            return numRightRotations(bugLookStartDir, moveDir) -
                    numRightRotations(bugLookStartDir, bugLastMoveDir);
        } else {
            return numLeftRotations(bugLookStartDir, moveDir) -
                    numLeftRotations(bugLookStartDir, bugLastMoveDir);
        }
    }

    /**
     * Number of rotations to the right moving from start to end
     * @param start direction started in
     * @param end direction ended up with
     * @return numRightRotations from start to end
     */
    public static int numRightRotations(Direction start, Direction end) {
        return (dirToInt(end) - dirToInt(start) + 8) % 8;
    }

    /**
     * Number of rotations to the left moving from start to end
     * @param start direction started in
     * @param end direction ended up with
     * @return numLeftRotations from start to end
     */
    public static int numLeftRotations(Direction start, Direction end) {
        return (dirToInt(start) - dirToInt(end) + 8) % 8;
    }

    /**
     * Convert from direction to integer, in ccw direction.
     * @param dir
     * @return
     */
    static int dirToInt(Direction dir) {
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
}


/*
private boolean shouldMove(Direction dir) throws GameActionException {
    if (!rc.canMove(dir)) return false;

    MapLocation desiredLoc = rc.getLocation().add(dir);
    for (RobotInfo r: rc.senseNearbyRobots()) {
        if (r.getTeam() == enemy && r.getType().canShoot()) {
            if (desiredLoc.distanceSquaredTo(r.getLocation()) < r.getType().sensorRadiusSquared) {
                return false; // note that this will return false very easily for an HQ
            }
        }
    }

         // TODO: memoize where the HQ is once you find it, so that you can attempt to path around it,
         // because as of right now if you come within range of their HQ the drone will just freeze.

    return true;
}
 */