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
import piuk.blockchain.android.util.MonetaryUtil;

class BalanceHeaderAdapter extends ArrayAdapter<ItemAccount> {

    private boolean isBtc;
    private final MonetaryUtil monetaryUtil;
    private String fiatUnits;
    private double exchangeRate;

    BalanceHeaderAdapter(Context context,
                         int textViewResourceId,
                         List<ItemAccount> accountList,
                         boolean isBtc,
                         MonetaryUtil monetaryUtil,
                         String fiatUnits,
                         double exchangeRate) {
        super(context, textViewResourceId, accountList);
        this.isBtc = isBtc;
        this.monetaryUtil = monetaryUtil;
        this.fiatUnits = fiatUnits;
        this.exchangeRate = exchangeRate;
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

            binding.accountName.setText(item.label);

            if (isBtc) {
                binding.balance.setText(item.displayBalance);
            } else {
                double btcBalance = item.absoluteBalance / 1e8;
                double fiatBalance = exchangeRate * btcBalance;

                String balance = monetaryUtil.getFiatFormat(fiatUnits).format(Math.abs(fiatBalance)) + " " + fiatUnits;
                binding.balance.setText(balance);
            }

            return binding.getRoot();

        } else {
            SpinnerBalanceHeaderBinding binding = DataBindingUtil.inflate(
                    LayoutInflater.from(getContext()),
                    R.layout.spinner_balance_header,
                    parent,
                    false);

            ItemAccount item = getItem(position);

            binding.text.setText(item.label);
            return binding.getRoot();
        }
    }

    void notifyBtcChanged(boolean isBtc) {
        this.isBtc = isBtc;
        notifyDataSetChanged();
    }

    void notifyFiatUnitsChanged(String fiatUnits, double exchangeRate) {
        this.fiatUnits = fiatUnits;
        this.exchangeRate = exchangeRate;
        notifyDataSetChanged();
    }
}
