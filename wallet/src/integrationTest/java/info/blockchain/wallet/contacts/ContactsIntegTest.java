package info.blockchain.wallet.contacts;

import info.blockchain.wallet.BaseIntegTest;
import info.blockchain.wallet.api.PersistentUrls;
import info.blockchain.wallet.bip44.HDWallet;
import info.blockchain.wallet.bip44.HDWalletFactory;
import info.blockchain.wallet.bip44.HDWalletFactory.Language;
import info.blockchain.wallet.contacts.data.Contact;
import info.blockchain.wallet.contacts.data.FacilitatedTransaction;
import info.blockchain.wallet.contacts.data.PaymentCurrency;
import info.blockchain.wallet.contacts.data.PaymentRequest;
import info.blockchain.wallet.contacts.data.RequestForPaymentRequest;
import info.blockchain.wallet.metadata.data.Message;
import info.blockchain.wallet.util.MetadataUtil;

import org.bitcoinj.crypto.DeterministicKey;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

public class ContactsIntegTest extends BaseIntegTest{

    //dev wallets
    private String wallet_A_guid = "014fb9fc-64f9-4cf5-b76b-d927d7619717";
    private String wallet_A_sharedKey = "bc73239b-d3d9-4bee-a1f9-80248e179486";
    private String wallet_A_seedHex = "20e3939d08ddf727f34a130704cd925e";

    private String wallet_B_guid = "6fbe154a-35e0-46fb-a22b-699dc7cba87c";
    private String wallet_B_sharedKey = "49e58bdb-5a66-4353-923a-3b49054603d6";
    private String wallet_B_seedHex = "b88d0d894c19ad1d8e7f1563b7455f7c";

    private HDWallet getWallet() throws Exception {

        return HDWalletFactory
            .restoreWallet(PersistentUrls.getInstance().getBitcoinParams(), Language.US,
                "15e23aa73d25994f1921a1256f93f72c", "", 1);
    }

    @Test
    public void testMetadataIntegration() throws Exception {

        DeterministicKey sharedMetaDataHDNode = MetadataUtil
            .deriveSharedMetadataNode(getWallet().getMasterKey());
        DeterministicKey metaDataHDNode = MetadataUtil
            .deriveMetadataNode(getWallet().getMasterKey());

        Contacts contacts = new Contacts(metaDataHDNode, sharedMetaDataHDNode);
        contacts.wipe();
        contacts.fetch();
        Assert.assertTrue(contacts.getContactList().size() == 0);

        List<Contact> contactList = new ArrayList<>();
        Contact contact1 = new Contact();
        contact1.setSurname("Doe");
        contact1.setName("John");
        contact1.setCompany("Blockchain");
        contactList.add(contact1);

        Contact contact2 = new Contact();
        contact2.setSurname("Clark");
        contact2.setName("Bill");
        contact2.setCompany("Blockchain");
        contactList.add(contact2);

        contacts.setContactList(contactList);
        Assert.assertTrue(contacts.getContactList().size() == 2);

        contacts.save();

        contacts = new Contacts(metaDataHDNode, sharedMetaDataHDNode);
        contacts.fetch();
        Assert.assertTrue(contacts.getContactList().size() == 2);

        Assert.assertEquals(contacts.getContactList().get(contact1.getId()).getName(), "John");
        Assert.assertEquals(contacts.getContactList().get(contact1.getId()).getSurname(), "Doe");
        Assert.assertEquals(contacts.getContactList().get(contact1.getId()).getCompany(), "Blockchain");
        Assert.assertEquals(contacts.getContactList().get(contact2.getId()).getName(), "Bill");
        Assert.assertEquals(contacts.getContactList().get(contact2.getId()).getSurname(), "Clark");
        Assert.assertEquals(contacts.getContactList().get(contact2.getId()).getCompany(), "Blockchain");

        contacts.wipe();

        contacts = new Contacts(metaDataHDNode, sharedMetaDataHDNode);
        contacts.fetch();
        Assert.assertTrue(contacts.getContactList().size() == 0);

    }

    @Test
    public void testSharedMetadataIntegration() throws Exception {

        /*
        Create wallets
         */
        HDWallet a_wallet = HDWalletFactory.createWallet(PersistentUrls.getInstance().getBitcoinParams(), Language.US, 12, "", 1);
        DeterministicKey sharedMetaDataHDNode = MetadataUtil
            .deriveSharedMetadataNode(a_wallet.getMasterKey());
        DeterministicKey metaDataHDNode = MetadataUtil.deriveMetadataNode(a_wallet.getMasterKey());
        Contacts a_contacts = new Contacts(metaDataHDNode, sharedMetaDataHDNode);
        a_contacts.publishXpub();
        a_contacts.fetch();

        HDWallet b_wallet = HDWalletFactory.createWallet(PersistentUrls.getInstance().getBitcoinParams(), Language.US,12, "", 1);
        sharedMetaDataHDNode = MetadataUtil.deriveSharedMetadataNode(b_wallet.getMasterKey());
        metaDataHDNode = MetadataUtil.deriveMetadataNode(b_wallet.getMasterKey());
        Contacts b_contacts = new Contacts(metaDataHDNode, sharedMetaDataHDNode);
        b_contacts.publishXpub();
        b_contacts.fetch();

        System.out.println("Sender: " + a_contacts.getMdid());
        System.out.println("Recipient: " + b_contacts.getMdid());

        /*
        Pair
         */
        System.out.println("\n\n////////////////////////");
        System.out.println("/////////Pairing////////");
        System.out.println("////////////////////////");
        System.out.println("\n--Sender--");
        Contact me = new Contact();
        me.setName("Riaan");
        Contact him = new Contact();
        him.setName("Jaume");
        Contact a_invite = a_contacts.createInvitation(me, him);
        String oneTimeUri = a_invite.createURI();
        System.out.println("createInvitation: " + oneTimeUri);

        System.out.println("\n--Recipient--");
        Contact a_accepted_invite = b_contacts.acceptInvitationLink(oneTimeUri);
        System.out.println("accept Invitation: " + a_accepted_invite.toJson());

        System.out.println("\n--Sender--");
        boolean accepted = a_contacts.readInvitationSent(a_invite);
        System.out.println("Accepted: " + accepted);

        //Mdid's can now be retrieved from contact list
        String RiaanMdid = b_contacts.getContactList().get(a_accepted_invite.getId()).getMdid();
        String JaumeMdid = a_contacts.getContactList().get(him.getId()).getMdid();

        /*
        Send messages
         */
        System.out.println("\n\n////////////////////////");
        System.out.println("////////Messages////////");
        System.out.println("////////////////////////");
        System.out.println(
            "Sending message 'Hey hey' to " + JaumeMdid);
        a_contacts.sendMessage(JaumeMdid, "Hey hey", 66, true);

        System.out.println("\n--Recipient--");
        List<Message> messages = b_contacts.getMessages(true);

        Message message = messages.get(messages.size() - 1);
        b_contacts.readMessage(message.getId());
        System.out.println("Received message: " + message.getPayload());
        b_contacts.markMessageAsRead(message.getId(), true);
        System.out
            .println("Sending message 'Got it' to " + RiaanMdid);
        b_contacts.sendMessage(RiaanMdid, "Got it", 66, true);


        /*
        Send Request Payment Request
         */
        System.out.println("\n////////////////////////");
        System.out.println("////Payment Request 1///");
        System.out.println("////////////////////////");
        System.out.println("\n--Sender--");
        //Step 1
        RequestForPaymentRequest rpr = new RequestForPaymentRequest(17940000, "For the pizza",
            PaymentCurrency.BITCOIN);
        System.out.println(rpr.toJson());
        a_contacts.sendRequestForPaymentRequest(JaumeMdid, rpr);
        System.out.println("Send RPR: " + rpr.toJson());

        System.out.println("\n--Recipient--");
        List<Contact> b_unreadList = b_contacts.digestUnreadPaymentRequests();

        FacilitatedTransaction ftx = null;

        for (Contact unread : b_unreadList) {
            Set<Entry<String, FacilitatedTransaction>> set = unread.getFacilitatedTransactions()
                .entrySet();
            for (Entry<String, FacilitatedTransaction> item : set) {
                System.out.println("Received RPR tx_id: " + item.getValue().getId());
                ftx = item.getValue();
            }
        }

        //Step 2
        PaymentRequest pr = new PaymentRequest();
        pr.setId(ftx.getId());
        pr.setIntendedAmount(ftx.getIntendedAmount());
        pr.setAddress(b_wallet.getAccount(0).getReceive().getAddressAt(0)
            .getAddressString());//should be next available
        System.out.println("Send PR to '" + RiaanMdid + "': " + pr.toJson());
        b_contacts.sendPaymentRequest(RiaanMdid, pr, ftx.getId());

        //Step 3
        System.out.println("\n--Sender--");
        List<Contact> a_unreadList = a_contacts.digestUnreadPaymentRequests();

        for (Contact unread : a_unreadList) {
            Set<Entry<String, FacilitatedTransaction>> set = unread.getFacilitatedTransactions()
                .entrySet();
            for (Entry<String, FacilitatedTransaction> item : set) {
                System.out.println("Received PR: " + item.getValue().toJson());
                System.out.println("Making payment to " + item.getValue().getAddress());
                System.out
                    .println("Send payment broadcasted message ftx: " + item.getValue().getId());

                try {
                    a_contacts.sendPaymentBroadcasted(a_contacts.getContactList().get(0).getMdid(),
                        "978acce2b1163c5b42f55e98efad345aedb451e936f667f7e35917cd555c6bd7",
                        item.getValue().getId());
                } catch (Exception e) {
                    System.out
                        .println("payment should be rejected if tx_hash doesn't belong to wallet");
                    //hack this by changing Transactions.class line 190 from `if (our_xput) {` to `if (true) {`
                }
            }
        }

        System.out.println("\n--Recipient--");
        b_unreadList = b_contacts.digestUnreadPaymentRequests();
        for (Contact unread : b_unreadList) {
            Set<Entry<String, FacilitatedTransaction>> set = unread.getFacilitatedTransactions()
                .entrySet();
            for (Entry<String, FacilitatedTransaction> item : set) {
                System.out
                    .println("Received payment broadcast tx_hash: " + item.getValue().getTxHash());
            }
        }
    }
}
