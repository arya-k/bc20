package prelim;

import battlecode.common.*;

class DeliveryDrone extends Unit {
    @Override
    public void onAwake() throws GameActionException {
        System.out.println("I'm an DeliveryDrone!");
    }

    @Override
    public void onUpdate() throws GameActionException {
    }
}
