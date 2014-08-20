package com.dc.jira.jira.workflow;

import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.event.type.EventDispatchOption;
import com.atlassian.jira.issue.CustomFieldManager;
import com.atlassian.jira.issue.IssueManager;
import com.atlassian.jira.issue.MutableIssue;
import com.atlassian.jira.issue.customfields.option.Option;
import com.atlassian.jira.issue.fields.CustomField;
import com.atlassian.jira.issue.index.IssueIndexManager;
import com.atlassian.jira.issue.watchers.WatcherManager;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.user.UserUtils;
import com.atlassian.jira.user.util.UserManager;
import com.atlassian.jira.util.ImportUtils;
import com.atlassian.jira.workflow.function.issue.AbstractJiraFunctionProvider;
import com.opensymphony.module.propertyset.PropertySet;
import com.opensymphony.workflow.WorkflowException;
import com.opensymphony.workflow.WorkflowContext;

/*
 This is the post-function class that gets executed at the end of the transition.
 Any parameters that were saved in your factory class will be available in the transientVars Map.
 */

@SuppressWarnings("unused")
public class PAMFechasSLAs extends AbstractJiraFunctionProvider {
	private static final Logger log = LoggerFactory.getLogger(PAMFechasSLAs.class);
	public static final String USER_ADMIN = "userAdmin";
	private static final String FIELD_NAME = "field";

	@SuppressWarnings({ "rawtypes" })
	public void execute(Map transientVars, Map args, PropertySet ps) throws WorkflowException {
		MutableIssue issue = getIssue(transientVars);
		IssueManager issueManager = ComponentAccessor.getIssueManager();

		String currentUser = ((WorkflowContext) transientVars.get("context")).getCaller();

		CustomFieldManager customFieldManager = ComponentAccessor.getCustomFieldManager();
		// No es necesario
		String fechaInicial = (String) args.get(FIELD_NAME);
		CustomField fechaSLA = customFieldManager.getCustomFieldObjectByName(fechaInicial);
		Date fecha = (Date) issue.getCustomFieldValue(fechaSLA);

		Calendar fechaforSLA = Calendar.getInstance();
		fechaforSLA.add(Calendar.HOUR, 48);
		Timestamp time = new Timestamp(fechaforSLA.getTimeInMillis());

		issue.setCustomFieldValue(fechaSLA, new Timestamp(fechaforSLA.getTimeInMillis()));

		IssueIndexManager issueIndexManager = ComponentAccessor.getIssueIndexManager();
		ImportUtils.setIndexIssues(false);
		try {
			issueIndexManager.deIndex(issue);
		} catch (Exception ie) {
			log.warn("Failed to deindex issue: " + issue.getKey(), ie);
		}
		try {
			issueManager.updateIssue(UserUtils.getUser((String) args.get("userAdmin")), issue,
					EventDispatchOption.DO_NOT_DISPATCH, false);
		} catch (Exception ie) {
			log.warn("Failed to updateIssue issue: " + issue.getKey(), ie);
		}
		ImportUtils.setIndexIssues(true);
		try {
			issueIndexManager.reIndex(issue);
		} catch (Exception e) {
			log.warn("Failed to reindex issue: " + issue.getKey(), e);
		}
		log.warn("Usuarios Watcher añadidos.");
	}
}