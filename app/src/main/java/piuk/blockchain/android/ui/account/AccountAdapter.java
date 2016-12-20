package piuk.blockchain.android.ui.account;

import android.graphics.drawable.Drawable;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;

import piuk.blockchain.android.R;

class AccountAdapter extends RecyclerView.Adapter<AccountAdapter.ViewHolder> {

    private static final int TYPE_IMPORTED_HEADER = -1;
    private static final int TYPE_CREATE_NEW_WALLET_BUTTON = -2;
    private static final int TYPE_IMPORT_ADDRESS_BUTTON = -3;
    private ArrayList<AccountItem> items;
    private AccountHeadersListener listener;

    AccountAdapter(ArrayList<AccountItem> myAccountItems) {
        items = myAccountItems;
    }

    @Override
    public AccountAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.activity_accounts_row, parent, false);

        if (viewType == TYPE_IMPORTED_HEADER) {
            v = LayoutInflater.from(parent.getContext()).inflate(R.layout.activity_accounts_row_header, parent, false);

        } else if (viewType == TYPE_CREATE_NEW_WALLET_BUTTON || viewType == TYPE_IMPORT_ADDRESS_BUTTON) {
            v = LayoutInflater.from(parent.getContext()).inflate(R.layout.activity_accounts_row_buttons, parent, false);
        }

        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        // Imported Items header
        if (holder.getItemViewType() == TYPE_IMPORTED_HEADER)
            return;

        // Create new wallet button
        if (holder.getItemViewType() == TYPE_CREATE_NEW_WALLET_BUTTON) {
            holder.description.setText(R.string.create_new);

            holder.cardView.setOnClickListener(v -> {
                if (listener != null) listener.onCreateNewClicked();
            });
            return;
        }

        // Import address button
        if (holder.getItemViewType() == TYPE_IMPORT_ADDRESS_BUTTON) {
            holder.description.setText(R.string.import_address);

            holder.cardView.setOnClickListener(v -> {
                if (listener != null) listener.onImportAddressClicked();
            });
            return;
        }

        AccountItem accountItem = items.get(position);

        // Normal account view
        holder.cardView.setOnClickListener(v -> {
            if (listener != null) listener.onCardClicked(accountItem.getCorrectPosition());
        });

        holder.title.setText(accountItem.getLabel());

        if (!accountItem.getAddress().isEmpty()) {
            holder.address.setVisibility(View.VISIBLE);
            holder.address.setText(accountItem.getAddress());
        } else {
            holder.address.setVisibility(View.GONE);
        }

        if (accountItem.isArchived()) {
            holder.amount.setText(R.string.archived_label);
            holder.amount.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.blockchain_transfer_blue));
        } else {
            holder.amount.setText(accountItem.getAmount());
            holder.amount.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.blockchain_receive_green));
        }

        if (accountItem.isWatchOnly()) {
            holder.tag.setText(holder.itemView.getContext().getString(R.string.watch_only));
            holder.tag.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.blockchain_send_red));
        }

        if (accountItem.isDefault()) {
            holder.tag.setText(holder.itemView.getContext().getString(R.string.default_label));
            holder.tag.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.blockchain_grey));
        }

        if (!accountItem.isWatchOnly() && !accountItem.isDefault()) {
            holder.tag.setVisibility(View.GONE);
        } else {
            holder.tag.setVisibility(View.VISIBLE);
        }

        Drawable drawable = accountItem.getIcon();
        if (drawable != null) {
            holder.icon.setImageDrawable(drawable);
        }
    }

    @Override
    public int getItemCount() {
        return !items.isEmpty() ? items.size() : 0;
    }

    @Override
    public int getItemViewType(int position) {
        String title = items.get(position).getLabel();

        if (title.equals(AccountActivity.IMPORTED_HEADER)) {
            return TYPE_IMPORTED_HEADER;
        } else if (title.equals(AccountActivity.IMPORT_ADDRESS)) {
            return TYPE_IMPORT_ADDRESS_BUTTON;
        } else if (title.equals(AccountActivity.CREATE_NEW)) {
            return TYPE_CREATE_NEW_WALLET_BUTTON;
        }

        return 0;
    }

    void setAccountHeaderListener(AccountHeadersListener accountHeadersListener) {
        listener = accountHeadersListener;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        CardView cardView;
        TextView title;
        TextView address;
        ImageView icon;
        TextView amount;
        TextView tag;
        TextView description;

        ViewHolder(View view) {
            super(view);
            cardView = (CardView) view.findViewById(R.id.card_view);
            title = (TextView) view.findViewById(R.id.my_account_row_label);
            address = (TextView) view.findViewById(R.id.my_account_row_address);
            icon = (ImageView) view.findViewById(R.id.my_account_row_icon);
            amount = (TextView) view.findViewById(R.id.my_account_row_amount);
            tag = (TextView) view.findViewById(R.id.my_account_row_tag);
            description = (TextView) view.findViewById(R.id.description);
        }
    }

    interface AccountHeadersListener {

        void onCreateNewClicked();

        void onImportAddressClicked();

        void onCardClicked(int correctedPosition);
    }
}