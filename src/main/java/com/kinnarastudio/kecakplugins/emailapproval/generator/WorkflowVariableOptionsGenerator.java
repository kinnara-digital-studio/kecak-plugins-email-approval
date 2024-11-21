package com.kinnarastudio.kecakplugins.emailapproval.generator;

import org.joget.apps.app.model.AppDefinition;
import org.joget.apps.app.model.PackageDefinition;
import org.joget.apps.app.service.AppUtil;
import org.joget.apps.form.model.FormRow;
import org.joget.apps.form.service.FormUtil;
import org.joget.workflow.model.WorkflowVariable;
import org.joget.workflow.model.service.WorkflowManager;

import java.util.Collection;
import java.util.Optional;
import java.util.stream.Stream;

public class WorkflowVariableOptionsGenerator implements OptionsGenerator {
    private AppDefinition appDefinition;

    public WorkflowVariableOptionsGenerator(AppDefinition appDefinition) {
        this.appDefinition = appDefinition;
    }

    @Override
    public void generate(OnGenerateListener listener) {
        WorkflowManager workflowManager = (WorkflowManager) AppUtil.getApplicationContext().getBean("workflowManager");

        Optional.of(appDefinition)
                .map(AppDefinition::getPackageDefinition)
                .map(PackageDefinition::getId)
                .map(workflowManager::getProcessList)
                .map(Collection::stream)
                .orElseGet(Stream::empty)
                .flatMap(p -> workflowManager.getProcessVariableDefinitionList(p.getId()).stream())
                .map(WorkflowVariable::getId)
                .distinct()
                .sorted()
                .map(var -> {
                    FormRow row = new FormRow();
                    row.setProperty(FormUtil.PROPERTY_VALUE, var);
                    row.setProperty(FormUtil.PROPERTY_LABEL, var);
                    return row;
                })
                .forEach(listener::onGenerated);
    }
}
