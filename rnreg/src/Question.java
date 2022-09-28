/*
 * $CwuRes: src/regi/www/webapps/rnreg/src/Question.java,v 1.3 2007/03/08 00:13:03 jgerdes Exp $
 * Question.java
 *  This is the question object of the registration quiz. This is used to hold the questions
 *  after they are pulled from the database and also used to grade them at the end of the 
 *  quiz. The Display function allows the question to be adequately displayed when needed.
 *
 *  Required Objects:
 *      None
 *
 *  Required Functions:
 *      None
 *  
 *  Supplied Functions:
 *      Display() -- Displays the question
 *      Grade()  -- Used to determine if the Answer is correct.
 *
 * Created on November 21, 2006 by Josh Turner
 * Last Modified on February 26th, 2006 by Josh Turner
 *
 */

import java.util.*;
import java.text.*;

public class Question {

    public static final long serialVersionUID = 110;
    /*
     * 1.0.0 - Initial Release
     * 1.1.0 - Reworked to use the new normalized database.
     */
    
    public String strQuestion;
    public int intID;
    public String strAnswers[];
    private int intCorrectAnswer;
    public String strHint;
       
    /** Creates a new instance of Question */
    public Question(int newID, String newQuestion, String newAnswers[], String newHint, int newCorrectAnswer) {
        intID = newID;
        strQuestion = newQuestion;
        strAnswers = newAnswers;
        strHint = newHint;
        intCorrectAnswer = newCorrectAnswer;
    }

    public String Display() {
      int intAnswerCount = 0;   
      /* prints the question and a set of radio buttons for the answers. */
        String tmpString = "";
        tmpString += "<br /> " +strQuestion +":";
        for (int i = 0; i < strAnswers.length;i++) {
              tmpString += "<br /><input type ='radio' name = '" + intID +"' value = '"+ intAnswerCount++ + "'>" + strAnswers[i];
        }
        return tmpString;
    }
    
    public boolean Grade(int testAnswer) {
        //Simple grade function
        if (testAnswer == intCorrectAnswer) {
            return true;
        }
        else {
            return false;
        }
    }
   
}
