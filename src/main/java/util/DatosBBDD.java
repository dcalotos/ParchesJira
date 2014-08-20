package util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;

public class DatosBBDD {
	String strUsuario = "jiradbpluginuser";
	String strPassword = "jiradbpluginuser";
	String strBaseDatos = "jiradbplugin";
	String strHost = "lerida";

	String strConsultaSQL;
	Connection conn = null;
	Statement stmt;
	ResultSet rs;

	public DatosBBDD() {
	}

	private boolean ConectarServidor() throws Exception {
		try {
			Class.forName("net.sourceforge.jtds.jdbc.Driver");
			String urlBD = "sqlserver://" + strHost + "/" + strBaseDatos + ";";
			// conn = DriverManager.getConnection("jdbc:jtds:" + urlBD + "user=" + strUsuario + ";password=" +
			// strPassword + ";instance=sqlexpress");
			conn = DriverManager.getConnection("jdbc:jtds:" + urlBD + "user=" + strUsuario + ";password=" + strPassword
					+ ";");
			if (conn != null) {
				stmt = conn.createStatement();
				return true;
			} else
				return false;
		} catch (SQLException e) {
			e.printStackTrace();
			return false;
		}
	}

	private boolean ConectarServidorOracle() throws Exception {
		try {
			Class.forName("oracle.jdbc.driver.OracleDriver");
			// conn = DriverManager.getConnection("jdbc:oracle:thin:@ " +
			// "(description = (load_balance = yes)(failover = on)" + "(address_list ="
			// + "(address = (protocol = tcp)(host = PHXDB-PRE1vip.wke.es)(port = 1521))"
			// + "(address = (protocol = tcp)(host = PHXDB-PRE2vip.wke.es)(port = 1521)))" +
			// "(connect_data =(service_name = PHXEPRE)"
			// + "(FAILOVER_MODE =(TYPE = select)" + "(METHOD = basic)" + "(RETRIES = 10)" + "(DELAY = 3))))",
			// "CONEXECA", "hG3!ddz37a");
			conn = DriverManager.getConnection("jdbc:oracle:thin:@" + "(DESCRIPTION = "
					+ "(ADDRESS = (PROTOCOL = TCP)(HOST = phxdbe-pro1vip.wke.es)(PORT = 1521)) "
					+ "(ADDRESS = (PROTOCOL = TCP)(HOST = phxdbe-pro2vip.wke.es)(PORT = 1521)) "
					+ "(LOAD_BALANCE = yes) " + "(CONNECT_DATA = " + "(SERVER = DEDICATED) "
					+ "(SERVICE_NAME = phxepro) " + "(FAILOVER_MODE = " + "(TYPE = SELECT) " + "(METHOD = BASIC) "
					+ "(RETRIES = 10) " + "(DELAY = 3) " + ") " + ") " + ")", "CONEXECA", "hG3!ddz37a");

			if (conn != null) {
				stmt = conn.createStatement();
				return true;
			} else
				return false;
		} catch (SQLException e) {
			e.printStackTrace();
			return false;
		}
	}

	// Cierra objeto Resultset
	public void cerrar(ResultSet rs) {
		try {
			rs.close();
		} catch (Exception ex) {
		}
	}

	// Cierra objeto Statement
	public void cerrar(Statement st) {
		try {
			st.close();
		} catch (Exception ex) {
		}
	}

	// Cierra objeto Connection
	public void cerrar(Connection con) {
		try {
			con.close();
		} catch (Exception ex) {
		}
	}

	public int ObtencionPesoTipoProblema(String tipoProblema) {
		int pesoProblema = 1;
		String problema = "";
		try {
			if (ConectarServidor()) {
				problema = tipoProblema.replace("'", "''");
				rs = stmt.executeQuery("select tippro_peso from pt_tipo_problema where tippro_problema='" + problema
						+ "'");
				if (rs.next()) {
					pesoProblema = rs.getInt(1);
				}
				cerrar(rs);
				cerrar(stmt);
			}
		} catch (Exception e) {
			pesoProblema = 1;
		} finally {
			if (conn != null) {
				cerrar(conn);
			}
		}
		return pesoProblema;
	}

	public int ObtencionPesoProductoOnline(String productoOnline) {
		String producto = "";
		int pesoProducto = 1;
		try {
			if (ConectarServidor()) {
				producto = productoOnline.replace("'", "''");
				rs = stmt.executeQuery("select pro_peso from pt_producto where pro_producto='" + producto + "'");
				if (rs.next()) {
					pesoProducto = rs.getInt(1);
				}
				cerrar(rs);
				cerrar(stmt);
			}
		} catch (Exception e) {
			pesoProducto = 1;
		} finally {
			if (conn != null) {
				cerrar(conn);
			}
		}
		return pesoProducto;
	}

	public String ObtencionManagerProductoOnline(String producto) {
		String productoOnline = "";
		String managerProducto = "lgontier";
		try {
			if (ConectarServidor()) {
				productoOnline = producto.replace("'", "''");
				rs = stmt.executeQuery("select pro_product_manager from pt_producto where pro_producto='"
						+ productoOnline + "'");
				if (rs.next()) {
					managerProducto = rs.getString(1);
				}
				cerrar(rs);
				cerrar(stmt);
			}
		} catch (Exception e) {
			managerProducto = "lgontier";
		} finally {
			if (conn != null) {
				cerrar(conn);
			}
		}
		return managerProducto;
	}

	public String ObtencionManagerTipoProblemaFrancia(String tipoProblema) {
		String problema = "";
		String managerProducto = "lgontier";
		try {
			if (ConectarServidor()) {
				problema = tipoProblema.replace("'", "''");
				rs = stmt.executeQuery("select tippro_asignado_wkfrance from pt_tipo_problema where tippro_problema='"
						+ problema + "'");
				if (rs.next()) {
					managerProducto = rs.getString(1);
				}
				cerrar(rs);
				cerrar(stmt);
			}
		} catch (Exception e) {
			managerProducto = "lgontier";
		} finally {
			if (conn != null) {
				cerrar(conn);
			}
		}
		return managerProducto;
	}

	public String ObtencionResponsableUdNFrancia(String UdN) {
		String responsable = "lgontier";
		try {
			if (ConectarServidor()) {
				rs = stmt.executeQuery("select pro_asignado_wkfrance from pt_producto where pro_udn='" + UdN + "'");
				if (rs.next()) {
					responsable = rs.getString(1);
				}
				cerrar(rs);
				cerrar(stmt);
			}
		} catch (Exception e) {
			responsable = "lgontier";
		} finally {
			if (conn != null) {
				cerrar(conn);
			}
		}
		return responsable;
	}

	public String ObtencionUsuarioBackupArquitectura(String componente) {
		String usuarioBackup = "apascual";
		try {
			if (ConectarServidor()) {
				rs = stmt
						.executeQuery("select com_usuario_informado from pt_componente_arquitectura where com_componente='"
								+ componente + "'");
				if (rs.next()) {
					usuarioBackup = rs.getString(1);
				}
				cerrar(rs);
				cerrar(stmt);
			}
		} catch (Exception e) {
			usuarioBackup = "apascual";
		} finally {
			if (conn != null) {
				cerrar(conn);
			}
		}
		return usuarioBackup;
	}

	public String ObtencionUsuarioAdministradorConsultas(String issueType) {
		String reponsable = "adminwcj";
		try {
			if (ConectarServidor()) {
				rs = stmt.executeQuery("select coo_usuario from pt_coordinador_consultas where coo_tipo_issue='"
						+ issueType + "'");
				if (rs.next()) {
					reponsable = rs.getString(1);
				}
				cerrar(rs);
				cerrar(stmt);
			}
		} catch (Exception e) {
			reponsable = "adminwcj";
		} finally {
			if (conn != null) {
				cerrar(conn);
			}
		}
		return reponsable;
	}

	public String ObtencionResponsableSubmateria(String submateria) {
		String responsable = "adminwcj";
		try {
			if (ConectarServidor()) {
				rs = stmt.executeQuery("select cj_responsable from pt_responsable_submaterias where cj_submateria='"
						+ submateria + "'");
				if (rs.next()) {
					responsable = rs.getString(1);
				}
				cerrar(rs);
				cerrar(stmt);
			}
		} catch (Exception e) {
			responsable = "adminwcj";
		} finally {
			if (conn != null) {
				cerrar(conn);
			}
		}
		return responsable;
	}

	public boolean ObtencionBOPermiso(String aplicacion, String modulo, String usuario, String issueType) {
		boolean permisoOK = false;

		try {
			if (ConectarServidor()) {
				if (modulo != "") {
					rs = stmt
							.executeQuery("SELECT pe.id FROM bo_permissions pe WHERE pe.bo_id_app= (SELECT ap.id FROM bo_applications ap WHERE ap.bo_app_name='"
									+ aplicacion
									+ "' AND ap.bo_app_module='"
									+ modulo
									+ "') AND pe.bo_id_isstyp=(SELECT it.id FROM bo_issue_type it WHERE it.bo_issue_type='"
									+ issueType
									+ "') AND pe.bo_id_user=(SELECT us.id FROM bo_users us WHERE us.bo_users = '"
									+ usuario + "')");
				} else {
					rs = stmt
							.executeQuery("SELECT pe.id FROM bo_permissions pe WHERE pe.bo_id_app= (SELECT ap.id FROM bo_applications ap WHERE ap.bo_app_name='"
									+ aplicacion
									+ "') AND pe.bo_id_isstyp=(SELECT it.id FROM bo_issue_type it WHERE it.bo_issue_type='"
									+ issueType
									+ "') AND pe.bo_id_user=(SELECT us.id FROM bo_users us WHERE us.bo_users = '"
									+ usuario + "')");
				}
				if (rs.next()) {
					permisoOK = true;
				}
				cerrar(rs);
				cerrar(stmt);
			}
		} catch (Exception e) {
			permisoOK = false;
		} finally {
			if (conn != null) {
				cerrar(conn);
			}
		}
		return permisoOK;
	}

	public String ObtencionValidador(String informante) {
		String responsable = "";
		try {
			if (ConectarServidor()) {
				rs = stmt.executeQuery("select gdv_validador from gdv_validators where gdv_peticionario ='"
						+ informante + "'");
				if (rs.next()) {
					responsable = rs.getString(1);
				}
				cerrar(rs);
				cerrar(stmt);
			}
		} catch (Exception e) {
			responsable = "adminjira";
		} finally {
			if (conn != null) {
				cerrar(conn);
			}
		}
		return responsable;
	}

	public ArrayList<String> ObtencionDirector(String informante) {
		ArrayList<String>  responsable = new ArrayList<String>();
		try {
			if (ConectarServidor()) {
				rs = stmt.executeQuery("select gdv_director,gdv_validador from gdv_validators where gdv_peticionario ='"
						+ informante + "'");
				if (rs.next()) {
					responsable.add(rs.getString(1));
					responsable.add(rs.getString(2));
				}
				cerrar(rs);
				cerrar(stmt);
			}
		} catch (Exception e) {
			responsable.add("adminjira");
		} finally {
			if (conn != null) {
				cerrar(conn);
			}
		}
		return responsable;
	}

	public ArrayList<String> ObtencionDatosPhoenix(String CUC) {
		ArrayList<String> datosBBDD = new ArrayList<String>();
		String CLIUBICON[];
		try {
			if (ConectarServidorOracle()) {
				CLIUBICON = CUC.split("-");
				rs = stmt
						.executeQuery("SELECT F005_TELEFONO AS TELEFONO, P027_CODIPOST AS CODPOSTAL, NOM_PROV AS PROVINCIA, F002_NOMBCLIE AS AYUNTAMIENTO, F002_VACARCON AS VCC, CLASE, P026_HABITANTES AS HABITANTES "
								+ "FROM V_CONSULTOR_AYUNTAMIENTOS "
								+ "WHERE F002_CLIENTE = "
								+ CLIUBICON[0]
								+ " AND F003_UBICACIO = " + CLIUBICON[1] + " AND F004_CONTACTO= " + CLIUBICON[2] + "");
				if (rs.next()) {
					datosBBDD.add(0, Long.toString(rs.getLong(1)));
					datosBBDD.add(1, rs.getString(2));
					datosBBDD.add(2, rs.getString(3));
					datosBBDD.add(3, rs.getString(4));
					datosBBDD.add(4, Long.toString(rs.getLong(5)));
					datosBBDD.add(5, rs.getString(6));
					datosBBDD.add(6, Long.toString(rs.getLong(7)));
				}
				cerrar(rs);
				cerrar(stmt);
			}
		} catch (Exception e) {
			datosBBDD.clear();
		} finally {
			if (conn != null) {
				cerrar(conn);
			}
		}
		return datosBBDD;
	}

	// PENDIENTE
	public int InsercionDatosBackOffice(String IdConsulta) {
		int insertOK = 0;

		try {
			if (ConectarServidorOracle()) {

				conn.setAutoCommit(false);
				Statement stmt = conn.createStatement();

				String SQL = "INSERT INTO .... VALUES (...)";
				stmt.executeUpdate(SQL);
				// Si no hay error
				conn.commit();
				insertOK = 1;
				cerrar(stmt);
			}
		} catch (SQLException se) {
			// Si hay algún error.
			try {
				conn.rollback();
			} catch (SQLException e) {
				e.printStackTrace();
			} finally {
				insertOK = 0;
			}
		} catch (Exception e) {
			insertOK = 0;
			e.printStackTrace();
		} finally {
			if (conn != null) {
				cerrar(conn);
			}
		}
		return insertOK;
	}
}
