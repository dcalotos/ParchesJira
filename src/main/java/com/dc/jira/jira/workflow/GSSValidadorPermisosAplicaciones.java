package com.dc.jira.jira.workflow;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import util.DatosBBDD;

import com.atlassian.crowd.embedded.api.User;
import com.atlassian.jira.bc.project.component.ProjectComponent;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.issue.CustomFieldManager;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.IssueManager;
import com.atlassian.jira.issue.MutableIssue;
import com.atlassian.jira.issue.fields.CustomField;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.project.ProjectManager;
import com.atlassian.jira.security.roles.ProjectRole;
import com.atlassian.jira.security.roles.ProjectRoleActors;
import com.atlassian.jira.security.roles.ProjectRoleManager;
import com.atlassian.jira.user.util.UserUtil;
import com.opensymphony.module.propertyset.PropertySet;
import com.opensymphony.workflow.Validator;
import com.opensymphony.workflow.InvalidInputException;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;

@SuppressWarnings("unused")
public class GSSValidadorPermisosAplicaciones implements Validator {
	private static final Logger log = LoggerFactory.getLogger(GSSValidadorPermisosAplicaciones.class);

	private final CustomFieldManager customFieldManager;

	private static final String FIELD_NAME = "field";

	public GSSValidadorPermisosAplicaciones(CustomFieldManager customFieldManager, ProjectManager projectManager) {
		this.customFieldManager = customFieldManager;
	}

	@SuppressWarnings("rawtypes")
	public void validate(Map transientVars, Map args, PropertySet ps) throws InvalidInputException {
		String cfAplicacionesBOSAUX = "";
		Object cfValue = null;
		String aplicacion = "";
		String modulo = "";
		String componente = "";
		String informante = "";
		Boolean permiso = false;

		Issue issue = (Issue) transientVars.get("issue");

		if (issue.getProjectObject().getKey().contains("BACSAUX") || issue.getProjectObject().getKey().contains("BO")) {

			String field = (String) args.get(FIELD_NAME);

			informante = issue.getReporter().getName();

			// UserUtil uu = ComponentAccessor.getUserUtil();
			log.warn("Reporter:" + issue.getReporter().getDisplayName());
			SortedSet<String> gruposBackoffice = ComponentAccessor.getUserUtil().getGroupNamesForUser(informante);
			if (gruposBackoffice.contains("Backoffice") || gruposBackoffice.contains("jira-administrators")) {
				permiso = true;
			} else if (gruposBackoffice.contains("SAUX users")
					&& (issue.getIssueTypeObject().getName().contains("Incidence") || issue.getIssueTypeObject()
							.getName().contains("Technical question"))) {
				permiso = true;
			} else if (field != null) {
				CustomField customField = customFieldManager.getCustomFieldObjectByName(field);
				cfAplicacionesBOSAUX = (String) issue.getCustomFieldValue(customField).toString();
				if (cfAplicacionesBOSAUX.contains(",")) {
					aplicacion = cfAplicacionesBOSAUX.substring(cfAplicacionesBOSAUX.indexOf("=") + 1,
							cfAplicacionesBOSAUX.indexOf(","));
					modulo = cfAplicacionesBOSAUX.substring(cfAplicacionesBOSAUX.indexOf("1=") + 2,
							cfAplicacionesBOSAUX.indexOf("}"));
				} else {
					aplicacion = cfAplicacionesBOSAUX.substring(cfAplicacionesBOSAUX.indexOf("=") + 1,
							cfAplicacionesBOSAUX.indexOf("}"));
				}

				DatosBBDD metodos = new DatosBBDD();
				permiso = metodos.ObtencionBOPermiso(aplicacion, modulo, informante, issue.getIssueTypeObject()
						.getName());
				metodos = null;

				if (!permiso) {

					throw new InvalidInputException("<b>NO TIENE PERMISOS</b> para crear solicitudes de <b>"
							+ issue.getIssueTypeObject().getName() + "</b> sobre la aplicación <b>" + aplicacion
							+ "</b>.<br />" + "Sí los necesita póngase en contacto con el líder del proyecto <b>"
							+ issue.getProjectObject().getProjectLead().getDisplayName() + "</b> .");

				}
			}
		} else if (issue.getProjectObject().getKey().contains("PHOENIX")
				|| issue.getProjectObject().getKey().contains("PRUEPHO")) {
			informante = issue.getReporter().getName();

			log.warn("Reporter:" + issue.getReporter().getDisplayName());
			SortedSet<String> gruposPhoenix = ComponentAccessor.getUserUtil().getGroupNamesForUser(informante);
			if (gruposPhoenix.contains("Phoenix") || gruposPhoenix.contains("jira-administrators")) {
				permiso = true;
			} else if ((gruposPhoenix.contains("Phoenix users") || gruposPhoenix.contains("jira-users"))
					&& (issue.getIssueTypeObject().getName().contains("Incidence") || issue.getIssueTypeObject()
							.getName().contains("Technical question"))) {
				permiso = true;
			} else if (gruposPhoenix.contains("Phoenix Operation users")
					&& issue.getIssueTypeObject().getName().contains("Operation")) {
				permiso = true;
			} else {
				log.warn("componente:" + componente);
				componente = issue.getComponentObjects().iterator().next().getName();
				if (componente != null) {

					DatosBBDD metodos = new DatosBBDD();
					permiso = metodos.ObtencionBOPermiso(componente, componente, informante, issue.getIssueTypeObject()
							.getName());
					metodos = null;

				} else {
					permiso = false;
				}
			}

			if (!permiso) {
				ProjectManager projectManager = ComponentAccessor.getProjectManager();
				Project project = projectManager.getProjectObjByKey("PHOENIX");

				ProjectRoleManager projectRoleManager = ComponentAccessor.getComponentOfType(ProjectRoleManager.class);
				ProjectRole projectRole = projectRoleManager.getProjectRole("Deployer");

				ProjectRoleActors projectRoleActors = projectRoleManager.getProjectRoleActors(projectRole, project);
				Set allUsers = projectRoleActors.getUsers();
				String usuariosDeployer = "";
				for (Iterator it = allUsers.iterator(); it.hasNext();) {
					User usuario = (User) it.next();
					usuariosDeployer += usuario.getDisplayName() + ", ";

				}
				log.warn("usuariosDeployer:" + usuariosDeployer);
				log.warn("IssueType:" + issue.getIssueTypeObject().getName());
				log.warn("componente:" + componente);
				throw new InvalidInputException("<b>NO TIENE PERMISOS</b> para crear solicitudes de <b>"
						+ issue.getIssueTypeObject().getName() + "</b> del componente <b>" + componente + "</b>.<br />"
						+ "Sí los necesita póngase en contacto con los responsables de permisos de PHOENIX. <b>"
						+ usuariosDeployer + "</b> .");
			}
		}
		/*
		 * if (issue.dueDate) { if (issue.dueDate.before(Calendar.getInstance().getTime())) { invalidInputException =
		 * new InvalidInputException("Due date must be in the future.") } }
		 */

	}
}
