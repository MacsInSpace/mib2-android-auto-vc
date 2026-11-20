package de.macsinspace.vcaa;

import de.vw.mib.bap.mqbab2.common.api.androidauto.AndroidAutoService;

/**
 * Whether the phone is guiding, as the cockpit functions should see it.
 *
 * The stock flag (ASL property 895953920, isAndroidAutoRouteGuidanceActive)
 * is written by the unit's own RequestHandler when the phone changes the
 * navigation focus, but on a Golf P4521 it stayed true after the phone handed
 * the focus back when a route was stopped: RG_Status stayed 1 and the cockpit
 * kept its dial in guidance until a restart (owner's Golf, 2026-11-19).
 *
 * So the jar also follows the focus messages itself: the phone guides only
 * while the stock flag says so and the last focus the jar saw was the phone's.
 * Before the jar has seen any focus message it follows the stock flag alone.
 *
 * After a hand-back the phone does not always ask for the focus again when
 * the next route starts (#160: only "native" came, twice), so a new turn from
 * the phone after a hand-back also counts as the phone guiding again.
 */
public final class PhoneGuiding {
    private static final int UNKNOWN = -1;
    private static final int NATIVE = 0;
    private static final int PROJECTED = 1;
    /** A turn this soon after the hand-back is the old route's last one. */
    private static final long SETTLE_MS = 3000;
    private static volatile int focus = UNKNOWN;
    private static volatile long nativeAt;

    private PhoneGuiding() {
    }

    public static void focusNative() {
        nativeAt = System.currentTimeMillis();
        focus = NATIVE;
    }

    public static void focusProjected() {
        focus = PROJECTED;
    }

    /** A new turn arrived: true if this ends a hand-back (focus was the car's). */
    public static boolean turnArrived() {
        if (focus != NATIVE || System.currentTimeMillis() - nativeAt < SETTLE_MS) {
            return false;
        }
        focus = PROJECTED;
        return true;
    }

    public static boolean active(AndroidAutoService service) {
        return focus != NATIVE && service.isAndroidAutoRouteGuidanceActive();
    }

    public static String describe(AndroidAutoService service) {
        return "stock " + service.isAndroidAutoRouteGuidanceActive()
                + ", focus " + (focus == NATIVE ? "car" : focus == PROJECTED ? "phone" : "not seen");
    }
}
