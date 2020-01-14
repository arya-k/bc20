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

        RobotInfo[] nearRobots = rc.senseNearbyRobots(RobotType.LANDSCAPER.sensorRadiusSquared, rc.getTeam());
        MapLocation hqLoc = new MapLocation(0,0);
        for (int i = 0; i < nearRobots.length; ++i) {
            if (nearRobots[i].getType() == RobotType.HQ) {
                hqLoc = nearRobots[i].getLocation();
            }
        }


        MapLocation target = hqLoc.add(Direction.SOUTHWEST);
        while (!rc.getLocation().equals(target) && !bn.isStuck()) {
            bn.goTo(target);
            Clock.yield();
        }

        while (!rc.canBuildRobot(RobotType.DESIGN_SCHOOL, Direction.NORTH)) {
            Clock.yield();
        }
        rc.buildRobot(RobotType.DESIGN_SCHOOL, Direction.NORTH);

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