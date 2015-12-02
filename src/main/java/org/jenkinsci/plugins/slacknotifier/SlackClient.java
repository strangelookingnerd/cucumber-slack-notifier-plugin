package org.jenkinsci.plugins.slacknotifier;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Logger;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.StringRequestEntity;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class SlackClient {

	private static final Logger LOG = Logger.getLogger(SlackClient.class.getName());

	private static final String ENCODING = "UTF-8";
	private static final String CONTENT_TYPE = "application/json";

	private final String webhookUrl;
	private final String jenkinsUrl;
	private final String channel;

	private int totalScenarios = 0;
	private int totalFeatures = 0;
	private int passPercent = 0;

	public SlackClient(String webhookUrl, String jenkinsUrl, String channel) {
		this.webhookUrl = webhookUrl;
		this.jenkinsUrl = jenkinsUrl;
		this.channel = channel;
	}

	public void postToSlack(JsonElement results, final String jobName, final int buildNumber) {
		LOG.info("Publishing test report to slack channel: " + channel);
		Map<String, String> features = results == null ? dummyResults() : processResults(results);
		String json = createJSONSlackMessage(features, jobName, buildNumber);
		postToSlack(json);
	}

	private Map<String, String> dummyResults() {
		Map<String, String> results = new TreeMap<String, String>();
		results.put("Dummy Test", "100");
		totalScenarios = 1;
		totalFeatures = 1;
		passPercent = 100;
		return results;
	}

	private Map<String, String> processResults(JsonElement resultElement) {
		Map<String, String> results = new TreeMap<String, String>();
		JsonArray features = resultElement.getAsJsonArray();
		totalFeatures = features.size();
		int failedScenarios = 0;
		for (JsonElement featureElement : features) {
			JsonObject feature = featureElement.getAsJsonObject();
			JsonArray elements = feature.get("elements").getAsJsonArray();
			int scenariosTotal = elements.size();
			int failed = 0;
			for (JsonElement scenarioElement : elements) {
				JsonObject scenario = scenarioElement.getAsJsonObject();
				JsonArray steps = scenario.get("steps").getAsJsonArray();
				for (JsonElement stepElement : steps) {
					JsonObject step = stepElement.getAsJsonObject();
					String result = step.get("result").getAsJsonObject().get("status").getAsString();
					if (!result.equals("passed")) {
						failed = failed + 1;
						failedScenarios = failedScenarios + 1;
						break;
					}
				}
			}
			totalScenarios = totalScenarios + scenariosTotal;
			final int passPercent = Math.round(((scenariosTotal - failed) * 100) / scenariosTotal);
			results.put(feature.get("uri").getAsString(), Integer.toString(passPercent));
		}
		passPercent = Math.round(((totalScenarios - failedScenarios) * 100) / totalScenarios);
		return results;
	}

	private void postToSlack(String json) {
		LOG.fine("Json being posted: " + json);
		StringRequestEntity requestEntity = getStringRequestEntity(json);
		PostMethod postMethod = new PostMethod(webhookUrl);
		postMethod.setRequestEntity(requestEntity);
		postToSlack(postMethod);
	}

	private void postToSlack(PostMethod postMethod) {
		HttpClient http = new HttpClient();
		try {
			int status = http.executeMethod(postMethod);
			if (status != 200) {
				throw new RuntimeException("Received HTTP Status code [" + status + "] while posting to slack");
			}
		} catch (IOException e) {
			throw new RuntimeException("Message could not be posted", e);
		}
	}

	private String createJSONSlackMessage(final Map<String, String> features, final String jobName,
			final int buildNumber) {
		final JsonObject json = new JsonObject();
		json.addProperty("channel", "#" + channel);
		addCaption(json, buildNumber, jobName);
		json.add("fields", getFields(jobName, buildNumber, features));

		if (passPercent == 100) {
			addColourAndIcon(json, "good", ":thumbsup:");
		} else if (passPercent >= 98) {
			addColourAndIcon(json, "warning", ":hand:");
		} else {
			addColourAndIcon(json, "danger", ":thumbsdown:");
		}

		json.addProperty("username", jobName);
		LOG.info(json.toString());
		return json.toString();
	}

	private void addCaption(final JsonObject json, final int buildNumber, final String jobName) {
		final String hyperLink = jenkinsUrl + "/job/" + jobName + "/" + buildNumber + "/cucumber-html-reports/";
		final String caption = "A total of " + totalFeatures + " features consisting of " + totalScenarios
				+ " scenarios were executed for build: <" + hyperLink + "|" + buildNumber + ">";
		LOG.info(caption);
		json.addProperty("pretext", caption);
	}

	private void addColourAndIcon(JsonObject json, String good, String value) {
		json.addProperty("color", good);
		json.addProperty("icon_emoji", value);
	}

	private JsonArray getFields(final String jobName, final int buildNumber, final Map<String, String> features) {
		final String hyperLink = jenkinsUrl + "/job/" + jobName + "/" + buildNumber + "/cucumber-html-reports/";
		final JsonArray fields = new JsonArray();
		fields.add(shortTitle("Features"));
		fields.add(shortTitle("Pass %"));
		for (Map.Entry<String, String> feature : features.entrySet()) {
			final String featureFileName = feature.getKey();
			String featureName = featureFileName.replace(".feature", "");
			featureName = featureName.replaceAll("_", " ");
			fields.add(shortObject("<" + hyperLink + featureFileName + ".html|" + featureName + ">"));
			fields.add(shortObject(feature.getValue() + " %"));
		}
		fields.add(shortObject("-------------------------------"));
		fields.add(shortObject("-------"));
		fields.add(shortObject("Total Passed"));
		fields.add(shortObject(passPercent + " %"));
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

	private StringRequestEntity getStringRequestEntity(String json) {
		try {
			return new StringRequestEntity(json, CONTENT_TYPE, ENCODING);
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(ENCODING + " encoding is not supported with [" + json + "]");
		}
	}
}
