package de.macsinspace.vcaa;

import de.adi961.miblogger.MIBLogger;
import de.vw.mib.bap.datatypes.BAPString;

import java.lang.reflect.Method;

/**
 * BAPString.setContent returns the BAPString on VW firmware and void on SEAT
 * firmware. A direct call is linked by its exact signature, so a jar compiled
 * against one fails on the other (no music or arrows on the cluster). Looking
 * the method up by name and argument type works on both.
 */
public class BapStringCompat {

    private static Method setFromString;
    private static Method setFromBapString;

    public static void set(BAPString target, String value) {
        try {
            if (setFromString == null) {
                setFromString = BAPString.class.getMethod("setContent", new Class[]{String.class});
            }
            setFromString.invoke(target, new Object[]{value});
        } catch (Exception e) {
            MIBLogger.getInstance().error("BAPString.setContent(String) failed: " + e);
        }
    }

    public static void set(BAPString target, BAPString value) {
        try {
            if (setFromBapString == null) {
                setFromBapString = BAPString.class.getMethod("setContent", new Class[]{BAPString.class});
            }
            setFromBapString.invoke(target, new Object[]{value});
        } catch (Exception e) {
            MIBLogger.getInstance().error("BAPString.setContent(BAPString) failed: " + e);
        }
    }
}
