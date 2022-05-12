// SPDX-License-Identifier: MIT
package com.mercedesbenz.sechub.integrationtest.internal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class IntegrationTestExampleConstants {

    public static final String EXAMPLE_CONTENT_ROOT_PATH = "sechub-integrationtest/build/sechub/example/content/";

    /**
     * The default infrascan white list URI. The usage can be found
     * "/sechub-integrationtest/src/test/resources/sechub-integrationtest-client-infrascan.json"
     */
    public static final String INFRASCAN_DEFAULT_WHITELEIST_ENTRY = "https://fscan.intranet.example.org";

    public static final String PATH_TO_ZIPFILE_WITH_PDS_CODESCAN_CRITICAL_FINDINGS = "pds/codescan/upload/zipfile_contains_inttest_codescan_with_critical.zip";

    public static final String PATH_TO_ZIPFILE_WITH_PDS_CODESCAN_LOW_FINDINGS = "pds/codescan/upload/zipfile_contains_inttest_codescan_with_low.zip";

    public static TestDataFolderList TESTDATA_FOLDERS = new TestDataFolderList();

    public static class IntegrationTestExampleFolder {
        private boolean isExistingContent;
        private String path;

        public IntegrationTestExampleFolder(String path, boolean isExistingContent) {
            this.path = path;
            this.isExistingContent = isExistingContent;
        }

        public String getPath() {
            return path;
        }

        public boolean isExistingContent() {
            return isExistingContent;
        }

        @Override
        public String toString() {
            return "IntegrationTestExampleFolder [isExistingContent=" + isExistingContent + ", " + (path != null ? "path=" + path : "") + "]";
        }

    }

    public static class TestDataFolderList {

        private List<IntegrationTestExampleFolder> exampleContentFolders = new ArrayList<>();

        public List<IntegrationTestExampleFolder> getExampleContentFolders() {
            return Collections.unmodifiableList(exampleContentFolders);
        }

        private TestDataFolderList() {
            for (MockData mockData : MockData.values()) {
                if (mockData.isTargetUsedAsFolder()) {
                    String target = mockData.getTarget();
                    exampleContentFolders.add(new IntegrationTestExampleFolder(target, mockData.isTargetNeedingExistingData()));
                }
            }
        }
    }
}
