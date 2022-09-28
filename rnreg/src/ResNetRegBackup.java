/* $CwuRes: src/regi/www/webapps/rnreg/src/ResNetRegBackup.java,v 1.1 2018/10/10 19:00:34 alvisoj Exp $
 *
 * Uncomment lines:
 *   100(MAC), 103-107 (MAC), 208-251 (LDAP)
 *
 * Comment/Remove Lines
 *  104 (MAC), 252-253 (LDAP)
 */
/*
import java.io.*;
import java.net.*;
import java.sql.*;
import java.util.*;
import java.util.Date;
import java.util.logging.*;
import java.security.*;
import javax.servlet.*;
import javax.servlet.http.*;
import sun.net.smtp.SmtpClient;

import com.novell.ldap.*;
import edu.cwu.networks.*;
import oracle.jdbc.driver.*;


public class ResNetReg extends HttpServlet {

    // constants
    private static final String CONNSTR = "jdbc:oracle:thin:@anat.cts.cwu.edu:1521:resnet";
    private static final String DBUSER = "resnet";
    private static final String DBPWORD = "fakeN3ws!";
    private static final String LDAPSERV1 = "ldap.ad.cwu.edu";
    private static final String TEMPLATE1 = "rnreg1.template";
    private static final String TEMPLATE2 = "rnreg2.template";
    private static final int INCORRECT_PARAMETERS = -1;
    private static final int AUTHENTICATION_FAILED = -2;
    private static final int JDBC_ERROR = -3;
    private static final int INSERT_FAILED = -4;
    private static final int NIC_EXISTS = -5;
    private static final int RECORD_NOT_FOUND = -6;
    private static final int UPDATE_FAILED = -7;
    private static final int DELETE_FAILED = -8;
    private static final int CANNOT_REGISTER = -9;
    private static final int LIMIT_REACHED = -10;
    private static final int ILLICIT_USER = -11;

    private static Connection conn;
    private static Statement stmt;
    // must be Object for synchronization purposes
    private static Integer vlan = new Integer(121);
    // to be set by getInitArgs()
    private static String connStr = null;
    private static boolean debug = false;
    
    //private static final Logger test_logger = Logger.getLogger(ResNetReg.class.getName());
/*    
    private static void setupLogger(){
        test_logger.setLevel(Level.ALL);
        try {
            FileHandler myHandler = new FileHandler("/var/log/tomcat5/base/rnreg.log", 50000000, 10, true);
            myHandler.setFormatter(new SimpleFormatter());
            test_logger.addHandler(myHandler);
            test_logger.setLevel(Level.CONFIG);
            test_logger.log(Level.SEVERE, "TEST MESSAGE");
        } catch (IOException ex) {
            test_logger.log(Level.SEVERE, ex.getMessage(), ex);
        } catch (SecurityException ex) {
            test_logger.log(Level.SEVERE, ex.getMessage(), ex);
        }
    }
*/    
  /*  public void init(ServletConfig config) throws ServletException {

        super.init(config);
        // retrieve initialization parameters
        getInitArgs(config);
        //setupLogger();

	   // set up database connection
	   try {
		  DriverManager.registerDriver(new oracle.jdbc.driver.OracleDriver());
		  conn = DriverManager.getConnection(connStr, DBUSER, DBPWORD);
                  if (conn == null) {
                      System.out.println("Connection is null");
                  }
		  stmt = conn.createStatement();
                  if (stmt == null) {
                      System.out.println("Statement is null");
                  }
	   }
	   catch (SQLException se) {}

    }


    // retrieves initialization arguments with defaults assumed if not found
    private static void getInitArgs(ServletConfig config) {

        if ((connStr = config.getInitParameter("connStr")) == null)
            connStr = CONNSTR;

    }

/*
    public void doGet(HttpServletRequest req, HttpServletResponse res)
        throws ServletException, IOException {
           //System.setProperty("java.library.path", "/home/alex/LibJarp/libjarp.so");    // this does not work (duh)
	   res.setContentType("text/html");
	   PrintWriter out = res.getWriter();
           
           //check if there is an existing session, don't create a new one
	   HttpSession session = req.getSession(false); 
           //String page = "";
	   if (session == null)                                                     
		  loginRedirect(res);                                               
	   else {
                // if they havn't done the quiz then give it to them
               try { 
                   Integer.parseInt((String) session.getAttribute("QuizDone"));               // this statement must be returning a "null" value for the catch to initiate every single time         
               }
               
               catch (NumberFormatException ex) { 
                   
                   System.out.println("Threw error " + ex.getMessage());
                   //doQuiz(req, out);                                                        // servlet is defaulting to this code in the catch statement 
                 
               } 
               

		  String remoteIP = req.getRemoteAddr();                                    // becomes unreachable if finally block has a return statement for quizDoneRedirect 
                  System.out.println(remoteIP);
                  //String mac = ArpCache.getmac(remoteIP);                                   // getting java.lang.NoClassDefFoundError: Could not initialize class ArpCache error KB 
                                                                                            // only get error with fresh cache data, jarp error comes back if not 
                  String mac = "000000000000";                                                                          
                  //@TODO change system.out.println to a more elegant form of logging
                  Enumeration Attributes = session.getAttributeNames();
                  System.out.println(new Date() + ": " + "Session ID = " + session.getId());
                  while ( Attributes.hasMoreElements() ) {     
                         Object temp_attr = Attributes.nextElement();
                         Object temp_value = session.getAttribute(temp_attr.toString());
                         System.out.println(new Date() + ": " + temp_attr.toString() + " = " + temp_value.toString());
                  }
                  System.out.println(new Date() + ": " + remoteIP + ":" + mac);
                  if (stmt == null) {
                      System.out.println("Statement is null part 2");
                  }
                  if (conn == null) {
                      System.out.println("Connection is ALSO null part 2");
                  }
		  if (mac != null) {
			 // see if address is already in database
			 String owner = null;
			 boolean isRegistered = false;
			 try {
				synchronized (stmt) {
				    ResultSet rs =
					   stmt.executeQuery("select dname from resnet.hosts " +
									 "where ha='" + mac + "'");
				    if (rs.next()) {
					   isRegistered = true;
					   owner = rs.getString("dname");
				    }
				}
			 }
			 catch (SQLException se) {}
			 if (isRegistered) {
				out.println("<HTML>");
				out.println("<HEAD>");
				out.println("<TITLE>Already Registered</TITLE>");
				out.println("</HEAD>");
				out.println("<BODY background=\"images/grey1.gif\">");
				out.println("<font face=Arial size=-1>");
				out.println("Your Ethernet address (" + mac +
						  ") is already registered to " +
						  (owner == null ? "unknown" : owner) + ".");
				out.println("</font>");
				out.println("</BODY>");
				out.println("</HTML>");
				return;
			 }
		  }
		  
                  
                  
                  ServletContext servletCont = req.getSession().getServletContext();
                  String template1 = servletCont.getRealPath("WEB-INF/classes/"+ TEMPLATE1);            // template 1 is null, getRealPath is deprecated 
                  System.out.println("String template1 is " + template1);
                  
		  BufferedReader in = new BufferedReader(new FileReader(template1));                    // here is where the NullPointerException is 
		  String line;
		  while ((line = in.readLine()) != null) {
			 // parameter substitution
			 if (line.indexOf("<!-- %") != -1) {
				if (line.indexOf("NIC") != -1) {
				    if (mac != null) {
					   out.println("<input type=hidden name=nic value=" + mac + ">" +
								mac);
				    }
				    else
					   out.println("<input type=text name=nic size=13 maxlength=12>");
				}


				else if (line.indexOf("OS") != -1) {
				    String os = getOS(req);
				    // display option list if unable to determine OS and/or
				    // if unable to determine MAC address
				    if ((mac == null) || os.equals("unknown")) {
					   out.println("<select name=os>");
					   try {
						  ResultSet rs = null;
						  synchronized(stmt) {
							 rs = stmt.executeQuery("select name from " +
											    "resnet.oses order by name");
							 while (rs.next())
								out.println("<option>" + rs.getString("name"));
						  }
					   }
					   catch (SQLException se) {}
					   out.println("</select>");
				    }
				    else
					   out.println("<input type=hidden name=os value=\"" + os +
								"\">" + os);
				}
				else if (line.indexOf("CACHE_MESSAGE") != -1) {
				    if (mac ==null) {
					out.println("<h4>IMPORTANT: If you have already " +
						    "registered this device and are still being redirected here, please " +
						    "clear your browser cache/history:" + 
                                                    "<ul style=\"padding-left:20px\"><li>Windows - control+shift+delete</li><li>Mac/Safari - Command + Option + E</li></ul></h4> ");
				    }
				}
			 }
			 else
				out.println(line);
		  }
		  in.close();
	   }


    } 

   /* public void doPost(HttpServletRequest req, HttpServletResponse res)
        throws ServletException, IOException {
    	
    	res.setContentType("text/html");
    	PrintWriter out = res.getWriter();

    	// if the quiz was submitted then go back to it
    	if(!(req.getParameter("SubmitQuiz") == null)) {                             // edit this to try to direct from login to register, bypassing quiz, if already registered KB
               doQuiz(req, out);                                                   // commenting this out does nothing, quiz still accessed ? KB 
               
        } 
           
        String action = req.getParameter("action");
        
	    if (action.equals("login")) {
	    	String ldapDN = null;
	    	HttpSession session = req.getSession(true);                         // this is the only getSession attribute with a true boolean in the servlet KB 
	    	session.setAttribute("username", "");
	    	session.setAttribute("psid", "");
	    	String username = req.getParameter("username").toLowerCase();       // finds gets username entered, cross-references username with ldap, confirms legitimate KB
	    	//debug
	    	if (debug) System.out.println(new Date() + ": " + "RNTEST DEBUG - " + "User Submitted Username -> " + username);
            
	    	// CN entered
	    	Vector dnList = UserTools.findDN(username, "ldap.ad.cwu.edu", "dc=ad,dc=cwu,dc=edu");
		  
	    	if ((dnList == null) || (dnList.size() > 1) || ((dnList.indexOf("applicants") > -1) || (dnList.indexOf("alumni") > -1))) {
	    		session.invalidate();
                        System.out.println("ln247");
	    		errorMsg(out, AUTHENTICATION_FAILED);
	    		return;
	    	}
	    	// Else check for dname
	    	else {
	    		ldapDN = (String)dnList.elementAt(0);
	    		username = "." + UserTools.comma2Dot(ldapDN);
	    		//debug
	    	    if (debug) System.out.println(new Date() + ": " + "RNTEST DEBUG - " + "ldapDN at element 0 of dnList -> " + ldapDN);
	    	    if (debug) System.out.println(new Date() + ": " + "RNTEST DEBUG - " + "username after UserTools.comma2dot -> " + username);            
			}				
	    	
	    	// Error check
	    	if (ldapDN == null) {
	    		errorMsg(out, ILLICIT_USER);
	    		session.invalidate();
	    		return;
	    	}

	    	// Authenticate and retrieve cwuPSID at same time.
	    	// Error could conceivably come from missing cwuPSID,
	    	// but that is highly unlikely.
	    	String password = req.getParameter("pword");                        // gets password entered, binds password with ldap object KB
	    	String psid = null;

	    	if (UserTools.ldapBind(ldapDN, password, "ldap.ad.cwu.edu") == false) { 
	    		//unable to bind to LDAP, they do not have an account!!
	    		//@TODO change system.out.println to a more elegant form of logging
	    		// System.out.println(new Date() + ": " + "Unable to bind to LDAP: " + dn);
	    		session.invalidate();
                        System.out.println("ln 277");
	    		errorMsg(out, AUTHENTICATION_FAILED);
	    		//debug
	    	        if (debug) System.out.println(new Date() + ": " + "RNTEST DEBUG - Unable to bind to LDAP: " + ldapDN);
		        return;
	    	}
	    	//@TODO Check if they have a PSID.  If not, put something in there so we don't crash the system
	    	else if ((psid = UserTools.getPSID(ldapDN, password, "ldap.ad.cwu.edu")) == null) {
	    		//@TODO Kludge.  We need to find a better way to deal with users who do not have a PSID in LDAP (Guests, applicants, etc.)
	    		//fill in a fake PSID for users who don't have one
	    		psid = "1";
	    		//debug
	    		if (debug) System.out.println(new Date() + ": " + "RNTEST DEBUG - User does not have SID");
	    	}
			 session.setAttribute("username", username);                // if username/password legitimate, binds info to current session & redirects KB
			 session.setAttribute("psid", psid);
			 // sendRedirect() does odd things pointed to servlet
			 out.println("<html>");
			 out.println("<head>");
			 out.println("<META HTTP-EQUIV=\"Refresh\" CONTENT=\"0;url=/rnreg/ResNetReg\">");
			 out.println("<title>Registration Redirect</title>");
			 out.println("</head>");
			 out.println("<body background=\"images/grey1.gif\">");
			 out.println("</BODY>");
			 out.println("</HTML>");
	    }
	    else if (action.equals("register")) {
	      String username = null;
	      String hostname = null;
	      String psid = null;
		  String nic = req.getParameter("nic");                         // takes MAC address from regwireless.html form submission KB
		  String os = req.getParameter("os");                           // takes operating system from regwireless.html form submission KB
		  HttpSession session = req.getSession(false);
		  if ((session == null) ||
			 ((username = (String)session.getAttribute("username")) == null) ||
			 ((psid = (String)session.getAttribute("psid")) == null))
			 loginRedirect(res);
		  else if (nic == null || os == null) {
			 errorMsg(out, INCORRECT_PARAMETERS);
		  }
		  else {
			 String sqlString = "insert into resnet.hosts " +       // post-registration prep for insertion into database with SQL query 
				"(sid, dname, ha, os_id, vlan) values (" +
				psid + ", '" + username + "', '" + nic + "', " +
				"(select id from resnet.oses where " +
				"name='" + os + "'), " + vlan.intValue() + ")";
			 try {
				synchronized (stmt) {
				    // Check if able to register
				    ResultSet rs = stmt.executeQuery("select can_register from " +
											  "resnet.users where sid = " + psid);
				    if (rs.next()) {
					   if (rs.getInt("can_register") == 0) {
						  errorMsg(out, CANNOT_REGISTER);
						  return;
					   }
				    }
				    
				    stmt.executeUpdate(sqlString);
				    rs = stmt.executeQuery("select hostname from resnet.registry " +
									  "where ha='" + nic + "'");
				    if (rs.next())
					   hostname = rs.getString("hostname");
				    synchronized (vlan) {
                                           //@TODO Fix this kludge.  We did away with VLAN 131, so we are doing some gymnastics to avoid handing it out.
					   int vlanNum = vlan.intValue();
					   if (vlanNum == 133) {
						  vlanNum = 121;
					   } else if (vlanNum == 130) {
						  vlanNum = 132;
					   } else {
						  ++vlanNum;
					   }
					   vlan = new Integer(vlanNum);
				    }
				}
                                //@TODO Fix the formatting of this code, what level should all this be indented at?
				out.println("<HTML>");
				out.println("<HEAD>");
				out.println("<TITLE>ResNet Registration Results</TITLE>");
				out.println("<script language=\"JavaScript\">");
				out.println("function netsupport_screenshot() {");
				out.println("open(\"/rnreg/notify/images/netsupport_screenshot.jpg\",\"window3\",\"toolbar=no,titlebar=false,width=510,height=310,resizable=no,scrollbars=no\");");
				out.println("}");
				out.println("function windows_help() {");
				out.println("open(\"/rnreg/notify/netsupport_windows.html\", \"window4\", \"toolbar=no,titlebar=false,width=600,height=320,scrollbars=yes,resizable=yes\");");
				out.println("}");
				out.println("function mac_help() {");
				out.println("open(\"/rnreg/notify/netsupport_mac.html\", \"window5\", \"toolbar=no,titlebar=false,width=600,height=320,scrollbars=yes,resizable=yes\");");
				out.println("}");
				out.println("</script>");
				out.println("</HEAD>");
				out.println("<BODY background=\"images/grey1.gif\">");
				out.println("<font face=Arial size=-1>");
				out.println("<h2>You have successfully registered this computer!</h2>" +
						"<h3>Restart your computer after 15 minutes.  " +
						"Your internet access will be enabled after you restart.</h3>"); 
				out.println("<u><b>Registration Information:</b></u><br>" +
						"<b>Registered to:</b> " + username + "<br>" +
						"<b>Hostname:</b> " + hostname + "<br>" +
						"<b>Hardware Address:</b> " + nic + "<br>" +
						"<b>Operating System:</b> " + os + "<br>");


out.println("<hr noshade><p>As part of Central Washington University's Emergency Notification System (ENS), CWU has " +
"implemented several services including \"Desktop Alert!\". Desktop Alert! will broadcast a message to all computers " +
"connected to the CWU Campus and Residential Network (ResNet). Residential computers owned by students must opt-in to " +
"this system by clicking on the \"Accept\" button below. We highly recommend that all students registering computers on " +
"CWU ResNet accept this service.</p>");

out.println("<p>Please choose the correct version for your operating system:</p>");
out.println("<table width='100%' border='0'><tr>");
out.println("<th width='20%'>Windows XP/Vista/7<form name='download_windows' action='/rnreg/notify/client/resnet_notify.exe'><input value='Accept' type='submit'></form><a href='javascript:windows_help();'><font size=-2>instructions</font></a></th>");
out.println("<th width='10%'>-OR-</th>");
out.println("<th width='20%'>Mac OS<form name='download_mac' action='/rnreg/notify/client/ResNet-Notify.dmg'><input value='Accept' type='submit'></form><a href='javascript:mac_help();'><font size=-2>instructions</font></a></th>");
out.println("<th width='50%'><font size=-2>If you do not want to install the Desktop Alert! client, simply close this browser window.</font></th></tr></table>");
out.println("<p><font size=-2>Students who subscribe to this system will only receive safety related emergency notifications as they occur, along with a test message approximately once per quarter.  When an alert is sent, you will see a popup window on your screen <a href='javascript:netsupport_screenshot();'>(example)</a> that will give you information about the emergency.  All CWU owned computers will be subscribed to this system.  Your computer will only receive notifications when it is connected to the CWU Network (Campus or ResNet).  This system will <i>never</i> be used to spam students on non-emergency related situations.</font></p>");


//This is the original web page content
//				out.println("Registration of the host with Ethernet " +
//						  "address <b>" + nic + "</b> was successful.<br>" +
//						  "The hostname for this system is <b>"
//						  + hostname + ".d.resnet.cwu.edu</b>.<p>Please shut " +
//						  "down your computer " +
//						  "and restart it after the next quarter hour.");

//TESTING
//Here is where I will add a form.  Email address text box, with submit button.  This will trigger an email to be sent that will
//register the form information with terilee's emergency email notification system.  Maybe include instructions for common cell phone
//text messaging email addresses.  Remember 256 character limit for text messages.

//out.println("<hr noshade><b>***Please take this opportunity to sign up for CWU Alert mailing list.***</b><br>");
//out.println("<p>The CWU Alert E-mail System is a notification system that allows CWU to quickly" +
//		" communicate safety-related emergency information to interested parties who have" +
//		" subscribed to the service.  By opting in, you are providing us with your permission" +
//		" to send information, which may include information protected under confidentiality" +
//		" rules, to e-mail addresses you provide. You may opt out of the system at any time.</p>" +
//		"<p><strong>Please note:</strong> If you choose not to subscribe to the CWU Alert E-mail" +
//		" System, you will not receive safety-related emergency e-mails directly from CWU. As an" +
//		" alternative, interested parties may monitor the CWU Emergency Closures page at" +
//		" http://www.cwu.edu/closures.html.</p><p><strong>Please note:</strong> CWU will be" +
//		" implementing an enhanced alert notification system for students, faculty and staff to" +
//		" receive campus alerts via phone and text messaging in the near future.</p>");
 
//out.println("<form name=req method='post' action='/rnreg/ResNetReg'>");
//out.println("<b>Email Address:</b>");
//out.println("<input type='text' name='email_address'>");
//out.println("<input type='hidden' name='action' value='list_signup'>");
//out.println("<input type='submit' name='SubmitList' value='Submit'>");
//out.println("</form>");
//GNITSET 
/*  COMMENT HERE 
				out.println("</font>");
				out.println("</BODY>");
				out.println("</HTML>");		 
			 }
			 catch (SQLException se) {
				if (se.toString().indexOf("ORA-00001") != -1)
				    errorMsg(out, NIC_EXISTS);
				else if (se.toString().indexOf("ORA-20001") != -1)
				    errorMsg(out, LIMIT_REACHED);
				else
				    errorMsg(out, INSERT_FAILED);
			 }
		  }
	   }
	   else if (action.equals("edit")) {
		  String psid = null;
		  HttpSession session = req.getSession(false);
		  if ((session == null) ||
			 ((psid = (String)session.getAttribute("psid")) == null))
			 loginRedirect(res);
		  else {
			 // each edit request should have a row number
			 String rownum = req.getParameter("rownum");
			 Vector data = (Vector)session.getAttribute("data");
			 if (rownum == null)
				errorMsg(out, INCORRECT_PARAMETERS);
			 else {
				int row = Integer.parseInt(rownum, 10);
				// new request
				if (row == -2 || data == null) {
				    String sqlString = "select hostname, " +
					   "ha from " +
					   "resnet.hosts where " +
					   "sid='" + psid + "' and " +
					   "enable=1 and vlan!=(select id from resnet.vlans " +
					   "where name='RES-JAIL')";
				    ResultSet rs = null;
				    data = new Vector();
				    try {
					   synchronized (stmt) {
						  rs = stmt.executeQuery(sqlString);
						  while (rs.next())
							 data.add(new RnRecord(rs.getString("hostname"),
									       rs.getString("ha")));
					   }
				    }
				    catch (SQLException se) { errorMsg(out, JDBC_ERROR); }
				    session.setAttribute("data", data);
				    if (data.isEmpty())
					   errorMsg(out, RECORD_NOT_FOUND);
				    else
					   showRecord(req, out, data, 0);
				}
				else {
				    // request for next or previous record
				    if (data.isEmpty())
					   errorMsg(out, RECORD_NOT_FOUND);
				    else {
					   // wrap to last record
					   if (row < 0 )
						  showRecord(req, out, data, data.size() - 1);
					   else if (data.size() > row)
						  showRecord(req, out, data, row);
					   // wrap to first record
					   else
						  showRecord(req, out, data, 0);
				    } 
				}
			 }
		  }
	   }
	   else if (action.equals("update")) {
		  updateRecord(req, res, out);
	   }
	   else if (action.equals("delete")) {
		  deleteRecord(req, res, out);
	   }
	   else if (action.equals("logout")) {
		  HttpSession session = req.getSession(false);
		  if (session != null)
			 session.invalidate();
		  loginRedirect(res);
	   }
           //@TODO I don't think this is used any longer and could probably be removed.
	   else if (action.equals("list_signup")) {
		//Send an email
		try {
		   String email_address = req.getParameter("email_address");
                   //@TODO change system.out.println to a more elegant form of logging
		   System.out.println(new Date() + ": " + "Email list signup for " + email_address);
		   SmtpClient smtp = new SmtpClient();
		   smtp.from("sysmgr@cwu.edu");
		   smtp.to("listserv@cwu.edu,sysmgr@cwu.edu");
		   PrintStream msg = smtp.startMessage();
		   msg.println("To: listserv@cwu.edu,sysmgr@cwu.edu");
		   msg.println("From: sysmgr@cwu.edu");
		   msg.println("Subject: CWU_EReport");

		   //message body should be in the following format:
		   // <subscribe|unsubscribe> <list name> <email address>
		   msg.println("Subscribe CWU_EReport " + email_address);

		   smtp.closeServer();
		} catch( Exception e ) {
                    //@TODO change system.out.println to a more elegant form of logging
		    System.out.println(new Date() + ": " + "Exception in email sending code: " + e);
	   	} //end catch
                                out.println("<HTML>");
                                out.println("<HEAD>");
                                out.println("<TITLE>ResNet Registration Complete</TITLE>");
                                out.println("</HEAD>");
                                out.println("<BODY background=\"images/grey1.gif\">");
                                out.println("<font face=Arial size=-1>");
                                out.println("<h2><b>Registration Complete.</b></h2>" +
					"<p>Restart your computer in <b>15 minutes</b> and your internet " +
					"access will be enabled.</p> <br> <p>Thank you for registering your " + 
					"email address to receive CWU Alert emails.</p>");
                                out.println("</font>");
                                out.println("</BODY>");
                                out.println("</HTML>");
	   }
	   else
		  errorMsg(out, INCORRECT_PARAMETERS);
    }

    
    private void quizDoneRedirect(HttpServletResponse res) throws java.io.IOException {
	   res.sendRedirect("regwireless.html"); // "/home/alex/Desktop/HostReg Files (copy)/rnreg/web/regwireless.html"
    } 
    */
    /* COMMENT HERE 
    private void doQuiz(HttpServletRequest req, PrintWriter out) {
        //prepares this to be seen via a browser
        Quiz testQuiz = new Quiz(); //new quiz object
        Database dbDatabase = new Database(); //new database object
        HttpSession session = req.getSession(false);
        int  intAttemps = 0; //sets the number of attempts to 0
        //selects the max of the success column - 0 if they failed or 1 if they have passed
        //also counts how many entries which shows how many attempts they have done
//TESTING
//OLD   ResultSet passQuery = dbDatabase.SelectQuery("select max(success), count(id) from resnet.quiz_results where SID = " + (String) session.getValue("psid"));
	ResultSet passQuery = dbDatabase.SelectQuery( "select max(resnet.quiz_results.success), count(resnet.quiz_results.id) from resnet.quiz_results, resnet.quiz_type where resnet.quiz_type.id = resnet.quiz_results.type_id(+) and resnet.quiz_type.name = 'registration' and resnet.quiz_results.sid = " + (String) session.getAttribute("psid").toString() );
//GNITSET 
        try {
            //if they have passed it, do not make them do it again
            if (passQuery.getString(1).equals("0")) {                               // check here as well for bypassing quiz screen if already completed KB 
		//do nothing
            }
            else {
		session.setAttribute("QuizDone", "1");
                //@TODO change system.out.println to a more elegant form of logging
		System.out.println(new Date() + ": " + session.getAttribute("username").toString() + " has already completed the quiz.  Quiz skipped.");        // changed getValue("username") to non-deprecated version here KB 
                return;
            }
            //stores the number of attempts
             intAttemps = Integer.parseInt(passQuery.getString(2));
        }
        catch (SQLException ex) {
            //@TODO change system.out.println to a more elegant form of logging 
            System.out.println(new Date() + ": " + "SQL Exception: " + ex.getMessage());
	}  
        catch (java.lang.NullPointerException ex) {
            //@TODO change system.out.println to a more elegant form of logging
            System.out.println(new Date() + ": " + "Null Pointer Exception: " + ex.getMessage());
	// the query was null
        }

           //only students need to take the quiz
	    String tmpUsername = (String)session.getAttribute("username");
	    if(tmpUsername.indexOf("students") == -1)  {
                session.setAttribute("QuizDone", "0");
                //@TODO change system.out.println to a more elegant form of logging
		System.out.println(new Date() + ": " + tmpUsername + " is not a student.  Quiz Skipped.");
                return;
            }


                out.println("<html>");
                out.println("<head>");
                out.println("<title>ResNet Registration Quiz</title>");
                out.println("<script language=\"JavaScript\">");
                out.println("");
                out.println("function aup() {");
                out.println("open(\"resnet_aup.html\", \"window2\", ");
                out.println("\"toolbar=no,titlebar=false,width=600,height=350,resizable=yes,scrollbars=yes\");");  
                out.println("}");
                out.println("</script>");
                out.println("</head>");
                out.println("<body background='../rnreg/images/grey1.gif'>");       

               if(!(req.getParameter("SubmitQuiz") == null)) { 
                    //processes if it has been submitted. This code is written here so that it can work with all the params and such.
            
                    AllQuestions allQuestions = new AllQuestions(); // object that holds all the questions in the table

                    // some various variables
                    int i = 0; // iteration variable
                    int currentID =0;
                    int currentAnswer =0;
                    int totalCorrect = 0;
                    int totalWrong = 0;
                    int intMinScore = testQuiz.MinScore; // Grabs the min score from the quiz object
                    String strThingsToKnow = "<br />Things you should know<br/><ul>";
                    String questionsWrong = "^";
                    String questionsCorrect = "^";

                    Enumeration paramNames = req.getParameterNames(); //gets all the names
                    while(paramNames.hasMoreElements()) {//cycles through, if it is an id number it will store it
                        String paramName = (String)paramNames.nextElement();
                        try { //Try and catch to account for it not being an int
                            currentID = Integer.parseInt(paramName);
                            currentAnswer = Integer.parseInt(req.getParameter(paramName));
                            // if it is right it increases the correct count, if it is wrong then it increases the wrong count and pulls the hint
                            if (allQuestions.question[currentID].Grade(currentAnswer)) {
                               // out.println("<br />"+allQuestions.question[currentID].strQuestion+"<br /> Correct");
                                totalCorrect++;
                                questionsCorrect += currentID+"^";
                            }
                            else {
                               strThingsToKnow +="<li>"+allQuestions.question[currentID].strHint+"</li>";
                               totalWrong++;
                               questionsWrong += currentID+"^";
                            }
                        }
                        catch (NumberFormatException ex) {
                            //Not a valid number. Skip iteration. This accounts for submit.
                        }     
                        catch (NullPointerException ex) {
                            out.println("<br /><b>Error Loading Question...</b><br />");
                        }
                    }
                    strThingsToKnow +="<li> <a href= 'javascript:aup();'>CWU ResNet Acceptable Use Policy</a></li></ul>"; //finishes the bulleted list

                    // if any are wrong then they get the list of hints
                    if (totalWrong > 0) {
                        out.println(strThingsToKnow);
                    }

                    int pass = 0;
                    //tells them if they pass/fail
                    if (totalCorrect < intMinScore) {
                        //@TODO change system.out.println to a more elegant form of logging
			System.out.println(new Date() + ": " + "Quiz failed." + session.getValue("username") +  "Total Correct: "+totalCorrect+" Total Wrong: "+totalWrong);
                        out.println("<br/><br/>I'm sorry to inform you that you have only answered "+totalCorrect+" correctly. The minimum passing score is "+intMinScore+" and thus you have failed. Please study the above information before trying again.");
                        out.println("<br/> Please try again. Click <a href='ResNetReg'>Here</a> to get a new quiz.");
                        pass = 0;
                    }
                    else {
                        //@TODO change system.out.println to a more elegant form of logging
			System.out.println(new Date() + ": " + "Quiz Passed." + session.getValue("username") +  "Total Correct: "+totalCorrect+" Total Wrong: "+totalWrong);
                        out.println("<br /> You have passed the quiz, Congratulations!");
                        out.println("<br /> Continue the process <a href='ResNetReg'>Here</a>");
                        pass = 1;
                        session.setAttribute("QuizDone", "1");

                    }                 
                    //stores their pass/fail results in the table
		    int QUIZ_TYPE = 0;
		    String strErrors = "";
		    try {
      		    	QUIZ_TYPE = dbDatabase.SelectQuery("select id from quiz_type where name='registration'").getInt(1);
		    }
		    catch (SQLException ex) {
       			while (ex != null) {
			    strErrors += ("\nSQL Exception:  " + ex.getMessage ());
            	  	    ex = ex.getNextException ();
       			}
                        //@TODO change system.out.println to a more elegant form of logging
       			System.out.println(new Date() + ": " + strErrors);
		    }
		    catch (java.lang.Exception ex) {
                        //@TODO change system.out.println to a more elegant form of logging
        	        System.out.println(new Date() + ": " + "Exception:  " + ex.getMessage ());
		    }
			
                    dbDatabase.RunQuery("insert into resnet.quiz_results (type_id, sid, success, test_date) values ("+QUIZ_TYPE+","+ (String) session.getValue("psid")+","+pass+",sysdate)");

                    
                    //This section puts entries in the quiz_Stats table for every question, marking if they got it right or wrong
                    String correctQuestions[];
                    correctQuestions = questionsCorrect.split("\\^"); 
                    for(int x=1; x < correctQuestions.length;x++) {
                        if (!(correctQuestions[x] == "")) {
                           dbDatabase.RunQuery("insert into resnet.quiz_stats(question_id, correct) values ("+correctQuestions[x]+", 1)");
                        }
                    }
                    
                    String wrongQuestions[] = questionsWrong.split("\\^");    
                    for(int x=1; x < wrongQuestions.length;x++) {
                        if (!(wrongQuestions[x] == "")) {
                            dbDatabase.RunQuery("insert into resnet.quiz_stats(question_id, incorrect) values ("+wrongQuestions[x]+", 1)");
                        }
                    }
                    allQuestions.Close();
               }

               else {
                //This section displays the quiz for the student. If they have attempted it before then it will display that. After 10 attemps
                // they are told to call ResNet Support.
                out.println("<form name='form1' method='post' action='"+req.getContextPath()+req.getServletPath()+"'>");
                
		out.println("<br/><font size = 4> The following quiz has been implemented to meet with a new ResNet policy that requires all students to have a basic understanding of the AUP, P2P, and general internet safety. This quiz should be relatively simple and is able to be retaken if you should fail. However, the quiz must be completed successfully before you are able to register on the network.</font>");

		if (intAttemps > 0) {
                    out.println("<br/>You have attempted this quiz " +intAttemps+" time(s). This quiz must be completed before you are able to register");
                }
		
                if (intAttemps >= 10) {
                    out.println("<br/>If you need additional assitance, please call ResNet Support at (509)963-2200<br/><br/>");
                }

		//@TODO change system.out.println to a more elegant form of logging
                System.out.println(new Date() + ": " + "Serving Quiz, Attempt No: " +intAttemps+" for " + session.getAttribute("username").toString());
                out.println("<br />Please Note: Students who have special needs or disabilities that may affect their ability to access information and/or material presented in this quiz are encouraged to contact ResNet Support for additional assistance. <br/>Before proceeding, please take a moment to review the <a href= 'policy.html;'>CWU ResNet Acceptable Use Policy</a><br/>");
                testQuiz.populateQuiz();  //creates the quiz
                out.println(testQuiz.Display());
                out.println("<br/>");
                out.println("<input type='hidden' name='action' value='quiz'>");
                out.println("<input type='submit' name='SubmitQuiz' value='Submit'>");
                out.println("<input type='reset' name='Reset' value='Reset'>");
                out.println("</form>");        

               }
           
                out.println("</body>");
                out.println("</html>");
                out.close();  
                dbDatabase.Close(); //Closes the connection
           }


    private String getOS(HttpServletRequest req) {

	   String os = "unknown";
	   String agent = req.getHeader("user-agent");
           System.out.println(new Date() + ": " + "User-Agent string = " + agent);
	   if (agent != null) {
		  agent = agent.toLowerCase();
		  // Windows flavors
		  if ((agent.indexOf("win") != -1) || (agent.indexOf("16bit") != -1 )) {
			 if (agent.indexOf("windows nt 5.1") != -1)
				os = "Windows XP";
			 else if (agent.indexOf("windows nt 5.2") != -1)
				os = "Windows 2003";
		     	 else if (agent.indexOf("windows nt 6.0") != -1)
				os = "Windows Vista";
			 else if (agent.indexOf("windows nt 6.1") != -1)
                             //if (agent.indexOf("xbox") == -1)
                                os = "Windows 7";
                         else if (agent.indexOf("windows nt 6.2") != -1)
				os = "Windows 8";
			 else if (agent.indexOf("windows nt 6.3") != -1)
				os = "Windows 8.1";
			 else
				os = "Windows (Other)";
		  }
		  // Macintosh flavors
		  else if (agent.indexOf("mac") != -1) {
			 if ((agent.indexOf("ppc") != -1) || (agent.indexOf("powerpc") != -1))
				os = "Mac OS (PPC)";
			 else if ((agent.indexOf("mac os x") != -1) || (agent.indexOf("macintosh") != -1))
				os = "Mac OS X";
		  }
		  // UNIX flavors
		  else if (agent.indexOf("freebsd") != -1)
			 os = "FreeBSD";
		  else if (agent.indexOf("linux") != -1)
			 os = "Linux";
                  else if (agent.indexOf("android") != -1)
                         os = "Android";
                  
		  //Consoles with web browsers
		  else if (agent.indexOf("playstation 3") != -1)
			 os = "Playstation 3";
		  else if (agent.indexOf("playstation 4") != -1)
			 os = "Playstation 4";
		  else if (agent.indexOf("nintendo wii") != -1)
			 os = "Wii";
                  else if (agent.indexOf("xbox") != -1)
                         os = "Xbox 360";
		  else if (agent.indexOf("xbox one") != -1)
	   		 os = "Xbox One";
	   }
           System.out.println(new Date() + ": " + "OS detected as: " + os);
	   return os;
	
    }


    private void showRecord(HttpServletRequest req, PrintWriter out, Vector data, int row)
	   throws java.io.IOException {

	   int dataSize = data.size();
	   if (row >= dataSize || row < 0)
		  row = 0;
	   String hostname = ((RnRecord)data.elementAt(row)).getHostname();
	   String ha = ((RnRecord)data.elementAt(row)).getHa();

	   String template2 = req.getRealPath("WEB-INF/classes/" + TEMPLATE2);
	   BufferedReader in = new BufferedReader(new FileReader(template2));
	   String line;
	   while ((line = in.readLine()) != null) {
		  // parameter substitution
		  if (line.indexOf("<!-- %") != -1) {
			 if (line.indexOf("ROWNUM") != -1)
				out.println("curRow = " + row + ";");
			 else if (line.indexOf("HOSTNAME") != -1)
				out.println("<input type=hidden name=hostname value=\"" + hostname +
						  "\">" + hostname);
			 else if (line.indexOf("NIC") != -1)
				out.println("<input type=text name=nic value=\"" + ha + 
						  "\" size=13 maxlength=12>");
		  }
		  else
			 out.println(line);
	   }
	   in.close();

    }


    private void updateRecord(HttpServletRequest req, HttpServletResponse res, PrintWriter out)
	   throws java.io.IOException {

	   String username = null;
	   Vector data = null;
	   HttpSession session = req.getSession(false);
	   if ((session == null) ||
		  ((username = (String)session.getAttribute("username")) == null) ||
		  ((data = (Vector)session.getAttribute("data")) == null))
		  loginRedirect(res);
	   else {
		  String hostname = req.getParameter("hostname");
		  String nic = req.getParameter("nic");
		  int rownum = Integer.parseInt(req.getParameter("rownum"));
		  RnRecord record = (RnRecord)data.elementAt(rownum);
		  record.setHa(nic);
		  data.setElementAt(record, rownum);
		  String sqlString = "update resnet.hosts set ha='" + nic + "' " +
			 "where hostname='" + hostname + "' and dname='" + username + "'";
		  try {
                     synchronized (stmt) {
				stmt.executeUpdate(sqlString);
			 }
			 showRecord(req, out, data, rownum);
		  }
		  catch (SQLException se) { errorMsg(out, UPDATE_FAILED); }
	   }

    }

    private void deleteRecord(HttpServletRequest req, HttpServletResponse res, PrintWriter out)
	   throws java.io.IOException {

	   String username = null;
	   Vector data = null;
	   HttpSession session = req.getSession(false);
	   if ((session == null) ||
		  ((username = (String)session.getAttribute("username")) == null) ||
		  ((data = (Vector)session.getAttribute("data")) == null))
		  loginRedirect(res);
	   else {
		  String hostname = req.getParameter("hostname");
		  String nic = req.getParameter("nic");
		  int rownum = Integer.parseInt(req.getParameter("rownum"));
		  String sqlString = "delete from resnet.hosts where hostname='" +
			 hostname + "' and dname='" + username + "' and ha='" + nic + "'";
		  try {
			 synchronized (stmt) {
				stmt.executeUpdate(sqlString);
			 }
			 data.removeElementAt(rownum);
			 if (data.isEmpty()) {
				out.println("<HTML>");
				out.println("<HEAD>");
				out.println("<TITLE>Registration Error</TITLE>");
				out.println("</HEAD>");
				out.println("<BODY background=\"images/grey1.gif\">");
				out.println("<font face=Arial size=-1>");
				out.println("Last record deleted");
				out.println("</font>");
				out.println("</BODY>");
				out.println("</HTML>");
			 }
			 else
				showRecord(req, out, data, rownum);
		  }
		  catch (SQLException se) { errorMsg(out, DELETE_FAILED); }
	   }

    }


    private void loginRedirect(HttpServletResponse res) throws java.io.IOException {
	   res.sendRedirect("rnreg-login.html");
    }


    private static String[] errors =
	   new String[] {"incorrect parameters",
				  "authentication failed",
				  "general JDBC failure",
				  "failed to insert record",
				  "Ethernet address already in database",
				  "no records found",
				  "failed to update record",
				  "failed to delete record",
				  "registration disabled due to policy violation",
				  "registration limit of 10 computers exceeded",
				  "User does not exist or is disabled"};

    private void errorMsg(PrintWriter out, int errNo) {

	   out.println("<HTML>");
	   out.println("<HEAD>");
	   out.println("<TITLE>Registration Error</TITLE>");
	   out.println("</HEAD>");
	   out.println("<BODY background=\"images/grey1.gif\">");
	   out.println("<font face=Arial size=-1>");
	   out.println("Error " + errNo + ": " + errors[Math.abs(errNo) - 1] +
				"<BR>");
	   out.println("</font>");
	   out.println("</BODY>");
	   out.println("</HTML>");

    }

}

*/