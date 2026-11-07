package de.vw.mib.asl.internal.androidauto.target;

import de.adi961.miblogger.MIBLogger;
import de.macsinspace.vcaa.Variant;
import de.vw.mib.asl.api.navbap.ASLNavBAPFactory;
import de.vw.mib.bap.mqbab2.common.api.APIFactory;
import de.vw.mib.bap.mqbab2.common.api.navigation.NavigationASLDataAdapter;
import org.dsi.ifc.androidauto2.Constants;
import org.dsi.ifc.navigation.BapManeuverDescriptor;



public class NavigationHandler {
    private final ManeuverDescriptorSender maneuverDescriptorSender;

    private NavTurn nextTurn;

    /*
     * The road name for the cockpit ("towards ..."). With the phone's map on
     * the cockpit, Google Maps draws the same text in its own card, so the
     * turns jar by default sends it only while the cockpit is not showing the
     * map. CLUSTERMAP_TURN_TEXT in gal_dualscreen.conf (written by the
     * installer to TURN_TEXT_FILE): auto, always or never.
     */
    private static final String TURN_TEXT_FILE = "/mnt/app/root/vcaa_turn_text";
    private static final int TEXT_AUTO = 0;
    private static final int TEXT_ALWAYS = 1;
    private static final int TEXT_NEVER = 2;
    private final int textMode;
    private String road = "";
    private String sentRoad;
    private int loggedMapStatus = -1;


    public NavigationHandler() {
        this.maneuverDescriptorSender = new ManeuverDescriptorSender();
        this.textMode = Variant.KEEP_MAP_VIEW ? readTextMode() : TEXT_ALWAYS;
    }

    private static int readTextMode() {
        String v = "";
        java.io.BufferedReader r = null;
        try {
            r = new java.io.BufferedReader(new java.io.FileReader(TURN_TEXT_FILE));
            String line = r.readLine();
            v = line == null ? "" : line.trim();
        } catch (Throwable t) {
            // no file: auto
        } finally {
            try {
                if (r != null) {
                    r.close();
                }
            } catch (Throwable t) {
                // ignore
            }
        }
        int mode = "always".equals(v) ? TEXT_ALWAYS : ("never".equals(v) ? TEXT_NEVER : TEXT_AUTO);
        MIBLogger.getInstance().info("road name on the cockpit: " + (mode == TEXT_ALWAYS ? "always" : (mode == TEXT_NEVER ? "never" : "auto, only while the cockpit shows no map")));
        return mode;
    }

    private boolean cockpitShowsMap() {
        int status;
        try {
            status = APIFactory.getAPIFactory().getNavigationService().getKombiMapStatus();
        } catch (Throwable t) {
            status = 0;
        }
        if (status != this.loggedMapStatus) {
            this.loggedMapStatus = status;
            MIBLogger.getInstance().info("cockpit map status " + status);
        }
        return status != 0;
    }

    /** Sends the road name, or "", as the mode and the cockpit's view decide; only on change. */
    private void sendRoad() {
        String want = this.road;
        if (this.textMode == TEXT_NEVER || (this.textMode == TEXT_AUTO && cockpitShowsMap())) {
            want = "";
        }
        if (want.equals(this.sentRoad)) {
            return;
        }
        this.sentRoad = want;
        ASLNavBAPFactory.getNavBAPApi().updateBapTurnToInfo(want, "");
        if (this.textMode == TEXT_AUTO) {
            MIBLogger.getInstance().info(want.length() == 0 && this.road.length() != 0 ? "road name held back: the cockpit shows the map" : "road name sent: " + want);
        }
    }

    public void navigationFocus(int focus) {
        NavigationASLDataAdapter adapter = (NavigationASLDataAdapter) APIFactory.getAPIFactory().getNavigationService();
        adapter.datapoolValueChanged(733); // Navigation status id

        switch (focus) {
            case Constants.NAVFOCUS_NATIVE: {
                navStopped();
                break;
            }
            case Constants.NAVFOCUS_PROJECTED: {
                navStarted();
                break;
            }
        }
    }

    private void navStopped() {
        nextTurn = null;
        road = "";
        sentRoad = "";
        ASLNavBAPFactory.getNavBAPApi().updateBapTurnToInfo("", "");
        try {
            ASLNavBAPFactory.getNavBAPApi().updateBapDistanceToNextManeuver(-1, 0, false, 0);
        } catch (Throwable t) {
            MIBLogger.getInstance().error("could not clear the distance: " + t);
        }
        maneuverDescriptorSender.clear();
    }

    private void navStarted() {
        nextTurn = null;
        road = "";
        sentRoad = null;
        maneuverDescriptorSender.reset();
    }

    public void handleNextTurnEvent(String road, int turnSide, int event, int turnAngle, int turnNumber) {
        this.road = road == null ? "" : road;
        sentRoad = textMode == TEXT_ALWAYS ? null : sentRoad;
        sendRoad();

        nextTurn = new NavTurn(turnSide, event, turnAngle, turnNumber);
    }

    public void handleUpdateNextTurnDistanceEvent(int distanceMeters, int timeSeconds) {
        MIBLogger.getInstance().trace("distanceMeters: " + distanceMeters + " timeSeconds: " + timeSeconds);

        int unit = distanceMeters < 1000 ? 0 : 1;
        int bargraph = 0;
        boolean bargraphOn = false;

        int bargraphThresholdMeters = getBaragraphThresholdMeters();
        if (distanceMeters <= bargraphThresholdMeters) {
            bargraphOn = true;
            // set bargraph between 0 and 100
            bargraph = (distanceMeters * 100) / bargraphThresholdMeters;
        }

        sendRoad(); // the driver may have switched the cockpit's map on or off
        sendManeuver(distanceMeters, timeSeconds);
        ASLNavBAPFactory.getNavBAPApi().updateBapDistanceToNextManeuver(distanceMeters, unit, bargraphOn, bargraph);
    }


    private int getBaragraphThresholdMeters() {
        // When NAVIGATIONTURNEVENT_OFF_RAMP we assume a highway. Start showing the Bargraph from 2km onwards
        if (nextTurn != null && nextTurn.getEvent() == Constants.NAVIGATIONTURNEVENT_OFF_RAMP) {
            return 2000;
        }

        return 200;
    }

    private void sendManeuver(int distanceMeters, int timeSeconds) {
        if (nextTurn != null && nextTurn.getEvent() == Constants.NAVIGATIONTURNEVENT_OFF_RAMP && distanceMeters <= 2000) {
            sendNextManeuverDescriptor();
            return;
        }

        if (nextTurn != null && distanceMeters <= 200) {
            sendNextManeuverDescriptor();
            return;
        }

        sendFollowRoadManeuverDescriptor();
    }

    private void sendNextManeuverDescriptor() {
        if (nextTurn == null) {
            MIBLogger.getInstance().error("no nextManeuver found");
            return;
        }

        maneuverDescriptorSender.send(nextTurn.toManeuverDescriptor());
    }

    private void sendFollowRoadManeuverDescriptor() {
        BapManeuverDescriptor maneuverDescriptor = new BapManeuverDescriptor();
        maneuverDescriptor.mainElement = ManeuverDescriptorMainElement.FOLLOW_STREET;
        maneuverDescriptor.direction = ManeuverDescriptorDirection.STRAIGHT;

        maneuverDescriptorSender.send(maneuverDescriptor);
    }
}
