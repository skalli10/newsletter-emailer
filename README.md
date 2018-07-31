newsletter-emailer
==================

This project prepares newsletters and emails them.

 

Design
======

 

Input Data: email_target_member

create table email_target_member

(

newsletter_subscription_id number(10),

email_address varchar2(100),

first_name varchar2(30),

last_name varchar2(30),

sort_seq_no number(10),

channel_cd varchar2(10),

file_name varchar2(30),

dw_load_dt date

)

loop on (select distinct channel_cd, file_name from email_target_member) 

Create a folder with channel_cd value.  
        Select rows from email_target_member with channel_cd, file_name and
write to a file with \| as delimitter 

        SCP copy the files to the machine dedicated to send emails. It reads the
files and sends out emails.

        send error notification email to operations team when there are
exceptions.

end loop 

Output Files: CSV files with following fields

email_address, newsletter_subscription_id, coupon_cd

 

These files are copied to the dedicated email server which sends out emails and
tracks bounced emails.

 

Unit Test
=========

The file test_data.sql contains test data.

 

 

Running
=======

rm indicator.txt

javac NewsletterList.java

java NewsletterList

 

Utilities
=========

The utility GenerateKey is used to encrypt a password that can be supplied in
the config file.

 

Libraries Used
==============

\----------------------------

from sun: jaf-1_0_2-upd2.zip contains

activation.jar

\----------------------------

from sun: javamail-1_3_3-ea.zip contains:

mail.jar

imap.jar

mailapi.jar

pop3.jar

smtp.jar

\------------------------------

http://www.jcraft.com/jsch/

jsch-0.1.20.jar libraries

jsch-0.1.20.zip examples and source code

 

download from sourceforge.net/projects/jsch/

\----------------------------

 

 

 

 
