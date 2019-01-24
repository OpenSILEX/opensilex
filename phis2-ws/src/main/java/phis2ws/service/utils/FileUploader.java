//**********************************************************************************************
//                                       FileUploader.java 
// Author(s): Arnaud Charleroy 
// PHIS-SILEX
// Copyright © INRA 2016
// Creation date: May 2016
// Contact: arnaud.charleroy@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
// Last modification date:  Jan., 2019
// Subject: A class which permit to send a file to a distant server
//***********************************************************************************************
package phis2ws.service.utils;

import java.io.File;
 
import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpException;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import phis2ws.service.PropertiesFileManager;
 
/**
 * Classe qui étends la libraire JSch qui permet de réaliser des appels en SFTP en Java de façon siplifiée.
 * @author Arnaud Charleroy
 * @update [Andréas Garcia] 23 Jan., 2019 : Add generic function to create nested 
 * directories from a complete path
 */
public class FileUploader extends JSch{
    final static Logger LOGGER = LoggerFactory.getLogger(FileUploader.class);
    private static final String PROPERTY_FILE_NAME = "service";
    private String SFTPHost;
    private String SFTPUser;
    private String SFTPPass;
    private String SFTPWorkingDirectory;
    private Session session = null;
    private Channel channel = null;
    private ChannelSftp channelSftp = null;
    
    public FileUploader() {
        // Paramtères de connexion
        SFTPHost = PropertiesFileManager.getConfigFileProperty(PROPERTY_FILE_NAME, "uploadFileServerIP");
        SFTPUser = PropertiesFileManager.getConfigFileProperty(PROPERTY_FILE_NAME, "uploadFileServerUsername");
        SFTPPass = PropertiesFileManager.getConfigFileProperty(PROPERTY_FILE_NAME, "uploadFileServerPassword");
        SFTPWorkingDirectory = PropertiesFileManager.getConfigFileProperty(PROPERTY_FILE_NAME, "uploadFileServerDirectory");
        
        try {
            // Connection
            session = getSession(SFTPUser,SFTPHost);
            session.setConfig("StrictHostKeyChecking", "no");
            session.setPassword(SFTPPass);
            session.connect();
            channel = session.openChannel("sftp");
            channel.connect();
            channelSftp = (ChannelSftp) channel;
            //Changement de dossier
            channelSftp.cd(SFTPWorkingDirectory);
            
        } catch (SftpException ex) {
            LOGGER.error(ex.getMessage(), ex);        
        } catch (JSchException ex) {
            LOGGER.error(ex.getMessage(), ex);  
        }
    }
    
    public boolean fileTransfer(File f, String filename) {
        // Envoie d'un fichier au dossier prévu
        FileInputStream fStream = null;
        try {
            fStream = new FileInputStream(f);
            channelSftp.put(new FileInputStream(f), filename);
        } catch (FileNotFoundException ex) {
            LOGGER.error(ex.getMessage(), ex);
            return false;
        } catch (SftpException | IOException ex) {
            LOGGER.error(ex.getMessage(), ex);  
            return false;
        }finally{
            if(fStream != null){
                try {
                    fStream.close();
                } catch (IOException ex) {
                    LOGGER.error("Error during file closing", ex);  
                }
            }
        }
        
        return true;
    }
    
    /**
     * Create nested directories from a given path.
     * The mkdir function of thet ChannelSftp object can only create one 
     * directory in the current folder. If a path with several folders is given, 
     * an exception is thrown.
     * So we have to mannually implement the behaviour desired.
     * @param nestedDirectoriesPath
     * @throws SftpException 
     */
    public void createNestedDirectories(String nestedDirectoriesPath) throws SftpException {
        ChannelSftp channelTarget = getChannelSftp();
        
        // Split the complete path into a list of folders
        String[] directories = nestedDirectoriesPath.split("/");
        channelTarget.cd("/");
        for (String directory : directories) {
            if (directory.length() > 0) {
                try {
                    channelTarget.cd(directory);
                } catch (SftpException e2) {
                    // If the folder doesn't exist, create it and go into
                    channelTarget.mkdir(directory);
                    channelTarget.cd(directory);
                }
            }
        }
        // Go back to the original folder
        for (String directory : directories) {
            channelTarget.cd("..");
        }
    }
    
    /**
     * Fermeture des ressources
     */
    public void closeConnection(){
        channelSftp.exit();
        channelSftp.disconnect();
        channel.disconnect();
        session.disconnect();
    }

    public String getSFTPHost() {
        return SFTPHost;
    }

    public void setSFTPHost(String SFTPHost) {
        this.SFTPHost = SFTPHost;
    }

    public String getSFTPUser() {
        return SFTPUser;
    }

    public void setSFTPUser(String SFTPUser) {
        this.SFTPUser = SFTPUser;
    }

    public String getSFTPPass() {
        return SFTPPass;
    }

    public void setSFTPPass(String SFTPPass) {
        this.SFTPPass = SFTPPass;
    }

    public String getSFTPWorkingDirectory() {
        return SFTPWorkingDirectory;
    }

    public void setSFTPWorkingDirectory(String SFTPWorkingDirectory) {
        this.SFTPWorkingDirectory = SFTPWorkingDirectory;
    }

    public Session getSession() {
        return session;
    }

    public void setSession(Session session) {
        this.session = session;
    }

    public Channel getChannel() {
        return channel;
    }

    public void setChannel(Channel channel) {
        this.channel = channel;
    }

    public ChannelSftp getChannelSftp() {
        return channelSftp;
    }

    public void setChannelSftp(ChannelSftp channelSftp) {
        this.channelSftp = channelSftp;
    }
    
    
}

//public static void main(String[] args) {
//    FileUploader jsch = new FileUploader();
//    File f = new File("/home/Arnaud Charleroy/Documents/1.jpg");
//    boolean fileTransfer = jsch.fileTransfer(f, "tranféréee.jpg");
//    System.err.println(fileTransfer);
//    jsch.closeConnection();
//    }
// 
//}

