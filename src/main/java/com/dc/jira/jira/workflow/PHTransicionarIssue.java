package com.dc.jira.jira.workflow;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.atlassian.crowd.embedded.api.User;
import com.atlassian.jira.bc.issue.IssueService;
import com.atlassian.jira.bc.issue.IssueService.IssueResult;
import com.atlassian.jira.bc.issue.IssueService.TransitionValidationResult;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.config.StatusManager;
import com.atlassian.jira.event.type.EventDispatchOption;
import com.atlassian.jira.issue.CustomFieldManager;
import com.atlassian.jira.issue.IssueManager;
import com.atlassian.jira.issue.MutableIssue;
import com.atlassian.jira.issue.fields.CustomField;
import com.atlassian.jira.issue.index.IssueIndexManager;
import com.atlassian.jira.issue.status.Status;
import com.atlassian.jira.security.JiraAuthenticationContext;
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
public class PHTransicionarIssue extends AbstractJiraFunctionProvider {
	private static final Logger log = LoggerFactory.getLogger(PHTransicionarIssue.class);

	private final JiraAuthenticationContext authenticationContext;

	public PHTransicionarIssue(JiraAuthenticationContext authenticationContext) {
		this.authenticationContext = authenticationContext;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void execute(Map transientVars, Map args, PropertySet ps) throws WorkflowException {
		boolean proyectoTransicionable = false;
		String horas = "";
		MutableIssue issue = getIssue(transientVars);
		String project = issue.getProjectObject().getKey();
		log.warn("proyecto JIRA: " + project);
		String projectLead = issue.getProjectObject().getLeadUserName();
		log.warn("Project Lead: " + projectLead);
		String userAdmin = (String) args.get("userAdmin");
		log.warn("userAdmin: " + userAdmin);
		String proHoras = (String) args.get("ProyectoHoras");
		log.warn("ProyectoHoras: " + proHoras);

		String[] proyectos = proHoras.split(",");

		for (String datosProyecto : proyectos) {
			String proyecto = datosProyecto.substring(0, datosProyecto.indexOf("-"));
			if (project.contains(proyecto)) {
				log.warn("proyecto: " + proyecto);
				horas = datosProyecto.substring(datosProyecto.indexOf("-") + 1, datosProyecto.length());
				log.warn("horas: " + horas);
				proyectoTransicionable = true;
				log.warn("proyectoTransicionable: " + proyectoTransicionable);
				break;
			}
		}

		if (proyectoTransicionable) {
			// String horas = proHoras.substring(proHoras.indexOf("-") + 1, proHoras.length());

			CustomFieldManager customFieldManager = ComponentAccessor.getCustomFieldManager();
			List<CustomField> customFields = customFieldManager.getCustomFieldObjects(issue);

			String esfuerzo = "0";

			// esfuerzo = customFieldManager.getCustomFieldObject(Long.parseLong("10000")).getValue(issue).toString();
			// esfuerzo = customFieldManager.getCustomFieldObject(Long.parseLong("11461")).getValue(issue).toString();

			if (customFields.contains(customFieldManager.getCustomFieldObjectByName("Effort [Hours]"))) {
				if (customFieldManager.getCustomFieldObjectByName("Effort [Hours]").getValue(issue) != null) {
					esfuerzo = customFieldManager.getCustomFieldObjectByName("Effort [Hours]").getValue(issue).toString();
					log.warn("esfuerzo: " + esfuerzo);
				} else {
					esfuerzo = "0";
					log.warn("Field Effort [Hours] contains null value.");
				}
			} else {
				esfuerzo = "0";
				log.warn("CustomField Effort [Hours] not exists.");
			}
			log.warn("esfuerzo: [" + esfuerzo + "]");

			if ((Double.parseDouble(esfuerzo) <= Integer.parseInt(horas))) {
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
					if (actionDescriptor.getName().contains("Minor Effort")) {
						actionId = actionDescriptor.getId();
						log.warn("actionId a utilizar: [" + actionId + "]");
						break;
					}
				}

				// Recomiendan que se haga con IssueService y no con IssueManager y WorkflowManager
				IssueService issueService = ComponentAccessor.getIssueService();
				TransitionValidationResult transitionValidationResult = issueService.validateTransition(UserUtils.getUser((String) args.get("userAdmin")),
						issue.getId(), actionId, issueService.newIssueInputParameters());
				// TransitionValidationResult transitionValidationResult = issueService.validateTransition(authenticationContext.getLoggedInUser(),
				// issue.getId(), actionId, issueService.newIssueInputParameters());
				if (transitionValidationResult.isValid()) {
					IssueResult transitionResult = issueService.transition(UserUtils.getUser((String) args.get("userAdmin")), transitionValidationResult);
					
					// IssueResult transitionResult = issueService.transition(authenticationContext.getLoggedInUser(), transitionValidationResult);
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
							if (estado.getName().contains("Assigned")) {
								log.warn("estado.getName(): [" + estado.getName() + "] + estado.getId(): [" + estado.getId() + "]");
								estadoFinal = estado;
								break;
							}
						}
						issue.setStatusObject(estadoFinal);

						String asignadoFinal = null;
						DelegatingApplicationUser assign = null;
						if (customFields.contains(customFieldManager.getCustomFieldObjectByName("Solved by"))) {
							if (customFieldManager.getCustomFieldObjectByName("Solved by").getValue(issue) != null) {
								CustomField solvedBy = customFieldManager.getCustomFieldObjectByName("Solved by");
								assign = (DelegatingApplicationUser) customFieldManager.getCustomFieldObjectByName("Solved by").getValue(issue);
								asignadoFinal = assign.getUsername();
							} else {
								log.warn("Field Solved by contains null value. Assignee Project Lead: " + projectLead);
								asignadoFinal = UserUtils.getUser(projectLead).getName();
								//asignadoFinal = UserUtils.getUser((String) args.get("userAdmin")).getName();
								log.warn("Field Solved by contains null value.");
							}
						} else {
							log.warn("Field Solved by contains null value. Assignee Project Lead: " + projectLead);
							asignadoFinal = UserUtils.getUser(projectLead).getName();
							//asignadoFinal = UserUtils.getUser((String) args.get("userAdmin")).getName();
							log.warn("CustomField Solved by not exists.");
						}
						log.warn("Solved by: [" + asignadoFinal + "]");
						log.warn("Project Lead: " + projectLead);
						issue.setAssignee(UserUtils.getUser(asignadoFinal));
						// issue.setAssignee(UserUtils.getUser(asignadoFinal));

						IssueManager issueManager = ComponentAccessor.getIssueManager();
						IssueIndexManager issueIndexManager = ComponentAccessor.getIssueIndexManager();
						ImportUtils.setIndexIssues(false);
						try {
							issueIndexManager.deIndex(issue);
						} catch (Exception ie) {
							log.warn("Failed to deindex issue: " + issue.getKey(), ie);
						}
						//issueManager.updateIssue(UserUtils.getUser((String) args.get(projectLead)), issue, EventDispatchOption.DO_NOT_DISPATCH, false);
						issueManager.updateIssue(UserUtils.getUser((String) args.get("userAdmin")), issue, EventDispatchOption.DO_NOT_DISPATCH, false);
						ImportUtils.setIndexIssues(true);
						try {
							issueIndexManager.reIndex(issue);
						} catch (Exception e) {
							log.warn("Failed to reindex issue: " + issue.getKey(), e);
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
}