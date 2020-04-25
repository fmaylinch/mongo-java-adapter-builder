package com.codethen.mongo.adapter.generator.sample;

import java.util.List;

public class Person {

	public enum Gender {
		MALE, FEMALE
	}

	private String id;
	private List<String> friendIds;
	private String name;
	private List<String> nicknames;
	private int age;
	private String number;
	private boolean famous;
	private Gender gender;
	private List<Gender> preferredGenders;
	private AddressExt address;
	private List<Address> otherAddresses;


	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public List<String> getFriendIds() {
		return friendIds;
	}

	public void setFriendIds(List<String> friendIds) {
		this.friendIds = friendIds;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public List<String> getNicknames() {
		return nicknames;
	}

	public void setNicknames(List<String> nicknames) {
		this.nicknames = nicknames;
	}

	public int getAge() {
		return age;
	}

	public void setAge(int age) {
		this.age = age;
	}

	public String getNumber() {
		return number;
	}

	public void setNumber(String number) {
		this.number = number;
	}

	public boolean isFamous() {
		return famous;
	}

	public void setFamous(boolean famous) {
		this.famous = famous;
	}

	public Gender getGender() {
		return gender;
	}

	public void setGender(Gender gender) {
		this.gender = gender;
	}

	public List<Gender> getPreferredGenders() {
		return preferredGenders;
	}

	public void setPreferredGenders(List<Gender> preferredGenders) {
		this.preferredGenders = preferredGenders;
	}

	public AddressExt getAddress() {
		return address;
	}

	public void setAddress(AddressExt address) {
		this.address = address;
	}

	public List<Address> getOtherAddresses() {
		return otherAddresses;
	}

	public void setOtherAddresses(List<Address> otherAddresses) {
		this.otherAddresses = otherAddresses;
	}
}
