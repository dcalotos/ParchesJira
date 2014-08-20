package com.dc.jira.jira.workflow;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.config.util.AttachmentPathManager;
import com.atlassian.jira.issue.AttachmentManager;
import com.atlassian.jira.issue.CustomFieldManager;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.ModifiedValue;
import com.atlassian.jira.util.PathUtils;
import com.atlassian.jira.web.util.AttachmentException;
import com.atlassian.jira.workflow.function.issue.AbstractJiraFunctionProvider;
import com.opensymphony.module.propertyset.PropertySet;
import com.opensymphony.workflow.InvalidInputException;
import com.opensymphony.workflow.WorkflowException;

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

import javax.naming.AuthenticationException;

import com.atlassian.jira.issue.MutableIssue;
import com.atlassian.jira.issue.attachment.Attachment;
import com.atlassian.jira.issue.fields.CustomField;
import com.atlassian.jira.issue.util.DefaultIssueChangeHolder;
import com.atlassian.jira.issue.util.IssueChangeHolder;

@SuppressWarnings("unused")
public class CECExportarConsultas extends AbstractJiraFunctionProvider {
	private static final Logger log = LoggerFactory.getLogger(CECExportarConsultas.class);

	public static final String USER_ADMIN = "userAdmin";
	// Recuperar con REST las consultas a exportar

	private static String DIRECTORIO = "";

	private static String datosXML = "";
	private static String dateOut = "";
	private static String datosConfirmacionInsercion = "";

	// Datos a exportar
	private static String sumario = "";
	private static String titulo = "";
	private static String materia = "";

	private static String planteamientoDefinitivo = "";
	private static String respuestaFinal = "";

	private static String productoOnline = "";
	private static String clave = "";

	private static String materiaEspecialista = "";
	private static String especialista = "";
	private static String responsable = "";

	private static String clase = "";
	private static String habitantes = "";
	private static String ayuntamiento = "";
	private static String vcc = "";
	private static String cargo = "";

	private static String filename = "";
	private static String fileId = "";

	private static String Proyecto = "";

	@SuppressWarnings("rawtypes")
	public void execute(Map transientVars, Map args, PropertySet ps) throws WorkflowException {
		String urlUpdate = "";
		String url = "";
		int statusCode = 0;
		boolean ficRespuesta = false;

		try {
			DIRECTORIO = (String) args.get("directorio");
			log.warn("DIRECTORIO: " + DIRECTORIO);

			log.warn("Inicio Proceso Exportación a BDE...");
			MutableIssue mutableIssue = getIssue(transientVars);
			log.warn("MutableIssue: " + mutableIssue.getKey());
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
					String filePath = PathUtils.joinPaths(pathManager.getAttachmentPath(), mutableIssue
							.getProjectObject().getKey(), mutableIssue.getKey(), attachment.getId().toString());
					File atFile = new File(filePath);

					if (atFile.exists() && attachment.getString("filename").toLowerCase().contains("respuesta.doc")) {
						try {
							if (atFile.canRead()) {
								File destino = new File(DIRECTORIO + "word\\Respuesta_" + clave + ".doc");
								log.warn("destino: " + destino);
								if (!destino.exists()) {
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
									log.warn("Fichero copiado a destino");
									ficRespuesta = true;
									break;
								} else {
									dateOut = dateOut + "Fichero Respuesta.doc ya existe. ";
									log.warn("dateOut");
								}
							}
						} catch (SecurityException se) {
							System.out.println("Could not read attachment file. Not copying. (${se.message})");
						}
					} else {
						System.out.println("Attachment file does not exist where it should. Not copying.");
					}
				}
			}
			// Comprobar si no hay fichero respuesta.doc
			// Sí existe se envía un fichero word vacío y el Planteamiento final
			// y la Respuesta final van el el xml de la tarea JIRA
			if (!ficRespuesta) {
				File origen;
				if (DIRECTORIO.contains("Java")) {
					origen = new File(DIRECTORIO + "RespuestaVacia.doc");
				} else {
					origen = new File("\\\\iehreditor02\\WKF\\Productos\\ConsultasJira\\xml\\RespuestaVacia.doc");
				}
				// File origen = new File("\\\\iehreditor02\\WKF\\Productos\\ConsultasJira\\xml\\RespuestaVacia.doc");
				log.warn("origen: " + origen);
				File destino = new File(DIRECTORIO + "word\\RespuestaVacia_" + clave + ".doc");
				log.warn("destino: " + destino);
				if (!destino.exists()) {
					InputStream inOrigen = new FileInputStream(origen);
					OutputStream outDestino = new FileOutputStream(destino);
					log.warn("Copiando destino");
					byte[] buf = new byte[1024];
					int len;
					while ((len = inOrigen.read(buf)) > 0) {
						outDestino.write(buf, 0, len);
					}
					inOrigen.close();
					outDestino.close();
					log.warn("Fichero copiado a destino");
				} else {
					dateOut = dateOut + "Fichero RespuestaVacia.doc ya existe. ";
					log.warn("dateOut");
				}
			}

			titulo = customFieldManager.getCustomFieldObjectByName("Title")
					.getValue(mutableIssue).toString();
			log.warn("titulo: " + titulo);
			
			planteamientoDefinitivo = customFieldManager.getCustomFieldObjectByName("Planteamiento definitivo")
					.getValue(mutableIssue).toString();
			log.warn("planteamientoDefinitivo: " + planteamientoDefinitivo);

			respuestaFinal = customFieldManager.getCustomFieldObjectByName("Respuesta final").getValue(mutableIssue)
					.toString();
			log.warn("respuestaFinal: " + respuestaFinal);

			productoOnline = customFieldManager.getCustomFieldObjectByName("Producto online").getValue(mutableIssue)
					.toString();
			log.warn("productoOnline: " + productoOnline);

			materiaEspecialista = customFieldManager.getCustomFieldObjectByName("Materia - Especialista")
					.getValue(mutableIssue).toString();
			log.warn("materia - especialista: [" + materiaEspecialista + "]");
			if (materiaEspecialista != "") {
				materia = materiaEspecialista.substring(materiaEspecialista.indexOf("=") + 1,
						materiaEspecialista.indexOf(","));
				log.warn("materia: [" + materia + "]");
				responsable = materiaEspecialista.substring(materiaEspecialista.indexOf("1=") + 2,
						materiaEspecialista.indexOf("}"));
				log.warn("responsable: [" + responsable + "]");
				especialista = responsable.substring(responsable.indexOf("(") + 1, responsable.length() - 1);
				log.warn("especialista: [" + especialista + "]");
			}
			log.warn("materia - especialista: [" + materia + "] - [" + especialista + "]");

			ayuntamiento = customFieldManager.getCustomFieldObjectByName("Ayuntamiento").getValue(mutableIssue)
					.toString();
			log.warn("ayuntamiento: " + ayuntamiento);

			clase = customFieldManager.getCustomFieldObjectByName("Clase").getValue(mutableIssue).toString();
			log.warn("clase: " + clase);

			habitantes = customFieldManager.getCustomFieldObjectByName("Número de habitantes población")
					.getValue(mutableIssue).toString();
			log.warn("habitantes: " + habitantes);

			vcc = customFieldManager.getCustomFieldObjectByName("Valor de cartera").getValue(mutableIssue).toString();
			log.warn("vcc: " + vcc);

			cargo = customFieldManager.getCustomFieldObjectByName("Cargo").getValue(mutableIssue).toString();
			log.warn("cargo: " + cargo);

			datosXML = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>";
			datosXML = datosXML + "<DatosConsulta>";
			datosXML = datosXML + "<clave>" + clave + "</clave>";
			datosXML = datosXML + "<sumario>" + sumario + "</sumario>";
			datosXML = datosXML + "<titulo>" + titulo + "</titulo>";
			datosXML = datosXML + "<planteamientoDefinitivo>" + planteamientoDefinitivo + "</planteamientoDefinitivo>";
			datosXML = datosXML + "<respuestaFinal>" + respuestaFinal + "</respuestaFinal>";
			datosXML = datosXML + "<productoOnline>" + productoOnline + "</productoOnline>";
			datosXML = datosXML + "<materia>" + materia + "</materia>";
			datosXML = datosXML + "<especialista>" + especialista + "</especialista>";
			datosXML = datosXML + "<ayuntamiento>" + ayuntamiento + "</ayuntamiento>";
			datosXML = datosXML + "<clase>" + clase + "</clase>";
			datosXML = datosXML + "<habitantes>" + habitantes + "</habitantes>";
			datosXML = datosXML + "<vcc>" + vcc + "</vcc>";
			datosXML = datosXML + "<cargo>" + cargo + "</cargo>";
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
				CustomField exportadaBDE = ComponentAccessor.getCustomFieldManager().getCustomFieldObjectByName(
						"Exportada a BDE");
				IssueChangeHolder changeHolder = new DefaultIssueChangeHolder();
				exportadaBDE.updateValue(null, mutableIssue,
						new ModifiedValue(mutableIssue.getCustomFieldValue(exportadaBDE), datosConfirmacionInsercion),
						changeHolder);
				log.warn("Guardado el campo de la tarea Exportada a BDE");
			} catch (IOException ioe) {
				log.warn("Error: " + ioe.getMessage());
				ioe.printStackTrace();
			} catch (Exception e) {
				log.warn("Error: " + e.getMessage());
				e.printStackTrace();
			}
		} catch (Exception e) {
			log.warn("Error: " + e.getMessage());
			throw new InvalidInputException("<B>Error en la exportación a BDE.</B><BR />"
					+ "Póngase en contacto con el Administrador de JIRA.");
		} finally {
			log.warn("Finalización Proceso Exportación a BDE...");
		}
	}
}