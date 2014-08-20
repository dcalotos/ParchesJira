package com.dc.jira.jira.workflow;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.naming.AuthenticationException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import util.DatosBBDD;

import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.event.type.EventDispatchOption;
import com.atlassian.jira.issue.CustomFieldManager;
import com.atlassian.jira.issue.IssueManager;
import com.atlassian.jira.issue.MutableIssue;
import com.atlassian.jira.issue.customfields.manager.OptionsManager;
import com.atlassian.jira.issue.customfields.option.Option;
import com.atlassian.jira.issue.customfields.option.Options;
import com.atlassian.jira.issue.fields.CustomField;
import com.atlassian.jira.issue.fields.config.FieldConfig;
import com.atlassian.jira.issue.index.IssueIndexManager;
import com.atlassian.jira.user.UserUtils;
import com.atlassian.jira.user.util.UserUtil;
import com.atlassian.jira.util.ImportUtils;
import com.atlassian.jira.util.json.JSONException;
import com.atlassian.jira.workflow.function.issue.AbstractJiraFunctionProvider;

import com.opensymphony.module.propertyset.PropertySet;
import com.opensymphony.workflow.InvalidInputException;
import com.opensymphony.workflow.WorkflowException;

@SuppressWarnings("unused")
public class CECCompletarDatosPhoenix extends AbstractJiraFunctionProvider {
	private static final Logger log = LoggerFactory.getLogger(CECCompletarDatosPhoenix.class);
	public static final String USER_ADMIN = "userAdmin";

	String strConsultaSQL;
	Connection conn = null;
	Statement stmt;
	ResultSet rs;

	@SuppressWarnings({"rawtypes"})
	public void execute(Map transientVars, Map args, PropertySet ps) throws WorkflowException {
		String CUC = "";
		String editIssueData = "";

		ArrayList<String> datosPhoenix = new ArrayList<String>();
		MutableIssue mutableIssue = getIssue(transientVars);
		IssueManager issueManager = ComponentAccessor.getIssueManager();
		UserUtil uu = ComponentAccessor.getUserUtil();

		CustomFieldManager customFieldManager = ComponentAccessor.getCustomFieldManager();
		CUC = customFieldManager.getCustomFieldObjectByName("CUC").getValue(mutableIssue).toString();
		if (CUC.contains("/")) {
			CUC = CUC.replace("/", "-");
		}
		log.warn("CUC [" + CUC + "].");

		if (!CUC.contains("7956627")) {
			DatosBBDD metodos = new DatosBBDD();
			datosPhoenix = metodos.ObtencionDatosPhoenix(CUC);

			metodos = null;
			if (datosPhoenix.size() > 0) {
				log.warn("datosPhoenix Telefono [" + datosPhoenix.get(0) + "].");
				log.warn("datosPhoenix CodPostal [" + datosPhoenix.get(1) + "].");
				log.warn("datosPhoenix Provincia [" + datosPhoenix.get(2) + "].");
				log.warn("datosPhoenix Ayuntamiento [" + datosPhoenix.get(3) + "].");
				log.warn("datosPhoenix VCC [" + datosPhoenix.get(4) + "].");
				log.warn("datosPhoenix Clase [" + datosPhoenix.get(5) + "].");
				log.warn("datosPhoenix Habitantes [" + datosPhoenix.get(6) + "].");

				String Telefono = datosPhoenix.get(0);
				String CodPostal = datosPhoenix.get(1);
				String Provincia = datosPhoenix.get(2);
				String Ayuntamiento = datosPhoenix.get(3);
				String Clase = datosPhoenix.get(5);
				String VCC = datosPhoenix.get(4);
				String Habitantes = datosPhoenix.get(6);
				/*
				 * CustomField ambito=customFieldManager.getCustomFieldObjectByName("Ámbito"); FieldConfig ambitoConfig
				 * = ambito.getRelevantConfig(mutableIssue); OptionsManager
				 * optionsManager=ComponentAccessor.getOptionsManager(); Options options =
				 * optionsManager.getOptions(ambitoConfig); /* List<Option> opcionesAmbito=options.getRootOptions();
				 * Iterator<Option> iter=opcionesAmbito.iterator(); while(iter.hasNext()){ Option
				 * opcion=(Option)iter.next(); opcion.getOptionId(); }
				 */
				/*
				 * Option ambitoOption = options.getOptionForValue(Ambito, null); if (ambitoOption == null) {
				 * log.error("No se peude establecer el valor " + Ambito +
				 * " porque no se corresponde con ningún valor válido del campo personalizado."); } else { String
				 * valorTexto=ambitoOption.getValue(); ambitoID=ambitoOption.getOptionId(); }
				 */
				/*
				 * if (ambitoOption == null) { log.error("No se puede establecer el valor " + Ambito +
				 * " porque no se corresponde con ningún valor válido del campo personalizado."); } else {
				 * mutableIssue.setCustomFieldValue(ambito, ambitoOption); }
				 */
				CustomField TelefonoCF = ComponentAccessor.getCustomFieldManager().getCustomFieldObjectByName(
						"Teléfono");
				mutableIssue.setCustomFieldValue(TelefonoCF, Telefono);
				CustomField CodPostalCF = ComponentAccessor.getCustomFieldManager().getCustomFieldObjectByName(
						"Código Postal");
				mutableIssue.setCustomFieldValue(CodPostalCF, CodPostal);
				CustomField ProvinciaCF = ComponentAccessor.getCustomFieldManager().getCustomFieldObjectByName(
						"Provincia");
				mutableIssue.setCustomFieldValue(ProvinciaCF, Provincia);
				CustomField AyuntamientoCF = ComponentAccessor.getCustomFieldManager().getCustomFieldObjectByName(
						"Ayuntamiento");
				mutableIssue.setCustomFieldValue(AyuntamientoCF, Ayuntamiento);
				CustomField ClaseCF = ComponentAccessor.getCustomFieldManager().getCustomFieldObjectByName("Clase");
				mutableIssue.setCustomFieldValue(ClaseCF, Clase);
				CustomField VCCCF = ComponentAccessor.getCustomFieldManager().getCustomFieldObjectByName(
						"Valor de cartera");
				mutableIssue.setCustomFieldValue(VCCCF, VCC);
				CustomField HabitantesCF = ComponentAccessor.getCustomFieldManager().getCustomFieldObjectByName(
						"Número de habitantes población");
				mutableIssue.setCustomFieldValue(HabitantesCF, Habitantes);

				IssueIndexManager issueIndexManager = ComponentAccessor.getIssueIndexManager();
				ImportUtils.setIndexIssues(false);
				try {
					issueIndexManager.deIndex(mutableIssue);
				} catch (Exception ie) {
					log.warn("Failed to deindex issue: " + mutableIssue.getKey(), ie);
				}
				try {
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
				log.warn("Proceso completado: " + mutableIssue.getKey());

			} else {
				throw new InvalidInputException(
						"El CUC [ "
								+ CUC
								+ " ] no existe en la base de datos de Phoenix. No se recuperaron los valores correspondientes. Compruebe el valor.");
			}
		}
	}
}