package com.dc.jira.jira.workflow;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
public class PAMControlRentabilidad implements Validator {
	private static final Logger log = LoggerFactory.getLogger(PAMControlRentabilidad.class);

	private final CustomFieldManager customFieldManager;
	private static final String FIELD_NAME = "field";

	public PAMControlRentabilidad(CustomFieldManager customFieldManager) {
		this.customFieldManager = customFieldManager;
	}

	@SuppressWarnings("rawtypes")
	public void validate(Map transientVars, Map args, PropertySet ps) throws InvalidInputException {

		Object cfValue = null;
		String justificacion = "";

		Issue issue = (Issue) transientVars.get("issue");
		String field = (String) args.get(FIELD_NAME);	

		double ingresos = Double.parseDouble(issue.getCustomFieldValue(
				customFieldManager.getCustomFieldObjectByName("Revenues")).toString());

		double margin = Double.parseDouble(issue
				.getCustomFieldValue(customFieldManager.getCustomFieldObjectByName("Margin")).toString()
				.replace("%", "").replace(",", "."));

		if (margin < 45 || ingresos < 5000) {

			CustomField customField = customFieldManager.getCustomFieldObjectByName(field);

			if (customField != null) {
				// Check if the custom field value is NULL
				if (issue.getCustomFieldValue(customField) == null) {

					throw new InvalidInputException(
							"Tiene que completar el campo justificación cuando la rentabilidad del proyecto no llega al 45% o los ingresos son inferiores a 5.000 € ."
									+ "Sí lo necesita póngase en contacto con Diana Abreu.");
				}
			}
		}
	}
}
