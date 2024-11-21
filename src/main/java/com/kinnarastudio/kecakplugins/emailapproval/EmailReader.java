package com.kinnarastudio.kecakplugins.emailapproval;

import org.joget.apps.app.service.AppUtil;
import org.joget.commons.util.LogUtil;
import org.joget.plugin.base.PluginManager;
import org.kecak.apps.app.model.DefaultEmailProcessorPlugin;

import java.util.Map;
import java.util.ResourceBundle;

/**
 * @author aristo
 * Simply read the email and do nothing
 */
public class EmailReader extends DefaultEmailProcessorPlugin {
    @Override
    public void parse(String from, String subject, String body, Map<String, Object> properties) {
        LogUtil.info(getClassName(), "from ["+from+"] subject ["+subject+"] body ["+body+"]");
    }

    @Override
    public String getName() {
        return "Email Reader";
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
        return getName();
    }

    @Override
    public String getClassName() {
        return getClass().getName();
    }

    @Override
    public String getPropertyOptions() {
        return null;
    }
}
