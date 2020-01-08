package prelim;

import battlecode.common.*;

class Miner extends Unit {
    @Override
    public void onAwake() throws GameActionException {
        System.out.println("I'm an Miner!");
    }

    @Override
    public void onUpdate() throws GameActionException {
    }
}
