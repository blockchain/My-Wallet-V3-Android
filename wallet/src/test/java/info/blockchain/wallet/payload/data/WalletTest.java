package info.blockchain.wallet.payload.data;

import info.blockchain.api.data.UnspentOutputs;
import info.blockchain.wallet.WalletApiMockedResponseTest;
import info.blockchain.wallet.crypto.AESUtil;
import info.blockchain.wallet.exceptions.DecryptionException;
import info.blockchain.wallet.exceptions.HDWalletException;
import info.blockchain.wallet.exceptions.NoSuchAddressException;
import info.blockchain.wallet.payment.Payment;
import info.blockchain.wallet.payment.SpendableUnspentOutputs;
import info.blockchain.wallet.util.DoubleEncryptionFactory;
import org.bitcoinj.core.Base58;
import org.bitcoinj.core.ECKey;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.crypto.DeterministicKey;
import org.bitcoinj.params.BitcoinMainNetParams;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.math.BigInteger;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map.Entry;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/*
WalletBase
    |
    |__WalletWrapper
            |
            |__Wallet
 */
public final class WalletTest extends WalletApiMockedResponseTest {

    private NetworkParameters networkParameters = BitcoinMainNetParams.get();

    private Wallet givenWalletFromResouce(String resourceName) {
        try {
            return Wallet.fromJson(networkParameters, getResourceContent(resourceName));
        } catch (HDWalletException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    private String getResourceContent(String resourceName) {
        try {
            URL resource = getClass().getClassLoader().getResource(resourceName);
            assertNotNull(resource);
            URI uri = resource.toURI();
            return new String(Files.readAllBytes(Paths.get(uri)), Charset.forName("utf-8"));
        } catch (URISyntaxException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void fromJson_1() {
        String resourceName = "wallet_body_1.txt";
        Wallet wallet = givenWalletFromResouce(resourceName);
        assertEquals("a09910d9-1906-4ea1-a956-2508c3fe0661", wallet.getGuid());
        assertEquals("d14f3d2c-f883-40da-87e2-c8448521ee64", wallet.getSharedKey());
        Assert.assertTrue(wallet.isDoubleEncryption());
        assertEquals("1f7cb884545e89e4083c10522bf8b991e8e13551aa5816110cb9419277fb4652", wallet.getDpasswordhash());

        for (Entry<String, String> item : wallet.getTxNotes().entrySet()) {
            assertEquals("94a4934712fd40f2b91b7be256eacad49a50b850c949313b07046664d24c0e4c", item.getKey());
            assertEquals("Bought Pizza", item.getValue());
        }

        //Options parsing tested in OptionsTest
        assertNotNull(wallet.getOptions());

        //HdWallets parsing tested in HdWalletsBodyTest
        assertNotNull(wallet.getHdWallets());

        //Keys parsing tested in KeysBodyTest
        assertNotNull(wallet.getLegacyAddressList());

        //AddressBook parsing tested in AddressBookTest
        assertNotNull(wallet.getAddressBook());
    }

    @Test
    public void fromJson_2() {

        Wallet wallet = givenWalletFromResouce("wallet_body_2.txt");
        assertEquals("9ebb4d4f-f36e-40d6-9a3e-5a3cca5f83d6", wallet.getGuid());
        assertEquals("41cf823f-2dcd-4967-88d1-ef9af8689fc6", wallet.getSharedKey());
        assertFalse(wallet.isDoubleEncryption());
        Assert.assertNull(wallet.getDpasswordhash());

        //Options parsing tested in OptionsTest
        assertNotNull(wallet.getOptions());

        //Keys parsing tested in KeysBodyTest
        assertNotNull(wallet.getLegacyAddressList());
    }

    @Test
    public void fromJson_3() {

        Wallet wallet = givenWalletFromResouce("wallet_body_3.txt");
        assertEquals("2ca9b0e4-6b82-4dae-9fef-e8b300c72aa2", wallet.getGuid());
        assertEquals("e8553981-b196-47cc-8858-5b0d16284f61", wallet.getSharedKey());
        assertFalse(wallet.isDoubleEncryption());
        Assert.assertNull(wallet.getDpasswordhash());

        //Options parsing tested in OptionsTest
        assertNotNull(wallet.getWalletOptions());//very old key for options
        assertEquals(10, wallet.getWalletOptions().getPbkdf2Iterations());

        //old wallet_options should have created new options
        assertNotNull(wallet.getOptions());
        assertEquals(10, wallet.getOptions().getPbkdf2Iterations());

        //Keys parsing tested in KeysBodyTest
        assertNotNull(wallet.getLegacyAddressList());
    }

    @Test
    public void fromJson_4() {

        Wallet wallet = givenWalletFromResouce("wallet_body_4.txt");
        assertEquals("4077b6d9-73b3-4d22-96d4-9f8810fec435", wallet.getGuid());
        assertEquals("fa1beb37-5836-41d1-9f73-09f292076eb9", wallet.getSharedKey());
    }

    @Test
    public void testToJSON() throws Exception {

        //Ensure toJson doesn't write any unintended fields
        Wallet wallet = givenWalletFromResouce("wallet_body_1.txt");
        String jsonString = wallet.toJson();

        JSONObject jsonObject = new JSONObject(jsonString);
        assertEquals(10, jsonObject.keySet().size());
    }

    @Test
    public void validateSecondPassword() throws Exception {

        Wallet wallet = givenWalletFromResouce("wallet_body_1.txt");

        wallet.validateSecondPassword("hello");
        Assert.assertTrue(true);
    }

    @Test(expected = DecryptionException.class)
    public void validateSecondPassword_fail() throws Exception {

        Wallet wallet = givenWalletFromResouce("wallet_body_1.txt");

        wallet.validateSecondPassword("bogus");
    }

    @Test
    public void addAccount() throws Exception {

        Wallet wallet = givenWalletFromResouce("wallet_body_6.txt");

        assertEquals(1, wallet.getHdWallets().get(0).getAccounts().size());
        wallet.addAccount(networkParameters, 0, "Some Label", null);
        assertEquals(2, wallet.getHdWallets().get(0).getAccounts().size());

        Account account = wallet.getHdWallets().get(0)
                .getAccount(wallet.getHdWallets().get(0).getAccounts().size() - 1);

        assertEquals("xpub6DTFzKMsjf1Tt9KwHMYnQxMLGuVRcobDZdzDuhtc6xfvafsBFqsBS4RNM54kdJs9zK8RKkSbjSbwCeUJjxiySaBKTf8dmyXgUgVnFY7yS9x", account.getXpub());
        assertEquals("xprv9zTuaopyuHTAffFUBL1n3pQbisewDLsNCR4d7KUzYd8whsY2iJYvtG6tVp1c3jRU4euNj3qdb6wCrmCwg1JRPfPghmH3hJ5ubRJVmqMGwyy", account.getXpriv());
    }

    @Test(expected = DecryptionException.class)
    public void addAccount_doubleEncryptionError() throws Exception {

        Wallet wallet = givenWalletFromResouce("wallet_body_6.txt");

        assertEquals(1, wallet.getHdWallets().get(0).getAccounts().size());
        wallet.addAccount(networkParameters, 0, "Some Label", "hello");
    }

    @Test
    public void addAccount_doubleEncrypted() throws Exception {

        Wallet wallet = givenWalletFromResouce("wallet_body_7.txt");

        assertEquals(2, wallet.getHdWallets().get(0).getAccounts().size());
        wallet.addAccount(networkParameters, 0, "Some Label", "hello");
        assertEquals(3, wallet.getHdWallets().get(0).getAccounts().size());

        Account account = wallet.getHdWallets().get(0)
                .getAccount(wallet.getHdWallets().get(0).getAccounts().size() - 1);

        assertEquals("xpub6DEe2bJAU7GbUw3HDGPUY9c77mUcP9xvAWEhx9GReuJM9gppeGxHqBcaYAfrsyY8R6cfVRsuFhi2PokQFYLEQBVpM8p4MTLzEHpVu4SWq9a", account.getXpub());

        //Private key will be encrypted
        String decryptedXpriv = DoubleEncryptionFactory.decrypt(
                account.getXpriv(), wallet.getSharedKey(), "hello",
                wallet.getOptions().getPbkdf2Iterations());
        assertEquals("xprv9zFHd5mGdjiJGSxp7ErUB1fNZje7yhF4oHK79krp6ZmNGtVg6je3HPJ6gueSWrVR9oqdqriu2DcshvTfSRu6PXyWiAbP8n6S7DVWEpu5kAE", decryptedXpriv);
    }

    @Test
    public void addLegacyAddress()
            throws Exception {

        Wallet wallet = givenWalletFromResouce("wallet_body_6.txt");

        assertEquals(0, wallet.getLegacyAddressList().size());
        mockInterceptor.setResponseString("cb600366ef7a94b991aa04557fc1d9c272ba00df6b1d9791d71c66efa0ae7fe9");
        wallet.addLegacyAddress("Some Label", null);
        assertEquals(1, wallet.getLegacyAddressList().size());

        LegacyAddress address = wallet.getLegacyAddressList().get(wallet.getLegacyAddressList().size() - 1);

        assertNotNull(address.getPrivateKey());
        assertNotNull(address.getAddress());

        assertEquals("1", address.getAddress().substring(0, 1));
    }

    @Test
    public void addLegacyAddress_doubleEncrypted()
            throws Exception {

        Wallet wallet = givenWalletFromResouce("wallet_body_1.txt");

        assertEquals(19, wallet.getLegacyAddressList().size());
        mockInterceptor.setResponseString("cb600366ef7a94b991aa04557fc1d9c272ba00df6b1d9791d71c66efa0ae7fe9");
        wallet.addLegacyAddress("Some Label", "hello");
        assertEquals(20, wallet.getLegacyAddressList().size());

        LegacyAddress address = wallet.getLegacyAddressList().get(wallet.getLegacyAddressList().size() - 1);

        assertNotNull(address.getPrivateKey());
        assertNotNull(address.getAddress());

        assertEquals("==", address.getPrivateKey().substring(address.getPrivateKey().length() - 2));
        assertEquals("1", address.getAddress().substring(0, 1));
    }

    @Test
    public void setKeyForLegacyAddress()
            throws Exception {

        Wallet wallet = givenWalletFromResouce("wallet_body_6.txt");

        mockInterceptor.setResponseString("cb600366ef7a94b991aa04557fc1d9c272ba00df6b1d9791d71c66efa0ae7fe9");
        wallet.addLegacyAddress("Some Label", null);

        LegacyAddress address = wallet.getLegacyAddressList().get(wallet.getLegacyAddressList().size() - 1);

        ECKey ecKey = DeterministicKey.fromPrivate(Base58.decode(address.getPrivateKey()));

        wallet.setKeyForLegacyAddress(ecKey, null);
    }

    @Test(expected = NoSuchAddressException.class)
    public void setKeyForLegacyAddress_NoSuchAddressException()
            throws Exception {

        Wallet wallet = givenWalletFromResouce("wallet_body_6.txt");

        mockInterceptor.setResponseString("cb600366ef7a94b991aa04557fc1d9c272ba00df6b1d9791d71c66efa0ae7fe9");
        wallet.addLegacyAddress("Some Label", null);

        LegacyAddress address = wallet.getLegacyAddressList().get(wallet.getLegacyAddressList().size() - 1);

        //Try to set address key with ECKey not found in available addresses.
        ECKey ecKey = new ECKey();
        wallet.setKeyForLegacyAddress(ecKey, null);
    }

    @Test
    public void setKeyForLegacyAddress_doubleEncrypted()
            throws Exception {

        Wallet wallet = givenWalletFromResouce("wallet_body_1.txt");

        mockInterceptor.setResponseString("cb600366ef7a94b991aa04557fc1d9c272ba00df6b1d9791d71c66efa0ae7fe9");
        wallet.addLegacyAddress("Some Label", "hello");

        LegacyAddress address = wallet.getLegacyAddressList().get(wallet.getLegacyAddressList().size() - 1);

        final String decryptedOriginalPrivateKey = AESUtil
                .decrypt(address.getPrivateKey(), wallet.getSharedKey() + "hello",
                        wallet.getOptions().getPbkdf2Iterations());

        //Remove private key so we can set it again
        address.setPrivateKey(null);

        //Same key for created address, but unencrypted
        ECKey ecKey = DeterministicKey.fromPrivate(Base58.decode(decryptedOriginalPrivateKey));

        //Set private key
        wallet.setKeyForLegacyAddress(ecKey, "hello");

        //Get new set key
        address = wallet.getLegacyAddressList().get(wallet.getLegacyAddressList().size() - 1);
        String decryptedSetPrivateKey = AESUtil
                .decrypt(address.getPrivateKey(), wallet.getSharedKey() + "hello",
                        wallet.getOptions().getPbkdf2Iterations());

        //Original private key must match newly set private key (unencrypted)
        assertEquals(decryptedOriginalPrivateKey, decryptedSetPrivateKey);
    }

    @Test(expected = DecryptionException.class)
    public void setKeyForLegacyAddress_DecryptionException()
            throws Exception {

        Wallet wallet = givenWalletFromResouce("wallet_body_1.txt");

        mockInterceptor.setResponseString("cb600366ef7a94b991aa04557fc1d9c272ba00df6b1d9791d71c66efa0ae7fe9");
        wallet.addLegacyAddress("Some Label", "hello");

        LegacyAddress address = wallet.getLegacyAddressList().get(wallet.getLegacyAddressList().size() - 1);

        final String decryptedOriginalPrivateKey = AESUtil
                .decrypt(address.getPrivateKey(), wallet.getSharedKey() + "hello",
                        wallet.getOptions().getPbkdf2Iterations());

        //Remove private key so we can set it again
        address.setPrivateKey(null);

        //Same key for created address, but unencrypted
        ECKey ecKey = DeterministicKey.fromPrivate(Base58.decode(decryptedOriginalPrivateKey));

        //Set private key
        wallet.setKeyForLegacyAddress(ecKey, "bogus");
    }

    @Test
    public void decryptHDWallet() throws Exception {
        Wallet wallet = givenWalletFromResouce("wallet_body_1.txt");
        wallet.decryptHDWallet(networkParameters, 0, "hello");
    }

    @Test(expected = DecryptionException.class)
    public void decryptHDWallet_DecryptionException() throws Exception {
        Wallet wallet = givenWalletFromResouce("wallet_body_1.txt");
        wallet.decryptHDWallet(networkParameters, 0, "bogus");
    }

    @Test
    public void getMasterKey() throws Exception {
        Wallet wallet = givenWalletFromResouce("wallet_body_1.txt");
        wallet.decryptHDWallet(networkParameters, 0, "hello");
        assertEquals("4NPYyXS5fhyoTHgDPt81cQ4838j1tRwmeRbK8pGLB1Xg",
                Base58.encode(wallet.getHdWallets().get(0).getMasterKey().getPrivKeyBytes()));
    }

    @Test(expected = DecryptionException.class)
    public void getMasterKey_DecryptionException() throws Exception {
        Wallet wallet = givenWalletFromResouce("wallet_body_1.txt");
        wallet.decryptHDWallet(networkParameters, 0, "bogus");
        wallet.getHdWallets().get(0).getMasterKey();
    }

    @Test
    public void getMnemonic() throws Exception {
        Wallet wallet = givenWalletFromResouce("wallet_body_1.txt");
        wallet.decryptHDWallet(networkParameters, 0, "hello");
        assertEquals("[car, region, outdoor, punch, poverty, shadow, insane, claim, one, whisper, learn, alert]",
                wallet.getHdWallets().get(0).getMnemonic().toString());
    }

    @Test(expected = DecryptionException.class)
    public void getMnemonic_DecryptionException() throws Exception {
        Wallet wallet = givenWalletFromResouce("wallet_body_1.txt");
        wallet.decryptHDWallet(networkParameters, 0, "bogus");
        wallet.getHdWallets().get(0).getMnemonic();
    }

    @Test
    public void getHDKeysForSigning() throws Exception {
        Wallet wallet = givenWalletFromResouce("wallet_body_1.txt");

        /*
        8 available Payment. [80200,70000,60000,50000,40000,30000,20000,10000]
         */
        UnspentOutputs unspentOutputs = UnspentOutputs.fromJson(getResourceContent("wallet_body_1_account1_unspent.txt"));

        Payment payment = new Payment();

        long spendAmount = 80200L + 70000L + 60000L + 50000L + 40000L + 30000L + 20000L + 10000L - Payment.DUST.longValue();
        long feeManual = Payment.DUST.longValue();

        SpendableUnspentOutputs paymentBundle = payment
                .getSpendableCoins(unspentOutputs, BigInteger.valueOf(spendAmount - feeManual),
                        BigInteger.valueOf(30000L));

        wallet.decryptHDWallet(networkParameters, 0, "hello");
        List<ECKey> keyList = wallet.getHdWallets().get(0)
                .getHDKeysForSigning(wallet.getHdWallets().get(0).getAccount(0), paymentBundle);

        //Contains 5 matching keys for signing
        assertEquals(5, keyList.size());
    }

    @Test
    public void createNewWallet() throws Exception {

        String label = "HDAccount 1";
        Wallet payload = new Wallet(label);

        assertEquals(36, payload.getGuid().length());//GUIDs are 36 in length
        assertEquals(label, payload.getHdWallets().get(0).getAccounts().get(0).getLabel());

        assertEquals(1, payload.getHdWallets().get(0).getAccounts().size());

        assertEquals(5000, payload.getOptions().getPbkdf2Iterations());
        assertEquals(600000, payload.getOptions().getLogoutTime());
        assertEquals(10000, payload.getOptions().getFeePerKb());
    }
}
