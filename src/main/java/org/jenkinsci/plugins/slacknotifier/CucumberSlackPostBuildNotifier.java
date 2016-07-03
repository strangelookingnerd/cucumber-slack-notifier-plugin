package org.jenkinsci.plugins.slacknotifier;

import hudson.Extension;
import hudson.Launcher;
import hudson.model.BuildListener;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Publisher;
import hudson.tasks.Recorder;
import hudson.util.FormValidation;

import java.io.IOException;
import java.util.logging.Logger;

import javax.servlet.ServletException;

import net.sf.json.JSONObject;

import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

public class CucumberSlackPostBuildNotifier extends Recorder {

	private static final Logger LOG = Logger.getLogger(CucumberSlackPostBuildNotifier.class.getName());

	private final String channel;
	private final String json;

	@DataBoundConstructor
	public CucumberSlackPostBuildNotifier(String channel, String json) {
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
	public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener)
			throws InterruptedException, IOException {
		String webhookUrl = CucumberSlack.get().getWebHookEndpoint();
		String jenkinsUrl = CucumberSlack.get().getJenkinsServerUrl();

		if (StringUtils.isEmpty(webhookUrl)) {
			LOG.fine("Skipping cucumber slack notifier...");
			return true;
		}

		CucumberSlackService service = new CucumberSlackService(webhookUrl, jenkinsUrl);
		service.sendCucumberReportToSlack(build, json, channel);

		return true;
	}

	@Override
	public DescriptorImpl getDescriptor() {
		return (DescriptorImpl) super.getDescriptor();
	}

	@Extension
	public static final class DescriptorImpl extends BuildStepDescriptor<Publisher> {

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
			return "Send Cucumber Report to Slack";
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

	public BuildStepMonitor getRequiredMonitorService() {
		return BuildStepMonitor.STEP;
	}
}
