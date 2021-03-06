/* -*-mode:java; c-basic-offset:2; -*- */
// http://www.jcraft.com/jsch/
import com.jcraft.jsch.*;

import java.io.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

public class ScpFrom{
  public static void main(String[] arg){
    if(arg.length!=2){
      System.err.println("usage: java ScpFrom user@remotehost:file1 file2");
      System.exit(-1);
    }

    try{

      String user=arg[0].substring(0, arg[0].indexOf('@'));
      arg[0]=arg[0].substring(arg[0].indexOf('@')+1);
      String host=arg[0].substring(0, arg[0].indexOf(':'));
      String rfile=arg[0].substring(arg[0].indexOf(':')+1);
      String lfile=arg[1];

      String prefix=null;
      if(new File(lfile).isDirectory()){
        prefix=lfile+File.separator;
      }

      JSch jsch=new JSch();
      Session session=jsch.getSession(user, host, 22);

      // username and password will be given via UserInfo interface.
      UserInfo ui=new MyUserInfo();
      session.setUserInfo(ui);
      session.connect();

      // exec 'scp -f rfile' remotely
      String command="scp -f "+rfile;
      Channel channel=session.openChannel("exec");
      ((ChannelExec)channel).setCommand(command);

      // get I/O streams for remote scp
      OutputStream out=channel.getOutputStream();
      InputStream in=channel.getInputStream();

      channel.connect();

      byte[] buf=new byte[1024];

      // send '\0'
      buf[0]=0; out.write(buf, 0, 1); out.flush();

      while(true){
	int c=checkAck(in);
        if(c!='C'){
	  break;
	}

        // read '0644 '
        in.read(buf, 0, 5);

        int filesize=0;
        while(true){
          in.read(buf, 0, 1);
          if(buf[0]==' ')break;
          filesize=filesize*10+(buf[0]-'0');
        }

        String file=null;
        for(int i=0;;i++){
          in.read(buf, i, 1);
          if(buf[i]==(byte)0x0a){
            file=new String(buf, 0, i);
            break;
  	  }
        }

	//System.out.println("filesize="+filesize+", file="+file);

        // send '\0'
        buf[0]=0; out.write(buf, 0, 1); out.flush();

        // read a content of lfile
        FileOutputStream fos=new FileOutputStream(prefix==null ?
						  lfile :
						  prefix+file);
        int foo;
        while(true){
          if(buf.length<filesize) foo=buf.length;
	  else foo=filesize;
          in.read(buf, 0, foo);
          fos.write(buf, 0, foo);
          filesize-=foo;
          if(filesize==0) break;
        }
        fos.close();

        byte[] tmp=new byte[1];

	if(checkAck(in)!=0){
	  System.exit(0);
	}

        // send '\0'
        buf[0]=0; out.write(buf, 0, 1); out.flush();
      }
      System.exit(0);
    }
    catch(Exception e){
      System.out.println(e);
    }
  }

  static int checkAck(InputStream in) throws IOException{
    int b=in.read();
    // b may be 0 for success,
    //          1 for error,
    //          2 for fatal error,
    //          -1
    if(b==0) return b;
    if(b==-1) return b;

    if(b==1 || b==2){
      StringBuffer sb=new StringBuffer();
      int c;
      do {
	c=in.read();
	sb.append((char)c);
      }
      while(c!='\n');
      if(b==1){ // error
	System.out.print(sb.toString());
      }
      if(b==2){ // fatal error
	System.out.print(sb.toString());
      }
    }
    return b;
  }

  public static class MyUserInfo implements UserInfo{
    public String getPassword(){ return passwd; }
    public boolean promptYesNo(String str){
      Object[] options={ "yes", "no" };
      int foo=JOptionPane.showOptionDialog(null,
             str,
             "Warning",
             JOptionPane.DEFAULT_OPTION,
             JOptionPane.WARNING_MESSAGE,
             null, options, options[0]);
       return foo==0;
    }

    String passwd;
    JTextField passwordField=(JTextField)new JPasswordField(20);

    public String getPassphrase(){ return null; }
    public boolean promptPassphrase(String message){ return true; }
    public boolean promptPassword(String message){
      Object[] ob={passwordField};
      int result=
	  JOptionPane.showConfirmDialog(null, ob, message,
					JOptionPane.OK_CANCEL_OPTION);
      if(result==JOptionPane.OK_OPTION){
	passwd=passwordField.getText();
	return true;
      }
      else{ return false; }
    }
    public void showMessage(String message){
      JOptionPane.showMessageDialog(null, message);
    }
  }

}

