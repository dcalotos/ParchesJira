package com.dc.jira.jira.workflow;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import util.DatosBBDD;

import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.config.ConstantsManager;
import com.atlassian.jira.event.type.EventDispatchOption;
import com.atlassian.jira.issue.CustomFieldManager;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.IssueManager;
import com.atlassian.jira.plugin.projectoperation.ProjectOperationModuleDescriptor;
import com.atlassian.jira.user.UserUtils;
import com.atlassian.jira.user.util.UserUtil;
import com.atlassian.jira.util.ImportUtils;
import com.atlassian.jira.workflow.function.issue.AbstractJiraFunctionProvider;
import com.opensymphony.module.propertyset.PropertySet;
import com.opensymphony.workflow.WorkflowException;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Map;
import com.atlassian.jira.issue.MutableIssue;
import com.atlassian.jira.issue.fields.CustomField;
import com.atlassian.jira.issue.index.IssueIndexManager;

/*
 This is the post-function class that gets executed at the end of the transition.
 Any parameters that were saved in your factory class will be available in the transientVars Map.
 */

@SuppressWarnings("unused")
public class GSSControlPermisosEdicion extends AbstractJiraFunctionProvider {
	private static final Logger log = LoggerFactory.getLogger(GSSControlPermisosEdicion.class);
	private String userAdmin;

	@SuppressWarnings("rawtypes")
	public void execute(Map transientVars, Map args, PropertySet ps) throws WorkflowException {
		MutableIssue mutableIssue = getIssue(transientVars);
		String tipoIssue = mutableIssue.getIssueTypeObject().getName();
		String project = mutableIssue.getProjectObject().getKey();
		setUserAdmin((String) args.get("userAdmin"));

		String productOnline = "";
		String aplicacion = "";
		String modulo = "";
		String datosCalculo="";

		CustomFieldManager customFieldManager = ComponentAccessor.getCustomFieldManager();

		if (project.contains("BACSAUX") || project.contains("BO")) {
			CustomField customField = customFieldManager.getCustomFieldObjectByName("Aplicaciones BO SAUX");
			String cfAplicacionesBOSAUX = (String) mutableIssue.getCustomFieldValue(customField).toString();
			/*
			 * if (cfAplicacionesBOSAUX.contains(",")) { aplicacion =
			 * cfAplicacionesBOSAUX.substring(cfAplicacionesBOSAUX.indexOf("=") + 1, cfAplicacionesBOSAUX.indexOf(","));
			 * modulo = cfAplicacionesBOSAUX.substring(cfAplicacionesBOSAUX.indexOf("1=") + 2,
			 * cfAplicacionesBOSAUX.indexOf("}")); } else { aplicacion =
			 * cfAplicacionesBOSAUX.substring(cfAplicacionesBOSAUX.indexOf("=") + 1, cfAplicacionesBOSAUX.indexOf("}"));
			 * }
			 */
			datosCalculo = tipoIssue + "$" + cfAplicacionesBOSAUX ;
			log.warn("FdatosCalculo BACSAUX: " + datosCalculo);
		}
		
		if (project.contains("PHOENIX") || project.contains("PRUEPHO")) {
			String componente = mutableIssue.getComponentObjects().iterator().next().getName();
			datosCalculo = tipoIssue + "$" + componente ;
			log.warn("FdatosCalculo PHOENIX: " + datosCalculo);
		}

		CustomField datosCalculoPrioridad = ComponentAccessor.getCustomFieldManager().getCustomFieldObjectByName("DCP");
		mutableIssue.setCustomFieldValue(datosCalculoPrioridad, datosCalculo);

		IssueIndexManager issueIndexManager = ComponentAccessor.getIssueIndexManager();
		ImportUtils.setIndexIssues(false);
		try {
			issueIndexManager.deIndex(mutableIssue);
		} catch (Exception ie) {
			log.warn("Failed to deindex issue: " + mutableIssue.getKey(), ie);
		}
		try {
			IssueManager issueManager = ComponentAccessor.getIssueManager();
			issueManager.updateIssue(UserUtils.getUser((String) args.get("userAdmin")), mutableIssue,
					EventDispatchOption.DO_NOT_DISPATCH, false);
		} catch (Exception ie) {
			log.warn("Failed to deindex issue: " + mutableIssue.getKey(), ie);
		}
		ImportUtils.setIndexIssues(true);
		try {
			issueIndexManager.reIndex(mutableIssue);
		} catch (Exception e) {
			log.warn("Failed to reindex issue: " + mutableIssue.getKey(), e);
		}
		log.warn("Cambio de prioridad realizado.");
	}

	public String getUserAdmin() {
		return userAdmin;
	}

	public void setUserAdmin(String userAdmin) {
		this.userAdmin = userAdmin;
	}
}