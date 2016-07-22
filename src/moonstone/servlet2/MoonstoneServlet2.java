package moonstone.servlet2;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.Hashtable;
import java.util.Vector;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;

import moonstone.annotation.Annotation;
import moonstone.grammar.Grammar;
import moonstone.rulebuilder.MoonstoneRuleInterface;
import tsl.knowledge.engine.FileIO;

/**
 * Servlet implementation class MoonstoneServlet2
 */
@WebServlet("/MoonstoneServlet2")
public class MoonstoneServlet2 extends HttpServlet {
	protected Tables tables = null;
	protected MoonstoneRuleInterface moonstone = null;
	// private Hashtable<String, String> uidhash = new Hashtable();
	private static final long serialVersionUID = 1L;
	// private int userIDCounter = 0;

	/**
	 * @see HttpServlet#HttpServlet()
	 */
	public MoonstoneServlet2() {
		super();
		// TODO Auto-generated constructor stub
	}

	public void init(ServletConfig config) throws ServletException {
		this.tables = new Tables(this);
		if (this.moonstone == null) {
			this.moonstone = MoonstoneRuleInterface.createMoonstoneRuleBuilder(false, false);
		}
	}

	public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		response.setContentType("text/html");

		// String userID = getUserID(request);
		// if (userID == null) {
		// userID = "Moonstone" + String.valueOf(this.userIDCounter++);
		// Cookie cookie = new Cookie("userID", userID);
		// response.addCookie(cookie);
		// }

		String docType = "<!doctype html public \"-//w3c//dtd html 4.0 " + "transitional//en\">\n";

		String head = "<head>"
				+ "<meta http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\"><title>Moonstone</title>"
				// + "<script type=\"text/javascript\"
				// src=\"Moonstone.js\"></script>"
				+ "</head>";

		String grammarstr = "";
		for (String gname : this.moonstone.getGrammarDirectoryNames()) {
			String checked = "";
			if (this.moonstone.getCurrentGrammar().getName().contains(gname)) {
				checked = "checked";
			}
			grammarstr += "<input type=\"radio\" name=\"grammar\" value=\"" + gname + "\" " + checked + ">" + gname
					+ "</input><br>";
		}
		grammarstr += "<br>";

		String body = "<body>" + "<form id=\"textForm\" action=\"\" method=\"post\" accept-charset=UTF-8 >"
				+ "<input type=\"radio\" name=\"viewType\" value=\"Concepts\">Lowest Annotations with Dictionary CUIs/ Grammar Concepts</input><br>"
				+ "<input type=\"radio\" name=\"viewType\" value=\"CUI\">Lowest Annotations with Dictionary CUIs Only</input><br>"
				+ "<input type=\"radio\" name=\"viewType\" value=\"Parsetrees\" checked>Full Parsetrees</input><br><br>"

				+ "<input type=\"checkbox\" name=\"allLevels\" value=\"AllLevels\" checked>All Parsetree Levels</input><br><br><br>"

				+ grammarstr

				+ "<input type=\"checkbox\" name=\"useDictionary\">Use UMLS Dictionary</input><br><br>"

				+ "Medical Text:<br>"
				+ "<textarea name=\"doctext\" id=\"doctext\" cols=\"80\" rows=\"20\">Complains of cough and fever, temperature above 101</textarea><br>"
				+ "<input type=\"submit\" value=\"Submit Text\"></form><br>";

		body += "</body>";

		PrintWriter out = response.getWriter();
		String title = "Moonstone";

		String output = docType + "<html>" + head + body + "</html>";
		out.println(output);
		out.close();

	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse
	 *      response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		response.setContentType("text/html");
		PrintWriter out = response.getWriter();

		String docType = "<!doctype html public \"-//w3c//dtd html 4.0 " + "transitional//en\">\n";

		String head = "<meta http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\"><title>Moonstone</title>";
		// + "<script type=\"text/javascript\" src=\"Moonstone.js\"></script>"

		String body = "No processing performed.";
		String text = request.getParameter("doctext");
		String fstr = this.getFileString(request);
		String viewtype = request.getParameter("viewType");
		String useDictionaryString = request.getParameter("useDictionary");
		String allLevelsString = request.getParameter("allLevels");
		boolean useDictionary = ("on".equals(useDictionaryString) ? true : false);
		boolean conceptsOnly = ("Concepts".equals(viewtype));
		boolean cuiOnly = ("CUI".equals(viewtype));
		boolean allLevels = ("AllLevels".equals(allLevelsString));
		
		String gname = request.getParameter("grammar").toString();
		this.moonstone.changeGrammarDirectoryName(gname);
		
		this.moonstone.setIsUseIndexFinder(useDictionary);
		
		if (text != null) {
			body = this.getAnnotationResults(text, conceptsOnly, cuiOnly, useDictionary, allLevels);
		} else if (fstr != null) {
			body = "File received.";
		}

		String title = "Moonstone";
		String style = "<STYLE TYPE=\"text/css\"><!--TD{font-family: Arial; font-size: 10pt;}---></STYLE>";
		String aref = "<a href=\"http://blutc-dev.chpc.utah.edu/MoonstoneServlet2/MoonstoneServlet2\">Return to main</a>";
		// String aref = "<a
		// href=\"http://blutc-dev.chpc.utah.edu/MoonstoneServlet2/MoonstoneServlet2\">Return
		// to main</a>";
		head = "<head>" + head + "</head>";
		body = "<body>" + aref + body + "</body>";
		String output = docType + "<html>" + head + body + "</html>";
		out.println(output);
		out.close();
	}

	private String getAnnotationResults(String text, boolean conceptsOnly, boolean cuiOnly, boolean useDictionary,
			boolean allLevels) {
		String body = "";
		if (text != null) {

			String useDictionaryString = (useDictionary ? "true" : "false");
			this.moonstone.getKnowledgeEngine().getStartupParameters().setPropertyValue(FileIO.LoadUMLS,
					useDictionaryString);
			Vector<Annotation> annotations = this.moonstone.applyNarrativeGrammarToText(null, text, true, true, true);
			if (annotations != null) {
				body = tables.extractAnnotationsIntoHTMLTable(annotations, conceptsOnly, cuiOnly, allLevels);
			}
		}
		return body;
	}

	// private String getUserID(HttpServletRequest request) {
	// Cookie[] cookies = request.getCookies();
	// String userID = null;
	// if (cookies != null) {
	// for (Cookie cookie : cookies) {
	// if ("userID".equals(cookie.getName())) {
	// userID = cookie.getValue();
	// break;
	// }
	// }
	// }
	// return userID;
	// }

	// private boolean userIsAuthenticated(String uid) {
	// return uid != null && this.uidhash.get(uid) != null;
	// }

	private String getFileString(HttpServletRequest request) {
		StringBuffer sb = new StringBuffer();
		try {
			Part filePart = request.getPart("rulefile");
			if (filePart != null) {
				String fileName = getFileName(filePart);
				InputStream filecontent = filePart.getInputStream();
				int read = 0;
				final byte[] bytes = new byte[1024];

				while ((read = filecontent.read(bytes)) != -1) {
					int x = 1;
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return sb.toString();
	}

	private String getFileName(Part part) {
		if (part != null) {
			final String partHeader = part.getHeader("content-disposition");
			for (String content : part.getHeader("content-disposition").split(";")) {
				if (content.trim().startsWith("rulefile")) {
					return content.substring(content.indexOf('=') + 1).trim().replace("\"", "");
				}
			}
		}
		return null;
	}

	private static String getAnnotationFilterMenuHTML() {
		String html = "<menu><li>Parsetrees</li>" + "<li>Concepts Only</li>" + "</menu>";
		return html;
	}

	private static String getGrammarFileUploadHTML() {
		String html = "<form id=\"fileForm\" action=\"\" method=\"post\" enctype=\"multipart/form-data\">"
				+ "Grammar file:<br><input type=\"file\" name=\"rulefile\" size=\"40\"><br>"
				+ "<input type=\"submit\" value=\"Upload\" name=\"upload\" id=\"upload\"></form>";
		return html;
	}

}
