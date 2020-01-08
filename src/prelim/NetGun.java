package prelim;

import battlecode.common.*;

class NetGun extends Building {
    @Override
    public void onAwake() throws GameActionException {
        System.out.println("I'm an NetGun!");
    }

    @Override
    public void onUpdate() throws GameActionException {
        int victim = robotToShoot();
        if(victim != -1) rc.shootUnit(victim);
        Clock.yield();
    }
}
