/*
 * Written from the stock class's bytecode (MU1367 lsd.jxe): every method of
 * the stock MapPresentation is empty, so the head unit ignores what the
 * cluster sends on BAP FctID 54 (Map_Presentation). This version keeps that
 * behaviour and only logs it.
 *
 * Why: Map_Presentation carries the cockpit's layout -- largeMapView (wide
 * map, small dials) and whether a side menu is open over the map. The
 * cluster map's CarPlay safe area and the phone's view of the cluster
 * should follow it (mib-cluster-map #19, #48), and SEAT's full-width view
 * still has to be told apart from the wide one. Cycle the VIEW button with
 * this loaded and the log shows what each layout sends.
 */
package de.vw.mib.bap.mqbab2.navsd.functions;

import de.adi961.miblogger.MIBLogger;
import de.vw.mib.bap.datatypes.BAPEntity;
import de.vw.mib.bap.functions.BAPFunctionListener;
import de.vw.mib.bap.functions.Property;
import de.vw.mib.bap.functions.PropertyListener;
import de.vw.mib.bap.mqbab2.common.api.stages.BAPStage;
import de.vw.mib.bap.mqbab2.common.api.stages.BAPStageInitializer;
import de.vw.mib.bap.mqbab2.common.api.stages.Function;
import de.vw.mib.bap.mqbab2.generated.navsd.serializer.Map_Presentation_ASG_HMI_State;
import de.vw.mib.bap.mqbab2.generated.navsd.serializer.Map_Presentation_SetGet;
import de.vw.mib.bap.mqbab2.generated.navsd.serializer.Map_Presentation_Status;
import de.vw.mib.bap.mqbab2.navsd.api.ASLNavSDConstants;

public class MapPresentation
        extends Function
        implements Property,
        ASLNavSDConstants {
    static /* synthetic */ Class class$de$vw$mib$bap$mqbab2$generated$navsd$serializer$Map_Presentation_Status;

    public MapPresentation() {
        super();
    }

    public BAPEntity init(BAPStageInitializer bAPStageInitializer) {
        MIBLogger.getInstance().info("MapPresentation: init (FctID 54)");
        return null;
    }

    protected Map_Presentation_Status dequeueBAPEntity() {
        return (Map_Presentation_Status) this.context.dequeueBAPEntity(this,
                class$de$vw$mib$bap$mqbab2$generated$navsd$serializer$Map_Presentation_Status == null
                        ? (class$de$vw$mib$bap$mqbab2$generated$navsd$serializer$Map_Presentation_Status = MapPresentation
                                .class$("de.vw.mib.bap.mqbab2.generated.navsd.serializer.Map_Presentation_Status"))
                        : class$de$vw$mib$bap$mqbab2$generated$navsd$serializer$Map_Presentation_Status);
    }

    public void setFunctionData(BAPStage bAPStage, Object object) {
    }

    public int getFunctionId() {
        return 54;
    }

    public void initialize(boolean bl) {
    }

    public void uninitialize() {
    }

    public void process(int n) {
    }

    public void requestAcknowledge() {
    }

    public void errorAcknowledge() {
    }

    public void getProperty(BAPEntity bAPEntity, PropertyListener propertyListener) {
        log("get", bAPEntity);
    }

    public void setGetProperty(BAPEntity bAPEntity, PropertyListener propertyListener) {
        log("setget", bAPEntity);
    }

    public void ackProperty(BAPEntity bAPEntity, PropertyListener propertyListener) {
        log("ack", bAPEntity);
    }

    public void indicationError(int n, BAPFunctionListener bAPFunctionListener) {
        MIBLogger.getInstance().info("MapPresentation: indicationError " + n);
    }

    /* Never let logging break the BAP stage that called us. */
    private static void log(String what, BAPEntity entity) {
        try {
            Map_Presentation_ASG_HMI_State s = null;
            if (entity instanceof Map_Presentation_SetGet) {
                s = ((Map_Presentation_SetGet) entity).asg_Hmi_State;
            } else if (entity instanceof Map_Presentation_Status) {
                s = ((Map_Presentation_Status) entity).asg_Hmi_State;
            }
            if (s != null) {
                MIBLogger.getInstance().info("MapPresentation: " + what
                        + " largeMapView=" + s.largeMapView
                        + " leftSideMenueOpen=" + s.leftSideMenueOpen
                        + " rightSideMenueOpen=" + s.rightSideMenueOpen
                        + " t=" + System.currentTimeMillis());
            } else {
                MIBLogger.getInstance().info("MapPresentation: " + what + " "
                        + (entity == null ? "null" : entity.getClass().getName() + " " + entity)
                        + " t=" + System.currentTimeMillis());
            }
        } catch (Throwable t) {
            // nothing: the stock class did nothing here either
        }
    }

    static /* synthetic */ Class class$(String string) {
        try {
            return Class.forName(string);
        } catch (ClassNotFoundException classNotFoundException) {
            throw new NoClassDefFoundError(classNotFoundException.getMessage());
        }
    }
}
