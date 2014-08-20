package com.dc.jira.jira.workflow;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.config.util.AttachmentPathManager;
import com.atlassian.jira.issue.AttachmentManager;
import com.atlassian.jira.issue.CustomFieldManager;
import com.atlassian.jira.issue.ModifiedValue;
import com.atlassian.jira.issue.MutableIssue;
import com.atlassian.jira.issue.attachment.Attachment;
import com.atlassian.jira.issue.fields.CustomField;
import com.atlassian.jira.issue.util.DefaultIssueChangeHolder;
import com.atlassian.jira.issue.util.IssueChangeHolder;
import com.atlassian.jira.util.PathUtils;
import com.atlassian.jira.workflow.function.issue.AbstractJiraFunctionProvider;
import com.opensymphony.module.propertyset.PropertySet;
import com.opensymphony.workflow.InvalidInputException;
import com.opensymphony.workflow.WorkflowException;

@SuppressWarnings("unused")
public class WCJExportarConsultasBDE extends AbstractJiraFunctionProvider {
	private static final Logger log = LoggerFactory.getLogger(WCJExportarConsultasBDE.class);

	public static final String USER_ADMIN = "userAdmin";

	private static String DIRECTORIO = "";

	private static String datosXML = "";
	private static String dateOut = "";
	private static String datosConfirmacionInsercion = "";

	// Datos a exportar
	private static String sumario = "";
	private static String materia = "";
	private static String submateria = "";
	private static String materiaSubmateria = "";
	private static String productoOnline = "";
	private static String clave = "";

	private static String filename = "";
	private static String fileId = "";

	private static String Proyecto = "";

	@SuppressWarnings("rawtypes")
	public void execute(Map transientVars, Map args, PropertySet ps) throws WorkflowException {
		String urlUpdate = "";
		String url = "";
		int statusCode = 0;

		log.warn("Inicio Proceso Exportación a BDE...");
		MutableIssue mutableIssue = getIssue(transientVars);
		log.warn("MutableIssue: " + mutableIssue.getKey());

		DIRECTORIO = (String) args.get("directorio");
		log.warn("DIRECTORIO: " + DIRECTORIO);
		log.warn("Inicio Proceso Exportación a BDE...");

		Proyecto = mutableIssue.getProjectObject().getKey();
		CustomFieldManager customFieldManager = ComponentAccessor.getCustomFieldManager();
		clave = mutableIssue.getKey();
		log.warn("clave: " + clave);
		sumario = mutableIssue.getSummary();
		log.warn("sumario: " + sumario);

		// Comprobar si hay fichero respuesta.doc
		// Sí existe se procesa y se envía el fichero word

		AttachmentManager attchMgr = ComponentAccessor.getAttachmentManager();
		AttachmentPathManager pathManager = ComponentAccessor.getAttachmentPathManager();
		List<Attachment> attchments = attchMgr.getAttachments(mutableIssue);
		if (!attchments.isEmpty()) {
			for (Attachment attachment : attchments) {
				String filePath = PathUtils.joinPaths(pathManager.getAttachmentPath(), mutableIssue.getProjectObject()
						.getKey(), mutableIssue.getKey(), attachment.getId().toString());
				File atFile = new File(filePath);
				if (atFile.exists() && attachment.getString("filename").toLowerCase().contains("respuesta.doc")) {
					try {
						if (atFile.canRead()) {
							File destino = new File(DIRECTORIO + "word\\Respuesta_" + clave + ".doc");
							log.warn("destino: " + destino);
							if (!destino.exists()) {
								try{
								InputStream inOrigen = new FileInputStream(atFile);
								OutputStream outDestino = new FileOutputStream(destino);
								log.warn("Copiando destino");
								byte[] buf = new byte[1024];
								int len;
								while ((len = inOrigen.read(buf)) > 0) {
									outDestino.write(buf, 0, len);
								}
								inOrigen.close();
								outDestino.close();
								}catch (Exception e){
									log.warn("Fichero NO copiado a destino: " + destino);
								}
								log.warn("Fichero copiado a destino");
							} else {
								dateOut = dateOut + "Fichero Respuesta.doc ya existe. ";
								log.warn("dateOut");
							}
						}
					} catch (SecurityException se) {
						log.warn("Could not read attachment file. Not copying. (${se.message})");
						throw new InvalidInputException("<B>Error en la exportación a BDE.</B><BR />"
								+ "Póngase en contacto con el Administrador de JIRA.");
					}
					
					clave = mutableIssue.getKey();
					log.warn("clave: " + clave);
					sumario = mutableIssue.getSummary();
					log.warn("sumario: " + sumario);
					productoOnline = customFieldManager.getCustomFieldObjectByName("Producto online")
							.getValue(mutableIssue).toString();
					log.warn("productoOnline: " + productoOnline);
					materiaSubmateria = customFieldManager.getCustomFieldObjectByName("Materia y submateria")
							.getValue(mutableIssue).toString();
					if (materiaSubmateria != "") {
						materia = materiaSubmateria.substring(materiaSubmateria.indexOf("=") + 1,
								materiaSubmateria.indexOf(","));
						submateria = materiaSubmateria.substring(materiaSubmateria.indexOf("1=") + 2,
								materiaSubmateria.indexOf("}"));
					}
					log.warn("materia - submateria: [" + materia + "] - [" + submateria + "]");

					datosXML = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>";
					datosXML = datosXML + "<DatosConsulta>";
					datosXML = datosXML + "<clave>" + clave + "</clave>";
					datosXML = datosXML + "<sumario>" + sumario + "</sumario>";
					datosXML = datosXML + "<productoOnline>" + productoOnline + "</productoOnline>";
					datosXML = datosXML + "<submateria>" + submateria + "</submateria>";
					datosXML = datosXML + "</DatosConsulta>";
					
					log.warn("datosXML: " + datosXML);

					String DirectorioXML = DIRECTORIO + "xml";
					log.warn(DirectorioXML);

					dateOut = "";
					File ficheroXML = new File(DirectorioXML, "JIRA_" + clave + "_ExportBDE.xml");
					try {
						if (!ficheroXML.exists()) {
							FileOutputStream fos = new FileOutputStream(ficheroXML);
							Writer out = new OutputStreamWriter(fos, "UTF8");
							out.write(datosXML);
							out.close();
							log.warn("Escrito Fichero xml...");
						} else {
							dateOut = "Fichero XML ya existe. ";
							log.warn(dateOut);
						}
						if (!dateOut.contains("ya existe")) {
							dateOut = new SimpleDateFormat("EEEE, d MMM yyyy HH:mm:ss").format(new Date());
							datosConfirmacionInsercion = "Enviada a BDE el " + dateOut;
						} else {
							datosConfirmacionInsercion = dateOut;
						}
						log.warn("dateOut: " + dateOut);

						log.warn("Guardando Exportada a BDE");
						CustomField exportadaBDE = ComponentAccessor.getCustomFieldManager()
								.getCustomFieldObjectByName("Exportada a BDE");
						IssueChangeHolder changeHolder = new DefaultIssueChangeHolder();
						exportadaBDE.updateValue(null, mutableIssue,
								new ModifiedValue(mutableIssue.getCustomFieldValue(exportadaBDE),
										datosConfirmacionInsercion), changeHolder);
						log.warn("Guardado el campo de la tarea Exportada a BDE");
					} catch (IOException ioe) {
						log.warn("Error: " + ioe.getMessage());
						throw new InvalidInputException("<B>Error en la exportación a BDE.</B><BR />"
								+ "Póngase en contacto con el Administrador de JIRA.");
					} catch (Exception e) {
						log.warn("Error: " + e.getMessage());
						throw new InvalidInputException("<B>Error en la exportación a BDE.</B><BR />"
								+ "Póngase en contacto con el Administrador de JIRA.");
					}
					break;
				}// END IF Respuesta

			}// END FOR ATTACH
		}
	}
}
