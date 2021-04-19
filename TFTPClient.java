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
   
   // Other attributes
   public static final int SERVER_PORT = 32001;
   private Socket socket = null;
    
    
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
      taLog.appendText("doing choose folder\n");
   }
   
   public void doUpload()
   {
      taLog.appendText("doing upload\n");      
   }
   
   public void doDownload()
   {
      taLog.appendText("doing download\n");   
   } 
}