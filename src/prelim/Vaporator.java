package prelim;

import battlecode.common.*;

class Vaporator extends Building {
    @Override
    public void onAwake() throws GameActionException {
        System.out.println("I'm an Vaporator!");
    }

    @Override
    public void onUpdate() throws GameActionException {
    }
}
