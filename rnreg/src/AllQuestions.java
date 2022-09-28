/*
 * $CwuRes: src/regi/www/webapps/rnreg/src/AllQuestions.java,v 1.3 2007/03/08 00:13:03 jgerdes Exp $
 * AllQuestions.java
 *  This object is used to pull the full table of questions into one object 
 *  location, as opposed to having them split by category. This is useful for
 *  grading because it allows the questions to be referenced via their ID number
 *  which is also the index of the array.
 *
 *  Required Objects:
 *      Question
 *
 *  Required Functions:
 *      None
 *  
 *  Supplied Functions:
 *      None
 *
 * Created on December 5, 2006 By Josh Turner 
 * Last Modified on February 26th, 2006 by Josh Turner
 *
 */

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;

public class AllQuestions {
    public static final long serialVersionUID = 110;
    /*
     * 1.0.0 - Initial Release
     * 1.1.0 - Reworked to use the new normalized database.
     */
        
    private Database dbDatabase = new Database();
    public Question question[];
        
    /** Creates a new instance of AllQuestions */
    public AllQuestions() {
        /*
         * This query is rather confusing because it uses a couple of functions.
         * select 
         * resnet.quiz_questions.id, -- Question ID, this will be a number that was automatically assigned to the question
         * resnet.quiz_get_categories(id), -- Categories, this will return the category IDs seperated by two carats(^^) i.e. 1^^2^^3
         * resnet.quiz_questions.question, -- Question, this is the actual content of the question
         * resnet.quiz_get_answers(id), -- Answers, this will also be answer IDs seperated by two carats(^^)
         * resnet.quiz_questions.hint, -- Hint, this is a string that goes on the last page if they got it wrong
         * resnet.quiz_get_correct_answer(id) -- Answer Number, this involves a function that determines which answer in 
         *          number order is correct, for example if the second answer was correct this would return a 1 since it is base 0
         * from resnet.quiz_questions -- the table that stores the information
         */
        ResultSet results = dbDatabase.SelectQuery("select resnet.quiz_questions.id, resnet.quiz_get_categories(id), resnet.quiz_questions.question, resnet.quiz_get_answers(id), resnet.quiz_questions.hint, resnet.quiz_get_correct_answer(id) from resnet.quiz_questions");
        try {               
            // Sets constants to make this simpler 
            int ID = 1;
            int CATEGORY = 2;
            int QUESTION = 3;
            int ANSWER = 4;
            int HINT = 5;
            int CORRECTANSWER = 6;
            String tmpString = "";
            
            // some empty variables
            int currentID;
            String currentCategories[];
            String currentQuestion = "";
            String currentAnswers[];
            String currentHint = "";
            int currentCorrectAnswer;
            
            //Grabs the highest id number so it can make an appropriate array
            int highestID = dbDatabase.SelectQuery("select max(id) from resnet.quiz_questions").getInt(ID);
            question = new Question[highestID+1];
            
           do { 
                //Stores the information for each question object
                currentID = Integer.parseInt(results.getString(ID));
                currentQuestion = results.getString(QUESTION);
                currentAnswers = results.getString(ANSWER).split("\\^\\^");
                currentHint = results.getString(HINT);
                currentCorrectAnswer = results.getInt(CORRECTANSWER);                
                question[currentID] = new Question(currentID, currentQuestion, currentAnswers, currentHint, currentCorrectAnswer);
                results.next();
            }
            while ( !(results.isLast()));
        }
        catch (SQLException ex) {
             System.out.println("\nSQL Exception:  " + ex.getMessage ());
	}  
    }    
    
    public void Close() {
        //Closes the database connection
        dbDatabase.Close();
    }
}
