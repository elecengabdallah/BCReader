/*
 * This program developed by Abd-Allah Farag
 * It is a simple program to communicate with Cognex DataMan 260 Barcode Reader
 * Apache Telnet API is used here according to Apache license
 * Developed in 31/05/2016 03:00 AM
 * Version 0.1
 */

import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.SocketException;
import java.util.GregorianCalendar;
import java.util.StringTokenizer;

import org.apache.commons.net.telnet.EchoOptionHandler;
import org.apache.commons.net.telnet.InvalidTelnetOptionException;
import org.apache.commons.net.telnet.SuppressGAOptionHandler;
import org.apache.commons.net.telnet.TelnetClient;
import org.apache.commons.net.telnet.TelnetNotificationHandler;
import org.apache.commons.net.telnet.TerminalTypeOptionHandler;




public class BCReader implements TelnetNotificationHandler {
	
	public FileWriter dataFileWrite;
	
	private InputStream in;
	private OutputStream out;
	private TelnetClient telnetSocket;
	private BCReaderGUI gui = null;
	private long time;
	
	private String serverAddress = null;
	private int serverPort = 0;
	
//	private StringTokenizer stringTokenizer;
//	private String lastGoodCode;
//	private int validCodes = 0 , badCodes = 0;
	
	
	/**
	 * Constructor
	 */	
	BCReader(String serverAddress, int serverPort, BCReaderGUI gui) {
		this.serverAddress = serverAddress;
		this.serverPort = serverPort;
		this.gui = gui;
		
		try {
			time = new GregorianCalendar().getTimeInMillis();
			dataFileWrite = new FileWriter("dataFile" + String.valueOf(time) + ".txt");
		} catch (IOException e1) {
			gui.append("Error creating data file!!!");
		}
	}
	
	
	@Override
	public void receivedNegotiation(int negotioation_code, int option_code) {
		String command = null;
		switch (negotioation_code) {
		case TelnetNotificationHandler.RECEIVED_DO:
			command = "DO";
			break;
		case TelnetNotificationHandler.RECEIVED_DONT:
			command = "DONT";
			break;
		case TelnetNotificationHandler.RECEIVED_WILL:
			command = "WILL";
			break;
		case TelnetNotificationHandler.RECEIVED_WONT:
			command = "WONT";
			break;
		case TelnetNotificationHandler.RECEIVED_COMMAND:
			command = "COMMAND";
			break;
		default:
			command = Integer.toString(negotioation_code);
			break;
		}
		gui.append("Received " + command + " for option code " + option_code);
	}
	
	/***
	 * start the telnet communication
	 * @return true if communication started
	 */
	public boolean start() {
		// create telnet client socket object
		telnetSocket = new TelnetClient();
		
//		set option handlers
		TerminalTypeOptionHandler ttopt = new TerminalTypeOptionHandler("VT100", false, false, true, false);
		EchoOptionHandler echoopt = new EchoOptionHandler(true, false, true, false);
		SuppressGAOptionHandler gaopt = new SuppressGAOptionHandler(true, true, true, true);
		
//		try registering option handlers
		try {
			telnetSocket.addOptionHandler(ttopt);
			telnetSocket.addOptionHandler(echoopt);
			telnetSocket.addOptionHandler(gaopt);
		} catch (InvalidTelnetOptionException e) {
			gui.append("Error registering option handlers1: " + e.getMessage());
		} catch (IOException e) {
			gui.append("Error registering option handlers2: " + e.getMessage());
		} 
		
//		try to connect Barcode Reader
		try {
			telnetSocket.connect(serverAddress, serverPort);
			gui.append("Connected to Barcode Reader");
		} catch (SocketException e) {
			// TODO Auto-generated catch block
			gui.append("Error connecting to Baracode Reader1: " + e.getMessage());
			return false;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			gui.append("Error connecting to Baracode Reader2: " + e.getMessage());
			return false;
		}
		
//		try to get input and output streams
		try {
			in = telnetSocket.getInputStream();
			out = telnetSocket.getOutputStream();
			gui.append("Input/output streams Created");
		} catch (Exception ioE) {
			gui.append("Error creating input/output streams");
			try {
				telnetSocket.disconnect();
			} catch (IOException e){}
			return false;
		}
		
		new ListenFromBCReader().start();

		return true;
	}
	
	/***
	 * 
	 * @param str Data to be recorded
	 */
	public void recordData(String str) {
		try {
			dataFileWrite.write(str);
			dataFileWrite.flush();
		} catch (Exception e) {
			gui.append("Error recodrding data to data file");
		}
	}
	
	/***
	 * send messages to Barcode Reader
	 * @param msg input ... string to be sent.
	 */
	public void sendMsg(String msg) {
		
		byte[] outstr = new byte[1024];
		int outstrLength = 0;
		
		outstr = msg.getBytes();
		outstrLength = outstr.length;
		
		try {
			out.write(outstr, 0, outstrLength);
			out.flush();
		} catch (Exception e) {
			gui.append("Error sending msg to server!!!!");
		}
	}
	
	/***
	 * disconnect method if error occurs or stop button pressed
	 */
	public void disconnect() {
		
		try {
			telnetSocket.disconnect();
			gui.append("Communication disconnected.");
		} catch (Exception e) {
			gui.append("Error disconnecting communication!!!");
			e.printStackTrace();
		}
		
	}
	
	// Listening thread gets data from Barcode Reader
	class ListenFromBCReader extends Thread{

		public void run() {
			
			byte[] inBuff = new byte[2048];
			int inBuffLenght = 0;
			String str = null;
			
			try {
				do {
					inBuffLenght = in.read(inBuff);
					if(inBuffLenght > 0) {
						str = new String(inBuff, 0, inBuffLenght);
						if(str.endsWith("  ")) {
							gui.dataRecorder.analyseData(str);
						};
						
						
						recordData(str+"\r\n");
						gui.append(str);
						
					}
				} while (inBuffLenght >= 0);
			} catch (Exception e) {
				gui.append("Error while reading from Barcode Reader: " + e.getMessage());
				e.printStackTrace();
			}
			
			try {
				disconnect();
			} catch(Exception e) {
				gui.append("Error disconnecting from listening thread");
			}
		}
	}

}

