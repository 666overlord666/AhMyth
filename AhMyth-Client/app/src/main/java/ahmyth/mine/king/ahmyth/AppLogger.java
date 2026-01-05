package ahmyth.mine.king.ahmyth;

import android.util.Log;
// import ahmyth.mine.king.ahmyth.BuildConfig; // Это будет импортировано автоматически компилятором при использовании

public class AppLogger {
    private static final String APP_TAG = "AhMythClient"; // Общий тег для всех логов приложения

    // Debug логи, активные только в отладочных сборках
    public static void d(String tag, String message) {
        if (BuildConfig.DEBUG) {
            Log.d(APP_TAG, tag + ": " + message);
        }
    }

    // Debug логи с Throwable, активные только в отладочных сборках
    public static void d(String tag, String message, Throwable tr) {
        if (BuildConfig.DEBUG) {
            Log.d(APP_TAG, tag + ": " + message, tr);
        }
    }

    // Error логи, активные всегда
    public static void e(String tag, String message) {
        Log.e(APP_TAG, tag + ": " + message);
    }

    // Error логи с Throwable, активные всегда
    public static void e(String tag, String message, Throwable tr) {
        Log.e(APP_TAG, tag + ": " + message, tr);
    }

    // Другие уровни логирования (info, warn, verbose) могут быть добавлены по аналогии
}
