import oracle.jdbc.driver.*;

import java.io.*;

import java.lang.*;

import java.security.*;

import java.sql.*;

import java.text.SimpleDateFormat;

import java.util.Calendar;
import java.util.Date;
import java.util.Properties;
import java.util.StringTokenizer;

import javax.activation.*;

import javax.crypto.*;
import javax.crypto.Cipher;

import javax.mail.MessagingException;
import javax.mail.internet.*;

import javax.swing.*;


class GenerateKey
{
    public static void main(String[] args)
    {
        try
        {


            // key
            String base64_key = "Xu9uhq4ZkWQ=" ;

			String sshDirPath = System.getProperty("user.home");
			System.out.println("sshDirPath=[" + sshDirPath + "]");

            //  open up standard input
            BufferedReader br = new BufferedReader(new InputStreamReader(System.in));

            System.out.print("Enter Clear Text: " ) ;
            String clear_text = br.readLine();



            Key key = PasswordUtil.decodeKey(base64_key);

            String base64_text = PasswordUtil.encrypt(clear_text, key) ;

            System.out.println("base64encoded(encrypted(clear_test))=[" + base64_text + "]");

            System.out.println("clear_text=[" + PasswordUtil.decrypt(base64_text, key) + "]");

            // Generate a new Key
            //{
            //    System.out.println(PasswordUtil.generateRandomKey());
            //    Key key = PasswordUtil.generateRandomKey();
            //    System.out.println("base64_key=[" + PasswordUtil.encodeKey(key) + "]");
		    //}



        }
        catch (Exception e)
        {
            throw new RuntimeException("Failed ", e);
        }
    }
}

