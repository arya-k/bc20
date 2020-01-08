package prelim;

import battlecode.common.*;

class Landscaper extends Unit {
    @Override
    public void onAwake() throws GameActionException {
        System.out.println("I'm an Landscaper!");
    }

    @Override
    public void onUpdate() throws GameActionException {
    }
}
