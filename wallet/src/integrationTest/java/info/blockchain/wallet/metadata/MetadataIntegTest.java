package info.blockchain.wallet.metadata;

import info.blockchain.wallet.BaseIntegTest;
import info.blockchain.wallet.api.PersistentUrls;
import info.blockchain.wallet.bip44.HDWallet;
import info.blockchain.wallet.bip44.HDWalletFactory;
import info.blockchain.wallet.bip44.HDWalletFactory.Language;
import info.blockchain.wallet.contacts.data.PublicContactDetails;
import info.blockchain.wallet.util.MetadataUtil;
import org.bitcoinj.crypto.DeterministicKey;
import org.junit.Test;

public class MetadataIntegTest extends BaseIntegTest {

    private HDWallet getWallet() throws Exception {

        return HDWalletFactory
            .restoreWallet(PersistentUrls.getInstance().getBitcoinParams(), Language.US,
                "15e23aa73d25994f1921a1256f93f72c",
                "",
                1);
    }

    @Test
    public void testFetchExistingMagicHash() throws Exception {

        /*
        Magic hash need to be computed correctly otherwise consecutive PUT will fail
         */
        DeterministicKey metaDataHDNode = MetadataUtil
            .deriveMetadataNode(getWallet().getMasterKey());

        Metadata metadata = new Metadata.Builder(metaDataHDNode, 2)
            .build();
        metadata.putMetadata(new PublicContactDetails("hello").toJson());
        metadata.setMagicHash(null);
        metadata.putMetadata(new PublicContactDetails("hello").toJson());
        metadata.deleteMetadata(new PublicContactDetails("hello").toJson());

        metadata = new Metadata.Builder(metaDataHDNode, 2)
            .build();
        metadata.putMetadata(new PublicContactDetails("Yolo2").toJson());

        metadata = new Metadata.Builder(metaDataHDNode, 2)
            .build();
        metadata.putMetadata(new PublicContactDetails("Yolo3").toJson());


    }
}
