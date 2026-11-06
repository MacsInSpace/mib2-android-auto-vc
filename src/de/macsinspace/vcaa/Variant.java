package de.macsinspace.vcaa;

/**
 * Which jar this build is. The full jar (VCAndroidAuto.jar) sends Android
 * Auto's turn data to the cockpit and shows the track playing. The music-only
 * jar (VCAndroidAutoMusic.jar, "make music") shows only the track: it leaves
 * out the navsd overrides and never touches navigation focus, so the cockpit
 * never switches to its own arrow view. On VW that switch blanks the phone's
 * map from mib-cluster-map when a route starts.
 *
 * "make music" builds from a copy of the sources with this set to true.
 */
public final class Variant {
    public static final boolean MUSIC_ONLY = false;

    private Variant() {
    }
}
