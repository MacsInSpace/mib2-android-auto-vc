package de.macsinspace.vcaa;

import de.adi961.miblogger.MIBLogger;
import de.vw.mib.asl.framework.api.dsiproxy.DSIProxy;
import de.vw.mib.asl.framework.api.dsiproxy.DSIProxyFactory;
import de.vw.mib.asl.framework.api.dsiproxy.DSIServiceStateListener;
import de.vw.mib.asl.internal.kombipictureserver.usecaces.CoverArt;
import de.vw.mib.bap.mqbab2.audiosd.functions.CurrentStationInfo;
import de.vw.mib.genericevents.Target;
import org.dsi.ifc.carplay.AppState;
import org.dsi.ifc.carplay.CallState;
import org.dsi.ifc.carplay.DSICarplay;
import org.dsi.ifc.carplay.DSICarplayListener;
import org.dsi.ifc.carplay.DeviceInfo;
import org.dsi.ifc.carplay.PlaybackInfo;
import org.dsi.ifc.carplay.Resource;
import org.dsi.ifc.carplay.TelephonyState;
import org.dsi.ifc.carplay.TrackData;
import org.dsi.ifc.global.ResourceLocator;

/**
 * CarPlay track title, artist and album for the cockpit's media line.
 *
 * The stock CarPlay target never subscribes to the now-playing attribute, so the
 * cockpit only shows "Apple CarPlay". This listener subscribes to it on its own
 * (attribute 3, the same data the head unit gets from the iPhone) and hands the
 * text to CurrentStationInfo. It never sends anything to the phone: no audio,
 * video, focus or navigation calls. Every DSI call is wrapped so a failure here
 * only costs the track text.
 */
public final class CarPlayNowPlaying implements DSICarplayListener, DSIServiceStateListener {
    private static final int ATTR_NOWPLAYINGDATA = 3;
    // 6 = ATTR_COVERARTURL: the order of DSICarplay's ATTR_ fields with
    // DEVICEINFO 2 and MAINAUDIOTYPE 12 (both in the stock subscription).
    private static final int ATTR_COVERARTURL = 6;
    private static final int[] ATTRS = new int[] { ATTR_NOWPLAYINGDATA, ATTR_COVERARTURL };

    private static CarPlayNowPlaying instance;
    private static String title = "";
    private static String artist = "";
    private static String album = "";
    private static boolean known;
    private static ResourceLocator cover;

    private final Target owner;
    private DSICarplay service;
    private boolean responseListenerAdded;

    private CarPlayNowPlaying(Target owner) {
        this.owner = owner;
    }

    /** Called once from AndroidAutoTarget.startup(). */
    public static synchronized void start(Target owner) {
        if (instance != null) {
            return;
        }
        try {
            CarPlayNowPlaying next = new CarPlayNowPlaying(owner);
            DSIProxyFactory.getDSIProxyAPI().getDSIProxy().addServiceStateListener(DSICarplay.class, next);
            instance = next;
            log("CarPlay now playing: waiting for the CarPlay service");
        } catch (Throwable t) {
            log("CarPlay now playing: start failed " + t);
        }
    }

    public static synchronized void stop() {
        CarPlayNowPlaying old = instance;
        instance = null;
        if (old == null) {
            return;
        }
        try {
            DSIProxyFactory.getDSIProxyAPI().getDSIProxy().removeServiceStateListener(DSICarplay.class, old);
        } catch (Throwable t) {
            log("CarPlay now playing: stop failed " + t);
        }
        old.detach();
    }

    /** Subscribe again, e.g. when a phone connects; cheap if already subscribed. */
    public static synchronized void resubscribe() {
        if (instance != null && instance.service != null) {
            instance.subscribe();
        }
    }

    /** The iPhone's cover for the current track, or null. */
    public static synchronized ResourceLocator coverArt() {
        return known ? cover : null;
    }

    /** {title, artist, album} of the current CarPlay track, or null if none is known. */
    public static synchronized String[] snapshot() {
        if (!known || title.length() == 0) {
            return null;
        }
        return new String[] { title, artist, album };
    }

    public void registered(String name, int instanceId) {
        synchronized (CarPlayNowPlaying.class) {
            if (instance != this) {
                return;
            }
            try {
                DSIProxy proxy = DSIProxyFactory.getDSIProxyAPI().getDSIProxy();
                this.service = (DSICarplay) proxy.getService(this.owner, DSICarplay.class);
                if (this.service == null) {
                    log("CarPlay now playing: no service for " + name);
                    return;
                }
                if (!this.responseListenerAdded) {
                    proxy.addResponseListener(this.owner, DSICarplayListener.class, this);
                    this.responseListenerAdded = true;
                }
                this.subscribe();
                log("CarPlay now playing: subscribed (" + name + ", " + instanceId + ")");
            } catch (Throwable t) {
                log("CarPlay now playing: subscribe failed " + t);
            }
        }
    }

    public void unregistered(String name, int instanceId) {
        synchronized (CarPlayNowPlaying.class) {
            if (instance != this) {
                return;
            }
            this.detach();
            log("CarPlay now playing: service gone (" + name + ")");
        }
    }

    private void subscribe() {
        try {
            this.service.setNotification(ATTRS, this);
        } catch (Throwable t) {
            log("CarPlay now playing: setNotification failed " + t);
        }
    }

    private void detach() {
        try {
            if (this.service != null) {
                this.service.clearNotification(ATTRS, this);
            }
        } catch (Throwable t) {
            // the service is going away anyway
        }
        try {
            if (this.responseListenerAdded) {
                DSIProxyFactory.getDSIProxyAPI().getDSIProxy().removeResponseListener(this.owner, DSICarplayListener.class, this);
            }
        } catch (Throwable t) {
            // nothing else to undo
        }
        this.responseListenerAdded = false;
        this.service = null;
        setCover(null);
        setTrack(null, null, null, false);
    }

    private static void setTrack(String newTitle, String newArtist, String newAlbum, boolean valid) {
        boolean changed;
        synchronized (CarPlayNowPlaying.class) {
            String t = clean(newTitle);
            String a = clean(newArtist);
            String b = clean(newAlbum);
            boolean k = valid && t.length() != 0;
            changed = k != known || !t.equals(title) || !a.equals(artist) || !b.equals(album);
            known = k;
            title = t;
            artist = a;
            album = b;
        }
        if (changed) {
            CurrentStationInfo.refreshLater();
            CoverArt.refreshFromCarPlay();
        }
    }

    private static void setCover(ResourceLocator locator) {
        boolean changed;
        synchronized (CarPlayNowPlaying.class) {
            String was = cover == null ? null : cover.toString();
            String now = locator == null ? null : locator.toString();
            changed = was == null ? now != null : !was.equals(now);
            cover = locator;
        }
        if (changed) {
            CoverArt.refreshFromCarPlay();
        }
    }

    private static String clean(String s) {
        return s == null ? "" : s.trim();
    }

    public void updateNowPlayingData(TrackData data, int valid) {
        try {
            if (instance != this) {
                return;
            }
            if (data == null || valid != 1) {
                setTrack(null, null, null, false);
            } else {
                setTrack(data.title, data.artist, data.album, true);
            }
            MIBLogger.getInstance().debug("CarPlay now playing: valid=" + valid + " " + (data == null ? "null" : data.toString()));
        } catch (Throwable t) {
            log("CarPlay now playing: update failed " + t);
        }
    }

    private static void log(String message) {
        try {
            MIBLogger.getInstance().info(message);
        } catch (Throwable t) {
            // logging must never break the target
        }
    }

    public void asyncException(int code, String text, int request) {
        log("CarPlay now playing: DSI exception " + code + " " + text);
    }

    public void updateMode(Resource[] r, AppState[] a, int valid) { }
    public void responseModeChange(Resource[] r, AppState[] a, int valid) { }
    public void requestBTDeactivation(String a, int b) { }
    public void updateDeviceInfo(DeviceInfo d, int valid) { }
    public void updateCallState(CallState[] a, int valid) { }
    public void updateTelephonyState(TelephonyState a, int valid) { }
    public void updatePlaybackState(PlaybackInfo a, int valid) { }
    public void updatePlayposition(int position, int valid) { }
    public void updateCoverArtUrl(ResourceLocator locator, int valid) {
        try {
            if (instance != this) {
                return;
            }
            setCover(valid == 1 ? locator : null);
            log("CarPlay cover: valid=" + valid + " " + (locator == null ? "null" : locator.toString()));
        } catch (Throwable t) {
            log("CarPlay cover: update failed " + t);
        }
    }
    public void updateTextInputState(int a, int valid) { }
    public void duckAudio(int a, double b) { }
    public void unduckAudio(int a) { }
    public void oemAppSelected() { }
    public void updateMainAudioType(int a, int valid) { }
}
