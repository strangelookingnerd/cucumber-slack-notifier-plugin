package org.jenkinsci.plugins.slacknotifier;

public class FeatureResult {
    private final String uri;
    private final String name;
    private final int passPercentage;

    public FeatureResult(String uri, String name, int passPercentage) {
        this.uri = uri;
        this.name = name;
        this.passPercentage = passPercentage;
    }

    public String toString() {
        return this.uri + "=" + this.passPercentage;
    }

    public String getUri() {
        return this.uri;
    }

    public String getDisplayName() {
        return this.name;
    }

    public int getPassPercentage() {
        return this.passPercentage;
    }
}