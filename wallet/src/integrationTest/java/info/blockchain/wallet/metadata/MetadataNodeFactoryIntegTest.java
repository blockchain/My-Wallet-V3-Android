package info.blockchain.wallet.metadata;

import info.blockchain.wallet.BaseIntegTest;
import info.blockchain.wallet.api.PersistentUrls;
import info.blockchain.wallet.bip44.HDWallet;
import info.blockchain.wallet.bip44.HDWalletFactory;
import info.blockchain.wallet.bip44.HDWalletFactory.Language;
import org.junit.Assert;
import org.junit.Test;

public class MetadataNodeFactoryIntegTest extends BaseIntegTest {

    private MetadataNodeFactory subject;

    private HDWallet getWallet() throws Exception {
        return HDWalletFactory
            .restoreWallet(PersistentUrls.getInstance().getBitcoinParams(), Language.US,
                "15e23aa73d25994f1921a1256f93f72c",
                "",
                1);
    }

    @Test
    public void Constructor_legacyNode_should_be_unavailable() throws Exception {

        subject = new MetadataNodeFactory("guid","sharedKey","walletPassword");
        subject.saveMetadataHdNodes(getWallet().getMasterKey());

        subject = new MetadataNodeFactory("guid","sharedKey","walletPassword");
        subject.saveMetadataHdNodes(getWallet().getMasterKey());

        Assert.assertTrue(subject.isMetadataUsable());
        Assert.assertFalse(subject.isLegacySecondPwNodeAvailable());
    }

    @Test
    public void saveMetadataHdNodes() throws Exception {

        subject = new MetadataNodeFactory("guid","sharedKey","walletPassword");
        subject.saveMetadataHdNodes(getWallet().getMasterKey());

        Assert.assertTrue(subject.isMetadataUsable());
    }
}
