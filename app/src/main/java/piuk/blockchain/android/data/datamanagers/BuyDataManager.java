package piuk.blockchain.android.data.datamanagers;

import android.util.Log;

import com.fasterxml.jackson.databind.ObjectMapper;

import info.blockchain.wallet.metadata.Metadata;
import info.blockchain.wallet.payload.PayloadManager;
import info.blockchain.wallet.util.MetadataUtil;

import org.bitcoinj.crypto.DeterministicKey;

import java.util.ArrayList;
import java.util.List;

import io.reactivex.Observable;
import io.reactivex.subjects.ReplaySubject;
import piuk.blockchain.android.data.exchange.ExchangeData;
import piuk.blockchain.android.data.exchange.TradeData;
import piuk.blockchain.android.data.rxjava.RxUtil;

/**
 * Created by justin on 4/28/17.
 */

public class BuyDataManager {
    private static final String TAG = BuyDataManager.class.getSimpleName();
    private static final int METADATA_TYPE_EXCHANGE = 3;
    private static BuyDataManager instance;

    private OnboardingDataManager onboardingDataManager;
    private SettingsDataManager settingsDataManager;
    private PayloadDataManager payloadDataManager;

    private PayloadManager payloadManager;
    private ReplaySubject<Metadata> metadataSubject;

    private boolean didStartLoad = false;

    private BuyDataManager(OnboardingDataManager onboardingDataManager, SettingsDataManager settingsDataManager, PayloadDataManager payloadDataManager) {
        this.onboardingDataManager = onboardingDataManager;
        this.settingsDataManager = settingsDataManager;
        this.payloadDataManager = payloadDataManager;

        this.payloadManager = PayloadManager.getInstance();
        this.metadataSubject = ReplaySubject.create(1);
    }

    public static BuyDataManager getInstance(OnboardingDataManager onboardingDataManager, SettingsDataManager settingsDataManager, PayloadDataManager payloadDataManager) {
        if (instance == null) {
            instance = new BuyDataManager(onboardingDataManager, settingsDataManager, payloadDataManager);
        }
        return instance;
    }

    public Observable<Boolean> getCanBuy() {
        return Observable.combineLatest(
                this.onboardingDataManager.getIfSepaCountry(),
                this.getIsInvited(),
                (isSepa, isInvited) -> isSepa && isInvited
        );
    }

    private Observable<Boolean> getIsInvited() {
        return this.settingsDataManager.initSettings(
                payloadDataManager.getWallet().getGuid(),
                payloadDataManager.getWallet().getSharedKey()
        ).map(settings -> {
            // TODO: implement settings.invited.sfox
            return true;
        });
    }

    public Observable<String> getPendingTradeAddresses() {
        Log.d(TAG, "getPendingTradeAddresses: called");
        return this.getExchangeData()
                .flatMap(metadata ->
                        Observable.fromCallable(metadata::getMetadata)
                )
                .flatMapIterable(exchangeData -> {
                    ObjectMapper mapper = new ObjectMapper();
                    ExchangeData data = mapper.readValue(exchangeData, ExchangeData.class);

                    List<TradeData> trades = new ArrayList<TradeData>();
                    if (data.getCoinify() != null) {
                        trades.addAll(data.getCoinify().getTrades());
                    } else if (data.getSfox() != null) {
                        trades.addAll(data.getSfox().getTrades());
                    }

                    return trades;
                })
                .filter(tradeData ->
                        !tradeData.isConfirmed()
                )
                .map(tradeData ->
                        payloadDataManager.getReceiveAddressAtPosition(
                                payloadDataManager.getAccount(tradeData.getAccountIndex()),
                                tradeData.getReceiveIndex()
                        )
                )
                .map(address -> {
                    Log.d(TAG, "getPendingTradeAddresses: found address: " + address);
                    return address;
                })
                .distinct();
    }

    public Observable<Metadata> getExchangeData() {
        if (!didStartLoad) {
            reloadExchangeData();
            didStartLoad = true;
        }
        return metadataSubject;
    }

    public void reloadExchangeData() {
        Observable<Metadata> exchangeDataStream = getMetadata();
        exchangeDataStream.subscribeWith(metadataSubject);
    }

    private Observable<Metadata> getMetadata() {
        return Observable.fromCallable(() -> {
            DeterministicKey masterKey = payloadManager
                    .getPayload()
                    .getHdWallets().get(0)
                    .getMasterKey();
            DeterministicKey metadataHDNode = MetadataUtil.deriveMetadataNode(masterKey);
            return new Metadata.Builder(metadataHDNode, METADATA_TYPE_EXCHANGE).build();
        }).compose(RxUtil.applySchedulersToObservable());
    }
}
