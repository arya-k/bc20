package prelim;
import battlecode.common.*;

public strictfp class RobotPlayer {
    public static void run(RobotController rc) throws GameActionException {
        Robot robot = null;
        try {
            Robot.init(rc);
            switch (rc.getType()) {
                case HQ:                 robot = new HQ();                 break;
                case MINER:              robot = new Miner();              break;
                case REFINERY:           robot = new Refinery();           break;
                case VAPORATOR:          robot = new Vaporator();          break;
                case DESIGN_SCHOOL:      robot = new DesignSchool();       break;
                case FULFILLMENT_CENTER: robot = new FullfillmentCenter(); break;
                case LANDSCAPER:         robot = new Landscaper();         break;
                case DELIVERY_DRONE:     robot = new DeliveryDrone();      break;
                case NET_GUN:            robot = new NetGun();             break;
            }

            robot.onAwake();
        } catch (Exception e) {
            System.out.println("Exception in " + rc.getType());
            e.printStackTrace();
        }

        //noinspection InfiniteLoopStatement
        while (true) {
            try {
                //noinspection InfiniteLoopStatement
                while(true) {
                    robot.onUpdate();
                }
            } catch (Exception e) {
                System.out.println("Exception in " + rc.getType());
                e.printStackTrace();
            }
        }
    }
}
