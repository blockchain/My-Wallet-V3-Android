package info.blockchain.wallet.shapeshift.regulation

import com.blockchain.morph.regulation.UsState
import com.blockchain.morph.regulation.UsStatesDataManager
import io.reactivex.Completable
import io.reactivex.Observable
import piuk.blockchain.androidcore.data.shapeshift.ShapeShiftDataManager
import piuk.blockchain.androidcore.data.walletoptions.WalletOptionsDataManager

internal class ShapeShiftUsStatesDataManager(
    private val shapeShiftDataManager: ShapeShiftDataManager,
    private val walletOptionsDataManager: WalletOptionsDataManager
) : UsStatesDataManager {

    override fun setState(state: UsState): Completable {
        return shapeShiftDataManager.setState(info.blockchain.wallet.shapeshift.data.State(state.name, state.code))
    }

    override fun isStateWhitelisted(usState: UsState): Observable<Boolean> {
        return walletOptionsDataManager
            .isStateWhitelisted(usState.code)
    }
}
