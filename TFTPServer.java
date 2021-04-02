import javafx.application.*;
import javafx.event.*;
import javafx.scene.*;
import javafx.scene.control.*;
import javafx.scene.control.Alert.*;
import javafx.scene.text.*;
import javafx.scene.layout.*;
import javafx.stage.*;
import javafx.geometry.*;

import java.net.*;
import java.io.*;
import java.util.*;
/*
* RemoteFileServer - A multithreaded program that allows for remote client connection, and file upload/download.
* Author: Nicholas Anthony & Victor Hermes
* Date: 03/12/21
*/

public class TFTPServer extends Application implements TFTPConstants{
// Window attributes
   private Stage stage;
   private Scene scene;
   private VBox root;
   
   // GUI Components\
   private Button btnFolder = new Button("Choose folder: ");
   private TextField tfFolder = new TextField();
   private Button btnStartStop = new Button("Start"); 
   private Label lblStartStop = new Label("Start/stop server: ");
   private TextArea taLog = new TextArea();
   
   //Server stuff
   private ServerThread serverThread = null;
   private DatagramSocket socket = null;   
   
   /**
    * main program
    */
   public static void main(String[] args) {
      launch(args);
   }
   
   /**
    * Launch, draw and set up GUI
    * Do server stuff
    */
   public void start(Stage _stage) {
      // Window setup
      stage = _stage;
      stage.setTitle("The Compiler's TFTP Server");
      stage.setOnCloseRequest(
         new EventHandler<WindowEvent>() {
            public void handle(WindowEvent evt) { System.exit(0); }
         });
      File currentDir = new File(".");
      tfFolder.setDisable(true);
      tfFolder.setText(currentDir.getAbsolutePath());
      
      stage.setResizable(false);
      root = new VBox(8);
      
      //Top components
      FlowPane fpTop = new FlowPane(8,8);
      fpTop.setAlignment(Pos.BASELINE_LEFT);
      fpTop.getChildren().addAll(btnFolder);
      root.getChildren().add(fpTop);
      
      //Mid components
      ScrollPane spMid = new ScrollPane();
      spMid.setContent(tfFolder);
      root.getChildren().add(spMid);
      
      //Mid 2 components
      FlowPane fpMid = new FlowPane(8,8);
      fpMid.setAlignment(Pos.BASELINE_LEFT);
      fpMid.getChildren().addAll(lblStartStop, btnStartStop);
      root.getChildren().add(fpMid);
      
      // Bot (Log) components
      FlowPane fpBot = new FlowPane(8,8);
      fpBot.setAlignment(Pos.CENTER);
      taLog.setPrefRowCount(10);
      taLog.setPrefColumnCount(35);
      taLog.setWrapText(true);
      fpBot.getChildren().addAll(new Label("Log:"), taLog);
      root.getChildren().add(fpBot);
      
      btnStartStop.setOnAction(e ->{
            if (btnStartStop.getText().equals("Start")) doStart();
            else if (btnStartStop.getText().equals("Stop")) doStop();
      });
      
      // Show window
      scene = new Scene(root, 600, 300);
      stage.setScene(scene);
      stage.setX(800);
      stage.setY(100);
      stage.show();      
   }
   
   //Starts accepting connections
   public void doStart(){
      serverThread = new ServerThread();
      serverThread.start();
      btnStartStop.setText("Stop");
   }
   
   //Stops accepting connections
   public void doStop(){
      btnStartStop.setText("Start");
      try { 
         socket.close();
      }catch (Exception e){
         log("Exception while closing socket: " + e);
      }
      //socket = null;
   }
   
   
   /** 
    * ServerThread
    * does the basic non-GUI work of the server 
    */
   class ServerThread extends Thread {
      public void run(){
            //Start listening on TFTP_PORT (69)
         try {
            socket = new DatagramSocket(TFTP_PORT);
         }
         catch(IOException ioe) {
            log("IO Exception (sSocket): " + ioe);
            return;
         }
         }
      }   
      
      //log - utility to log in thread-safety
      private void log(String message){
         Platform.runLater(
            new Runnable(){
               public void run(){
                  taLog.appendText(message);
               }
            });
      }
   }
   
   class ClientThread extends Thread{
         //Since attributes are per-object items, each ClientThread has its OWN
         //socket, unique to the client. 
      private Socket cSocket;
      private String clientID = "";
      private String currentDir = "";
      private TextArea taLog;
         
      private DataOutputStream dos = null;
      private DataInputStream dis = null;
         
         //Constructor for ClientThread
      public ClientThread(Socket _cSocket, TextArea _taLog){
         cSocket = _cSocket;
         clientID = cSocket.getInetAddress().getHostAddress() + ":" + cSocket.getPort();
         taLog = _taLog;
      }
         
         //Main program for a ClientThread
      public void run(){
            
         log(clientID + " Client connected");
            
            //Initializing I/O
         try{
            dis = new DataInputStream(cSocket.getInputStream());
            dos = new DataOutputStream(cSocket.getOutputStream());
            File currentDirFinder = new File(".");
            currentDir = currentDirFinder.getCanonicalPath();
                
            while(true){
               String inputLine = dis.readUTF();
               log(clientID + " executed command " + inputLine + "\n");
               switch(inputLine){ //Making sure input is passed correctly
                  case "1":
                     doList();
                     break;
                  case "2": 
                     doCD();
                     break;
                  case "3":
                     doUpload();
                     break;
                  case "4": 
                     doDownload();
                     break;
               }
            }
         }catch (Exception e){
            log(clientID + "Exception occurred: " + e + "\n");
            return;
         } 
      } //of run
         
         //Lists files in given directory
      private void doList(){
         try{
            int fileCount = 0;
            File currentDirectory = new File(currentDir);
            File[] files = currentDirectory.listFiles(); //Getting names of all files and directories in given directory
            dos.writeInt(files.length + 1);
            dos.flush();
            log("Number of files: " + files.length);
            dos.writeUTF("Listing of: " + currentDir);
            dos.flush();
            for(File f : files){ //Listing
               dos.writeUTF((f.isDirectory() ? "D- " : "F- ") + f.getName());
               dos.flush();
               fileCount++;
            }
            log(clientID + ": " + fileCount + " files listed from directory " + currentDirectory + "\n");
            dos.writeUTF(fileCount + " files listed from directory " + currentDirectory + "\n");
            dos.writeUTF("Done.");
            dos.flush();
         }catch(Exception e){
            alert(Alert.AlertType.INFORMATION, "Error occurred (command ID: 1). " + e, "Error");
            return;
         }
      }
         
         //Checks new directory provided by the user and changes it 
      private void doCD(){
         File newDir = null;
         
         try{
            String input = dis.readUTF();
            if (input.length() < 2){
               log(clientID + ": Invalid directory.");
               dos.writeUTF("Invalid directory.");
               dos.flush();
               return;
            }
            newDir = new File(currentDir + File.separator + input); //Setting new directory from input
                  
            if (newDir.isDirectory() && newDir.exists()){ //If newdir is a directory & exists, we can use it
               currentDir = newDir.getCanonicalPath(); //Setting our global currentDir variable to new directory
               log(clientID + ": Directory changed to " + newDir.getCanonicalPath());
               dos.writeUTF("Directory changed to " + newDir.getCanonicalPath()); 
            }else{
               log(clientID + ": Directory not found");
               dos.writeUTF("Directory not found");
            }
                  
         }catch (Exception e){
            alert(AlertType.ERROR, "Error occurred: " + e, "Error");
            log(clientID + ": Error occurred: " + e + "\n");
            return;
         }
      }
         
      private void doUpload(){
         try{
            String input = dis.readUTF();
            if (input.length() < 2){
               log(clientID + ": Impossible File name specified\n");
               dos.writeUTF("Impossible File name specified");
               dos.flush();
            }
            
            File upload = new File(currentDir + File.separator + input); //Get location of target upload file
            DataOutputStream fileOS = new DataOutputStream(new FileOutputStream(upload)); //Preparing to upload received information to target file
            log(clientID + ": Uploading " + upload + "...\n");  
            long fileLength = dis.readLong(); //Get file length in long format from the client
            log("2.We got here\n");
            long i = 0L;
            while (i < fileLength) {
               byte b = dis.readByte(); //Read the byte from the client
               fileOS.writeByte(b); //Send the byte to be written in the target upload file
               i++;
            } 
            fileOS.close();
            log(clientID + ": Upload complete.\n");
            dos.writeUTF("Upload complete.");
         }catch(Exception e){
            alert(Alert.AlertType.INFORMATION, "An error occurred while uploading the file.", "UPLOAD ERROR");
            return;
         }
            
      }
      
      private void doDownload(){
         try{
            String input = dis.readUTF();
            if (input.length() < 2){
               log(clientID + ": Impossible File name specified\n");
               dos.writeUTF("Impossible File name specified");
               dos.flush();
            }
            
            File download = new File(currentDir + File.separator + input); //File to be read and sent to clients
            if(!download.exists()){
               download.createNewFile();
            }
            DataInputStream fileIS = new DataInputStream(new FileInputStream(download)); //Preparing to read the file
            log(clientID + ": Downloading " + download + "...\n");  
            long fileLength = download.length(); //Getting file length in long format to send to the client
            dos.writeLong(fileLength); //Sending file length to client
            log("1.We got here\n");
            long i = 0L;
            while (i < fileLength) {
               byte b = fileIS.readByte(); //Read in the byte from the target file
               dos.writeByte(b); //Send byte to client
               i++;
            } 
            fileIS.close();
            log(clientID + ": Download complete.\n");
            dos.writeUTF("Download complete.");
         }catch(Exception e){
            alert(Alert.AlertType.INFORMATION, "An error occurred while downloading the file.", "DOWNLOAD ERROR");
            return;
         }
      }
         //log - utility to log in thread-safety
      private void log(String message){
         Platform.runLater(
            new Runnable(){
               public void run(){
                  taLog.appendText(message);
               }
            });
      }
         
         //alert - utility to alert in thread-safety
      private void alert(final Alert.AlertType type, final String message, final String header) {
         Platform.runLater(
            new Runnable() {
               public void run() {
                  Alert alert = new Alert(type, message);
                  alert.setHeaderText(header);
                  alert.showAndWait();
               }
            });
      }
   }
