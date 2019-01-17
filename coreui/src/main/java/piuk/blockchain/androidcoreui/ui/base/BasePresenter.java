package piuk.blockchain.androidcoreui.ui.base;

import android.support.annotation.CallSuper;

import io.reactivex.disposables.CompositeDisposable;

public abstract class BasePresenter<VIEW extends View> implements Presenter<VIEW> {

    private CompositeDisposable compositeDisposable;
    private VIEW view;

    protected BasePresenter() {
        compositeDisposable = new CompositeDisposable();
    }

    public CompositeDisposable getCompositeDisposable() {
        return compositeDisposable;
    }

    @Override
    public void onViewResumed() {
        // No-op
    }

    @Override
    public void onViewPaused() {
        // No-op
    }

    @Override
    public void initView(VIEW view) {
        this.view = view;
    }

    @Override
    public VIEW getView() {
        // TODO: 28/02/2018 In the future, migrate this to Kotlin and mark as nullable
        return view;
    }

    @CallSuper
    @Override
    public void onViewDestroyed() {
        /* Clear all subscriptions so that:
         * 1) all processes are cancelled
         * 2) processes don't try to update a null View
         * 3) background processes don't leak memory
         */
        getCompositeDisposable().clear();

        /*
         * Being explicit here prevents holding onto a View reference unnecessarily
         */
        view = null;
    }

    public abstract void onViewReady();

}
