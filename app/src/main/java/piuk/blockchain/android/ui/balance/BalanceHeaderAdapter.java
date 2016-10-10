package piuk.blockchain.android.ui.balance;

import android.content.Context;
import android.databinding.DataBindingUtil;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

import java.util.List;

import piuk.blockchain.android.R;
import piuk.blockchain.android.databinding.ItemBalanceAccountDropdownBinding;
import piuk.blockchain.android.databinding.SpinnerBalanceHeaderBinding;
import piuk.blockchain.android.ui.account.ItemAccount;

class BalanceHeaderAdapter extends ArrayAdapter<ItemAccount> {

    BalanceHeaderAdapter(Context context, int textViewResourceId, List<ItemAccount> accountList) {
        super(context, textViewResourceId, accountList);
    }

    @Override
    public View getDropDownView(int position, View convertView, @NonNull ViewGroup parent) {
        return getCustomView(position, parent, true);
    }

    @NonNull
    @Override
    public View getView(int position, View convertView, @NonNull ViewGroup parent) {
        return getCustomView(position, parent, false);
    }

    private View getCustomView(int position, ViewGroup parent, boolean isDropdownView) {
        if (isDropdownView) {
            ItemBalanceAccountDropdownBinding binding = DataBindingUtil.inflate(
                    LayoutInflater.from(getContext()),
                    R.layout.item_balance_account_dropdown,
                    parent,
                    false);

            ItemAccount item = getItem(position);
            assert item != null;

            binding.accountName.setText(item.label);
            binding.balance.setText(item.balance);

            return binding.getRoot();

        } else {
            SpinnerBalanceHeaderBinding binding = DataBindingUtil.inflate(
                    LayoutInflater.from(getContext()),
                    R.layout.spinner_balance_header,
                    parent,
                    false);

            ItemAccount item = getItem(position);
            assert item != null;

            binding.text.setText(item.label);
            return binding.getRoot();
        }
    }
}
