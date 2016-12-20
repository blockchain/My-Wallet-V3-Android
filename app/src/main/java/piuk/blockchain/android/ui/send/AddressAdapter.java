package piuk.blockchain.android.ui.send;

import android.content.Context;
import android.databinding.DataBindingUtil;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

import java.util.List;

import piuk.blockchain.android.R;
import piuk.blockchain.android.databinding.ItemAddressBinding;
import piuk.blockchain.android.databinding.SpinnerItemBinding;
import piuk.blockchain.android.ui.account.ItemAccount;
import piuk.blockchain.android.util.MonetaryUtil;

public class AddressAdapter extends ArrayAdapter<ItemAccount> {

    private boolean showText;
    private boolean isBtc;
    private MonetaryUtil monetaryUtil;
    private String fiatUnits;
    private double exchangeRate;

    /**
     * Constructor that allows handling both BTC and Fiat
     */
    public AddressAdapter(Context context,
                          int textViewResourceId,
                          List<ItemAccount> accountList,
                          boolean showText,
                          boolean isBtc,
                          MonetaryUtil monetaryUtil,
                          String fiatUnits,
                          double exchangeRate) {
        super(context, textViewResourceId, accountList);
        this.showText = showText;
        this.isBtc = isBtc;
        this.monetaryUtil = monetaryUtil;
        this.fiatUnits = fiatUnits;
        this.exchangeRate = exchangeRate;
    }

    /**
     * BTC only constructor
     */
    public AddressAdapter(Context context,
                          int textViewResourceId,
                          List<ItemAccount> accountList,
                          boolean showText) {
        super(context, textViewResourceId, accountList);
        this.showText = showText;
        isBtc = true;
    }

    public void updateData(List<ItemAccount> accountList) {
        clear();
        addAll(accountList);
        notifyDataSetChanged();
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
            ItemAddressBinding binding = DataBindingUtil.inflate(
                    LayoutInflater.from(getContext()),
                    R.layout.item_address,
                    parent,
                    false);

            ItemAccount item = getItem(position);

            if (item.tag == null || item.tag.isEmpty()) {
                binding.tvTag.setVisibility(View.GONE);
            } else {
                binding.tvTag.setText(item.tag);
            }
            binding.tvLabel.setText(item.label);

            if (isBtc) {
                binding.tvBalance.setText(item.displayBalance);
            } else {
                double btcBalance = item.absoluteBalance / 1e8;
                double fiatBalance = exchangeRate * btcBalance;

                String balance = monetaryUtil.getFiatFormat(fiatUnits).format(Math.abs(fiatBalance)) + " " + fiatUnits;
                binding.tvBalance.setText(balance);
            }

            return binding.getRoot();

        } else {
            SpinnerItemBinding binding = DataBindingUtil.inflate(
                    LayoutInflater.from(getContext()),
                    R.layout.spinner_item,
                    parent,
                    false);

            if (showText) {
                ItemAccount item = getItem(position);
                binding.text.setText(item.label);
            }
            return binding.getRoot();
        }
    }
}
