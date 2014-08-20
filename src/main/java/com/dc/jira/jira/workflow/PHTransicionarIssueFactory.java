package com.dc.jira.jira.workflow;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import com.atlassian.crowd.embedded.api.User;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.plugin.workflow.AbstractWorkflowPluginFactory;
import com.atlassian.jira.plugin.workflow.WorkflowPluginFunctionFactory;
import com.atlassian.jira.workflow.WorkflowManager;
import com.opensymphony.workflow.loader.AbstractDescriptor;
import com.opensymphony.workflow.loader.FunctionDescriptor;

public class PHTransicionarIssueFactory extends AbstractWorkflowPluginFactory implements WorkflowPluginFunctionFactory {
	@SuppressWarnings("unused")
	private WorkflowManager workflowManager;

	public PHTransicionarIssueFactory(WorkflowManager workflowManager) {
		this.workflowManager = workflowManager;
	}

	@Override
	protected void getVelocityParamsForInput(Map<String, Object> velocityParams) {
		Collection<User> admUsers = ComponentAccessor.getGroupManager().getUsersInGroup("jira-administrators");
		velocityParams.put("allUsers", admUsers);
		velocityParams.put("ProyectoHoras", "PHOENIX-40");
	}

	@Override
	protected void getVelocityParamsForEdit(Map<String, Object> velocityParams, AbstractDescriptor descriptor) {
		getVelocityParamsForInput(velocityParams);
		getVelocityParamsForView(velocityParams, descriptor);
	}

	@Override
	protected void getVelocityParamsForView(Map<String, Object> velocityParams, AbstractDescriptor descriptor) {
		if (!(descriptor instanceof FunctionDescriptor)) {
			throw new IllegalArgumentException("Descriptor must be a FunctionDescriptor.");
		}

		FunctionDescriptor functionDescriptor = (FunctionDescriptor) descriptor;

		String userAdmin = (String) functionDescriptor.getArgs().get("userAdmin");
		velocityParams.put("userAdmin", userAdmin);
		
		String proHoras = (String) functionDescriptor.getArgs().get("ProyectoHoras");
		if (proHoras == null) {
			proHoras = "PHOENIX-40";
		}
		velocityParams.put("ProyectoHoras", proHoras);
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public Map<String, ?> getDescriptorParams(Map<String, Object> formParams) {
		Map params = new HashMap();

		String userAdmin = extractSingleParam(formParams, "userAdmin");
		params.put("userAdmin", userAdmin);

		String proHoras = extractSingleParam(formParams, "ProyectoHoras");
		params.put("ProyectoHoras", proHoras);

		return params;
	}

}