package com.kinnara.kecakplugins.emailapproval;

import java.util.ArrayList;
import java.util.Collection;

import com.kinnara.kecakplugins.emailapproval.optionsbinder.ActivityOptionsBinder;
import com.kinnara.kecakplugins.emailapproval.optionsbinder.WorkflowVariableOptionsBinder;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

public class Activator implements BundleActivator {

    protected Collection<ServiceRegistration> registrationList;

    public void start(BundleContext context) {
        registrationList = new ArrayList<ServiceRegistration>();

        //Register plugin here
        registrationList.add(context.registerService(EmailApprovalProcessor.class.getName(), new EmailApprovalProcessor(), null));
        registrationList.add(context.registerService(EmailApprovalNotification.class.getName(), new EmailApprovalNotification(), null));
        registrationList.add(context.registerService(EmailReader.class.getName(), new EmailReader(), null));
        registrationList.add(context.registerService(ActivityOptionsBinder.class.getName(), new ActivityOptionsBinder(), null));
        registrationList.add(context.registerService(WorkflowVariableOptionsBinder.class.getName(), new WorkflowVariableOptionsBinder(), null));
    }

    public void stop(BundleContext context) {
        for (ServiceRegistration registration : registrationList) {
            registration.unregister();
        }
    }
}