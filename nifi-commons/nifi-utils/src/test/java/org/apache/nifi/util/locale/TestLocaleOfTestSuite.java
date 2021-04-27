/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.nifi.util.locale;

import org.junit.Assume;
import org.junit.Test;

import java.util.Arrays;
import java.util.Locale;
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
        String languageTag = Locale.getDefault().toLanguageTag();
        logger.warning(String.format(
                "Test environment: locale=[%s] user.language=[%s], user.country=[%s], user.region=[%s]",
                languageTag, userLanguage, userCountry, userRegion));
        Assume.assumeTrue(Arrays.asList("en", "fr", "ja").contains(userLanguage));
        Assume.assumeTrue(Arrays.asList("US", "AU", "FR", "JP").contains(userCountry));
        Assume.assumeTrue(Arrays.asList("en-US", "fr-FR").contains(languageTag));
    }
}
