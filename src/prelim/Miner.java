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

        MapLocation target = new MapLocation(36,29);
        while (!rc.getLocation().equals(target) && !bn.isStuck()) {
            bn.goTo(target);
            Clock.yield();
        }

        while (!rc.canBuildRobot(RobotType.DESIGN_SCHOOL, Direction.SOUTH)) {
            Clock.yield();
        }
        rc.buildRobot(RobotType.DESIGN_SCHOOL, Direction.SOUTH);

        target = new MapLocation(38,32);
        while (!rc.getLocation().equals(target) && !bn.isStuck()) {
            bn.goTo(target);
            Clock.yield();
        }


    }

    @Override
    public void onUpdate() throws GameActionException {

    }
}