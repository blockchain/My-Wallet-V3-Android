package piuk.blockchain.android.data.rxjava;

import io.reactivex.CompletableTransformer;
import io.reactivex.Observable;
import io.reactivex.ObservableTransformer;
import io.reactivex.Single;
import io.reactivex.SingleTransformer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;
import piuk.blockchain.androidcoreui.ui.base.BasePresenter;
import piuk.blockchain.android.util.extensions.RxCompositeExtensions;
import piuk.blockchain.androidcore.utils.extensions.RxSchedulingExtensions;
import timber.log.Timber;

/**
 * A class for basic RxJava utilities, ie Transformer classes
 *
 * @deprecated These functions have all been moved out to other, more locally scoped extension
 * classes. The only reason to use these is if you're not using Kotlin, which should be rectified
 * pretty quickly.
 */

@Deprecated
public final class RxUtil {

    /**
     * Applies standard Schedulers to an {@link Observable}, ie IO for subscription, Main Thread for
     * onNext/onComplete/onError
     *
     * @deprecated Use {@link RxSchedulingExtensions} instead if referenced from Kotlin.
     */
    @Deprecated
    public static <T> ObservableTransformer<T, T> applySchedulersToObservable() {
        return observable -> observable.subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnError(Timber::e);
    }

    /**
     * Applies standard Schedulers to a {@link io.reactivex.Completable}, ie IO for subscription,
     * Main Thread for onNext/onComplete/onError
     *
     * @deprecated Use {@link RxSchedulingExtensions} instead if referenced from Kotlin.
     */
    @Deprecated
    public static CompletableTransformer applySchedulersToCompletable() {
        return observable -> observable.subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnError(Timber::e);
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

    /**
     * Adds the subscription to the upstream {@link Observable} to the {@link CompositeDisposable}
     * supplied by a class extending {@link BasePresenter}. This allows the subscription to be
     * cancelled automatically by the Presenter on Android lifecycle events.
     *
     * @param presenter A class extending {@link BasePresenter}
     * @param <T>       The type of the upstream {@link Observable}
     * @deprecated Use {@link RxCompositeExtensions} instead if referenced from Kotlin.
     */
    @Deprecated
    public static <T> ObservableTransformer<T, T> addObservableToCompositeDisposable(BasePresenter presenter) {
        return upstream -> upstream.doOnSubscribe(disposable ->
                presenter.getCompositeDisposable().add(disposable));
    }

    /**
     * Adds the subscription to the upstream {@link io.reactivex.Completable} to the {@link
     * CompositeDisposable} supplied by a class extending {@link BasePresenter}. This allows the
     * subscription to be cancelled automatically by the Presenter on Android lifecycle events.
     *
     * @param presenter A class extending {@link BasePresenter}
     * @deprecated Use {@link RxCompositeExtensions} instead if referenced from Kotlin.
     */
    @Deprecated
    public static CompletableTransformer addCompletableToCompositeDisposable(BasePresenter presenter) {
        return upstream -> upstream.doOnSubscribe(disposable ->
                presenter.getCompositeDisposable().add(disposable));
    }

    /**
     * Adds the subscription to the upstream {@link Single} to the {@link CompositeDisposable}
     * supplied by a class extending {@link BasePresenter}. This allows the subscription to be
     * cancelled automatically by the Presenter on Android lifecycle events.
     *
     * @param presenter A class extending {@link BasePresenter}
     * @param <T>       The type of the upstream {@link Single}
     * @deprecated Use {@link RxCompositeExtensions} instead if referenced from Kotlin.
     */
    @Deprecated
    public static <T> SingleTransformer<T, T> addSingleToCompositeDisposable(BasePresenter presenter) {
        return upstream -> upstream.doOnSubscribe(disposable ->
                presenter.getCompositeDisposable().add(disposable));
    }

}
