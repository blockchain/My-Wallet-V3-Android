package piuk.blockchain.android.data.rxjava;

import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Func1;
import rx.schedulers.Schedulers;

/**
 * Created by adambennett on 12/08/2016.
 *
 * A class for basic RxJava utilities, ie Transformer classes
 */

public class RxUtil {

    /**
     * Applies standard Schedulers to an Observable, ie IO for subscription, Main Thread for
     * onNext/onComplete/onError
     */
    public static <T> Observable.Transformer<T, T> applySchedulers() {
        return observable -> observable.subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    }

    /**
     * Allows you to call two different {@link Observable} objects based on result of a predicate.
     */
    public static <T, R> Func1<? super T, ? extends Observable<? extends R>> ternary(
            Func1<T, Boolean> predicate,
            Func1<? super T, ? extends Observable<? extends R>> ifTrue,
            Func1<? super T, ? extends Observable<? extends R>> ifFalse) {
        return (item) -> predicate.call(item)
                ? ifTrue.call(item)
                : ifFalse.call(item);
    }
}
