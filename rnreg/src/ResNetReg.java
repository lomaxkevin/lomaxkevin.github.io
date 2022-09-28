/* $CwuRes: src/regi/www/webapps/rnreg/src/ResNetReg.java,v 1.199 2019/09/03 21:31:45 resnet Exp $
 *
 * Uncomment lines:
 *   100(MAC), 103-107 (MAC), 208-251 (LDAP)
 *
 * Comment/Remove Lines
 *  104 (MAC), 252-253 (LDAP)
 */

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
    /* These are the creds for the test DB anat */ 
    //private static final String CONNSTR = "jdbc:oracle:thin:@anat.cts.cwu.edu:1521:resnet";
    //private static final String DBUSER = "resnet";
    //private static final String DBPWORD = "fakeN3ws!";
    
    //private static final String CONNSTR = "jdbc:oracle:thin:@mot.resnet.cwu.edu:1521:resnet";
    //private static final String DBUSER = "resapp";
    //private static final String DBPWORD = "r3sappl3";
    
    // this is the correct creds for the live mot 
    private static final String CONNSTR = "jdbc:oracle:thin:@mot.resnet.cwu.edu:1521:resnet";
    private static final String DBUSER = "resnet";
    private static final String DBPWORD = "dukenukem";
    
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
    private static boolean debug = true;
    
    private static final Logger test_logger = Logger.getLogger(ResNetReg.class.getName());
    
    /* private static void setupLogger(){
        test_logger.setLevel(Level.ALL);
        try {
            FileHandler myHandler = new FileHandler("/var/log/test_rnreg.log", 50000000, 10, true);
            myHandler.setFormatter(new SimpleFormatter());
            test_logger.addHandler(myHandler);
            test_logger.setLevel(Level.CONFIG);
            test_logger.log(Level.SEVERE, "TEST MESSAGE");
        } catch (IOException ex) {
            test_logger.log(Level.SEVERE, ex.getMessage(), ex);
        } catch (SecurityException ex) {
            test_logger.log(Level.SEVERE, ex.getMessage(), ex);
        }
    } */
    
      /*
     * Initializes a logger to document log messages from STDOUT streams.
     *
     * @param   none
    */
    private static void setupLogger() {
         try {
            FileHandler myHandler = new FileHandler("/var/log/test_rnreg.log", 50000000, 10, true);
            test_logger.addHandler(myHandler);
            test_logger.setLevel(Level.ALL);
            myHandler.setFormatter(new SimpleFormatter());
            test_logger.log(Level.WARNING, "My First Log");
         } catch (SecurityException ex) {
            test_logger.log(Level.SEVERE, ex.getMessage(), ex);
         } catch (IOException ex) {
            test_logger.log(Level.SEVERE, ex.getMessage(), ex);
         }
    }
    
    /*
     * Called by server immediately after the server constructs the servlet's instance.
     * Used to perform servlet's initialization -- creating or loading objects that
     * are used by the servlet in the handling of its requests.
     *
     * @param   config a ServletConfig object
     */
    public void init(ServletConfig config) throws ServletException {

        super.init(config);
        // retrieve initialization parameters
        getInitArgs(config);
        setupLogger();

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


    /*
     * Retrieves initialization arguments with defaults assumed if not found
     *
     * @param   config a ServletConfig object, which supplies the servlet with
     * information about its initialization parameters.
     */
    private static void getInitArgs(ServletConfig config) {

        if ((connStr = config.getInitParameter("connStr")) == null)
            connStr = CONNSTR;

    }

    /*
     * Processes HTTP GET requests from the servlet.
     * Checks to see if user has completed the AUP quiz.
     * If they have, it serves them the wired registration page.
     * If they have not, it serves them the AUP quiz.
     *
     * @param   req an HTTP request to the servlet object
     * @param   res an HTTP response to the servlet object
     */
    public void doGet(HttpServletRequest req, HttpServletResponse res)
        throws ServletException, IOException {
           
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
                   Integer.parseInt((String) session.getAttribute("QuizDone"));                        
               }              
               catch (NumberFormatException ex) { 
                   
                   System.out.println("Threw error " + ex.getMessage());
                   doQuiz(req, out);                                                        
                 
               } 
                  
                  
		  String remoteIP = req.getRemoteAddr();                                    
                  System.out.println("remote IP is currently: " + remoteIP);
                                                                                            
                  String mac = null;                                                                          
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
          String template1 = servletCont.getRealPath("/regwired.html");            
		  
		  BufferedReader in = new BufferedReader(new FileReader(template1));                    
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
                    out.println("<h4>TEST TEST TEST</h4>");
				    }
				}
			 }
			 else
				out.println(line);
		  }
		  in.close();
	   }


    } 
    /*
     * Processes HTTP POST requests from the servlet. Actions include:
     * -login: processes login action using CWU credentials
     * -register: registers a device to the network, stores device information in database
     * -edit: allows user to edit or remove registered devices on the network 
     * -update
     * -delete
     * -logout
     * -list-signup
     *
     * @param   req an HTTP request to the servlet object 
     * @param   res an HTTP response to the servlet object 
    */
    public void doPost(HttpServletRequest req, HttpServletResponse res)
        throws ServletException, IOException {
    	
    	res.setContentType("text/html");
    	PrintWriter out = res.getWriter();

    	// if the quiz was submitted then go back to it
    	if(!(req.getParameter("SubmitQuiz") == null)) {                             
               doQuiz(req, out);                                                   
               
        } 
           
        String action = req.getParameter("action");
        
	    if (action.equals("login")) {
	    	String ldapDN = null;
	    	HttpSession session = req.getSession(true);                        
	    	session.setAttribute("username", "");
	    	session.setAttribute("psid", "");
	    	String username = req.getParameter("username").toLowerCase();       
	    	//debug
	    	if (debug) System.out.println(new Date() + ": " + "RNTEST DEBUG - " + "User Submitted Username -> " + username);
            
	    	// CN entered
	    	Vector dnList = UserTools.findDN(username, "ldap.ad.cwu.edu", "dc=ad,dc=cwu,dc=edu");
		  
	    	if ((dnList == null) || (dnList.size() > 1) || ((dnList.indexOf("applicants") > -1) || (dnList.indexOf("alumni") > -1)) ) {
	    		session.invalidate();
                System.out.println("ln247");
	    		//errorMsgNew(out, AUTHENTICATION_FAILED);
	    		errorMsg(out, AUTHENTICATION_FAILED);
	    		return;
	    	}
	    	
	    	/*if ((dnList.indexOf("faculty & staff") > -1)) {
                session.invalidate();
                // need to have specialized redirect for staff to reg devices on campus network not resnet 
	    	} */ 
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
	    	String password = req.getParameter("pword");                        
	    	String psid = null;

	    	if (UserTools.ldapBind(ldapDN, password, "ldap.ad.cwu.edu") == false) { 
	    		//unable to bind to LDAP, they do not have an account!!
	    		//@TODO change system.out.println to a more elegant form of logging
	    		// System.out.println(new Date() + ": " + "Unable to bind to LDAP: " + dn);
	    		session.invalidate();
                        System.out.println("ln 277");
	    		//errorMsgNew(out, AUTHENTICATION_FAILED);
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
			 session.setAttribute("username", username);                
			 session.setAttribute("psid", psid);
			 // sendRedirect() does odd things pointed to servlet
			 out.println("<html>");
			 out.println("<head>");
			 out.println("<META HTTP-EQUIV=\"Refresh\" CONTENT=\"0;url=/test_rnreg/ResNetReg\">");
			 out.println("<title>Registration Redirect</title>");
			 out.println("</head>");
			 out.println("<body>");
			 out.println("</BODY>");
			 out.println("</HTML>"); 
	    }
	    
	    else if (action.equals("register")) {
	      String username = null;
	      String hostname = null;
	      String psid = null;
		  String nic = req.getParameter("nic");                         
		  String os = req.getParameter("os");                           
		  HttpSession session = req.getSession(false);
		  if ((session == null) ||
			 ((username = (String)session.getAttribute("username")) == null) ||
			 ((psid = (String)session.getAttribute("psid")) == null))
			 loginRedirect(res);
		  else if (nic == null || os == null) {
			 errorMsg(out, INCORRECT_PARAMETERS);
		  }
		  else {
			 String sqlString = "insert into resnet.hosts " +               // this insert statement could be incorrect, or the database config isnt spitting out info to portal 
				"(sid, dname, ha, os_id, vlan) values (" +
				psid + ", '" + username + "', '" + nic + "', " +
				"(select id from resnet.oses where " +
				"name='" + os + "'), " + vlan.intValue() + ")";
			 try {
                if (stmt == null) {
                    errorMsgStatement(out);
                }
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

                out.println("<HTML>");
                out.println("<HEAD>");
                out.println("<link type='text/css' rel='stylesheet' href='./CSS/primary.css' media='all' />");
                out.println("<META HTTP-EQUIV='expires' CONTENT='Sat, 9 July 1960 07:00:00 GMT'>");
                out.println("<META HTTP-EQUIV='Pragma' CONTENT='no-cache'>");
                out.println("<title>ResNet Registration Results</title>");
                out.println("<script src='//code.jquery.com/jquery-1.10.2.js'></script>");
                out.println("<script>");
                out.println("$(function(){$('#header').load('./header.html'); $('#footer').load('./footer.html'); });");
                out.println("</script>");
                out.println("</HEAD>");
                out.println("<BODY>");
                out.println("<div id='header'></div>");
                out.println("<div id='wrapper'>");
                out.println("<a name='main-content'></a>");
                out.println("<div class='verticalLine'>");
                out.println("<div id='left'>  <div class='region'>");
                out.println("<div id='block-system-main-menu' class='block block-menu'>");
                out.println("<div class='content'>");
                out.println("<p>&nbsp;</p><p>&nbsp;</p>");
                out.println("<ul class='menu'><li class='first collapsed  collapsed ' id='dhtml_menu-327'><a href='./register.html' title=''>Register a Device</a><ul class='menu'>");
                out.println("<li class='first' id='dhtml_menu-326'><a href='./regwired.html' title=''>Wired Device</a></li>");
                out.println("<li id='dhtml_menu-1449'><a href='./regwireless.html' title=''>Wireless Device (Game LAN)</a></li>");
                out.println("<li  id='dhtml_menu-1452'><a href='./gamehelp.html' title=''>Game Consoles Help</a></li>");
                out.println("<li class='last' id='dhtml_menu-1469'><a href='./wiredhelp.html' title=''>Wired Registration Help</a></li></ul></li>");
                out.println("<li class='collapsed collapsed' id='dhtml_menu-328'><a href='./editwired.html' title=''>Remove Devices</a><ul class='menu'>");
                out.println("<li class='first' id='dhtml_menu-1453'><a href='./editwired.html' title=''>Wireless Devices</a></li>");
                out.println("<li id='dhtml_menu-1471'><a href='./editwired.html' title=''>Wired Devices</a></li>");
                
                out.println("<li>  <br/><br/><br/><br/><br/><br/><br/><br/><br/><br/><br/><br/></li>");
                out.println("<li class='last' id='dhtml_menu-1468'><a href='https://www.cwu.edu/resnet/contact-us' title=''>Contact Us</a></li></ul>");
                out.println("</div>");
                out.println("</div>");
                out.println("<div id='block-menu-menu-related-links-menu' class='block block-menu'>");
                out.println("<div class='content'>");
                out.println("<ul class='menu'><li class='first last' id='dhtml_menu-1467'><a href='http://www.cwu.edu/its' title='' target='_blank'>IS Home</a></li></ul>");
                out.println("</div>");
                out.println("</div>");
                out.println("<div id='block-block-3'>");
                out.println("<div class='content'>");
                out.println("<p class='indent'><a href='/resnet/contact-us'>Resnet Support</a>:<br />(509) 963-2001<br /><a href='http://www.cwu.edu/its-css/computer-labs' target='_blank'>Computer Labs</a>:<br />(509) 963-2989<br /><a href='http://www.cwu.edu/its-helpdesk'>Service Desk</a>:<br />(509) 963-2001</p><p class='indent'>Hours:<br />Mon - Fri, 8 AM to 5 PM</p>");
                out.println("</div></div></div></div></div>");
                	
                out.println("<table style='width:100%'>");
                out.println("<tr>");
                out.println("<td><h2>You have successfully registered this computer!</h2></td>");
                out.println("</tr>");
                out.println("<tr>");
                out.println("<td><h3>Restart your computer after 15 minutes.</h3></td>");
                out.println("</tr>");
                out.println("<tr>");
                out.println("<td><h3>Your internet access will be enabled after you restart.</h3></td>");
                out.println("</tr>");
                out.println("<tr>");
                out.println("<td><u><b>Registration Information:</b></u></td><br>");
                out.println("</tr>");
                out.println("<tr>");
                out.println("<td><b>Registered to:</b> " + username + " <br></td>");
                out.println("</tr>");
                out.println("<tr>");
                out.println("<td><b>Hostname:</b>  " + hostname +" <br></td>");
                out.println("</tr>");
                out.println("<tr>");
                out.println("<td><b>Hardware Address:</b> " + nic + "<br></td>");
                out.println("</tr>");
                out.println("<tr>");
                out.println("<td><b>Operating System:</b> " + os + "<br></td>");
                out.println("</tr>");
                out.println("</table>");
                out.println("</div>"); // trying to get footer out of wrapper content 
                out.println("<div id='footer'></div>");
                out.println("</body>");
                out.println("</html>");


                
/*out.println("<hr noshade><p>As part of Central Washington University's Emergency Notification System (ENS), CWU has " +
"implemented several services including \"Desktop Alert!\". Desktop Alert! will broadcast a message to all computers " +
"connected to the CWU Campus and Residential Network (ResNet). Residential computers owned by students must opt-in to " +
"this system by clicking on the \"Accept\" button below. We highly recommend that all students registering computers on " +
"CWU ResNet accept this service.</p>");

out.println("<p>Please choose the correct version for your operating system:</p>");
out.println("<table width='100%' border='0'><tr>");
out.println("<th width='20%'>Windows XP/Vista/7/10<form name='download_windows' action='/rnreg/notify/client/resnet_notify.exe'><input value='Accept' type='submit'></form><a href='javascript:windows_help();'><font size=-2>instructions</font></a></th>");
out.println("<th width='10%'>-OR-</th>");
out.println("<th width='20%'>Mac OS<form name='download_mac' action='/rnreg/notify/client/ResNet-Notify.dmg'><input value='Accept' type='submit'></form><a href='javascript:mac_help();'><font size=-2>instructions</font></a></th>");
out.println("<th width='50%'><font size=-2>If you do not want to install the Desktop Alert! client, simply close this browser window.</font></th></tr></table>");
out.println("<p><font size=-2>Students who subscribe to this system will only receive safety related emergency notifications as they occur, along with a test message approximately once per quarter.  When an alert is sent, you will see a popup window on your screen <a href='javascript:netsupport_screenshot();'>(example)</a> that will give you information about the emergency.  All CWU owned computers will be subscribed to this system.  Your computer will only receive notifications when it is connected to the CWU Network (Campus or ResNet).  This system will <i>never</i> be used to spam students on non-emergency related situations.</font></p>"); */


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

			 }
			 catch (SQLException se) {
				if (se.toString().indexOf("ORA-00001") != -1)
				    errorMsg(out, NIC_EXISTS);
				else if (se.toString().indexOf("ORA-20001") != -1)
				    errorMsg(out, LIMIT_REACHED);
				else
				    //errorMsgInsertNew(out, INSERT_FAILED);
				    errorMsg(out, INSERT_FAILED);
			 }
		  }
	   }
	   
	   else if (action.equals("register-wireless")) {
	      String username = null;
	      String hostname = null;
	      String psid = null;
		  String nic = req.getParameter("nic");                         
		  String os = req.getParameter("os");                           
		  HttpSession session = req.getSession(false);
		  if ((session == null) ||
			 ((username = (String)session.getAttribute("username")) == null) ||
			 ((psid = (String)session.getAttribute("psid")) == null))
			 loginRedirect(res);
		  else if (nic == null || os == null) {
			 //errorMsg(out, INCORRECT_PARAMETERS);
			 errorMsgVLAN(out, nic, os);
		  }
		  else {
			 String sqlString = "insert into resnet.hosts " +               // this insert statement could be incorrect, or the database config isnt spitting out info to portal 
				"(sid, dname, ha, os_id, vlan) values (" +
				psid + ", '" + username + "', '" + nic + "', " +
				"(select id from resnet.oses where " +
				"name='" + os + "'), " + 807 + ")";
			 try {
                if (stmt == null) {
                    errorMsgStatement(out);
                }
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

                out.println("<HTML>");
                out.println("<HEAD>");
                out.println("<link type='text/css' rel='stylesheet' href='./CSS/primary.css' media='all' />");
                out.println("<META HTTP-EQUIV='expires' CONTENT='Sat, 9 July 1960 07:00:00 GMT'>");
                out.println("<META HTTP-EQUIV='Pragma' CONTENT='no-cache'>");
                out.println("<title>ResNet Registration Results</title>");
                out.println("<script src='//code.jquery.com/jquery-1.10.2.js'></script>");
                out.println("<script>");
                out.println("$(function(){$('#header').load('./header.html'); $('#footer').load('./footer.html'); });");
                out.println("</script>");
                out.println("</HEAD>");
                out.println("<BODY>");
                out.println("<div id='header'></div>");
                out.println("<div id='wrapper'>");
                out.println("<a name='main-content'></a>");
                out.println("<div class='verticalLine'>");
                out.println("<div id='left'>  <div class='region'>");
                out.println("<div id='block-system-main-menu' class='block block-menu'>");
                out.println("<div class='content'>");
                out.println("<p>&nbsp;</p><p>&nbsp;</p>");
                out.println("<ul class='menu'><li class='first collapsed  collapsed ' id='dhtml_menu-327'><a href='./register.html' title=''>Register a Device</a><ul class='menu'>");
                out.println("<li class='first' id='dhtml_menu-326'><a href='./regwired.html' title=''>Wired Device</a></li>");
                out.println("<li id='dhtml_menu-1449'><a href='./regwireless.html' title=''>Wireless Device (Game LAN)</a></li>");
                out.println("<li  id='dhtml_menu-1452'><a href='./gamehelp.html' title=''>Game Consoles Help</a></li>");
                out.println("<li class='last' id='dhtml_menu-1469'><a href='./wiredhelp.html' title=''>Wired Registration Help</a></li></ul></li>");
                out.println("<li class='collapsed collapsed' id='dhtml_menu-328'><a href='./editwired.html' title=''>Remove Devices</a><ul class='menu'>");
                out.println("<li class='first' id='dhtml_menu-1453'><a href='./editwired.html' title=''>Wireless Devices</a></li>");
                out.println("<li id='dhtml_menu-1471'><a href='./editwired.html' title=''>Wired Devices</a></li>");
                
                out.println("<li>  <br/><br/><br/><br/><br/><br/><br/><br/><br/><br/><br/><br/></li>");
                out.println("<li class='last' id='dhtml_menu-1468'><a href='https://www.cwu.edu/resnet/contact-us' title=''>Contact Us</a></li></ul>");
                out.println("</div>");
                out.println("</div>");
                out.println("<div id='block-menu-menu-related-links-menu' class='block block-menu'>");
                out.println("<div class='content'>");
                out.println("<ul class='menu'><li class='first last' id='dhtml_menu-1467'><a href='http://www.cwu.edu/its' title='' target='_blank'>IS Home</a></li></ul>");
                out.println("</div>");
                out.println("</div>");
                out.println("<div id='block-block-3'>");
                out.println("<div class='content'>");
                out.println("<p class='indent'><a href='/resnet/contact-us'>Resnet Support</a>:<br />(509) 963-2001<br /><a href='http://www.cwu.edu/its-css/computer-labs' target='_blank'>Computer Labs</a>:<br />(509) 963-2989<br /><a href='http://www.cwu.edu/its-helpdesk'>Service Desk</a>:<br />(509) 963-2001</p><p class='indent'>Hours:<br />Mon - Fri, 8 AM to 5 PM</p>");
                out.println("</div></div></div></div></div>");
                	
                out.println("<table style='width:220%'>");
                out.println("<tr>");
                out.println("<td><h2>You have successfully registered this device to CWU-play!</h2></td>");
                out.println("</tr>");
                out.println("<tr>");
                out.println("<td><h2>The password for this network is: </h2></td>");
                out.println("</tr>");
                
                out.println("<tr>");
                out.println("<td><p><mark><strong><font size='7'>wellington1891</font></strong></mark></p></td><br><br><br>");
                out.println("</tr>");
                
                
                out.println("<tr>");
                out.println("<td><h3>Restart your device after 15 minutes.</h3></td>");
                out.println("</tr>");
                out.println("<tr>");
                out.println("<td><h3>Your internet access will be enabled after you restart.</h3></td>");
                out.println("</tr>");
                
                out.println("<tr>");
                out.println("<td><p>&nbsp;</p><p>&nbsp;</p></td>");
                out.println("</tr>");
                
                out.println("<tr>");
                out.println("<td><u><b>Registration Information:</b></u></td><br>");
                out.println("</tr>");
                out.println("<tr>");
                out.println("<td><b>Registered to:</b> " + username + " <br></td>");
                out.println("</tr>");
                out.println("<tr>");
                out.println("<td><b>Hostname:</b>  " + hostname +" <br></td>");
                out.println("</tr>");
                out.println("<tr>");
                out.println("<td><b>Hardware Address:</b> " + nic + "<br></td>");
                out.println("</tr>");
                out.println("<tr>");
                out.println("<td><b>Operating System:</b> " + os + "<br></td>");
                out.println("</tr>");
                out.println("</table>");
                out.println("</div>"); // trying to get footer out of wrapper content 
                out.println("<div id='footer'></div>");
                out.println("</body>");
                out.println("</html>");
                
                
            }
			 catch (SQLException se) {
				if (se.toString().indexOf("ORA-00001") != -1)
				    errorMsg(out, NIC_EXISTS);
				else if (se.toString().indexOf("ORA-20001") != -1)
				    errorMsg(out, LIMIT_REACHED);
				else
				    errorMsgInsertVLAN(out, INSERT_FAILED, sqlString, se.getErrorCode(), se.getMessage());
			 }
		  }
	   }
           
	   else if (action.equals("edit")) {
		  String psid = null;
		  HttpSession session = req.getSession(false); // debug change getsession to true?? 
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
				    String sqlString = "select hostname, " + "CREATE_DATE, " + "OS_ID, " +
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
									       rs.getString("ha"), rs.getString("CREATE_DATE"), rs.getString("OS_ID")));
					   }
				    }
				    catch (SQLException se) { errorMsg(out, JDBC_ERROR); }
				    session.setAttribute("data", data);
				    if (data.isEmpty())
					   //errorMsgNew(out, RECORD_NOT_FOUND); --> This gets called if no data vector 
					   errorMsgNoRecord(out, RECORD_NOT_FOUND);
				    else
					   showRecord(req, out, data, 0);
					   
				}
				else {
				    // request for next or previous record
				    if (data.isEmpty())
					   //errorMsg(out, RECORD_NOT_FOUND);
					   errorMsgNoRecord(out, RECORD_NOT_FOUND);
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

    /*
     * Called to redirect user to wireless registration page if quiz is completed by user.
     *
     * @param   res an HTTP response to the servlet object
     */
    private void quizDoneRedirect(HttpServletResponse res) throws java.io.IOException {
	   res.sendRedirect("regwireless.html"); 
    } 
    
    /*
     * Processes all quiz-related logic for the user. Serves quiz and manages
     * database of users who have completed the AUP quiz required for
     * ResNet access.
     *
     * @param   req an HTTP request to the servlet object
     * @param   out a PrintWriter object in order to serve HTML to browser
     */
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
            if (passQuery.getString(1).equals("0")) {                               
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

        /* --------------------- old quiz html starts HERE -------------
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
                out.println("<body background='../test_rnreg/images/grey1.gif'>");      
           ---------------------------------------------------------------------- */
                out.println("<HTML>");
                out.println("<HEAD>");
                out.println("<link type='text/css' rel='stylesheet' href='./CSS/primary.css' media='all' />");
                out.println("<META HTTP-EQUIV='expires' CONTENT='Sat, 9 July 1960 07:00:00 GMT'>");
                out.println("<META HTTP-EQUIV='Pragma' CONTENT='no-cache'>");
                out.println("<title>ResNet Registration Quiz</title>");
                out.println("<script language=\"JavaScript\">");
                out.println("");
                out.println("function aup() {");
                out.println("open(\"resnet_aup.html\", \"window2\", ");
                out.println("\"toolbar=no,titlebar=false,width=600,height=350,resizable=yes,scrollbars=yes\");");  
                out.println("}");
                out.println("</script>");
                out.println("<script src='//code.jquery.com/jquery-1.10.2.js'></script>");
                out.println("<script>");
                out.println("$(function(){$('#header').load('./header2.html'); $('#footer').load('./footer.html'); });");
                out.println("</script>");
                out.println("</HEAD>");
                out.println("<BODY>");
                out.println("<div id='header'></div>");
                
                /* this is an attempt to move the quiz to the left a bit */
                out.println("<div id='wrapper'>");
                out.println("<a name='main-content'></a>");
                out.println("<div class='verticalLine'>");
                out.println("<div id='left'>  <div class='region'>");
                out.println("<div id='block-system-main-menu' class='block block-menu'>");
                out.println("<div class='content'>");
                out.println("<p>&nbsp;</p><p>&nbsp;</p>");
                out.println("</div></div></div></div></div>");
                
                
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
                        out.println("<br /> You have passed the quiz, Congratulations!</br>");
                        out.println("<br /> Please click <a href='ResNetReg'>Here</a> to proceed with registration.");
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
       			System.out.println("<p>attempting to debug broken quiz processing here 1048</p>");
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
                out.println("<br />Please Note: Students who have special needs or disabilities that may affect their ability to access information and/or material presented in this quiz are encouraged to contact ResNet Support for additional assistance. <br/>Before proceeding, please take a moment to review the <a href= 'policy.html' target=\"_blank\";>CWU ResNet Acceptable Use Policy</a><br/>");
                testQuiz.populateQuiz();  //creates the quiz
                out.println(testQuiz.Display());
                out.println("<br/>");
                out.println("<input type='hidden' name='action' value='quiz'>");
                out.println("<input type='submit' name='SubmitQuiz' value='Submit'>");
                out.println("<input type='reset' name='Reset' value='Reset'>");
                out.println("</form>");        

               }
           /*
                out.println("</body>");
                out.println("</html>"); */ //this is the end of the quiz html KB
                out.println("</div>"); // this should wrap the quiz and move it left?? 
                out.println("<div id='footer'></div>");
                out.println("</body>");
                out.println("</html>");
                out.close();  
                dbDatabase.Close(); //Closes the connection
           }

    /*
     * Subroutine to detect a user's operating system.
     *
     * @param   req an HTTP request to the servlet object
    */
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

    /*
     * Function that uses doPost()'s database data to populate a user's currently-registered
     * devices. Allows user choice to delete devices if they deem it necessary.
     *
     * @param   req     an HTTP request to the servlet object
     * @param   out     a PrintWriter object to write HTML to user's browser
     * @param   data    a Vector object to hold a user's registration data
     * @param   row     an integer that acts as a pointer for the vector
     */
    private void showRecord(HttpServletRequest req, PrintWriter out, Vector data, int row)
	   throws java.io.IOException {

	   int dataSize = data.size();
	   if (row >= dataSize || row < 0)
		  row = 0;
	   String hostname = ((RnRecord)data.elementAt(row)).getHostname();
	   String ha = ((RnRecord)data.elementAt(row)).getHa();
	   String createDate = ((RnRecord)data.elementAt(row)).getCreateDate();
	   String opSystem = ((RnRecord)data.elementAt(row)).getOpSystem();
	   
	   //int os = Integer.parseInt(opSystem);
	   int os = 0;
	   if (opSystem != null) {
            os = Integer.valueOf(opSystem);
	   }
	   
	   String resultOs = "";
	   
	   Enumeration names;
	   String key = "";
	   //int key;
	   
	   Hashtable<Integer, String> systems = new Hashtable<Integer, String>();
	   
	   systems.put(42, "Windows 8");
	   systems.put(37, "Windows 2003");
	   systems.put(38, "Windows (Other)");
	   systems.put(39, "Playstation 3");
	   systems.put(40, "Wii");
	   systems.put(41, "Windows 7");
	   systems.put(62, "Windows 10");
	   systems.put(53, "Windows 8.1");
	   systems.put(54, "Playstation 4");
	   systems.put(55, "Xbox One");
	   systems.put(1, "Unknown");
	   systems.put(2, "Other");
	   systems.put(43, "Android");
	   systems.put(7, "Windows NT");
	   systems.put(9, "Linux");
	   systems.put(11, "Free BSD");
	   systems.put(12, "Mac OS X");
	   systems.put(13, "Mac OS (PPC)");
	   systems.put(14, "Windows XP");
	   systems.put(15, "Xbox");
	   systems.put(16, "Playstation");
	   systems.put(17, "Xbox 360");
	   systems.put(27, "Windows Vista");
	   systems.put(72, "Wii U");
	   systems.put(73, "Nintendo Switch");
	   systems.put(74, "Apple TV");
	   systems.put(75, "Smart TV");
	   systems.put(76, "Roku");
	   systems.put(77, "FireStick");
	   
	   names = systems.keys();
	   
	   // perhaps just add a try/catch here to catch the NullPointerException if it were to occur  
	   if (os != 0) {
	   // add an if (os) {} here and wrap the while loop inside of it in case oS is null??  
          while (names.hasMoreElements()) {
            key = names.nextElement().toString();
            int temp = Integer.valueOf(key);
            if (temp == os) {
                resultOs = systems.get(Integer.parseInt(key)); // could replace Integer.parseInt(key)) with temp here?? 
            }
          }
	   }
	   // The following is an attempt to get the MACVendor API functionality integrated into hostreg
	   // Follow-up: it works 
	   
	   String baseURL = "http://api.macvendors.com/";
	   String macVendor = "";
	   
	   
	   try {
            StringBuilder result = new StringBuilder();
            URL url = new URL(baseURL + ha);
            HttpURLConnection conn = (HttpURLConnection)url.openConnection();
            conn.setRequestMethod("GET");
            BufferedReader rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String line = "";
            
            while ((line = rd.readLine()) != null) {
                result.append(line);
            }
            rd.close();
            macVendor = result.toString();
            
	   
	   } catch (FileNotFoundException e) {
            System.out.println(e.toString());
            
	   } catch (IOException e) {
            System.out.println(e.toString());
	   }
	   

	   String template2 = req.getRealPath("/WEB-INF/classes/" + TEMPLATE2);
	   BufferedReader in = new BufferedReader(new FileReader(template2));
	   String line;
	   while ((line = in.readLine()) != null) {
		  // parameter substitution
		  if (line.indexOf("<!-- %") != -1) {
			 if (line.indexOf("ROWNUM") != -1)
				out.println("curRow = " + row + ";");
				
            else if (line.indexOf("MACVENDOR") != -1)
                out.println("<input type=hidden name=OPSYSTEM value=\"" + macVendor +
						  "\">" + macVendor);
				
			else if (line.indexOf("OPSYSTEM") != -1)
                out.println("<input type=hidden name=OPSYSTEM value=\"" + resultOs +
						  "\">" + resultOs);	
				
             else if (line.indexOf("CREATE_DATE") != -1)
                out.println("<input type=hidden name=CREATE_DATE value=\"" + createDate +
						  "\">" + createDate);
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

    /*
     * Function that uses doPost()'s database data to update a user's currently-registered
     * devices. Allows user choice to edit or update devices if they deem it necessary.
     *
     * @param   req     an HTTP request to the servlet object
     * @param   res     an HTTP response to the servlet object
     * @param   out     a PrintWriter object to write HTML to user's browser
     */
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

    /*
     * Function that uses doPost()'s database data to delete a user's currently-registered
     * devices from the ResNet database.
     *
     * @param   req     an HTTP request to the servlet object
     * @param   res     an HTTP response to the servlet object
     * @param   out     a PrintWriter object to write HTML to user's browser
     */
    private void deleteRecord(HttpServletRequest req, HttpServletResponse res, PrintWriter out)
	   throws java.io.IOException {

	   String username = null;
	   Vector data = null;
	   HttpSession session = req.getSession(false);
	   
	   if ((session == null) ||
		  ((username = (String)session.getAttribute("username")) == null) ||
		  ((data = (Vector)session.getAttribute("data")) == null))
		  loginRedirect(res);
		  //errorMsgRemoveDevice(out, username, data);
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
		  catch (SQLException se) { 
            errorMsg(out, DELETE_FAILED); 
            errorMsgDelete(out, DELETE_FAILED, sqlString, se.getErrorCode(), se.getMessage());
          }
	   }

    }

    /*
     * Simple redirect function to redirect back to the servlet's login page.
     *
     * @param   res     an HTTP response to the servlet object
     */
    private void loginRedirect(HttpServletResponse res) throws java.io.IOException {
	   res.sendRedirect("login.html");
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

    /* private void errorMsg(PrintWriter out, int errNo) {

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

    } */
    
    private void errorMsg(PrintWriter out, int errNo) {
        
        String redirectLink = "";
        if (errNo == -1 || errNo == -2) {
            redirectLink = "./login.html";
        } else {
            redirectLink = "./regwired.html";
        }
    
        out.println("<HTML>");
        out.println("<HEAD>");
        out.println("<link type='text/css' rel='stylesheet' href='./CSS/primary.css' media='all' />");
        out.println("<META HTTP-EQUIV='expires' CONTENT='Sat, 9 July 1960 07:00:00 GMT'>");
        out.println("<META HTTP-EQUIV='Pragma' CONTENT='no-cache'>");
        out.println("<title>ResNet Registration Results</title>");
        out.println("<script src='//code.jquery.com/jquery-1.10.2.js'></script>");
        out.println("<script>");
        out.println("$(function(){$('#header').load('./header2.html'); $('#footer').load('./footer.html'); });");
        out.println("</script>");
        out.println("</HEAD>");
        out.println("<BODY>");
        out.println("<div id='header'></div>");
        out.println("<div id='wrapper'>");
        out.println("<a name='main-content'></a>");
        out.println("<div class='verticalLine'>");
        out.println("<div id='left'>  <div class='region'>");
        out.println("<div id='block-system-main-menu' class='block block-menu'>");
        out.println("<div class='content'>");
        out.println("<p>&nbsp;</p><p>&nbsp;</p>");
        out.println("<ul class='menu'><li class='first collapsed  collapsed ' id='dhtml_menu-327'><a href='./register.html' title=''>Register a Device</a><ul class='menu'>");
        out.println("<li class='first' id='dhtml_menu-326'><a href='./regwired.html' title=''>Wired Device</a></li>");
        out.println("<li id='dhtml_menu-1449'><a href='./regwireless.html' title=''>Wireless Device (Game LAN)</a></li>");
        out.println("<li  id='dhtml_menu-1452'><a href='./gamehelp.html' title=''>Game Consoles Help</a></li>");
        out.println("<li class='last' id='dhtml_menu-1469'><a href='./wiredhelp.html' title=''>Wired Registration Help</a></li></ul></li>");
        out.println("<li class='collapsed collapsed' id='dhtml_menu-328'><a href='./editwired.html' title=''>Remove Devices</a><ul class='menu'>");
        out.println("<li class='first' id='dhtml_menu-1453'><a href='./editwired.html' title=''>Wireless Devices</a></li>");
        out.println("<li id='dhtml_menu-1471'><a href='./editwired.html' title=''>Wired Devices</a></li>");
        out.println("<li>  <br/><br/><br/><br/><br/><br/><br/><br/><br/><br/><br/><br/></li>");
        out.println("<li class='last' id='dhtml_menu-1468'><a href='https://www.cwu.edu/resnet/contact-us' title=''>Contact Us</a></li></ul>");
        out.println("</div>");
        out.println("</div>");
        out.println("<div id='block-menu-menu-related-links-menu' class='block block-menu'>");
        out.println("<div class='content'>");
        out.println("<ul class='menu'><li class='first last' id='dhtml_menu-1467'><a href='http://www.cwu.edu/its' title='' target='_blank'>IS Home</a></li></ul>");
        out.println("</div>");
        out.println("</div>");
        out.println("<div id='block-block-3'>");
        out.println("<div class='content'>");
        out.println("<p class='indent'><a href='/resnet/contact-us'>Resnet Support</a>:<br />(509) 963-2001<br /><a href='http://www.cwu.edu/its-css/computer-labs' target='_blank'>Computer Labs</a>:<br />(509) 963-2989<br /><a href='http://www.cwu.edu/its-helpdesk'>Service Desk</a>:<br />(509) 963-2001</p><p class='indent'>Hours:<br />Mon - Fri, 8 AM to 5 PM</p>");
        out.println("</div></div></div></div></div>");
                	
        out.println("<table style='width:220%'>");
        out.println("<tr>");
        out.println("<td><h2>An unexpected error has occurred.</h2></td>");
        out.println("</tr>");
        out.println("<tr>");
        out.println("<td><h2>Please click <a href='" + redirectLink + "'>here</a> to try again. </h2></td>");
        out.println("</tr>");
        out.println("<tr>");
        out.println("<td><h3>Error " + errNo + ": " + errors[Math.abs(errNo) - 1] +
				"<BR></h3></td>");
        out.println("</tr>");
        out.println("</table>");
        out.println("</div>"); // trying to get footer out of wrapper content 
        out.println("<div id='footer'></div>");
        out.println("</body>");
        out.println("</html>");
    
    }
    
    private void errorMsgVLAN(PrintWriter out, String nic, String os) {
        

	   out.println("<HTML>");
	   out.println("<HEAD>");
	   out.println("<TITLE>GameLAN Insert Error</TITLE>");
	   out.println("</HEAD>");
	   out.println("<BODY background=\"images/grey1.gif\">");
	   out.println("<font face=Arial size=-1>");
	   out.println("<p>User nic: " + nic + "</p><br>");
	   out.println("<p>User os: " + os + "</p><br>");
	   out.println("</font>");
	   out.println("</BODY>");
	   out.println("</HTML>");
    
    }
    
    private void errorMsgNew(PrintWriter out, int errNo) {
        out.println("<HTML>");
        out.println("<HEAD>");
        out.println("<link type='text/css' rel='stylesheet' href='./CSS/primary.css' media='all' />");
        out.println("<META HTTP-EQUIV='expires' CONTENT='Sat, 9 July 1960 07:00:00 GMT'>");
        out.println("<META HTTP-EQUIV='Pragma' CONTENT='no-cache'>");
        out.println("<title>ResNet Registration Results</title>");
        out.println("<script src='//code.jquery.com/jquery-1.10.2.js'></script>");
        out.println("<script>");
        out.println("$(function(){$('#header').load('./header2.html'); $('#footer').load('./footer.html'); });");
        out.println("</script>");
        out.println("</HEAD>");
        out.println("<BODY>");
        out.println("<div id='header'></div>");
        out.println("<div id='wrapper'>");
        out.println("<a name='main-content'></a>");
        out.println("<div class='verticalLine'>");
        out.println("<div id='left'>  <div class='region'>");
        out.println("<div id='block-system-main-menu' class='block block-menu'>");
        out.println("<div class='content'>");
        out.println("<p>&nbsp;</p><p>&nbsp;</p>");
        out.println("<ul class='menu'><li class='first collapsed  collapsed ' id='dhtml_menu-327'><a href='./register.html' title=''>Register a Device</a><ul class='menu'>");
        out.println("<li class='first' id='dhtml_menu-326'><a href='./regwired.html' title=''>Wired Device</a></li>");
        out.println("<li id='dhtml_menu-1449'><a href='./regwireless.html' title=''>Wireless Device (Game LAN)</a></li>");
        out.println("<li  id='dhtml_menu-1452'><a href='./gamehelp.html' title=''>Game Consoles Help</a></li>");
        out.println("<li class='last' id='dhtml_menu-1469'><a href='./wiredhelp.html' title=''>Wired Registration Help</a></li></ul></li>");
        out.println("<li class='collapsed collapsed' id='dhtml_menu-328'><a href='./editwired.html' title=''>Remove Devices</a><ul class='menu'>");
        out.println("<li class='first' id='dhtml_menu-1453'><a href='./editwired.html' title=''>Wireless Devices</a></li>");
        out.println("<li id='dhtml_menu-1471'><a href='./editwired.html' title=''>Wired Devices</a></li>");
        out.println("<li>  <br/><br/><br/><br/><br/><br/><br/><br/><br/><br/><br/><br/></li>");
        out.println("<li class='last' id='dhtml_menu-1468'><a href='https://www.cwu.edu/resnet/contact-us' title=''>Contact Us</a></li></ul>");
        out.println("</div>");
        out.println("</div>");
        out.println("<div id='block-menu-menu-related-links-menu' class='block block-menu'>");
        out.println("<div class='content'>");
        out.println("<ul class='menu'><li class='first last' id='dhtml_menu-1467'><a href='http://www.cwu.edu/its' title='' target='_blank'>IS Home</a></li></ul>");
        out.println("</div>");
        out.println("</div>");
        out.println("<div id='block-block-3'>");
        out.println("<div class='content'>");
        out.println("<p class='indent'><a href='/resnet/contact-us'>Resnet Support</a>:<br />(509) 963-2001<br /><a href='http://www.cwu.edu/its-css/computer-labs' target='_blank'>Computer Labs</a>:<br />(509) 963-2989<br /><a href='http://www.cwu.edu/its-helpdesk'>Service Desk</a>:<br />(509) 963-2001</p><p class='indent'>Hours:<br />Mon - Fri, 8 AM to 5 PM</p>");
        out.println("</div></div></div></div></div>");
                	
        out.println("<table style='width:220%'>");
        out.println("<tr>");
        out.println("<td><h2>Incorrect username and/or password.</h2></td>");
        out.println("</tr>");
        out.println("<tr>");
        out.println("<td><h2>Please click <a href='./login.html'>here</a> to try again. </h2></td>");
        out.println("</tr>");
        out.println("<tr>");
        out.println("<td><h3>Error " + errNo + ": " + errors[Math.abs(errNo) - 1] +
				"<BR></h3></td>");
        out.println("</tr>");
        out.println("</table>");
        out.println("</div>"); // trying to get footer out of wrapper content 
        out.println("<div id='footer'></div>");
        out.println("</body>");
        out.println("</html>");
    }
    
    private void errorMsgInsert(PrintWriter out, int errNo, String error, int errorCode, String message) {

	   out.println("<HTML>");
	   out.println("<HEAD>");
	   out.println("<TITLE>Registration Error</TITLE>");
	   out.println("</HEAD>");
	   out.println("<BODY background=\"images/grey1.gif\">");
	   out.println("<font face=Arial size=-1>");
	   out.println("Error " + errNo + ": " + errors[Math.abs(errNo) - 1] +
				"<BR>");
       out.println("SQL string is: " + error.toString() + "<BR>");
       out.println("SQL error code via getErrorCode() is: " + String.valueOf(errorCode) + "<BR>");
       out.println("SQL error message via getMessage is: " + message + "<BR>");
	   out.println("</font>");
	   out.println("</BODY>");
	   out.println("</HTML>");

    }
    
    private void errorMsgNoRecord(PrintWriter out, int errNo) {
        out.println("<HTML>");
        out.println("<HEAD>");
        out.println("<link type='text/css' rel='stylesheet' href='./CSS/primary.css' media='all' />");
        out.println("<META HTTP-EQUIV='expires' CONTENT='Sat, 9 July 1960 07:00:00 GMT'>");
        out.println("<META HTTP-EQUIV='Pragma' CONTENT='no-cache'>");
        out.println("<title>ResNet Registration Results</title>");
        out.println("<script src='//code.jquery.com/jquery-1.10.2.js'></script>");
        out.println("<script>");
        out.println("$(function(){$('#header').load('./header.html'); $('#footer').load('./footer.html'); });");
        out.println("</script>");
        out.println("</HEAD>");
        out.println("<BODY>");
        out.println("<div id='header'></div>");
        out.println("<div id='wrapper'>");
        out.println("<a name='main-content'></a>");
        out.println("<div class='verticalLine'>");
        out.println("<div id='left'>  <div class='region'>");
        out.println("<div id='block-system-main-menu' class='block block-menu'>");
        out.println("<div class='content'>");
        out.println("<p>&nbsp;</p><p>&nbsp;</p>");
        out.println("<ul class='menu'><li class='first collapsed  collapsed ' id='dhtml_menu-327'><a href='./register.html' title=''>Register a Device</a><ul class='menu'>");
        out.println("<li class='first' id='dhtml_menu-326'><a href='./regwired.html' title=''>Wired Device</a></li>");
        out.println("<li id='dhtml_menu-1449'><a href='./regwireless.html' title=''>Wireless Device (Game LAN)</a></li>");
        out.println("<li  id='dhtml_menu-1452'><a href='./gamehelp.html' title=''>Game Consoles Help</a></li>");
        out.println("<li class='last' id='dhtml_menu-1469'><a href='./wiredhelp.html' title=''>Wired Registration Help</a></li></ul></li>");
        out.println("<li class='collapsed collapsed' id='dhtml_menu-328'><a href='./editwired.html' title=''>Remove Devices</a><ul class='menu'>");
        out.println("<li class='first' id='dhtml_menu-1453'><a href='./editwired.html' title=''>Wireless Devices</a></li>");
        out.println("<li id='dhtml_menu-1471'><a href='./editwired.html' title=''>Wired Devices</a></li>");
       
        out.println("<li>  <br/><br/><br/><br/><br/><br/><br/><br/><br/><br/><br/><br/></li>");
        out.println("<li class='last' id='dhtml_menu-1468'><a href='https://www.cwu.edu/resnet/contact-us' title=''>Contact Us</a></li></ul>");
        out.println("</div>");
        out.println("</div>");
        out.println("<div id='block-menu-menu-related-links-menu' class='block block-menu'>");
        out.println("<div class='content'>");
        out.println("<ul class='menu'><li class='first last' id='dhtml_menu-1467'><a href='http://www.cwu.edu/its' title='' target='_blank'>IS Home</a></li></ul>");
        out.println("</div>");
        out.println("</div>");
        out.println("<div id='block-block-3'>");
        out.println("<div class='content'>");
        out.println("<p class='indent'><a href='/resnet/contact-us'>Resnet Support</a>:<br />(509) 963-2001<br /><a href='http://www.cwu.edu/its-css/computer-labs' target='_blank'>Computer Labs</a>:<br />(509) 963-2989<br /><a href='http://www.cwu.edu/its-helpdesk'>Service Desk</a>:<br />(509) 963-2001</p><p class='indent'>Hours:<br />Mon - Fri, 8 AM to 5 PM</p>");
        out.println("</div></div></div></div></div>");
                	
        out.println("<table style='width:220%'>");
        out.println("<tr>");
        out.println("<td><h2>No devices found..</h2></td>");
        out.println("</tr>");
        out.println("<tr>");
        out.println("<td><h2>Please click <a href='./regwired.html'>here</a> to start registering devices. </h2></td>");
        out.println("</tr>");
        out.println("<tr>");
        out.println("<td><h3>Error " + errNo + ": " + errors[Math.abs(errNo) - 1] +
				"<BR></h3></td>");
        out.println("</tr>");
        out.println("</table>");
        out.println("</div>"); // trying to get footer out of wrapper content 
        out.println("<div id='footer'></div>");
        out.println("</body>");
        out.println("</html>");
    }
    
    private void errorMsgInsertNew(PrintWriter out, int errNo) {
        out.println("<HTML>");
        out.println("<HEAD>");
        out.println("<link type='text/css' rel='stylesheet' href='./CSS/primary.css' media='all' />");
        out.println("<META HTTP-EQUIV='expires' CONTENT='Sat, 9 July 1960 07:00:00 GMT'>");
        out.println("<META HTTP-EQUIV='Pragma' CONTENT='no-cache'>");
        out.println("<title>ResNet Registration Results</title>");
        out.println("<script src='//code.jquery.com/jquery-1.10.2.js'></script>");
        out.println("<script>");
        out.println("$(function(){$('#header').load('./header.html'); $('#footer').load('./footer.html'); });");
        out.println("</script>");
        out.println("</HEAD>");
        out.println("<BODY>");
        out.println("<div id='header'></div>");
        out.println("<div id='wrapper'>");
        out.println("<a name='main-content'></a>");
        out.println("<div class='verticalLine'>");
        out.println("<div id='left'>  <div class='region'>");
        out.println("<div id='block-system-main-menu' class='block block-menu'>");
        out.println("<div class='content'>");
        out.println("<p>&nbsp;</p><p>&nbsp;</p>");
        out.println("<ul class='menu'><li class='first collapsed  collapsed ' id='dhtml_menu-327'><a href='./register.html' title=''>Register a Device</a><ul class='menu'>");
        out.println("<li class='first' id='dhtml_menu-326'><a href='./regwired.html' title=''>Wired Device</a></li>");
        out.println("<li id='dhtml_menu-1449'><a href='./regwireless.html' title=''>Wireless Device (Game LAN)</a></li>");
        out.println("<li  id='dhtml_menu-1452'><a href='./gamehelp.html' title=''>Game Consoles Help</a></li>");
        out.println("<li class='last' id='dhtml_menu-1469'><a href='./wiredhelp.html' title=''>Wired Registration Help</a></li></ul></li>");
        out.println("<li class='collapsed collapsed' id='dhtml_menu-328'><a href='./editwired.html' title=''>Remove Devices</a><ul class='menu'>");
        out.println("<li class='first' id='dhtml_menu-1453'><a href='./editwired.html' title=''>Wireless Devices</a></li>");
        out.println("<li id='dhtml_menu-1471'><a href='./editwired.html' title=''>Wired Devices</a></li>");
        
        out.println("<li>  <br/><br/><br/><br/><br/><br/><br/><br/><br/><br/><br/><br/></li>");
        out.println("<li class='last' id='dhtml_menu-1468'><a href='https://www.cwu.edu/resnet/contact-us' title=''>Contact Us</a></li></ul>");
        out.println("</div>");
        out.println("</div>");
        out.println("<div id='block-menu-menu-related-links-menu' class='block block-menu'>");
        out.println("<div class='content'>");
        out.println("<ul class='menu'><li class='first last' id='dhtml_menu-1467'><a href='http://www.cwu.edu/its' title='' target='_blank'>IS Home</a></li></ul>");
        out.println("</div>");
        out.println("</div>");
        out.println("<div id='block-block-3'>");
        out.println("<div class='content'>");
        out.println("<p class='indent'><a href='/resnet/contact-us'>Resnet Support</a>:<br />(509) 963-2001<br /><a href='http://www.cwu.edu/its-css/computer-labs' target='_blank'>Computer Labs</a>:<br />(509) 963-2989<br /><a href='http://www.cwu.edu/its-helpdesk'>Service Desk</a>:<br />(509) 963-2001</p><p class='indent'>Hours:<br />Mon - Fri, 8 AM to 5 PM</p>");
        out.println("</div></div></div></div></div>");
                	
        out.println("<table style='width:220%'>");
        out.println("<tr>");
        out.println("<td><h2>Failed to insert record. Please enter a valid MAC address.</h2></td>");
        out.println("</tr>");
        out.println("<tr>");
        out.println("<td><h2>Please click <a href='./regwired.html'>here</a> to try again. </h2></td>");
        out.println("</tr>");
        out.println("<tr>");
        out.println("<td><h3>Error " + errNo + ": " + errors[Math.abs(errNo) - 1] +
				"<BR></h3></td>");
        out.println("</tr>");
        out.println("</table>");
        out.println("</div>"); // trying to get footer out of wrapper content 
        out.println("<div id='footer'></div>");
        out.println("</body>");
        out.println("</html>");
    }
    
    private void errorMsgInsertVLAN(PrintWriter out, int errNo, String error, int errorCode, String message) {

	   out.println("<HTML>");
	   out.println("<HEAD>");
	   out.println("<TITLE>Registration Error</TITLE>");
	   out.println("</HEAD>");
	   out.println("<BODY background=\"images/grey1.gif\">");
	   out.println("<font face=Arial size=-1>");
	   out.println("Error " + errNo + ": " + errors[Math.abs(errNo) - 1] +
				"<BR>");
       out.println("SQL string is: " + error.toString() + "<BR>");
       out.println("SQL error code via getErrorCode() is: " + String.valueOf(errorCode) + "<BR>");
       out.println("SQL error message via getMessage is: " + message + "<BR>");
       out.println("This error is exclusive to the VLAN, so the action works (line 695)<BR>");
	   out.println("</font>");
	   out.println("</BODY>");
	   out.println("</HTML>");

    }
    
    private void errorMsgDelete(PrintWriter out, int errNo, String error, int errorCode, String message) {

	   out.println("<HTML>");
	   out.println("<HEAD>");
	   out.println("<TITLE>Deletion Error</TITLE>");
	   out.println("</HEAD>");
	   out.println("<BODY background=\"images/grey1.gif\">");
	   out.println("<font face=Arial size=-1>");
	   out.println("Error " + errNo + ": " + errors[Math.abs(errNo) - 1] +
				"<BR>");
       out.println("SQL string is: " + error.toString() + "<BR>");
       out.println("SQL error code via getErrorCode() is: " + String.valueOf(errorCode) + "<BR>");
       out.println("SQL error message via getMessage is: " + message + "<BR>");
	   out.println("</font>");
	   out.println("</BODY>");
	   out.println("</HTML>");

    }
    private void errorMsgRemoveDevice(PrintWriter out, String name, Vector data) {

       boolean flag = false;
	   if (data == null) {
         flag = true;
	   }
	   
	   out.println("<HTML>");
	   out.println("<HEAD>");
	   out.println("<TITLE>Deletion Error</TITLE>");
	   out.println("</HEAD>");
	   out.println("<BODY background=\"images/grey1.gif\">");
	   out.println("<font face=Arial size=-1>");
       out.println("Username of session is: " + name.toString() + "<BR>");
       //out.println("Vector contents: " + data.toString() + "<BR>");
       //out.println("Vector size: " + data.size() + "<BR>");
       //out.println("Vector null? " + data.isEmpty() + "<BR>");
       out.println("If true, vector is null ---> " + flag + "<BR>");
	   out.println("</font>");
	   out.println("</BODY>");
	   out.println("</HTML>");

    }
    
    
    private void errorMsgStatement(PrintWriter out) {

	   out.println("<HTML>");
	   out.println("<HEAD>");
	   out.println("<TITLE>Registration Error</TITLE>");
	   out.println("</HEAD>");
	   out.println("<BODY background=\"images/grey1.gif\">");
	   out.println("<font face=Arial size=-1>");
	   out.println("Statement is null --> line 400 ResNetReg.java");
	   out.println("</font>");
	   out.println("</BODY>");
	   out.println("</HTML>");

    }

}
