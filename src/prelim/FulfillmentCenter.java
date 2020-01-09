package prelim;

import battlecode.common.*;

class FulfillmentCenter extends Building {
    static RobotType spawnType = RobotType.DELIVERY_DRONE;
    static int spawnTick = 0;

    @Override
    public void onAwake() throws GameActionException {
        //soup, pollution, flooding, elevation, accessability, friendly x4, enemy x4, net gun danger
        spawnWeights = new int[]{0,0,0,0,0,0,0,1,1,0,0,-10000};
    }

    @Override
    public void onUpdate() throws GameActionException {
        if(shouldSpawn()) trySpawn();
        Clock.yield();
    }

    private boolean shouldSpawn() throws GameActionException {
        if(rc.getTeamSoup() < DRONE_SOUP_THRESHOLD) return false;
        spawnTick++;
        return ((spawnTick %= DRONE_RATE) == 0);
    }

    private void trySpawn() throws GameActionException {
        Direction dir = getSpawnDirection(spawnType);
        if(dir != null) rc.buildRobot(spawnType, dir);
    }
}
