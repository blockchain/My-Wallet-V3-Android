package info.blockchain.wallet.payload;

import info.blockchain.wallet.LegacyAddressHelper;
import info.blockchain.wallet.WalletApiMockedResponseTest;
import info.blockchain.wallet.exceptions.HDWalletException;
import info.blockchain.wallet.exceptions.InvalidCredentialsException;
import info.blockchain.wallet.exceptions.ServerConnectionException;
import info.blockchain.wallet.exceptions.UnsupportedVersionException;
import info.blockchain.wallet.multiaddress.TransactionSummary;
import info.blockchain.wallet.multiaddress.TransactionSummary.Direction;
import info.blockchain.wallet.payload.data.Account;
import info.blockchain.wallet.payload.data.AddressLabel;
import info.blockchain.wallet.payload.data.LegacyAddress;
import info.blockchain.wallet.payload.data.Wallet;
import org.bitcoinj.core.Base58;
import org.bitcoinj.core.ECKey;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.crypto.DeterministicKey;
import org.bitcoinj.params.BitcoinMainNetParams;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.net.URI;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public final class PayloadManagerTest extends WalletApiMockedResponseTest {

    private NetworkParameters networkParameters = BitcoinMainNetParams.get();
    private PayloadManager payloadManager;

    @Before
    public void setup() {
        payloadManager = PayloadManager.getInstance();
    }

    @After
    public void tearDown() {
        payloadManager.wipe();
    }

    @Test
    public void getInstance() {
        Assert.assertNotNull(payloadManager);
    }

    @Test
    public void create() throws Exception {

        LinkedList<String> responseList = new LinkedList<>();
        responseList.add("MyWallet save successful.");
        //Responses for multi address, 'All' and individual xpub
        responseList.add("{}");//multiaddress responses - not testing this so can be empty.
        responseList.add("{}");
        responseList.add("{}");
        mockInterceptor.setResponseStringList(responseList);
        payloadManager.create("My HDWallet", "name@email.com", "SomePassword");

        Wallet walletBody = payloadManager
                .getPayload();

        Assert.assertEquals(36, walletBody.getGuid().length());//GUIDs are 36 in length
        Assert.assertEquals("My HDWallet", walletBody.getHdWallets().get(0).getAccounts().get(0).getLabel());

        Assert.assertEquals(1, walletBody.getHdWallets().get(0).getAccounts().size());

        Assert.assertEquals(5000, walletBody.getOptions().getPbkdf2Iterations());
        Assert.assertEquals(600000, walletBody.getOptions().getLogoutTime());
        Assert.assertEquals(10000, walletBody.getOptions().getFeePerKb());
    }

    @Test(expected = ServerConnectionException.class)
    public void create_ServerConnectionException() throws Exception {

        mockInterceptor.setResponseString("Save failed.");
        mockInterceptor.setResponseCode(500);
        payloadManager.create("My HDWallet", "name@email.com", "SomePassword");
    }

    @Test
    public void recoverFromMnemonic() throws Exception {

        String mnemonic = "all all all all all all all all all all all all";

        LinkedList<String> responseList = new LinkedList<>();
        //Responses for checking how many accounts to recover
        responseList.add(new String(Files.readAllBytes(Paths.get(getClass().getClassLoader().getResource(
                "balance/wallet_all_balance_1.txt").toURI())), Charset.forName("utf-8")));
        responseList.add(new String(Files.readAllBytes(Paths.get(getClass().getClassLoader().getResource(
                "balance/wallet_all_balance_2.txt").toURI())), Charset.forName("utf-8")));
        responseList.add(new String(Files.readAllBytes(Paths.get(getClass().getClassLoader().getResource(
                "balance/wallet_all_balance_3.txt").toURI())), Charset.forName("utf-8")));
        responseList.add("HDWallet successfully synced with server");

        //responses for initializing multi address
        responseList.add("{}");//multiaddress responses - not testing this so can be empty.
        responseList.add("{}");
        responseList.add("{}");
        responseList.add("{}");
        responseList.add("{}");
        responseList.add("{}");
        responseList.add("{}");
        responseList.add("{}");
        responseList.add("{}");
        responseList.add("{}");
        responseList.add("{}");
        responseList.add("{}");
        mockInterceptor.setResponseStringList(responseList);

        payloadManager.recoverFromMnemonic(mnemonic, "My HDWallet", "name@email.com", "SomePassword");

        Wallet walletBody = payloadManager
                .getPayload();

        Assert.assertEquals(36, walletBody.getGuid().length());//GUIDs are 36 in length
        Assert.assertEquals("My HDWallet", walletBody.getHdWallets().get(0).getAccounts().get(0).getLabel());
        Assert.assertEquals("0660cc198330660cc198330660cc1983", walletBody.getHdWallets().get(0).getSeedHex());

        Assert.assertEquals(10, walletBody.getHdWallets().get(0).getAccounts().size());

        Assert.assertEquals(5000, walletBody.getOptions().getPbkdf2Iterations());
        Assert.assertEquals(600000, walletBody.getOptions().getLogoutTime());
        Assert.assertEquals(10000, walletBody.getOptions().getFeePerKb());
    }

    @Test(expected = ServerConnectionException.class)
    public void recoverFromMnemonic_ServerConnectionException() throws Exception {

        String mnemonic = "all all all all all all all all all all all all";

        LinkedList<String> responseList = new LinkedList<>();
        responseList.add(new String(Files.readAllBytes(Paths.get(getClass().getClassLoader().getResource(
                "balance/wallet_all_balance_1.txt").toURI())), Charset.forName("utf-8")));
        responseList.add(new String(Files.readAllBytes(Paths.get(getClass().getClassLoader().getResource(
                "balance/wallet_all_balance_2.txt").toURI())), Charset.forName("utf-8")));
        responseList.add(new String(Files.readAllBytes(Paths.get(getClass().getClassLoader().getResource(
                "balance/wallet_all_balance_3.txt").toURI())), Charset.forName("utf-8")));
        responseList.add("Save failed");
        mockInterceptor.setResponseStringList(responseList);

        //checking if xpubs has txs succeeds but then savinf fails
        LinkedList<Integer> codes = new LinkedList<>();
        codes.add(200);
        codes.add(200);
        codes.add(200);
        codes.add(500);
        mockInterceptor.setResponseCodeList(codes);

        payloadManager.recoverFromMnemonic(mnemonic, "My HDWallet", "name@email.com", "SomePassword");

        Wallet walletBody = payloadManager
                .getPayload();

        Assert.assertEquals(36, walletBody.getGuid().length());//GUIDs are 36 in length
        Assert.assertEquals("My HDWallet", walletBody.getHdWallets().get(0).getAccounts().get(0).getLabel());
        Assert.assertEquals("0660cc198330660cc198330660cc1983", walletBody.getHdWallets().get(0).getSeedHex());

        Assert.assertEquals(10, walletBody.getHdWallets().get(0).getAccounts().size());

        Assert.assertEquals(5000, walletBody.getOptions().getPbkdf2Iterations());
        Assert.assertEquals(600000, walletBody.getOptions().getLogoutTime());
        Assert.assertEquals(10000, walletBody.getOptions().getFeePerKb());
    }

    @Test(expected = UnsupportedVersionException.class)
    public void initializeAndDecrypt_unsupported_version() throws Exception {

        URI uri = getClass().getClassLoader()
                .getResource("wallet_v4_unsupported.txt").toURI();
        String walletBase = new String(Files.readAllBytes(Paths.get(uri)),
                Charset.forName("utf-8"));

        mockInterceptor.setResponseString(walletBase);
        payloadManager.initializeAndDecrypt(networkParameters, "any_shared_key", "any_guid", "SomeTestPassword");
    }

    @Test
    public void initializeAndDecrypt() throws Exception {

        URI uri = getClass().getClassLoader().getResource("wallet_v3_3.txt").toURI();
        String walletBase = new String(Files.readAllBytes(Paths.get(uri)),
                Charset.forName("utf-8"));

        LinkedList<String> responseList = new LinkedList<>();
        responseList.add(walletBase);
        responseList.add("{}");//multiaddress responses - not testing this so can be empty.
        responseList.add("{}");
        responseList.add("{}");
        mockInterceptor.setResponseStringList(responseList);
        payloadManager.initializeAndDecrypt(networkParameters, "any", "any", "SomeTestPassword");
    }

    @Test(expected = InvalidCredentialsException.class)
    public void initializeAndDecrypt_invalidGuid() throws Exception {

        URI uri = getClass().getClassLoader().getResource("invalid_guid.txt").toURI();
        String walletBase = new String(Files.readAllBytes(Paths.get(uri)),
                Charset.forName("utf-8"));

        mockInterceptor.setResponseString(walletBase);
        mockInterceptor.setResponseCode(500);
        payloadManager.initializeAndDecrypt(networkParameters, "any", "any", "SomeTestPassword");
    }

    @Test(expected = HDWalletException.class)
    public void save_HDWalletException() throws Exception {
        //Nothing to save
        payloadManager.save();
    }

    @Test
    public void save() throws Exception {

        LinkedList<String> responseList = new LinkedList<>();
        responseList.add("MyWallet save successful.");
        responseList.add("{}");//multiaddress responses - not testing this so can be empty.
        responseList.add("{}");
        responseList.add("{}");
        mockInterceptor.setResponseStringList(responseList);
        payloadManager.create("My HDWallet", "name@email.com", "SomePassword");

        mockInterceptor.setResponseString("MyWallet save successful.");
        payloadManager.save();
    }

    @Test
    public void upgradeV2PayloadToV3() {
        //Tested in integration tests
    }

    @Test
    public void addAccount() throws Exception {

        LinkedList<String> responseList = new LinkedList<>();
        responseList.add("MyWallet save successful.");
        responseList.add("{}");//multiaddress responses - not testing this so can be empty.
        responseList.add("{}");
        responseList.add("{}");
        mockInterceptor.setResponseStringList(responseList);
        payloadManager.create("My HDWallet", "name@email.com", "MyTestWallet");

        Assert.assertEquals(1, payloadManager.getPayload().getHdWallets().get(0).getAccounts().size());

        responseList = new LinkedList<>();
        responseList.add("MyWallet save successful");
        responseList.add("{}");
        responseList.add("{}");
        responseList.add("{}");
        responseList.add("{}");
        mockInterceptor.setResponseStringList(responseList);
        payloadManager.addAccount(networkParameters, "Some Label", null);
        Assert.assertEquals(2, payloadManager.getPayload().getHdWallets().get(0).getAccounts().size());

        responseList = new LinkedList<>();
        responseList.add("MyWallet save successful");
        responseList.add("{}");
        responseList.add("{}");
        responseList.add("{}");
        responseList.add("{}");
        responseList.add("{}");
        mockInterceptor.setResponseStringList(responseList);
        payloadManager.addAccount(networkParameters, "Some Label", null);
        Assert.assertEquals(3, payloadManager.getPayload().getHdWallets().get(0).getAccounts().size());

    }

    @Test
    public void addLegacyAddress() throws Exception {

        LinkedList<String> responseList = new LinkedList<>();
        responseList.add("MyWallet save successful.");
        responseList.add("{}");//multiaddress responses - not testing this so can be empty.
        responseList.add("{}");
        responseList.add("{}");
        responseList.add("{}");
        mockInterceptor.setResponseStringList(responseList);
        payloadManager.create("My HDWallet", "name@email.com", "MyTestWallet");

        Assert.assertEquals(0, payloadManager.getPayload().getLegacyAddressList().size());

        responseList = new LinkedList<>();
        responseList.add("MyWallet save successful");
        responseList.add("{}");
        responseList.add("{}");
        responseList.add("{}");
        responseList.add("{}");
        mockInterceptor.setResponseStringList(responseList);
        payloadManager.addLegacyAddress(LegacyAddressHelper.getLegacyAddress());
        Assert.assertEquals(1, payloadManager.getPayload().getLegacyAddressList().size());

        responseList = new LinkedList<>();
        responseList.add("MyWallet save successful");
        responseList.add("{}");
        responseList.add("{}");
        responseList.add("{}");
        responseList.add("{}");
        responseList.add("{}");
        mockInterceptor.setResponseStringList(responseList);
        payloadManager.addLegacyAddress(LegacyAddressHelper.getLegacyAddress());
        Assert.assertEquals(2, payloadManager.getPayload().getLegacyAddressList().size());

    }

    @Test
    public void setKeyForLegacyAddress() throws Exception {

        LinkedList<String> responseList = new LinkedList<>();
        responseList.add("MyWallet save successful.");
        responseList.add("{}");//multiaddress responses - not testing this so can be empty.
        responseList.add("{}");
        responseList.add("{}");
        mockInterceptor.setResponseStringList(responseList);
        payloadManager.create("My HDWallet", "name@email.com", "MyTestWallet");

        Assert.assertEquals(0, payloadManager.getPayload().getLegacyAddressList().size());

        responseList = new LinkedList<>();
        responseList.add("MyWallet save successful");
        responseList.add("{}");
        responseList.add("{}");
        responseList.add("{}");
        responseList.add("{}");
        mockInterceptor.setResponseStringList(responseList);
        payloadManager.addLegacyAddress(LegacyAddressHelper.getLegacyAddress());
        Assert.assertEquals(1, payloadManager.getPayload().getLegacyAddressList().size());

        LegacyAddress legacyAddressBody = payloadManager.getPayload()
                .getLegacyAddressList().get(0);

        ECKey ecKey = DeterministicKey
                .fromPrivate(Base58.decode(legacyAddressBody.getPrivateKey()));


        legacyAddressBody.setPrivateKey(null);
        mockInterceptor.setResponseString("MyWallet save successful.");
        payloadManager.setKeyForLegacyAddress(ecKey, null);
    }

    @Test
    public void setKeyForLegacyAddress_NoSuchAddressException() throws Exception {

        LinkedList<String> responseList = new LinkedList<>();
        responseList.add("MyWallet save successful.");
        responseList.add("{}");//multiaddress responses - not testing this so can be empty.
        responseList.add("{}");
        responseList.add("{}");
        mockInterceptor.setResponseStringList(responseList);
        payloadManager.create("My HDWallet", "name@email.com", "MyTestWallet");

        Assert.assertEquals(0, payloadManager.getPayload().getLegacyAddressList().size());

        responseList = new LinkedList<>();
        responseList.add("MyWallet save successful");
        responseList.add("{}");
        responseList.add("{}");
        responseList.add("{}");
        responseList.add("{}");
        mockInterceptor.setResponseStringList(responseList);
        payloadManager.addLegacyAddress(LegacyAddressHelper.getLegacyAddress());
        Assert.assertEquals(1, payloadManager.getPayload().getLegacyAddressList().size());

        LegacyAddress existingLegacyAddressBody = payloadManager.getPayload()
                .getLegacyAddressList().get(0);

        //Try non matching ECKey
        ECKey ecKey = new ECKey();

        responseList = new LinkedList<>();
        responseList.add("MyWallet save successful");
        responseList.add("{}");
        responseList.add("{}");
        responseList.add("{}");
        responseList.add("{}");
        responseList.add("{}");
        mockInterceptor.setResponseStringList(responseList);

        LegacyAddress newlyAdded = payloadManager
                .setKeyForLegacyAddress(ecKey, null);

        //Ensure new address is created if no match found
        Assert.assertNotNull(newlyAdded);
        Assert.assertNotNull(newlyAdded.getPrivateKey());
        Assert.assertNotNull(newlyAdded.getAddress());
        Assert.assertNotEquals(existingLegacyAddressBody.getPrivateKey(), newlyAdded.getPrivateKey());
        Assert.assertNotEquals(existingLegacyAddressBody.getAddress(), newlyAdded.getAddress());
    }

    @Test
    public void setKeyForLegacyAddress_saveFail_revert() throws Exception {

        LinkedList<String> responseList = new LinkedList<>();
        responseList.add("MyWallet save successful.");
        responseList.add("{}");//multiaddress responses - not testing this so can be empty.
        responseList.add("{}");
        responseList.add("{}");
        mockInterceptor.setResponseStringList(responseList);
        payloadManager.create("My HDWallet", "name@email.com", "MyTestWallet");

        Assert.assertEquals(0, payloadManager.getPayload().getLegacyAddressList().size());

        responseList = new LinkedList<>();
        responseList.add("MyWallet save successful");
        responseList.add("{}");
        responseList.add("{}");
        responseList.add("{}");
        responseList.add("{}");
        mockInterceptor.setResponseStringList(responseList);
        payloadManager.addLegacyAddress(LegacyAddressHelper.getLegacyAddress());
        Assert.assertEquals(1, payloadManager.getPayload().getLegacyAddressList().size());

        LegacyAddress legacyAddressBody = payloadManager.getPayload()
                .getLegacyAddressList().get(0);

        ECKey ecKey = DeterministicKey
                .fromPrivate(Base58.decode(legacyAddressBody.getPrivateKey()));

        legacyAddressBody.setPrivateKey(null);
        mockInterceptor.setResponseCode(500);
        mockInterceptor.setResponseString("Oops something went wrong");
        payloadManager.setKeyForLegacyAddress(ecKey, null);

        //Ensure private key reverted on save fail
        Assert.assertNull(legacyAddressBody.getPrivateKey());
    }

    @Test
    public void getNextAddress() throws Exception {

        URI uri = getClass().getClassLoader().getResource("wallet_v3_5.txt").toURI();
        String walletBase = new String(Files.readAllBytes(Paths.get(uri)), Charset.forName("utf-8"));

        LinkedList<String> responseList = new LinkedList<>();
        responseList.add(walletBase);
        responseList.add("{}");
        responseList.add(new String(Files.readAllBytes(Paths.get(getClass().getClassLoader().getResource(
                "multiaddress/wallet_v3_5_m1.txt").toURI())), Charset.forName("utf-8")));
        responseList.add(new String(Files.readAllBytes(Paths.get(getClass().getClassLoader().getResource(
                "multiaddress/wallet_v3_5_m2.txt").toURI())), Charset.forName("utf-8")));
        responseList.add(new String(Files.readAllBytes(Paths.get(getClass().getClassLoader().getResource(
                "multiaddress/wallet_v3_5_m3.txt").toURI())), Charset.forName("utf-8")));
        responseList.add(new String(Files.readAllBytes(Paths.get(getClass().getClassLoader().getResource(
                "multiaddress/wallet_v3_5_m4.txt").toURI())), Charset.forName("utf-8")));
        mockInterceptor.setResponseStringList(responseList);

        payloadManager.initializeAndDecrypt(networkParameters, "06f6fa9c-d0fe-403d-815a-111ee26888e2", "4750d125-5344-4b79-9cf9-6e3c97bc9523", "MyTestWallet");

        Wallet wallet = payloadManager.getPayload();

        //Reserve an address to ensure it gets skipped
        List<AddressLabel> labelList = new ArrayList<>();
        labelList.add(AddressLabel.fromJson("{\n"
                + "              \"index\": 1,\n"
                + "              \"label\": \"Reserved\"\n"
                + "            }"));

        Account account = wallet.getHdWallets().get(0).getAccounts().get(0);
        account.setAddressLabels(labelList);

        //set up indexes first
        payloadManager.getAccountTransactions(account.getXpub(), 50, 0);

        //Next Receive
        String nextReceiveAddress = payloadManager.getNextReceiveAddress(account);
        Assert.assertEquals("1H9FdkaryqzB9xacDbJrcjXsJ9By4UVbQw", nextReceiveAddress);

        //Increment receive and check
        payloadManager.incrementNextReceiveAddress(account);
        nextReceiveAddress = payloadManager.getNextReceiveAddress(account);
        Assert.assertEquals("18DU2RjyadUmRK7sHTBHtbJx5VcwthHyF7", nextReceiveAddress);

        //Next Change
        String nextChangeAddress = payloadManager.getNextChangeAddress(account);
        Assert.assertEquals("1GEXfMa4SMh3iUZxP8HHQy7Wo3aqce72Nm", nextChangeAddress);

        //Increment Change and check
        payloadManager.incrementNextChangeAddress(account);
        nextChangeAddress = payloadManager.getNextChangeAddress(account);
        Assert.assertEquals("1NzpLHV6LLVFCYdYA5woYL9pHJ48KQJc9K", nextChangeAddress);
    }

    @Test
    public void balance() throws Exception {
        URI uri = getClass().getClassLoader().getResource("wallet_v3_6.txt").toURI();
        String walletBase = new String(Files.readAllBytes(Paths.get(uri)), Charset.forName("utf-8"));

        LinkedList<String> responseList = new LinkedList<>();
        responseList.add(walletBase);
        // Bitcoin
        responseList.add(new String(Files.readAllBytes(Paths.get(getClass().getClassLoader().getResource(
                "balance/wallet_v3_6_balance.txt").toURI())), Charset.forName("utf-8")));
        // Bitcoin Cash
        responseList.add(new String(Files.readAllBytes(Paths.get(getClass().getClassLoader().getResource(
                "balance/wallet_v3_6_balance.txt").toURI())), Charset.forName("utf-8")));

        mockInterceptor.setResponseStringList(responseList);

        payloadManager.initializeAndDecrypt(networkParameters, "any", "any", "MyTestWallet");

        //'All' wallet balance and transactions
        Assert.assertEquals(743071, payloadManager.getWalletBalance().longValue());

        //Imported addresses consolidated
        Assert.assertEquals(137505, payloadManager.getImportedAddressesBalance().longValue());

        //Account and address balances
        String first = "xpub6CdH6yzYXhTtR7UHJHtoTeWm3nbuyg9msj3rJvFnfMew9CBff6Rp62zdTrC57Spz4TpeRPL8m9xLiVaddpjEx4Dzidtk44rd4N2xu9XTrSV";
        Assert.assertEquals(566349, payloadManager.getAddressBalance(first).longValue());

        String second = "xpub6CdH6yzYXhTtTGPPL4Djjp1HqFmAPx4uyqoG6Ffz9nPysv8vR8t8PEJ3RGaSRwMm7kRZ3MAcKgB6u4g1znFo82j4q2hdShmDyw3zuMxhDSL";
        Assert.assertEquals(39217, payloadManager.getAddressBalance(second).longValue());

        String third = "189iKJLruPtUorasDuxmc6fMRVxz6zxpPS";
        Assert.assertEquals(137505, payloadManager.getAddressBalance(third).longValue());
    }

    @Test
    public void getAccountTransactions() throws Exception {
        //guid 5350e5d5-bd65-456f-b150-e6cc089f0b26
        URI uri = getClass().getClassLoader().getResource("wallet_v3_6.txt").toURI();
        String walletBase = new String(Files.readAllBytes(Paths.get(uri)), Charset.forName("utf-8"));

        LinkedList<String> responseList = new LinkedList<>();
        responseList.add(walletBase);
        // Bitcoin
        responseList.add(new String(Files.readAllBytes(Paths.get(getClass().getClassLoader().getResource(
                "balance/wallet_v3_6_balance.txt").toURI())), Charset.forName("utf-8")));
        // Bitcoin cash
        responseList.add(new String(Files.readAllBytes(Paths.get(getClass().getClassLoader().getResource(
                "balance/wallet_v3_6_balance.txt").toURI())), Charset.forName("utf-8")));
        // Bitcoin
        responseList.add(new String(Files.readAllBytes(Paths.get(getClass().getClassLoader().getResource(
                "multiaddress/wallet_v3_6_m1.txt").toURI())), Charset.forName("utf-8")));
        // Bitcoin cash
        responseList.add(new String(Files.readAllBytes(Paths.get(getClass().getClassLoader().getResource(
                "multiaddress/wallet_v3_6_m1.txt").toURI())), Charset.forName("utf-8")));
        mockInterceptor.setResponseStringList(responseList);

        payloadManager.initializeAndDecrypt(networkParameters, "0f28735d-0b89-405d-a40f-ee3e85c3c78c", "5350e5d5-bd65-456f-b150-e6cc089f0b26", "MyTestWallet");

        //Account 1
        String first = "xpub6CdH6yzYXhTtR7UHJHtoTeWm3nbuyg9msj3rJvFnfMew9CBff6Rp62zdTrC57Spz4TpeRPL8m9xLiVaddpjEx4Dzidtk44rd4N2xu9XTrSV";
        mockInterceptor.setResponseString(new String(Files.readAllBytes(Paths.get(getClass().getClassLoader().getResource(
                "multiaddress/wallet_v3_6_m2.txt").toURI())), Charset.forName("utf-8")));

        List<TransactionSummary> transactionSummaries = payloadManager
                .getAccountTransactions(first, 50, 0);
        Assert.assertEquals(8, transactionSummaries.size());
        TransactionSummary summary = transactionSummaries.get(0);
        Assert.assertEquals(68563, summary.getTotal().longValue());
        Assert.assertEquals(Direction.TRANSFERRED, summary.getDirection());
        Assert.assertEquals(1, summary.getInputsMap().size());
        Assert.assertTrue(summary.getInputsMap().keySet().contains("125QEfWq3eKzAQQHeqcMcDMeZGm13hVRvU"));//My Bitcoin Account
        Assert.assertEquals(2, summary.getOutputsMap().size());
        Assert.assertTrue(summary.getOutputsMap().keySet().contains("1Nm1yxXCTodAkQ9RAEquVdSneJGeubqeTw"));//Savings account
        Assert.assertTrue(summary.getOutputsMap().keySet().contains("189iKJLruPtUorasDuxmc6fMRVxz6zxpPS"));

        summary = transactionSummaries.get(1);
        Assert.assertEquals(138068, summary.getTotal().longValue());
        Assert.assertEquals(Direction.SENT, summary.getDirection());
        Assert.assertEquals(1, summary.getInputsMap().size());
        Assert.assertTrue(summary.getInputsMap().keySet().contains("1CQpuTQrJQLW6PEar17zsd9EV14cZknqWJ"));//My Bitcoin Wallet
        Assert.assertEquals(2, summary.getOutputsMap().size());
        Assert.assertTrue(summary.getOutputsMap().keySet().contains("1LQwNvEMnYjNCNxeUJzDfD8mcSqhm2ouPp"));
        Assert.assertTrue(summary.getOutputsMap().keySet().contains("1AdTcerDBY735kDhQWit5Scroae6piQ2yw"));

        summary = transactionSummaries.get(2);
        Assert.assertEquals(800100, summary.getTotal().longValue());
        Assert.assertEquals(Direction.RECEIVED, summary.getDirection());
        Assert.assertEquals(1, summary.getInputsMap().size());
        Assert.assertTrue(summary.getInputsMap().keySet().contains("19CMnkUgBnTBNiTWXwoZr6Gb3aeXKHvuGG"));
        Assert.assertEquals(1, summary.getOutputsMap().size());
        Assert.assertTrue(summary.getOutputsMap().keySet().contains("1CQpuTQrJQLW6PEar17zsd9EV14cZknqWJ"));//My Bitcoin Wallet

        summary = transactionSummaries.get(3);
        Assert.assertEquals(35194, summary.getTotal().longValue());
        Assert.assertEquals(Direction.SENT, summary.getDirection());
        Assert.assertEquals(1, summary.getInputsMap().size());
        Assert.assertTrue(summary.getInputsMap().keySet().contains("15HjFY96ZANBkN5kvPRgrXH93jnntqs32n"));//My Bitcoin Wallet
        Assert.assertEquals(1, summary.getOutputsMap().size());
        Assert.assertTrue(summary.getOutputsMap().keySet().contains("1PQ9ZYhv9PwbWQQN74XRqUCjC32JrkyzB9"));

        summary = transactionSummaries.get(4);
        Assert.assertEquals(98326, summary.getTotal().longValue());
        Assert.assertEquals(Direction.TRANSFERRED, summary.getDirection());
        Assert.assertEquals(1, summary.getInputsMap().size());
        Assert.assertTrue(summary.getInputsMap().keySet().contains("1Peysd3qYDe35yNp6KB1ZkbVYHr42JT9zZ"));//My Bitcoin Wallet
        Assert.assertEquals(1, summary.getOutputsMap().size());
        Assert.assertTrue(summary.getOutputsMap().keySet().contains("189iKJLruPtUorasDuxmc6fMRVxz6zxpPS"));

        summary = transactionSummaries.get(5);
        Assert.assertEquals(160640, summary.getTotal().longValue());
        Assert.assertEquals(Direction.RECEIVED, summary.getDirection());
        Assert.assertEquals(1, summary.getInputsMap().size());
        Assert.assertTrue(summary.getInputsMap().keySet().contains("1BZe6YLaf2HiwJdnBbLyKWAqNia7foVe1w"));
        Assert.assertEquals(1, summary.getOutputsMap().size());
        Assert.assertTrue(summary.getOutputsMap().keySet().contains("1Peysd3qYDe35yNp6KB1ZkbVYHr42JT9zZ"));//My Bitcoin Wallet

        summary = transactionSummaries.get(6);
        Assert.assertEquals(9833, summary.getTotal().longValue());
        Assert.assertEquals(Direction.TRANSFERRED, summary.getDirection());
        Assert.assertEquals(1, summary.getInputsMap().size());
        Assert.assertTrue(summary.getInputsMap().keySet().contains("17ijgwpGsVQRzMjsdAfdmeP53kpw9yvXur"));//My Bitcoin Wallet
        Assert.assertEquals(1, summary.getOutputsMap().size());
        Assert.assertTrue(summary.getOutputsMap().keySet().contains("1AtunWT3F6WvQc3aaPuPbNGeBpVF3ZPM5r"));//Savings account

        summary = transactionSummaries.get(7);
        Assert.assertEquals(40160, summary.getTotal().longValue());
        Assert.assertEquals(Direction.RECEIVED, summary.getDirection());
        Assert.assertEquals(1, summary.getInputsMap().size());
        Assert.assertTrue(summary.getInputsMap().keySet().contains("1Baa1cjB1CyBVSjw8SkFZ2YBuiwKnKLXhe"));
        Assert.assertEquals(1, summary.getOutputsMap().size());
        Assert.assertTrue(summary.getOutputsMap().keySet().contains("17ijgwpGsVQRzMjsdAfdmeP53kpw9yvXur"));//My Bitcoin Wallet

        //Account 2
        String second = "xpub6CdH6yzYXhTtTGPPL4Djjp1HqFmAPx4uyqoG6Ffz9nPysv8vR8t8PEJ3RGaSRwMm7kRZ3MAcKgB6u4g1znFo82j4q2hdShmDyw3zuMxhDSL";
        mockInterceptor.setResponseString(new String(Files.readAllBytes(Paths.get(getClass().getClassLoader().getResource(
                "multiaddress/wallet_v3_6_m3.txt").toURI())), Charset.forName("utf-8")));
        transactionSummaries = payloadManager
                .getAccountTransactions(second, 50, 0);
        Assert.assertEquals(2, transactionSummaries.size());
        summary = transactionSummaries.get(0);
        Assert.assertEquals(68563, summary.getTotal().longValue());
        Assert.assertEquals(Direction.TRANSFERRED, summary.getDirection());
        Assert.assertEquals(1, summary.getInputsMap().size());
        Assert.assertTrue(summary.getInputsMap().keySet().contains("125QEfWq3eKzAQQHeqcMcDMeZGm13hVRvU"));//My Bitcoin Wallet
        Assert.assertEquals(2, summary.getOutputsMap().size());
        Assert.assertTrue(summary.getOutputsMap().keySet().contains("1Nm1yxXCTodAkQ9RAEquVdSneJGeubqeTw"));//Savings account
        Assert.assertTrue(summary.getOutputsMap().keySet().contains("189iKJLruPtUorasDuxmc6fMRVxz6zxpPS"));

        summary = transactionSummaries.get(1);
        Assert.assertEquals(9833, summary.getTotal().longValue());
        Assert.assertEquals(Direction.TRANSFERRED, summary.getDirection());
        Assert.assertEquals(1, summary.getInputsMap().size());
        Assert.assertTrue(summary.getInputsMap().keySet().contains("17ijgwpGsVQRzMjsdAfdmeP53kpw9yvXur"));//My Bitcoin Wallet
        Assert.assertEquals(1, summary.getOutputsMap().size());
        Assert.assertTrue(summary.getOutputsMap().keySet().contains("1AtunWT3F6WvQc3aaPuPbNGeBpVF3ZPM5r"));//Savings account

        //Imported addresses (consolidated)
        mockInterceptor.setResponseString(new String(Files.readAllBytes(Paths.get(getClass().getClassLoader().getResource(
                "multiaddress/wallet_v3_6_m1.txt").toURI())), Charset.forName("utf-8")));
        transactionSummaries = payloadManager.getImportedAddressesTransactions(50, 0);
        Assert.assertEquals(2, transactionSummaries.size());

        summary = transactionSummaries.get(0);
        Assert.assertEquals(2, transactionSummaries.size());
        Assert.assertEquals(68563, summary.getTotal().longValue());
        Assert.assertEquals(Direction.TRANSFERRED, summary.getDirection());
        Assert.assertEquals(1, summary.getInputsMap().size());
        Assert.assertTrue(summary.getInputsMap().keySet().contains("125QEfWq3eKzAQQHeqcMcDMeZGm13hVRvU"));//My Bitcoin Wallet
        Assert.assertEquals(2, summary.getOutputsMap().size());
        Assert.assertTrue(summary.getOutputsMap().keySet().contains("1Nm1yxXCTodAkQ9RAEquVdSneJGeubqeTw"));//Savings account
        Assert.assertTrue(summary.getOutputsMap().keySet().contains("189iKJLruPtUorasDuxmc6fMRVxz6zxpPS"));

        summary = transactionSummaries.get(1);
        Assert.assertEquals(98326, summary.getTotal().longValue());
        Assert.assertEquals(Direction.TRANSFERRED, summary.getDirection());
        Assert.assertEquals(1, summary.getInputsMap().size());
        Assert.assertTrue(summary.getInputsMap().keySet().contains("1Peysd3qYDe35yNp6KB1ZkbVYHr42JT9zZ"));//My Bitcoin Wallet
        Assert.assertEquals(1, summary.getOutputsMap().size());
        Assert.assertTrue(summary.getOutputsMap().keySet().contains("189iKJLruPtUorasDuxmc6fMRVxz6zxpPS"));
    }
}