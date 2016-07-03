package org.jenkinsci.plugins.slacknotifier;

import java.util.List;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

public class CucumberResult {
	final List<FeatureResult> featureResults;
	final int passPercentage;
	final int totalScenarios;
	
	public CucumberResult(List<FeatureResult> featureResults, int totalScenarios, int passPercentage) {
		this.featureResults = featureResults;
		this.totalScenarios = totalScenarios;
		this.passPercentage = passPercentage;
	}
	
	public int getPassPercentage() {
		return this.passPercentage;
	}
	
	public int getTotalFeatures() {
		return this.featureResults.size();
	}
	
	public int getTotalScenarios() {
		return this.totalScenarios;
	}
	
	public List<FeatureResult> getFeatureResults() {
		return this.featureResults;
	}
	
	public String toSlackMessage(final String jobName,
			final int buildNumber, final String channel, final String jenkinsUrl) {
		final JsonObject json = new JsonObject();
		json.addProperty("channel", "#" + channel);
		addCaption(json, buildNumber, jobName, jenkinsUrl);
		json.add("fields", getFields(jobName, buildNumber, jenkinsUrl));

		if (getPassPercentage() == 100) {
			addColourAndIcon(json, "good", ":thumbsup:");
		} else if (getPassPercentage() >= 98) {
			addColourAndIcon(json, "warning", ":hand:");
		} else {
			addColourAndIcon(json, "danger", ":thumbsdown:");
		}

		json.addProperty("username", jobName);
		return json.toString();
	}

	private void addCaption(final JsonObject json, final int buildNumber, final String jobName, final String jenkinsUrl) {
		final String hyperLink = jenkinsUrl + "/job/" + jobName + "/" + buildNumber + "/cucumber-html-reports/";
		final String caption = "A total of " + getTotalFeatures() + " features consisting of " + getTotalScenarios()
				+ " scenarios were executed for build: <" + hyperLink + "|" + buildNumber + ">";
		json.addProperty("pretext", caption);
	}
	
	private void addColourAndIcon(JsonObject json, String good, String value) {
		json.addProperty("color", good);
		json.addProperty("icon_emoji", value);
	}

	private JsonArray getFields(final String jobName, final int buildNumber, final String jenkinsUrl) {
		final String hyperLink = jenkinsUrl + "/job/" + jobName + "/" + buildNumber + "/cucumber-html-reports/";
		final JsonArray fields = new JsonArray();
		fields.add(shortTitle("Features"));
		fields.add(shortTitle("Pass %"));
		for (FeatureResult feature : getFeatureResults()) {
			final String featureFileName = feature.getName();
			String featureName = featureFileName.replace("-feature", "");
			featureName = featureName.replaceAll("_", " ");
			fields.add(shortObject("<" + hyperLink + featureFileName + ".html|" + featureName + ">"));
			fields.add(shortObject(feature.getPassPercentage() + " %"));
		}
		fields.add(shortObject("-------------------------------"));
		fields.add(shortObject("-------"));
		fields.add(shortObject("Total Passed"));
		fields.add(shortObject(getPassPercentage() + " %"));
		return fields;
	}

	
	private JsonObject shortObject(final String value) {
		JsonObject obj = new JsonObject();
		obj.addProperty("value", value);
		obj.addProperty("short", true);
		return obj;
	}

	private JsonObject shortTitle(final String title) {
		JsonObject obj = new JsonObject();
		obj.addProperty("title", title);
		obj.addProperty("short", true);
		return obj;
	}
}