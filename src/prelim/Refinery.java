package prelim;

import battlecode.common.*;

class Refinery extends Building {
    @Override
    public void onAwake() throws GameActionException {
        System.out.println("I'm an Refinery!");
    }

    @Override
    public void onUpdate() throws GameActionException {
    }
}
