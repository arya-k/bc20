package prelim;

import battlecode.common.*;

class HQ extends Robot {
    @Override
    public void onAwake() throws GameActionException {
        System.out.println("I'm an HQ!");
    }

    @Override
    public void onUpdate() throws GameActionException {
    }
}
