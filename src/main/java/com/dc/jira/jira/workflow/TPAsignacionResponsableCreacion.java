package com.dc.jira.jira.workflow;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.event.type.EventDispatchOption;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.IssueManager;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.user.UserUtils;
import com.atlassian.jira.user.util.UserManager;
import com.atlassian.jira.user.util.UserUtil;
import com.atlassian.jira.util.ImportUtils;
import com.atlassian.jira.workflow.function.issue.AbstractJiraFunctionProvider;
import com.opensymphony.module.propertyset.PropertySet;
import com.opensymphony.workflow.WorkflowException;
import java.util.Map;
import java.util.SortedSet;

import com.atlassian.jira.issue.MutableIssue;
import com.atlassian.jira.issue.fields.CustomField;
import com.atlassian.jira.issue.index.IssueIndexManager;

@SuppressWarnings("unused")
public class TPAsignacionResponsableCreacion extends AbstractJiraFunctionProvider {
	private static final Logger log = LoggerFactory.getLogger(TPAsignacionResponsableCreacion.class);
	public static final String USER_ADMIN = "userAdmin";

	@SuppressWarnings({ "rawtypes" })
	public void execute(Map transientVars, Map args, PropertySet ps) throws WorkflowException {

		MutableIssue issue = getIssue(transientVars);
		IssueManager issueManager = ComponentAccessor.getIssueManager();
		UserUtil uu = ComponentAccessor.getUserUtil();

		log.warn("Reporter:" + issue.getReporter().getDisplayName());
		SortedSet<String> grupos = ComponentAccessor.getUserUtil().getGroupNamesForUser(issue.getReporter().getName());

		if (grupos.contains("Análisis Documental")) {
			String responsable = "jlimas";
			issue.setAssignee(UserUtils.getUser(responsable));
			//issue.setAssignee(uu.getUserByName(responsable).getDirectoryUser());
			//issue.setAssignee(uu.getUser(responsable));
			IssueIndexManager issueIndexManager = ComponentAccessor.getIssueIndexManager();
			ImportUtils.setIndexIssues(false);
			try {
				issueIndexManager.deIndex(issue);
			} catch (Exception ie) {
				log.warn("Failed to deindex issue: " + issue.getKey(), ie);
			}
			try {
				issueManager.updateIssue(UserUtils.getUser((String) args.get("userAdmin")), issue, EventDispatchOption.DO_NOT_DISPATCH, false);
				//issueManager.updateIssue(uu.getUser((String) args.get("userAdmin")), issue, EventDispatchOption.DO_NOT_DISPATCH, false);
				//issueManager.updateIssue(uu.getUserByName((String) args.get("userAdmin")).getDirectoryUser(), issue, EventDispatchOption.DO_NOT_DISPATCH, false);
			} catch (Exception ie) {
				log.warn("Failed to updateIssue issue: " + issue.getKey(), ie);
			}
			ImportUtils.setIndexIssues(true);
			try {
				issueIndexManager.reIndex(issue);
			} catch (Exception e) {
				log.warn("Failed to reindex issue: " + issue.getKey(), e);
			}
			log.warn("Cambio de usuario realizado.");
		}
	}
}
