package info.blockchain.wallet.payload;

import info.blockchain.api.blockexplorer.BlockExplorer;
import info.blockchain.api.blockexplorer.FilterType;
import info.blockchain.api.data.Balance;
import info.blockchain.balance.CryptoCurrency;
import info.blockchain.balance.CryptoValue;
import retrofit2.Call;

import javax.annotation.Nonnull;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

public abstract class BalanceManager {

    private BlockExplorer blockExplorer;
    private CryptoCurrency cryptoCurrency;

    @Nonnull
    private CryptoBalanceMap balanceMap;

    BalanceManager(@Nonnull BlockExplorer blockExplorer, @Nonnull CryptoCurrency cryptoCurrency) {
        this.blockExplorer = blockExplorer;
        this.cryptoCurrency = cryptoCurrency;
        balanceMap = CryptoBalanceMap.zero(cryptoCurrency);
    }

    public void subtractAmountFromAddressBalance(String address, BigInteger amount) {
        balanceMap = balanceMap.subtractAmountFromAddress(address, new CryptoValue(cryptoCurrency, amount));
    }

    @Nonnull
    public BigInteger getAddressBalance(String address) {
        return balanceMap.get(address).getAmount();
    }

    @Nonnull
    public BigInteger getWalletBalance() {
        return balanceMap.getTotalSpendable().getAmount();
    }

    @Nonnull
    public BigInteger getImportedAddressesBalance() {
        return balanceMap.getTotalSpendableLegacy().getAmount();
    }

    @Nonnull
    public BigInteger getWatchOnlyBalance() {
        return balanceMap.getTotalWatchOnly().getAmount();
    }

    public void updateAllBalances(
            Set<String> xpubs,
            Set<String> legacyAddresses,
            Set<String> legacyWatchOnlyAddresses
    ) {
        balanceMap = CryptoBalanceMapKt.calculateCryptoBalanceMap(
                cryptoCurrency,
                getBalanceQuery(),
                xpubs,
                legacyAddresses,
                legacyWatchOnlyAddresses
        );
    }

    private BalanceCall getBalanceQuery() {
        return new BalanceCall(blockExplorer, cryptoCurrency);
    }

    /**
     * @deprecated Use getBalanceQuery
     */
    @Deprecated
    public Call<HashMap<String, Balance>> getBalanceOfAddresses(List<String> addresses) {
        return getBlockExplorer().getBalance("btc", addresses, FilterType.RemoveUnspendable);
    }

    BlockExplorer getBlockExplorer() {
        return blockExplorer;
    }
}
