package prelim;

import battlecode.common.*;

class FulfillmentCenter extends Robot {
    static int unitTick = 0;

    @Override
    public void onAwake() throws GameActionException {
        System.out.println("I'm an FulfillmentCenter!");
    }

    @Override
    public void onUpdate() throws GameActionException {
        if(shouldSpawn()) trySpawn();
    }

    private boolean shouldSpawn() throws GameActionException {
        if(rc.getTeamSoup() > DRONE_SOUP_THRESHOLD) unitTick++;
        return ((unitTick %= DRONE_RATE) == 0);
    }

    private void trySpawn() throws GameActionException {
        Direction dir = getSpawnDirection();
        if(dir != null) rc.buildRobot(RobotType.DELIVERY_DRONE, dir);
    }
}
