package piuk.blockchain.android.ui.upgrade;

import android.content.Context;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.annotation.StringRes;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AlertDialog;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import javax.inject.Inject;

import piuk.blockchain.android.R;
import piuk.blockchain.android.databinding.ActivityUpgradeWalletBinding;
import piuk.blockchain.android.injection.Injector;
import com.blockchain.ui.password.SecondPasswordHandler;
import piuk.blockchain.androidcoreui.ui.base.BaseMvpActivity;
import piuk.blockchain.androidcoreui.ui.customviews.MaterialProgressDialog;
import piuk.blockchain.androidcoreui.ui.customviews.ToastCustom;

public class UpgradeWalletActivity extends BaseMvpActivity<UpgradeWalletView, UpgradeWalletPresenter>
        implements UpgradeWalletView,
        ViewPager.OnPageChangeListener {

    private ActivityUpgradeWalletBinding binding;
    private MaterialProgressDialog progressDialog;
    @Inject UpgradeWalletPresenter upgradeWalletPresenter;

    {
        Injector.getInstance().getPresenterComponent().inject(this);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = DataBindingUtil.setContentView(this, R.layout.activity_upgrade_wallet);

        binding.upgradePageHeader.setFactory(() -> {
            TextView textView = new TextView(this);
            textView.setGravity(Gravity.CENTER);
            textView.setTextSize(14);
            textView.setTextColor(ContextCompat.getColor(this, R.color.primary_navy_medium));
            return textView;
        });

        binding.upgradePageHeader.setInAnimation(AnimationUtils.loadAnimation(this, R.anim.fade_in));
        binding.upgradePageHeader.setOutAnimation(AnimationUtils.loadAnimation(this, R.anim.fade_out));
        binding.upgradePageHeader.setText(getResources().getString(R.string.upgrade_page_1));

        CustomPagerAdapter adapter = new CustomPagerAdapter(this);
        binding.pager.setAdapter(adapter);
        binding.pager.addOnPageChangeListener(this);

        binding.upgradeBtn.setOnClickListener(v -> upgradeClicked());

        onViewReady();
    }

    @Override
    protected UpgradeWalletPresenter createPresenter() {
        return upgradeWalletPresenter;
    }

    @Override
    protected UpgradeWalletView getView() {
        return this;
    }

    @Override
    public void showChangePasswordDialog() {
        final LinearLayout pwLayout =
                (LinearLayout) getLayoutInflater().inflate(R.layout.modal_change_password, null);

        new AlertDialog.Builder(this, R.style.AlertDialogStyle)
                .setTitle(R.string.app_name)
                .setMessage(R.string.weak_password)
                .setCancelable(false)
                .setView(pwLayout)
                .setPositiveButton(R.string.yes, (dialog, whichButton) -> {
                    String password1 = ((EditText) pwLayout.findViewById(R.id.pw1)).getText().toString();
                    String password2 = ((EditText) pwLayout.findViewById(R.id.pw2)).getText().toString();
                    getPresenter().submitPasswords(password1, password2);
                })
                .setNegativeButton(R.string.no, (dialog, whichButton) ->
                        showToast(R.string.password_unchanged, ToastCustom.TYPE_GENERAL))
                .show();
    }

    @Override
    public void showToast(@StringRes int message, @ToastCustom.ToastType String type) {
        ToastCustom.makeText(this, getString(message), ToastCustom.LENGTH_SHORT, type);
    }

    @Override
    public void onUpgradeStarted() {
        binding.upgradePageTitle.setText(getString(R.string.upgrading));
        binding.upgradePageHeader.setText(getString(R.string.upgrading_started_info));
        binding.progressBar.setVisibility(View.VISIBLE);
        binding.pager.setVisibility(View.GONE);
        binding.upgradeActionContainer.setVisibility(View.GONE);
    }

    @Override
    public void onUpgradeCompleted() {
        binding.upgradePageTitle.setText(getString(R.string.upgrade_success_heading));
        binding.upgradePageHeader.setText(getString(R.string.upgrade_success_info));
        binding.progressBar.setVisibility(View.GONE);
        binding.btnUpgradeComplete.setVisibility(View.VISIBLE);
        binding.btnUpgradeComplete.setOnClickListener(v -> getPresenter().onContinueClicked());
    }

    @Override
    public void onUpgradeFailed() {
        binding.upgradePageTitle.setText(getString(R.string.upgrade_fail_heading));
        binding.upgradePageHeader.setText(getString(R.string.upgrade_fail_info));
        binding.progressBar.setVisibility(View.GONE);
        binding.btnUpgradeComplete.setVisibility(View.VISIBLE);
        binding.btnUpgradeComplete.setText(getString(R.string.CLOSE));
        binding.btnUpgradeComplete.setOnClickListener(v -> onBackPressed());
    }

    @Override
    public void onBackButtonPressed() {
        super.onBackPressed();
    }

    @Override
    public void onBackPressed() {
        getPresenter().onBackButtonPressed(this);
    }

    @Override
    public void showProgressDialog(@StringRes int message) {
        progressDialog = new MaterialProgressDialog(this);
        progressDialog.setCancelable(false);
        progressDialog.setMessage(message);
        progressDialog.show();
    }

    @Override
    public void dismissProgressDialog() {
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
            progressDialog = null;
        }
    }

    @Override
    public void onPageSelected(int position) {
        setSelectedPage(position);
    }

    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
        // No-op
    }

    @Override
    public void onPageScrollStateChanged(int state) {
        // No-op
    }

    private void upgradeClicked() {
        getSecondPasswordHandler().validate(new SecondPasswordHandler.ResultListener() {
            @Override
            public void onNoSecondPassword() {
                getPresenter().onUpgradeRequested(null);
            }

            @Override
            public void onSecondPasswordValidated(String validatedSecondPassword) {
                getPresenter().onUpgradeRequested(validatedSecondPassword);
            }
        });
    }

    private void setSelectedPage(int position) {
        switch (position) {
            case 0:
                binding.upgradePageHeader.setText(getResources().getString(R.string.upgrade_page_1));
                setBackground(binding.pageBox0, R.drawable.rounded_view_accent_blue);
                setBackground(binding.pageBox1, R.drawable.rounded_view_dark_blue);
                setBackground(binding.pageBox2, R.drawable.rounded_view_dark_blue);
                break;
            case 1:
                binding.upgradePageHeader.setText(getResources().getString(R.string.upgrade_page_2));
                setBackground(binding.pageBox0, R.drawable.rounded_view_dark_blue);
                setBackground(binding.pageBox1, R.drawable.rounded_view_accent_blue);
                setBackground(binding.pageBox2, R.drawable.rounded_view_dark_blue);
                break;
            case 2:
                binding.upgradePageHeader.setText(getResources().getString(R.string.upgrade_page_3));
                setBackground(binding.pageBox0, R.drawable.rounded_view_dark_blue);
                setBackground(binding.pageBox1, R.drawable.rounded_view_dark_blue);
                setBackground(binding.pageBox2, R.drawable.rounded_view_accent_blue);
                break;
        }
    }

    private void setBackground(View view, int res) {
        view.setBackground(ContextCompat.getDrawable(this, res));
    }

    private static class CustomPagerAdapter extends PagerAdapter {

        private Context context;
        private LayoutInflater inflater;
        private int[] resources = {
                R.drawable.upgrade_backup,
                R.drawable.upgrade_hd,
                R.drawable.upgrade_balance
        };

        CustomPagerAdapter(Context context) {
            this.context = context;
            inflater = (LayoutInflater) this.context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        }

        @Override
        public int getCount() {
            return resources.length;
        }

        @Override
        public boolean isViewFromObject(View view, Object object) {
            return view == object;
        }

        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            View itemView = inflater.inflate(R.layout.activity_upgrade_wallet_pager_item, container, false);

            ImageView imageView = (ImageView) itemView.findViewById(R.id.imageView);
            imageView.setImageResource(resources[position]);

            container.addView(itemView);

            return itemView;
        }

        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            container.removeView((LinearLayout) object);
        }
    }

}