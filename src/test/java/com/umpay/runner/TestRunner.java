package com.umpay.runner;

import com.umpay.utility.BaseClass;
import com.umpay.utility.BrowserFactory;
import io.cucumber.testng.AbstractTestNGCucumberTests;
import io.cucumber.testng.CucumberOptions;

import org.testng.annotations.DataProvider;
import org.testng.annotations.AfterSuite;

@CucumberOptions(
    features = "src/test/resources",
    glue = {"com.umpay.stepdefs", "com.umpay.utility"},
    // @manual scenarios are the ones not yet confirmed to run without a person
    // watching, so they are left out of the unattended run. Override with
    // -Dcucumber.filter.tags="@manual"
    //
    // @e2e is excluded because EndToEndTest.feature walks the same flows the per-feature
    // files already cover. Running both would register two accounts and move money twice
    // for one run. Run the journey on its own with -Dcucumber.filter.tags="@e2e"
    tags = "not @manual and not @e2e",
    plugin = {"pretty", "html:target/cucumber-reports.html", "json:target/cucumber.json", "junit:target/cucumber.xml", "testng:target/testng-cucumber.xml"},
    publish = true
)
public class TestRunner extends AbstractTestNGCucumberTests {
    @Override
    @DataProvider(parallel = false)
    public Object[][] scenarios() {
        return super.scenarios();
    }

    @AfterSuite(alwaysRun = true)
    public void tearDownSuite() {
        BrowserFactory.shutdown();
        BaseClass.sendEmailStatic();
    }
}
