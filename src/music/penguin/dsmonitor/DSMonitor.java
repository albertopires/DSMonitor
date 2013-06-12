package music.penguin.dsmonitor;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import javax.management.MBeanServerConnection;
import javax.management.ObjectInstance;
import javax.management.ObjectName;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.sql.DataSource;

/**
 * Servlet implementation class EclipseServlet
 */
public class DSMonitor extends HttpServlet {
	private static final long	serialVersionUID	= 1L;

	/**
	 * @see HttpServlet#HttpServlet()
	 */
	public DSMonitor() {
		super();
	}

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {

		response.setContentType("text/html;charset=UTF-8");

		PrintWriter out = response.getWriter();
		try {
			out.println("<html>");
			out.println("<head>");
			out.println("<title>Data Source Monitor</title>");
			out.println("<link rel=\"stylesheet\" type=\"text/css\" href=\"dsmonitor.css\" />");
			out.println("</head>");
			out.println("<body>");
			out.println("<h1>Servlet DSMonitor (2011091501) at " + request.getContextPath()
					+ "</h1>");
			out.println("<pre>");
			out.println("" + new Date());
			out.println("</pre>");
			String table = listDs();
			response.setHeader("dsfail", "false");
			if (table != null)
				if (table.length() > 0) {
					response.setHeader("dsfail", "true");
					out.println("<table border=1>");
					out.println("<tr><th>DS</th><th>Exception</th></tr>");
					out.println(table);
					out.println("</table>");
				}
			out.println("<pre>");
			out.println("" + new Date());
			out.println("</pre>");
			out.println("</body>");
			out.println("</html>");
		} finally {
			out.close();
		}
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
	}

	public static String listDs() {
		try {
			InitialContext ctx = new InitialContext();
			MBeanServerConnection server;
			server = (MBeanServerConnection) ctx.lookup("jmx/invoker/RMIAdaptor");
			Set<?> o;
			o = server.queryMBeans(new ObjectName("jboss.jca:*,service=ManagedConnectionPool"),
					null);
			System.out.println("Count : " + o.size());
			ObjectInstance objins;
			Iterator<?> i = o.iterator();
			StringBuilder sb = new StringBuilder();
			//
			ExecutorService executor = Executors.newSingleThreadExecutor();
			Task task = new Task();
			while (i.hasNext()) {
				objins = (ObjectInstance) i.next();
				System.err.println("Data : " + objins.getObjectName());
				String[] st = objins.getObjectName().toString().split("=");
				String dsStr = st[st.length - 1];
				System.out.println("Name: " + dsStr);
				if( dsStr.matches("JmsXA")) continue;
				String result = testDs(dsStr, ctx, executor, task);
				if (result != null) {
					result = result.replace(';', '\n');
					sb.append("<tr><td>");
					sb.append(dsStr);
					sb.append("</td><td>");
					sb.append(result);
					sb.append("</td></tr>");
				}
			}
			executor.shutdown();
			return sb.toString();
		} catch (Exception e) {
			System.err.println("Exception : " + e.getMessage());
			return null;
		}
	}

	public static String testDs(String dsStr, Context initCtx, ExecutorService executor, Task task) {
		try {
			DataSource ds = (DataSource) initCtx.lookup("java:" + dsStr);
			task.setDs(ds);
			executor.invokeAll(Arrays.asList(task), 1, TimeUnit.SECONDS);
			if (task.isFail())
				return task.getMsg();
			return null;
		} catch (NamingException e) {
			System.err.println("NamingException : " + e.getMessage());
			return e.getMessage();
		} catch (Exception e) {
			System.err.println("Exception : " + e.getMessage());
			if (executor != null)
				executor.shutdown();
			return e.getMessage();
		}
	}
}

class Task implements Callable<String> {
	private DataSource	ds;
	private String		msg;
	private boolean		fail;

	public Task() {
		this.fail = true;
		this.msg = null;
	}

	public Task(DataSource ds) {
		this.ds = ds;
		this.fail = true;
		this.msg = null;
	}

	public String call() {
		try {
			System.out.println("Started..");
			Connection con = ds.getConnection();
			con.close();
			System.out.println("Finished!");
		} catch (SQLException ex) {
			msg = ex.getMessage();
			fail = true;
		}
		return null;
	}

	public void setDs(DataSource ds) {
		this.ds = ds;
		this.fail = true;
		this.msg = null;
	}

	public boolean isFail() {
		return fail;
	}

	public String getMsg() {
		return msg;
	}
}
