package com.kinnara.kecakplugins.emailapproval;

import org.joget.apps.app.model.AppDefinition;
import org.joget.apps.app.model.DefaultEmailProcessorPlugin;
import org.joget.apps.app.model.PackageActivityForm;
import org.joget.apps.app.service.AppService;
import org.joget.apps.app.service.AppUtil;
import org.joget.apps.form.model.Element;
import org.joget.apps.form.model.Form;
import org.joget.apps.form.model.FormData;
import org.joget.apps.form.service.FormUtil;
import org.joget.commons.util.LogUtil;
import org.joget.commons.util.StringUtil;
import org.joget.plugin.base.Plugin;
import org.joget.plugin.base.PluginManager;
import org.joget.workflow.model.WorkflowActivity;
import org.joget.workflow.model.WorkflowAssignment;
import org.joget.workflow.model.WorkflowProcessLink;
import org.joget.workflow.model.dao.WorkflowProcessLinkDao;
import org.joget.workflow.model.service.WorkflowManager;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.context.ApplicationContext;

import javax.annotation.Nonnull;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public class EmailApproval extends DefaultEmailProcessorPlugin {
    @Override
    public void parse(String from, String subject, String body, Map<String, Object> properties) {
        LogUtil.info(getClassName(), "Parsing email from ["+from+"] subject ["+subject+"] body ["+body+"]");

        String subjectPattern = getPropertyString("subjectPattern");
        Matcher matcher = Pattern.compile("\\{([a-zA-Z0-9_]+)\\}").matcher(subjectPattern);
        String subjectRegex = createRegex(subjectPattern);
        Pattern pattern2 = Pattern.compile("^" + subjectRegex + "$");
        Matcher matcher2 = pattern2.matcher(subject);

        String processId = null;
        while (matcher2.find()) {
            int count = 1;
            while (matcher.find()) {
                String key = matcher.group(1);
                String value = matcher2.group(count);
                if ("processId".equals(key)) {
                    processId = value;
                }
                count++;
            }
        }

        if (processId != null) {
            parseEmailContent(processId, body.toString());
        } else {
            LogUtil.info(getClass().getName(), "Empty process ID");
        }
    }


    @SuppressWarnings("unchecked")
    private void parseEmailContent(String processId, String emailContent) {
        ApplicationContext applicationContext = AppUtil.getApplicationContext();
        WorkflowProcessLinkDao workflowProcessLinkDao = (WorkflowProcessLinkDao) applicationContext.getBean("workflowProcessLinkDao");
        WorkflowManager workflowManager = (WorkflowManager) applicationContext.getBean("workflowManager");

        String content = emailContent.replaceAll("\\r?\\n", " ");
        content = content.replaceAll("\\_\\_", " ");
        content = StringUtil.decodeURL(content);

        WorkflowAssignment workflowAssignment = Optional.ofNullable(workflowProcessLinkDao.getLinks(processId))
                .map(Collection::stream)
                .orElse(Stream.empty())
                .map(WorkflowProcessLink::getProcessId)
                .map(workflowManager::getAssignmentByProcess)
                .filter(Objects::nonNull)
                .filter(assignment -> Arrays.stream(getPropertyString("activities").split(";")).anyMatch(s -> assignment.getActivityDefId().equals(s)))
                .peek(assignment -> LogUtil.info(getClassName(), "Process ID [" + assignment.getProcessId() + "] assignment ID ["+assignment.getActivityId()+"]"))
                .findFirst()
                .orElse(null);

        if (workflowAssignment == null) {
            LogUtil.info(this.getClass().getName(), "Assignment for process ["+processId+"] is null");
            return;
        }

        String emailContentPattern = getPropertyString("bodyPattern").replaceAll("\\r?\\n", " ");
        String patternRegex = createRegex(emailContentPattern);
        LogUtil.info(this.getClass().getName(), "Content REGEX [" + patternRegex + "]");
        Pattern pattern = Pattern.compile("\\{([a-zA-Z0-9_]+)\\}");
        Matcher matcher = pattern.matcher(emailContentPattern);

        Pattern pattern2 = Pattern.compile("^" + patternRegex + "$");
        Matcher matcher2 = pattern2.matcher(content);

        Map<String, String> variables = new HashMap<>();
        Map<String, String> fields = new HashMap<>();

        while (matcher2.find()) {
            int count = 1;
            while (matcher.find()) {
                String key = matcher.group(1);
                String value = matcher2.group(count);
                if (key.startsWith("var_")) {
                    key = key.replaceAll("var_", "");
                    LogUtil.info(this.getClass().getName(), "[Var] "+key);
                    variables.put(key, value.trim());
                } else if (key.startsWith("form_")) {
                    key = key.replaceAll("form_", "");
                    LogUtil.info(this.getClass().getName(), "[Form] "+key+" ,[VALUE] "+value);
                    if(value == null || value.trim().equals("")){
                        value = "-";
                    }
                    fields.put(key, value);
                }
                count++;
            }
        }

        completeActivity(workflowAssignment, fields, variables);
    }

    @SuppressWarnings({"deprecation", "unchecked"})
    private void completeActivity(WorkflowAssignment assignment, @Nonnull final Map<String, String> fields, @Nonnull final Map<String, String> variables) {
        AppService appService = (AppService) AppUtil.getApplicationContext().getBean("appService");
        AppDefinition appDefinition = appService.getAppDefinitionForWorkflowActivity(assignment.getActivityId());

        //if has form data to submit
        final FormData formData = new FormData();
        PackageActivityForm activityForm = appService.viewAssignmentForm(appDefinition, assignment, formData, null, "");
        Form form = activityForm.getForm();
        for (Map.Entry<String, String> entry : fields.entrySet()) {
            Element element = FormUtil.findElement(entry.getKey(), form, formData, true);
            if(element != null) {
                String parameterName = FormUtil.getElementParameterName(element);
                formData.addRequestParameterValues(parameterName, new String[] {entry.getValue()});
            }
        }

        FormData resultFormData = appService.completeAssignmentForm(appDefinition.getAppId(), appDefinition.getVersion().toString(), assignment.getActivityId(), formData, variables);
        resultFormData.getFormErrors().forEach((key, value) ->
                LogUtil.warn(getClassName(), "Error submitting form [" + form.getPropertyString(FormUtil.PROPERTY_ID) + "] field [" + key + "] message [" + value + "]"));

//        WorkflowAssignment nextAssignment = workflowManager.getAssignmentByProcess(resultFormData.getProcessId());
//	                    sendAutoReply(sender, subject);
//	                addActivityLog(sender, processId, activityId, subject, message, variables, formData.getRequestParams());
    }

    private String createRegex(String raw) {
        String result = escapeString(raw, null);
        result = result.replaceAll("\\\\\\{unuse\\\\\\}", "__([\\\\s\\\\S]*)").replaceAll("\\\\\\{[a-zA-Z0-9_]+\\\\\\}", "(.*?)");
        if (result.startsWith("__")) {
            result = result.substring(2);
        }
        return result;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private String escapeString(String inStr, Map<String, String> replaceMap) {
        if (replaceMap != null) {
            Iterator it = replaceMap.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<String, String> pairs = (Map.Entry<String, String>) it.next();
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
        return "Email Approval";
    }

    @Override
    public String getVersion() {
        return getClass().getPackage().getImplementationVersion();
    }

    @Override
    public String getDescription() {
        return getClass().getPackage().getImplementationTitle();
    }

    @Override
    public String getLabel() {
        return getName();
    }

    @Override
    public String getClassName() {
        return getClass().getName();
    }

    @Override
    public String getPropertyOptions() {
        ApplicationContext applicationContext = AppUtil.getApplicationContext();
        AppDefinition appDefinition = AppUtil.getCurrentAppDefinition();
        WorkflowManager workflowManager = (WorkflowManager) applicationContext.getBean("workflowManager");
        JSONObject activitiesProperty = new JSONObject();
        try {
            activitiesProperty.put("name", "activities");
            activitiesProperty.put("label", "@@emailApproval.activities@@");
            activitiesProperty.put("required", "true");
            if(appDefinition != null && isClassInstalled("com.kinnara.kecakplugins.workflowcomponentoptionsbinder.ActivityOptionsBinder")) {
                String appId = appDefinition.getAppId();
                String appVersion = appDefinition.getVersion().toString();
                activitiesProperty.put("type", "multiselect");
                activitiesProperty.put("options_ajax","[CONTEXT_PATH]/web/json/app[APP_PATH]/plugin/com.kinnara.kecakplugins.workflowcomponentoptionsbinder.ActivityOptionsBinder/service?appId="+appId + "&appVersion=" + appVersion + "&type=" + WorkflowActivity.TYPE_NORMAL);
            } else {
                activitiesProperty.put("type", "textfield");
            }
        } catch (JSONException ignored) { }

        return AppUtil.readPluginResource(getClassName(), "/properties/emailApproval.json", new String[] {activitiesProperty.toString().replaceAll("\"", "'")}, false, "/messages/emailApproval");
    }

    private boolean isClassInstalled(String className) {
        PluginManager pluginManager = (PluginManager) AppUtil.getApplicationContext().getBean("pluginManager");
        Plugin plugin = pluginManager.getPlugin(className);
        return plugin != null;
    }
}
