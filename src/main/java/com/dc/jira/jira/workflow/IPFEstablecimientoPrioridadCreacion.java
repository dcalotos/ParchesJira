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

@SuppressWarnings("unused")
public class IPFEstablecimientoPrioridadCreacion extends AbstractJiraFunctionProvider {
	private static final Logger log = LoggerFactory.getLogger(IPFEstablecimientoPrioridadCreacion.class);
	private String userAdmin;

	@SuppressWarnings("rawtypes")
	public void execute(Map transientVars, Map args, PropertySet ps) throws WorkflowException {
		MutableIssue mutableIssue = getIssue(transientVars);
		String tipoIssue = mutableIssue.getIssueTypeObject().getName();
		if (tipoIssue.equals("Incidencia")) {
			setUserAdmin((String) args.get("userAdmin"));

			String productOnline = "";
			String productVal = "";
			int iProductLevelVal = 1;

			CustomFieldManager customFieldManager = ComponentAccessor.getCustomFieldManager();
			DatosBBDD metodos = new DatosBBDD();

			productOnline = customFieldManager.getCustomFieldObjectByName("Produits On line France").getValue(mutableIssue).toString();
			if (productOnline != "") {
				productVal = productOnline.substring(productOnline.indexOf("1=") + 2, productOnline.indexOf("}"));
				iProductLevelVal = metodos.ObtencionPesoProductoOnline(productVal);
			}
			String problemVal = "";
			int iProblemTypeVal = 1;
			problemVal = customFieldManager.getCustomFieldObjectByName("Type de problème").getValue(mutableIssue).toString();
			if (problemVal != "") {
				iProblemTypeVal = metodos.ObtencionPesoTipoProblema(problemVal);
			}

			metodos = null;

			int iPriorityEdit = 0;
			String sPriorityVal = "";
			// Productos de los dos pesos anteriores
			iPriorityEdit = iProblemTypeVal * iProductLevelVal;
			// log.warn("iPriorityEdit: " + iPriorityEdit);
			if (iPriorityEdit > 0) {
				if (iPriorityEdit >= 32) {
					sPriorityVal = "1";
				} else if (iPriorityEdit >= 16) {
					sPriorityVal = "2";
				} else if (iPriorityEdit >= 8) {
					sPriorityVal = "3";
				} else if (iPriorityEdit >= 4) {
					sPriorityVal = "4";
				} else {
					sPriorityVal = "5";
				}
			} else {
				// Si el producto de los pesos falla, se establece la prioridad por defecto. Para obtenerla utilizamos el gestor de issue types,
				// estados, prioridades y resoluciones. Este gestor se encarga de almacenamiento en caché de estas constantes, así como todas las
				// actualizaciones de las operaciones de modificar, borrar y añadir en la base de datos.
				ConstantsManager constantsManager = ComponentAccessor.getConstantsManager(); // El valor por defecto normalmente es Media
				sPriorityVal = constantsManager.getDefaultPriorityObject().getId();
			}

			String datosCalculo = productVal + "$" + problemVal + "$" + sPriorityVal;
			IssueManager issueManager = ComponentAccessor.getIssueManager();
			CustomField datosCalculoPrioridad = ComponentAccessor.getCustomFieldManager().getCustomFieldObjectByName("DCP");
			mutableIssue.setCustomFieldValue(datosCalculoPrioridad, datosCalculo);

			mutableIssue.setPriorityId(sPriorityVal);
			// Clase que gestiona la indexacion
			IssueIndexManager issueIndexManager = ComponentAccessor.getIssueIndexManager();
			ImportUtils.setIndexIssues(false);
			// Deindexamos la issue para realziar el cambio
			try {
				issueIndexManager.deIndex(mutableIssue);
				// log.warn("Deindexing issue");
			} catch (Exception ie) {
				log.warn("Failed to deindex issue: " + mutableIssue.getKey(), ie);
			}
			// Llamamos al gestor de la issue para que realice el cambio que el usuario que realiza la operación tiene que tener permisos de modificacion hemos
			// provocado en mutableissue en la base de datos
			try {
				issueManager.updateIssue(UserUtils.getUser((String) args.get("userAdmin")), mutableIssue, EventDispatchOption.DO_NOT_DISPATCH, false);
			} catch (Exception ie) {
				log.warn("Failed to updateIssue issue: " + mutableIssue.getKey(), ie);
			}
			// Método obsoleto
			// issue.store();
			// Siempre que se hace un cambio en una issue es necesario volver a reindexar la issue
			ImportUtils.setIndexIssues(true);
			try {
				issueIndexManager.reIndex(mutableIssue);
				// log.warn("Reindexing issue");
			} catch (Exception e) {
				log.warn("Failed to reindex issue: " + mutableIssue.getKey(), e);
			}
			log.warn("Cambio de prioridad realizado.");
		}
	}

	public String getUserAdmin() {
		return userAdmin;
	}

	public void setUserAdmin(String userAdmin) {
		this.userAdmin = userAdmin;
	}
}