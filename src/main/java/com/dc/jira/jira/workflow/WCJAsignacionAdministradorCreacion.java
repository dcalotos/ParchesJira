package com.dc.jira.jira.workflow;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import util.DatosBBDD;

import com.atlassian.crowd.embedded.api.User;
import com.atlassian.jira.bc.project.component.ProjectComponent;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.event.type.EventDispatchOption;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.IssueManager;
import com.atlassian.jira.user.UserUtils;
import com.atlassian.jira.user.util.UserUtil;
import com.atlassian.jira.util.ImportUtils;
import com.atlassian.jira.workflow.function.issue.AbstractJiraFunctionProvider;
import com.opensymphony.module.propertyset.PropertySet;
import com.opensymphony.workflow.WorkflowException;
import java.util.Map;
import com.atlassian.jira.issue.MutableIssue;
import com.atlassian.jira.issue.index.IssueIndexManager;


@SuppressWarnings("unused")
public class WCJAsignacionAdministradorCreacion extends AbstractJiraFunctionProvider {
	private static final Logger log = LoggerFactory.getLogger(WCJAsignacionAdministradorCreacion.class);
	private String userAdmin;

	@SuppressWarnings("rawtypes")
	public void execute(Map transientVars, Map args, PropertySet ps) throws WorkflowException {
		MutableIssue issue = getIssue(transientVars);
		setUserAdmin((String) args.get("userAdmin"));

		String responsable = "";
		IssueManager issueManager = ComponentAccessor.getIssueManager();
		String issueType = issue.getIssueTypeObject().getName();

		if (issueType != null) {
			DatosBBDD metodos = new DatosBBDD();
			responsable = metodos.ObtencionUsuarioAdministradorConsultas(issueType);
			metodos = null;
		}
		issue.setAssignee(UserUtils.getUser(responsable));
		issue.setReporter(UserUtils.getUser(responsable));
		log.warn("Usuario asignado." + responsable);

		IssueIndexManager issueIndexManager = ComponentAccessor.getIssueIndexManager();
		ImportUtils.setIndexIssues(false);
		try {
			issueIndexManager.deIndex(issue);
		} catch (Exception ie) {
			log.warn("Failed to deindex issue: " + issue.getKey(), ie);
		}
		try {
			issueManager.updateIssue(UserUtils.getUser((String) args.get("userAdmin")), issue, EventDispatchOption.DO_NOT_DISPATCH, false);
		} catch (Exception ie) {
			log.warn("Failed to updateIssue issue: " + issue.getKey(), ie);
		}
		ImportUtils.setIndexIssues(true);
		try {
			issueIndexManager.reIndex(issue);
		} catch (Exception e) {
			log.warn("Failed to reindex issue: " + issue.getKey(), e);
		}
	}

	public String getUserAdmin() {
		return userAdmin;
	}

	public void setUserAdmin(String userAdmin) {
		this.userAdmin = userAdmin;
	}
}