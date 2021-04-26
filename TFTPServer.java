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
      
      btnStartStop.setOnAction(
         e ->{
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
               ClientThread ct = new ClientThread(incPacket); //Create ClientThread and pass it the incoming packet 
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
   
   
   class ClientThread extends Thread{
      private DatagramPacket packet = null;
      private DatagramSocket clientSocket = null;
         
      private InetAddress clientID;
         
      private DataOutputStream dos = null;
      private DataInputStream dis = null;
         
         //Constructor for ClientThread
      public ClientThread(DatagramPacket _packet){
         packet = _packet;
      }
         
         //Main program for a ClientThread
      public void run(){
         try{
            clientSocket = new DatagramSocket();
            clientSocket.setSoTimeout(1000);
            // receiving a packet and reading in the info
            PacketBuilder pktbR = new PacketBuilder(packet);
            pktbR.dissect();
            
            switch(pktbR.getOpcode()){
               case 1:
                  log("RRQ process initiated with file name " + pktbR.getFilename());
                  doRRQPacket(pktbR);
                  break;
               case 2:
                  log("WRQ process initiated with file name " + pktbR.getFilename());
                  //doWRQPacket(pktbR);
                  break;
               case 3:
                  log("Invalid opcode sent: " + pktbR.getOpcode());
                  sendErrPkt(5, pktbR.getPort(), pktbR.getAddress(), 4, null, "Invalid opcode sent" + pktbR.getOpcode(), null, 0);
                  break;
               case 4:
                  log("Invalid opcode sent: " + pktbR.getOpcode());
                  sendErrPkt(5, pktbR.getPort(), pktbR.getAddress(), 4, null, "Invalid opcode sent" + pktbR.getOpcode(), null, 0);
                  break;
               case 5:
                  log("Invalid opcode sent: " + pktbR.getOpcode());
                  sendErrPkt(5, pktbR.getPort(), pktbR.getAddress(), 4, null, "Invalid opcode sent" + pktbR.getOpcode(), null, 0);
                  break;   
               }
            
         }catch (Exception e){
            log(clientID + "Exception occurred: " + e + "\n");
            return;
         } 
      } //of run
      /*
      * doRRQPacket - takes given filename and sends file through DATA packet to client
      */
      private void doRRQPacket(PacketBuilder pktb){
         //Find filename
         String fileName = tfFolder.getText() + File.separator + pktb.getFilename();
         log("RRQ - Opening " + fileName + "...");
         FileInputStream fis = null;
         try{
            File f = new File(fileName);
            fis = new FileInputStream(f);
         }catch (IOException ioe){
            sendErrPkt(5, pktb.getPort(), pktb.getAddress(), 4, null, "Error reading file", null, 0);
         } 
         //read file
         int blockSize = 512;
         int fSize = 0;
         int blockNo = 1;
         while(blockSize == 512){
            byte[] block = new byte[512];
            try{
               fSize = fis.read(block);
            }catch(IOException ioe){ fSize = 0;}
            try{
               PacketBuilder pktOut = new PacketBuilder(3, pktb.getPort(), pktb.getAddress(), blockNo, null, null, block, fSize);
               log("RRQ - Server sending " /*+ PacketChecker.decode(pktOut)*/);
               clientSocket.send(pktOut.build());
            }catch (IOException ioe){}
            
            blockSize = fSize; //Making sure there's still data left in the file to read and send
            DatagramPacket ackPkt = new DatagramPacket(new byte[1500], MAX_PACKET_SIZE);
            try{
               clientSocket.receive(ackPkt);
            }catch (SocketTimeoutException ste){
               log("RRQ - Timed out awaiting ACK packet");
            }catch(IOException ioe) {}
            
            PacketBuilder ackPktb = new PacketBuilder(ackPkt);
            ackPktb.dissect();
            if (ackPktb.getOpcode() != ACK){
               sendErrPkt(5, ackPktb.getPort(), ackPktb.getAddress(), 4, null, "Illegal opcode: " + ackPktb.getOpcode() , null, 0);
            }
           blockNo++; 
         }
      }
      
      private void doWRQPacket(PacketBuilder pktb){
         //Find filename
         String fileName = tfFolder.getText() + File.separator + pktb.getFilename();
         log("WRQ - Opening " + fileName + "...");
         try{
            File f = new File(fileName);
            dos = new DataOutputStream(new FileOutputStream(f));
         }catch (IOException ioe){
            sendErrPkt(5, pktb.getPort(), pktb.getAddress(), 4, null, "Error reading file", null, 0);
            return;
         } 
         //read file
         int blockSize = 512;
         int fSize = 0;
         int blockNo = 1;
         while(true){
            //Send ACK packet
            //First action in the while loop because we need to respond to the initial WRQ packet
            try{
               PacketBuilder pktOut = new PacketBuilder(4, pktb.getPort(), pktb.getAddress(), blockNo, null, null, null, null);
               log("WRQ - Server sending " /*+ PacketChecker.decode(pktOut)*/);
               clientSocket.send(pktOut.build());
            }catch (IOException ioe){}
            if (blockSize < 512) break; //If all the file data has been written, break
            
            DatagramPacket pktIn = new DatagramPacket(new byte[1500], MAX_PACKET_SIZE);
            try{
               csocket.receive(pktIn);
            }catch(SocketTimeoutException ste) {
               log("WRQ - Timed out awaiting DATA packet");
               return;
            }
            log("WRQ - Server received " /*+ PacketChecker.decode(pktIn)*/);
            
            blockSize = fSize; //Making sure there's still data left in the file to read and send
            DatagramPacket ackPkt = new DatagramPacket(new byte[1500], MAX_PACKET_SIZE);
            try{
               clientSocket.receive(ackPkt);
            }catch (SocketTimeoutException ste){
               log("RRQ - Timed out waiting for ACK from client");
            }catch(IOException ioe) {}
            
            PacketBuilder ackPktb = new PacketBuilder(ackPkt);
            ackPktb.dissect();
            if (ackPktb.getOpcode() != ACK){
               sendErrPkt(5, ackPktb.getPort(), ackPktb.getAddress(), 4, null, "Illegal opcode: " + ackPktb.getOpcode() , null, 0);
            }
           blockNo++; 
         }
      }
      
      private void sendErrPkt(int _opcode, int _port, InetAddress _address, int _blockNo, String _filename, String _msg, byte[] _data, int _dataLen){
         try{
            PacketBuilder errPkt = new PacketBuilder(_opcode, _port, _address, _blockNo,  _filename, _msg, _data, _dataLen);
            clientSocket.send(errPkt.build());
         }catch(IOException ioe){}
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
}