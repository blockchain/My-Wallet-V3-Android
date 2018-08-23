package info.blockchain.wallet.shapeshift.regulation

import com.blockchain.morph.regulation.UsState
import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.argumentCaptor
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.verifyZeroInteractions
import io.reactivex.Completable
import io.reactivex.Observable
import org.amshove.kluent.`it returns`
import org.amshove.kluent.`should be`
import org.amshove.kluent.`should equal`
import org.junit.Test
import piuk.blockchain.androidcore.data.shapeshift.ShapeShiftDataManager
import piuk.blockchain.androidcore.data.walletoptions.WalletOptionsDataManager

class ShapeShiftUsStatesDataManagerTest {

    @Test
    fun `isStateWhitelisted delegates to WalletOptionsDataManager`() {
        val shapeShiftDataManager = mock<ShapeShiftDataManager>()
        val result = mock<Observable<Boolean>>()
        val walletOptionsDataManager = mock<WalletOptionsDataManager> {
            on { isStateWhitelisted("code") } `it returns` result
        }
        ShapeShiftUsStatesDataManager(shapeShiftDataManager, walletOptionsDataManager)
            .isStateWhitelisted(UsState("", "code")) `should be` result
        verifyZeroInteractions(shapeShiftDataManager)
    }

    @Test
    fun `setState delegates to ShapeShiftDataManager`() {
        val result = mock<Completable>()
        val shapeShiftDataManager = mock<ShapeShiftDataManager> {
            on { setState(any()) } `it returns` result
        }
        val walletOptionsDataManager = mock<WalletOptionsDataManager>()
        ShapeShiftUsStatesDataManager(shapeShiftDataManager, walletOptionsDataManager)
            .setState(UsState("The State name", "The State code")) `should be` result
        argumentCaptor<info.blockchain.wallet.shapeshift.data.State>().apply {
            verify(shapeShiftDataManager).setState(capture())
            firstValue.name `should equal` "The State name"
            firstValue.code `should equal` "The State code"
        }
        verifyZeroInteractions(walletOptionsDataManager)
    }
}
