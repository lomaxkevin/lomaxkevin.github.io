/*
 * $CwuRes: src/regi/www/webapps/rnreg/src/Category.java,v 1.3 2007/03/08 00:13:03 jgerdes Exp $
 * Category.java
 *  This object holds the questions for the given category. It is populated by using the getEmpty()
 *  funtion to get the next empty question which is then replaced with an actual question. When
 *  the quiz is being populated the Display() function is called to display the questions.
 *
 *  Required Objects:
 *      Question
 *
 *  Required Functions:
 *      Question.Display()
 *  
 *  Supplied Functions:
 *      Display()
 *      getEmpty()
 *
 * Created on November 21, 2006 by Josh Turner
 * Last Modified on February 26th, 2006 by Josh Turner
 */

public class Category {
    public static final long serialVersionUID = 110;
    /*
     * 1.0.0 - Initial Release
     * 1.1.0 - Reworked to use the new normalized database.
     */
    
    public String Name = "";
    public Question Questions[];
    private int intPassingScore = -1;
    public String FullName = "";
    
    public int id;
    
    /** Creates a new instance of Category */   
    public Category(int newId, String newName, int newCount) {
        //New
        FullName = newName;
        id = newId;
        Question newQuestions[] = new Question[newCount];
        Questions = newQuestions;
    }
    
    public String Display() {
        //Displays the name and cycles through to run display on each question object
        String tmpString = "<br /><b>" +FullName +":</b> <br/>";
        for (int y = 0; y < Questions.length; y++) {
            if (!(Questions[y] == null)) {
                tmpString += Questions[y].Display() + "<br/>";                
            }
        }
        return tmpString;
    }
    
    public int getEmpty() {
        /*
         * This is used for populating the category. It simply returns the ID of an empty question
         * if there is one.
         */
        
        int tmpInt = -1; // If none are empty it returns -1
        for (int i = 0; i < Questions.length; i++) { // loops through the questions
            if (Questions[i] == null) { // if the question object is null then it returns it records the number
                tmpInt = i;
                break; // breaks out
            }
        }
        return tmpInt; // returns the number of an empty one, or it returns -1
    }
}
