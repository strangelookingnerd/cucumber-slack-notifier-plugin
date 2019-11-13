package org.jenkinsci.plugins.slacknotifier;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.stream.JsonReader;
import org.junit.Test;

import java.io.*;
import java.util.Collections;

import static org.junit.Assert.*;

public class SlackClientTest {

    @Test
    public void canGenerateFullSuccessfulSlackMessage() throws FileNotFoundException {
        JsonElement element = loadTestResultFile("successful-result.json");
        assertNotNull(element);
        CucumberResult result = new SlackClient("http://jenkins:8080/", "http://slack.com/", false).processResults(element);
        assertNotNull(result);
        assertNotNull(result.getFeatureResults());
        assertEquals(8, result.getTotalScenarios());
        assertEquals(8, result.getTotalFeatures());
        assertEquals(100, result.getPassPercentage());

        String slackMessage = result.toSlackMessage("test-job", 7, "http://jenkins:8080/", null);
        assertNotNull(slackMessage);
        assertTrue(slackMessage.contains("<http://jenkins:8080/job/test-job/7/cucumber-html-reports/report-feature_751168504.html|Validate Confluence Home Page>"));
        assertTrue(slackMessage.contains("<http://jenkins:8080/job/test-job/7/cucumber-html-reports/report-feature_1_552978313.html|Validate Gerrit Home Page>"));
    }

    @Test
    public void canGenerateMinimalSuccessfulSlackMessage() throws FileNotFoundException {
        JsonElement element = loadTestResultFile("successful-result.json");
        assertNotNull(element);
        CucumberResult result = new SlackClient("http://jenkins:8080/", "http://slack.com/", true).processResults(element);
        assertNotNull(result);
        assertNotNull(result.getFeatureResults());
        assertEquals(8, result.getTotalScenarios());
        assertEquals(0, result.getTotalFeatures());
        assertEquals(100, result.getPassPercentage());

        String slackMessage = result.toSlackMessage("test-job", 7, "http://jenkins:8080/", null);
        assertNotNull(slackMessage);
    }

    @Test
    public void canGenerateFullFailedSlackMessage() throws FileNotFoundException {
        JsonElement element = loadTestResultFile("failed-result.json");
        assertNotNull(element);
        CucumberResult result = new SlackClient("http://jenkins:8080/", "http://slack.com/", false).processResults(element);
        assertNotNull(result);
        assertNotNull(result.getFeatureResults());
        assertEquals(8, result.getTotalScenarios());
        assertEquals(8, result.getTotalFeatures());
        assertEquals(87, result.getPassPercentage());
    }

    @Test
    public void canGenerateMinimalFailedSlackMessage() throws FileNotFoundException {
        JsonElement element = loadTestResultFile("failed-result.json");
        assertNotNull(element);
        CucumberResult result = new SlackClient("http://jenkins:8080/", "http://slack.com/", true).processResults(element);
        assertNotNull(result);
        assertNotNull(result.getFeatureResults());
        assertEquals(8, result.getTotalScenarios());
        assertEquals(1, result.getTotalFeatures());
        assertEquals(87, result.getPassPercentage());
    }

    @Test
    public void canGenerateGoodMessage() {
        String slackMessage = successfulResult().toSlackMessage("test-job", 1, "http://jenkins:8080/", null);
        assertNotNull(slackMessage);
        assertTrue(slackMessage.contains("good"));
    }

    @Test
    public void canGenerateMarginalMessage() {
        String slackMessage = marginalResult().toSlackMessage("test-job", 1, "http://jenkins:8080/", null);
        assertNotNull(slackMessage);
        assertTrue(slackMessage.contains("warning"));
    }

    @Test
    public void canGenerateBadMessage() {
        String slackMessage = badResult().toSlackMessage("test-job", 1, "http://jenkins:8080/", null);
        assertNotNull(slackMessage);
        assertTrue(slackMessage.contains("danger"));
    }

    private JsonElement loadTestResultFile(String filename) throws FileNotFoundException {
        File result = new File("src/test/resources", filename);
        assertNotNull(result);
        assertTrue(result.exists());
        return getResultFileAsJsonElement(new FileInputStream(result));
    }

    private JsonElement getResultFileAsJsonElement(InputStream stream) {
        final Gson gson = new Gson();
        final JsonReader jsonReader = new JsonReader(new InputStreamReader(stream));
        return gson.fromJson(jsonReader, JsonElement.class);
    }

    private CucumberResult successfulResult() {
        return new CucumberResult(Collections.singletonList(new FeatureResult("Dummy Uri","Dummy Feature", 100)), 1, 100);
    }

    private CucumberResult badResult() {
        return new CucumberResult(Collections.singletonList(new FeatureResult("Dummy Uri","Dummy Feature", 0)), 1, 0);
    }

    private CucumberResult marginalResult() {
        return new CucumberResult(Collections.singletonList(new FeatureResult("Dummy Uri","Dummy Feature", 99)), 1, 99);
    }
}
