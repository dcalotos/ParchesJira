package com.dc.jira;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
//import java.util.Set;
import java.util.SortedSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;

import util.DatosBBDD;

import com.atlassian.crowd.embedded.api.User;
import com.atlassian.event.api.EventListener;
import com.atlassian.event.api.EventPublisher;
import com.atlassian.jira.bc.issue.IssueService;
import com.atlassian.jira.bc.issue.IssueService.IssueResult;
import com.atlassian.jira.bc.issue.IssueService.TransitionValidationResult;
import com.atlassian.jira.bc.project.component.ProjectComponent;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.config.StatusManager;
import com.atlassian.jira.event.issue.IssueEvent;
import com.atlassian.jira.event.type.EventDispatchOption;
import com.atlassian.jira.event.type.EventType;
//import com.atlassian.jira.exception.IssuePermissionException;
import com.atlassian.jira.issue.CustomFieldManager;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.IssueManager;
import com.atlassian.jira.issue.MutableIssue;
import com.atlassian.jira.issue.customfields.manager.OptionsManager;
import com.atlassian.jira.issue.customfields.option.Option;
import com.atlassian.jira.issue.fields.CustomField;
import com.atlassian.jira.issue.fields.config.FieldConfig;
import com.atlassian.jira.issue.index.IssueIndexManager;
import com.atlassian.jira.issue.security.IssueSecurityLevel;
import com.atlassian.jira.issue.security.IssueSecurityLevelManager;
import com.atlassian.jira.issue.status.Status;
//import com.atlassian.jira.project.Project;
//import com.atlassian.jira.project.ProjectManager;
//import com.atlassian.jira.security.roles.ProjectRole;
//import com.atlassian.jira.security.roles.ProjectRoleActors;
//import com.atlassian.jira.security.roles.ProjectRoleManager;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.user.DelegatingApplicationUser;
import com.atlassian.jira.user.UserUtils;
import com.atlassian.jira.user.util.UserUtil;
import com.atlassian.jira.util.ImportUtils;
import com.atlassian.jira.workflow.JiraWorkflow;
import com.opensymphony.workflow.InvalidInputException;
import com.opensymphony.workflow.loader.ActionDescriptor;
import com.opensymphony.workflow.loader.StepDescriptor;

@SuppressWarnings("unused")
public class ProcesarEdicion implements InitializingBean, DisposableBean {
	private static final Logger log = LoggerFactory.getLogger(ProcesarEdicion.class);
	private final EventPublisher eventPublisher;

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@EventListener
	public void onIssueEvent(IssueEvent issueEvent) throws InvalidInputException {
		String project = issueEvent.getProject().getKey();
		Long eventTypeId = issueEvent.getEventTypeId();
		Issue issue = issueEvent.getIssue();

		/***********************************************************************************************************
		 * 
		 * EDICION IPO. Cambio de Prioridad en IPO mediante edición del campo prioridad, Tipo de problema o/y Producto.
		 * 
		 ***********************************************************************************************************/
		if ((eventTypeId.equals(EventType.ISSUE_UPDATED_ID) || eventTypeId.equals(EventType.ISSUE_MOVED_ID))
				&& (project.contains("IPO") && issue.getIssueTypeObject().getName().equals("Incidencia"))) {
			Long issue_id = issue.getId();
			IssueManager issueManager = ComponentAccessor.getIssueManager();
			User user = issueEvent.getUser();
			MutableIssue mutableIssue = issueManager.getIssueObject(issue_id);
			CustomFieldManager customFieldManager = ComponentAccessor.getCustomFieldManager();
			boolean grabarTipoProblema = false;

			List<CustomField> customFields = customFieldManager.getCustomFieldObjects(mutableIssue);

			String sDatosCalculoPrioridad = "";

			if (customFields.contains(customFieldManager.getCustomFieldObjectByName("DCP"))) {
				if (customFieldManager.getCustomFieldObjectByName("DCP").getValue(mutableIssue) != null) {
					sDatosCalculoPrioridad = customFieldManager.getCustomFieldObjectByName("DCP")
							.getValue(mutableIssue).toString();
				} else {
					sDatosCalculoPrioridad = "";
					log.warn("Field DCP contains null value.");
				}
			} else {
				sDatosCalculoPrioridad = "";
				log.warn("CustomField DCP not exists.");
			}

			String sProductAnt = "";
			String sProblemAnt = "";
			String sPriorityAnt = "";

			if (sDatosCalculoPrioridad != "") {
				String[] campos = sDatosCalculoPrioridad.split("\\$");

				sProductAnt = campos[0];
				sProblemAnt = campos[1];
				sPriorityAnt = campos[2];
			}
			String productOnline = "";
			String productVal = "";
			int iProductLevelVal = 1; // Variable utilizada para guardar el peso del Producto de la issue

			DatosBBDD metodos = new DatosBBDD();

			if (customFields.contains(customFieldManager.getCustomFieldObjectByName("PRODUCTOS ONLINE"))) {
				if (customFieldManager.getCustomFieldObjectByName("PRODUCTOS ONLINE").getValue(mutableIssue) != null) {
					productOnline = customFieldManager.getCustomFieldObjectByName("PRODUCTOS ONLINE")
							.getValue(mutableIssue).toString();
					if (productOnline != "") {
						productVal = productOnline.substring(productOnline.indexOf("1=") + 2,
								productOnline.indexOf("}"));
						iProductLevelVal = metodos.ObtencionPesoProductoOnline(productVal);
					}
				} else {
					iProductLevelVal = 1;
					log.warn("Field PRODUCTOS ONLINE contains null value.");
				}
			} else {
				iProductLevelVal = 1;
				log.warn("CustomField PRODUCTOS ONLINE not exists.");
			}

			String problemVal = ""; // variable utilizada para guardar el peso de Tipo de problema de la issue
			int iProblemTypeVal = 1; // Cargamos la lista de campos personalizados de la issue
			if (customFields.contains(customFieldManager.getCustomFieldObjectByName("Tipo de Problema"))) {
				if (customFieldManager.getCustomFieldObjectByName("Tipo de Problema").getValue(mutableIssue) != null) {
					try {
						problemVal = customFieldManager.getCustomFieldObjectByName("Tipo de Problema")
								.getValue(mutableIssue).toString();
					} catch (Exception e) {
						log.warn("Field DCP contains null value.");
					}
				}
				if (problemVal == null || problemVal == "") {
					problemVal = "Otras incidencias no listadas";
					grabarTipoProblema = true;
				}
			} else {
				problemVal = "Otras incidencias no listadas";
				grabarTipoProblema = true;
			}

			if (problemVal != "") {
				iProblemTypeVal = metodos.ObtencionPesoTipoProblema(problemVal);
			} else {
				iProblemTypeVal = 1;
			}

			metodos = null;

			int iPriorityEdit = 0;

			String sPriorityVal = "";
			iPriorityEdit = iProblemTypeVal * iProductLevelVal;

			// Prevalece el cambio de prioridad sobre cualquier otro cambio en la edición.
			if ((!mutableIssue.getPriorityObject().getId().contains(sPriorityAnt))
					&& eventTypeId.equals(EventType.ISSUE_UPDATED_ID)) {
				sPriorityVal = mutableIssue.getPriorityObject().getId();
				// Si la incidencia viene movida de otro proyecto calculamos la prioridad y el campo DCP
			} else if (eventTypeId.equals(EventType.ISSUE_MOVED_ID)
					|| (!problemVal.contains(sProblemAnt) || !productVal.contains(sProductAnt))) {
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
				}
			} else {
				sPriorityVal = sPriorityAnt;
			}

			String datosCalculo = productVal + "$" + problemVal + "$" + sPriorityVal;
			CustomField datosCalculoPrioridad = ComponentAccessor.getCustomFieldManager().getCustomFieldObjectByName(
					"DCP");
			mutableIssue.setCustomFieldValue(datosCalculoPrioridad, datosCalculo);

			if (grabarTipoProblema) {
				String tipodeProblema = "Otras incidencias no listadas";
				CustomField tipoProblema = ComponentAccessor.getCustomFieldManager().getCustomFieldObjectByName(
						"Tipo de Problema");
				OptionsManager optionsManager = ComponentAccessor.getOptionsManager();
				FieldConfig relevantConfig = tipoProblema.getRelevantConfig(mutableIssue);
				List<Option> options = optionsManager.findByOptionValue(tipodeProblema);
				if (options.size() == 0) {
					try {
						Long optionId = Long.parseLong(tipodeProblema);
						Option option = optionsManager.findByOptionId(optionId);
						options = Collections.singletonList(option);
					} catch (NumberFormatException e) {
					}
				}
				for (Option option : options) {
					FieldConfig fieldConfig = option.getRelatedCustomField();
					if (relevantConfig != null && relevantConfig.equals(fieldConfig)) {
						mutableIssue.setCustomFieldValue(tipoProblema, option);
					}
				}
			}

			mutableIssue.setPriorityId(sPriorityVal);
			IssueIndexManager issueIndexManager = ComponentAccessor.getIssueIndexManager();
			ImportUtils.setIndexIssues(false);
			try {
				issueIndexManager.deIndex(mutableIssue);
			} catch (Exception ie) {
				log.warn("Failed to deindex issue: " + issue_id, ie);
			}
			try {
				issueManager.updateIssue(user, mutableIssue, EventDispatchOption.DO_NOT_DISPATCH, false);
			} catch (Exception ie) {
				log.warn("Error Update Issue: " + issue_id, ie);
			}

			// mutableIssue.store();
			ImportUtils.setIndexIssues(true);
			try {
				issueIndexManager.reIndex(mutableIssue);
			} catch (Exception e) {
				log.warn("Failed to reindex issue: " + issue_id, e);
			}
		}
		/***********************************************************************************************************
		 * 
		 * EDICION ARQ. Cambio del campo Otros Usuarios mediante la edición del Componente.
		 * 
		 ***********************************************************************************************************/
		if (eventTypeId.equals(EventType.ISSUE_UPDATED_ID) && ((project.contains("ARQ")))) {
			Long issue_id = issue.getId();
			IssueManager issueManager = ComponentAccessor.getIssueManager();
			User user = issueEvent.getUser();
			MutableIssue mutableIssue = issueManager.getIssueObject(issue_id);
			UserUtil uu = ComponentAccessor.getUserUtil();
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
			CustomField otrosUsuariosInteresados = ComponentAccessor.getCustomFieldManager()
					.getCustomFieldObjectByName("Otros usuarios interesados");
			userList = (ArrayList<ApplicationUser>) otrosUsuariosInteresados.getValue(issue);
			if (userList == null && usuarioBackup != "") {
				userList = new ArrayList<ApplicationUser>();
				actualizar = true;
			} else if (!userList.contains(uu.getUserByName(usuarioBackup).getDirectoryUser()) && usuarioBackup != "") {
				actualizar = true;
			}
			if (actualizar == true) {
				userList.add(uu.getUserByName(usuarioBackup));
				mutableIssue.setCustomFieldValue(otrosUsuariosInteresados, userList);
				IssueIndexManager issueIndexManager = ComponentAccessor.getIssueIndexManager();
				ImportUtils.setIndexIssues(false);
				try {
					issueIndexManager.deIndex(issue);
				} catch (Exception ie) {
					log.warn("Failed to deindex issue: " + issue.getKey(), ie);
				}
				try {
					issueManager.updateIssue(user, mutableIssue, EventDispatchOption.DO_NOT_DISPATCH, false);
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
		/***********************************************************************************************************
		 * 
		 * EDICION IPF. Cambio de Prioridad en IPF mediante edición del campo prioridad, Tipo de problema o/y Producto.
		 * 
		 ***********************************************************************************************************/
		if ((eventTypeId.equals(EventType.ISSUE_UPDATED_ID) || eventTypeId.equals(EventType.ISSUE_MOVED_ID))
				&& (project.contains("IPF"))) {
			Long issue_id = issue.getId();
			IssueManager issueManager = ComponentAccessor.getIssueManager();
			User user = issueEvent.getUser();
			MutableIssue mutableIssue = issueManager.getIssueObject(issue_id);
			CustomFieldManager customFieldManager = ComponentAccessor.getCustomFieldManager();
			boolean grabarTypedeproblème = false;

			List<CustomField> customFields = customFieldManager.getCustomFieldObjects(mutableIssue);

			String sDatosCalculoPrioridad = "";

			if (customFields.contains(customFieldManager.getCustomFieldObjectByName("DCP"))) {
				if (customFieldManager.getCustomFieldObjectByName("DCP").getValue(mutableIssue) != null) {
					sDatosCalculoPrioridad = customFieldManager.getCustomFieldObjectByName("DCP")
							.getValue(mutableIssue).toString();
				} else {
					sDatosCalculoPrioridad = "";
					log.warn("Field DCP contains null value.");
				}
			} else {
				sDatosCalculoPrioridad = "";
				log.warn("CustomField DCP not exists.");
			}

			String sProductAnt = "";
			String sProblemAnt = "";
			String sPriorityAnt = "";

			if (sDatosCalculoPrioridad != "") {
				String[] campos = sDatosCalculoPrioridad.split("\\$");

				sProductAnt = campos[0];
				sProblemAnt = campos[1];
				sPriorityAnt = campos[2];
			}
			String productOnline = "";
			String productVal = "";
			int iProductLevelVal = 1; // Variable utilizada para guardar el peso del Producto de la issue

			DatosBBDD metodos = new DatosBBDD();

			if (customFields.contains(customFieldManager.getCustomFieldObjectByName("Produits On line France"))) {
				if (customFieldManager.getCustomFieldObjectByName("Produits On line France").getValue(mutableIssue) != null) {
					productOnline = customFieldManager.getCustomFieldObjectByName("Produits On line France")
							.getValue(mutableIssue).toString();
					if (productOnline != "") {
						productVal = productOnline.substring(productOnline.indexOf("1=") + 2,
								productOnline.indexOf("}"));
						iProductLevelVal = metodos.ObtencionPesoProductoOnline(productVal);
					}
				} else {
					log.warn("Field Produits On line France contains null value.");
				}
			} else {
				log.warn("CustomField Produits On line France not exists.");
			}

			String problemVal = ""; // variable utilizada para guardar el peso de Tipo de problema de la issue
			int iProblemTypeVal = 1; // Cargamos la lista de campos personalizados de la issue
			if (customFields.contains(customFieldManager.getCustomFieldObjectByName("Type de problème"))) {
				if (customFieldManager.getCustomFieldObjectByName("Type de problème").getValue(mutableIssue) != null) {
					try {
						problemVal = customFieldManager.getCustomFieldObjectByName("Type de problème")
								.getValue(mutableIssue).toString();
					} catch (Exception e) {
						log.warn("Field DCP contains null value.");
					}
				}
				if (problemVal == null || problemVal == "") {
					grabarTypedeproblème = true;
					problemVal = "Autre incident non répertorié";
				}
			} else {
				grabarTypedeproblème = true;
				problemVal = "Autre incident non répertorié";
			}

			if (problemVal != "") {
				iProblemTypeVal = metodos.ObtencionPesoTipoProblema(problemVal);
			}

			metodos = null;

			int iPriorityEdit = 0;

			String sPriorityVal = "";
			iPriorityEdit = iProblemTypeVal * iProductLevelVal;

			// Prevalece el cambio de prioridad sobre cualquier otro cambio en la edición.
			if ((!mutableIssue.getPriorityObject().getId().contains(sPriorityAnt))
					&& eventTypeId.equals(EventType.ISSUE_UPDATED_ID)) {
				sPriorityVal = mutableIssue.getPriorityObject().getId();
				// Si la incidencia viene movida de otro proyecto calculamos la prioridad y el campo DCP
			} else if (eventTypeId.equals(EventType.ISSUE_MOVED_ID)
					|| (!problemVal.contains(sProblemAnt) || !productVal.contains(sProductAnt))) {
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
				}
			} else {
				sPriorityVal = sPriorityAnt;
			}

			String datosCalculo = productVal + "$" + problemVal + "$" + sPriorityVal;
			CustomField datosCalculoPrioridad = ComponentAccessor.getCustomFieldManager().getCustomFieldObjectByName(
					"DCP");
			mutableIssue.setCustomFieldValue(datosCalculoPrioridad, datosCalculo);

			if (grabarTypedeproblème) {
				String tipodeProblema = "Autre incident non répertorié";
				CustomField tipoProblema = ComponentAccessor.getCustomFieldManager().getCustomFieldObjectByName(
						"Type de problème");
				OptionsManager optionsManager = ComponentAccessor.getOptionsManager();
				FieldConfig relevantConfig = tipoProblema.getRelevantConfig(mutableIssue);
				List<Option> options = optionsManager.findByOptionValue(tipodeProblema);
				if (options.size() == 0) {
					try {
						Long optionId = Long.parseLong(tipodeProblema);
						Option option = optionsManager.findByOptionId(optionId);
						options = Collections.singletonList(option);
					} catch (NumberFormatException e) {
					}
				}
				for (Option option : options) {
					FieldConfig fieldConfig = option.getRelatedCustomField();
					if (relevantConfig != null && relevantConfig.equals(fieldConfig)) {
						mutableIssue.setCustomFieldValue(tipoProblema, option);
					}
				}
			}

			mutableIssue.setPriorityId(sPriorityVal);
			IssueIndexManager issueIndexManager = ComponentAccessor.getIssueIndexManager();
			ImportUtils.setIndexIssues(false);
			try {
				issueIndexManager.deIndex(mutableIssue);
			} catch (Exception ie) {
				log.warn("Failed to deindex issue: " + issue_id, ie);
			}
			issueManager.updateIssue(user, mutableIssue, EventDispatchOption.DO_NOT_DISPATCH, false);
			// mutableIssue.store();
			ImportUtils.setIndexIssues(true);
			try {
				issueIndexManager.reIndex(mutableIssue);
			} catch (Exception e) {
				log.warn("Failed to reindex issue: " + issue_id, e);
			}
		}
		/***********************************************************************************************************
		 * 
		 * EDICION WCJ. Cambio del campo Materia y Submateria en Edicion.
		 * 
		 ***********************************************************************************************************/
		if (eventTypeId.equals(EventType.ISSUE_UPDATED_ID) && ((project.contains("WCJ")))) {
			Long issue_id = issue.getId();
			log.warn("Editando Consulta Jurídica... ");
			IssueManager issueManager = ComponentAccessor.getIssueManager();
			User user = issueEvent.getUser();
			log.warn("User: " + user.getDisplayName());
			MutableIssue mutableIssue = issueManager.getIssueObject(issue_id);
			log.warn("MutableIssue: " + mutableIssue.getKey());
			UserUtil uu = ComponentAccessor.getUserUtil();
			String materiaSubmateria = "";
			String materia = "";
			String submateria = "";
			String responsable = "";
			log.warn("Accediendo a grupos del usuario...");
			log.warn("Usuario asignado: " + issue.getAssignee().getName());
			if (issue.getAssignee().getName() != null) {
				SortedSet<String> grupos = ComponentAccessor.getUserUtil().getGroupNamesForUser(
						issue.getAssignee().getName());
				log.warn("Grupos: " + grupos.toString());
				if (grupos.contains("CJ Especialistas Externos")) {
					log.warn("Dentro de especialistas externos...");
					IssueSecurityLevelManager issueSecurityLevelManager = ComponentAccessor
							.getIssueSecurityLevelManager();
					List issueSecurityLevels = issueSecurityLevelManager.getUsersSecurityLevels(
							issue.getProjectObject(), issue.getAssignee());
					Iterator it = issueSecurityLevels.listIterator();
					while (it.hasNext()) {
						IssueSecurityLevel issueSecurityLevel = (IssueSecurityLevel) it.next();
						if (issueSecurityLevel.getName().contains("NivSeg CJ Externas")) {
							mutableIssue.setSecurityLevelId(issueSecurityLevel.getId());
							break;
						}
					}
				}
			} else {
				CustomFieldManager customFieldManager = ComponentAccessor.getCustomFieldManager();
				log.warn("Creando objeto de metodos de base de datos de parámetros...");
				DatosBBDD metodos = new DatosBBDD();

				materiaSubmateria = customFieldManager.getCustomFieldObjectByName("Materia y submateria")
						.getValue(issue).toString();
				log.warn("Materia y Submateria: " + materiaSubmateria);
				if (materiaSubmateria != "") {
					materia = materiaSubmateria.substring(materiaSubmateria.indexOf("=") + 1,
							materiaSubmateria.indexOf(","));
					log.warn("Materia: " + materia);
					submateria = materiaSubmateria.substring(materiaSubmateria.indexOf("1=") + 2,
							materiaSubmateria.indexOf("}"));
					log.warn("Submateria: " + submateria);
					responsable = metodos.ObtencionResponsableSubmateria(submateria);
					log.warn("Responsable: " + responsable);
				}

				metodos = null;
			}
			IssueIndexManager issueIndexManager = ComponentAccessor.getIssueIndexManager();
			ImportUtils.setIndexIssues(false);
			try {
				issueIndexManager.deIndex(issue);
			} catch (Exception ie) {
				log.warn("Failed to deindex issue: " + issue.getKey(), ie);
			}
			mutableIssue.setAssignee(uu.getUserByName(responsable).getDirectoryUser());
			log.warn("Asignado a la issue[" + responsable + "].");
			issueManager.updateIssue(user, mutableIssue, EventDispatchOption.DO_NOT_DISPATCH, false);

			ImportUtils.setIndexIssues(true);
			try {
				issueIndexManager.reIndex(issue);
			} catch (Exception e) {
				log.warn("Failed to reindex issue: " + issue.getKey(), e);
			}
		}
		/***********************************************************************************************************
		 * 
		 * EDICION PHOENIX. Cambio de campos IssueType o Componentes
		 * 
		 ***********************************************************************************************************/
		if (eventTypeId.equals(EventType.ISSUE_UPDATED_ID)
				&& (project.contains("PHOENIX") || project.contains("BACSAUX") || project.contains("BO") || project
						.contains("PRUEPHO"))) {

			String issueType = "";
			String componente = "";
			String cfAplicacionesBOSAUX = "";
			String sDatosCalculoPrioridad = "";
			String issueTypeOriginal = "";
			String componenteOriginal = "";
			String AplicacionesBOSAUXOriginal = "";
			String[] campos = null;

			String editadoPor = "";
			boolean grabarEditedby = false;

			Long issue_id = issue.getId();
			IssueManager issueManager = ComponentAccessor.getIssueManager();
			User user = issueEvent.getUser();
			MutableIssue mutableIssue = issueManager.getIssueObject(issue_id);
			CustomFieldManager customFieldManager = ComponentAccessor.getCustomFieldManager();
			List<CustomField> customFields = customFieldManager.getCustomFieldObjects(mutableIssue);

			if (customFields.contains(customFieldManager.getCustomFieldObjectByName("DCP"))) {
				if (customFieldManager.getCustomFieldObjectByName("DCP").getValue(mutableIssue) != null) {
					sDatosCalculoPrioridad = customFieldManager.getCustomFieldObjectByName("DCP")
							.getValue(mutableIssue).toString();
				} else {
					sDatosCalculoPrioridad = "";
					log.warn("Field DCP contains null value.");
				}
			} else {
				sDatosCalculoPrioridad = "";
				log.warn("CustomField DCP not exists.");
			}

			if (sDatosCalculoPrioridad != "") {
				issueType = mutableIssue.getIssueTypeObject().getName();

				campos = sDatosCalculoPrioridad.split("\\$");
				issueTypeOriginal = campos[0];

				if (project.contains("BACSAUX") || project.contains("BO")) {
					CustomField customField = customFieldManager.getCustomFieldObjectByName("Aplicaciones BO SAUX");
					cfAplicacionesBOSAUX = (String) mutableIssue.getCustomFieldValue(customField).toString();

					AplicacionesBOSAUXOriginal = campos[1];
					if (!AplicacionesBOSAUXOriginal.contains(cfAplicacionesBOSAUX)
							|| !issueTypeOriginal.contains(issueType)) {

						String aplicacionOriginal = "";
						String moduloOriginal = "";

						if (AplicacionesBOSAUXOriginal.contains(",")) {
							aplicacionOriginal = AplicacionesBOSAUXOriginal.substring(
									AplicacionesBOSAUXOriginal.indexOf("=") + 1,
									AplicacionesBOSAUXOriginal.indexOf(","));
							moduloOriginal = AplicacionesBOSAUXOriginal.substring(
									AplicacionesBOSAUXOriginal.indexOf("1=") + 2,
									AplicacionesBOSAUXOriginal.indexOf("}"));
						} else {
							aplicacionOriginal = AplicacionesBOSAUXOriginal.substring(
									AplicacionesBOSAUXOriginal.indexOf("=") + 1,
									AplicacionesBOSAUXOriginal.indexOf("}"));
						}

						String aplicacion = "";
						String modulo = "";

						if (cfAplicacionesBOSAUX.contains(",")) {
							aplicacion = cfAplicacionesBOSAUX.substring(cfAplicacionesBOSAUX.indexOf("=") + 1,
									cfAplicacionesBOSAUX.indexOf(","));
							modulo = cfAplicacionesBOSAUX.substring(cfAplicacionesBOSAUX.indexOf("1=") + 2,
									cfAplicacionesBOSAUX.indexOf("}"));
						} else {
							aplicacion = cfAplicacionesBOSAUX.substring(cfAplicacionesBOSAUX.indexOf("=") + 1,
									cfAplicacionesBOSAUX.indexOf("}"));
						}

						editadoPor = user.getDisplayName() + ". Fecha : "
								+ new SimpleDateFormat("EEEE, d MMM yyyy HH:mm:ss").format(new Date());

						if (issueTypeOriginal.contains(issueType)) {
							editadoPor += ". IssueType: [" + issueType + "]. ";
						} else {
							editadoPor += ". IssueType: [" + issueTypeOriginal + " --> " + issueType + "]. ";
						}
						if (aplicacionOriginal.contains(aplicacion)) {
							editadoPor += "Aplicacion: [" + aplicacion + "]. ";
						} else {
							editadoPor += "Aplicacion: [" + aplicacionOriginal + " --> " + aplicacion + "]. ";
						}
						if (moduloOriginal.contains(aplicacion)) {
							editadoPor += "Modulo: [" + modulo + "].";
						} else {
							editadoPor += "Modulo: [" + moduloOriginal + " --> " + modulo + "].";
						}

						sDatosCalculoPrioridad = issueType + "$" + cfAplicacionesBOSAUX;
						grabarEditedby = true;
					}
				}

				if (project.contains("PHOENIX") || project.contains("PRUEPHO")) {
					componenteOriginal = campos[1];
					componente = mutableIssue.getComponentObjects().iterator().next().getName();
					if (!componenteOriginal.contains(componente) || !issueTypeOriginal.contains(issueType)) {

						editadoPor = user.getDisplayName() + ". Fecha : "
								+ new SimpleDateFormat("EEEE, d MMM yyyy HH:mm:ss").format(new Date());

						if (issueTypeOriginal.contains(issueType)) {
							editadoPor += ". IssueType: [" + issueType + "]. ";
						} else {
							editadoPor += ". IssueType: [" + issueTypeOriginal + " --> " + issueType + "]. ";
						}
						if (componenteOriginal.contains(componente)) {
							editadoPor += "Componente: [" + componente + "].";
						} else {
							editadoPor += "Componente: [" + componenteOriginal + " --> " + componente + "].";
						}
						sDatosCalculoPrioridad = issueType + "$" + componente;
						grabarEditedby = true;
					}
				}

				if (grabarEditedby) {
					IssueIndexManager issueIndexManager = ComponentAccessor.getIssueIndexManager();
					ImportUtils.setIndexIssues(false);
					try {
						issueIndexManager.deIndex(issue);
					} catch (Exception ie) {
						log.warn("Failed to deindex issue: " + issue.getKey(), ie);
					}

					CustomField editadopor = ComponentAccessor.getCustomFieldManager().getCustomFieldObjectByName(
							"Edited by");
					mutableIssue.setCustomFieldValue(editadopor, editadoPor);
					log.warn("Editado por[" + editadoPor + "].");

					CustomField datosCalculoPrioridad = ComponentAccessor.getCustomFieldManager()
							.getCustomFieldObjectByName("DCP");
					mutableIssue.setCustomFieldValue(datosCalculoPrioridad, sDatosCalculoPrioridad);

					issueManager.updateIssue(user, mutableIssue, EventDispatchOption.DO_NOT_DISPATCH, false);

					ImportUtils.setIndexIssues(true);
					try {
						issueIndexManager.reIndex(issue);
					} catch (Exception e) {
						log.warn("Failed to reindex issue: " + issue.getKey(), e);
					}
				}
			}
		}
		/***********************************************************************************************************
		 * 
		 * CREACION GDV. Transicionar la petición en caso de que no tenga Director asociado. NO FUNCIONA
		 * 
		 ***********************************************************************************************************/
		/*
		if (eventTypeId.equals(EventType.ISSUE_CREATED_ID) && (project.contains("GDV"))) {
			Long issue_id = issue.getId();
			IssueManager issueManager = ComponentAccessor.getIssueManager();
			@SuppressWarnings("unused")
			User user = issueEvent.getUser();
			MutableIssue mutableIssue = issueManager.getIssueObject(issue_id);
			CustomFieldManager customFieldManager = ComponentAccessor.getCustomFieldManager();

			UserUtil uu = ComponentAccessor.getUserUtil();
			String responsable = "";

			String userAdmin = "adminjira";
			log.warn("userAdmin: " + userAdmin);

			List<CustomField> customFields = customFieldManager.getCustomFieldObjects(issue);
			@SuppressWarnings("unused")
			DelegatingApplicationUser director = null;
			// Si el campo Director está a nulo la pasamos directamente a Asignada
			try {
				director = (DelegatingApplicationUser) customFieldManager.getCustomFieldObjectByName("Director]")
						.getValue(issue);
			} catch (Exception e) {
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
						UserUtils.getUser(userAdmin), issue.getId(), actionId, issueService.newIssueInputParameters());
				// TransitionValidationResult transitionValidationResult =
				// issueService.validateTransition(authenticationContext.getLoggedInUser(),
				// issue.getId(), actionId, issueService.newIssueInputParameters());

				if (transitionValidationResult.isValid()) {
					IssueResult transitionResult = issueService.transition(UserUtils.getUser(userAdmin),
							transitionValidationResult);

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
							log.warn("estado: [" + estado.getName() + "]. Descripción: [" + estado.getDescription()
									+ "]");
							// if (estado.getName().equals("Resolved")) {
							if (estado.getName().contains("Assigned")) { // No sé si será Asignada en español
								log.warn("estado.getName(): [" + estado.getName() + "] + estado.getId(): ["
										+ estado.getId() + "]");
								estadoFinal = estado;
								break;
							}
						}
						mutableIssue.setStatusObject(estadoFinal);
						String empleadoViajero = null;
						DelegatingApplicationUser viajero = null;
						if (customFields
								.contains(customFieldManager.getCustomFieldObjectByName("Employee who travels"))) {
							if (customFieldManager.getCustomFieldObjectByName("Employee who travels").getValue(issue) != null) {
								// CustomField solvedBy =
								// customFieldManager.getCustomFieldObjectByName("Employee who travels");
								viajero = (DelegatingApplicationUser) customFieldManager.getCustomFieldObjectByName(
										"Employee who travels").getValue(issue);
								empleadoViajero = viajero.getUsername();
							} else {
								empleadoViajero = UserUtils.getUser(userAdmin).getName();
								log.warn("Field Employee who travels contains null value.");
							}
						} else {
							empleadoViajero = UserUtils.getUser(userAdmin).getName();
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
						mutableIssue.setCustomFieldValue(manager, userList.get(0));
						// Se puede hacer por postfunction de JIRA Suite Utilities. No se si esto se ejecuta al hacer la
						// trasición automática.

						IssueIndexManager issueIndexManager = ComponentAccessor.getIssueIndexManager();
						ImportUtils.setIndexIssues(false);
						try {
							issueIndexManager.deIndex(issue);
						} catch (Exception ie) {
							log.warn("Failed to deindex issue: " + issue.getKey(), ie);
						}
						// issueManager.updateIssue(UserUtils.getUser((String) args.get(projectLead)), issue,
						// EventDispatchOption.DO_NOT_DISPATCH, false);
						issueManager.updateIssue(UserUtils.getUser(userAdmin), mutableIssue,
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
		*/
	}

	public ProcesarEdicion(EventPublisher eventPublisher) {
		this.eventPublisher = eventPublisher;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		eventPublisher.register(this);
	}

	@Override
	public void destroy() throws Exception {
		eventPublisher.unregister(this);
	}
}