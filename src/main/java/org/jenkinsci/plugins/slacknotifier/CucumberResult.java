package org.jenkinsci.plugins.slacknotifier;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.apache.commons.lang.StringUtils;

import java.util.List;

public class CucumberResult {
    private final List<FeatureResult> featureResults;
    private final int passPercentage;
    private final int totalScenarios;

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

    public String toSlackMessage(final String jobName, final int buildNumber, final String jenkinsUrl, final String extra) {
        final JsonObject json = new JsonObject();
        json.addProperty("channel", "#");
        addCaption(json, buildNumber, jobName, jenkinsUrl, extra);

        final JsonArray attachmentsJson = new JsonArray();
        JsonObject fields = new JsonObject();
        fields.add("fields", getFields(jobName, buildNumber, jenkinsUrl));
        attachmentsJson.add(fields);

        if (getPassPercentage() == 100) {
            addAttachmentColor(attachmentsJson, "good");
            json.addProperty("icon_emoji", ":thumbsup:");
        } else if (getPassPercentage() >= 98) {
            addAttachmentColor(attachmentsJson, "warning");
            json.addProperty("icon_emoji", ":hand:");
        } else {
            addAttachmentColor(attachmentsJson, "danger");
            json.addProperty("icon_emoji", ":thumbsdown:");
        }

        json.add("attachments", attachmentsJson);
        json.addProperty("username", jobName);
        return json.toString();
    }

    private String getJenkinsHyperlink(final String jenkinsUrl, final String jobName, final int buildNumber) {
        StringBuilder s = new StringBuilder();
        s.append(jenkinsUrl);
        if (!jenkinsUrl.trim().endsWith("/")) {
            s.append("/");
        }
        s.append("job/");
        s.append(jobName);
        s.append("/");
        s.append(buildNumber);
        s.append("/");
        return s.toString();
    }

    public String toHeader(final String jobName, final int buildNumber, final String jenkinsUrl, final String extra) {
        StringBuilder s = new StringBuilder();
        if (StringUtils.isNotEmpty(extra)) {
            s.append(extra);
        }
        s.append("Features: ");
        s.append(getTotalFeatures());
        s.append(", Scenarios: ");
        s.append(getTotalScenarios());
        s.append(", Build: <");
        s.append(getJenkinsHyperlink(jenkinsUrl, jobName, buildNumber));
        s.append("cucumber-html-reports/|");
        s.append(buildNumber);
        s.append(">");
        return s.toString();
    }

    private void addCaption(final JsonObject json, final int buildNumber, final String jobName, final String jenkinsUrl, final String extra) {
        json.addProperty("text", toHeader(jobName, buildNumber, jenkinsUrl, extra));
    }

    private void addAttachmentColor(JsonArray jsonArray, String colorValue) {
        jsonArray.get(0).getAsJsonObject().addProperty("color", colorValue);
    }

    private JsonArray getFields(final String jobName, final int buildNumber, final String jenkinsUrl) {
        final String hyperLink = getJenkinsHyperlink(jenkinsUrl, jobName, buildNumber) + "cucumber-html-reports/";
        final JsonArray fields = new JsonArray();
        fields.add(shortTitle("Features"));
        fields.add(shortTitle("Pass %"));
        generateFeaturesFields(fields, hyperLink);
        fields.add(shortObject("-------------------------------"));
        fields.add(shortObject("-------"));
        fields.add(shortObject("Total Passed"));
        fields.add(shortObject(getPassPercentage() + " %"));
        return fields;
    }

    private void generateFeaturesFields(JsonArray fields, String hyperLink){
        int counter = 0;
        for (FeatureResult feature : getFeatureResults()) {
            final String featureDisplayName = feature.getDisplayName();
            final String featureFileUri = feature.getUri();

            if (counter == 0){
                fields.add(shortObject("<" + hyperLink + "report-feature_" + toValidFileName(featureFileUri) + ".html|" + featureDisplayName + ">"));
            }else{
                fields.add(shortObject("<" + hyperLink + "report-feature_" + counter + "_" + toValidFileName(featureFileUri) + ".html|" + featureDisplayName + ">"));
            }

            fields.add(shortObject(feature.getPassPercentage() + " %"));
            counter++;
        }
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

    /**
     * Converts characters of passed string and replaces to hash which can be treated as valid file name
     *
     * @param fileName sequence that should be converted
     * @return converted string
     */
    private String toValidFileName(String fileName) {
        // adds MAX_VALUE to eliminate minus character which might be returned by hashCode()
        return Long.toString((long) fileName.hashCode() + Integer.MAX_VALUE);
    }

}