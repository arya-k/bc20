package prelim;

import battlecode.common.*;

class AlwaysSafeLolz implements NavSafetyPolicy {
    @Override
    public boolean isSafeToMoveTo(MapLocation loc) {
        return true;
    }
}

class Miner extends Unit {
    NavSafetyPolicy nsp;
    BugNav bn;

    @Override
    public void onAwake() throws GameActionException {
        System.out.println("I'm an Miner!");
        nsp = new AlwaysSafeLolz();
        bn = new BugNav(rc, nsp);
    }

    @Override
    public void onUpdate() throws GameActionException {
        MapLocation target = new MapLocation(0,0);
        if (!rc.getLocation().equals(target) && !bn.isStuck()) {
            bn.goTo(target);
        }
        Clock.yield();
    }
}