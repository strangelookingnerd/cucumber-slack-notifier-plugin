package org.jenkinsci.plugins.slacknotifier;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;

import org.junit.Test;

public class CucumberResultTest {
	
	@Test
	public void canGenerateHeader() {
		String header = successfulResult().toHeader("test-job", 1, "http://localhost:8080/", null);
		assertNotNull(header);
		assertTrue(header.contains("Features: 1"));
		assertTrue(header.contains("Scenarios: 1"));
		assertTrue(header.contains("Build: <http://localhost:8080/job/test-job/1/cucumber-html-reports/|1>"));
	}
	
	@Test
	public void canGenerateHeaderWithExtraInformation() {
		String header = successfulResult().toHeader("test-job", 1, "http://localhost:8080/", "Extra Content");
		assertNotNull(header);
		assertTrue(header.contains("Extra Content"));
		assertTrue(header.contains("Features: 1"));
		assertTrue(header.contains("Scenarios: 1"));
		assertTrue(header.contains("Build: <http://localhost:8080/job/test-job/1/cucumber-html-reports/|1>"));
	}
	
	private CucumberResult successfulResult() {
		return new CucumberResult(Arrays.asList(new FeatureResult("Dummy Test", 100)),1,100);
	}
}
