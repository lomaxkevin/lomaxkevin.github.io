/*
 * $CwuRes: src/regi/www/webapps/rnreg/src/Database.java,v 1.11 2019/09/03 21:28:39 resnet Exp $
 * Database.java
 *  This object holds the connection to the Oracle database and pulls the information when requested 
 *  via the supplied functions. The database information needs to be modified at the top with the 
 *  correct information if the data is ever moved.
 *
 *  Required Objects:
 *      None
 *
 *  Required Functions:
 *      None
 *  
 *  Supplied Functions:
 *      RunQuery() -- This is used to run queries that DO NOT have a ResultSet, such as Insert and Update
 *      SelectAll() -- Selects the whole table and returns it as a ResultSet
 *      SelectQuery() -- Used to run queries that DO have a ResultSet, such as Select
 *      Close() -- Allows the connection to be closed (currently does nothing)
 *
 * Created on November 28th, 2006 by Josh Turner
 * Last Modified on February 26th, 2006 by Josh Turner
 */

import java.sql.*;

import javax.swing.*;

public final class Database {
	
	public static final long serialVersionUID = 110;
	/*
	 * 1.0.0 - Initial Release
         * 1.0.1 - Changed error messages to be System.out.println so there is no graphical
         * 1.0.2 - Extremely simplified the class
         * 1.1.0 - Reworked to use the new normalized database.
	 */
	
	/* Internal Variables */
	private String strDriver = "oracle.jdbc.driver.OracleDriver";	
	private String strType = "Oracle 12";
	private String strDatabase = null;
	
    /* These are the creds for the live mot database cnxn */
	private String strHost = "mot.resnet.cwu.edu:1521:resnet";
	private String strUser = "resnet";
	private String strPassword = "dukenukem";
	
	/* These are the creds for the test anat database cnxn */
	//private String strHost = "anat.cts.cwu.edu:1521:resnet";                // removed ".networks.cwu.edu" from the end of strHost 
	//private String strUser = "resnet";
	//private String strPassword = "fakeN3ws!";
	
	private String strDatabaseURL ="jdbc:oracle:thin:@"+strHost;
	
    private String TABLE = "RESNET.REGIQUIZ_QUESTIONS";
    private static Connection connection = null;
    private static Statement statement;
    private String strErrors = ""; 
       
	/* Set to not need creation vars*/
	public Database() {
	}
	
	private void getConnection() {
            /* Establishses and passes a connection if there isn't one, otherwise it uses it again */		
            if(connection == null) {
                try {
                    Class.forName(strDriver); //sets the driver 
                    connection = DriverManager.getConnection(strDatabaseURL, strUser, strPassword); //sets up the connection
                    System.out.println("The try block in getConnection works too");
                }
                //Error Handling
                catch (SQLException ex) {
                    while (ex != null) {  
                            strErrors += (ex.getMessage ());
                            ex = ex.getNextException ();  
                     } 
                    System.out.println("SQL Exception:  THE ERRORS ARE COMING FROM INSIDE THE DATABASE.java getConnection() \n" +strErrors);
                }  
                catch (ClassNotFoundException ex) {
                    System.out.println("Error Loading Driver:  " + ex );
                }
                catch (java.lang.Exception ex) {
                    System.out.println("Exception:  " + ex.getMessage ());
                }
            } 
        }
	
        public boolean RunQuery(String newQuery) {
            /* 
             * This function is used to run a query that does not return results. 
             * It is to be passed an update, insert, or similar query and will process that.
             * If the query succeeds then it returns true, otherwise it returns false.             
             */
            getConnection(); //gets a connection            
            try {
                //Creates a statement and runs the query on the connection
                statement = connection.createStatement();
                statement.executeUpdate(newQuery); 
                return true;
            }
            //Error Handling
            catch (SQLException ex) {
                    while (ex != null) {  
                    strErrors += ("\nSQL Exception:  " + ex.getMessage ());
                    ex = ex.getNextException ();  
                    } 
                    System.out.println(strErrors);
                    return false;

            }  
            catch (java.lang.Exception ex) {
                    System.out.println("Exception:  " + ex.getMessage ());
                    return false;
            }
        }
        
        public ResultSet SelectQuery(String newQuery) {
            /* 
             * This function I used to do a query that does return results and those results
             * are returned as a ResultSet and is set on the first line.
             * Queries sent to this will generally be select queries
             */
            
            getConnection(); //gets a connection
            try {
                //creates a statement that can be scrolled through more than once
                statement = connection.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE);
                ResultSet results = statement.executeQuery(newQuery);  //executes the query
                results.next();//starts on the first entry
                return results;
            }
            //Error Handling
            catch (SQLException ex) {
                    while (ex != null) {  
                    strErrors += ("\nSQL Exception:  " + ex.getMessage ());
                    ex = ex.getNextException ();  
                    } 
                    System.out.println(strErrors);
                    return null;

            }  
            catch (java.lang.Exception ex) {
                    System.out.println("Exception:  " + ex.getMessage ());
                    return null;
            }
        }
        
        public void Close() {
        }
 }      
