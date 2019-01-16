package com.meida.adapter;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.daimajia.swipe.SwipeLayout;
import com.daimajia.swipe.adapters.RecyclerSwipeAdapter;
import com.lqr.ninegridimageview.LQRNineGridImageView;
import com.lqr.ninegridimageview.LQRNineGridImageViewAdapter;
import com.meida.freedconn.R;
import com.meida.model.CommonData;
import com.meida.share.BaseHttp;

import java.util.ArrayList;
import java.util.List;

/**
 * 项目名称：FreedConn
 * 创建人：小卷毛
 * 创建时间：2019-01-03 17:50
 */
public class ContactAdapter extends RecyclerSwipeAdapter<RecyclerView.ViewHolder> {

    private static final int ITEM_TYPE_SWIPE = 1001;
    private static final int ITEM_TYPE_NO_SWIPE = 1002;
    private Context mContext;
    private List<CommonData> mDatas;
    private OnItemClickListener onItemClickListener;
    private OnItemDeleteClickListener onItemDeleteClickListener;

    public ContactAdapter(Context context) {
        mDatas = new ArrayList<>();
        mContext = context;
    }

    public void updateData(List<CommonData> datas) {
        mDatas.clear();
        mDatas.addAll(datas);
        notifyDataSetChanged();
    }

    public void setOnItemClickListener(OnItemClickListener onItemClickListener) {
        this.onItemClickListener = onItemClickListener;
    }

    public void setOnItemDeleteClickListener(OnItemDeleteClickListener onItemDeleteClickListener) {
        this.onItemDeleteClickListener = onItemDeleteClickListener;
    }

    private LayoutInflater getLayoutInflater() {
        return LayoutInflater.from(mContext);
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        if (viewType == ITEM_TYPE_SWIPE) {
            View view = getLayoutInflater().inflate(R.layout.item_contact_list, parent, false);
            return new ItemSwipeViewHolder(view);
        } else {
            View view = getLayoutInflater().inflate(R.layout.item_contact_header, parent, false);
            return new ItemNoSwipeViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(final RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof ItemSwipeViewHolder) {
            final ItemSwipeViewHolder swipeHolder = (ItemSwipeViewHolder) holder;
            swipeHolder.swipe.setShowMode(SwipeLayout.ShowMode.PullOut);
            swipeHolder.bind(mDatas.get(position));
            swipeHolder.mContent.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (onItemClickListener != null)
                        onItemClickListener.onClick(holder.getAdapterPosition());
                }
            });
            swipeHolder.tvDel.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mItemManger.removeShownLayouts(swipeHolder.swipe);
                    mItemManger.closeAllItems();
                    if (onItemDeleteClickListener != null)
                        onItemDeleteClickListener.onDelete(holder.getAdapterPosition());
                }
            });
            mItemManger.bindView(swipeHolder.itemView, position);
        } else {
            ItemNoSwipeViewHolder noSwipeHolder = (ItemNoSwipeViewHolder) holder;
            noSwipeHolder.bind(mDatas.get(position));
            noSwipeHolder.mContent.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (onItemClickListener != null)
                        onItemClickListener.onClick(holder.getAdapterPosition());
                }
            });
        }
    }

    @Override
    public int getItemViewType(int position) {
        return position == 0 ? ITEM_TYPE_NO_SWIPE : ITEM_TYPE_SWIPE;
    }

    @Override
    public int getItemCount() {
        return mDatas.size();
    }

    @Override
    public int getSwipeLayoutResourceId(int position) {
        return position > 0 ? R.id.item_contact_swipe : -1;
    }

    class ItemSwipeViewHolder extends RecyclerView.ViewHolder {

        SwipeLayout swipe;
        LQRNineGridImageView nineImg;
        TextView tvName;
        TextView tvDel;
        View mContent;

        ItemSwipeViewHolder(@NonNull View itemView) {
            super(itemView);
            swipe = itemView.findViewById(R.id.item_contact_swipe);
            nineImg = itemView.findViewById(R.id.item_contact_nine);
            tvName = itemView.findViewById(R.id.item_contact_name);
            tvDel = itemView.findViewById(R.id.item_contact_del);
            mContent = itemView.findViewById(R.id.item_contact);
        }

        public void bind(CommonData data) {
            if (TextUtils.isEmpty(data.getClusterId())) tvName.setText(data.getUserName());
            else tvName.setText(data.getClusterName());

            nineImg.setAdapter(new LQRNineGridImageViewAdapter<String>() {
                @Override
                protected void onDisplayImage(Context context, ImageView imageView, String url) {
                    Glide.with(context).load(url)
                            .apply(RequestOptions
                                    .centerCropTransform()
                                    .placeholder(R.mipmap.default_logo)
                                    .error(R.mipmap.default_logo)
                                    .dontAnimate())
                            .into(imageView);
                }
            });

            ArrayList<String> list = new ArrayList<>();
            if (TextUtils.isEmpty(data.getClusterId())) {
                list.add(BaseHttp.INSTANCE.getBaseImg() + data.getUserHead());
            } else {
                List<CommonData> items = data.getClusterMembers();
                if (items != null)
                    for (CommonData item: items)
                        if (item != null)
                            list.add(BaseHttp.INSTANCE.getBaseImg() + item.getUserHead());
            }

            nineImg.setImagesData(list);
        }
    }

    class ItemNoSwipeViewHolder extends RecyclerView.ViewHolder {

        TextView tvNum;
        View mContent;

        ItemNoSwipeViewHolder(@NonNull View itemView) {
            super(itemView);
            tvNum = itemView.findViewById(R.id.item_contact_num);
            mContent = itemView.findViewById(R.id.item_contact);
        }

        public void bind(CommonData data) {
            tvNum.setText(data.getRequtsetCount());
        }
    }

    public interface OnItemClickListener {
        void onClick(int position);
    }

    public interface OnItemDeleteClickListener {
        void onDelete(int position);
    }

}
