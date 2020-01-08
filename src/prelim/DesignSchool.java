package prelim;

import battlecode.common.*;

class DesignSchool extends Building {
    static RobotType spawnType = RobotType.LANDSCAPER;
    static int spawnTick = 0;

    @Override
    public void onAwake() throws GameActionException {
        System.out.println("I'm an DesignSchool!");
    }

    @Override
    public void onUpdate() throws GameActionException {
        if(shouldSpawn()) trySpawn();
        Clock.yield();
    }

    private boolean shouldSpawn() throws GameActionException {
        if(rc.getTeamSoup() < LANDSCAPER_SOUP_THRESHOLD) return false;
        spawnTick++;
        return ((spawnTick %= LANDSCAPER_RATE) == 0);
    }

    private void trySpawn() throws GameActionException {
        Direction dir = getSpawnDirection(spawnType);
        if(dir != null) rc.buildRobot(spawnType, dir);
    }
}
