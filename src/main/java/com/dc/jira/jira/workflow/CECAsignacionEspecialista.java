package com.dc.jira.jira.workflow;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.event.type.EventDispatchOption;
import com.atlassian.jira.issue.CustomFieldManager;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.IssueManager;
import com.atlassian.jira.user.UserUtils;
import com.atlassian.jira.user.util.UserUtil;
import com.atlassian.jira.util.ImportUtils;
import com.atlassian.jira.workflow.function.issue.AbstractJiraFunctionProvider;
import com.opensymphony.module.propertyset.PropertySet;
import com.opensymphony.workflow.WorkflowException;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;

import com.atlassian.jira.issue.MutableIssue;
import com.atlassian.jira.issue.customfields.manager.OptionsManager;
import com.atlassian.jira.issue.customfields.option.Option;
import com.atlassian.jira.issue.fields.CustomField;
import com.atlassian.jira.issue.fields.config.FieldConfig;
import com.atlassian.jira.issue.index.IssueIndexManager;
import com.atlassian.jira.issue.security.IssueSecurityLevel;
import com.atlassian.jira.issue.security.IssueSecurityLevelManager;

@SuppressWarnings("unused")
public class CECAsignacionEspecialista extends AbstractJiraFunctionProvider {
	private static final Logger log = LoggerFactory.getLogger(CECAsignacionEspecialista.class);
	public static final String ADMIN_USER = "userAdmin";

	private <T> ArrayList<T> asArrayList(T value) {
        ArrayList<T> list = new ArrayList<T>(1);
        list.add(value);
        return list;
    }
	
	@SuppressWarnings("rawtypes")
	public void execute(Map transientVars, Map args, PropertySet ps) throws WorkflowException {
		String materiaEspecialista = "";
		String materia = "";
		String especialista = "";
		String responsable = "";
		String CUC = "";
		String admin = (String) args.get("userAdmin");
		MutableIssue issue = getIssue(transientVars);

		IssueManager issueManager = ComponentAccessor.getIssueManager();

		log.warn("Asignación especialista");
		CustomFieldManager customFieldManager = ComponentAccessor.getCustomFieldManager();
		materiaEspecialista = customFieldManager.getCustomFieldObjectByName("Materia - Especialista").getValue(issue)
				.toString();
		// materiaEspecialista = customFieldManager.getCustomFieldObject(11024L).getValue(issue).toString();
		if (materiaEspecialista != "") {
			materia = materiaEspecialista.substring(materiaEspecialista.indexOf("=") + 1,
					materiaEspecialista.indexOf(","));
			especialista = materiaEspecialista.substring(materiaEspecialista.indexOf("1=") + 2,
					materiaEspecialista.indexOf("}"));
			responsable = especialista.substring(especialista.indexOf("(") + 1, especialista.length() - 1);
		}
		log.warn("Asignación especialista [" + responsable + "].");

		issue.setAssignee(UserUtils.getUser(responsable));

		UserUtil uu = ComponentAccessor.getUserUtil();
		// Asignar al campo Especialista el responsable para activar el nivel de seguridad del usuario externo que está
		// definido como
		// User Custom Field Value (Especialista)
		CustomField especialistas = ComponentAccessor.getCustomFieldManager()
				.getCustomFieldObjectByName("Especialista");
		issue.setCustomFieldValue(especialistas, uu.getUserByName(responsable));

		String project = issue.getProjectObject().getKey();
		SortedSet<String> grupos = ComponentAccessor.getUserUtil().getGroupNamesForUser(issue.getAssignee().getName());
		log.warn("Compruebo Usuario Externo");
		if (grupos.contains("CJ Especialista Externo EC")) {
			IssueSecurityLevelManager issueSecurityLevelManager = ComponentAccessor.getIssueSecurityLevelManager();
			List issueSecurityLevels = issueSecurityLevelManager.getUsersSecurityLevels(issue.getProjectObject(),
					issue.getAssignee());
			Iterator it = issueSecurityLevels.listIterator();
			while (it.hasNext()) {
				IssueSecurityLevel issueSecurityLevel = (IssueSecurityLevel) it.next();
				if (issueSecurityLevel.getName().contains("NivSeg CJ Externas EC")) {
					issue.setSecurityLevelId(issueSecurityLevel.getId());
					break;
				}
			}
		}

		CUC = customFieldManager.getCustomFieldObjectByName("CUC").getValue(issue).toString();
		if (CUC.contains("7956627")) {
			CustomField publish = customFieldManager.getCustomFieldObjectByName("Publish");
			OptionsManager optionsManager = ComponentAccessor.getOptionsManager();
			FieldConfig publishConfig = publish.getRelevantConfig(issue);
			List<Option> options = optionsManager.getOptions(publishConfig);
			if (options != null) {
				for (Option option : options) {
					if (option.getValue().contains("No")) {
						issue.setCustomFieldValue(publish, asArrayList(option));
						break;
					}
				}
			}
		}

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
		// issue.store();
		ImportUtils.setIndexIssues(true);
		try {
			issueIndexManager.reIndex(issue);
		} catch (Exception e) {
			log.warn("Failed to reindex issue: " + issue.getKey(), e);
		}
		log.warn("Cambio de usuario realizado.");
	}
}