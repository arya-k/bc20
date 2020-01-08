package prelim;

import battlecode.common.*;

class FulfillmentCenter extends Building {
    static RobotType spawnType = RobotType.DELIVERY_DRONE;
    static int spawnTick = 0;

    @Override
    public void onAwake() throws GameActionException {
        System.out.println("I'm an FulfillmentCenter!");
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
