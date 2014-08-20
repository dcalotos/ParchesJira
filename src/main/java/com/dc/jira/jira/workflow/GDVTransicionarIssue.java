package com.dc.jira.jira.workflow;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import util.DatosBBDD;

import com.atlassian.crowd.embedded.api.User;
import com.atlassian.jira.bc.issue.IssueService;
import com.atlassian.jira.bc.issue.IssueService.IssueResult;
import com.atlassian.jira.bc.issue.IssueService.TransitionValidationResult;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.config.StatusManager;
import com.atlassian.jira.event.type.EventDispatchOption;
import com.atlassian.jira.event.type.EventType;
import com.atlassian.jira.issue.CustomFieldManager;
import com.atlassian.jira.issue.IssueManager;
import com.atlassian.jira.issue.MutableIssue;
import com.atlassian.jira.issue.fields.CustomField;
import com.atlassian.jira.issue.index.IssueIndexManager;
import com.atlassian.jira.issue.status.Status;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.user.DelegatingApplicationUser;
import com.atlassian.jira.user.UserUtils;
import com.atlassian.jira.user.util.UserUtil;
import com.atlassian.jira.util.ImportUtils;
import com.atlassian.jira.workflow.JiraWorkflow;
import com.atlassian.jira.workflow.function.issue.AbstractJiraFunctionProvider;
import com.opensymphony.module.propertyset.PropertySet;
import com.opensymphony.workflow.WorkflowException;
import com.opensymphony.workflow.loader.ActionDescriptor;
import com.opensymphony.workflow.loader.StepDescriptor;

@SuppressWarnings("unused")
public class GDVTransicionarIssue extends AbstractJiraFunctionProvider {
	private static final Logger log = LoggerFactory.getLogger(GDVTransicionarIssue.class);

	private final JiraAuthenticationContext authenticationContext;

	public GDVTransicionarIssue(JiraAuthenticationContext authenticationContext) {
		this.authenticationContext = authenticationContext;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void execute(Map transientVars, Map args, PropertySet ps) throws WorkflowException {
		boolean proyectoTransicionable = false;
		String horas = "";
		MutableIssue issue = getIssue(transientVars);
		UserUtil uu = ComponentAccessor.getUserUtil();
		String responsable = "";
		
		String userAdmin = (String) args.get("userAdmin");
		log.warn("userAdmin: " + userAdmin);

		CustomFieldManager customFieldManager = ComponentAccessor.getCustomFieldManager();
		List<CustomField> customFields = customFieldManager.getCustomFieldObjects(issue);
		DelegatingApplicationUser director = null;
		// Si el campo Director está a nulo la pasamos directamente a Asignada
		try{
			director = (DelegatingApplicationUser) customFieldManager.getCustomFieldObjectByName("Director]").getValue(issue);
		}catch (Exception e){
			JiraWorkflow workFlow = ComponentAccessor.getWorkflowManager().getWorkflow(issue);
			Status estadoActual = issue.getStatusObject();
			log.warn("estadoActual: [" + estadoActual.getName() + "]");
			StepDescriptor pasoActual = workFlow.getLinkedStep(estadoActual);
			log.warn("pasoActual: [" + pasoActual.getName() + "]");
			List<ActionDescriptor> actions = pasoActual.getActions();
			int actionId = 0;
			for (ActionDescriptor actionDescriptor : actions) {
				log.warn("actionDescriptor.getName(): [" + actionDescriptor.getName() + "]");
				// if (actionDescriptor.getName().equals("Resolve Issue")) {
				if (actionDescriptor.getName().contains("Aprobacion COMEX")) {
					actionId = actionDescriptor.getId();
					log.warn("actionId a utilizar: [" + actionId + "]");
					break;
				}
			}

			// Recomiendan que se haga con IssueService y no con IssueManager y WorkflowManager
			IssueService issueService = ComponentAccessor.getIssueService();
			TransitionValidationResult transitionValidationResult = issueService.validateTransition(
					UserUtils.getUser((String) args.get("userAdmin")), issue.getId(), actionId,
					issueService.newIssueInputParameters());
			// TransitionValidationResult transitionValidationResult =
			// issueService.validateTransition(authenticationContext.getLoggedInUser(),
			// issue.getId(), actionId, issueService.newIssueInputParameters());
			
			if (transitionValidationResult.isValid()) {
				IssueResult transitionResult = issueService.transition(
						UserUtils.getUser((String) args.get("userAdmin")), transitionValidationResult);

				// IssueResult transitionResult = issueService.transition(authenticationContext.getLoggedInUser(),
				// transitionValidationResult);
				if (transitionResult.isValid()) {
					log.warn("Transición realizada");
					Status estadoFinal = null;
					estadoFinal = issue.getStatusObject();
					log.warn("estadoFinal: [" + estadoFinal.getName() + "]");
					// Modificar los valores que tendría que tener la issue en el nuevo estado
					StatusManager statusManager = ComponentAccessor.getComponentOfType(StatusManager.class);
					// Status estadoFinal=statusManager.getStatus("Assigned");
					Collection<Status> estados = statusManager.getStatuses();

					for (Status estado : estados) {
						log.warn("estado: [" + estado.getName() + "]. Descripción: [" + estado.getDescription() + "]");
						// if (estado.getName().equals("Resolved")) {
						if (estado.getName().contains("Asignada")) { // No sé si será Asignada en español
							log.warn("estado.getName(): [" + estado.getName() + "] + estado.getId(): ["
									+ estado.getId() + "]");
							estadoFinal = estado;
							break;
						}
					}
					issue.setStatusObject(estadoFinal);
					String empleadoViajero = null;
					DelegatingApplicationUser viajero = null;
					if (customFields.contains(customFieldManager.getCustomFieldObjectByName("Employee who travels"))) {
						if (customFieldManager.getCustomFieldObjectByName("Employee who travels").getValue(issue) != null) {
							// CustomField solvedBy =
							// customFieldManager.getCustomFieldObjectByName("Employee who travels");
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
					responsable = metodos.ObtencionValidador(empleadoViajero);
					metodos = null;

					// Por seguridad hay que rellenar el campo Manager para la petición.
					ArrayList<ApplicationUser> userList = new ArrayList<ApplicationUser>();
					CustomField manager = ComponentAccessor.getCustomFieldManager().getCustomFieldObjectByName(
							"Manager");
					userList.add(uu.getUserByName(responsable));
					issue.setCustomFieldValue(manager, userList.get(0));
					// Se puede hacer por postfunction de JIRA Suite Utilities. No se si esto se ejecuta al hacer la
					// trasición automática.

					IssueManager issueManager = ComponentAccessor.getIssueManager();
					IssueIndexManager issueIndexManager = ComponentAccessor.getIssueIndexManager();
					ImportUtils.setIndexIssues(false);
					try {
						issueIndexManager.deIndex(issue);
					} catch (Exception ie) {
						log.warn("Failed to deindex issue: " + issue.getKey(), ie);
					}
					// issueManager.updateIssue(UserUtils.getUser((String) args.get(projectLead)), issue,
					// EventDispatchOption.DO_NOT_DISPATCH, false);
					issueManager.updateIssue(UserUtils.getUser((String) args.get("userAdmin")), issue,
							EventDispatchOption.DO_NOT_DISPATCH, false);
					ImportUtils.setIndexIssues(true);
					try {
						issueIndexManager.reIndex(issue);
					} catch (Exception ex) {
						log.warn("Failed to reindex issue: " + issue.getKey(), ex);
					}
				} else {
					log.warn("Transición NO realizada");
				}
			} else {
				List<String> errorList = new ArrayList();
				errorList.addAll(transitionValidationResult.getErrorCollection().getErrorMessages());
				for (int i = 0; errorList != null && i < errorList.size(); i++) {
					log.warn("El error es :" + errorList.get(i));
				}
			}
		}
	}
}
