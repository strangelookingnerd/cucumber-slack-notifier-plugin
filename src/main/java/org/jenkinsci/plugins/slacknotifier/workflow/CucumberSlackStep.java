package org.jenkinsci.plugins.slacknotifier.workflow;

import hudson.AbortException;
import hudson.Extension;
import hudson.FilePath;
import hudson.Util;
import hudson.model.Run;
import hudson.model.TaskListener;
import jenkins.model.Jenkins;
import org.jenkinsci.plugins.slacknotifier.CucumberSlack;
import org.jenkinsci.plugins.slacknotifier.CucumberSlackService;
import org.jenkinsci.plugins.workflow.steps.AbstractStepDescriptorImpl;
import org.jenkinsci.plugins.workflow.steps.AbstractStepImpl;
import org.jenkinsci.plugins.workflow.steps.AbstractSynchronousNonBlockingStepExecution;
import org.jenkinsci.plugins.workflow.steps.StepContextParameter;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

import javax.annotation.Nonnull;
import javax.inject.Inject;

public class CucumberSlackStep extends AbstractStepImpl {

    private final @Nonnull
    String channel;
    private String json;
    private boolean hideSuccessfulResults;
    private String extra;
    private boolean failOnError;

    @DataBoundConstructor
    public CucumberSlackStep(@Nonnull String channel) {
        this.channel = channel;
    }

    @Nonnull
    public String getChannel() {
        return channel;
    }

    public String getJson() {
        return json;
    }

    @DataBoundSetter
    public void setJson(String json) {
        this.json = Util.fixEmpty(json);
    }

    public boolean getHideSuccessfulResults() {
        return hideSuccessfulResults;
    }

    @DataBoundSetter
    public void setHideSuccessfulResults(String hideSuccessfulResults) {
        this.hideSuccessfulResults = Boolean.getBoolean(Util.fixEmpty(hideSuccessfulResults));
    }

    public String getExtra() {
        return extra;
    }

    @DataBoundSetter
    public void setExtra(String extra) {
        this.extra = Util.fixEmpty(extra);
    }

    public boolean isFailOnError() {
        return failOnError;
    }

    @DataBoundSetter
    public void setFailOnError(boolean failOnError) {
        this.failOnError = failOnError;
    }

    @Extension
    public static class DescriptorImpl extends AbstractStepDescriptorImpl {

        public DescriptorImpl() {
            super(CucumberSlackSendExecution.class);
        }

        @Override
        public String getFunctionName() {
            return "cucumberSlackSend";
        }

        @Override
        public String getDisplayName() {
            return "Send cucumber notifications to slack";
        }
    }

    public static class CucumberSlackSendExecution extends AbstractSynchronousNonBlockingStepExecution<Void> {

        private static final long serialVersionUID = 1L;

        @Inject
        private transient CucumberSlackStep step;

        @StepContextParameter
        private transient TaskListener listener;

        @StepContextParameter
        private transient Run<?, ?> run;

        @StepContextParameter
        private transient FilePath workspace;

        @Override
        protected Void run() throws Exception {

            //default to global config values if not set in step, but allow step to override all global settings
            Jenkins jenkins;
            //Jenkins.getInstance() may return null, no message sent in that case
            try {
                jenkins = Jenkins.getInstance();
            } catch (NullPointerException ne) {
                listener.error("Unable to notify slack", ne);
                return null;
            }

            CucumberSlack.CucumberSlackDescriptor cucumberSlackDesc = jenkins.getDescriptorByType(CucumberSlack.CucumberSlackDescriptor.class);

            String webHookEndpoint = cucumberSlackDesc.getWebHookEndpoint();
            String json = step.json;
            boolean hideSuccessfulResults = step.hideSuccessfulResults;
            String channel = step.channel;
            String extra = step.extra;

            CucumberSlackService slackService = new CucumberSlackService(webHookEndpoint);

            try {
                slackService.sendCucumberReportToSlack(run, workspace, json, channel, extra, hideSuccessfulResults);
            } catch (Exception exp) {
                if (step.failOnError) {
                    throw new AbortException("Unable to send slack notification: " + exp);
                }
            }

            return null;
        }

    }

}
