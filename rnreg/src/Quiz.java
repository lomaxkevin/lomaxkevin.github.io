/*
 * $CwuRes: src/regi/www/webapps/rnreg/src/Quiz.java,v 1.6 2018/10/10 19:00:34 alvisoj Exp $
 * Quiz.java
 *  This is the quiz object of the registration quiz. The category names and counts can be modified via the arrays near
 *  the top of the program. When populateQuiz() is called the object connects to the database and processes questions
 *  in a random order to fill the specified categories. After it is populated the Display() function can be called to
 *  display the quiz for the user.
 *
 * Depends on the following objects:
 *      AllQuestions -- Object that can hold all the questions after the quiz has been taken
 *      Category -- The category object allows questions to be sorted and each hold their given number of question objects      
 *      Database -- The database connection to Oracle, used to run queries.
 *      Question -- This holds the question objects themselves.
 *
 *  Supplied Functions:
 *      getCategories() -- Returns a list of categories
 *      populateQuiz() -- This function is what fills the quiz object with a new set of questions
 *      Display() -- Displays the quiz so the user can take it  
 *
 * Created on November 21, 2006 By Josh Turner
 * Last Modified on February 26th, 2006 by Josh Turner
 */
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;

public class Quiz {

    public static final long serialVersionUID = 110;
    /*
     * 1.0.0 - Initial Release
     * 1.1.0 - Reworked to use the new normalized database
     */
    
    private Category ctgCategories[];
    public int MinScore = 0;
    private int QUIZ_TYPE = 0;
    private Database dbDatabase = new Database();
    String strErrors = "";
    private int intQuestionTotal = 0;
    
    
    /* Creates a new instance of Quiz */
    public Quiz() {
        try {
            int tmpID, tmpCount;
            String tmpName;
		// Pulls the Quiz Type from the database
		QUIZ_TYPE = dbDatabase.SelectQuery("select id from quiz_type where name='registration'").getInt(1);
            // Pulls the MinScore from the database
            MinScore = dbDatabase.SelectQuery("select max(passing_score) from resnet.quiz_type where id = "+QUIZ_TYPE).getInt(1);
            //Pulls the count of how many categories there are
            ctgCategories = new Category[dbDatabase.SelectQuery("select count(id) from resnet.quiz_categories where type_id = "+QUIZ_TYPE).getInt(1)];
            //Pulls the category information
            ResultSet tmpResults = dbDatabase.SelectQuery("select id, name, number_questions from resnet.quiz_categories where type_id = "+QUIZ_TYPE);
            for (int i=0;i<ctgCategories.length;i++) {
                //Grabs the 3 variables
                tmpID = tmpResults.getInt(1);
                tmpName = tmpResults.getString(2);
                tmpCount = tmpResults.getInt(3);
                
                //Makes the object
                ctgCategories[i] = new Category(tmpID, tmpName, tmpCount);
                
                //Adds the count to the total
                intQuestionTotal += tmpCount;
                //Moves to the next one
                if(!(tmpResults.isLast())) {
                    tmpResults.next();                    
                }
            }       
        }
        //Error Handling
        catch (SQLException ex) {
                while (ex != null) {  
                strErrors += ("\nSQL Exception:  " + ex.getMessage ());
                ex = ex.getNextException ();  
                } 
                System.out.println(strErrors);
        }  
        catch (java.lang.Exception ex) {
                System.out.println("Exception:  " + ex.getMessage ());
        }
    }
    
    // populates the quiz
    public void populateQuiz() {
        /*
         * This query is rather confusing because it uses a couple of functions.
         * select 
         * resnet.quiz_questions.id, -- Question ID, this will be a number that was automatically assigned to the question
         * resnet.quiz_get_categories(id), -- Categories, this will return the category IDs seperated by two carats(^^) i.e. 1^^2^^3
         * resnet.quiz_questions.question, -- Question, this is the actual content of the question
         * resnet.quiz_get_answers(id), -- Answers, this will also be answer IDs seperated by two carats(^^)
         * resnet.quiz_questions.hint -- Hint, this is a string that goes on the last page if they got it wrong
         * from resnet.quiz_questions -- the table that stores the information
         */
        ResultSet results = dbDatabase.SelectQuery("select resnet.quiz_questions.id, resnet.quiz_get_categories(id), resnet.quiz_questions.question, resnet.quiz_get_answers(id), resnet.quiz_questions.hint from resnet.quiz_questions order by dbms_random.value");
        try {   
            
            // Sets constants to make this simpler 
            int ID = 1;
            int CATEGORY = 2;
            int QUESTION = 3;
            int ANSWER = 4;
            int HINT = 5;
            int CORRECTANSWER = 6;
            String tmpString = "";
            System.out.println("ResultSet object results points to: " + results);
            // some empty variables
            int currentID;
            String currentCategories[];
            String currentQuestion = "";
            String currentAnswers[];
            String currentHint = "";
            int currentCorrectAnswer;
            
           do { 
                // Sets the variables for the current question 
                try {
                    //System.out.println("This error is pre-parseInt in nested try block");
                    currentID = Integer.parseInt(results.getString(ID));
                    //System.out.println("This error is post-parseInt in nested try block");
                    currentCategories = results.getString(CATEGORY).split("\\^\\^"); //splits the categories into an array
                    currentQuestion = results.getString(QUESTION);
                    currentAnswers = results.getString(ANSWER).split("\\^\\^"); //splits the answers into an array
                    currentHint = results.getString(HINT);
                    currentCorrectAnswer = -1; //irrelevant for this part of the quiz

                    // Cycles through the current question's categories
                    for (int i = 0; i < currentCategories.length; i++) {
                        // Cycles through the categories in the quiz
                        for (int x = 0; x < ctgCategories.length; x++) { 
                            // Processes if the question is in the current category
                            if (ctgCategories[x].id == Integer.parseInt(currentCategories[i])) {
                                // getEmpty() will return either the number of an empty question or -1 if it doesnt find one
                                // it then creates the question and jumps out of the loop.
                                if (ctgCategories[x].getEmpty() >= 0) { 
                                    Question tmpQuestion = new Question(currentID, currentQuestion, currentAnswers, currentHint, currentCorrectAnswer);
                                    ctgCategories[x].Questions[ctgCategories[x].getEmpty()] = tmpQuestion;
                                    x = ctgCategories.length +1;
                                    i = currentCategories.length+1; // causes it not to loop again
                                }
                            }
                        }
                    }
                }
                catch (NullPointerException ex) {
                    //There was a problem with that question, move on to the next. This is to
                    // handle if a question is there without an answer or something along those lines
                    System.out.println("Populate quiz failed " + ex.getMessage());
                }
                results.next();
           }
            while (!(results.isLast()));
            
        }
        catch (SQLException ex) {
             System.out.println("\nSQL Exception:  " + ex.getMessage ());

	}  
        dbDatabase.Close(); //closes the connection
    }
    
    
    public String Display() {
        //Cycles through and displays all categories of questions;
        String tmpString = "";
            for (int i = 0; i < ctgCategories.length;i++) {
                tmpString += ctgCategories[i].Display();
            }
        return tmpString;
    }
}

