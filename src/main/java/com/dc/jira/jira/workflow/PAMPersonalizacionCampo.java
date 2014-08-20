package com.dc.jira.jira.workflow;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.event.type.EventDispatchOption;
import com.atlassian.jira.issue.CustomFieldManager;
import com.atlassian.jira.issue.IssueManager;
import com.atlassian.jira.issue.MutableIssue;
import com.atlassian.jira.issue.fields.CustomField;
import com.atlassian.jira.issue.index.IssueIndexManager;
import com.atlassian.jira.user.UserUtils;
import com.atlassian.jira.util.ImportUtils;
import com.atlassian.jira.workflow.function.issue.AbstractJiraFunctionProvider;
import com.opensymphony.module.propertyset.PropertySet;
import com.opensymphony.workflow.WorkflowException;

/*
 This is the post-function class that gets executed at the end of the transition.
 Any parameters that were saved in your factory class will be available in the transientVars Map.
 */

public class PAMPersonalizacionCampo extends AbstractJiraFunctionProvider {
	private static final Logger log = LoggerFactory.getLogger(PAMPersonalizacionCampo.class);
	public static final String USER_ADMIN = "userAdmin";
	private static final String FIELD_NAME = "field";

	@SuppressWarnings("rawtypes")
	public void execute(Map transientVars, Map args, PropertySet ps) throws WorkflowException {
		MutableIssue issue = getIssue(transientVars);
		IssueManager issueManager = ComponentAccessor.getIssueManager();
		CustomFieldManager customFieldManager = ComponentAccessor.getCustomFieldManager();

		String textojustificacion = "";
		String textoJustificacionFormateado = "";

		String field = (String) args.get(FIELD_NAME);

		CustomField justification = customFieldManager.getCustomFieldObjectByName(field);

		double ingresos = Double.parseDouble(issue.getCustomFieldValue(
				customFieldManager.getCustomFieldObjectByName("Revenues")).toString());

		double margin = Double.parseDouble(issue
				.getCustomFieldValue(customFieldManager.getCustomFieldObjectByName("Margin")).toString()
				.replace("%", "").replace(",", "."));

		if (margin < 45 || ingresos < 5000) {

			if (justification != null) {
				// Check if the custom field value is NULL
				if (issue.getCustomFieldValue(justification) != null) {

					textojustificacion = (String) issue.getCustomFieldValue(justification).toString();

					textoJustificacionFormateado = "{panel:title=|borderStyle=none|borderColor=#ccc|titleBGColor=#F7D6C1|bgColor=#FFFFFF} h3. {color:red} "
							+ textojustificacion + " {color}{panel}";

					issue.setCustomFieldValue(justification, textoJustificacionFormateado);

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
					log.warn("campo Formateado.");
				}
			}
		}
	}
}