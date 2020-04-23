package com.kinnara.kecakplugins.emailapproval.optionsbinder;

import com.kinnara.kecakplugins.emailapproval.generator.WorkflowVariableOptionsGenerator;
import org.joget.apps.app.model.AppDefinition;
import org.joget.apps.app.model.PackageDefinition;
import org.joget.apps.app.service.AppUtil;
import org.joget.apps.form.model.*;
import org.joget.apps.form.service.FormUtil;
import org.joget.plugin.base.PluginWebSupport;
import org.joget.workflow.model.service.WorkflowUserManager;
import org.joget.workflow.util.WorkflowUtil;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class WorkflowVariableOptionsBinder extends FormBinder implements FormLoadOptionsBinder, PluginWebSupport {
    @Override
    public FormRowSet load(Element element, String primaryKey, FormData formData) {
        FormRowSet result = new FormRowSet();
        WorkflowVariableOptionsGenerator generator = new WorkflowVariableOptionsGenerator(AppUtil.getCurrentAppDefinition());
        generator.generate(result::add);
        return result;
    }

    @Override
    public String getName() {
        return getClass().getName();
    }

    @Override
    public String getVersion() {
        return getClass().getPackage().getImplementationVersion();
    }

    @Override
    public String getDescription() {
        return getClass().getPackage().getImplementationTitle();
    }

    /**
     * Parameters
     * <ul>
     *     <li>packageId : required - Package ID of current Appllication</li>
     * </ul>
     * @param request
     * @param response
     * @throws ServletException
     * @throws IOException
     */
    @Override
    public void webService(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        boolean isAdmin = WorkflowUtil.isCurrentUserInRole(WorkflowUserManager.ROLE_ADMIN);
        if (!isAdmin) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        AppDefinition appDefinition = AppUtil.getCurrentAppDefinition();

        final JSONArray result = new JSONArray();

        try {
            // add empty record
            JSONObject empty = new JSONObject();
            empty.put(FormUtil.PROPERTY_VALUE, "");
            empty.put(FormUtil.PROPERTY_LABEL, "");
            result.put(empty);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        WorkflowVariableOptionsGenerator generator = new WorkflowVariableOptionsGenerator(appDefinition);
        generator.generate(row -> {
            JSONObject item = new JSONObject();
            try {
                item.put(FormUtil.PROPERTY_VALUE, row.getProperty(FormUtil.PROPERTY_VALUE));
                item.put(FormUtil.PROPERTY_LABEL, row.getProperty(FormUtil.PROPERTY_LABEL));
                result.put(item);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        });

        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType("application/json");
        response.getWriter().write(result.toString());
    }

    @Override
    public String getLabel() {
        return "Workflow Variable Options Binder";
    }

    @Override
    public String getClassName() {
        return getClass().getName();
    }

    @Override
    public String getPropertyOptions() {
        return "";
    }
}
