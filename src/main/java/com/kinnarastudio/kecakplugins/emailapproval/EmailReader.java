package com.kinnarastudio.kecakplugins.emailapproval;

import org.joget.commons.util.LogUtil;
import org.kecak.apps.app.model.DefaultEmailProcessorPlugin;

import java.util.Map;

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
        return null;
    }
}
