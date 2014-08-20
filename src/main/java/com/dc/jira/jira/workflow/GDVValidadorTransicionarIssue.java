package com.dc.jira.jira.workflow;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import util.DatosBBDD;

import com.atlassian.crowd.embedded.api.User;
import com.atlassian.jira.bc.issue.IssueService;
import com.atlassian.jira.bc.issue.IssueService.IssueResult;
import com.atlassian.jira.bc.issue.IssueService.TransitionValidationResult;
import com.atlassian.jira.bc.project.component.ProjectComponent;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.config.StatusManager;
import com.atlassian.jira.event.type.EventDispatchOption;
import com.atlassian.jira.issue.CustomFieldManager;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.IssueManager;
import com.atlassian.jira.issue.MutableIssue;
import com.atlassian.jira.issue.fields.CustomField;
import com.atlassian.jira.issue.index.IssueIndexManager;
import com.atlassian.jira.issue.status.Status;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.user.DelegatingApplicationUser;
import com.atlassian.jira.user.UserUtils;
import com.atlassian.jira.user.util.UserUtil;
import com.atlassian.jira.util.ImportUtils;
import com.atlassian.jira.workflow.JiraWorkflow;

import com.opensymphony.module.propertyset.PropertySet;
import com.opensymphony.workflow.Validator;
import com.opensymphony.workflow.InvalidInputException;
import com.opensymphony.workflow.loader.ActionDescriptor;
import com.opensymphony.workflow.loader.StepDescriptor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;

@SuppressWarnings("unused")
public class GDVValidadorTransicionarIssue implements Validator {
	private static final Logger log = LoggerFactory.getLogger(GDVValidadorUsuario.class);

	private final CustomFieldManager customFieldManager;

	private static final String FIELD_NAME = "field";

	public GDVValidadorTransicionarIssue(CustomFieldManager customFieldManager) {
		this.customFieldManager = customFieldManager;
	}

	@SuppressWarnings({ "rawtypes", "null" })
	public void validate(Map transientVars, Map args, PropertySet ps) throws InvalidInputException {

		Issue issue = (Issue) transientVars.get("issue");
		Long issue_id = issue.getId();
		IssueManager issueManager = ComponentAccessor.getIssueManager();
		MutableIssue mutableIssue = issueManager.getIssueObject(issue_id);
		CustomFieldManager customFieldManager = ComponentAccessor.getCustomFieldManager();
		List<CustomField> customFields = customFieldManager.getCustomFieldObjects(issue);
		
		UserUtil uu = ComponentAccessor.getUserUtil();
		ArrayList<String>  responsable = new ArrayList<String>();

		String empleadoViajero = null;
		DelegatingApplicationUser viajero = null;
		if (customFields.contains(customFieldManager.getCustomFieldObjectByName("Employee who travels"))) {
			if (customFieldManager.getCustomFieldObjectByName("Employee who travels").getValue(issue) != null) {
				// CustomField solvedBy = customFieldManager.getCustomFieldObjectByName("Employee who travels");
				viajero = (DelegatingApplicationUser) customFieldManager.getCustomFieldObjectByName(
						"Employee who travels").getValue(issue);
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
		responsable = metodos.ObtencionDirector(empleadoViajero);
		metodos = null;

		if (null != responsable) {

			mutableIssue.setAssignee(UserUtils.getUser(responsable.get(0)));

			// Por seguridad hay que rellenar el campo Director para la petición.
			ArrayList<ApplicationUser> userList = new ArrayList<ApplicationUser>();
			CustomField director = ComponentAccessor.getCustomFieldManager().getCustomFieldObjectByName("Director");

			userList.add(uu.getUserByName(responsable.get(0)));
			mutableIssue.setCustomFieldValue(director, userList.get(0));
			// Se puede hacer por postfunction de JIRA Suite Utilities

			IssueIndexManager issueIndexManager = ComponentAccessor.getIssueIndexManager();
			ImportUtils.setIndexIssues(false);
			try {
				issueIndexManager.deIndex(issue);
			} catch (Exception ie) {
				log.warn("Failed to deindex issue: " + issue.getKey(), ie);
			}
			try {
				issueManager.updateIssue(UserUtils.getUser((String) args.get("userAdmin")), mutableIssue,
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
			log.warn("Cambio de usuario realizado.");
		} else { // Si no tiene director le asignamos la petición directamente al COMEX
			String validador = "";
			metodos = new DatosBBDD();
			//validador = metodos.ObtencionValidador(issue.getReporter().getName());
			validador = metodos.ObtencionValidador(empleadoViajero);
			metodos = null;
			mutableIssue.setAssignee(UserUtils.getUser(responsable.get(1)));

			// Por seguridad hay que rellenar el campo Director para la petición.
			ArrayList<ApplicationUser> userList = new ArrayList<ApplicationUser>();
			CustomField manager = ComponentAccessor.getCustomFieldManager().getCustomFieldObjectByName("Manager");

			userList.add(uu.getUserByName(responsable.get(1)));
			mutableIssue.setCustomFieldValue(manager, userList.get(0));
			// Se puede hacer por postfunction de JIRA Suite Utilities

			IssueIndexManager issueIndexManager = ComponentAccessor.getIssueIndexManager();
			ImportUtils.setIndexIssues(false);
			try {
				issueIndexManager.deIndex(issue);
			} catch (Exception ie) {
				log.warn("Failed to deindex issue: " + issue.getKey(), ie);
			}
			try {
				issueManager.updateIssue(UserUtils.getUser((String) args.get("userAdmin")), mutableIssue,
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
			log.warn("Cambio de usuario realizado.");
		}
	}
}
