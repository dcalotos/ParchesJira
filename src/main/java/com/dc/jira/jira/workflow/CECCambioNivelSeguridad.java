package com.dc.jira.jira.workflow;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;

import com.atlassian.jira.issue.MutableIssue;
import com.atlassian.jira.issue.index.IssueIndexManager;
import com.atlassian.jira.issue.security.IssueSecurityLevel;
import com.atlassian.jira.issue.security.IssueSecurityLevelManager;

@SuppressWarnings("unused")
public class CECCambioNivelSeguridad extends AbstractJiraFunctionProvider {
	private static final Logger log = LoggerFactory.getLogger(CECCambioNivelSeguridad.class);
	public static final String ADMIN_USER = "adminUser";

	@SuppressWarnings("rawtypes")
	public void execute(Map transientVars, Map args, PropertySet ps) throws WorkflowException {
		MutableIssue issue = getIssue(transientVars);
		IssueManager issueManager = ComponentAccessor.getIssueManager();
		String adminUser = (String) args.get("adminUser");

		SortedSet<String> grupos = ComponentAccessor.getUserUtil().getGroupNamesForUser(issue.getAssignee().getName());

		if (grupos.contains("CJ Especialista Externo EC")) {
			IssueSecurityLevelManager issueSecurityLevelManager = ComponentAccessor.getIssueSecurityLevelManager();
			List issueSecurityLevels = issueSecurityLevelManager.getUsersSecurityLevels(issue.getProjectObject(), issue.getAssignee());
			Iterator it = issueSecurityLevels.listIterator();
			while (it.hasNext()) {
				IssueSecurityLevel issueSecurityLevel = (IssueSecurityLevel) it.next();
				if (issueSecurityLevel.getName().contains("NivSeg CJ Externas EC")) {
					issue.setSecurityLevelId(issueSecurityLevel.getId());
					break;
				}
			}
			IssueIndexManager issueIndexManager = ComponentAccessor.getIssueIndexManager();
			ImportUtils.setIndexIssues(false);
			try {
				issueIndexManager.deIndex(issue);
			} catch (Exception ie) {
				log.warn("Failed to deindex issue: " + issue.getKey(), ie);
			}

			issueManager.updateIssue(UserUtils.getUser((String) args.get("adminUser")), issue, EventDispatchOption.DO_NOT_DISPATCH, false);

			ImportUtils.setIndexIssues(true);
			try {
				issueIndexManager.reIndex(issue);
			} catch (Exception e) {
				log.warn("Failed to reindex issue: " + issue.getKey(), e);
			}
		}
	}
}