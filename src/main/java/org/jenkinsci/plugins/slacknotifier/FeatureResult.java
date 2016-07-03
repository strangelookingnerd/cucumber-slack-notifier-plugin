package org.jenkinsci.plugins.slacknotifier;

public class FeatureResult {
	final String name;
	final int passPercentage;

	public FeatureResult(String name, int passPercentage) {
		this.name = name;
		this.passPercentage = passPercentage;
	}

	public String toString() {
		return this.name + "=" + this.passPercentage;
	}
	
	public String getName() {
		return this.name;
	}
	
	public int getPassPercentage() {
		return this.passPercentage;
	}
}