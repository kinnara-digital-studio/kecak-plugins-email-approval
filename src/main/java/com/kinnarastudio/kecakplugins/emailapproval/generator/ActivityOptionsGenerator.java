package com.kinnarastudio.kecakplugins.emailapproval.generator;

import org.joget.apps.app.model.AppDefinition;
import org.joget.apps.app.model.PackageDefinition;
import org.joget.apps.app.service.AppUtil;
import org.joget.apps.form.model.FormRow;
import org.joget.apps.form.service.FormUtil;
import org.joget.workflow.model.WorkflowActivity;
import org.joget.workflow.model.WorkflowProcess;
import org.joget.workflow.model.service.WorkflowManager;
import org.springframework.context.ApplicationContext;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * Helper to generate list of activities/tools/sub-processes/routes
 * @author aristo
 *
 */
public class ActivityOptionsGenerator implements OptionsGenerator {
	/**
	 * {@link WorkflowActivity}
	 */
	final private String type;
	final private AppDefinition appDefinition;
	
	/**
	 * @param type
	 * activities : {@link WorkflowActivity#TYPE_NORMAL}
	 * tools : {@link WorkflowActivity#TYPE_TOOL}
	 * sub-processes : {@link WorkflowActivity#TYPE_SUBFLOW}
	 * routes : {@link WorkflowActivity#TYPE_ROUTE}
	 */

	public ActivityOptionsGenerator(AppDefinition appDefinition, String type) {
		this.type = type;
		this.appDefinition = appDefinition;
	}
	
	public final void generate(OnGenerateListener listener) {
		ApplicationContext appContext = AppUtil.getApplicationContext();
		WorkflowManager wfManager = (WorkflowManager)appContext.getBean("workflowManager");

		PackageDefinition packageDefinition = appDefinition.getPackageDefinition();
        Long packageVersion = (packageDefinition != null) ? packageDefinition.getVersion() : 1L;
        Collection<WorkflowProcess> processes = wfManager.getProcessList(appDefinition.getAppId(), packageVersion.toString());

		Set<String> uniqueName = new HashSet<>();

		for(WorkflowProcess process : processes) {
			Collection<WorkflowActivity> activities = wfManager.getProcessActivityDefinitionList(process.getId());
			for(WorkflowActivity activity : activities) {
				if(type == null || "".equals(type) || activity.getType().equals(type)) {
					if(uniqueName.add(activity.getId())) {
						FormRow row = new FormRow();
						row.put(FormUtil.PROPERTY_VALUE, activity.getId());
						row.put(FormUtil.PROPERTY_LABEL, activity.getName() + " ("+activity.getId()+")");
						listener.onGenerated(row);
					}
				}
			}
		}
	}
}
