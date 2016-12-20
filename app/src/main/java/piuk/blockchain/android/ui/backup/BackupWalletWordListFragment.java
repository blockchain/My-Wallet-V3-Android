package piuk.blockchain.android.ui.backup;

import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.Context;
import android.databinding.DataBindingUtil;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.InputMethodManager;

import piuk.blockchain.android.R;
import piuk.blockchain.android.databinding.FragmentBackupWordListBinding;
import piuk.blockchain.android.util.BackupWalletUtil;
import piuk.blockchain.android.util.annotations.Thunk;

public class BackupWalletWordListFragment extends Fragment {

    @Thunk FragmentBackupWordListBinding binding;
    @Thunk Animation animEnterFromRight;
    @Thunk Animation animEnterFromLeft;
    private Animation animExitToLeft;
    private Animation animExitToRight;

    int currentWordIndex = 0;
    @Thunk String[] mnemonic;
    @Thunk String word;
    private String of;
    private String secondPassword;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_backup_word_list, container, false);

        ActionBar supportActionBar = ((AppCompatActivity) getActivity()).getSupportActionBar();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && supportActionBar != null) {
            supportActionBar.setElevation(0F);
        }

        Bundle bundle = getArguments();
        if (bundle != null) {
            secondPassword = bundle.getString("second_password");
        }

        word = getResources().getString(R.string.backup_word);
        of = getResources().getString(R.string.backup_of);

        animExitToLeft = AnimationUtils.loadAnimation(getActivity(), R.anim.exit_to_left);
        animEnterFromRight = AnimationUtils.loadAnimation(getActivity(), R.anim.enter_from_right);

        animExitToRight = AnimationUtils.loadAnimation(getActivity(), R.anim.exit_to_right);
        animEnterFromLeft = AnimationUtils.loadAnimation(getActivity(), R.anim.enter_from_left);

        mnemonic = new BackupWalletUtil().getMnemonic(secondPassword);
        if (currentWordIndex == mnemonic.length) {
            currentWordIndex = 0;
        }
        binding.tvCurrentWord.setText(word + " " + (currentWordIndex + 1) + " " + of + " 12");
        binding.tvPressReveal.setText(mnemonic[currentWordIndex]);

        binding.nextWordAction.setOnClickListener(v -> {

            if (currentWordIndex >= 0) {
                binding.previousWordAction.setVisibility(View.VISIBLE);
            } else {
                binding.previousWordAction.setVisibility(View.GONE);
            }

            if (currentWordIndex < mnemonic.length) {

                animExitToLeft.setAnimationListener(new Animation.AnimationListener() {
                    @Override
                    public void onAnimationStart(Animation animation) {
                        binding.tvPressReveal.setText("");
                        binding.tvCurrentWord.setText(word + " " + (currentWordIndex + 1) + " " + of + " 12");
                    }

                    @Override
                    public void onAnimationRepeat(Animation animation) {
                        // No-op
                    }

                    @Override
                    public void onAnimationEnd(Animation animation) {
                        binding.cardLayout.startAnimation(animEnterFromRight);
                        binding.tvPressReveal.setText(mnemonic[currentWordIndex]);
                    }
                });

                binding.cardLayout.startAnimation(animExitToLeft);

                currentWordIndex++;
            }

            if (currentWordIndex == mnemonic.length) {

                currentWordIndex = 0;

                Fragment fragment = new BackupWalletVerifyFragment();
                if (secondPassword != null) {
                    Bundle args = new Bundle();
                    args.putString("second_password", secondPassword);
                    fragment.setArguments(args);
                }

                getFragmentManager().beginTransaction()
                        .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                        .replace(R.id.content_frame, fragment)
                        .addToBackStack(null)
                        .commit();
            } else {

                if (currentWordIndex == mnemonic.length - 1) {
                    binding.nextWordAction.setText(getResources().getString(R.string.backup_done));
                } else {
                    binding.nextWordAction.setText(getResources().getString(R.string.backup_next_word));
                }
            }
        });

        binding.previousWordAction.setOnClickListener(v1 -> {

            binding.nextWordAction.setText(getResources().getString(R.string.backup_next_word));

            if (currentWordIndex == 1) {
                binding.previousWordAction.setVisibility(View.GONE);
            }

            animExitToRight.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {
                    binding.tvPressReveal.setText("");
                    binding.tvCurrentWord.setText(word + " " + (currentWordIndex + 1) + " " + of + " 12");
                }

                @Override
                public void onAnimationRepeat(Animation animation) {
                    // No-op
                }

                @Override
                public void onAnimationEnd(Animation animation) {
                    binding.cardLayout.startAnimation(animEnterFromLeft);
                    binding.tvPressReveal.setText(mnemonic[currentWordIndex]);
                }
            });

            binding.cardLayout.startAnimation(animExitToRight);

            currentWordIndex--;
        });

        return binding.getRoot();
    }

    @Override
    public void onResume() {
        super.onResume();
        currentWordIndex = 0;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        View view = getActivity().getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }
}