package prelim;

import battlecode.common.*;

class DesignSchool extends Building {
    static RobotType spawnType = RobotType.LANDSCAPER;
    static int spawnTick = 0;

    @Override
    public void onAwake() throws GameActionException {
        //soup, pollution, flooding, elevation, accessability, friendly x4, enemy x4, net gun danger
        spawnWeights = new int[]{0,-1,-1,0,0,0,0,0,0,0,0,0,0};
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
