package info.blockchain.wallet.payload;

import info.blockchain.wallet.BaseIntegTest;
import info.blockchain.wallet.payload.data.HDWallet;
import info.blockchain.wallet.payload.data.LegacyAddress;
import info.blockchain.wallet.payload.data.Wallet;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.params.BitcoinMainNetParams;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;

public final class PayloadManagerIntegTest extends BaseIntegTest {

    private PayloadManager payloadManager;

    @Before
    public void setup() {
        payloadManager = PayloadManager.getInstance();
    }

    @Test
    public void upgradeV2PayloadToV3() throws Exception {

        //Create a wallet
        payloadManager.create("My HDWallet", "name@email.com", "MyTestWallet");

        Wallet walletBody = payloadManager.getPayload();

        //Remove HD part
        walletBody.setHdWallets(new ArrayList<HDWallet>());

        //Add legacy so we have at least 1 address
        LegacyAddress address = new LegacyAddress();
        address.setLabel("HDAddress label");
        address.setAddress("1PbCM934wxCoVc2y5dJqWpi2w8eHur1W2T");
        LegacyAddress newlyAdded = walletBody.addLegacyAddress(address, null);

        final String guidOriginal = walletBody.getGuid();

        walletBody.upgradeV2PayloadToV3(null, "HDAccount Name2");

        //Check that existing legacy addresses still exist
        Assert.assertEquals(newlyAdded.getAddress(), walletBody.getLegacyAddressList().get(0).getAddress());

        //Check that Guid is still same
        Assert.assertEquals(walletBody.getGuid(), guidOriginal);

        //Check that wallet is flagged as upgraded
        Assert.assertTrue(walletBody.isUpgraded());

        //Check that 1 account exists with keys
        String xpriv = walletBody.getHdWallets().get(0).getAccounts().get(0).getXpriv();
        Assert.assertTrue(xpriv != null && !xpriv.isEmpty());

        //Check that mnemonic exists
        try {
            Assert.assertEquals(walletBody.getHdWallets().get(0).getMnemonic().size(), 12);
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail("upgradeV2PayloadToV3 failed");
        }
    }

    @Test
    public void recoverFromMnemonic_1() throws Exception {

        String mnemonic = "all all all all all all all all all all all all";
        String seedHex = "0660cc198330660cc198330660cc1983";

        payloadManager.recoverFromMnemonic(mnemonic, "My Bitcoin Wallet", "name@email.com", "SomePassword");

        Wallet walletBody = payloadManager
                .getPayload();

        Assert.assertEquals(seedHex, walletBody.getHdWallets().get(0).getSeedHex());
        Assert.assertEquals(10, walletBody.getHdWallets().get(0).getAccounts().size());
        Assert.assertEquals("My Bitcoin Wallet", walletBody.getHdWallets().get(0).getAccounts().get(0).getLabel());
        Assert.assertEquals("My Bitcoin Wallet 2", walletBody.getHdWallets().get(0).getAccounts().get(1).getLabel());
        Assert.assertEquals("My Bitcoin Wallet 3", walletBody.getHdWallets().get(0).getAccounts().get(2).getLabel());
    }

    @Test
    public void recoverFromMnemonic_2() throws Exception {

        String mnemonic = "one defy stock very oven junk neutral weather sweet pyramid celery sorry";
        String seedHex = "9aa737587979dcf2a53fc5dbb5e09467";

        payloadManager.recoverFromMnemonic(mnemonic, "My HDWallet", "name@email.com", "SomePassword");

        Wallet walletBody = payloadManager.getPayload();

        Assert.assertEquals(seedHex, walletBody.getHdWallets().get(0).getSeedHex());
    }

    @Test
    public void initializeAndDecrypt() throws Exception {

        String guid = "f4c49ecb-ac6e-4b45-add4-21dafb90d804";
        String sharedKey = "ba600158-2216-4166-b40c-ee50b33f1835";
        String pw = "testtesttest";
        NetworkParameters networkParameters = BitcoinMainNetParams.get();

        payloadManager.initializeAndDecrypt(networkParameters, sharedKey, guid, pw);

        Assert.assertEquals(guid, payloadManager.getPayload().getGuid());
        Assert.assertEquals(sharedKey, payloadManager.getPayload().getSharedKey());
        Assert.assertEquals(pw, payloadManager.getTempPassword());

        payloadManager.getPayload().getHdWallets().get(0).getAccount(0).setLabel("Some Label");
        Assert.assertTrue(payloadManager.save());
    }
}