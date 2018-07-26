package info.blockchain.wallet.contacts;

import info.blockchain.wallet.BlockchainFramework;
import info.blockchain.wallet.FrameworkInterface;
import info.blockchain.wallet.MockInterceptor;
import info.blockchain.wallet.api.Environment;
import info.blockchain.wallet.api.PersistentUrls;
import info.blockchain.wallet.bip44.HDWallet;
import info.blockchain.wallet.bip44.HDWalletFactory;
import info.blockchain.wallet.bip44.HDWalletFactory.Language;
import info.blockchain.wallet.contacts.data.Contact;
import info.blockchain.wallet.contacts.data.FacilitatedTransaction;
import info.blockchain.wallet.contacts.data.PaymentBroadcasted;
import info.blockchain.wallet.contacts.data.PaymentCurrency;
import info.blockchain.wallet.contacts.data.PaymentRequest;
import info.blockchain.wallet.contacts.data.RequestForPaymentRequest;
import info.blockchain.wallet.exceptions.MetadataException;
import info.blockchain.wallet.exceptions.SharedMetadataException;
import info.blockchain.wallet.metadata.data.Message;
import info.blockchain.wallet.util.MetadataUtil;
import info.blockchain.wallet.util.RestClient;

import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.crypto.DeterministicKey;
import org.bitcoinj.params.BitcoinCashMainNetParams;
import org.bitcoinj.params.BitcoinMainNetParams;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.spongycastle.crypto.InvalidCipherTextException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;

public final class ContactsTest {

    MockInterceptor mockInterceptor;

    private final String magic = "{\"payload\":\"VSSU6yr2Y63SD/q9uzEK5YrXo8n+i/Li6RreS53oFupMuyuNIQB2tJ0Ek1tGMWbB+zzZDR6E4NjXWHoZ6tXfxYdlNvs/SSg4y83Xm2P5UQi3zQ+UlwCC75d46UpoBQh9QHh56j9VvBXKfkkm9m1fHBGeevG8dM3FxOziHXwalaShv01F1w8Q7BHN8m9KLkg3ELajhpbAqX6V6OeHfH2/OqqW9BMVURCWn1a0IF8O32se08kU9y3saOOXx/QBEHKGxP7GwpftnUgT28BkwEjB6Q6A+AYuwnJxoa36GqVSNMw2gv10Gxjic59L2FfJvjg40oXjhhnnGfyQtCWFqj15GX15Kv0Krn/oLLZM0gERe0fpelRVYG2iK2+ytYh76s0L\",\"version\":1,\"type_id\":4,\"signature\":\"IIhKFiFFlQRsNcJsk3Pa45CtwnmBCxRCq7ncxScXK/U6XawV3zza7RvGyAp3M41cdXYOvmFFErQAp0TZytJQ+qo=\",\"prev_magic_hash\":\"e00c9cfe5756507508a07fddd5139491f1a52a0e087593627ae0490297a48842\",\"address\":\"1LF1QvtK6gnxJ3f8tZx9hamWS9jytKZJ6C\",\"created_at\":1482153702000,\"updated_at\":1502285812000}";
    private final String success = "{\"status\":\"success\"}";
    private final String fail = "{\"status\":\"fail\"}";

    @Before
    public void setup() {

        mockInterceptor = MockInterceptor.getInstance();

        //Set environment
        BlockchainFramework.init(new FrameworkInterface() {
            @Override
            public Retrofit getRetrofitApiInstance() {

                HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
                loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);

                OkHttpClient okHttpClient = new OkHttpClient.Builder()
                        .addInterceptor(mockInterceptor)//Mock responses
                        .addInterceptor(loggingInterceptor)//Extensive logging
                        .build();
                return RestClient.getRetrofitApiInstance(okHttpClient);
            }

            @Override
            public Retrofit getRetrofitExplorerInstance() {
                return null;
            }

            @Override
            public Environment getEnvironment() {
                return Environment.PRODUCTION;
            }

            @Override
            public NetworkParameters getBitcoinParams() {
                return BitcoinMainNetParams.get();
            }

            @Override
            public NetworkParameters getBitcoinCashParams() {
                return BitcoinCashMainNetParams.get();
            }

            @Override
            public String getApiCode() {
                return null;
            }

            @Override
            public String getDevice() {
                return null;
            }

            @Override
            public String getAppVersion() {
                return null;
            }
        });
    }

    private HDWallet getWallet() throws Exception {

        return HDWalletFactory
                .restoreWallet(PersistentUrls.getInstance().getBitcoinParams(), Language.US,
                        "15e23aa73d25994f1921a1256f93f72c", "", 1);
    }

    private Contacts init() throws Exception {
        HDWallet b_wallet = getWallet();
        DeterministicKey sharedMetaDataHDNode = MetadataUtil.deriveSharedMetadataNode(b_wallet.getMasterKey());
        DeterministicKey metaDataHDNode = MetadataUtil.deriveMetadataNode(b_wallet.getMasterKey());

        mockInterceptor.setResponseString(magic);
        return new Contacts(metaDataHDNode, sharedMetaDataHDNode);
    }

    @Test
    public void fetch() throws Exception {

        Contacts contacts = init();
        mockInterceptor.setResponseString(magic);
        contacts.fetch();
        Assert.assertEquals(contacts.getContactList().size(), 2);
    }

    @Test
    public void fetch_IOException() throws Exception {

        Contacts contacts = init();
        try {
            mockInterceptor.setIOException(true);
            contacts.fetch();
        } catch (MetadataException e) {
            e.printStackTrace();
        } catch (IOException e) {
            Assert.assertTrue(true);
            return;
        } catch (InvalidCipherTextException e) {
            e.printStackTrace();
        } finally {
            mockInterceptor.setIOException(false);
        }
        Assert.assertTrue("IOException not caught", false);
    }

    @Test
    public void save() throws Exception {
        Contacts contacts = init();
        Contact contact = new Contact();
        contact.setName("John");

        LinkedList<String> responseList = new LinkedList<>();
        responseList.add(success);//add contact response
        responseList.add(success);//save response
        mockInterceptor.setResponseStringList(responseList);

        contacts.addContact(contact);
        contacts.save();

        Assert.assertTrue(true);
    }

    @Test
    public void save_IOException() throws Exception {
        Contacts contacts = init();
        Contact contact = new Contact();
        contact.setName("John");

        mockInterceptor.setResponseString(success);
        contacts.addContact(contact);

        mockInterceptor.setIOException(true);
        try {
            contacts.save();
        } catch (IOException e) {
            Assert.assertTrue(true);
            return;
        } catch (MetadataException e) {
            e.printStackTrace();
        } catch (InvalidCipherTextException e) {
            e.printStackTrace();
        } finally {
            mockInterceptor.setIOException(false);
        }
        Assert.assertTrue(false);
    }

    @Test
    public void wipe() throws Exception {
        Contacts contacts = init();
        Contact contact = new Contact();
        contact.setName("John");

        LinkedList<String> responseList = new LinkedList<>();
        responseList.add(success);//add contact
        responseList.add(success);//wipe
        mockInterceptor.setResponseStringList(responseList);

        contacts.addContact(contact);
        contacts.wipe();

        Assert.assertTrue(contacts.getContactList().size() == 0);
    }

    @Test
    public void wipe_IOException() throws Exception {
        Contacts contacts = init();
        Contact contact = new Contact();
        contact.setName("John");

        mockInterceptor.setResponseString(success);
        contacts.addContact(contact);

        mockInterceptor.setIOException(true);
        try {
            contacts.wipe();
        } catch (IOException e) {
            Assert.assertTrue(true);
            return;
        } catch (MetadataException e) {
            e.printStackTrace();
        } catch (InvalidCipherTextException e) {
            e.printStackTrace();
        } finally {
            mockInterceptor.setIOException(false);
        }
        Assert.assertTrue(false);
    }

    @Test
    public void getMdid() throws Exception {
        Contacts contacts = init();
        Assert.assertEquals("1borrXJLeFgwF1aKS3io9c3rQ1uXHf1s5", contacts.getMdid());
    }

    @Test
    public void setContactList() throws Exception {

        Contacts contacts = init();

        LinkedList<String> responseList = new LinkedList<>();
        responseList.add(success);//addContact
        responseList.add(success);//addContact
        responseList.add(success);//setContactList save
        mockInterceptor.setResponseStringList(responseList);

        contacts.addContact(new Contact());
        contacts.addContact(new Contact());

        Assert.assertEquals(2, contacts.getContactList().size());

        contacts.setContactList(new ArrayList<Contact>());

        Assert.assertEquals(0, contacts.getContactList().size());
    }

    @Test
    public void setContactList_IOException() throws Exception {

        Contacts contacts = init();

        LinkedList<String> responseList = new LinkedList<>();
        responseList.add(success);//addContact
        responseList.add(success);//addContact
        responseList.add(fail);//setContactList save
        mockInterceptor.setResponseStringList(responseList);

        contacts.addContact(new Contact());
        contacts.addContact(new Contact());

        Assert.assertEquals(2, contacts.getContactList().size());

        mockInterceptor.setIOException(true);
        try {
            contacts.setContactList(new ArrayList<Contact>());
        } catch (MetadataException e) {
            Assert.assertTrue(false);
        } catch (IOException e) {
            Assert.assertTrue(true);
        } catch (InvalidCipherTextException e) {
            Assert.assertTrue(false);
        } finally {
            mockInterceptor.setIOException(false);
        }

        Assert.assertEquals(0, contacts.getContactList().size());
    }

    @Test
    public void addContact() throws Exception {

        Contacts contacts = init();

        LinkedList<String> responseList = new LinkedList<>();
        responseList.add(success);//addContact
        responseList.add(success);//addContact
        mockInterceptor.setResponseStringList(responseList);

        contacts.addContact(new Contact());

        Assert.assertEquals(1, contacts.getContactList().size());

        contacts.addContact(new Contact());
        Assert.assertEquals(2, contacts.getContactList().size());
    }

    @Test
    public void removeContact() throws Exception {

        Contact c1 = new Contact();
        Contact c2 = new Contact();

        Contacts contacts = init();

        LinkedList<String> responseList = new LinkedList<>();
        responseList.add(success);//addContact
        responseList.add(success);//addContact
        responseList.add(success);//removeContact
        mockInterceptor.setResponseStringList(responseList);

        contacts.addContact(c1);
        contacts.addContact(c2);

        Assert.assertEquals(2, contacts.getContactList().size());

        contacts.removeContact(c1);
        Assert.assertEquals(1, contacts.getContactList().size());
    }

    @Test
    public void publishXpub() throws Exception {
        Contacts contacts = init();

        LinkedList<String> responseList = new LinkedList<>();
        responseList.add(fail);//magic - string doesn't matter 404 will be caught
        responseList.add(success);//put metadata
        mockInterceptor.setResponseStringList(responseList);

        LinkedList<Integer> responseCodeList = new LinkedList<>();
        responseCodeList.add(404);//fetch magic - 404 = new magic hash
        responseCodeList.add(200);
        mockInterceptor.setResponseCodeList(responseCodeList);

        try {
            contacts.publishXpub();
            Assert.assertTrue(true);
        } catch (MetadataException e) {
            e.printStackTrace();
            Assert.fail();
        } catch (IOException e) {
            e.printStackTrace();
            Assert.fail();
        } catch (InvalidCipherTextException e) {
            e.printStackTrace();
            Assert.fail();
        }
    }

    @Test
    public void publishXpub_IOException() throws Exception {
        Contacts contacts = init();

        try {
            mockInterceptor.setIOException(true);
            contacts.publishXpub();
        } catch (MetadataException e) {
            e.printStackTrace();
        } catch (IOException e) {
            Assert.assertTrue(true);
            return;
        } catch (InvalidCipherTextException e) {
            e.printStackTrace();
        } finally {
            mockInterceptor.setIOException(false);
        }
        Assert.fail();
    }

    @Test
    public void fetchXpub() throws Exception {
        Contacts contacts = init();
        mockInterceptor.setResponseString("{\"payload\":\"eyJ4cHViIjoieHB1YjY4aGpMM01rdmZ6S1pSdXZmQUFBZWFiYUFuWmpnWXFVM0ZTbTFMRUNxRGNhVHI1N013YzREY2lHcTJKQnJyVG9zOHNuUHg3OG1MdEt4dGFQSzJMcWJoVTVnMW9QRk1hR29uRTI3a0g4S0dBIn0=\",\"version\":1,\"type_id\":4,\"signature\":\"IE2zczTK0sRPLRu/vfbM3v6S7gAIh2o+UQxkn1P4uUT+KqCU+P8kVEt7SLjixIQqSb4UzlKNKirXUBiNWGU4Ygg=\",\"prev_magic_hash\":\"8f122f88cad5faedcc3433dbf0618cea17a5682da2c3dfdf36d03d63f88a90c6\",\"created_at\":1482242589000,\"updated_at\":1482244810000,\"address\":\"16uJDcPbvegnJUhgXr5TW9nd9wbJYNWBAd\"}");
        String xpub = contacts.fetchXpub("16uJDcPbvegnJUhgXr5TW9nd9wbJYNWBAd");
        Assert.assertTrue(xpub.equals("xpub68hjL3MkvfzKZRuvfAAAeabaAnZjgYqU3FSm1LECqDcaTr57Mwc4DciGq2JBrrTos8snPx78mLtKxtaPK2LqbhU5g1oPFMaGonE27kH8KGA"));
    }

    @Test
    public void fetchXpub_IOException() throws Exception {
        Contacts contacts = init();
        mockInterceptor.setResponseString("{\"payload\":\"eyJ4cHViIjoieHB1YjY4aGpMM01rdmZ6S1pSdXZmQUFBZWFiYUFuWmpnWXFVM0ZTbTFMRUNxRGNhVHI1N013YzREY2lHcTJKQnJyVG9zOHNuUHg3OG1MdEt4dGFQSzJMcWJoVTVnMW9QRk1hR29uRTI3a0g4S0dBIn0=\",\"version\":1,\"type_id\":4,\"signature\":\"IE2zczTK0sRPLRu/vfbM3v6S7gAIh2o+UQxkn1P4uUT+KqCU+P8kVEt7SLjixIQqSb4UzlKNKirXUBiNWGU4Ygg=\",\"prev_magic_hash\":\"8f122f88cad5faedcc3433dbf0618cea17a5682da2c3dfdf36d03d63f88a90c6\",\"created_at\":1482242589000,\"updated_at\":1482244810000,\"address\":\"16uJDcPbvegnJUhgXr5TW9nd9wbJYNWBAd\"}");
        try {
            mockInterceptor.setIOException(true);
            contacts.fetchXpub("16uJDcPbvegnJUhgXr5TW9nd9wbJYNWBAd");
        } catch (MetadataException e) {
            e.printStackTrace();
        } catch (IOException e) {
            Assert.assertTrue(true);
            return;
        } catch (InvalidCipherTextException e) {
            e.printStackTrace();
        } finally {
            mockInterceptor.setIOException(false);
        }
        Assert.assertTrue(false);
    }

    @Test
    public void createInvitation() throws Exception {
        // Not testable in current state
    }

    @Test
    public void createInvitation_IOException() throws Exception {
        Contacts contacts = init();
        Contact me = new Contact();
        me.setName("Me");
        Contact him = new Contact();
        him.setName("Him");

        Contact myInvite = null;
        try {
            mockInterceptor.setIOException(true);
            myInvite = contacts.createInvitation(me, him);
        } catch (IOException e) {
            Assert.assertTrue(true);
            return;
        } catch (SharedMetadataException e) {
            e.printStackTrace();
        } finally {
            mockInterceptor.setIOException(false);
        }

        Assert.assertTrue(contacts.getContactList().get(0).getName().equals("Him"));
        Assert.assertTrue(myInvite.getName().equals("Me"));
        Assert.assertTrue(myInvite.getMdid().equals("1borrXJLeFgwF1aKS3io9c3rQ1uXHf1s5"));
    }

    @Test
    public void readInvitationLink() throws Exception {
        Contacts contacts = init();
        Contact receivedInvite = contacts.readInvitationLink("http://blockchain.info/invite?id=852bb6796c2aefb7ea96131b785da397dca9cb3bee5df4ea7c937493613e9c37&name=Me");

        Assert.assertTrue(receivedInvite.getName().equals("Me"));
        Assert.assertTrue(receivedInvite.getInvitationReceived().equals("852bb6796c2aefb7ea96131b785da397dca9cb3bee5df4ea7c937493613e9c37"));
    }

    @Test
    public void acceptInvitationLink() throws Exception {
        // Not testable in current state
    }

    @Test
    public void digestUnreadPaymentRequests_RPR() throws Exception {

        Contacts contacts = init();

        Contact contact = new Contact();
        contact.setId("5b47394c-f0d1-416e-8e9d-d63a91709d03");
        contact.setName("Jacob");
        contact.setMdid("13cA57Hvs5zT8yq852aUZeoYfX9DBXCTTR");
        contact.setFacilitatedTransactions(new HashMap<String, FacilitatedTransaction>());
        mockInterceptor.setResponseString(success);//addContact
        contacts.addContact(contact);

        RequestForPaymentRequest rpr = new RequestForPaymentRequest();
        rpr.setId("a9feb110-1ae2-4242-9246-f1d6ec3e3be8");
        rpr.setIntendedAmount(17940000);
        rpr.setCurrency(PaymentCurrency.BITCOIN);
        rpr.setNote("For the pizza");

        List<Message> messages = new ArrayList<>();
        Message message = new Message();
        message.setPayload(rpr.toJson());
        message.setSignature("IDNOxhoWL/gj12kNGte37e2oxzK/A9lVeH3YyjDgk4TTRO0YlkYQnHxS0qcvY8EnVMHhELUgzv/7IQrkypCuktU=");
        message.setRecipient("17uHsZXWqXB5ChW5fNGPnxyTJQEd1ugKca");
        message.setId("cc11e4cf-2cd4-4de5-a2fc-125c43625ec5");
        message.setSender("13cA57Hvs5zT8yq852aUZeoYfX9DBXCTTR");
        message.setSent(1485788921000L);
        message.setProcessed(false);
        message.setType(0);
        messages.add(message);

        mockInterceptor.setResponseString(success);//save
        List<Contact> unreadPaymentRequests = contacts.digestUnreadPaymentRequests(messages, false);
        for (Contact item : unreadPaymentRequests) {

            FacilitatedTransaction ftx = item.getFacilitatedTransactions().get(rpr.getId());

            Assert.assertEquals(contact.getName(), item.getName());
            Assert.assertEquals(rpr.getId(), ftx.getId());
            Assert.assertEquals("waiting_address", ftx.getState());
            Assert.assertEquals(17940000L, ftx.getIntendedAmount());
            Assert.assertEquals(rpr.getCurrency(), PaymentCurrency.BITCOIN);
            Assert.assertEquals("rpr_receiver", ftx.getRole());
            Assert.assertEquals(rpr.getNote(), ftx.getNote());
        }
    }

    @Test
    public void digestUnreadPaymentRequests_RP() throws Exception {

        Contacts contacts = init();

        Contact contact = new Contact();
        contact.setId("5b47394c-f0d1-416e-8e9d-d63a91709d03");
        contact.setName("Jacob");
        contact.setMdid("13cA57Hvs5zT8yq852aUZeoYfX9DBXCTTR");
        contact.setFacilitatedTransactions(new HashMap<String, FacilitatedTransaction>());
        mockInterceptor.setResponseString(success);//addContact
        contacts.addContact(contact);

        /* Set up Payment Request */
        PaymentRequest pr = new PaymentRequest();
        pr.setId("a9feb110-1ae2-4242-9246-f1d6ec3e3be8");
        pr.setIntendedAmount(28940000);
        pr.setCurrency(PaymentCurrency.BITCOIN);
        pr.setNote("For the pizza");
        pr.setAddress("15sAyHb9zBsZbVnaSXz2UivTZYxnjjrEkX");

        List<Message> messages = new ArrayList<>();
        Message message = new Message();
        message.setPayload(pr.toJson());
        message.setSignature("IDNOxhoWL/gj12kNGte37e2oxzK/A9lVeH3YyjDgk4TTRO0YlkYQnHxS0qcvY8EnVMHhELUgzv/7IQrkypCuktU=");
        message.setRecipient("17uHsZXWqXB5ChW5fNGPnxyTJQEd1ugKca");
        message.setId("cc11e4cf-2cd4-4de5-a2fc-125c43625ec5");
        message.setSender("13cA57Hvs5zT8yq852aUZeoYfX9DBXCTTR");
        message.setSent(1485788921000L);
        message.setProcessed(false);
        message.setType(1);
        messages.add(message);

        mockInterceptor.setResponseString(success);//save
        List<Contact> unreadPaymentRequests = contacts.digestUnreadPaymentRequests(messages, false);
        for (Contact item : unreadPaymentRequests) {

            FacilitatedTransaction ftx = item.getFacilitatedTransactions().get(pr.getId());

            Assert.assertEquals(contact.getName(), item.getName());
            Assert.assertEquals(pr.getId(), ftx.getId());
            Assert.assertEquals("waiting_payment", ftx.getState());
            Assert.assertEquals(28940000L, ftx.getIntendedAmount());
            Assert.assertEquals("pr_receiver", ftx.getRole());
            Assert.assertEquals(pr.getNote(), ftx.getNote());
            Assert.assertEquals(pr.getAddress(), ftx.getAddress());
        }

        /* Complete above payment request */
        PaymentBroadcasted b = new PaymentBroadcasted(pr.getId(), "this_will_be_the_tx_hash");

        messages = new ArrayList<>();
        message = new Message();
        message.setPayload(b.toJson());
        message.setSignature("IDNOxhoWL/gj12kNGte37e2oxzK/A9lVeH3YyjDgk4TTRO0YlkYQnHxS0qcvY8EnVMHhELUgzv/7IQrkypCuktU=");
        message.setRecipient("17uHsZXWqXB5ChW5fNGPnxyTJQEd1ugKca");
        message.setId("cc11e4cf-2cd4-4de5-a2fc-125c43625ec5");
        message.setSender("13cA57Hvs5zT8yq852aUZeoYfX9DBXCTTR");
        message.setSent(1485788921000L);
        message.setProcessed(false);
        message.setType(2);
        messages.add(message);

        mockInterceptor.setResponseString(success);//save
        unreadPaymentRequests = contacts.digestUnreadPaymentRequests(messages, false);
        for (Contact item : unreadPaymentRequests) {

            FacilitatedTransaction ftx = item.getFacilitatedTransactions().get(b.getId());

            Assert.assertEquals(contact.getName(), item.getName());
            Assert.assertEquals(b.getId(), ftx.getId());
            Assert.assertEquals("payment_broadcasted", ftx.getState());
            Assert.assertEquals(28940000L, ftx.getIntendedAmount());
            Assert.assertEquals("pr_receiver", ftx.getRole());
            Assert.assertEquals(pr.getNote(), ftx.getNote());
            Assert.assertEquals(pr.getAddress(), ftx.getAddress());
        }
    }
}