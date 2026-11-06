package de.macsinspace.vcaa;

/**
 * Which jar this build is. The full jar (VCAndroidAuto.jar) sends Android
 * Auto's turn data to the cockpit and shows the track playing. The music-only
 * jar (VCAndroidAutoMusic.jar, "make music") shows only the track: it leaves
 * out the navsd overrides and never touches navigation focus, so the cockpit
 * never switches to its own arrow view. On VW that switch blanks the phone's
 * map from mib-cluster-map when a route starts.
 *
 * The turns jar (VCAndroidAutoTurns.jar, "make turns") is the full jar with
 * KEEP_MAP_VIEW set: it sends the turn data but leaves the cockpit's view
 * (arrows or map, ActiveRgType) to the stock logic, so the cockpit should stay
 * on the map and draw its own arrow beside it instead of switching to the
 * arrow view.
 *
 * "make music" and "make turns" build from a copy of the sources with one of
 * these set to true.
 */
public final class Variant {
    public static final boolean MUSIC_ONLY = false;
    public static final boolean KEEP_MAP_VIEW = false;

    private Variant() {
    }
}
