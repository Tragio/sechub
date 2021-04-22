// SPDX-License-Identifier: MIT
package com.daimler.sechub.developertools.admin.ui.action.project;

import java.awt.event.ActionEvent;
import java.util.Optional;

import com.daimler.sechub.developertools.admin.ui.UIContext;
import com.daimler.sechub.developertools.admin.ui.action.AbstractUIAction;
import com.daimler.sechub.developertools.admin.ui.cache.InputCacheIdentifier;
import com.daimler.sechub.developertools.admin.ui.util.DataCollectorUtils;

public class UpdateProjectDescriptionAction extends AbstractUIAction {
    private static final long serialVersionUID = 1L;

    public UpdateProjectDescriptionAction(UIContext context) {
        super("Update project description", context);
    }

    @Override
    public void execute(ActionEvent e) {
        Optional<String> optProjectId = getUserInput("Please enter project ID/name", InputCacheIdentifier.PROJECT_ID);
        if (!optProjectId.isPresent()) {
            return;
        }

        String projectId = optProjectId.get().toLowerCase().trim();
        String description = getContext().getAdministration().fetchProjectDescription(projectId);
        
        Optional<String> optDescription = getUserInput("Description;", description);
        
        
        if (!optDescription.isPresent()) {
            return;
        }
        
        String newDescription = optDescription.get();

        getContext().getAdministration().updateProjectDescription(projectId, newDescription);
        
        // fetching the changed projectDetails and displaying them in the outputText
        
        String data = getContext().getAdministration().fetchProjectInfo(asSecHubId(projectId));
        outputAsBeautifiedJSONOnSuccess(data);
        
        data = DataCollectorUtils.fetchProfileInformationAboutProject(projectId, getContext());
        outputAsTextOnSuccess(data);
    }

}