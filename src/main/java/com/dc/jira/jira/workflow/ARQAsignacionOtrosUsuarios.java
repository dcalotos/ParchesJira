package com.dc.jira.jira.workflow;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import util.DatosBBDD;

import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.bc.project.component.ProjectComponent;
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import com.atlassian.jira.issue.MutableIssue;
import com.atlassian.jira.issue.fields.CustomField;
import com.atlassian.jira.issue.index.IssueIndexManager;


@SuppressWarnings("unused")
public class ARQAsignacionOtrosUsuarios extends AbstractJiraFunctionProvider {
	private static final Logger log = LoggerFactory.getLogger(ARQAsignacionOtrosUsuarios.class);

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public void execute(Map transientVars, Map args, PropertySet ps) throws WorkflowException {

		MutableIssue issue = getIssue(transientVars);
		IssueManager issueManager = ComponentAccessor.getIssueManager();
		String userAdmin = (String) args.get("userAdmin");
		String proyecto = issue.getProjectObject().getKey();
		
		UserUtil uu=ComponentAccessor.getUserUtil();

		boolean actualizar = false;
		String usuarioBackup = "";

		Collection projectComponents = issue.getComponentObjects();
		Iterator iterator = projectComponents.iterator();
		if (iterator.hasNext()) {
			DatosBBDD metodos = new DatosBBDD();
			ProjectComponent projectComponent = (ProjectComponent) iterator.next();
			String componente = projectComponent.getName();
			usuarioBackup = metodos.ObtencionUsuarioBackupArquitectura(componente);
			metodos = null;
		} else {
			usuarioBackup = "apascual";
		}

		ArrayList<ApplicationUser> userList = new ArrayList<ApplicationUser>();
		CustomField otrosUsuariosInteresados = ComponentAccessor.getCustomFieldManager().getCustomFieldObjectByName("Otros usuarios interesados");
		userList = (ArrayList<ApplicationUser>) otrosUsuariosInteresados.getValue(issue);
		if (userList == null && usuarioBackup != "") {
			userList = new ArrayList<ApplicationUser>();
			actualizar = true;
		} else if (!userList.contains(UserUtils.getUser(usuarioBackup)) && usuarioBackup != "") {
			actualizar = true;
		}
		if (actualizar == true) {
			//userList.add(UserUtils.getUser(usuarioBackup));
			userList.add(uu.getUserByName(usuarioBackup));
			issue.setCustomFieldValue(otrosUsuariosInteresados, userList);
			IssueIndexManager issueIndexManager = ComponentAccessor.getIssueIndexManager();
			ImportUtils.setIndexIssues(false);
			try {
				issueIndexManager.deIndex(issue);
			} catch (Exception ie) {
				log.warn("Failed to deindex issue: " + issue.getKey(), ie);
			}
			try {
				issueManager.updateIssue(UserUtils.getUser((String) args.get("userAdmin")), issue, EventDispatchOption.DO_NOT_DISPATCH, false);
			} catch (Exception e) {
				log.warn("Failed to update issue: " + issue.getKey(), e);
			}
			ImportUtils.setIndexIssues(true);
			try {
				issueIndexManager.reIndex(issue);

			} catch (Exception e) {
				log.warn("Failed to reindex issue: " + issue.getKey(), e);
			}
		}
	}
}
