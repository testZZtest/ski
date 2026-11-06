package ski.agent;

/** Single source of the app version; read by build.sh and app/build.gradle.kts. */
final class BuildInfo {
    static final String VERSION = "2.5.0";
    static final int VERSION_CODE = 203;
    /** Ski's Google Play listing (the play flavor, signed by Google). */
    static final String PLAY_URL = "https://play.google.com/store/apps/details?id=ski.agent";

    private BuildInfo() {
    }
}
