package prelim;

import battlecode.common.*;

class HQ extends Robot {
    static int unitTick = 0;

    @Override
    public void onAwake() throws GameActionException {
        //TODO: message position
    }

    @Override
    public void onUpdate() throws GameActionException {
        int id = robotToShoot();
        if(id != -1) {
            rc.shootUnit(id);
        } else {
            if(shouldSpawn()) trySpawn();
        }
    }

    private boolean shouldSpawn() throws GameActionException {
        if(rc.getTeamSoup() > MINER_SOUP_THRESHOLD) unitTick++;
        return ((unitTick %= MINER_RATE) == 0);
    }

    private void trySpawn() throws GameActionException {
        Direction dir = getSpawnDirection();
        if(dir != null) rc.buildRobot(RobotType.MINER, dir);
    }
}
