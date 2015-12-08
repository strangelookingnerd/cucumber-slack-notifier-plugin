package org.jenkinsci.plugins.slacknotifier;

import hudson.Extension;
import hudson.Launcher;
import hudson.model.BuildListener;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.Job;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import hudson.util.FormValidation;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.logging.Logger;

import javax.servlet.ServletException;

import jenkins.model.Jenkins;
import net.sf.json.JSONObject;

import org.apache.commons.io.FileUtils;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.stream.JsonReader;

public class CucumberSlackBuildStepNotifier extends Builder {

	private static final Logger LOG = Logger.getLogger(CucumberSlackBuildStepNotifier.class.getName());

	private final String channel;
	private final String json;

	@DataBoundConstructor
	public CucumberSlackBuildStepNotifier(String channel, String json) {
		this.channel = channel;
		this.json = json;
	}

	public String getChannel() {
		return channel;
	}

	public String getJson() {
		return json;
	}

	@Override
	public boolean perform(AbstractBuild build, Launcher launcher, BuildListener listener) {
		String webhookUrl = ((CucumberSlackBuildStepNotifier.DescriptorImpl) Jenkins.getInstance().getDescriptor(
				CucumberSlackBuildStepNotifier.class)).getWebHookEndpoint();
		String jenkinsUrl = ((CucumberSlackBuildStepNotifier.DescriptorImpl) Jenkins.getInstance().getDescriptor(
				CucumberSlackBuildStepNotifier.class)).getJenkinsServerUrl();

		LOG.info("Posting cucumber reports to slack for '" + build.getParent().getDisplayName() + "'");
		LOG.info("Cucumber reports are in '" + build.getParent().getRootDir() + "'");

		JsonElement jsonElement = getResultFileAsJsonElement(build.getParent());
		SlackClient client = new SlackClient(webhookUrl, jenkinsUrl, channel);
		client.postToSlack(jsonElement, build.getParent().getDisplayName(), build.getNumber());

		listener.getLogger().printf("message posted to slack");
		return true;
	}

	public String escape(String string) {
		string = string.replace("&", "&amp;");
		string = string.replace("<", "&lt;");
		string = string.replace(">", "&gt;");
		return string;
	}

	private JsonElement getResultFileAsJsonElement(Job job) {
		final File targetDirectory = new File(job.getRootDir(), "target");
		final File file = new File(targetDirectory, json);
		LOG.info("Publishing results file: " + file.getName());
		final String filePath = file.getAbsolutePath();
		LOG.info("file path: " + filePath);
		final Gson gson = new Gson();
		try {
			final JsonReader jsonReader = new JsonReader(new FileReader(file));
			return gson.fromJson(jsonReader, JsonElement.class);
		} catch (FileNotFoundException e) {
			throw new RuntimeException("Exception occurred while reading test results", e);
		}
	}

	@Override
	public DescriptorImpl getDescriptor() {
		return (DescriptorImpl) super.getDescriptor();
	}

	@Extension
	public static final class DescriptorImpl extends BuildStepDescriptor<Builder> {

		private String webHookEndpoint;
		private String jenkinsServerUrl;

		public DescriptorImpl() {
			load();
		}

		public FormValidation doCheckName(@QueryParameter String value) throws IOException, ServletException {
			if (value.length() == 0)
				return FormValidation.error("Please set a name");
			if (value.length() < 4)
				return FormValidation.warning("Isn't the name too short?");
			return FormValidation.ok();
		}

		public boolean isApplicable(Class<? extends AbstractProject> aClass) {
			return true;
		}

		public String getDisplayName() {
			return "Cucumber Report Slack Build Step Notifier";
		}

		@Override
		public boolean configure(StaplerRequest req, JSONObject formData) throws FormException {
			webHookEndpoint = formData.getString("webHookEndpoint");
			jenkinsServerUrl = formData.getString("jenkinsServerUrl");
			save();
			return super.configure(req, formData);
		}

		public String getWebHookEndpoint() {
			return webHookEndpoint;
		}

		public String getJenkinsServerUrl() {
			return jenkinsServerUrl;
		}
	}
}
