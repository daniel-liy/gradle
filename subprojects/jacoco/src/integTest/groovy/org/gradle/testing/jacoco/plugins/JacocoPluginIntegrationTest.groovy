/*
 * Copyright 2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gradle.testing.jacoco.plugins

import org.gradle.api.Project
import org.gradle.api.reporting.ReportingExtension
import org.gradle.integtests.fixtures.AbstractIntegrationSpec
import org.gradle.testing.jacoco.tasks.JacocoMerge
import spock.lang.Issue

class JacocoPluginIntegrationTest extends AbstractIntegrationSpec {

    private static final String REPORTING_BASE = "${Project.DEFAULT_BUILD_DIR_NAME}/${ReportingExtension.DEFAULT_REPORTS_DIR_NAME}"
    private static final String REPORT_HTML_DEFAULT_PATH = "${REPORTING_BASE}/jacoco/test/html/index.html"
    private static final String REPORT_XML_DEFAULT_PATH = "${REPORTING_BASE}/jacoco/test/jacocoTestReport.xml"
    private static final String REPORT_CSV_DEFAULT_REPORT = "${REPORTING_BASE}/jacoco/test/jacocoTestReport.csv"

    def setup() {
        buildFile << """
            apply plugin: "java"
            apply plugin: "jacoco"

            repositories {
                mavenCentral()
            }
            dependencies {
                testCompile 'junit:junit:4.11'
            }
        """
        createTestFiles()
    }

    void "allows configuring jacoco dependencies explicitly"() {
        buildFile << """
            dependencies {
                //downgrade version:
                jacocoAgent "org.jacoco:org.jacoco.agent:0.6.0.201210061924"
                jacocoAnt "org.jacoco:org.jacoco.ant:0.6.0.201210061924"
            }
        """

        when: succeeds("dependencies", "--configuration", "jacocoAgent")
        then: output.contains "org.jacoco:org.jacoco.agent:0.6.0.201210061924"

        when: succeeds("dependencies", "--configuration", "jacocoAnt")
        then: output.contains "org.jacoco:org.jacoco.ant:0.6.0.201210061924"
    }

    void generatesHtmlReportOnlyAsDefault() {
        when:
        succeeds('test', 'jacocoTestReport')

        then:
        file(REPORTING_BASE).listFiles().collect { it.name } as Set == ["jacoco", "tests"] as Set
        file(REPORT_HTML_DEFAULT_PATH).exists()
        file("${REPORTING_BASE}/jacoco/test").listFiles().collect { it.name } == ["html"]
    }

    void canConfigureReportsInJacocoTestReport() {
        given:
        buildFile << """
            jacocoTestReport {
                reports {
                    xml.enabled true
                    csv.enabled true
                    html.destination "\${buildDir}/jacocoHtml"
                }
            }
            """

        when:
        succeeds('test', 'jacocoTestReport')

        then:
        file("build/jacocoHtml/index.html").exists()
        file(REPORT_XML_DEFAULT_PATH).exists()
        file(REPORT_CSV_DEFAULT_REPORT).exists()
    }

    void respectsReportingBaseDir() {
        given:
        buildFile << """
            jacocoTestReport {
                reports.xml.enabled = true
                reports.csv.enabled = true
            }
            reporting{
                baseDir = "\$buildDir/customReports"
            }"""

        when:
        succeeds('test', 'jacocoTestReport')

        then:
        file("build/customReports/jacoco/test/html/index.html").exists()
        file("build/customReports/jacoco/test/jacocoTestReport.xml").exists()
        file("build/customReports/jacoco/test/jacocoTestReport.csv").exists()
    }

    void canConfigureReportDirectory() {
        given:
        def customReportDirectory = "customJacocoReportDir"
        buildFile << """
            jacocoTestReport {
                reports.xml.enabled = true
                reports.csv.enabled = true
            }
            jacoco {
                reportsDir = new File(buildDir, "$customReportDirectory")
            }
            """

        when:
        succeeds('test', 'jacocoTestReport')

        then:
        file("build/${customReportDirectory}/test/html/index.html").exists()
        file("build/${customReportDirectory}/test/jacocoTestReport.xml").exists()
        file("build/${customReportDirectory}/test/jacocoTestReport.csv").exists()
    }

    void jacocoReportIsIncremental() {
        when:
        succeeds('test', 'jacocoTestReport')

        then:
        file(REPORT_HTML_DEFAULT_PATH).exists()

        when:
        succeeds('jacocoTestReport')

        then:
        skippedTasks.contains(":jacocoTestReport")
        file(REPORT_HTML_DEFAULT_PATH).exists()

        when:
        file("${REPORTING_BASE}/jacoco/test/html/.resources").deleteDir()
        succeeds('test', 'jacocoTestReport')

        then:
        !skippedTasks.contains(":jacocoTestReport")
        file(REPORT_HTML_DEFAULT_PATH).exists()
    }

    void jacocoTestReportIsSkippedIfNoCoverageDataAvailable() {
        when:
        def executionResult = succeeds('jacocoTestReport')
        then:
        executionResult.assertTaskSkipped(':jacocoTestReport')
    }

    void canUseCoverageDataFromPreviousRunForCoverageReport() {
        when:
        succeeds('jacocoTestReport')

        then:
        skippedTasks.contains(":jacocoTestReport")
        !file(REPORT_HTML_DEFAULT_PATH).exists()

        when:
        succeeds('test')

        and:
        succeeds('jacocoTestReport')

        then:
        executedTasks.contains(":jacocoTestReport")
        file(REPORT_HTML_DEFAULT_PATH).exists()
    }

    void canMergeCoverageData() {
        given:
        buildFile << """
            task otherTests(type: Test) {
                binResultsDir file("bin")
                testSrcDirs = test.testSrcDirs
                testClassesDir = test.testClassesDir
                classpath = test.classpath
            }

            task jacocoMerge(type: ${JacocoMerge.name}) {
                executionData test, otherTests
            }
        """
        when:
        succeeds 'jacocoMerge'

        then:
        ":jacocoMerge" in nonSkippedTasks
        ":test" in nonSkippedTasks
        ":otherTests" in nonSkippedTasks
        file("build/jacoco/jacocoMerge.exec").exists()
    }

    @Issue("GRADLE-2917")
    void "configures default jacoco dependencies even if the configuration was resolved before"() {
        expect:
        //dependencies task forces resolution of the configurations
        succeeds "dependencies", "test", "jacocoTestReport"
    }

    private void createTestFiles() {
        file("src/main/java/org/gradle/Class1.java") <<
                "package org.gradle; public class Class1 { public boolean isFoo(Object arg) { return true; } }"
        file("src/test/java/org/gradle/Class1Test.java") <<
                "package org.gradle; import org.junit.Test; public class Class1Test { @Test public void someTest() { new Class1().isFoo(\"test\"); } }"
    }
}

