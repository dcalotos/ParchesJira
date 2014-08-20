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

import java.util.Calendar;
import java.util.Date;
import java.util.Formatter;
import java.util.Map;
import java.util.SortedSet;

import com.atlassian.jira.issue.MutableIssue;
import com.atlassian.jira.issue.fields.CustomField;
import com.atlassian.jira.issue.index.IssueIndexManager;

@SuppressWarnings("unused")
public class GDVGeneradorCodigo extends AbstractJiraFunctionProvider {
	private static final Logger log = LoggerFactory.getLogger(GDVGeneradorCodigo.class);
	public static final String USER_ADMIN = "userAdmin";

	@SuppressWarnings({ "rawtypes" })
	public void execute(Map transientVars, Map args, PropertySet ps) throws WorkflowException {

		MutableIssue issue = getIssue(transientVars);
		IssueManager issueManager = ComponentAccessor.getIssueManager();
		UserUtil uu = ComponentAccessor.getUserUtil();

		String travelCode=Integer.toString(Calendar.getInstance().get(Calendar.YEAR))+"-"+new Formatter().format("%05d",  0 + (int)(Math.random()*99999)).toString();
		
		CustomField travelCodeJIRA = ComponentAccessor.getCustomFieldManager().getCustomFieldObjectByName("Codigo autorizacion");
		issue.setCustomFieldValue(travelCodeJIRA, travelCode);
		
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
		log.warn("Código generado.");
	}
}
