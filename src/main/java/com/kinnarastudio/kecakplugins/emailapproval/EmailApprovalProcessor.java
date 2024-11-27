package com.kinnarastudio.kecakplugins.emailapproval;

import com.kinnarastudio.kecakplugins.emailapproval.optionsbinder.ActivityOptionsBinder;
import com.kinnarastudio.kecakplugins.emailapproval.optionsbinder.WorkflowVariableOptionsBinder;
import org.joget.apps.app.model.AppDefinition;
import org.joget.apps.app.model.PackageActivityForm;
import org.joget.apps.app.service.AppService;
import org.joget.apps.app.service.AppUtil;
import org.joget.apps.form.model.Element;
import org.joget.apps.form.model.Form;
import org.joget.apps.form.model.FormData;
import org.joget.apps.form.service.FormUtil;
import org.joget.commons.util.LogUtil;
import org.joget.commons.util.StringUtil;
import org.joget.plugin.base.PluginManager;
import org.joget.workflow.model.WorkflowAssignment;
import org.joget.workflow.model.WorkflowProcessLink;
import org.joget.workflow.model.dao.WorkflowProcessLinkDao;
import org.joget.workflow.model.service.WorkflowManager;
import org.kecak.apps.app.model.DefaultEmailProcessorPlugin;
import org.springframework.context.ApplicationContext;

import javax.annotation.Nonnull;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class EmailApprovalProcessor extends DefaultEmailProcessorPlugin implements Unclutter {
    @Override
    public void parse(String from, String subject, String body, Map<String, Object> properties) {
        String propSubjectPattern = "[{processId}][{var_" + getStatusVariable() + "}]";
        Matcher templateSubjectMatcher = Pattern.compile("\\{([a-zA-Z0-9_]+)\\}").matcher(propSubjectPattern);
        String templateSubjectRegex = createRegex(propSubjectPattern);
        Pattern templateSubjectPattern = Pattern.compile("^" + templateSubjectRegex + "$");
        Matcher contentSubjectMatcher = templateSubjectPattern.matcher(subject);

        Map<String, String> variables = new HashMap<>();
        Map<String, String> fields = new HashMap<>();

        String processId = null;
        while (contentSubjectMatcher.find()) {
            int count = 1;
            while (templateSubjectMatcher.find()) {
                String key = templateSubjectMatcher.group(1);
                String value = contentSubjectMatcher.group(count);
                if ("processId".equals(key)) {
                    processId = value.trim();
                } else if (key.startsWith("var_")) {
                    key = key.replaceAll("var_", "");
                    variables.put(key, value.trim());
                } else if (key.startsWith("form_")) {
                    key = key.replaceAll("form_", "");
                    if (value == null || value.trim().equals("")) {
                        value = "-";
                    }
                    fields.put(key, value);
                }
                count++;
            }
        }

        if (processId != null) {
            parseEmailContent(processId, body, variables, fields);
        } else {
            LogUtil.warn(getClass().getName(), "Empty process ID");
        }
    }


    @SuppressWarnings("unchecked")
    private void parseEmailContent(String processId, String emailContent, @Nonnull Map<String, String> variables, @Nonnull Map<String, String> fields) {
        String content = emailContent.replaceAll("\\r?\\n", " ");
        content = content.replaceAll("\\_\\_", " ");
        content = StringUtil.decodeURL(content);

        Optional<WorkflowAssignment> workflowAssignment = getActivityAssignment(processId);
        if (workflowAssignment.isEmpty()) {
            LogUtil.warn(this.getClass().getName(), "Assignment for process [" + processId + "] is null");
            return;
        }

        String emailContentPattern = getBodyPattern();
        String patternRegex = createRegex(emailContentPattern);
        Pattern pattern = Pattern.compile("\\{([a-zA-Z0-9_]+)\\}");
        Matcher matcher = pattern.matcher(emailContentPattern);

        Pattern pattern2 = Pattern.compile("^" + patternRegex + "$");
        Matcher matcher2 = pattern2.matcher(content);

        while (matcher2.find()) {
            int count = 1;
            while (matcher.find()) {
                String key = matcher.group(1);
                String value = matcher2.group(count);
                if (key.startsWith("var_")) {
                    key = key.replaceAll("var_", "");
                    variables.put(key, value.trim());
                } else if (key.startsWith("form_")) {
                    key = key.replaceAll("form_", "");
                    if (value == null || value.trim().equals("")) {
                        value = "-";
                    }
                    fields.put(key, value);
                }
                count++;
            }
        }

        workflowAssignment.ifPresent(a -> completeActivity(a, fields, variables));
    }

    /**
     * Complete assignment
     *
     * @param assignment
     * @param fields
     * @param variables
     */
    private void completeActivity(WorkflowAssignment assignment, @Nonnull final Map<String, String> fields, @Nonnull final Map<String, String> variables) {
        LogUtil.info(getClassName(), "Completing assignment [" + assignment.getActivityId() + "]");

        AppService appService = (AppService) AppUtil.getApplicationContext().getBean("appService");
        AppDefinition appDefinition = appService.getAppDefinitionForWorkflowActivity(assignment.getActivityId());

        //if has form data to submit
        final FormData formData = new FormData();
        formData.setActivityId(assignment.getActivityId());
        formData.setProcessId(assignment.getProcessId());

        PackageActivityForm activityForm = appService.viewAssignmentForm(appDefinition, assignment, formData, null, "");
        Form form = activityForm.getForm();

        // set request parameter for status workflow variable
        for (Map.Entry<String, String> entry : variables.entrySet()) {
            if (getStatusVariable().equals(entry.getKey())) {
                elementStream(form, formData)
                        .filter(e -> Optional.of("workflowVariable")
                                .map(e::getPropertyString)
                                .map(s -> getStatusVariable().equals(s))
                                .orElse(false))
                        .forEach(e -> {
                            String parameterName = FormUtil.getElementParameterName(e);
                            formData.addRequestParameterValues(parameterName, new String[]{entry.getValue()});
                        });
            }
        }

        for (Map.Entry<String, String> entry : fields.entrySet()) {
            Element element = FormUtil.findElement(entry.getKey(), form, formData, true);
            if (element != null) {
                String parameterName = FormUtil.getElementParameterName(element);
                formData.addRequestParameterValues(parameterName, new String[]{entry.getValue()});
            }
        }

        FormData resultFormData = appService.completeAssignmentForm(form, assignment, formData, variables);
        resultFormData.getFormErrors().forEach((key, value) ->
                LogUtil.warn(getClassName(), "Error submitting form [" + form.getPropertyString(FormUtil.PROPERTY_ID) + "] field [" + key + "] message [" + value + "]"));
    }

    private String createRegex(String raw) {
        String result = escapeString(raw, null);
        result = result.replaceAll("\\\\\\{unuse\\\\}", "__([\\\\s\\\\S]*)").replaceAll("\\\\\\{[a-zA-Z0-9_]+\\\\}", "(.*?)");
        if (result.startsWith("__")) {
            result = result.substring(2);
        }
        return result;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private String escapeString(String inStr, Map<String, String> replaceMap) {
        if (replaceMap != null) {
            for (Map.Entry<String, String> pairs : replaceMap.entrySet()) {
                inStr = inStr.replaceAll(pairs.getKey(), escapeRegex(pairs.getValue()));
            }
        }

        return escapeRegex(inStr);
    }

    private String escapeRegex(String inStr) {
        return (inStr != null) ? inStr.replaceAll("([\\\\*+\\[\\](){}\\$.?\\^|])", "\\\\$1") : null;
    }

    @Override
    public String getName() {
        return getClass().getName();
    }

    @Override
    public String getVersion() {
        PluginManager pluginManager = (PluginManager) AppUtil.getApplicationContext().getBean("pluginManager");
        ResourceBundle resourceBundle = pluginManager.getPluginMessageBundle(getClassName(), "/messages/BuildNumber");
        String buildNumber = resourceBundle.getString("buildNumber");
        return buildNumber;
    }

    @Override
    public String getDescription() {
        return getClass().getPackage().getImplementationTitle();
    }

    @Override
    public String getLabel() {
        return "Email Approval Processor";
    }

    @Override
    public String getClassName() {
        return getClass().getName();
    }

    @Override
    public String getPropertyOptions() {
        String[] args = new String[]{
                ActivityOptionsBinder.class.getName(),
                WorkflowVariableOptionsBinder.class.getName()
        };
        return AppUtil.readPluginResource(getClassName(), "/properties/emailApprovalProcessor.json", args, false, "/messages/emailApprovalProcessor");
    }

    /**
     * Get activity assignment based on processId / primaryKey
     * The method also check for linked process from table wf_process_link
     *
     * @param processId processId or primaryKey
     * @return
     */
    protected Optional<WorkflowAssignment> getActivityAssignment(String processId) {
        ApplicationContext applicationContext = AppUtil.getApplicationContext();
        WorkflowManager workflowManager = (WorkflowManager) applicationContext.getBean("workflowManager");
        return Optional.ofNullable(workflowManager.getAssignmentByProcess(processId));
    }

    /**
     * Get property "activities"
     *
     * @return
     */
    @Nonnull
    private Set<String> getActivities() {
        return Optional.ofNullable(getPropertyString("activities"))
                .map(s -> s.split(";"))
                .stream()
                .flatMap(Arrays::stream)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toSet());
    }

    /**
     * Get property "statusVariable"
     *
     * @return
     */
    @Nonnull
    protected String getStatusVariable() {
        return String.valueOf(getPropertyString("statusVariable"));
    }

    /**
     * Get property "bodyPattern"
     *
     * @return
     */
    @Nonnull
    protected String getBodyPattern() {
        return Optional.ofNullable(getPropertyString("bodyPattern"))
                .map(s -> s.replaceAll("\\r?\\n", " "))
                .orElse("");
    }

    /**
     * Stream element children
     *
     * @param element
     * @return
     */
    @Nonnull
    private Stream<Element> elementStream(@Nonnull Element element, FormData formData) {
        if (!element.isAuthorize(formData)) {
            return Stream.empty();
        }

        Stream<Element> stream = Stream.of(element);
        for (Element child : element.getChildren()) {
            stream = Stream.concat(stream, elementStream(child, formData));
        }
        return stream;
    }
}
