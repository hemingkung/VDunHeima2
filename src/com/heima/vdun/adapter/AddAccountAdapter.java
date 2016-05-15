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
import com.heima.vdun.entity.AccountInfo;
import com.heima.vdun.util.ImageDownloader;

public class AddAccountAdapter extends BaseAdapter {

	private List<AccountInfo> list;
	private LayoutInflater inflater;
	private ImageDownloader downloader;
	
	public AddAccountAdapter(List<AccountInfo> list, Context context) {
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
			convertView = inflater.inflate(R.layout.add_account_item, null);
			holder = new ViewHolder();
			holder.image = (ImageView) convertView
					.findViewById(R.id.iv_add_account_icon);
			holder.name = (TextView) convertView
			.findViewById(R.id.tv_add_account_name);
			
			convertView.setTag(holder);
		}

		AccountInfo info = list.get(position);
		holder = (ViewHolder) convertView.getTag();
		holder.name.setText(info.name);
		
		if(info.iconUrl!=null&&!info.iconUrl.trim().equals("")) {
			downloader.download(info.iconUrl, holder.image);
		}else {
			holder.image.setImageResource(R.drawable.default_icon);
		}
		return convertView;
	}

	private static final class ViewHolder {
		public ImageView image;
		public TextView name;
	}
}
