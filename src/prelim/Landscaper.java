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

    public void buildWallAt(MapLocation location, int height, boolean directlyUnder) throws GameActionException { // Landscape at a certain point
        // WHEN THE LANDSCAPER HAS ENOUGH DIRT OR AS MUCH AS IT CAN HAVE
        if (rc.getDirtCarrying() >= height ||
                (height > RobotType.LANDSCAPER.dirtLimit && rc.getDirtCarrying() == RobotType.LANDSCAPER.dirtLimit)) {

            // BUILD WALL ADJACENT TO LANDSCAPER
            if (!directlyUnder) {
                MapLocation[] adjLocations = arrayOfAdjLocations(location, location.directionTo(rc.getLocation()));
                if (!rc.getLocation().isAdjacentTo(location)) {
                    /*
                        Admittedly, I don't really like how this works right now, but I'm not sure how to tell it to
                        take a step back to an adjacent location from where it was before. As a result, it has to check
                        possibly 8 times to see how it got up to the location it's meaning to place dirt at.

                        There's always the unoptimized route of trying to have the landscaper take the direct path by
                        making its own path, and stopping when it's adjacent to the location, but that's not entirely
                        ideal either.
                     */

                    if (true) { // travel to location
                        moveToAdjacentLocation(adjLocations);
                    } else {
                        if (!rc.getLocation().isAdjacentTo(location)) {
                            buildPathTo(location);
                            moveToAdjacentLocation(adjLocations);
                        }
                    }
                }

                // PLACE AS MUCH DIRT AS POSSIBLE, OR TO THE HEIGHT AT THE LOCATION ADJACENT
                Direction dir = rc.getLocation().directionTo(location);
                for (int i = 0; i < height; ++i) {
                    if (rc.canDepositDirt(dir)) {
                        rc.depositDirt(dir);
                    } else {
                        break;
                    }
                }

                if (height > RobotType.LANDSCAPER.dirtLimit) {
                    buildWallAt(location, height - RobotType.LANDSCAPER.dirtLimit, directlyUnder);
                }
            }

            // BUILD WALL DIRECTLY BENEATH LANDSCAPER
            else {
                if (true)  { // travel to location
                    for (int i = 0; i < height; ++i) {
                        rc.depositDirt(Direction.CENTER);
                    }
                } else {
                    buildPathTo(location);
                    for (int i = 0; i < height; ++i) {
                        rc.depositDirt(Direction.CENTER);
                    }
                }
            }
        }

        // GATHER ENOUGH DIRT TO TRY AGAIN (didn't have enough before)
        else {
            gatherDirt();
            buildWallAt(location, height, directlyUnder);
        }
    }

    public void buildWallAround(MapLocation location, int height) throws GameActionException { // Landscape around a building at point 'location'
        MapLocation[] adjLocations = arrayOfAdjLocations(location, location.directionTo(rc.getLocation()));
        int currHeight = 0;
        while (currHeight < height-3) {
            // in order to avoid the landscaper trying to find the optimal adjacent location, it instead builds directly under itself while going
            // around the location and placing 3 dirt onto the next tile.
            // ***NOTE this does not currently account for the tiles around the location being different elevations***
            for (int i = 0; i < 8; ++i) {
                buildWallAt(adjLocations[i], 3, true);
            }
            currHeight += 3;
        }
        for (int i = 0; i < 8; ++i) {
            buildWallAt(adjLocations[i], height-currHeight, true);
        }
    }

    public void landscapeInShape(FastLocSet shape) throws GameActionException { // Landscape in given shape

    }

    public void digHoleAt(MapLocation location, int height) throws GameActionException { // Digs hole at location

    }

    public void gatherDirt() throws GameActionException { // Gathers dirt from surroundings, attempting to leave paths intact

    }

    public void dumpDirt() throws GameActionException { // Dumps dirt into surroundings, attempting to leave paths intact

    }

    public void buildPathTo(MapLocation location) throws GameActionException { // Builds a DIRECT path which it and other units can use to get to a certain place
        if (true) { // if landscaper can already reach that location -- using the pathfinding function
            return;
        } else {
            while (rc.getLocation() != location) {
                int elevationDiff = rc.senseElevation(rc.getLocation()) - rc.senseElevation(rc.getLocation().add(rc.getLocation().directionTo(location)));
                if (elevationDiff < -3) {
                    digHoleAt(rc.getLocation().add(rc.getLocation().directionTo(location)), elevationDiff - 3);
                } else if (elevationDiff > 3) {
                    buildWallAt(rc.getLocation().add(rc.getLocation().directionTo(location)), elevationDiff + 3, false);
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

    public void moveToAdjacentLocation(MapLocation[] adjLocations) throws GameActionException {
        for (int i = 0; i < 8; ++i) {
            if (rc.canMove(rc.getLocation().directionTo(adjLocations[i]))) {
                rc.move(rc.getLocation().directionTo(adjLocations[i]));
                break;
            }
        }
    }
}
