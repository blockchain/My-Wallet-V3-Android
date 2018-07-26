package info.blockchain.wallet.settings;

import info.blockchain.wallet.WalletApiMockedResponseTest;
import info.blockchain.wallet.api.data.Settings;
import io.reactivex.observers.TestObserver;
import org.junit.Assert;
import org.junit.Test;

public class SettingsManagerTest extends WalletApiMockedResponseTest {

    private String guid = "49819a7c-2426-49da-90fd-9dabbd837cc8";
    private String sharedKey = "f57b221c-38ec-4e5d-8164-f120d9df0d0b";

    private SettingsManager subject;

    @Test
    public void getInfo() {

        mockInterceptor.setResponseString("{\"btc_currency\":\"BTC\",\"notifications_type\":[],\"language\":\"en\",\"notifications_on\":0,\"ip_lock_on\":0,\"dial_code\":\"44\",\"block_tor_ips\":0,\"currency\":\"USD\",\"notifications_confirmations\":0,\"auto_email_backup\":0,\"never_save_auth_type\":0,\"email\":\"john@mail.com\",\"sms_verified\":0,\"is_api_access_enabled\":0,\"auth_type\":0,\"my_ip\":\"211.160.45.230\",\"email_verified\":0,\"languages\":{\"de\":\"German\",\"no\":\"Norwegian\",\"hi\":\"Hindi\",\"fi\":\"Finnish\",\"ru\":\"Russian\",\"pt\":\"Portuguese\",\"bg\":\"Bulgarian\",\"fr\":\"French\",\"hu\":\"Hungarian\",\"zh-cn\":\"Chinese\",\"sl\":\"Slovenian\",\"id\":\"Indonesian\",\"sv\":\"Swedish\",\"ko\":\"Korean\",\"zh-tw\":\"Chinese\",\"ms\":\"Malay\",\"el\":\"Greek\",\"en\":\"English\",\"it\":\"Italian\",\"es\":\"Spanish\",\"vi\":\"Vietnam\",\"th\":\"Thai\",\"ja\":\"Japanese\",\"pl\":\"Polish\",\"da\":\"Danish\",\"ro\":\"Romanian\",\"nl\":\"Dutch\",\"tr\":\"Turkish\"},\"invited\":{\"sfox\":false,\"unocoin\":true},\"firebase_token\":\"clHNVTMogVM:APA91bHgQDPvGK6kUf-A6CVF_AkXWisek4LI67QoH1e5gAM70wnOYJ2EA029o4tKIHqR_7t3PF0z3mNUOYVdon3mqlau3P0EyRilsjXnWBZ6McpXnUEvU7skXR4q2FM8qd2ZwmBDsx6q\",\"country_code\":\"GB\",\"unsubscribed\":false,\"logging_level\":0,\"guid\":\"4750d125-5344-4b79-9cf9-6e3c97bc9523\",\"btc_currencies\":{\"BTC\":\"Bitcoin\",\"UBC\":\"Bits (uBTC)\",\"MBC\":\"MilliBit (mBTC)\"},\"currencies\":{\"CHF\":\"Swiss Franc\",\"ISK\":\"Icelandic Króna\",\"HKD\":\"Hong Kong Dollar\",\"TWD\":\"New Taiwan dollar\",\"EUR\":\"Euro\",\"DKK\":\"Danish Krone\",\"CLP\":\"Chilean Peso\",\"USD\":\"U.S. dollar\",\"CAD\":\"Canadian Dollar\",\"INR\":\"Indian Rupee\",\"CNY\":\"Chinese yuan\",\"THB\":\"Thai baht\",\"AUD\":\"Australian Dollar\",\"KRW\":\"South Korean Won\",\"SGD\":\"Singapore Dollar\",\"JPY\":\"Japanese Yen\",\"PLN\":\"Polish Zloty\",\"GBP\":\"Great British Pound\",\"SEK\":\"Swedish Krona\",\"NZD\":\"New Zealand Dollar\",\"BRL\":\"Brazil Real\",\"RUB\":\"Russian Ruble\"}}");
        subject = new SettingsManager(walletApi);
        subject.initSettings(guid, sharedKey);

        final TestObserver<Settings> testObserver = subject.getInfo().test();

        testObserver.assertComplete();
        testObserver.assertNoErrors();
        Settings settingsBody = testObserver.values().get(0);

        Assert.assertEquals("BTC", settingsBody.getBtcCurrency());
        Assert.assertEquals(0, settingsBody.getNotificationsType().size());
        Assert.assertEquals("en", settingsBody.getLanguage());
        Assert.assertFalse(settingsBody.isNotificationsOn());
        Assert.assertEquals(0, settingsBody.getIpLockOn());
        Assert.assertEquals("44", settingsBody.getDialCode());
        Assert.assertFalse(settingsBody.isBlockTorIps());
        Assert.assertEquals("USD", settingsBody.getCurrency());
        Assert.assertEquals(0, settingsBody.getNotificationsConfirmations());
        Assert.assertFalse(settingsBody.isAutoEmailBackup());
        Assert.assertFalse(settingsBody.isNeverSaveAuthType());
        Assert.assertEquals("john@mail.com", settingsBody.getEmail());
        Assert.assertFalse(settingsBody.isSmsVerified());
        Assert.assertFalse(settingsBody.isApiAccessEnabled());
        Assert.assertEquals(0, settingsBody.getAuthType());
        Assert.assertEquals("211.160.45.230", settingsBody.getMyIp());
        Assert.assertFalse(settingsBody.isEmailVerified());
        Assert.assertEquals("GB", settingsBody.getCountryCode());
        Assert.assertEquals(0, settingsBody.getLoggingLevel());
        Assert.assertEquals("4750d125-5344-4b79-9cf9-6e3c97bc9523", settingsBody.getGuid());
        Assert.assertFalse(settingsBody.getInvited().get("sfox"));
        Assert.assertTrue(settingsBody.getInvited().get("unocoin"));
    }
}