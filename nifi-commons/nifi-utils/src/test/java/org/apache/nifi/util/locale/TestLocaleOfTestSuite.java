package org.apache.nifi.util.locale;

import org.junit.Assume;
import org.junit.Test;

import java.util.Arrays;
import java.util.logging.Logger;

/**
 * Testing of the test suite environment {@link java.util.Locale}.
 */
public class TestLocaleOfTestSuite {

    /**
     * Utility test that logs the {@link java.util.Locale} in which the project test suite is running.
     */
    @Test
    public void testLocaleOfTestSuiteExecution() {
        Logger logger = Logger.getLogger(getClass().getName());
        final String userLanguage = System.getProperty("user.language");
        final String userCountry = System.getProperty("user.country");
        final String userRegion = System.getProperty("user.region");
        logger.warning(String.format("Test environment: user.language=[%s], user.country=[%s], user.region=[%s]",
                userLanguage, userCountry, userRegion));
        Assume.assumeTrue(Arrays.asList("en", "fr", "ja").contains(userLanguage));
        Assume.assumeTrue(Arrays.asList("AU", "FR", "JP").contains(userCountry));
    }
}
