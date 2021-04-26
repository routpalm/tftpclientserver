import java.net.*;
import java.io.*;
/*
* PacketBuilder - Builds/Dissects packets 
* Author - Nicholas Anthony
* Date - 4/22/2021
*/
public class PacketBuilder implements TFTPConstants{
   //Packet that will be built by build() or taken from user in the constructor
   private DatagramPacket packet = null;
   
   //Attributes
   private InetAddress address = null;
   private int port = 0;
   private int opcode = 0; 
   private int blockNo = 0;
   private int dataLen = 0;
   private byte[] data = null;
   private String filename = null; 
   private String msg = null;
   
   //Create with packet usually for dissection purposes 
   public PacketBuilder(DatagramPacket _packet){
      packet = _packet;
   }
   
   //Create with raw info, usually for building purposes
   public PacketBuilder(int _opcode, int _port, InetAddress _address, int _blockNo, String _filename, String _msg, byte[] _data, int _dataLen){
      opcode = _opcode;
      port = _port;
      address = _address;
      blockNo = _blockNo;
      filename = _filename;
      msg = _msg;
      data = _data;
      dataLen = _dataLen;
   }
   
   //Getters
   public int getOpcode() { 
      return opcode; }
   public int getPort() { 
      return port; }
   public InetAddress getAddress() { 
      return address; }
   public int getBlockNo() { 
      return blockNo; }
   public String getFilename() { 
      return filename; }
   public String getMsg() { 
      return msg; }
   public byte[] getData() { 
      return data; }
   public int getDataLen() { 
      return dataLen; }
      
   /*
   * build() - takes raw info from user and builds a packet corresponding to the given opcode
   */
   public DatagramPacket build(){
      packet = null;
      
      switch(opcode){
         case RRQ: //If opcode is 1
            try{ 
               ByteArrayOutputStream baos = new ByteArrayOutputStream(2 + filename.length() + 1 + "octet".length() + 1); //BAOS is using the amt of bytes for the opcode + filename + octet
               DataOutputStream dos = new DataOutputStream(baos);
               
               //Writing to the ByteArray (opcode, file name, and addendum)
               dos.writeShort(opcode);
               dos.writeBytes(filename);
               dos.writeByte(0);
               dos.writeBytes("octet");
               dos.writeByte(0);
               dos.close();
               byte[] holder = baos.toByteArray(); //taking all data from dos and converting to byte array
               packet = new DatagramPacket(holder, holder.length, address, port); //Writing necessary data
            }catch(IOException ioe){}
            break;
         case WRQ://If opcode is 2
            try{ 
               ByteArrayOutputStream baos = new ByteArrayOutputStream(2 + filename.length() + 1 + "octet".length() + 1); //BAOS is using the amt of bytes for the opcode + filename + octet
               DataOutputStream dos = new DataOutputStream(baos);
               
               //Writing to the ByteArray (opcode, file name, and addendum)
               dos.writeShort(opcode);
               dos.writeBytes(filename);
               dos.writeByte(0);
               dos.writeBytes("octet");
               dos.writeByte(0);
            
               dos.close();
               byte[] holder = baos.toByteArray(); //taking all data from dos and converting to byte array
               packet = new DatagramPacket(holder, holder.length, address, port); //Writing necessary data
            }catch(IOException ioe){}
            break;
         case DATA: //If opcode is 3
            try{
               if (dataLen > 0){
                  ByteArrayOutputStream baos = new ByteArrayOutputStream(2 + 2 + data.length); //BAOS is using the amt of bytes for the opcode + data + dataLen + blockNo
                  DataOutputStream dos = new DataOutputStream(baos);
                  
                  //Writing to the ByteArray (opcode, block number, data, data length)
                  dos.writeShort(opcode);
                  dos.writeShort(blockNo);
                  dos.write(data, 0, dataLen);
                  dos.close();
                  byte[] holder = baos.toByteArray(); //taking all data from dos and converting to byte array
                  packet = new DatagramPacket(holder, holder.length, address, port); //Writing necessary data
               }
            }catch (IOException ioe) {}
            break;
         case ACK: //If opcode is 4
            try{
               ByteArrayOutputStream baos = new ByteArrayOutputStream(4); //BAOS is using the amt of bytes for the opcode + blockNo
               DataOutputStream dos = new DataOutputStream(baos);
               
               //Writing to the ByteArray (opcode, block number)
               dos.writeShort(opcode);
               dos.writeShort(blockNo);
               dos.close();
               byte[] holder = baos.toByteArray(); //taking all data from dos and converting to byte array
               packet = new DatagramPacket(holder, holder.length, address, port); //Writing necessary data
            }catch (IOException ioe) {}
            break;
         case ERROR: //If opcode is 5
            try{
               ByteArrayOutputStream baos = new ByteArrayOutputStream(4 + msg.length()); //BAOS is using the amt of bytes for the opcode + blockNo + the message
               DataOutputStream dos = new DataOutputStream(baos);
               
               //Writing to the ByteArray (opcode, error number, error message)
               dos.writeShort(opcode);
               dos.writeShort(blockNo);
               dos.writeBytes(msg);
               dos.writeByte(0);
               dos.close();
               byte[] holder = baos.toByteArray(); //taking all data from dos and converting to byte array
               packet = new DatagramPacket(holder, holder.length, address, port); //Writing necessary data
            }catch (IOException ioe) {}
            break;
      
      } //end of switch
      return packet;
   } //end of build()
   
   /*
   * dissect() - takes given packet and extracts information from it given the opcode
   */
   public void dissect(){
      if (packet != null){
         DataInputStream dis;
         try{
            ByteArrayInputStream bais = new ByteArrayInputStream(packet.getData(), packet.getOffset(), packet.getLength());
            dis = new DataInputStream(bais);
            int nread;
            address = packet.getAddress();
            port = packet.getPort();
            opcode = dis.readShort();
            
            switch(opcode){
               case 1: 
                  filename = readToZ(dis);
                  break;
               case 2: 
                  filename = readToZ(dis);
                  break;
               case 3:
                  blockNo = dis.readShort();
                  dataLen = packet.getLength() - 4;
                  data = new byte[dataLen];
                  nread = dis.read(data, 0, dataLen);
                  break;
               case 4:
                  blockNo = dis.readShort();
                  break;
               case 5:
                  blockNo = dis.readShort();
                  msg = readToZ(dis);
            }
            dis.close();
         }catch(Exception e){
            System.out.println("Error dissecting packet.");
            System.exit(2);
         }
      }
   }
   
   public static String readToZ(DataInputStream dis) throws Exception {      
      String value = "";      
      while (true) {         
         byte b = dis.readByte();         
         if (b == 0) 
            return value;
         value += (char) b;
      }   
   }
   
}