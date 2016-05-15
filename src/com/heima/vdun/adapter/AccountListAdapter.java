package com.heima.vdun.adapter;

import java.util.List;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.heima.vdun.R;
import com.heima.vdun.entity.AccountBean;
import com.heima.vdun.util.ImageDownloader;

public class AccountListAdapter extends BaseAdapter {

	private List<AccountBean> list;
	private LayoutInflater inflater;
	private ImageDownloader downloader;
	public AccountListAdapter(List<AccountBean> list, Context context) {
		this.list = list;
		inflater = LayoutInflater.from(context);
		downloader = new ImageDownloader();
	}

	public int getCount() {

		return list.size();
	}

	public Object getItem(int position) {

		return list.get(position);
	}

	public long getItemId(int position) {

		return position;
	}

	public View getView(int position, View convertView, ViewGroup parent) {

		ViewHolder holder;
		if (convertView == null) {
			convertView = inflater.inflate(R.layout.account_list_item, null);
			holder = new ViewHolder();
			holder.image = (ImageView) convertView
					.findViewById(R.id.iv_account_list_icon);
			holder.name = (TextView) convertView
			.findViewById(R.id.tv_account_list_name);
			holder.account = (TextView) convertView
			.findViewById(R.id.tv_account_list_account);
			
			convertView.setTag(holder);
		}

		AccountBean info = list.get(position);
		holder = (ViewHolder) convertView.getTag();
		holder.name.setText(info.name);
		holder.account.setText(info.account);
		
		if(info.iconUrl!=null&&!info.iconUrl.trim().equals("")) {
			
			//Logger.i("Test", "----------->"+info.iconUrl);
			downloader.download(info.iconUrl, holder.image);
		}else {
			holder.image.setImageResource(R.drawable.default_icon);
		}
		return convertView;
	}

	private static final class ViewHolder {
		public ImageView image;
		public TextView name;
		public TextView account;
	}
}
