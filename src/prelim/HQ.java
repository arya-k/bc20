package prelim;

import battlecode.common.*;

class HQ extends Building {
    static RobotType spawnType = RobotType.MINER;
    static int spawnTick = 0;

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
        Clock.yield();
    }

    private boolean shouldSpawn() throws GameActionException {
        if(rc.getTeamSoup() < MINER_SOUP_THRESHOLD) return false;
        System.out.println(spawnTick);
        spawnTick++;
        return ((spawnTick %= MINER_RATE) == 0);
    }

    private void trySpawn() throws GameActionException {
        Direction dir = getSpawnDirection(spawnType);
        System.out.println(dir);
        if(dir != null) rc.buildRobot(spawnType, dir);
    }
}
