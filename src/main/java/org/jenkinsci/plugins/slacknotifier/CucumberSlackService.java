package org.jenkinsci.plugins.slacknotifier;

import hudson.FilePath;
import hudson.model.AbstractBuild;
import jenkins.model.JenkinsLocationConfiguration;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.logging.Logger;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.stream.JsonReader;

public class CucumberSlackService {

	private static final Logger LOG = Logger.getLogger(CucumberSlackService.class.getName());

	private final String webhookUrl;
	private final String jenkinsUrl;

	public CucumberSlackService(String webhookUrl) {
		this.webhookUrl = webhookUrl;
		this.jenkinsUrl = JenkinsLocationConfiguration.get().getUrl();
	}

	public void sendCucumberReportToSlack(AbstractBuild build, String json, String channel) {
		LOG.info("Posting cucumber reports to slack for '" + build.getParent().getDisplayName() + "'");
		LOG.info("Cucumber reports are in '" + build.getWorkspace() + "'");

		FilePath workspace = build.getWorkspace();

		JsonElement jsonElement = getResultFileAsJsonElement(workspace, json);
		SlackClient client = new SlackClient(webhookUrl, jenkinsUrl, channel);
		client.postToSlack(jsonElement, build.getParent().getDisplayName(), build.getNumber());
	}

	private JsonElement getResultFileAsJsonElement(FilePath workspace, String json) {
		final FilePath jsonPath = new FilePath(workspace, json);
		LOG.info("file path: " + jsonPath);
		final Gson gson = new Gson();
		try {
			final JsonReader jsonReader = new JsonReader(new InputStreamReader(jsonPath.read()));
			return gson.fromJson(jsonReader, JsonElement.class);
		} catch (IOException e) {
			throw new RuntimeException("Exception occurred while reading test results", e);
		} catch (InterruptedException e) {
			throw new RuntimeException("Exception occurred while reading test results", e);
		}
	}

}
