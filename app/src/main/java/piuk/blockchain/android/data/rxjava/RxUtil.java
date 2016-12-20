package piuk.blockchain.android.data.rxjava;

import io.reactivex.CompletableTransformer;
import io.reactivex.Observable;
import io.reactivex.ObservableTransformer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;

/**
 * Created by adambennett on 12/08/2016.
 *
 * A class for basic RxJava utilities, ie Transformer classes
 */

public final class RxUtil {

    /**
     * Applies standard Schedulers to an {@link Observable}, ie IO for subscription, Main Thread for
     * onNext/onComplete/onError
     */
    public static <T> ObservableTransformer<T, T> applySchedulersToObservable() {
        return observable -> observable.subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    }

    /**
     * Applies standard Schedulers to a {@link io.reactivex.Completable}, ie IO for subscription,
     * Main Thread for onNext/onComplete/onError
     */
    public static CompletableTransformer applySchedulersToCompletable() {
        return observable -> observable.subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    }

    /**
     * Allows you to call two different {@link Observable} objects based on result of a predicate.
     */
    public static <T, R> Function<? super T, ? extends Observable<? extends R>> ternary(
            Function<T, Boolean> predicate,
            Function<? super T, ? extends Observable<? extends R>> ifTrue,
            Function<? super T, ? extends Observable<? extends R>> ifFalse) {
        return (item) -> predicate.apply(item)
                ? ifTrue.apply(item)
                : ifFalse.apply(item);
    }
}
