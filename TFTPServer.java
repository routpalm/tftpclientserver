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
* TFTPServer - A multithreaded program that allows for remote client connection, and file upload/download.
* Author: Nicholas Anthony, Victor Hermes, & Rowan Keller
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
   private DatagramSocket socket = null;   
   
   //Misc.
   private File dir = null;
   
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
      dir = new File(".");
      tfFolder.setDisable(true);
      tfFolder.setText(dir.getAbsolutePath());
      
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
      tfFolder.setPrefWidth(400);
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
      
      btnFolder.setOnAction(e -> { doChooseFolder(); });
      
      // Show window
      scene = new Scene(root, 600, 300);
      stage.setScene(scene);
      stage.setX(800);
      stage.setY(100);
      stage.show();      
   }
   
   //Starts accepting connections
   public void doStart(){
      btnStartStop.setText("Stop");
      btnFolder.setDisable(true);
      ListenerThread th = new ListenerThread();
      th.start();
   }
   
   //Stops accepting connections
   public void doStop(){
      btnStartStop.setText("Start");
      btnFolder.setDisable(false);
      if (socket != null){
         try { 
            socket.close();
         }catch (Exception e){
            log("Exception while closing socket: " + e);
         }
         socket = null;
      }
   }
   
   //Allows user to choose folder for use
   public void doChooseFolder(){
      DirectoryChooser dc = new DirectoryChooser();
      dc.setTitle("Choose a folder!");
      dc.setInitialDirectory(dir);
      dir = dc.showDialog(stage); //Changing directory to user specified folder
      tfFolder.setText(dir.getAbsolutePath());
   }
   
   
   /** 
    * ListenerThread
    * listens for new client packet
    */
   class ListenerThread extends Thread {
      public void run(){
         try{
            log("ListenerThread started...");
            
            //Initialize socket to TFTP port (69)
            socket = new DatagramSocket(TFTP_PORT);
            
            while (true){
              DatagramPacket incPacket = new DatagramPacket(new byte[1500], MAX_PACKET_SIZE); //Create empty packet to serve as vessel for incoming packet from client
              socket.receive(incPacket); //Attempt to receive data from client
              log("Packet received from client!");
              ClientThread ct = new ClientThread(incPacket, taLog); //Create ClientThread and pass it the incoming packet 
              ct.start();  //Start new clientThread
            }
         }catch(Exception e){
            log("Exception occurred in ListenerThread...");
         }
      }
    }   
      
      //log - utility to log in thread-safety
      private void log(String message){
         Platform.runLater(
            new Runnable(){
               public void run(){
                  taLog.appendText(message + "\n");
               }
            });
      }
   }
   
   class ClientThread extends Thread{
      private DatagramPacket packet = null;
      private DatagramSocket clientSocket = null;

      private TextArea taLog;
         
      private DataOutputStream dos = null;
      private DataInputStream dis = null;
         
         //Constructor for ClientThread
      public ClientThread(DatagramPacket _packet, TextArea _taLog){
         packet = _packet;
         taLog = _taLog;
      }
         
         //Main program for a ClientThread
      public void run(){
            
         try{

         }catch (Exception e){
            log(clientID + "Exception occurred: " + e + "\n");
            return;
         } 
      } //of run

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
