package prelim;

import battlecode.common.*;

class Landscaper extends Unit {
    @Override
    public void onAwake() throws GameActionException {
        System.out.println("I'm a Landscaper!");
    }

    @Override
    public void onUpdate() throws GameActionException {
    }

    /*
        Builds wall at a certain point
        if height < 0, it will dig a hole in that location
        *** height is NOT elevation. an elevation function will be written later. height is the number of dirt that will
         be placed or removed from that location. ***
     */
    public void landscapeAt(MapLocation location, int height, boolean directlyUnder) throws GameActionException {
        // WHEN THE LANDSCAPER HAS ENOUGH DIRT OR AS MUCH AS IT CAN HAVE
        if (height > 0 && (rc.getDirtCarrying() >= height ||
                    (height > RobotType.LANDSCAPER.dirtLimit && rc.getDirtCarrying() == RobotType.LANDSCAPER.dirtLimit)) ||
            height < 0 && (RobotType.LANDSCAPER.dirtLimit - rc.getDirtCarrying() > height ||
                    (height > RobotType.LANDSCAPER.dirtLimit && rc.getDirtCarrying() == 0))) {

            // BUILD WALL ADJACENT TO LANDSCAPER
            if (!directlyUnder) {
                if (! true) { // CANNOT travel to adjacent block to the location
                    buildPathTo(location);
                }

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
                    for (int i = 0; i < height; ++i) {
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

            // *** repetitive  code - don't like that ***
            else {
                if (true)  { // travel to location
                    if (height > 0) {
                        for (int i = 0; i < height; ++i) {
                            rc.depositDirt(Direction.CENTER);
                        }
                    } else {
                        for (int i = 0; i < height; ++i) {
                            rc.digDirt(Direction.CENTER);
                        }
                    }
                } else {
                    buildPathTo(location);
                    if (height > 0) {
                        for (int i = 0; i < height; ++i) {
                            rc.depositDirt(Direction.CENTER);
                        }
                    } else {
                        for (int i = 0; i < height; ++i) {
                            rc.digDirt(Direction.CENTER);
                        }
                    }
                }
            }
        }

        else if (height == 0) {
            return;
        }

        // GATHER ENOUGH DIRT TO TRY AGAIN (didn't have enough before)
        else {
            gatherDirt();
            landscapeAt(location, height, directlyUnder);
        }
    }

    // should change this to take in an elevation?
    public void buildWallAround(MapLocation location, int height) throws GameActionException { // Landscape around a building at point 'location'
        MapLocation[] adjLocations = arrayOfAdjLocations(location, location.directionTo(rc.getLocation()));
        int currHeight = 0;
        while (currHeight < height-3) {
            // in order to avoid the landscaper trying to find the optimal adjacent location, it instead builds directly under itself while going
            // around the location and placing 3 dirt onto the next tile.
            // ***NOTE this does not currently account for the tiles around the location being different elevations***
            for (int i = 0; i < 8; ++i) {
                landscapeAt(adjLocations[i], 3, true);
            }
            currHeight += 3;
        }
        for (int i = 0; i < 8; ++i) {
            landscapeAt(adjLocations[i], height-currHeight, true);
        }
    }

    public void landscapeInShape(FastLocSet shape, int elevation) throws GameActionException { // Landscape in given shape
        MapLocation[] locs = shape.getKeys();
        for (int i = 0; i < shape.getSize(); ++i) { // need a get size function :(
            
        }
    }

    public void gatherDirt() throws GameActionException { // Gathers dirt from surroundings, attempting to leave paths intact

    }

    public void dumpDirt() throws GameActionException { // Dumps dirt into surroundings, attempting to leave paths intact

    }

    public void levelArea() throws GameActionException { // Flattens all dirt to the same as the first location in a FastLocSet

    }

    public void buildPathTo(MapLocation location) throws GameActionException { // Builds a DIRECT path which it and other units can use to get to a certain place
        if (true) { // if landscaper can already reach that location -- using the pathfinding function
            return;
        } else {
            while (rc.getLocation() != location) {

                int elevationDiff = rc.senseElevation(rc.getLocation()) - rc.senseElevation(rc.getLocation().add(rc.getLocation().directionTo(location)));
                if (elevationDiff < -3) {
                    landscapeAt(rc.getLocation().add(rc.getLocation().directionTo(location)), -(elevationDiff - 3), false);
                } else if (elevationDiff > 3) {
                    landscapeAt(rc.getLocation().add(rc.getLocation().directionTo(location)), elevationDiff + 3, false);
                }
                rc.move(rc.getLocation().directionTo(location));
            }
        }
    }

    public MapLocation[] arrayOfAdjLocations(MapLocation location, Direction direction) throws GameActionException {
        // Returns an array of MapLocations surrounding a certain point
        MapLocation[] adjLocations = new MapLocation[8];
        for (int i = 0; i < 8; ++i) {
            adjLocations[i] = location.add(direction);
            direction.rotateRight(); // not sure if we should rotate left or right
        }
        return adjLocations;
    }
}
