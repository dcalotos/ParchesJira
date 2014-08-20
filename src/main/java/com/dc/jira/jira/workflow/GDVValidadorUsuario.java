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
import com.atlassian.jira.user.DelegatingApplicationUser;
import com.atlassian.jira.user.UserUtils;
import com.atlassian.jira.user.util.UserUtil;

import com.opensymphony.module.propertyset.PropertySet;
import com.opensymphony.workflow.Validator;
import com.opensymphony.workflow.InvalidInputException;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;

@SuppressWarnings("unused")
public class GDVValidadorUsuario implements Validator {
	private static final Logger log = LoggerFactory
			.getLogger(GDVValidadorUsuario.class);

	private final CustomFieldManager customFieldManager;

	private static final String FIELD_NAME = "field";

	public GDVValidadorUsuario(CustomFieldManager customFieldManager) {
		this.customFieldManager = customFieldManager;
	}

	@SuppressWarnings("rawtypes")
	public void validate(Map transientVars, Map args, PropertySet ps)
			throws InvalidInputException {

		Object cfValue = null;
		String empleadoViajero = "";
		String validador = "";
		DelegatingApplicationUser viajero = null;
		
		Boolean permiso = false;

		Issue issue = (Issue) transientVars.get("issue");

		String field = (String) args.get(FIELD_NAME);
		
		CustomField customField = customFieldManager.getCustomFieldObjectByName(field);
		viajero = (DelegatingApplicationUser) issue.getCustomFieldValue(customField);
		empleadoViajero = viajero.getUsername();
		log.warn("empleadoViajero: [" + empleadoViajero + "]");
		
		/*
		CustomFieldManager customFieldManager = ComponentAccessor.getCustomFieldManager();
		List<CustomField> customFields = customFieldManager.getCustomFieldObjects(issue);
		
		
		DelegatingApplicationUser viajero = null;
		if (customFields.contains(customFieldManager.getCustomFieldObjectByName("Employee who travels"))) {
			if (customFieldManager.getCustomFieldObjectByName("Employee who travels").getValue(issue) != null) {
				//CustomField solvedBy = customFieldManager.getCustomFieldObjectByName("Solved by");
				viajero = (DelegatingApplicationUser) customFieldManager.getCustomFieldObjectByName("Employee who travels").getValue(issue);
				empleadoViajero = viajero.getUsername();
			} else {
				empleadoViajero = UserUtils.getUser((String) args.get("userAdmin")).getName();
				log.warn("Field Employee who travels contains null value.");
			}
		} else {
			empleadoViajero = UserUtils.getUser((String) args.get("userAdmin")).getName();
			log.warn("CustomField Employee who travels not exists.");
		}
		log.warn("empleadoViajero: [" + empleadoViajero + "]");
		*/
		
		DatosBBDD metodos = new DatosBBDD();
		//validador = metodos.ObtencionValidador(issue.getReporter().getName());
		validador = metodos.ObtencionValidador(empleadoViajero);
		metodos = null;

		if (validador.length()==0) {
			throw new InvalidInputException(
					"<b>NO DISPONE DE UN RESPONSABLE DE APROBACIÓN.</b><br />" +
					"Sí lo necesita póngase en contacto con Nuria Fernández Suárez.");
		}

	}
}
