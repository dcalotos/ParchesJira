package com.dc.jira.jira.workflow;

import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.issue.CustomFieldManager;
import com.atlassian.jira.issue.IssueManager;
import com.atlassian.jira.issue.context.IssueContext;
import com.atlassian.jira.issue.fields.CustomField;
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
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@SuppressWarnings("unused")
public class MDIAsignacionUsuarioListaFactory extends AbstractWorkflowPluginFactory  implements WorkflowPluginFunctionFactory{

	public static final String USER_ADMIN = "userAdmin";
	
	private static final String FIELD_NAME = "field"; // Campo personalizado seleccionado.En vm es $field
	private static final String FIELDS = "fields"; // Campos personalizados seleccionado.En vm es $fields
	private static final String NOT_DEFINED = "No Definido";

	private ProjectManager projectManager;
	private ProjectRoleManager projectRoleManager;
	private IssueManager issueManager;
	private WorkflowManager workflowManager;
	
	private final CustomFieldManager customFieldManager;

	public MDIAsignacionUsuarioListaFactory(ProjectManager projectManager, IssueManager issueManager, ProjectRoleManager projectRoleManager,
			WorkflowManager workflowManager, CustomFieldManager customFieldManager) {
		this.projectManager = projectManager;
		this.projectRoleManager = projectRoleManager;
		this.issueManager = issueManager;
		this.workflowManager = workflowManager;
		this.customFieldManager = customFieldManager;
	}

	private Collection<CustomField> getCamposPersonalizados() {
		List<CustomField> customFields = customFieldManager.getCustomFieldObjects();
		return customFields;
	}
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	protected void getVelocityParamsForInput(Map<String, Object> velocityParams) {
		Map<String, String[]> myParams = ActionContext.getParameters();
		
		projectManager = ComponentAccessor.getProjectManager();
		Project project = projectManager.getProjectObjByKey("MDI");

		projectRoleManager = ComponentAccessor.getComponentOfType(ProjectRoleManager.class);
		ProjectRole projectRole = projectRoleManager.getProjectRole("Administrators");

		ProjectRoleActors projectRoleActors = projectRoleManager.getProjectRoleActors(projectRole, project);
		Set allUsers = projectRoleActors.getUsers();

		velocityParams.put("allUsers", allUsers);
		
		velocityParams.put(FIELDS, getCamposPersonalizados());
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
		
		String field = (String) functionDescriptor.getArgs().get(FIELD_NAME);
		if (field != null && field.trim().length() > 0)
			velocityParams.put(FIELD_NAME,field);
		else
			velocityParams.put(FIELD_NAME,NOT_DEFINED);
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public Map<String, ?> getDescriptorParams(Map<String, Object> formParams) {
		Map params = new HashMap();

		String userAdmin = extractSingleParam(formParams, "userAdmin");
		params.put("userAdmin", userAdmin);
		
		String field = extractSingleParam(formParams, FIELD_NAME);
		params.put(FIELD_NAME, field);

		return params;
	}
}