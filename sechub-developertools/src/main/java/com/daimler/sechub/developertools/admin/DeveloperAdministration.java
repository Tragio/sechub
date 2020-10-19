// SPDX-License-Identifier: MIT
package com.daimler.sechub.developertools.admin;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.client.DefaultResponseErrorHandler;
import org.springframework.web.client.ResponseErrorHandler;

import com.daimler.sechub.commons.model.JSONConverter;
import com.daimler.sechub.developertools.admin.ui.ConfigurationSetup;
import com.daimler.sechub.developertools.admin.ui.UIContext;
import com.daimler.sechub.integrationtest.api.AsPDSUser;
import com.daimler.sechub.integrationtest.api.AsUser;
import com.daimler.sechub.integrationtest.api.FixedTestUser;
import com.daimler.sechub.integrationtest.api.InternalAccess;
import com.daimler.sechub.integrationtest.api.TestAPI;
import com.daimler.sechub.integrationtest.api.TestUser;
import com.daimler.sechub.integrationtest.api.UserContext;
import com.daimler.sechub.integrationtest.api.WithSecHubClient;
import com.daimler.sechub.integrationtest.internal.SimpleTestStringList;
import com.daimler.sechub.integrationtest.internal.TestJSONHelper;
import com.daimler.sechub.integrationtest.internal.TestRestHelper;
import com.daimler.sechub.integrationtest.internal.TestRestHelper.RestHelperTarget;
import com.daimler.sechub.test.TestURLBuilder;
import com.daimler.sechub.test.executionprofile.TestExecutionProfile;
import com.daimler.sechub.test.executionprofile.TestExecutionProfileList;
import com.daimler.sechub.test.executorconfig.TestExecutorConfig;
import com.daimler.sechub.test.executorconfig.TestExecutorConfigList;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;

public class DeveloperAdministration {

    private ConfigProvider provider;
    private AdminUserContext userContext;
    private TestRestHelper restHelper;
    private TestURLBuilder urlBuilder;
    private ErrorHandler errorHandler;
    private UIContext uiContext;

    public DeveloperAdministration(ConfigProvider provider, ErrorHandler errorHandler,UIContext uiContext) {
        this.provider = provider;
        this.errorHandler = errorHandler;
        this.userContext = new AdminUserContext();
        this.uiContext=uiContext;
        this.restHelper = createTestRestHelperWithErrorHandling(errorHandler, userContext);
    }

    public UIContext getUiContext() {
        return uiContext;
    }
    
    private TestRestHelper createTestRestHelperWithErrorHandling(ErrorHandler provider, UserContext user) {
        return createTestRestHelperWithErrorHandling(provider, user, RestHelperTarget.SECHUB_SERVER);
    }

    private TestRestHelper createTestRestHelperWithErrorHandling(ErrorHandler provider, UserContext user, RestHelperTarget restHelperTarget) {
        return new TestRestHelper(user, restHelperTarget) {

            @Override
            protected ResponseErrorHandler createErrorHandler() {
                return new DefaultResponseErrorHandler() {

                    @Override
                    public void handleError(ClientHttpResponse response) throws IOException {
                        StringBuilder httpResponseProblem = new StringBuilder();
                        String statusText = response.getStatusText();
                        httpResponseProblem.append("status code::");
                        httpResponseProblem.append(response.getStatusCode());
                        if (statusText != null) {
                            httpResponseProblem.append(", text:");
                            httpResponseProblem.append(statusText);
                        }
                        try (BufferedReader br = new BufferedReader(new InputStreamReader(response.getBody(), "UTF-8"))) {
                            String line = null;
                            httpResponseProblem.append(",body:");
                            while ((line = br.readLine()) != null) {
                                httpResponseProblem.append("\n");
                                httpResponseProblem.append(line);
                            }
                        } catch (IOException e) {
                            provider.handleError("failed to read response body:" + e.getMessage());
                        }
                        StringBuilder errorOutput = new StringBuilder();
                        errorOutput.append("******************************************************************\n");
                        errorOutput.append("***                        SENT                                ***\n");
                        errorOutput.append("******************************************************************\n");
                        errorOutput.append("Last URL call:").append(TestRestHelper.getLastUrl());
                        if (TestRestHelper.getLastData() != null) {
                            errorOutput.append("\nWith data:").append(TestRestHelper.getLastData());
                        }
                        errorOutput.append("\n");
                        errorOutput.append("******************************************************************\n");
                        errorOutput.append("***                     RECEIVED                               ***\n");
                        errorOutput.append("******************************************************************\n");
                        errorOutput.append(httpResponseProblem).append("\n");
                        provider.handleError(errorOutput.toString());
                    }
                };
            }

        };
    }

    public ErrorHandler getErrorHandler() {
        return errorHandler;
    }

    public TestURLBuilder getUrlBuilder() {
        if (urlBuilder == null) {
            int port = provider.getPort();
            String server = provider.getServer();
            urlBuilder = new TestURLBuilder(provider.getProtocol(), port, server);
        }
        return urlBuilder;
    }

    public String fetchSignups() {
        return getRestHelper().getJSon(getUrlBuilder().buildAdminListsUserSignupsUrl());
    }

    public TestRestHelper getRestHelper() {
        return restHelper;
    }

    public PDSAdministration pds(String hostname, int port, String userid, String apiToken) {
        return new PDSAdministration(hostname, port, userid, apiToken);
    }

    public class PDSAdministration {

        private TestURLBuilder pdsUrlBuilder;

        public PDSAdministration(String hostname, int port, String userid, String apiToken) {
            pdsUrlBuilder = new TestURLBuilder("https", port, hostname);
            TestUser user = new FixedTestUser(userid, apiToken, userid + "_pds@example.com");
            restHelper = new TestRestHelper(user, RestHelperTarget.SECHUB_PDS);
        }

        public String getServerConfiguration() {
            return restHelper.getJSon(pdsUrlBuilder.pds().buildAdminGetServerConfiguration());
        }

        public String getServerAlive() {
            return restHelper.headStringFromURL(pdsUrlBuilder.pds().buildAnonymousCheckAlive());
        }

        public String createPDSJob(UUID sechubJobUUID, String productId, Map<String, String> params) {
            return AsPDSUser.createJobFor(sechubJobUUID, params, productId, restHelper, pdsUrlBuilder);
        }

        public String getExecutionStatus() {
            return restHelper.getJSon(pdsUrlBuilder.pds().buildAdminGetMonitoringStatus());
        }

        public String getJobResultOrError(String jobUUID) {
            return restHelper.getJSon(pdsUrlBuilder.pds().buildGetJobResultOrErrorText(UUID.fromString(jobUUID)));
        }

        public String getJobStatus(String jobUUID) {
            return restHelper.getJSon(pdsUrlBuilder.pds().buildGetJobStatus(UUID.fromString(jobUUID)));
        }

        public String markJobAsReadyToStart(UUID jobUUID) {
            AsPDSUser.markJobAsReadyToStart(jobUUID, restHelper, pdsUrlBuilder);
            return "triggered";
        }

        public String upload(UUID pdsJobUUID, File file, String uploadName) {
            AsPDSUser.upload(pdsUrlBuilder, restHelper, pdsJobUUID, uploadName, file);
            ;
            return "upload done - using uploadname:" + uploadName;
        }

    }

    public String acceptSignup(String userId) {
        getRestHelper().post(getUrlBuilder().buildAdminAcceptsUserSignUpUrl(userId));
        return "SENT";
    }

    public String declineSignup(String userId) {
        getRestHelper().delete(getUrlBuilder().buildAdminDeletesUserSignUpUrl(userId));
        return "SENT";
    }

    public String gGrantAdminRightsTo(String targetUser) {
        getRestHelper().post(getUrlBuilder().buildAdminGrantsSuperAdminRightsTo(targetUser));
        return "SENT";
    }

    public String revokeAddminRightsFrom(String targetUser) {
        getRestHelper().post(getUrlBuilder().buildAdminRevokesSuperAdminRightsFrom(targetUser));
        return "SENT";
    }

    public String createNewUserSignup(String name, String email) {

        String json = "{\"apiVersion\":\"1.0\",\r\n" + "		\"userId\":\"" + name + "\",\r\n" + "		\"emailAdress\":\"" + email + "\"}";
        return getRestHelper().postJSon(getUrlBuilder().buildUserSignUpUrl(), json);
    }

    public String fetchUserList() {
        return getRestHelper().getStringFromURL(getUrlBuilder().buildAdminListsUsersUrl());
    }

    public String fetchAdminList() {
        return getRestHelper().getStringFromURL(getUrlBuilder().buildAdminListsAdminsUrl());
    }

    public String fetchRunningJobsList() {
        return getRestHelper().getStringFromURL(getUrlBuilder().buildAdminFetchAllRunningJobsUrl());
    }

    /*
     * --------------- Execution configuration ------------------------
     */
    public String fetchExecutionProfiles() {
        return asTestUser().fetchProductExecutionProfilesAsJSON();
    }

    public TestExecutorConfig fetchExecutorConfiguration(UUID uuid) {
        return asTestUser().fetchProductExecutorConfig(uuid);
    }
    
    public String fetchExecutorConfigurations() {
        return asTestUser().fetchProductExecutorConfigListAsJSON();
    }
    
    public TestExecutorConfigList fetchExecutorConfigurationList() {
        return asTestUser().fetchProductExecutorConfigList();
    }

    public String deletExecutionConfig(UUID uuid) {
        asTestUser().deleteProductExecutorConfig(uuid);
        return "Deleted product executor config:" + uuid;
    }

    public String deletExecutionProfile(String profileId) {
        InternalAccess.forceDeleteOfProfileEvenDefaults(asTestUser(), profileId);
        return "Deleted product execution profile:" + profileId;
    }

    public String createProject(String projectId, String description, String owner, List<String> whiteListURLs) {
        /* @formatter:off */
		StringBuilder json = new StringBuilder();
		if (description==null || description.isEmpty()) {
			description = "description for project "+projectId;
		}
		TestJSONHelper jsonHelper = TestJSONHelper.get();
		json.append("{\n" +
				" \"apiVersion\":\"1.0\",\n" +
				" \"name\":\""+projectId+"\",\n" +
				" \"owner\":\""+owner+"\",\n" +
				" \"description\":\""+description+"\"");
		if (! whiteListURLs.isEmpty()) {
			json.append(",\n \"whiteList\" : {\"uris\":[");

			for (Iterator<String> it = whiteListURLs.iterator();it.hasNext();) {
				String url = it.next();
				json.append("\""+url+"\"");
				if (it.hasNext()){
					json.append(",");
				}
			}
			json.append("]\n");
			json.append("                 }\n");
		}

		json.append("}\n");
		jsonHelper.assertValidJson(json.toString());
		/* @formatter:on */
        return getRestHelper().postJSon(getUrlBuilder().buildAdminCreatesProjectUrl(), json.toString());
    }

    public String fetchProjectList() {
        return getRestHelper().getStringFromURL(getUrlBuilder().buildAdminListsProjectsUrl());
    }
    
    public List<String> fetchProjectIdList() {
        String json = fetchProjectList();
        SimpleTestStringList list = JSONConverter.get().fromJSON(SimpleTestStringList.class, json);
        return list;
    }

    public String fetchProjectInfo(String projectId) {
        return getRestHelper().getStringFromURL(getUrlBuilder().buildAdminShowsProjectDetailsUrl(projectId));
    }

    public String fetchUserInfo(String userId) {
        return getRestHelper().getStringFromURL(getUrlBuilder().buildAdminShowsUserDetailsUrl(userId));
    }

    public List<String> fetchProjectWhiteList(String projectId) {
        List<String> result = new ArrayList<>();
        String json = getRestHelper().getJSon(getUrlBuilder().buildAdminFetchProjectInfoUrl(projectId));
        TestJSONHelper jsonHelper = TestJSONHelper.get();
        JsonNode jsonNode = jsonHelper.readTree(json);
        JsonNode whitelist = jsonNode.get("whiteList");
        if (whitelist instanceof ArrayNode) {
            ArrayNode arrayNode = (ArrayNode) whitelist;
            for (JsonNode node : arrayNode) {
                String uriText = node.textValue();
                result.add(uriText);
            }

        }

        return result;
    }

    public String fetchProjectScanLogs(String projectId) {
        String json = getRestHelper().getJSon(getUrlBuilder().buildAdminFetchesScanLogsForProject(projectId));
        return json;
    }

    public String fetchJSONReport(String projectId, UUID sechubJobUUID) {
        String json = getRestHelper().getJSon(getUrlBuilder().buildFetchReport(projectId, sechubJobUUID));
        return json;
    }

    public String fetchJobStatus(String projectId, String jobUUID) {
        return getRestHelper().getStringFromURL(getUrlBuilder().buildFetchJobStatus(projectId, jobUUID));
    }

    public void updateProjectWhiteList(String projectId, List<String> result) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\"apiVersion\":\"1.0\", \"whiteList\":{\"uris\":[");
        for (Iterator<String> it = result.iterator(); it.hasNext();) {
            sb.append("\"");
            sb.append(it.next());
            sb.append("\"");
            if (it.hasNext()) {
                sb.append(",");
            }

        }
        sb.append("]}}");

        getRestHelper().postJSon(getUrlBuilder().buildUpdateProjectWhiteListUrl(projectId), sb.toString());
    }

    public String assignUserToProject(String userId, String projectId) {
        getRestHelper().post(getUrlBuilder().buildAdminAssignsUserToProjectUrl(userId, projectId));
        return "assigned " + userId + " to project " + projectId;
    }

    public String unassignUserFromProject(String userId, String projectId) {
        getRestHelper().delete(getUrlBuilder().buildAdminUnassignsUserFromProjectUrl(userId, projectId));
        return "unassigned " + userId + " to project " + projectId;
    }

    public String deleteProject(String projectId) {
        getRestHelper().delete(getUrlBuilder().buildAdminDeletesProject(projectId));
        return "sent";
    }

    public String deleteUser(String userId) {
        getRestHelper().delete(getUrlBuilder().buildAdminDeletesUserUrl(userId));
        return "sent";
    }

    public String cancelJob(UUID jobUUID) {
        getRestHelper().post(getUrlBuilder().buildAdminCancelsJob(jobUUID));
        return "cancel triggered";
    }

    public String requestNewApiToken(String emailAddress) {
        getRestHelper().post(getUrlBuilder().buildAnonymousRequestNewApiToken(emailAddress));
        return "Sent request for new API token for email: " + emailAddress + " - New API token will be delivered to this address if user exists!";
    }

    public String enableSchedulerJobProcessing() {
        getRestHelper().post(getUrlBuilder().buildAdminEnablesSchedulerJobProcessing());
        return "triggered enable job processing";
    }

    public String disableSchedulerJobProcessing() {
        getRestHelper().post(getUrlBuilder().buildAdminDisablesSchedulerJobProcessing());
        return "triggered disable job processing";
    }

    public String refreshSchedulerStatus() {
        getRestHelper().post(getUrlBuilder().buildAdminTriggersRefreshOfSchedulerStatus());
        return "triggered refresh for scheduler status";
    }

    public String getStatusList() {
        return getRestHelper().getJSon(getUrlBuilder().buildAdminListsStatusEntries());
    }

    public String checkAlive() {
        return getRestHelper().headStringFromURL(getUrlBuilder().buildCheckIsAliveUrl());
    }

    public String checkVersion() {
        return getRestHelper().getStringFromURL(getUrlBuilder().buildGetServerVersionUrl());
    }

    public String triggerDownloadFullScan(UUID sechubJobUUID) {

        String url = getUrlBuilder().buildAdminDownloadsZipFileContainingFullScanDataFor(sechubJobUUID);
        return commonTriggerDownloadInBrowser(url);
    }

    /**
     * Creates temporary test user object and provides direct access to integration
     * test object: AsUser. So all things available in integration tests can be done
     * directly without additional methods
     * 
     * @return asUser object
     */
    AsUser createAsUserTestObject() {
        String user = provider.getUser();
        String token = provider.getApiToken();
        TestUser testUser = new FixedTestUser(user, token);
        return TestAPI.as(testUser);
    }

    public WithSecHubClient withSecHubClientOnDefinedBinPath() {
        WithSecHubClient withSechubClient = createAsUserTestObject().withSecHubClient();
        String pathToBinaryparentFolder = ConfigurationSetup.SECHUB_PATH_TO_SECHUB_CLIENT_BINARY.getStringValue(null, false);
        withSechubClient.fromPath(pathToBinaryparentFolder);
        if (ConfigurationSetup.isTrustAllDenied()) {
            withSechubClient.denyTrustAll();
        }
        return withSechubClient;

    }

    public String triggerDownloadReport(String projectId, UUID sechubJobUUID) {
        String url = getUrlBuilder().buildFetchReport(projectId, sechubJobUUID);
        return commonTriggerDownloadInBrowser(url);
    }

    public String restartJob(UUID jobUUID) {
        getRestHelper().post(getUrlBuilder().buildAdminRestartsJob(jobUUID));
        return "restart job triggered";
    }

    public String restartJobHard(UUID jobUUID) {
        getRestHelper().post(getUrlBuilder().buildAdminRestartsJobHard(jobUUID));
        return "restart job (hard) triggered";
    }

    public String fetchProjectFalsePositiveConfiguration(String projectId) {
        return getRestHelper().getJSon(getUrlBuilder().buildUserFetchesFalsePositiveConfigurationOfProject(projectId));
    }

    public String markFalsePositivesForProjectByJobData(String projectId, String json) {
        return getRestHelper().putJSon(getUrlBuilder().buildUserAddsFalsePositiveJobDataListForProject(projectId), json);
    }

    public void deleteFalsePositivesForProject(String projectId, UUID jobUUID, int findingId) {
        getRestHelper().delete(getUrlBuilder().buildUserRemovesFalsePositiveEntryFromProject(projectId, jobUUID.toString(), "" + findingId));
    }

    private String commonTriggerDownloadInBrowser(String url) {
        try {
            java.awt.Desktop.getDesktop().browse(new URI(url));
        } catch (IOException | URISyntaxException e) {
            e.printStackTrace();
            return "Cannot open your system browser for url:" + url + ", please copy url and download in your browser manually.";
        }
        return "Triggered download of " + url + " inside your system browser.";
    }

    private class AdminUserContext implements UserContext {

        @Override
        public String getUserId() {
            return provider.getUser();
        }

        @Override
        public String getApiToken() {
            return provider.getApiToken();
        }

        @Override
        public boolean isAnonymous() {
            return false;
        }

        public TestUser toTestUser() {
            return new FixedTestUser(getUserId(), getApiToken(), getEmail());
        }

        @Override
        public void updateToken(String newToken) {
            /*
             * ignore - we do not need this here, because we just use the edited parts
             * inside text fields
             */
        }

        @Override
        public String getEmail() {
            return "superadmin@example.org";
        }

    }

    public UUID createExecutorConfig(TestExecutorConfig config) {
       return asTestUser().createProductExecutorConfig(config);
    }

    public void updateExecutorConfiguration(TestExecutorConfig config) {
       asTestUser().updateProdcutExecutorConfig(config.uuid, config);
    }

    public void createExecutionProfile(TestExecutionProfile profile) {
        asTestUser().createProductExecutionProfile(profile.id, profile);
    }

    public TestExecutionProfile fetchExecutionProfile(String profileId) {
        return asTestUser().fetchProductExecutionProfile(profileId);
    }

    public void updateExecutionProfile(TestExecutionProfile updatedProfile) {
        asTestUser().updateProductExecutionProfile(updatedProfile.id, updatedProfile);
    }

    private AsUser asTestUser() {
        return TestAPI.as(userContext.toTestUser());
    }

    public TestExecutionProfileList fetchExecutionProfileList() {
        return asTestUser().fetchProductExecutionProfiles();
    }

    public void addProjectIdsToProfile(String profileId, String ... projectIds) {
        asTestUser().addProjectIdsToProfile(profileId, projectIds);
    }
    
    public void removeProjectIdsFromProfile(String profileId, String ...projectIds) {
        asTestUser().removeProjectIdsFromProfile(profileId, projectIds);
    }

    public void addProjectIdsToProfile(String profileId, List<String> list) {
       addProjectIdsToProfile(profileId, list.toArray(new String[list.size()]));
    }

    public void removeProjectIdsFromProfile(String profileId, List<String> list) {
       removeProjectIdsFromProfile(profileId, list.toArray(new String[list.size()]));
    }
}
