package info.blockchain.wallet.util;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.hardware.Camera;

import org.bitcoinj.core.bip44.WalletFactory;

import java.io.File;
import java.util.Timer;
import java.util.TimerTask;

import info.blockchain.wallet.MainActivity;
import info.blockchain.wallet.PinEntryActivity;
import info.blockchain.wallet.crypto.AESUtil;
import info.blockchain.wallet.payload.PayloadFactory;

public class AppUtil {

    private static String REGEX_UUID = "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$";

    private static AppUtil instance = null;
	private static Context context = null;

    private static boolean DEBUG = false;
    private static boolean LEGACY = true;

    private static long TIMEOUT_DELAY = 1000L * 60L * 2L;
    private static long lastUserInteraction = 0L;
    private static Timer timer;
    private static boolean inBackground = false;
    private static boolean isLocked = true;

    private static String strReceiveQRFilename = null;

    private static long UPGRADE_REMINDER_DELAY = 1000L * 60L * 60L * 24L * 14L;
    private static boolean newlyCreated = false;
    private static boolean allowLockTimer = true;

	private AppUtil() { ; }

	public static AppUtil getInstance(Context ctx) {
		
		context = ctx;
		
		if(instance == null) {
            strReceiveQRFilename = context.getExternalCacheDir() + File.separator + "qr.png";
			instance = new AppUtil();
		}
		
		return instance;
	}

    public void clearCredentialsAndRestart() {
        WalletFactory.getInstance().set(null);
        PayloadFactory.getInstance().wipe();
		PrefsUtil.getInstance(context).clear();
		restartApp();
	}

	public void restartApp() {
		Intent intent = new Intent(context, MainActivity.class);
		intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
		context.startActivity(intent);
	}

	public void restartApp(String name, boolean value) {
		Intent intent = new Intent(context, MainActivity.class);
		intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
		if(name != null) {
    		intent.putExtra(name, value);
		}
		context.startActivity(intent);
	}

    public void closeApp() {
        Intent intent = new Intent(context, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra("EXIT_APP", true);
        context.startActivity(intent);
    }

    public boolean isDEBUG() {
        return DEBUG;
    }

    public void setDEBUG(boolean debug) {
        DEBUG = debug;
    }

    public void initUserInteraction() {
        lastUserInteraction = System.currentTimeMillis();
    }

    public void updateUserInteractionTime() {

        if(AppUtil.getInstance(context).isTimedOut()) {

            if(!AppUtil.getInstance(context).isLocked()) {
                Intent i = new Intent(context, PinEntryActivity.class);
                i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(i);
            }
        }else
            lastUserInteraction = System.currentTimeMillis();
    }

	public void clearUserInteractionTime() {
		lastUserInteraction = 0L;
	}

    public boolean isTimedOut() {

        boolean result = (System.currentTimeMillis() - lastUserInteraction) > TIMEOUT_DELAY;
        return result;
    }

    public void timeOut() {
        lastUserInteraction = 0L;
    }

    public String getReceiveQRFilename(){
        return strReceiveQRFilename;
    }

    public void deleteQR(){
        File file = new File(strReceiveQRFilename);
        if(file.exists()) {
            file.delete();
        }
    }

    public boolean isTimeForUpgradeReminder() {
        long lastReminder = 0L;
        try {
            lastReminder = PrefsUtil.getInstance(context).getValue(PrefsUtil.KEY_HD_UPGRADED_LAST_REMINDER, 0L);
        }
        catch(NumberFormatException nfe) {
            lastReminder = 0L;
        }
        return (System.currentTimeMillis() - lastReminder) > UPGRADE_REMINDER_DELAY;
    }

    public void setUpgradeReminder(long ts) {
        PrefsUtil.getInstance(context).setValue(PrefsUtil.KEY_HD_UPGRADED_LAST_REMINDER, ts);
    }

    public boolean isNewlyCreated() {
        return newlyCreated;
    }

    public void setNewlyCreated(boolean newlyCreated) {
        this.newlyCreated = newlyCreated;
    }

    public boolean isSane() {

        String guid = PrefsUtil.getInstance(context).getValue(PrefsUtil.KEY_GUID, "");

        if(!guid.matches(REGEX_UUID))  {
            return false;
        }

        String encryptedPassword = PrefsUtil.getInstance(context).getValue(PrefsUtil.KEY_ENCRYPTED_PASSWORD, "");
        String pinID = PrefsUtil.getInstance(context).getValue(PrefsUtil.KEY_PIN_IDENTIFIER, "");

        if(encryptedPassword.length() == 0 || pinID.length() == 0)  {
            return false;
        }

        return true;
    }

    public boolean isCameraOpen() {

        Camera camera = null;

        try {
            camera = Camera.open();
        }
        catch (RuntimeException e) {
            return true;
        }
        finally {
            if (camera != null) {
                camera.release();
            }
        }

        return false;
    }

    public String getSharedKey()   {
        if(PrefsUtil.getInstance(context).has(PrefsUtil.KEY_SHARED_KEY))    {
            String sharedKey = PrefsUtil.getInstance(context).getValue(PrefsUtil.KEY_SHARED_KEY, "");
            setSharedKey(sharedKey);
            PrefsUtil.getInstance(context).removeValue(PrefsUtil.KEY_SHARED_KEY);
            return sharedKey;
        }
        else    {
            return AESUtil.decrypt(PrefsUtil.getInstance(context).getValue(PrefsUtil.KEY_SHARED_KEY_X, ""), PayloadFactory.getInstance().getTempPassword(), AESUtil.PasswordPBKDF2Iterations);
        }

    }

    public String getSharedKey(CharSequenceX password)   {
        if(PrefsUtil.getInstance(context).has(PrefsUtil.KEY_SHARED_KEY))    {
            String sharedKey = PrefsUtil.getInstance(context).getValue(PrefsUtil.KEY_SHARED_KEY, "");
            setSharedKey(sharedKey);
            PrefsUtil.getInstance(context).removeValue(PrefsUtil.KEY_SHARED_KEY);
            return sharedKey;
        }
        else    {
            return AESUtil.decrypt(PrefsUtil.getInstance(context).getValue(PrefsUtil.KEY_SHARED_KEY_X, ""), password, AESUtil.PasswordPBKDF2Iterations);
        }

    }

    public void setSharedKey(String sharedKey)   {
        String sharedKeyEncrypted = AESUtil.encrypt(sharedKey, PayloadFactory.getInstance().getTempPassword(), AESUtil.PasswordPBKDF2Iterations);
        PrefsUtil.getInstance(context).setValue(PrefsUtil.KEY_SHARED_KEY_X, sharedKeyEncrypted);
    }

    public boolean isLegacy() {
        return LEGACY;
    }

    public void setLegacy(boolean lame) {
        LEGACY = lame;
    }

    /*
    Called from all activities' onPause
     */
    public void startLockTimer() {

        if(AppUtil.getInstance(context).isTimedOut() || AppUtil.getInstance(context).isLocked())return;

        if(timer!=null){
            timer.cancel();
        }

        if(allowLockTimer) {
            timer = new Timer();
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    if (context != null && !isLocked) {

                        clearUserInteractionTime();

                        if (inBackground) {
                            ((Activity) context).finish();
                        } else {
                            Intent i = new Intent(context, PinEntryActivity.class);
                            i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                            context.startActivity(i);
                        }
                    }
                }
            }, 2000);
        }
    }

    /*
    Called from all activities' onResume
     */
    public void stopLockTimer(){

        inBackground = false;

        //App is not backgrounded - interrupt thread
        if(timer!=null){
            timer.cancel();
        }
    }

    public static void setInBackground(boolean inBackground) {
        AppUtil.inBackground = inBackground;
    }

    public static boolean isLocked() {
        return isLocked;
    }

    public static void setIsLocked(boolean isLocked) {
        AppUtil.isLocked = isLocked;
        if(timer!=null){
            timer.cancel();
        }
    }

    /*
    Only disable the lock timer when app navigates to web browser,zeroblock or any qr sharing app.
    This will allow the blockchain app to not go to pin screen immediately
     */
    public static void setAllowLockTimer(boolean allowLockTimer) {
        AppUtil.allowLockTimer = allowLockTimer;
    }
}
