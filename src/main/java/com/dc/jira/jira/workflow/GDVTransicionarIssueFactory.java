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

public class GDVTransicionarIssueFactory extends AbstractWorkflowPluginFactory implements WorkflowPluginFunctionFactory {
	@SuppressWarnings("unused")
	private WorkflowManager workflowManager;

	public GDVTransicionarIssueFactory(WorkflowManager workflowManager) {
		this.workflowManager = workflowManager;
	}

	@Override
	protected void getVelocityParamsForInput(Map<String, Object> velocityParams) {
		Collection<User> admUsers = ComponentAccessor.getGroupManager().getUsersInGroup("jira-administrators");
		velocityParams.put("allUsers", admUsers);
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
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public Map<String, ?> getDescriptorParams(Map<String, Object> formParams) {
		Map params = new HashMap();

		String userAdmin = extractSingleParam(formParams, "userAdmin");
		params.put("userAdmin", userAdmin);

		return params;
	}

}