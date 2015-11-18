package info.blockchain.wallet.util;


import java.util.HashMap;

import android.content.Context;
//import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

/**
* This class obtains info on the currencies communicated via http://blockchain.info/ticker.
* 
*/
public class ExchangeRateFactory	{

	private static Context context = null;

	private static String strData = null;

    private static HashMap<String,Double> fxRates = null;
    private static HashMap<String,String> fxSymbols = null;
    
    private static ExchangeRateFactory instance = null;

    /**
    * Currencies handles by http://blockchain.info/ticker
    *
    */
    private static String[] currencies = {
    	   "AUD", "BRL", "CAD", "CHF", "CLP", "CNY", "DKK", "EUR", "GBP", "HKD",
    	   "ISK", "JPY", "KRW", "NZD", "PLN", "RUB", "SEK", "SGD", "THB", "TWD", "USD"
	};

	private static String[] currencyLabels = {
	    "United States Dollar - USD",
	    "Euro - EUR",
	    "British Pound Sterling - GBP",
	    "Australian Dollar - AUD",
	    "Brazilian Real - BRL",
	    "Canadian Dollar - CAD",
	    "Chinese Yuan - CNY",
	    "Swiss Franc - CHF",
	    "Chilean Peso - CLP",
	    "Danish Krone - DKK",
	    "Hong Kong Dollar - HKD",
	    "Icelandic Krona - ISK",
	    "Japanese Yen - JPY",
	    "South Korean Won - KRW",
	    "New Zealand Dollar - NZD",
	    "Polish Zloty - PLN",
	    "Russian Rouble - RUB",
	    "Swedish Krona - SEK",
	    "Singapore Dollar - SGD",
	    "Thai Baht - THB",
	    "Taiwanese Dollar - TWD",
		};

    
    private ExchangeRateFactory()	 { ; }

    public static ExchangeRateFactory getInstance(Context ctx)	 {
    	
    	context = ctx;
    	
    	if(instance == null)	 {
        	fxRates = new HashMap<String,Double>();
        	fxSymbols = new HashMap<String,String>();

    		instance = new ExchangeRateFactory();
    	}

    	return instance;
    }

    public double getLastPrice(String currency)	 {
    	if(fxRates.get(currency) != null && fxRates.get(currency) > 0.0)	 {
            PrefsUtil.getInstance(context).setValue("LAST_KNOWN_VALUE_FOR_CURRENCY_" + currency, Double.toString(fxRates.get(currency)));
    		return fxRates.get(currency);
    	}
    	else	 {
            return Double.parseDouble(PrefsUtil.getInstance(context).getValue("LAST_KNOWN_VALUE_FOR_CURRENCY_" + currency, "0.0"));
    	}
    }

    public String getSymbol(String currency)	 {
    	if(fxSymbols.get(currency) != null)	 {
    		return fxSymbols.get(currency);
    	}
    	else	 {
    		return null;
    	}
    }

    public String[] getCurrencies()	 {
    	return currencies;
    }

    public String[] getCurrencyLabels()	 {
    	return currencyLabels;
    }

    public void setData(String data)	 {
    	strData = data;
    }

    /**
     * Parse the data supplied to this instance.
     * 
     */
    public void updateFxPricesForEnabledCurrencies()	 {
    	for(int i = 0; i < currencies.length; i++)	 {
        	setFxPriceForCurrency(currencies[i]);
    	}
    }

    private void setFxPriceForCurrency(String currency)	 {
        try {
    		JSONObject jsonObject = new JSONObject(strData);
    		if(jsonObject != null)	{
    			JSONObject jsonCurr = jsonObject.getJSONObject(currency);
        		if(jsonCurr != null)	{
        			double last_price = jsonCurr.getDouble("last");
        			fxRates.put(currency, Double.valueOf(last_price));
        			String symbol = jsonCurr.getString("symbol");
        			fxSymbols.put(currency, symbol);
        		}
    		}
    	} catch (JSONException je) {
			fxRates.put(currency, Double.valueOf(-1.0));
			fxSymbols.put(currency, null);
    	}
    }
}
