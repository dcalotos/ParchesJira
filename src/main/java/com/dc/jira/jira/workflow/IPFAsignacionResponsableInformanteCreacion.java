package com.dc.jira.jira.workflow;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import util.DatosBBDD;

import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.event.type.EventDispatchOption;
import com.atlassian.jira.issue.CustomFieldManager;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.IssueManager;
import com.atlassian.jira.user.UserUtils;
import com.atlassian.jira.user.util.UserUtil;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.util.ImportUtils;
import com.atlassian.jira.workflow.function.issue.AbstractJiraFunctionProvider;
import com.opensymphony.module.propertyset.PropertySet;
import com.opensymphony.workflow.WorkflowException;

import java.util.List;
import java.util.Map;
import java.util.SortedSet;

import com.atlassian.jira.issue.MutableIssue;
import com.atlassian.jira.issue.fields.CustomField;
import com.atlassian.jira.issue.index.IssueIndexManager;

@SuppressWarnings("unused")
public class IPFAsignacionResponsableInformanteCreacion extends AbstractJiraFunctionProvider {
	private static final Logger log = LoggerFactory.getLogger(IPFAsignacionResponsableInformanteCreacion.class);
	private String userAdmin;

	@SuppressWarnings("rawtypes")
	public void execute(Map transientVars, Map args, PropertySet ps) throws WorkflowException {
		MutableIssue issue = getIssue(transientVars);

		setUserAdmin((String) args.get("userAdmin"));

		String UdNproducto = "";
		String UdN = "";
		String producto = "";
		String problema = "";
		String managerUser = "";
		String responsable = "";

		UserUtil uu = ComponentAccessor.getUserUtil();

		IssueManager issueManager = ComponentAccessor.getIssueManager();

		SortedSet<String> grupos = ComponentAccessor.getUserUtil().getGroupNamesForUser(issue.getReporter().getName());
		CustomFieldManager customFieldManager = ComponentAccessor.getCustomFieldManager();
		List<CustomField> customFields = customFieldManager.getCustomFieldObjects(issue);

		DatosBBDD metodos = new DatosBBDD();

		UdNproducto = customFieldManager.getCustomFieldObjectByName("Produits On line France").getValue(issue)
				.toString();
		if (UdNproducto != "") {
			producto = UdNproducto.substring(UdNproducto.indexOf("1=") + 2, UdNproducto.indexOf("}"));
			managerUser = metodos.ObtencionManagerProductoOnline(producto);
		}

		if (grupos.contains("Call one")) {
			problema = customFieldManager.getCustomFieldObjectByName("Type de problème").getValue(issue).toString();
			if (problema.contains("Site indisponible: Impossible d'accéder au produit")
					|| problema.contains("Authentification: problème de connexion (utilisateur non reconnu)")) {
				managerUser = metodos.ObtencionManagerTipoProblemaFrancia(problema);
			}
			// El reporter es el mismo que el asignada para las tareas del Grupo Call One
			responsable = managerUser;
			issue.setAssignee(UserUtils.getUser(responsable));
			issue.setReporter(UserUtils.getUser(responsable));
			log.warn("Usuario asignado." + responsable);

			CustomField callOne = ComponentAccessor.getCustomFieldManager().getCustomFieldObjectByName("Call one");
			// Para poder hacer un filtro de las tareas creadas por Call one
			issue.setCustomFieldValue(callOne, "1");
			log.warn("callOne:\t" + "1");
		} else {
			UdN = UdNproducto.substring(0, UdNproducto.indexOf("1=") - 2);
			if (!UdN.contains("WK France NAW")) {
				responsable = metodos.ObtencionResponsableUdNFrancia(producto);
				issue.setAssignee(UserUtils.getUser(responsable));
			}
		}

		metodos = null;

		CustomField manager = ComponentAccessor.getCustomFieldManager().getCustomFieldObjectByName("Manager");
		// issue.setCustomFieldValue(manager, UserUtils.getUser(managerUser));
		issue.setCustomFieldValue(manager, uu.getUserByName(managerUser));
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
			log.warn("Failed to update issue: " + issue.getKey(), ie);
		}
		// Método obsoleto
		// issue.store();
		ImportUtils.setIndexIssues(true);
		try {
			issueIndexManager.reIndex(issue);
		} catch (Exception e) {
			log.warn("Failed to reindex issue: " + issue.getKey(), e);
		}
		log.warn("Cambio de usuario realizado.");
	}

	public String getUserAdmin() {
		return userAdmin;
	}

	public void setUserAdmin(String userAdmin) {
		this.userAdmin = userAdmin;
	}
}