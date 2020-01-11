package prelim;

import battlecode.common.*;

class AlwaysSafeLolz implements NavSafetyPolicy {
    @Override
    public boolean isSafeToMoveTo(MapLocation loc) {
        return true;
    }
}

class Miner extends Unit {
    NavSafetyPolicy nsp = new AlwaysSafeLolz();
    BugNav bn = new BugNav(rc);

    @Override
    public void onAwake() throws GameActionException {
        System.out.println("I'm an Miner!");
    }

    @Override
    public void onUpdate() throws GameActionException {
        bn.goTo(new MapLocation(0, 0), nsp);
        Clock.yield();
    }
}