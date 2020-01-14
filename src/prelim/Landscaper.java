package prelim;

import battlecode.common.*;

class StayAliveSafetyPolicy implements NavSafetyPolicy {
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
    @Override
    public boolean isSafeToMoveTo(MapLocation loc) throws GameActionException {
        if (rc.canSenseLocation(loc)) { // double check the location is near enough

            // check that the tile isn't flooded
            if (rc.senseFlooding(loc)) {
                return false;
            }

            // check that the tile doesn't have a drone next to it
            RobotInfo[] nearRobots = rc.senseNearbyRobots(2, rc.getTeam().opponent());
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
        gatherDump = new FastLocSet();

        //buildHQMoat();

        RobotInfo[] nearRobots = rc.senseNearbyRobots(RobotType.LANDSCAPER.sensorRadiusSquared, rc.getTeam());
        MapLocation hqLoc = new MapLocation(0,0);
        for (int i = 0; i < nearRobots.length; ++i) {
            if (nearRobots[i].getType() == RobotType.HQ) {
                hqLoc = nearRobots[i].getLocation();
                System.out.println("The hq location is here " + hqLoc);
            }
        }

        //landscapeAt(new MapLocation(33, 26), -50, false);
        buildWallAround(hqLoc, 2, 15);
        //landscapeAt(new MapLocation(35, 21), 9, true);
        //buildPathTo(new MapLocation(36, 25));
        //travelTo(new MapLocation(36, 25));

    }

    @Override
    public void onUpdate() throws GameActionException {
    }

    private static FastLocSet landscaped;
    private static FastLocSet gatherDump;

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
        if (height == 0) {
            Clock.yield();
            return;
        }
        System.out.println("inside landscapeAt function");

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
                    (height >= RobotType.LANDSCAPER.dirtLimit && rc.getDirtCarrying() == RobotType.LANDSCAPER.dirtLimit)) ||
            (height < 0 && (RobotType.LANDSCAPER.dirtLimit - rc.getDirtCarrying() > -height ||
                (-height >= RobotType.LANDSCAPER.dirtLimit && rc.getDirtCarrying() == 0)))) {

            Direction dir;

            if (directlyUnder) {
                dir = Direction.CENTER;
                travelTo(location);
            } else {
                dir = rc.getLocation().directionTo(location);
                travelToAdj(location);
            }

            int build = height;

            if (height > 0) {
                if (height > RobotType.LANDSCAPER.dirtLimit) {
                    build = RobotType.LANDSCAPER.dirtLimit;
                }
                for (int i = 0; i < build; ++i) {
                    while (!rc.canDepositDirt(dir)) {
                        Clock.yield();
                    }
                    rc.depositDirt(dir);
                    Clock.yield();
                }
            } else {
                if (-height > RobotType.LANDSCAPER.dirtLimit) {
                    build = -RobotType.LANDSCAPER.dirtLimit;
                }
                for (int i = 0; i < -build; ++i) {
                    while (!rc.canDigDirt(dir)) {
                        Clock.yield();
                    }
                    rc.digDirt(dir);
                    Clock.yield();
                }
            }
        }

        // GATHER ENOUGH DIRT OR DUMP DIRT TO TRY AGAIN (didn't have enough (space) before)
        else {
            if (height > 0) {
                gatherDirt(1);
            } else {
                dumpDirt(1);
            }
            landscapeAt(location, height, directlyUnder);
        }

        if (-height > RobotType.LANDSCAPER.dirtLimit) {
            landscapeAt(location, height + RobotType.LANDSCAPER.dirtLimit, directlyUnder);
        }
        else if (height > RobotType.LANDSCAPER.dirtLimit) {
            landscapeAt(location, height - RobotType.LANDSCAPER.dirtLimit, directlyUnder);
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
        System.out.println("inside landscapetoelevation function");
        if (!rc.canSenseLocation(location)) {
            if (directlyUnder) {
                System.out.println("trying to travel to location");
                travelTo(location);

            } else {
                System.out.println("trying to travel adj to location");
                travelToAdj(location);
            }
        }

        int elevationDiff = elevation - rc.senseElevation(location); // positive if intended elevation is higher than curr elevation
        System.out.println("trying to landscape at location");
        landscapeAt(location, elevationDiff, directlyUnder);
        System.out.println("landscaped");
    }


    /**
     * Landscapes around a building at point 'location' to a certain elevation
     * @param location of building (typically) being built around
     * @param elevation of the walls
     * @throws GameActionException
     */
    public void buildWallAround(MapLocation location, int distanceFromPoint, int elevation) throws GameActionException {
        FastLocSet locs = setOfAdjLocations(location, location.directionTo(rc.getLocation()), distanceFromPoint);
        MapLocation[] adjLocations = locs.getKeys();

        for (int i = 0; i < locs.getSize(); ++i) {
            landscaped.add(adjLocations[i]);
        }
        System.out.println("I found the adjacent locations");

        // first levels the area so that it's all consistently at the same elevation, speeding up the next process
        //levelArea(locs);
        System.out.println("I leveled the area");
        if (!rc.canSenseLocation(adjLocations[0])) {
            travelTo(adjLocations[0]);
        }
        int height = elevation - rc.seseElevation(adjLocations[0]);
        Clock.yield();
        int currHeight = 0;
        while (currHeight < height-3) {
            // in order to avoid the landscaper trying to find the optimal adjacent location, it instead builds directly under itself while going
            // around the location and placing 3 dirt onto the next tile.

            for (int i = 0; i < locs.getSize(); ++i) {
                landscapeAt(adjLocations[i], 3, true);
            }
            currHeight += 3;
        }
        for (int i = 0; i < locs.getSize(); ++i) {
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
        MapLocation[] locs = shape.getKeys();
        MapLocation nearest;
        for (int i = 0; i < shape.getSize(); ++i) {
            nearest = nearestLoc(shape);
            landscapeToElevation(nearest, elevation, false);
        }
    }


    /**
     * Looks for an empty spot on the area around to the HQ, then builds up at that point to 1000 (currently)
     * @throws GameActionException
     */
    public void buildHQMoat() throws GameActionException {
        // not currently sure how to get the hq location, so this is just gonna assume that it's spawned somewhere where it can see the hq

        RobotInfo[] nearRobots = rc.senseNearbyRobots(RobotType.LANDSCAPER.sensorRadiusSquared, rc.getTeam());
        MapLocation hqLoc = new MapLocation(0,0);
        boolean found = false;
        for (int i = 0; i < nearRobots.length; ++i) {
            if (nearRobots[i].getType() == RobotType.HQ) {
                hqLoc = nearRobots[i].getLocation();
                found = true;
            }
        }
        if (!found) {
            return;
        }
        FastLocSet moatLocs = setOfAdjLocations(hqLoc, hqLoc.directionTo(rc.getLocation()), 2);
        MapLocation[] locs = moatLocs.getKeys();
        for (int i = 0; i < moatLocs.getSize(); ++i) {
            landscaped.add(locs[i]);
        }

        for (int i = 0; i < moatLocs.getSize(); ++i) {
            RobotInfo robot = rc.senseRobotAtLocation(locs[i]);
            if (robot == null) {
                // TODO: look at this again and make sure this is the best way to do it
                landscapeAt(locs[i], 1000, true);
            }
        }
    }


    /**
     * Searches all points 'distance' away from curr loc. If all points are landscaped (in the special arrays) try distance + 1
     * @param distance away from the landscaper's position that will be checked
     * @throws GameActionException
     */
    public void gatherDirt(int distance) throws GameActionException { // Gathers dirt from surroundings, attempting to leave paths intact
        /*
        search all points 'distance' away from curr loc. if no locations are found that aren't in the special arrays, try distance + 1
         */

        FastLocSet locs = setOfAdjLocations(rc.getLocation(), Direction.SOUTH, distance);
        MapLocation[] keys = locs.getKeys();

        for (int i = 0; i < locs.getSize(); ++i) {
            if (!landscaped.contains(keys[i])) {
                gatherDump.add(keys[i]);
                landscapeAt(keys[i], -(RobotType.LANDSCAPER.dirtLimit-rc.getDirtCarrying()), false);
                landscaped.remove(keys[i]);
                return;
            }
        }
        gatherDirt(distance + 1);

    }


    /**
     * Searches all points 'distance' away from curr loc. If all points are landscaped (in the special arrays) try distance + 1
     * @param distance away from the landscaper's position that will be checked
     * @throws GameActionException
     */
    public void dumpDirt(int distance) throws GameActionException { // Dumps dirt into surroundings, attempting to leave paths intact
        FastLocSet locs = setOfAdjLocations(rc.getLocation(), Direction.SOUTH, distance);
        MapLocation[] keys = locs.getKeys();

        for (int i = 0; i < locs.getSize(); ++i) {
            if (!landscaped.contains(keys[i])) {
                gatherDump.add(keys[i]);
                landscapeAt(keys[i], rc.getDirtCarrying(), false);
                landscaped.remove(keys[i]);
                return;
            }
        }
        dumpDirt(distance + 1);
    }


    /**
     * Flattens all dirt to the same as the first location in a FastLocSet
     * @param shape FastLocSet of locations that will be flattened
     * @throws GameActionException
     */
    public void levelArea(FastLocSet shape) throws GameActionException {
        System.out.println("inside level area function");
        MapLocation[] locs = shape.getKeys();
        if (!rc.canSenseLocation(locs[0])) {
            travelToAdj(locs[0]);
            System.out.println("traveled to location");
        }
        int elevation = rc.senseElevation(locs[0]);
        for (int i = 0; i < shape.getSize(); ++i) {
            System.out.println("trying to landscape");
            landscapeToElevation(locs[i], elevation, false);
            System.out.println("landscaped to elevation");
        }
    }


    /**
     * Builds a DIRECT path which it and other units can use to get to a certain place
     *
     * it might be good to find the optimal path, but i'm not currently sure how to do that.
     * maybe test the left and right elevations also to see what they're like?
     *
     * @param location of the tile the landscaper is building towards
     * @throws GameActionException
     */
    public void buildPathTo(MapLocation location) throws GameActionException {
//        while (!BugNav.isStuck() && !rc.getLocation().equals(location)) {
//            BugNav.goTo(location);
//            Clock.yield();
//        }


        while (!rc.getLocation().equals(location)) {
            landscaped.add(rc.getLocation());
            // positive if the following tile is higher than the current tile
            int elevationDiff = rc.senseElevation(rc.getLocation().add(rc.getLocation().directionTo(location))) - rc.senseElevation(rc.getLocation());

            //make sure that the landscaper won't run straight into water
            if (!rc.senseFlooding(rc.getLocation().add(rc.getLocation().directionTo(location)))) {
                if (elevationDiff < -3) {
                    landscapeAt(rc.getLocation().add(rc.getLocation().directionTo(location)), -(elevationDiff + 3), false);
                } else if (elevationDiff > 3) {
                    landscapeAt(rc.getLocation().add(rc.getLocation().directionTo(location)), -(elevationDiff - 3), false);
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


    /**
     * Creates a FastLocSet of adjacent locations to a certain point
     * @param location of the tile the locations are centered around
     * @param direction that it'll start in
     * @param distanceFromPoint that the wall should be from location
     * @return the FastLocSet of locations
     * @throws GameActionException
     */
    public FastLocSet setOfAdjLocations(MapLocation location, Direction direction, int distanceFromPoint) throws GameActionException {
        // Returns a FastLocSet of MapLocations surrounding a certain point from a certain distance
        FastLocSet adjLocations = new FastLocSet();
        MapLocation currLoc;

        for (int i = 0; i < 8; ++i) {
            currLoc = location;
            for (int j = 0; j < distanceFromPoint; ++j) {
                currLoc = currLoc.add(direction);
            }
            if (i > 0 && distanceFromPoint > 1) {
                MapLocation midLoc = adjLocations.getKeys()[adjLocations.getSize()-1];
                for (int k = 0; k < distanceFromPoint-1; ++k) {
                    midLoc = midLoc.add(midLoc.directionTo(currLoc));
                    adjLocations.add(midLoc);
                }
            }
            adjLocations.add(currLoc);
            direction = direction.rotateRight(); // not sure if we should rotate left or right
        }
        MapLocation midLoc = adjLocations.getKeys()[adjLocations.getSize()-1];
        for (int k = 0; k < distanceFromPoint-1; ++k) {
            midLoc = midLoc.add(midLoc.directionTo(adjLocations.getKeys()[0]));
            adjLocations.add(midLoc);
        }
        return adjLocations;
    }


    /**
     * computes and returns the nearest location to the current loc
     * @param locs FastLocSet of locations to check
     * @return the nearest location to the current loc
     * @throws GameActionException
     */
    public MapLocation nearestLoc(FastLocSet locs) throws GameActionException {
        MapLocation[] keys = locs.getKeys();

        int nearestDist = keys[0].distanceSquaredTo(rc.getLocation());
        MapLocation nearest = keys[0];

        for (int i = 1; i < locs.getSize(); ++i) {
            if (keys[i].distanceSquaredTo(rc.getLocation()) < nearestDist) {
                nearestDist = keys[i].distanceSquaredTo(rc.getLocation());
                nearest = keys[i];
            }
        }

        return nearest;
    }
}


