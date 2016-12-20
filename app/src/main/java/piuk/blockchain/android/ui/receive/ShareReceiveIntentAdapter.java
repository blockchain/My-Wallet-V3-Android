package piuk.blockchain.android.ui.receive;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;

import piuk.blockchain.android.R;

public class ShareReceiveIntentAdapter extends RecyclerView.Adapter<ShareReceiveIntentAdapter.ViewHolder> {

    private final List<ReceiveViewModel.SendPaymentCodeData> mData;
    private OnItemClickedListener itemClickedListener;
    private Context mContext;

    public ShareReceiveIntentAdapter(List<ReceiveViewModel.SendPaymentCodeData> repoDataArrayList) {
        mData = repoDataArrayList;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        mContext = parent.getContext();
        LayoutInflater inflater = LayoutInflater.from(mContext);
        View row = inflater.inflate(R.layout.receive_share_row, parent, false);

        return new ViewHolder(row);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        ReceiveViewModel.SendPaymentCodeData data = mData.get(position);

        holder.mTitleTextView.setText(data.getTitle());
        holder.mImageView.setImageDrawable(data.getLogo());

        holder.mRootView.setOnClickListener(view -> {
            if (itemClickedListener != null) itemClickedListener.onItemClicked();
            mContext.startActivity(data.getIntent());
        });
    }

    @Override
    public int getItemCount() {
        return mData != null ? mData.size() : 0;
    }

    public void setItemClickedListener(OnItemClickedListener itemClickedListener) {
        this.itemClickedListener = itemClickedListener;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        ImageView mImageView;
        TextView mTitleTextView;
        View mRootView;

        ViewHolder(View itemView) {
            super(itemView);
            mRootView = itemView;
            mImageView = (ImageView) itemView.findViewById(R.id.share_app_image);
            mTitleTextView = (TextView) itemView.findViewById(R.id.share_app_title);
        }
    }

    interface OnItemClickedListener {

        void onItemClicked();

    }
}
