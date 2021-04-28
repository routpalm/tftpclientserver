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

/**
 * TFTPClient - multithreaded TCP client
 * @author  Victor Hermes, Rowan Keller, Nicholas Anthony
 * @version 
 */

public class TFTPClient extends Application implements EventHandler<ActionEvent> {
   // Window attributes
   private Stage stage;
   private Scene scene;
   private VBox root;
   
   // Components - ROW1
   private Label lblServer = new Label("Server: ");
   private TextField tfServer = new TextField("localhost");
   
   //Components - ROW2
   private Button btnFolder = new Button("Choose Folder");
      
   // Components - ROW3
   private TextField tfFolderPath = new TextField();
   
   // Components - ROW4
   private Button btnUpload = new Button("Upload");
   private Button btnDownload = new Button("Download");
   
   // Components - ROW5
   private Label lblLog = new Label("Log:");
   
   // Components - ROW6
   private TextArea taLog = new TextArea();
   
   //IO attributes
   ObjectOutputStream oos = null;
   ObjectInputStream ois = null;
   DataOutputStream dos = null; // for saving data packets into a file
   DataInputStream dis = null; // for retrieving data to upload from a file
   
   // Other attributes
   public static final int SERVER_PORT = 32001;
   private DatagramSocket socket = null;
    
    
   /**
    * main program 
    */
   public static void main(String[] args) {
      launch(args);
   }
   
   /**
    * launch - draw and set up GUI
    */
   public void start(Stage _stage) {
      stage = _stage;
      stage.setTitle("TFTP Client:");
      stage.setOnCloseRequest(
         new EventHandler<WindowEvent>() {
            public void handle(WindowEvent evt) { 
               //doTerminate();
               System.exit(1);
            }
         });
      stage.setResizable(false);
      
      root = new VBox(8);
      
      // ROW1 - label, text field
      FlowPane fpRow1 = new FlowPane(8,8);
      tfServer.setPrefWidth(350);
      fpRow1.getChildren().addAll(lblServer, tfServer);
      root.getChildren().add(fpRow1);
      
      // ROW2 - button
      FlowPane fpRow2 = new FlowPane(8,8);
      fpRow2.getChildren().addAll(btnFolder);
      root.getChildren().add(fpRow2);
      
      // ROW3 - textfield
      FlowPane fpRow3 = new FlowPane(8,8);
      tfFolderPath.setPrefWidth(350);
      tfFolderPath.setFont(Font.font("MONOSPACED", FontWeight.NORMAL, tfFolderPath.getFont().getSize()));
      tfFolderPath.setText("placeHolder/directory/path/test");
      tfFolderPath.setPrefColumnCount(tfFolderPath.getText().length());
      ScrollPane sp = new ScrollPane();
      sp.setContent(tfFolderPath);
      
      tfFolderPath.setPrefWidth(250);
      fpRow3.getChildren().addAll(tfFolderPath);
      root.getChildren().addAll(fpRow3, sp);
      
      // ROW4 - button, button
      FlowPane fpRow4 = new FlowPane(8,8);
      fpRow4.getChildren().addAll(btnUpload, btnDownload);
      root.getChildren().add(fpRow4);
      
      // ROW5 - label
      FlowPane fpRow5 = new FlowPane(8,8);
      fpRow5.getChildren().addAll(lblLog);
      root.getChildren().add(fpRow5);
      
      // ROW6 - textarea
      FlowPane fpRow6 = new FlowPane(8,8);
      taLog.setPrefHeight(600);
      taLog.setPrefWidth(500);
      fpRow6.getChildren().addAll(taLog);
      root.getChildren().add(fpRow6);
      
      //button listeners
      btnFolder.setOnAction(this);
      btnUpload.setOnAction(this);
      btnDownload.setOnAction(this);
      
      // Show window
      scene = new Scene(root, 500, 600);
      stage.setScene(scene);
      stage.show();      
   }
   
   /**
    * Button dispatcher
    */
   public void handle(ActionEvent ae) 
   {
      String label = ((Button)ae.getSource()).getText();
      switch(label) {
         case "Choose Folder":
            doChooseFolder();
            break;
         case "Upload":
            doUpload();
            break;
         case "Download":
            doDownload();
            break;   
      }
   } 
   public void doChooseFolder()
   {
      DirectoryChooser dc = new DirectoryChooser();
      dc.setTitle("Choose a folder!");
      File dir = dc.showDialog(stage); //Changing directory to user specified folder
      tfFolderPath.setText(dir.getAbsolutePath());
   }
   
   public void doUpload()
   {
      taLog.appendText("doing upload\n");      
   }
   
   public void doDownload() /* FILE TO DOWNLOAD IS HARDCODED - MUST CHANGE */
   {
      TextInputDialog textInput = new TextInputDialog();
      textInput.setHeaderText("What is the file you want to download?");
      textInput.setContentText("Enter file to download: ");
      textInput.showAndWait();
      String fileName = textInput.getResult();
      
      FileChooser choice = new FileChooser();
      choice.setTitle("Where to save the new file");
      choice.setInitialDirectory(new File("."));
      choice.getExtensionFilters().addAll(new FileChooser.ExtensionFilter[] { new FileChooser.ExtensionFilter("All Files", new String[] { "*.*" }) });
      File savedFile = choice.showSaveDialog(stage);
      if (savedFile == null) {
         Alert alert = new Alert(Alert.AlertType.ERROR, "File not saved.");
         alert.showAndWait();
         return;
      }
      
      // starting a download thread for the file specified
      DownloadThread dlThread = new DownloadThread(fileName, savedFile, tfServer.getText());
      dlThread.start();   
      taLog.appendText("doing download\n");
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
   
   /**
    * DownloadThread - a class which handles the transaction of packets
    * necessary for a download
    */
   class DownloadThread extends Thread implements TFTPConstants {
      // Attributes
      String filename;
      String serverIP;
      File dlDest;
      
      
      // Constructor
      public DownloadThread(String _filename, File _dlDest, String _serverIP) {
         filename = _filename;
         serverIP = _serverIP;
         dlDest = _dlDest;
      }
      
      // Overriding run() - sending and receiving necessary packets
      public void run() {
         // opening up the connection to the server
         try {
            InetAddress inet = InetAddress.getByName(serverIP);
            socket = new DatagramSocket();
            socket.setSoTimeout(5000);
            // build initial RRQ packet and send
            PacketBuilder pktb = new PacketBuilder(1, 69, inet, 0, filename, null, new byte[1], 0);
            socket.send(pktb.build());
            // send ack
            // receive data packet until data length < 512 ()
            PacketBuilder pktbR;
            int blockNoR;
            int blockSize = 512;
            do {
               DatagramPacket incPacket = new DatagramPacket(new byte[1500], MAX_PACKET_SIZE); //Create empty packet to serve as vessel for incoming packet from server
               socket.receive(incPacket); //Attempt to receive data from server
               pktbR = new PacketBuilder(incPacket);
               log("Packet received from server!\n");
               pktbR.dissect();
               blockNoR = pktbR.getBlockNo();
               
               //send ACK with incremented block number after each received data packet
               blockNoR++;
               PacketBuilder ack = new PacketBuilder(4, pktbR.getPort(), inet, blockNoR, null, null, null, 0);
               DatagramPacket ackPkt = ack.build();
               log(ackPkt.getAddress() + " " + ackPkt.getPort());
               socket.send(ackPkt);
            
            // opening DOS for the file being downloaded
               try{
                  File f = dlDest;
                  dos = new DataOutputStream(new FileOutputStream(f));
               }catch (IOException ioe){
                  blockNoR++;
                  sendErrPkt(5, pktbR.getPort(), pktb.getAddress(), blockNoR, null, "Error reading file", null, 0);
                  return;
               } 
            // receiving data and flushing to file
               if (pktbR.getOpcode() == 5){
                  log(pktbR.getMsg()); //If error packet, we cant send it to dos
                  return;
               }else if (pktbR.getOpcode() != 3){ //If illegal opcode
                  blockNoR++;
                  sendErrPkt(5, pktbR.getPort(), pktbR.getAddress(), blockNoR, null, "Illegal opcode: " + pktbR.getOpcode() , null, 0);
                  log("Illegal opcode: " + pktbR.getOpcode() + "\n");
                  return;
               }else{
                  try{ //Set block variable to remaining data length; if less than 512, we have parsed all of the file
                     blockSize = pktbR.getDataLen();
                     dos.write(pktbR.getData(), 0, blockSize);
                     dos.flush();
                  }catch (IOException ioe){ log("RRQ - Error writing data\n");} 
               }
               System.out.println(blockSize);
            } while (blockSize == 512);
            // send last ACK and close the socket
            
            socket.close();
            log("Download finished, closing socket\n");
         }
         catch (SocketTimeoutException ste) {
            log("Error: Socket Timeout " + ste + "\n");
         }
         catch (UnknownHostException uhe) {
            log("Error: Unknown Host, " + uhe + "\n");
         }
         catch (SocketException se) {
            log("Error: Cannot open socket " + se + "\n");
         }
         catch (IOException ioe) {
            log("Error: IOE " + ioe + "\n");
         }
         socket.close();
         
      }
      
   } // end of DownloadThread
   /*
      * sendErrPkt() - sends an error packet
      */
   private void sendErrPkt(int _opcode, int _port, InetAddress _address, int _blockNo, String _filename, String _msg, byte[] _data, int _dataLen){
      try{
         PacketBuilder errPkt = new PacketBuilder(_opcode, _port, _address, _blockNo,  _filename, _msg, _data, _dataLen);
         socket.send(errPkt.build());
      }catch(IOException ioe){}
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

   
} // end of TFTPClient
