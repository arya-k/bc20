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

    public void landscapeAt(MapLocation location, int height) throws GameActionException { // Landscape at a certain point
        if (rc.getDirtCarrying() >= height ||
                (height > RobotType.LANDSCAPER.dirtLimit && rc.getDirtCarrying() == RobotType.LANDSCAPER.dirtLimit)) {

            MapLocation[] adjLocations = arrayOfAdjLocations(location, location.directionTo(rc.getLocation())); //input this into the pathfinder as the point B to test
            if (true) { // travel to location
                for (int i = 0; i < 8; ++i) {
                    if (true) { // travel to point adjLocations[i]
                        break;
                    }
                }
            } else {
                buildPathTo(location);
                for (int i = 0; i < 8; ++i) {
                    if (true) { // travel to point adjLocations[i]
                        break;
                    }
                }
            }
            Direction dir = rc.getLocation().directionTo(location);
            for (int i = 0; i < height; ++i) {
                rc.depositDirt(dir);
            }

            if (height > RobotType.LANDSCAPER.dirtLimit) {
                landscapeAt(location, height - RobotType.LANDSCAPER.dirtLimit);
            }

        } else {
            gatherDirt();
            landscapeAt(location, height);
        }
    }

    public void landscapeAround(MapLocation location, int height) throws GameActionException { // Landscape around a building at point 'location'

    }

    public void landscapeInShape(FastLocSet shape) throws GameActionException { // Landscape in given shape

    }

    public void gatherDirt() throws GameActionException { // Gathers dirt from surroundings, attempting to leave paths intact

    }

    public void buildPathTo(MapLocation location) throws GameActionException { // Builds a path which it and other units can use to get to a certain place
        if (true) { // if landscaper can already reach that location -- using the pathfinding function
            return;
        } else {

        }
    }

    public MapLocation[] arrayOfAdjLocations(MapLocation location, Direction direction) throws GameActionException {
        MapLocation[] adjLocations = new MapLocation[8];
        for (int i = 0; i < 8; ++i) {
            adjLocations[i] = location.add(direction);
            direction.rotateRight(); // not sure if we should rotate left or right first
        }
        return adjLocations;
    }

}
