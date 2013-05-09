package org.deuce.utest.bank;

import org.deuce.Atomic;

public class AccountImpl implements Account {

	private int amount = 0;
	final private String accountNumber;

	public AccountImpl( String accountNumber){
		this.accountNumber = accountNumber;
	}
	
	public void deposit(int amount) {
		this.amount += amount;
	}

	public String getAccountNumber() {
		return accountNumber;
	}

	@Atomic public int getBalance() {
		return amount;
	}

	public void withdraw(int amount) {
		this.amount -= amount;
	}

}
