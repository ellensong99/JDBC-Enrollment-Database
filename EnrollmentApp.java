import java.sql.*;
import java.util.Scanner;
import java.util.*;

// sample input: localhost 5432 database_name username password
public class EnrollmentApp {
    public static final String CONFLICTING_COURSE = "[Warning] Conflicting classes";
    public static final String INVALID_ID = "[Error] Invalid ID";
    public static final String INVALID_CLASS = "[Error] Invalid class";
    public static final String INVALID_ENROLLMENT = "[Error] Invalid enrollment";
    public static final String NO_ENROLLMENT = "[Warning] No enrollment";

    private Scanner input = new Scanner(System.in);
    private Connection connection = null;

    public EnrollmentApp(String[] args) {

        // loading the DBMS driver
        try {
            Class.forName("com.ibm.db2.jcc.DB2Driver");
        } catch (ClassNotFoundException e) {
            System.out.println("Missing DBMS driver.");
            e.printStackTrace();
        }

        try {
            // connecting to the a database
            connection = DriverManager.getConnection("jdbc:db2:CS348");
            System.out.println("Database connection open.\n");

            // setting auto commit to false
            connection.setAutoCommit(false);
        } catch (SQLException e) {
            System.out.println("DBMS connection failed.");
            e.printStackTrace();
        }
    }

    public static void main(String[] args) throws Exception {
        EnrollmentApp menu = new EnrollmentApp(args);
        menu.mainMenu(args);
        menu.exit();
    }

    public void exit() {

        try {
            // close database connection
            connection.commit();
            connection.close();
            System.out.println("Database connection closed.");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void mainMenu(String[] args) throws SQLException {

        mainMenu: while (true) {

            System.out.println("\n-- Actions --");

            System.out.println("Select an option: \n" + 
                "  1) Get your classes\n" + 
                "  2) Search your classmates\n" + 
                "  3) Major statistics of your class\n" + 
                "  0) Exit\n ");
            int selection = input.nextInt();
            input.nextLine();

            switch (selection) {
                case 1:
                    System.out.print("Please provide the user ID: ");
                    int userID = input.nextInt();
                    input.nextLine();
                    this.getClassByStudentID(userID);
                    break;
                case 2:
                    System.out.print("Please provide the user ID: ");
                    int id = input.nextInt();
                    input.nextLine();
                    System.out.print("Please provide the list of class names: ");
                    String classes = input.nextLine();

                    this.searchCommonClassmate(id, classes);
                    break;
                case 3:
                    System.out.print("Please provide the class name: ");
                    String myclass = input.nextLine();

                    this.getClassStatis(myclass);
                    break;
                case 0:
                    System.out.println("Returning...\n");
                    break mainMenu;
                default:
                    System.out.println("Invalid action.");
                    break;
            }
        }
    }

    /**
     * (a)
     * <p>
     * Print to std a list of classes that a student has been enrolled in with an ID
     * number supplied as an integer argument to the command line. Print one class
     * per row and the section number the given student is enrolled in next to each
     * class (separated by tab '\t'). Sort the output on class name. Write your code
     * inside the getClassByStudentID function.
     *
     * @param userID id number of the student
     * @throws SQLException
     */
// 301221823 112348546
// Unenrolled: 550156548.
    private void getClassByStudentID(int userID) throws SQLException {
        String invalidID = "select snum from student where snum = " + Integer.toString(userID);
        String invalidEnroll = "select cname, section from enrolled where snum = " + Integer.toString(userID) + " order by cname";
        String warnings = "with output as (select a.cname as cname, a.section as sec, b.meets_at as time from (select cname, section from enrolled where snum = " + Integer.toString(userID) + ") a " + 
                          "left join class b on a.cname = b.name and a.section = b.section order by time, cname) " + 
                          "select cname, time from output where time in (select time from output group by time having count(*) > 1) order by time, cname";
        try (Statement stmt = connection.createStatement()) {
            ResultSet rsID = stmt.executeQuery(invalidID);
            if (rsID.next() == false) {
                System.out.println(INVALID_ID);
            } else {
                ResultSet rsE = stmt.executeQuery(invalidEnroll);
                if (rsE.next()) {
                    System.out.println(rsE.getString("cname") + "\t" + rsE.getString("section"));
                    while (rsE.next()) {
                        System.out.println(rsE.getString("cname") + "\t" + rsE.getString("section"));
                    }
                    ResultSet rsW = stmt.executeQuery(warnings);
                    if (rsW.next()) {
                        System.out.println(CONFLICTING_COURSE);
                        System.out.println(rsW.getString("cname") + "\t" + rsW.getString("time"));
                        while (rsW.next()) {
                            System.out.println(rsW.getString("cname") + "\t" + rsW.getString("time"));
                        }
                    }
                } else {
                    System.out.println(NO_ENROLLMENT);
                } 
            }
        } catch (SQLException e) {
            System.out.println("SQL Exception");
        }
    }

    /**
     * (b)
     * <p>
     * Given a student ID number and a list of class names separated with comma as
     * arguments on the command line, print to std in alphabet order the list of
     * student names who have been enrolled in all the given classes as the given
     * student has.
     *
     * @param userID  id number of the student
     * @param classes a list of class names separated with comma
     * @throws SQLException
     */
    //112348546
    //Database Systems,Operating System Design
    private void searchCommonClassmate(int userID, String classes) throws SQLException {
        String [] lst = classes.split("[,]", 0);
        List<String> classList = Arrays.asList(lst);
        String invalidID = "select sname from student where snum = " + Integer.toString(userID);
        String validClass = "select distinct name from class";
        try (Statement stmt = connection.createStatement()) {
            ResultSet rsID = stmt.executeQuery(invalidID);
            if (rsID.next() == false) {
                System.out.println(INVALID_ID);
            } else if (!classes.isEmpty()) {
                ResultSet rsClass = stmt.executeQuery(validClass);
                List<String> validClasses = new ArrayList<String>();
                if (rsClass.next()) {
                    validClasses.add(rsClass.getString("name"));
                    while (rsClass.next()) {
                        validClasses.add(rsClass.getString("name"));
                    }
                }
                // verify input classList & compute
                List<String> nameList = new ArrayList<String>();
                for (String c: classList) {
                    if (!validClasses.contains(c)) {
                        System.out.println(INVALID_CLASS);
                        return;
                    } else {
                        String invalidEnroll = "select snum, section from enrolled where snum = " + Integer.toString(userID) + " and cname = \'" + c + "\'";
                        ResultSet rsEnroll = stmt.executeQuery(invalidEnroll);
                        if (rsEnroll.next() == false) {
                            System.out.println(INVALID_ENROLLMENT);
                            return;
                        } else {
                            int section = rsEnroll.getInt("section");
                            String curClass = "select distinct sname from (select distinct snum from enrolled where cname = \'" + c + "\' and section = " + Integer.toString(section) + ") a inner join student s on a.snum = s.snum";
                            ResultSet rsPeople = stmt.executeQuery(curClass);
                            List<String> tmpList = new ArrayList<String>();
                            if (rsPeople.next()) {
                                if (nameList.isEmpty()) {
                                    nameList.add(rsPeople.getString("sname"));
                                } else {
                                    tmpList.add(rsPeople.getString("sname"));
                                }
                                while (rsPeople.next()) {
                                    if (tmpList.isEmpty()) {
                                        nameList.add(rsPeople.getString("sname"));
                                    } else {
                                        tmpList.add(rsPeople.getString("sname"));
                                    }
                                }
                            }
                            if (!tmpList.isEmpty()) {
                                nameList.retainAll(tmpList);
                            }
                        }
                    }
                }
                Collections.sort(nameList);
                for (String name: nameList) {
                    System.out.println(name);
                }
            } else if (classes.isEmpty()) {
                String noEnroll = "select snum from enrolled where snum = " + Integer.toString(userID);
                ResultSet rsEnroll = stmt.executeQuery(noEnroll);
                if (rsEnroll.next() == false) {
                    System.out.println(NO_ENROLLMENT);
                    return;
                } else {
                    List<String> enrollClasses = new ArrayList<String>();
                    String allStudents = "select cname from enrolled where snum = " + Integer.toString(userID);
                    ResultSet allClasses = stmt.executeQuery(allStudents);
                    if (allClasses.next()) {
                        enrollClasses.add(allClasses.getString("cname"));
                        while (allClasses.next()) {
                            enrollClasses.add(allClasses.getString("cname"));
                        }
                    }
                    List<String> nameList = new ArrayList<String>();
                    for (String c: enrollClasses) {
                        String enroll = "select snum, section from enrolled where snum = " + Integer.toString(userID) + " and cname = \'" + c + "\'";
                        ResultSet rsE = stmt.executeQuery(enroll);
                        rsE.next();
                        int section = rsE.getInt("section");
                        String curClass = "select distinct sname from (select distinct snum from enrolled where cname = \'" + c + "\' and section = " + Integer.toString(section) + ") a inner join student s on a.snum = s.snum";
                        ResultSet rsPeople = stmt.executeQuery(curClass);
                        List<String> tmpList = new ArrayList<String>();
                        if (rsPeople.next()) {
                            if (nameList.isEmpty()) {
                                nameList.add(rsPeople.getString("sname"));
                            } else {
                                tmpList.add(rsPeople.getString("sname"));
                            }
                            while (rsPeople.next()) {
                                if (tmpList.isEmpty()) {
                                    nameList.add(rsPeople.getString("sname"));
                                } else {
                                    tmpList.add(rsPeople.getString("sname"));
                                }
                            }
                        }
                        if (!tmpList.isEmpty()) {
                            nameList.retainAll(tmpList);
                        }
                    }
                    Collections.sort(nameList);
                    for (String name: nameList) {
                        System.out.println(name);
                    }
                }
            }
        } catch (SQLException e) {
            System.out.println("SQL Exception");
        }

    }

    /**
     * (c)
     * <p>
     * Given a course name as a string on the command line, print to std the majors
     * of enrolled students and the count of students from each major, respectively.
     * Result should be sorted by major name. One major per row.
     *
     * @param className name of the class
     * @throws SQLException
     */
    private void getClassStatis(String className) throws SQLException {
        String validClass = "select * from class where name = \'" + className + "\'";
        try (Statement stmt = connection.createStatement()) {
            ResultSet rsC = stmt.executeQuery(validClass);
            //System.out.println("here");
            if (rsC.next() == false) {
                System.out.println(INVALID_CLASS);
            } else {
                String res = "with result as (select s.snum as num, COALESCE(s.major, 'TBD') as major from (select * from enrolled where cname = \'" + className +
                "\') a left join student s on a.snum = s.snum) select major, count(*) as cnt from result group by major order by major";
                ResultSet rsM = stmt.executeQuery(res);
                if (rsM.next()) {
                    System.out.println(rsM.getString("major") + "\t" + rsM.getString("cnt"));
                    while (rsM.next()) {
                        System.out.println(rsM.getString("major") + "\t" + rsM.getString("cnt"));
                    }
                }
            }
        } catch (SQLException e) {
            System.out.println("SQL Exception");
        }
    }
}
