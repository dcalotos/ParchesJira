package com.dc.jira.jira.workflow;

import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.issue.IssueManager;
import com.atlassian.jira.plugin.workflow.AbstractWorkflowPluginFactory;
import com.atlassian.jira.plugin.workflow.WorkflowPluginFunctionFactory;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.project.ProjectManager;
import com.atlassian.jira.security.roles.ProjectRole;
import com.atlassian.jira.security.roles.ProjectRoleActors;
import com.atlassian.jira.security.roles.ProjectRoleManager;
import com.atlassian.jira.workflow.JiraWorkflow;
import com.atlassian.jira.workflow.WorkflowManager;
import com.opensymphony.workflow.loader.*;

import webwork.action.ActionContext;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@SuppressWarnings("unused")
public class IPFAsignacionResponsableInformanteCreacionFactory extends AbstractWorkflowPluginFactory
		implements
			WorkflowPluginFunctionFactory {

	public static final String USER_ADMIN = "userAdmin";

	private ProjectManager projectManager;
	private ProjectRoleManager projectRoleManager;

	public IPFAsignacionResponsableInformanteCreacionFactory(ProjectManager projectManager, IssueManager issueManager,
			ProjectRoleManager projectRoleManager, WorkflowManager workflowManager) {
		this.projectManager = projectManager;
		this.projectRoleManager = projectRoleManager;
	}

	@SuppressWarnings({"rawtypes"})
	@Override
	protected void getVelocityParamsForInput(Map<String, Object> velocityParams) {
		// Map<String, String[]> myParams = ActionContext.getParameters();
		projectManager = ComponentAccessor.getProjectManager();
		Project project = projectManager.getProjectObjByKey("IPF");

		projectRoleManager = ComponentAccessor.getComponentOfType(ProjectRoleManager.class);
		ProjectRole projectRole = projectRoleManager.getProjectRole("Administrators");

		ProjectRoleActors projectRoleActors = projectRoleManager.getProjectRoleActors(projectRole, project);
		Set allUsers = projectRoleActors.getUsers();

		velocityParams.put("allUsers", allUsers);

	}

	@Override
	protected void getVelocityParamsForEdit(Map<String, Object> velocityParams, AbstractDescriptor descriptor) {

		getVelocityParamsForInput(velocityParams);
		getVelocityParamsForView(velocityParams, descriptor);

	}

	/*
	 * Este método es que se utiliza para ver datos a través de la macro de velocity. En este caso es un campo texto. En
	 * la macro de velocity: &nbsp;&nbsp;-&nbsp;&nbsp;&nbsp;Usuario administrador seleccionado para edición de la issue
	 * <b>$userAdmin</b>.
	 */
	@Override
	protected void getVelocityParamsForView(Map<String, Object> velocityParams, AbstractDescriptor descriptor) {
		if (!(descriptor instanceof FunctionDescriptor)) {
			throw new IllegalArgumentException("Descriptor must be a FunctionDescriptor.");
		}

		FunctionDescriptor functionDescriptor = (FunctionDescriptor) descriptor;

		String userAdmin = (String) functionDescriptor.getArgs().get("userAdmin");
		velocityParams.put("userAdmin", userAdmin);

	}

	/*
	 * Para pasar los valores a la function-class. Clase que tiene la lógica que hay que hacer en la postfunction. Los
	 * valores viajan en Map args, para poder ser recuperados.
	 */
	@SuppressWarnings({"unchecked", "rawtypes"})
	public Map<String, ?> getDescriptorParams(Map<String, Object> formParams) {
		Map params = new HashMap();

		String userAdmin = extractSingleParam(formParams, "userAdmin");
		params.put("userAdmin", userAdmin);

		return params;
	}
}