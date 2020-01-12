package prelim;

import battlecode.common.*;
import com.sun.javafx.collections.MapListenerHelper;

/**
 * Required to use BugNav.goTo() or Explore.go()
 * Defines a function that tells the pathfinding
 * whether it is safe to visit a certain location.
 */
interface NavSafetyPolicy {
    public boolean isSafeToMoveTo(MapLocation loc) throws GameActionException;
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
 *  NavSafetyPolicy nsp = new AvoidEnemiesInclHQSafetyPolicy(rc, enemy_hq_loc);
 *  BugNav bugNav = new BugNav(rc, nsp);
 *  while (!rc.getLocation().equals(target) && !bn.isStuck()) {
 *      bugNav.goTo(target);
 *      Clock.yield();
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
    private static int bugRotationCount; // number of rotations we have made around the obstacle
    private static int bugMovesSinceSeenObstacle; // to determine when to stop bugging
    private static FastLocSet flippoints = new FastLocSet(); // set of locations where bug flipped direction
    public static boolean stuck = false; // whether we are stuck

    public BugNav(RobotController rc, NavSafetyPolicy safety) {
        BugNav.rc = rc;
        BugNav.safety = safety;
    }

    public static boolean isStuck() {
        return stuck;
    }

    /**
     * Try to move towards a target destination.
     * @param destIn target destination
     * @throws GameActionException
     */
    public static void goTo(MapLocation destIn) throws GameActionException {
        if (!rc.isReady() || rc.getLocation().equals(destIn)) {
            return; // Robot can't do any actions this turn OR we have arrived.
        }

        if (!destIn.equals(dest)) {
            dest = destIn; // new target
            bugState = BugState.DIRECT;
        }

        bugMove();
    }

    /**
     * Larger logic for bugfinding algorithm- move towards target
     * bugging if necessary
     * @return whether the action was successful.
     * @throws GameActionException
     */
    private static void bugMove() throws GameActionException {
        if (bugState == BugState.BUG) {
            if (canEndBug()) {
                bugState = BugState.DIRECT;
            }
        }

        // If DIRECT mode, try to go directly to target
        if (bugState == BugState.DIRECT) {
            if (!tryMoveDirect()) {
                bugState = BugState.BUG;
                startBug();
            }
        }

        // If that failed, or if bugging, bug
        if (bugState == BugState.BUG) {
            bugTurn();
        }
    }

    /**
     * Determines when we no longer need to bug around an object
     * @return whether to stop bugging
     */
    private static boolean canEndBug() {
        if (bugMovesSinceSeenObstacle >= 4) return true;
        return (bugRotationCount <= 0 || bugRotationCount >= 8) &&
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
    private static boolean canMove(Direction dir) throws GameActionException {
        return rc.canMove(dir) && safety.isSafeToMoveTo(rc.getLocation().add(dir));
    }

    /**
     * Sets up the useful information for the bug pathfinding-
     * keeps track of distance to target, direction we've moved
     * in, whether we've rotated multiple times, etc...
     */
    private static void startBug() throws GameActionException {
        bugStartDistSq = rc.getLocation().distanceSquaredTo(dest);
        bugLastMoveDir = rc.getLocation().directionTo(dest);
        bugLookStartDir = rc.getLocation().directionTo(dest);
        bugRotationCount = 0;
        bugMovesSinceSeenObstacle = 0;

        // try to intelligently choose on which side we will keep the wall
        Direction leftTryDir = bugLastMoveDir.rotateLeft();
        for (int i = 0; i < 3; i++) {
            if (!canMove(leftTryDir)) leftTryDir = leftTryDir.rotateLeft();
            else break;
        }
        Direction rightTryDir = bugLastMoveDir.rotateRight();
        for (int i = 0; i < 3; i++) {
            if (!canMove(rightTryDir)) rightTryDir = rightTryDir.rotateRight();
            else break;
        }
        if (dest.distanceSquaredTo(rc.getLocation().add(leftTryDir)) < dest.distanceSquaredTo(rc.getLocation().add(rightTryDir))) {
            bugWallSide = WallSide.RIGHT;
        } else {
            bugWallSide = WallSide.LEFT;
        }
    }

    /**
     * Try and take one step using big movement, reversing direction if you hit the edge of the map.
     * @throws GameActionException
     */
    private static void bugTurn() throws GameActionException {
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
            return outOfBounds(rc.getLocation().add(bugLastMoveDir.rotateLeft()));
        } else {
            return outOfBounds(rc.getLocation().add(bugLastMoveDir.rotateRight()));
        }
    }

    /**
     * Whether the map location is out of the bounds of the world
     * @param loc the location to check
     * @return true if the location is out of bounds, false otherwise.
     */
    private static boolean outOfBounds(MapLocation loc) {
        return (loc.x < 0) || (loc.y < 0) || (loc.x >= rc.getMapWidth()) || (loc.y >= rc.getMapHeight());
    }

    /**
     * Reverse directions because we have hit the map
     * involves resetting the bug distances.
     */
    private static void reverseBugWallFollowDir() throws GameActionException {
        bugWallSide = (bugWallSide == WallSide.LEFT ? WallSide.RIGHT : WallSide.LEFT);
        stuck = flippoints.contains(rc.getLocation());
        flippoints.add(rc.getLocation());
        rc.setIndicatorDot(rc.getLocation(), 255,0, 0);
        startBug();
    }

    /**
     * Determines a direction the bug can move in to continue following
     * the wall in the proper direction
     * @return the direction to move in
     */
    public static Direction findBugMoveDir() throws GameActionException {
        bugMovesSinceSeenObstacle++;
        Direction dir = bugLookStartDir;
        for (int i = 8; i-- > 0;) {
            if (canMove(dir)) return dir;
            dir = (bugWallSide == WallSide.LEFT ? dir.rotateRight() : dir.rotateLeft());
            bugMovesSinceSeenObstacle = 0;
        }
        return null;
    }

    /**
     * Move bug in a specific direction,and update the info on the most recent moves.
     * @param dir direction we have decided to move in
     * @throws GameActionException could be thrown
     */
    public static void bugMoveInDir(Direction dir) throws GameActionException{
        rc.move(dir);
        bugRotationCount += calculateBugRotation(dir);
        bugLastMoveDir = dir;
        if (bugWallSide == WallSide.LEFT) {
            bugLookStartDir = dir.rotateLeft().rotateLeft();
        } else {
            bugLookStartDir = dir.rotateRight().rotateRight();
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
        return (end.ordinal() - start.ordinal() + 8) % 8;
    }

    /**
     * Number of rotations to the left moving from start to end
     * @param start direction started in
     * @param end direction ended up with
     * @return numLeftRotations from start to end
     */
    public static int numLeftRotations(Direction start, Direction end) {
        return (-end.ordinal() + start.ordinal() + 8) % 8;
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

         // we have to memoize where the HQ is once you find it, so that you can attempt to path around it,
         // because as of right now if you come within range of their HQ the drone will just freeze.

    return true;
}
 */