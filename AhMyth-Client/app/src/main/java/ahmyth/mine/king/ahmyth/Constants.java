package ahmyth.mine.king.ahmyth;

public class Constants {
    // Order commands
    public static final String ORDER_SCREEN_CAPTURE = "x0000sc";
    public static final String ORDER_CAMERA = "x0000ca";
    public static final String ORDER_FILE_MANAGER = "x0000fm";
    public static final String ORDER_SMS = "x0000sm";
    public static final String ORDER_CALLS = "x0000cl";
    public static final String ORDER_CONTACTS = "x0000cn";
    public static final String ORDER_MICROPHONE = "x0000mc";
    public static final String ORDER_LOCATION = "x0000lm";

    // Screen Capture related constants
    public static final int JPEG_COMPRESSION_QUALITY = 20; // 20% compression
    public static final int INITIAL_FRAMES_TO_SKIP = 3;    // Number of initial frames to skip in screen capture
    public static final int SCREEN_CAPTURE_TIMEOUT_MS = 5000; // 5 seconds timeout for screen capture
    public static final int MEDIA_PROJECTION_DELAY_MS = 300; // 300ms delay before auto-capturing after permission
}
