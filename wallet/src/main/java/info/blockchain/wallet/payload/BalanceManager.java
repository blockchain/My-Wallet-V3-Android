package info.blockchain.wallet.payload;

import info.blockchain.api.blockexplorer.BlockExplorer;
import info.blockchain.api.blockexplorer.FilterType;
import info.blockchain.api.data.Balance;
import info.blockchain.wallet.exceptions.ServerConnectionException;
import retrofit2.Call;
import retrofit2.Response;

import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

public class BalanceManager {

    private static final String WALLET_BALANCE = "all";
    private static final String IMPORTED_ADDRESSES_BALANCE = "all_legacy";

    private BlockExplorer blockExplorer;

    private HashMap<String, BigInteger> balanceMap;

    BalanceManager(BlockExplorer blockExplorer) {
        this.blockExplorer = blockExplorer;
        this.balanceMap = new HashMap<>();
    }

    public void subtractAmountFromAddressBalance(String address, BigInteger amount) throws Exception {
        //Update individual address
        BigInteger currentBalance = balanceMap.get(address);
        if (currentBalance == null) {
            throw new Exception("No info for this address. updateAllBalances should be called first.");
        }
        BigInteger newBalance = currentBalance.subtract(amount);
        balanceMap.put(address, newBalance);

        //Update wallet balance
        currentBalance = balanceMap.get(WALLET_BALANCE);
        if (currentBalance == null) {
            throw new Exception("No info for this address. updateAllBalances should be called first.");
        }
        newBalance = currentBalance.subtract(amount);
        balanceMap.put(WALLET_BALANCE, newBalance);
    }

    public BigInteger getAddressBalance(String address) {
        return balanceMap.get(address);
    }

    public BigInteger getWalletBalance() {
        return balanceMap.get(WALLET_BALANCE);
    }

    public BigInteger getImportedAddressesBalance() {
        return balanceMap.get(IMPORTED_ADDRESSES_BALANCE);
    }

    public void updateAllBalances(
            Set<String> legacyAddressList,
            Set<String> allAccountsAndAddresses,
            Set<String> excludeFromTotals
    ) throws ServerConnectionException, IOException {
        Call<HashMap<String, Balance>> call = getBalanceOfAddresses(new ArrayList<>(allAccountsAndAddresses));

        BigInteger walletFinalBalance = BigInteger.ZERO;
        BigInteger importedFinalBalance = BigInteger.ZERO;

        Response<HashMap<String, Balance>> exe = call.execute();
        if (exe.isSuccessful()) {

            Set<Entry<String, Balance>> set = exe.body().entrySet();
            for (Entry<String, Balance> item : set) {
                String address = item.getKey();
                Balance balance = item.getValue();

                balanceMap.put(address, balance.getFinalBalance());

                if (excludeFromTotals.contains(address)) {
                    continue;
                }

                //Consolidate 'All'
                walletFinalBalance = walletFinalBalance.add(balance.getFinalBalance());

                //Consolidate 'Imported'
                if (legacyAddressList.contains(address)) {
                    importedFinalBalance = importedFinalBalance.add(balance.getFinalBalance());
                }
            }

            balanceMap.put(WALLET_BALANCE, walletFinalBalance);
            balanceMap.put(IMPORTED_ADDRESSES_BALANCE, importedFinalBalance);

        } else {
            throw new ServerConnectionException(exe.errorBody().string());
        }
    }

    public Call<HashMap<String, Balance>> getBalanceOfAddresses(List<String> addresses) {
        return getBlockExplorer().getBalance("btc", addresses, FilterType.RemoveUnspendable);
    }

    BlockExplorer getBlockExplorer() {
        return blockExplorer;
    }
}
