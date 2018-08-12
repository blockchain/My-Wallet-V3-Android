package piuk.blockchain.android.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;

import javax.inject.Inject;

import com.blockchain.kyc.datamanagers.nabu.NabuDataManager;
import piuk.blockchain.android.data.bitcoincash.BchDataManager;
import piuk.blockchain.android.data.ethereum.EthDataManager;
import piuk.blockchain.android.data.websocket.WebSocketService;
import piuk.blockchain.android.injection.Injector;
import piuk.blockchain.android.ui.dashboard.DashboardPresenter;
import piuk.blockchain.android.util.OSUtil;
import piuk.blockchain.androidbuysell.datamanagers.BuyDataManager;
import piuk.blockchain.androidbuysell.datamanagers.CoinifyDataManager;
import piuk.blockchain.androidbuysell.repositories.AccessTokenStore;
import piuk.blockchain.androidbuysell.services.BuyConditions;
import piuk.blockchain.androidbuysell.services.ExchangeService;
import piuk.blockchain.androidcore.data.access.AccessState;
import piuk.blockchain.androidcore.data.shapeshift.ShapeShiftDataManager;
import piuk.blockchain.androidcore.data.walletoptions.WalletOptionsState;
import piuk.blockchain.androidcore.utils.PrefsUtil;

public class LogoutActivity extends AppCompatActivity {

    @Inject protected BuyDataManager buyDataManager;
    @Inject protected EthDataManager ethDataManager;
    @Inject protected BchDataManager bchDataManager;
    @Inject protected BuyConditions buyConditions;
    @Inject protected WalletOptionsState walletOptionsState;
    @Inject protected ShapeShiftDataManager shapeShiftDataManager;
    @Inject protected CoinifyDataManager coinifyDataManager;
    @Inject protected NabuDataManager nabuDataManager;
    @Inject protected OSUtil osUtil;

    {
        Injector.getInstance().getPresenterComponent().inject(this);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getIntent() != null && getIntent().getAction() != null) {
            if (getIntent().getAction().equals(AccessState.LOGOUT_ACTION)) {
                Intent intent = new Intent(this, WebSocketService.class);

                PrefsUtil prefsUtil = new PrefsUtil(this);

                //When user logs out, assume onboarding has been completed
                prefsUtil.setValue(PrefsUtil.KEY_ONBOARDING_COMPLETE, true);

                if (osUtil.isServiceRunning(WebSocketService.class)) {
                    stopService(intent);
                }

                // TODO: 21/02/2018 I'm not sure this is a great way to reset things, but it'll
                // do for now until we've had a rethink. Should individual datamanagers get
                // Rx events and handle their own state during logout?
                buyDataManager.wipe();
                ethDataManager.clearEthAccountDetails();
                bchDataManager.clearBchAccountDetails();
                shapeShiftDataManager.clearShapeShiftData();
                coinifyDataManager.clearAccessToken();
                nabuDataManager.clearAccessToken();
                DashboardPresenter.onLogout();

                buyConditions.wipe();
                walletOptionsState.wipe();

                AccessState.getInstance().setIsLoggedIn(false);
                finishAffinity();
            }
        }
    }
}
