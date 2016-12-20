package piuk.blockchain.android.data.rxjava;

import io.reactivex.CompletableObserver;
import io.reactivex.observers.DefaultObserver;

/**
 * To be used when the result of the subscription can be ignored
 */
public class IgnorableDefaultObserver<T> extends DefaultObserver<T> implements CompletableObserver {

    @Override
    public void onComplete() {
        // No-op
    }

    @Override
    public void onError(Throwable e) {
        // No-op
    }

    @Override
    public void onNext(Object o) {
        // No-op
    }
}