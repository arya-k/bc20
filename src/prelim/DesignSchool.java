package prelim;

import battlecode.common.*;

class DesignSchool extends Robot {
    static int unitTick = 0;

    @Override
    public void onAwake() throws GameActionException {
        System.out.println("I'm an DesignSchool!");
    }

    @Override
    public void onUpdate() throws GameActionException {
        if(shouldSpawn()) trySpawn();
    }

    private boolean shouldSpawn() throws GameActionException {
        if(rc.getTeamSoup() > LANDSCAPER_SOUP_THRESHOLD) unitTick++;
        return ((unitTick %= LANDSCAPER_RATE) == 0);
    }

    private void trySpawn() throws GameActionException {
        Direction dir = getSpawnDirection();
        if(dir != null) rc.buildRobot(RobotType.LANDSCAPER, dir);
    }
}
