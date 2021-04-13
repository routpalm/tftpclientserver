public interface TFTPConstants {
   /* OPcodes */
   public static final int RRQ = 1; // RRQ: read request, sent by the client in order to download data
   
   public static final int WRQ = 2; // WRQ: write request, sent by the client in order to upload data
   
   public static final int DATA = 3; // Indicates packets containing data (being uploaded or downloaded)
   
   public static final int ACK = 4; // Acknowledgement, indicates to the party sending data that the previous packet was received
   
   public static final int ERROR = 5; // Indicates that there was with the previous packet (terminates the connection)
   
   /* Error Codes */
   public static final int UNDEF = 0; // Undefined Error: allows customized error messages
   
   public static final int NOTFD = 1; // Not Found Error: when a requested file on the server isn't available (doesn't exist)
   
   public static final int ACCESS = 2; // Access Error: when the server can't open a file for download or upload
   
   public static final int DSKFUL = 3; // Disk Full Error: when the disk of the receiver of the data is full
   
   public static final int ILLOP = 4; // Illegal OPcode Error: when a packet with an illegal OPcode (<1 or >5) is sent
   
   public static final int UNKID = 5; // Unknown Transfer ID: not relevant to our setup
   
   public static final int FILEX = 6; // File Exists Error: not relevant to our setup
   
   public static final int NOUSR = 7; // No Such User Error: not relevant to our setup
   
   /* Misc. */
   public static final int MAX_PACKET_SIZE = 1500;
   
   public static final int TFTP_PORT = 69;
   
}