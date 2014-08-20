package com.dc.jira.jira.workflow;

//import com.atlassian.core.util.map.EasyMap;
import com.opensymphony.workflow.loader.AbstractDescriptor;
import com.opensymphony.workflow.loader.ValidatorDescriptor;
import com.atlassian.jira.issue.CustomFieldManager;
import com.atlassian.jira.issue.fields.CustomField;
import com.atlassian.jira.plugin.workflow.WorkflowPluginValidatorFactory;
import com.atlassian.jira.plugin.workflow.AbstractWorkflowPluginFactory;
import com.atlassian.jira.util.collect.MapBuilder;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public class GSSValidadorPermisosAplicacionesFactory extends AbstractWorkflowPluginFactory implements WorkflowPluginValidatorFactory {
	private static final String FIELD_NAME = "field"; // Campo personalizado seleccionado.En vm es $field
	private static final String FIELDS = "fields"; // Campos personalizados seleccionado.En vm es $fields
	private static final String NOT_DEFINED = "No Definido";

	private final CustomFieldManager customFieldManager;

	/**
	 * Constructor. Necesitamos la clase customFieldManager para la gestión de los campos personalizados.
	 * 
	 * @param customFieldManager
	 */
	public GSSValidadorPermisosAplicacionesFactory(CustomFieldManager customFieldManager) {
		this.customFieldManager = customFieldManager;
	}

	/**
	 * Obtener la lista de campos personalizados.
	 * 
	 * @return
	 */
	private Collection<CustomField> getCamposPersonalizados() {
		List<CustomField> customFields = customFieldManager.getCustomFieldObjects();
		return customFields;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	protected void getVelocityParamsForInput(Map velocityParams) {
		velocityParams.put(FIELDS, getCamposPersonalizados());
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	protected void getVelocityParamsForEdit(Map velocityParams, AbstractDescriptor descriptor) {
		velocityParams.put(FIELD_NAME, getNombreCampo(descriptor));
		velocityParams.put(FIELDS, getCamposPersonalizados());
	}

	@SuppressWarnings({ "unchecked", "rawtypes", "unused" })
	protected void getVelocityParamsForView(Map velocityParams, AbstractDescriptor descriptor) {
		if (!(descriptor instanceof ValidatorDescriptor)) {
			throw new IllegalArgumentException("Descriptor must be a ValidatorDescriptor.");
		}

		ValidatorDescriptor validatorDescriptor = (ValidatorDescriptor) descriptor;

		velocityParams.put(FIELD_NAME, getNombreCampo(descriptor));
	}
/*
	@SuppressWarnings({ "unchecked" })
	public Map<String, String> getDescriptorParams(Map<String, Object> conditionParams) {
		if (conditionParams != null && conditionParams.containsKey(FIELD_NAME)) {
			return EasyMap.build(FIELD_NAME, extractSingleParam(conditionParams, FIELD_NAME));
		}

		return EasyMap.build();
	}
*/
	public Map<String, String> getDescriptorParams(Map<String, Object> conditionParams) {
		if (conditionParams != null && conditionParams.containsKey(FIELD_NAME)) {
			return MapBuilder.build(FIELD_NAME, extractSingleParam(conditionParams, FIELD_NAME));
		}

		return MapBuilder.build(null,null);
	}
	
	private String getNombreCampo(AbstractDescriptor descriptor) {
		if (!(descriptor instanceof ValidatorDescriptor)) {
			throw new IllegalArgumentException("Descriptor must be a ConditionDescriptor.");
		}

		ValidatorDescriptor validatorDescriptor = (ValidatorDescriptor) descriptor;

		String field = (String) validatorDescriptor.getArgs().get(FIELD_NAME);
		if (field != null && field.trim().length() > 0)
			return field;
		else
			return NOT_DEFINED;
	}
}
