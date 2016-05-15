package com.heima.vdun.entity;

import java.io.Serializable;

public class AccountBean implements Serializable {

	private static final long serialVersionUID = 1L;
	//个人账户列表中的信息
	public String name;
	public String id;
	public String iconUrl;
	public String account;
	public String entry;
	public String createTime;

	public AccountBean() {
		
	}
	public AccountBean(String name,String iconUrl,String account,String entry) {
		this.name = name;
		this.iconUrl = iconUrl;
		this.account = account;
		this.entry = entry;
	}
}
