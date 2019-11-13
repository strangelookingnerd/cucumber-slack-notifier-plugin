package org.jenkinsci.plugins.slacknotifier;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.StringRequestEntity;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

public class SlackClient {

    private static final Logger LOG = Logger.getLogger(SlackClient.class.getName());

    private static final String ENCODING = "UTF-8";
    private static final String CONTENT_TYPE = "application/json";

    private final String jenkinsUrl;
    private final String channelWebhookUrl;
    private final boolean hideSuccessfulResults;

    public SlackClient(String jenkinsUrl, String channelWebhookUrl, boolean hideSuccessfulResults) {
        this.jenkinsUrl = jenkinsUrl;
        this.channelWebhookUrl = channelWebhookUrl;
        this.hideSuccessfulResults = hideSuccessfulResults;
    }

    public void postToSlack(JsonElement results, final String jobName, final int buildNumber, final String extra) {
        LOG.info("Publishing test report to slack channelWebhookUrl: " + channelWebhookUrl);
        CucumberResult result = results == null ? dummyResults() : processResults(results);
        String json = result.toSlackMessage(jobName, buildNumber, jenkinsUrl, extra);
        postToSlack(json);
    }

    private CucumberResult dummyResults() {
        return new CucumberResult(Collections.singletonList(new FeatureResult("Dummy Test","Dummy Test", 100)), 1, 100);
    }


    private void postToSlack(String json) {
        LOG.fine("Json being posted: " + json);
        StringRequestEntity requestEntity = getStringRequestEntity(json);
        PostMethod postMethod = new PostMethod(channelWebhookUrl);
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

    public CucumberResult processResults(JsonElement resultElement) {
        int totalScenarios = 0;
        int passPercent = 0;
        List<FeatureResult> results = new ArrayList<>();
        JsonArray features = resultElement.getAsJsonArray();
        int failedScenarios = 0;
        for (JsonElement featureElement : features) {
            JsonObject feature = featureElement.getAsJsonObject();
            JsonArray elements = feature.get("elements").getAsJsonArray();
            int scenariosTotal = 0;
            int failed = 0;
            for (JsonElement scenarioElement : elements) {
                JsonObject scenario = scenarioElement.getAsJsonObject();
                JsonArray steps = scenario.get("steps").getAsJsonArray();
                if (scenario.get("type").getAsString().equalsIgnoreCase("scenario")){
                    scenariosTotal = scenariosTotal + 1;
                }
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
            final int scenarioPassPercent = Math.round(((scenariosTotal - failed) * 100) / scenariosTotal);
            if (scenarioPassPercent != 100 || !hideSuccessfulResults) {
                results.add(new FeatureResult(feature.get("uri").getAsString(), feature.get("name").getAsString(), scenarioPassPercent));
            }
        }
        passPercent = Math.round(((totalScenarios - failedScenarios) * 100) / totalScenarios);
        return new CucumberResult(results, totalScenarios, passPercent);
    }

    private StringRequestEntity getStringRequestEntity(String json) {
        try {
            return new StringRequestEntity(json, CONTENT_TYPE, ENCODING);
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(ENCODING + " encoding is not supported with [" + json + "]", e);
        }
    }
}
