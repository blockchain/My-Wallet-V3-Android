package info.blockchain.wallet.access;

import android.content.Context;
import android.os.AsyncTask;

import org.json.JSONObject;
import org.spongycastle.util.encoders.Hex;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Date;
import java.util.concurrent.ExecutionException;

import info.blockchain.wallet.crypto.AESUtil;
import info.blockchain.wallet.payload.PayloadFactory;
import info.blockchain.wallet.util.AppUtil;
import info.blockchain.wallet.util.CharSequenceX;
import info.blockchain.wallet.util.PrefsUtil;
import info.blockchain.wallet.util.WebUtil;

//import android.util.Log;

public class AccessFactory	{

    private static String _key = null;
    private static String _value = null;
    private static String _pin = null;
	private static String _email = null;

    private static boolean isLoggedIn = false;

    private static Context context = null;
    private static AccessFactory instance = null;

    private AccessFactory()	{ ; }

    public static AccessFactory getInstance(Context ctx) {

    	context = ctx;

        if (instance == null) {
            instance = new AccessFactory();
        }

        return instance;
    }

    public boolean createPIN(CharSequenceX password, String pin) {

        if(pin == null || pin.equals("0000") || pin.length() != 4) {
            return false;
        }

        _pin = pin;

		AppUtil.getInstance(context).applyPRNGFixes();

        try {
            byte[] bytes = new byte[16];
            SecureRandom random = new SecureRandom();
            random.nextBytes(bytes);
            _key = new String(org.spongycastle.util.encoders.Hex.encode(bytes), "UTF-8");
//            Log.i("AccessFactory", "_key:" + _key);
            random.nextBytes(bytes);
            _value = new String(org.spongycastle.util.encoders.Hex.encode(bytes), "UTF-8");
//            Log.i("AccessFactory", "_value:" + _value);
            
            JSONObject json = apiStoreKey();
//            Log.i("AccessFactory", "JSON response:" + json.toString());
			if(json.get("success") != null) {
				String encrypted_password = AESUtil.encrypt(password.toString(), new CharSequenceX(_value), AESUtil.PinPbkdf2Iterations);
				PrefsUtil.getInstance(context).setValue(PrefsUtil.KEY_ENCRYPTED_PASSWORD, encrypted_password);
				PrefsUtil.getInstance(context).setValue(PrefsUtil.KEY_PIN_IDENTIFIER, _key);
				return true;
			}
			else {
				return false;
			}

        }
        catch(UnsupportedEncodingException uee) {
            return false;
        }
        catch(Exception e) {
        	e.printStackTrace();
            return false;
        }

    }

    public CharSequenceX validatePIN(String pin) throws Exception {
    	
    	CharSequenceX password = null;

        _pin = pin;
        _key = PrefsUtil.getInstance(context).getValue(PrefsUtil.KEY_PIN_IDENTIFIER, "");
        String encrypted_password = PrefsUtil.getInstance(context).getValue(PrefsUtil.KEY_ENCRYPTED_PASSWORD, "");

        try {
			final JSONObject json = apiGetValue();
//            Log.i("AccessFactory", "JSON response:" + json.toString());
			String decryptionKey = (String)json.get("success");
			password = new CharSequenceX(AESUtil.decrypt(encrypted_password, new CharSequenceX(decryptionKey), AESUtil.PinPbkdf2Iterations));
			return password;
        }
        catch(UnsupportedEncodingException uee) {
            throw uee;
        }
        catch(Exception e) {
        	e.printStackTrace();

            if(e.getMessage().contains("Invalid Response"))
                return null;
            else
                throw e;
        }
    }

    public boolean writeAccess() {
        return true;
    }

    public String getKey() {
        return _key;
    }

    public String getValue() {
        return _value;
    }

    public String getPIN() {
        return _pin;
    }

    public boolean isLoggedIn() {
        return isLoggedIn;
    }

    public void setIsLoggedIn(boolean logged) {
        isLoggedIn = logged;
    }

    private JSONObject apiGetValue() throws Exception {

        StringBuilder args = new StringBuilder();

        args.append("key=" + _key);
        args.append("&pin=" + _pin);
        args.append("&method=get");

        String response = WebUtil.getInstance().postURL(WebUtil.ACCESS_URL, args.toString(), 1);

        if (response == null || response.length() == 0)
            throw new Exception("Invalid Server Response");

        return new JSONObject(response);
    }

    private JSONObject apiStoreKey() throws Exception {

        StringBuilder args = new StringBuilder();

        args.append("key=" + _key);
        args.append("&value=" + _value);
        args.append("&pin=" + _pin);
        args.append("&method=put");

        String response = WebUtil.getInstance().postURL(WebUtil.ACCESS_URL, args.toString());

        if (response == null || response.length() == 0)
            throw new Exception("Invalid Server Response");

        return new JSONObject(response);
    }

	public boolean resendEmailConfirmation(String email) {

		if (email == null) {
			return false;
		}

		_email = email;

		String response = null;
		try {
			response = new ApiUpdateEmail().execute(_email).get();
		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (ExecutionException e) {
			e.printStackTrace();
		}

		if (response != null && response.equals("Email Updated")) {
			PrefsUtil.getInstance(context).setValue(PrefsUtil.KEY_EMAIL, _email);
			return true;
		} else {
			return false;
		}
	}

	private class ApiUpdateEmail extends AsyncTask<String, Void, String>{

		@Override
		protected String doInBackground(String... params) {
			StringBuilder args = new StringBuilder();

			int serverTimeOffset = 500;
			String sharedKey = PayloadFactory.getInstance().get().getSharedKey().toLowerCase();
			long now = new Date().getTime();

			long timestamp = (now - serverTimeOffset) / 10000;
			String text = sharedKey + Long.toString(timestamp);
			String SKHashHex = null;
			try {
				SKHashHex = new String(Hex.encode(MessageDigest.getInstance("SHA-256").digest(text.getBytes("UTF-8"))));
			} catch (NoSuchAlgorithmException e) {
				e.printStackTrace();
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			}
			int i = 0;
			String tSKUID = SKHashHex.substring(i, i+=8)+"-"+SKHashHex.substring(i, i+=4)+"-"+SKHashHex.substring(i, i+=4)+"-"+SKHashHex.substring(i, i+=4)+"-"+SKHashHex.substring(i, i+=12);

			args.append("length="+params[0].length());
			args.append("&payload="+params[0]);
			args.append("&method="+"update-email");

			args.append("&sharedKey=" + tSKUID);
			args.append("&sKTimestamp=" + Long.toString(timestamp));
			args.append("&sKDebugHexHash=" + SKHashHex);
			args.append("&sKDebugTimeOffset=" + Long.toString(serverTimeOffset));
			args.append("&sKDebugOriginalClientTime=" + Long.toString(now));
			args.append("&sKDebugOriginalSharedKey=" + sharedKey);
			args.append("&guid="+PayloadFactory.getInstance().get().getGuid());
			args.append("&format=plain");

			String response = null;
			try {
				response = WebUtil.getInstance().postURL(WebUtil.PAYLOAD_URL, args.toString());
			} catch (Exception e) {
				e.printStackTrace();
			}

			if (response == null || response.length() == 0)
				response = "Invalid Server Response";

			return response;
		}
	}
}
