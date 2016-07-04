package org.jenkinsci.plugins.slacknotifier;

import hudson.Extension;
import hudson.Launcher;
import hudson.model.BuildListener;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import hudson.util.FormValidation;

import java.io.IOException;
import java.util.logging.Logger;

import javax.servlet.ServletException;

import net.sf.json.JSONObject;

import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

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
		String webhookUrl = CucumberSlack.get().getWebHookEndpoint();
		
		if (StringUtils.isEmpty(webhookUrl)) {
			LOG.fine("Skipping cucumber slack notifier...");
			return true;
		}

		CucumberSlackService service = new CucumberSlackService(webhookUrl);
		service.sendCucumberReportToSlack(build, build.getWorkspace(), json, channel, null);

		return true;
	}

	@Override
	public DescriptorImpl getDescriptor() {
		return (DescriptorImpl) super.getDescriptor();
	}

	@Extension
	public static final class DescriptorImpl extends BuildStepDescriptor<Builder> {

		private String webHookEndpoint;
	
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
			save();
			return super.configure(req, formData);
		}

		public String getWebHookEndpoint() {
			return webHookEndpoint;
		}
	}
}
