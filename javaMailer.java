
import java.io.*;

import java.util.*;
import java.util.Properties;

import javax.activation.*;

import javax.mail.*;
import javax.mail.MessagingException;
import javax.mail.internet.*;


public class javaMailer
{
    MimeMessage mm = null;
    Multipart mp = null;
    Properties props = null;
    Session sessn = null;

    public javaMailer(Properties p)
    {
        props = p;
        sessn = Session.getInstance(props);

        mm = new MimeMessage(sessn);
        mp = new MimeMultipart();
    }

    public void setHost(String h)
    {
        props.setProperty("mail.smtp.host", h);
    }

    public void setRecipients(InternetAddress[] a)
    {
        if (mm != null)
        {
            try
            {
                mm.setRecipients(Message.RecipientType.TO, a);
            }
            catch (MessagingException mex)
            {
                System.err.println("setRecipients: " + mex.getMessage());
            }
        }
    }

    public void setSubject(String s)
    {
        //subj = s;
        if (mm != null)
        {
            try
            {
                mm.setSubject(s);
            }
            catch (MessagingException mex)
            {
                System.err.println("setSubject: " + mex.getMessage());
            }
        }
    }

    public void addBodyPart(MimeBodyPart mbp)
    {
        try
        {
            mp.addBodyPart(mbp);
        }
        catch (MessagingException e)
        {
            System.err.println("addBodyPart: " + e.getMessage());
        }
    }

    public boolean sendMessage()
    {
        if ((props.getProperty("mail.smtp.host") == null) ||
                (props.getProperty("mail.smtp.host").length() == 0))
        {
            System.err.println("Property mail.smtp.host Is Not Set!");

            return false;
        }
        else
        {
            try
            {
                String sn = props.getProperty("SENDER_NAME") ;
                String sa = props.getProperty("SENDER_ADDRESS") ;
                InternetAddress ia = new InternetAddress(sa, sn) ;
                mm.setFrom(ia);
                mm.setSentDate(new Date());
                mm.setContent(mp);
                Transport.send(mm);

                return true;
            }
            catch (Exception mex)
            {
                System.err.println("Exception in Java Mailer:  " +
                    mex.getMessage());

                return false;
            }
        }
    }
}
 //END CLASS javaMailer
//session.setDebug(debug);

