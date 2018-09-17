package piuk.blockchain.android.ui.transactions;

import android.support.annotation.NonNull;

import info.blockchain.wallet.multiaddress.TransactionSummary;

import org.apache.commons.lang3.tuple.Pair;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.TreeMap;

import javax.inject.Inject;

import piuk.blockchain.androidcore.data.bitcoincash.BchDataManager;
import piuk.blockchain.androidcore.data.payload.PayloadDataManager;
import piuk.blockchain.androidcore.data.transactions.models.Displayable;
import piuk.blockchain.androidcore.injection.PresenterScope;

import static info.blockchain.wallet.multiaddress.TransactionSummary.Direction.RECEIVED;

@PresenterScope
public class TransactionHelper {

    private PayloadDataManager payloadDataManager;
    private BchDataManager bchDataManager;

    @Inject
    public TransactionHelper(PayloadDataManager payloadDataManager, BchDataManager bchDataManager) {
        this.payloadDataManager = payloadDataManager;
        this.bchDataManager = bchDataManager;
    }

    /**
     * Return a Pair of maps that correspond to the inputs and outputs of a transaction, whilst
     * filtering out Change addresses.
     *
     * @param transactionSummary A {@link TransactionSummary} object
     * @return A Pair of Maps representing the input and output of the transaction
     */
    @NonNull
    Pair<HashMap<String, BigInteger>, HashMap<String, BigInteger>> filterNonChangeAddresses(Displayable transactionSummary) {

        HashMap<String, BigInteger> inputMap = new HashMap<>();
        HashMap<String, BigInteger> outputMap = new HashMap<>();

        ArrayList<String> inputXpubList = new ArrayList<>();

        // Inputs / From field
        if (transactionSummary.getDirection().equals(RECEIVED) && !transactionSummary.getInputsMap().isEmpty()) {
            // Only 1 addr for receive
            TreeMap<String, BigInteger> treeMap = new TreeMap<>();
            treeMap.putAll(transactionSummary.getInputsMap());
            inputMap.put(treeMap.lastKey(), treeMap.lastEntry().getValue());
        } else {
            for (String inputAddress : transactionSummary.getInputsMap().keySet()) {
                BigInteger inputValue = transactionSummary.getOutputsMap().get(inputAddress);
                // Move or Send
                // The address belongs to us
                String xpub = payloadDataManager.getXpubFromAddress(inputAddress);

                // Address belongs to xpub we own
                if (xpub != null) {
                    // Only add xpub once
                    if (!inputXpubList.contains(xpub)) {
                        inputMap.put(inputAddress, inputValue);
                        inputXpubList.add(xpub);
                    }
                } else {
                    // Legacy Address or someone else's address
                    inputMap.put(inputAddress, inputValue);
                }
            }
        }

        // Outputs / To field
        for (String outputAddress : transactionSummary.getOutputsMap().keySet()) {
            BigInteger outputValue = transactionSummary.getOutputsMap().get(outputAddress);

            if (payloadDataManager.isOwnHDAddress(outputAddress)) {
                // If output address belongs to an xpub we own - we have to check if it's change
                String xpub = payloadDataManager.getXpubFromAddress(outputAddress);
                if (inputXpubList.contains(xpub)) {
                    continue;// change back to same xpub
                }

                // Receiving to same address multiple times?
                if (outputMap.containsKey(outputAddress)) {
                    BigInteger prevAmount = outputMap.get(outputAddress).add(outputValue);
                    outputMap.put(outputAddress, prevAmount);
                } else {
                    outputMap.put(outputAddress, outputValue);
                }

            } else if (payloadDataManager.getWallet().getLegacyAddressStringList().contains(outputAddress)
                    || payloadDataManager.getWallet().getWatchOnlyAddressStringList().contains(outputAddress)) {
                // If output address belongs to a legacy address we own - we have to check if it's change
                // If it goes back to same address AND if it's not the total amount sent
                // (inputs x and y could send to output y in which case y is not receiving change, but rather the total amount)
                if (inputMap.containsKey(outputAddress) && outputValue.abs().compareTo(transactionSummary.getTotal()) != 0) {
                    continue;// change back to same input address
                }

                // Output more than tx amount - change
                if (outputValue.abs().compareTo(transactionSummary.getTotal()) == 1) {
                    continue;
                }

                outputMap.put(outputAddress, outputValue);
            } else {
                if (!transactionSummary.getDirection().equals(RECEIVED)) {
                    outputMap.put(outputAddress, outputValue);
                }
            }
        }

        return Pair.of(inputMap, outputMap);
    }

    Pair<HashMap<String, BigInteger>, HashMap<String, BigInteger>> filterNonChangeAddressesBch(Displayable transactionSummary) {

        HashMap<String, BigInteger> inputMap = new HashMap<>();
        HashMap<String, BigInteger> outputMap = new HashMap<>();

        ArrayList<String> inputXpubList = new ArrayList<>();

        // Inputs / From field
        if (transactionSummary.getDirection().equals(RECEIVED) && !transactionSummary.getInputsMap().isEmpty()) {
            // Only 1 addr for receive
            TreeMap<String, BigInteger> treeMap = new TreeMap<>();
            treeMap.putAll(transactionSummary.getInputsMap());
            inputMap.put(treeMap.lastKey(), treeMap.lastEntry().getValue());
        } else {
            for (String inputAddress : transactionSummary.getInputsMap().keySet()) {
                BigInteger inputValue = transactionSummary.getOutputsMap().get(inputAddress);
                // Move or Send
                // The address belongs to us
                String xpub = bchDataManager.getXpubFromAddress(inputAddress);

                // Address belongs to xpub we own
                if (xpub != null) {
                    // Only add xpub once
                    if (!inputXpubList.contains(xpub)) {
                        inputMap.put(inputAddress, inputValue);
                        inputXpubList.add(xpub);
                    }
                } else {
                    // Legacy Address or someone else's address
                    inputMap.put(inputAddress, inputValue);
                }
            }
        }

        // Outputs / To field
        for (String outputAddress : transactionSummary.getOutputsMap().keySet()) {
            BigInteger outputValue = transactionSummary.getOutputsMap().get(outputAddress);

            if (bchDataManager.isOwnAddress(outputAddress)) {
                // If output address belongs to an xpub we own - we have to check if it's change
                String xpub = bchDataManager.getXpubFromAddress(outputAddress);
                if (inputXpubList.contains(xpub)) {
                    continue;// change back to same xpub
                }

                // Receiving to same address multiple times?
                if (outputMap.containsKey(outputAddress)) {
                    BigInteger prevAmount = outputMap.get(outputAddress).add(outputValue);
                    outputMap.put(outputAddress, prevAmount);
                } else {
                    outputMap.put(outputAddress, outputValue);
                }

            } else if (bchDataManager.getLegacyAddressStringList().contains(outputAddress)
                    || bchDataManager.getWatchOnlyAddressStringList().contains(outputAddress)) {
                // If output address belongs to a legacy address we own - we have to check if it's change
                // If it goes back to same address AND if it's not the total amount sent
                // (inputs x and y could send to output y in which case y is not receiving change, but rather the total amount)
                if (inputMap.containsKey(outputAddress) && outputValue.abs().compareTo(transactionSummary.getTotal()) != 0) {
                    continue;// change back to same input address
                }

                // Output more than tx amount - change
                if (outputValue.abs().compareTo(transactionSummary.getTotal()) == 1) {
                    continue;
                }

                outputMap.put(outputAddress, outputValue);
            } else {
                if (!transactionSummary.getDirection().equals(RECEIVED)) {
                    outputMap.put(outputAddress, outputValue);
                }
            }
        }

        return Pair.of(inputMap, outputMap);
    }
}
