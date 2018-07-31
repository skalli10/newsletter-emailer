//---------------------------------------------------------------------------------
// MODIFICATION LOG
//---------------------------------------------------------------------------------
//     Name               Date       Version              Description
//---------------------------------------------------------------------------------
//     Sajni        06/30/2005           1.0              Initial
//---------------------------------------------------------------------------------

import oracle.jdbc.driver.*;

import java.io.*;

import java.lang.*;

import java.security.*;

import java.sql.*;

import java.text.SimpleDateFormat;
import java.text.NumberFormat ;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Date;
import java.util.Properties;
import java.util.StringTokenizer;

import javax.activation.*;

import javax.crypto.*;
import javax.crypto.Cipher;

import javax.mail.MessagingException;
import javax.mail.internet.*;

import javax.swing.*;


class NewsletterList
{
    static final String PROPERTY_FILE = "config.txt";
    static Connection conn;
    static Date startTime = null;
    static Date endTime = null;
    static Properties configParam = new Properties();
    static String logContents = "";

    // **************************** PROCEDURE: main ********************************
    public static void main(String[] args)
    {
        try
        {
            startTime = new Date();
            readConfiguration(PROPERTY_FILE);
            updateLogFile(" NEWSLETTER LIST PREPARATION ", false);
            updateLogFile("START: " + startTime.toString(), true);
            checkFileAge();
            connectToDB();
            loadData();
            conn.close();
            updateIndicatorFile();
            endTime = new Date();
            updateLogFile("END: " + endTime.toString(), true);
            updateLogFile("TOTAL TIME: " +
                calcHMS(endTime.getTime() - startTime.getTime()) + " ---- [" +
                Long.toString(endTime.getTime() - startTime.getTime()) +
                " milliseconds" + "]", true);
            sendMail("");
            System.exit(0);
        }
        catch (Exception ex)
        {
            JOptionPane.showMessageDialog(new JFrame(),
                "Exception: " + ex.getMessage(), "Error",
                JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }
    }
     //main

    //*******************************************************************************
    private static int computeDaysBetween(Date fromDate, Date toDate)
    {
        try
        {
            // swap
            if (fromDate.after(toDate))
            {
                Date tmp = toDate;
                toDate = fromDate;
                fromDate = tmp;
            }

            Calendar from = Calendar.getInstance();
            from.setTime(fromDate);

            Calendar to = Calendar.getInstance();
            to.setTime(toDate);

            int numDays = 0;

            while (from.get(Calendar.YEAR) < to.get(Calendar.YEAR))
            {
                numDays += from.getActualMaximum(Calendar.DAY_OF_YEAR);
                from.add(Calendar.YEAR, 1);
            }

            numDays += (to.get(Calendar.DAY_OF_YEAR) -
            from.get(Calendar.DAY_OF_YEAR));

            return numDays;
        }
        catch (Exception ex)
        {
            System.err.println("Exception in connectToDB: " + ex.getMessage());
            sendMail("Exception in connectToDB: " + ex.getMessage());
            updateLogFile("Program Failed", true);
            System.exit(1);

            return(-1);
        }
    }

    //*******************************************************************************
    private static String calcHMS(long timeInMilliSeconds)
    {
        try
        {
            long hours;
            long minutes;
            long seconds;

            seconds = timeInMilliSeconds / 1000 ;
            minutes = seconds / 60 ;
            hours = minutes / 24 ;

            seconds = seconds % 60 ;
            minutes = minutes % 24 ;

            return (hours + " hour(s) " + minutes + " minute(s) " + seconds +
            " second(s)");
        }
        catch (Exception ex)
        {
            System.err.println("Exception in calcHMS: " + ex.getMessage());
            sendMail("Exception in calcHMS: " + ex.getMessage());
            updateLogFile("Program Failed", true);
            System.exit(1);

            return "";
        }
    }

    //*******************************************************************************
    private static void readConfiguration(String propFile)
    {
        try
        {
            configParam.load(new FileInputStream(propFile));
        }
        catch (Exception ex)
        {
            System.err.println("Exception in readConfiguration: " +
                ex.getMessage());
            sendMail("Exception in readConfiguration: " + ex.getMessage());
            updateLogFile("Program Failed", true);
            System.exit(1);
        }
    }

    //*******************************************************************************
    private static void connectToDB()
    {
        String Url;

        try
        {
            String tempKey = configParam.getProperty("KEY");
            String tempPWD = configParam.getProperty("DATABASE_PW");
            String dbUser = configParam.getProperty("DATABASE_USER") ;
            String dbName = configParam.getProperty("DATABASE_NAME") ;

            Key key = PasswordUtil.decodeKey(tempKey);
            String pwd = PasswordUtil.decrypt(tempPWD, key);

            DriverManager.registerDriver(new oracle.jdbc.driver.OracleDriver());
            System.out.println("Driver Registered");

            Url = "jdbc:oracle:oci8:" + dbUser + "/" + pwd + "@" + dbName ;

            conn = DriverManager.getConnection(Url);
            //System.out.println("Connected to " + dbUser + "@" + dbName );
        }
        catch (Exception ex)
        {
            System.err.println("Exception in connectToDB: " + ex.getMessage());
            sendMail("Exception in connectToDB: " + ex.getMessage());
            updateLogFile("Program Failed", true);
            System.exit(1);
        }
    }

    //*******************************************************************************
    private static void checkFileAge()
    {
        try
        {
            // Exit if file younger than 2 days
            File f1 = new File(configParam.getProperty("SIMAPHORE_FILE"));

            if (f1.exists())
            {
                Date date = new Date();

                long last_mod_timestamp = f1.lastModified();
                Date last_modified = new Date(last_mod_timestamp);
                int numDays = computeDaysBetween(last_modified, date);

                if (numDays <= 2)
                {
                    System.out.println("It has been " + numDays +
                        " days since file was last modified. Exiting...");
                    System.exit(0);
                }
            }
        }
        catch (Exception ex)
        {
            System.err.println("Exception in checkFileAge: " + ex.getMessage());
            sendMail("Exception in checkFileAge: " + ex.getMessage());
            updateLogFile("Program Failed", true);
            System.exit(1);
        }
    }

    //*******************************************************************************
    private static void checkDate(Date d)
    {
        try
        {
            Date today = new Date();
            Date loadDate = d;

            int numDays = computeDaysBetween(loadDate, today);

            if (numDays > 2)
            {
                System.out.println(
                    "Data was uploaded before 2 days, no fresh data. Exiting...");
                updateLogFile("Data was uploaded before 2 days, no fresh data. Exiting...",
                    true);
                sendMail("");
                System.exit(0);
            }
        }
        catch (Exception ex)
        {
            System.err.println("Exception in checkDate: " + ex.getMessage());
            sendMail("Exception in checkDate: " + ex.getMessage());
            updateLogFile("Program Failed", true);
            System.exit(1);
        }
    }

    //*******************************************************************************
    private static void loadData()
    {
        int length = 0;
        int numLines = 0;

        // FIRST IDENTIFY DISTINCT DIRECTORIES AND FILES THAT NEED TO BE CREATED
        String sql1 = " select distinct channel_cd, file_name "
                    + " from email_target_member "
                    + " where channel_cd in ('KBT','ETY','TWN')";

        System.out.println(sql1) ;

        try
        {
            // By default be type TYPE_FORWARD_ONLY and
            // have a concurrency level of CONCUR_READ_ONLY
            Statement stmt1 = conn.createStatement();
            ResultSet cursor1 = stmt1.executeQuery(sql1);

            String spaces = "                    " ;
            NumberFormat nf = NumberFormat.getNumberInstance() ;

            // CREATE FILE WRITER AND BUFFERED WRITER FOR THE LOG FILE
            //FileWriter fw1 = new FileWriter(configParam.getProperty("LOG_FILE"), true);
            //BufferedWriter bw1 = new BufferedWriter (fw1);
            //bw1.write("START: " + new Date().toString());
            //bw1.newLine();
            updateLogFile(
                "  CHANNEL "
                + "LIST                 "
                + "                LINES"
                + "                BYTES ", true);

            updateLogFile(
                "  -------"
                + " --------------------"
                + " ---------------------"
                + " --------------------", true);

            // scp init
            String user = configParam.getProperty("REMOTE_USER") ;
            String host = configParam.getProperty("REMOTE_HOST_NAME") ;
            String tempPW = configParam.getProperty("REMOTE_PW") ;
            String tempKey = configParam.getProperty("KEY") ;
            Key key = PasswordUtil.decodeKey(tempKey);
            String pw = PasswordUtil.decrypt(tempPW, key);

            ScpTo scp = new ScpTo(host, user, pw) ;


            // local dir init
            GregorianCalendar now = new GregorianCalendar() ;
            String year = Integer.toString(now.get(Calendar.YEAR)) ;
            String month = Integer.toString(now.get(Calendar.MONTH)+1) ;
            String day = Integer.toString(now.get(Calendar.DAY_OF_MONTH)) ;

            if (month.length() == 1) month = "0" + month ;
            if (day.length() == 1) day = "0" + day ;

            String local_base_dir = year ;
            createDirectory(local_base_dir) ;

            local_base_dir = local_base_dir + "/" + month ;
            createDirectory(local_base_dir) ;

            local_base_dir = local_base_dir + "/" + day ;
            createDirectory(local_base_dir) ;


            //bw1.newLine();
            // LOOP THROUGH DISTINCT DIRECTORIES AND FILES
            while (cursor1.next())
            {
                // For maximum portability, result set columns within each row should be
                // read in left-to-right order, and each column should be read only once
                String directory = cursor1.getString(1);
                String fileName = cursor1.getString(2);

                String ldir = local_base_dir + "/" + directory ;
                createDirectory(ldir) ;

                numLines = createDataFile(ldir, directory, fileName);

                // copy the file
                String rdir = configParam.getProperty("REMOTE_BASE_DIR");

                if (directory.compareTo("ETY") == 0)
                {
                    rdir = rdir + "/" + configParam.getProperty("REMOTE_ET_DIR") ;
                }
                else if (directory.compareTo("KBT") == 0)
                {
                    rdir = rdir + "/" + configParam.getProperty("REMOTE_KB_DIR") ;
                }
                else if (directory.compareTo("TWN") == 0)
                {
                    rdir = rdir + "/" + configParam.getProperty("REMOTE_TW_DIR") ;
                }
                else
                {
                    rdir = null ;
                }

                String lfile = ldir + "/" + fileName + ".txt" ;
                String rfile = rdir + "/" + fileName + ".txt" ;

                int ret = scp.secure_copy (lfile, rfile) ;
                if (ret != 0)
                {
                    System.out.println("Error: in copying");
                }


                // Get the number of bytes in the file
                File file = new File(lfile);

                if (file.exists())
                {
                    length = (int)(file.length()) ;
                }
                else
                {
                    length = 0;
                }

                String str_lines = nf.format(numLines) ;
                String str_bytes = nf.format(length) ;

                updateLogFile(
                    "  "+directory + "     "
                    + fileName
                    + spaces.substring(fileName.length()) + " "
                    // + Integer.toString(numLines) + "\t"
                    + spaces.substring(str_lines.length()) + " "
                    + str_lines
                    // + Long.toString(length),
                    + spaces.substring(str_bytes.length()) + " "
                    + str_bytes
                    ,true);
            }
             //while

            cursor1.close();
        }
        catch (SQLException ex)
        {
            System.err.println("SQLException in loadData: " + ex.getMessage());
            sendMail("SQLException in loadData: " + ex.getMessage());
            System.exit(1);
        }
        catch (Exception e)
        {
            System.err.println("Exception in loadData: " + e);
            sendMail("Exception in loadData: " + e.getMessage());
            updateLogFile("Program Failed", true);
            System.exit(1);
        }
    }

    //*******************************************************************************
    private static void createDirectory (String ldir)
    {
        // System.out.println("mehod createDirectory:" + ldir);

        try
        {
            // IF DIRECTORY DOESNOT EXIST CREATE IT
            File dir = new File(ldir);

            if (!dir.exists())
            {
                boolean success = (new File(ldir).mkdir());

                if (!success)
                {
                    System.out.println("Creation of directory " +
                        ldir + "failed.");
                    System.exit(1);
                }
            }

        }
        catch (Exception ex)
        {
            System.err.println("Exception in updateLogFile: " +
                ex.getMessage());
            sendMail("Exception in createDirectory: " + ex.getMessage());
            System.exit(1);
        }
    }

    //*******************************************************************************
    private static void updateLogFile(String message, boolean flag)
    {
        System.out.println("Updating log file:" + message);

        try
        {
            // Create file log file if it doesn't exist
            File f1 = new File(configParam.getProperty("LOG_FILE"));

            if (!f1.exists())
            {
                boolean success = (f1.createNewFile());

                if (!success)
                {
                    System.out.println("Creation of the log file failed.");
                    System.exit(1);
                }
            }

            SimpleDateFormat bartDateFormat = new SimpleDateFormat(
                    "EEEE, dd MMMM yyyy 'at' hh:mm:ss aaa z");
            Date date = new Date();

            FileWriter fw = new FileWriter(configParam.getProperty("LOG_FILE"), flag);
            BufferedWriter bw = new BufferedWriter(fw);
            bw.write(message);
            bw.newLine();
            bw.close();
            fw.close();

            logContents = logContents + message + "\n";
        }
        catch (Exception ex)
        {
            System.err.println("Exception in updateLogFile: " +
                ex.getMessage());
            sendMail("Exception in updateLogFile: " + ex.getMessage());
            System.exit(1);
        }
    }

    //*******************************************************************************
    private static void updateIndicatorFile()
    {
        try
        {
            // Create indicator if it doesn't exist
            File f1 = new File(configParam.getProperty("SIMAPHORE_FILE"));

            if (!f1.exists())
            {
                boolean success = (f1.createNewFile());

                if (!success)
                {
                    System.out.println("Creation of file indicator.txt failed.");
                    updateLogFile("Creation of file indicator.txt failed.", true);
                    System.exit(1);
                }
            }

            SimpleDateFormat bartDateFormat = new SimpleDateFormat(
                    "EEEE, dd MMMM yyyy 'at' hh:mm:ss aaa z");
            Date date = new Date();

            FileWriter fw = new FileWriter(configParam.getProperty("SIMAPHORE_FILE"), true);
            BufferedWriter bw = new BufferedWriter(fw);
            bw.write("Last processed on " + bartDateFormat.format(date));
            System.out.println("Last processed on " +
                bartDateFormat.format(date));
            bw.newLine();
            bw.close();
            fw.close();
        }
        catch (Exception ex)
        {
            System.err.println("Exception in updateIndicatorFile: " +
                ex.getMessage());
            sendMail("Exception in updateIndicatorFile: " + ex.getMessage());
            updateLogFile("Program Failed", true);
            System.exit(1);
        }
    }

    //*******************************************************************************
    private static int createDataFile(String ldir, String directory, String fileName)
    {
        try
        {
            boolean flag = false;
            int numLines = 0;

            String coupon = null;

            // QUERY DATA THAT NEEDS TO BE SAVED IN CURRENT FILE
            String sql2 =
                  " select email_address, newsletter_subscription_id, coupon_cd "
                + " from email_target_member "
                + " where channel_cd = "
                + "'" + directory + "'"
                + " and file_name = "
                + "'" + fileName + "'"
                + " order by sort_seq_no ";

            System.out.println(sql2) ;


            Statement stmt2 = conn.createStatement();
            ResultSet cursor2 = stmt2.executeQuery(sql2);
            cursor2.setFetchSize(100);

            // CREATE FILE WRITER AND BUFFERED WRITER FOR DATA FILES
            FileWriter fw2 = new FileWriter(ldir + "/" + fileName + ".txt");
            BufferedWriter bw2 = new BufferedWriter(fw2);

            while (cursor2.next())
            {
                // CHECK IF DATA WAS LOADED BEFORE 2 DAYS. IF SO, SET FLAG TO TRUE
                if (!flag)
                {
                    // checkDate(cursor2.getDate("DW_LOAD_DT"));
                }

                coupon = cursor2.getString(3) ;
                if (coupon == null) coupon = "" ;

                bw2.write(cursor2.getString(1)
                    + "|" + cursor2.getString(2)
                    + "|" + coupon + "\n");

                numLines = numLines + 1;
            }
             // while

            cursor2.close();

            bw2.close();
            fw2.close();

            return numLines;
        }
        catch (Exception ex)
        {
            System.err.println("Exception in createDataFile: " +
                ex.getMessage());
            sendMail("Exception in createDataFile: " + ex.getMessage());
            updateLogFile("Program Failed", true);
            System.exit(1);

            return(-1);
        }
    }
     // END createDataFile

    //*******************************************************************************
    static private String[] parseAddress(String addressString, String delim)
    {
        try
        {
            StringTokenizer st = new StringTokenizer(addressString, delim);
            int tokenCount = st.countTokens();
            String[] addrs = new String[tokenCount];

            for (int x = 0; x < tokenCount; x++)
            {
                addrs[x] = st.nextToken();
            }

            return addrs;
        }
        catch (Exception ex)
        {
            System.err.println("Exception in parseAddress: " + ex.getMessage());
            sendMail("Exception in parseAddress: " + ex.getMessage());
            updateLogFile("Program Failed", true);
            System.exit(1);

            return null;
        }
    }
     //END parseAddress

    //*******************************************************************************
    static private void sendMail(String message)
    {
        javaMailer jm = null;
        MimeBodyPart emailText = null;
        MimeBodyPart emailAttachment = null;
        MimeBodyPart logFile = null;
        BufferedWriter outFile = null;

        try
        {
            // System.out.println("Starting send mail");

            String[] recipients = parseAddress(configParam.getProperty(
                        "RECIPIENTS"), ",");
            InternetAddress[] addrs = new InternetAddress[recipients.length];

            //Create InternetAddress objects for each clean mail_to address
            try
            {
                for (int x = 0; x < recipients.length; x++)
                {
                    recipients[x] = recipients[x].trim();
                    addrs[x] = new InternetAddress(recipients[x]);
                }
            }
            catch (AddressException e)
            {
                System.err.println(e.getMessage());
            }

            //System.out.println("Internet address set");

            //Create the mailer
            jm = new javaMailer(configParam);
            jm.setRecipients(addrs);
            jm.setSubject(configParam.getProperty("SUBJECT"));
            //System.out.println("Java Mailer instance created");


            //Create email body
            emailText = new MimeBodyPart();
            emailText.setText(logContents);
            jm.addBodyPart(emailText);

            //create the attachment and add the file to it
            File f2 = new File(configParam.getProperty("LOG_FILE"));
            if (f2.exists())
            {
                logFile = new MimeBodyPart();

                //create a file data source for the attachment
                FileDataSource fds2 = new FileDataSource(configParam.getProperty(
                            "LOG_FILE"));

                //add the file data source to the body part
                logFile.setDataHandler(new DataHandler(fds2));
                logFile.setFileName(configParam.getProperty("LOG_FILE"));
                jm.addBodyPart(logFile);
            }


            // System.out.println("Message body created, files attached.");

            if (jm.sendMessage())
            {
                System.out.println("Mail sent successfully.");
            }
            else
            {
                System.err.println("Unable to send email. jm.sendMessage() failed!");
                System.exit(1);
            }
        }
        catch (MessagingException me)
        {
            System.err.println("MSGException in sendMail: " + me.getMessage());
            updateLogFile("Program Failed", true);
            System.exit(1);
        }
         //end try block
    }
     //END sendMail()
}
 //class

