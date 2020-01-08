package prelim;

import battlecode.common.*;

class Landscaper extends Robot {
    @Override
    public void onAwake() throws GameActionException {
        System.out.println("I'm an Landscaper!");
    }

    @Override
    public void onUpdate() throws GameActionException {
    }

    public void landscapeAt(MapLocation location, int height) throws GameActionException { // Landscape at a certain point

    }

    public void landscapeAround(MapLocation location, int height) throws GameActionException { // Landscape around a building at point 'location'

    }

    public void landscapeInShape() throws GameActionException { // Landscape in given shape

    }

}
