package com.dc.jira.jira.workflow;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import util.DatosBBDD;

import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.event.type.EventDispatchOption;
import com.atlassian.jira.issue.CustomFieldManager;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.IssueManager;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.user.DelegatingApplicationUser;
import com.atlassian.jira.user.UserUtils;
import com.atlassian.jira.user.util.UserManager;
import com.atlassian.jira.user.util.UserUtil;
import com.atlassian.jira.util.ImportUtils;
import com.atlassian.jira.workflow.function.issue.AbstractJiraFunctionProvider;
import com.opensymphony.module.propertyset.PropertySet;
import com.opensymphony.workflow.InvalidInputException;
import com.opensymphony.workflow.WorkflowException;

import java.util.List;
import java.util.Map;
import java.util.SortedSet;

import com.atlassian.jira.issue.MutableIssue;
import com.atlassian.jira.issue.fields.CustomField;
import com.atlassian.jira.issue.index.IssueIndexManager;


@SuppressWarnings("unused")
public class GDVAsignacionValidador extends AbstractJiraFunctionProvider {
	private static final Logger log = LoggerFactory.getLogger(GDVAsignacionValidador.class);
	public static final String USER_ADMIN = "userAdmin";

	@SuppressWarnings({ "rawtypes" })
	public void execute(Map transientVars, Map args, PropertySet ps) throws WorkflowException {

		MutableIssue issue = getIssue(transientVars);
		IssueManager issueManager = ComponentAccessor.getIssueManager();
		UserUtil uu = ComponentAccessor.getUserUtil();
		String responsable = "";
		log.warn("Reporter:" + issue.getReporter().getDisplayName());
		SortedSet<String> grupos = ComponentAccessor.getUserUtil().getGroupNamesForUser(issue.getReporter().getName());

		CustomFieldManager customFieldManager = ComponentAccessor.getCustomFieldManager();
		List<CustomField> customFields = customFieldManager.getCustomFieldObjects(issue);

		String empleadoViajero = null;
		DelegatingApplicationUser viajero = null;
		if (customFields.contains(customFieldManager.getCustomFieldObjectByName("Employee who travels"))) {
			if (customFieldManager.getCustomFieldObjectByName("Employee who travels").getValue(issue) != null) {
				//CustomField solvedBy = customFieldManager.getCustomFieldObjectByName("Employee who travels");
				viajero = (DelegatingApplicationUser) customFieldManager.getCustomFieldObjectByName("Employee who travels").getValue(issue);
				empleadoViajero = viajero.getUsername();
			} else {
				empleadoViajero = UserUtils.getUser((String) args.get("userAdmin")).getName();
				log.warn("Field Employee who travels contains null value.");
			}
		} else {
			empleadoViajero = UserUtils.getUser((String) args.get("userAdmin")).getName();
			log.warn("CustomField Employee who travels not exists.");
		}
		log.warn("Employee who travels: [" + empleadoViajero + "]");

		DatosBBDD metodos = new DatosBBDD();
		responsable = metodos.ObtencionValidador(empleadoViajero);
		metodos = null;

		if (responsable.length() > 0) {

			issue.setAssignee(UserUtils.getUser(responsable));
			// issue.setAssignee(uu.getUserByName(responsable).getDirectoryUser());
			// issue.setAssignee(uu.getUser(responsable));
			IssueIndexManager issueIndexManager = ComponentAccessor.getIssueIndexManager();
			ImportUtils.setIndexIssues(false);
			try {
				issueIndexManager.deIndex(issue);
			} catch (Exception ie) {
				log.warn("Failed to deindex issue: " + issue.getKey(), ie);
			}
			try {
				issueManager.updateIssue(UserUtils.getUser((String) args.get("userAdmin")), issue, EventDispatchOption.DO_NOT_DISPATCH, false);
				// issueManager.updateIssue(uu.getUser((String)
				// args.get("userAdmin")), issue,
				// EventDispatchOption.DO_NOT_DISPATCH, false);
				// issueManager.updateIssue(uu.getUserByName((String)
				// args.get("userAdmin")).getDirectoryUser(), issue,
				// EventDispatchOption.DO_NOT_DISPATCH, false);
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
		} else {
			throw new InvalidInputException("<b>NO DISPONE DE UN RESPONSABLE DE APROBACIÓN.</b><br />"
					+ "Sí lo necesita póngase en contacto con Nuria Fernández Suárez.");
		}

	}
}
