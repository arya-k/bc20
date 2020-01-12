package prelim;

import battlecode.common.*;

class StayAliveSafetyPolicy implements NavSafetyPolicy {
    @Override

    RobotController rc;

    public StayAliveSafetyPolicy(RobotController rc_in) {
        rc = rc_in;
    }


    /**
     * Checks to make sure there isn't flooding or an adjacent drone to that tile
     * @param loc to which the the unit is trying to move
     * @return if it's safe to move to loc or not
     * @throws GameActionException
     */
    public boolean isSafeToMoveTo(MapLocation loc) throws GameActionException {
        if (rc.canSenseLocation(loc)) { // double check the location is near enough

            // check that the tile isn't flooded
            if (rc.senseFlooding(loc)) {
                return false;
            }

            // check that the tile doesn't have a drone next to it
            RobotInfo[] nearRobots = rc.senseNearbyRobots(1, rc.getTeam().opponent());
            for (int i = 0; i < nearRobots.length; ++i) {
                if (nearRobots[i].getType() == RobotType.DELIVERY_DRONE) {
                    return false;
                }
            }

        }
        return true;
    }

}

class Landscaper extends Unit {

    NavSafetyPolicy nsp = new StayAliveSafetyPolicy(rc);
    BugNav bn = new BugNav(rc, nsp);


    @Override
    public void onAwake() throws GameActionException {
        landscaped = new FastLocSet();
        paths = new FastLocSet();
    }

    @Override
    public void onUpdate() throws GameActionException {
    }

    private static FastLocSet landscaped;
    private static FastLocSet paths;

    public void travelTo(MapLocation location) throws GameActionException {
        // travel to location
        while (!BugNav.isStuck() && !rc.getLocation().equals(location)) {
            BugNav.goTo(location);
            Clock.yield();
        }

        if (!rc.getLocation().equals(location))  {
            buildPathTo(location);
        }
    }

    public void travelToAdj(MapLocation location) throws GameActionException {
        while (!BugNav.isStuck() && !rc.getLocation().isAdjacentTo(location)) {
            BugNav.goTo(location);
            Clock.yield();
        }

        if (!rc.getLocation().isAdjacentTo(location)) {
            buildPathTo(location);
        }
    }


    /**
     * Builds wall or digs a hole at a certain point
     * If height < 0, it will dig a hole in that location
     * *** height is NOT elevation. landscapeToElevation is the function for that. height is the number of dirt that will
     * be placed or removed from that location. ***
     * @param location of the tile being landscaped
     * @param height of the landscaping
     * @param directlyUnder whether or not the landscaper should stand on top while landscaping
     * @throws GameActionException
     */
    public void landscapeAt(MapLocation location, int height, boolean directlyUnder) throws GameActionException {

        /*
            Adding tiles that we have landscaped intentionally in the past to the landscaped FastLocSet. This is in order to
            avoid gathering or dumping dirt on those areas later. Should maybe set a height limit, as this will take into account
            every single path landscaped, but that also might be good to preserve. Too excessive?
         */
        if (!landscaped.contains(location)) {
            landscaped.add(location);
        }

        // WHEN THE LANDSCAPER HAS ENOUGH DIRT OR AS MUCH AS IT CAN HAVE
        if (height > 0 && (rc.getDirtCarrying() >= height ||
                    (height > RobotType.LANDSCAPER.dirtLimit && rc.getDirtCarrying() == RobotType.LANDSCAPER.dirtLimit)) ||
            height < 0 && (RobotType.LANDSCAPER.dirtLimit - rc.getDirtCarrying() > height ||
                    (height > RobotType.LANDSCAPER.dirtLimit && rc.getDirtCarrying() == 0))) {

            // BUILD WALL ADJACENT TO LANDSCAPER
            if (!directlyUnder) {

                travelToAdj(location);

                // PLACE AS MUCH DIRT AS POSSIBLE, OR TO THE HEIGHT AT THE LOCATION ADJACENT
                Direction dir = rc.getLocation().directionTo(location);
                if (height > 0) {
                    for (int i = 0; i < height; ++i) {
                        if (rc.canDepositDirt(dir)) {
                            rc.depositDirt(dir);
                        } else {
                            break;
                        }
                    }
                } else {
                    for (int i = 0; i < -height; ++i) {
                        if (rc.canDigDirt(dir)) {
                            rc.digDirt(dir);
                        } else {
                            break;
                        }
                    }
                }

                if (height > RobotType.LANDSCAPER.dirtLimit) {
                    landscapeAt(location, height - RobotType.LANDSCAPER.dirtLimit, directlyUnder);
                }
            }

            // BUILD WALL DIRECTLY BENEATH LANDSCAPER
            else {

                travelTo(location);

                if (height > 0) {
                    for (int i = 0; i < height; ++i) {
                        rc.depositDirt(Direction.CENTER);
                    }
                } else {
                    for (int i = 0; i < -height; ++i) {
                        rc.digDirt(Direction.CENTER);
                    }
                }
            }
        }

        // GATHER ENOUGH DIRT OR DUMP DIRT TO TRY AGAIN (didn't have enough (space) before)
        else if (height != 0) {
            if (height > 0) {
                gatherDirt();
            } else {
                dumpDirt();
            }
            landscapeAt(location, height, directlyUnder);
        }
    }



    /**
     * Landscapes to the proper elevation by first searching for the elevation of the intended block or traveling until it can see it,
     * then building up to the point where it is at the right elevation.
     * @param location of the tile being landscaped
     * @param elevation of the tile in the end
     * @param directlyUnder whether or not the landscaper should stand on top of the tile while landscaping or not
     * @throws GameActionException
     */
    public void landscapeToElevation(MapLocation location, int elevation, boolean directlyUnder) throws GameActionException {
        if (!rc.canSenseLocation(location)) {
            if (directlyUnder) {
                travelTo(location);

            } else {
                travelToAdj(location);
            }
        }

        int elevationDiff = elevation - rc.senseElevation(location); // positive if intended elevation is higher than curr elevation
        landscapeAt(location, elevationDiff, directlyUnder);
    }


    /**
     * Landscapes around a building at point 'location' to a certain elevation
     * @param location of building (typically) being built around
     * @param elevation of the walls
     * @throws GameActionException
     */
    public void buildWallAround(MapLocation location, int elevation) throws GameActionException {
        FastLocSet locs = arrayOfAdjLocations(location, location.directionTo(rc.getLocation()));
        MapLocation[] adjLocations = locs.getKeys();

        // first levels the area so that it's all consistently at the same elevation, speeding up the next process
        levelArea(locs);
        int height = elevation - rc.senseElevation(adjLocations[0]);
        int currHeight = 0;
        while (currHeight < height-3) {
            // in order to avoid the landscaper trying to find the optimal adjacent location, it instead builds directly under itself while going
            // around the location and placing 3 dirt onto the next tile.

            for (int i = 0; i < 8; ++i) {
                landscapeAt(adjLocations[i], 3, true);
            }
            currHeight += 3;
        }
        for (int i = 0; i < 8; ++i) {
            landscapeAt(adjLocations[i], height-currHeight, true);
        }
    }


    /**
     * Landscapes in a shape given a bunch of locations and elevations by selecting the closest location and landscaping that first, etc.
     * @param shape FastLocSet of locations that are included in the shape
     * @param elevation of the ditch/wall
     * @throws GameActionException
     */
    public void landscapeInShape(FastLocSet shape, int elevation) throws GameActionException { // Landscape in given shape
//        MapLocation[] locs = shape.getKeys();
//        for (int i = 0; i < shape.getSize(); ++i) {
//
//        }
    }

    public void gatherDirt() throws GameActionException { // Gathers dirt from surroundings, attempting to leave paths intact
        //it would be best to take this dirt from the really deep water, as there's no way we'll want to fill that up anyway
        // but how to get there?

    }

    public void dumpDirt() throws GameActionException { // Dumps dirt into surroundings, attempting to leave paths intact

    }


    /**
     * Flattens all dirt to the same as the first location in a FastLocSet
     * @param shape FastLocSet of locations that will be flattened
     * @throws GameActionException
     */
    public void levelArea(FastLocSet shape) throws GameActionException {
        MapLocation[] locs = shape.getKeys();
        if (!rc.canSenseLocation(locs[0])) {
            travelToAdj(locs[0]);
        }
        int elevation = rc.senseElevation(locs[0]);
        for (int i = 0; i < shape.getSize(); ++i) {
            landscapeToElevation(locs[i], elevation, false);
        }
    }


    /**
     * Builds a DIRECT path which it and other units can use to get to a certain place
     *
     * it might be good to find the optimal path, but i'm not currently sure how to do that.
     * maybe test the left and right elevations also to see what they're like?
     * @param location of the tile the landscaper is building towards
     * @throws GameActionException
     */
    public void buildPathTo(MapLocation location) throws GameActionException {
        while (!BugNav.isStuck() && !rc.getLocation().equals(location)) {
            BugNav.goTo(location);
            Clock.yield();
        }

        if (!rc.getLocation().equals(location))  {
            while (!rc.getLocation().equals(location)) {
                paths.add(rc.getLocation());
                // positive if the following tile is higher than the current tile
                int elevationDiff = rc.senseElevation(rc.getLocation().add(rc.getLocation().directionTo(location))) - rc.senseElevation(rc.getLocation());

                //make sure that the landscaper won't run straight into water
                if (!rc.senseFlooding(rc.getLocation().add(rc.getLocation().directionTo(location)))) {
                    if (elevationDiff < -3) {
                        landscapeAt(rc.getLocation().add(rc.getLocation().directionTo(location)), elevationDiff + 3, false);
                    } else if (elevationDiff > 3) {
                        landscapeAt(rc.getLocation().add(rc.getLocation().directionTo(location)), elevationDiff - 3, false);
                    }
                    if (rc.canMove(rc.getLocation().directionTo(location))) {
                        rc.move(rc.getLocation().directionTo(location));
                    }
                }
                else {
                    landscapeAt(rc.getLocation().add(rc.getLocation().directionTo(location)), elevationDiff - 3, false);
                }
            }
        }
    }


    /**
     * Creates a FastLocSet of adjacent locations to a certain point
     * @param location of the tile the locations are centered around
     * @param direction that it'll start in
     * @return the FastLocSet of locations
     * @throws GameActionException
     */
    public FastLocSet arrayOfAdjLocations(MapLocation location, Direction direction) throws GameActionException {
        // Returns an FastLocSet of MapLocations surrounding a certain point
        FastLocSet adjLocations = new FastLocSet();
        for (int i = 0; i < 8; ++i) {
            adjLocations.add(location.add(direction));
            direction.rotateRight(); // not sure if we should rotate left or right
        }
        return adjLocations;
    }
}
