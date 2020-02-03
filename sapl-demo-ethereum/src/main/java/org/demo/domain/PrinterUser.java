package org.demo.domain;

import java.util.Collection;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PrinterUser extends User {

	private static final long serialVersionUID = -6541109260884369275L;

	private String ethereumAddress;

	private String transactionHash;

	public PrinterUser(String userName, String password, String ethereumAddress,
			Collection<? extends GrantedAuthority> authorities) {
		super(userName, password, authorities);
		this.ethereumAddress = ethereumAddress;
	}

	@Override
	public boolean equals(Object user) {
		PrinterUser printerUser = (PrinterUser) user;
		if (this.ethereumAddress.equals(printerUser.ethereumAddress)
				&& this.transactionHash.equals(printerUser.transactionHash))
			return super.equals(user);
		return false;

	}

}
