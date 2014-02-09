import java.net.DatagramSocket;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.util.Scanner;
import java.lang.*;

/*
 * UDPclient.java
 * Systems and Networks II
 * Project 2
 * @author Eugene Neff
 * @author Bryce McAnally
 *
 * This file describes the functions to be implemented by the UDPclient class
 * You may also implement any auxillary functions you deem necessary.
 */
public class UDPclient
{
	private DatagramSocket _socket; // the socket for communication with a server
	public static int MAX_MESSAGE_LENGTH = 256;	

	/**
	 * Constructs a TCPclient object.
	 */
	public UDPclient()
	{
	}
	
	/**
	 * Creates a datagram socket and binds it to a free port.
	 *
	 * @return - 0 or a negative number describing an error code if the connection could not be established
	 */
	public int createSocket()
	{
		try
		{
			_socket = new DatagramSocket(0);
			return 0;
		}
		catch(Exception exception)
		{
			return -1;
		}
	}

	/**
	 * Sends a request for service to the server. Do not wait for a reply in this function. This will be
	 * an asynchronous call to the server.
	 * 
	 * @param request - the request to be sent
	 * @param hostAddr - the ip or hostname of the server
	 * @param port - the port number of the server
	 *
	 * @return - 0, if no error; otherwise, a negative number indicating the error
	 */
	public int sendRequest(String request, String hostAddr, int port)
	{
		try
		{
			byte[] sendBuffer = new byte[request.length()];
			sendBuffer = request.getBytes();			
			DatagramPacket packet = new DatagramPacket(sendBuffer, sendBuffer.length, InetAddress.getByName(hostAddr), port);
			_socket.send(packet);
			return 0;
		}
		catch(Exception exception)
		{
			return -1;
		}
	}
	
	/**
	 * Receives the server's response following a previously sent request.
	 *
	 * @return - the server's response or NULL if an error occured
	 */
	public String receiveResponse()
	{
		try
		{
			byte[] receiveBuffer = new byte[MAX_MESSAGE_LENGTH];			
			DatagramPacket packet = new DatagramPacket(receiveBuffer, receiveBuffer.length);
			_socket.receive(packet);
			return new String(receiveBuffer);
		}
		catch(Exception exception)
		{
			return null;
		}
	}
	
	/*
    	 * Prints the response to the screen in a formatted way.
    	 *
    	 * response - the server's response as an XML formatted string
    	 *
    	 */
	public static void printResponse(String response)
	{
		System.out.println("Server response: " + response);
	}
 
	/*
	 * Closes an open socket.
	 *
    	 * @return - 0, if no error; otherwise, a negative number indicating the error
	 */
	public int closeSocket() 
	{
		try
		{
		    	_socket.close();
			return 0;
		}
		catch(Exception exception)
		{
		    	return -1;
		}
	}

	/**
	 * The main function. Use this function for 
	 * testing your code. We will provide a new main function on the day of the lab demo.
	 */
	public static void main(String[] args)
	{
		final String serverName;
		final int serverPort;
		String request, response;
		Scanner reader = new Scanner(System.in);
		System.out.println("Server Info : ");
		serverName = reader.next();
		System.out.println("Port : ");		
		serverPort = reader.nextInt();
		System.out.println("Request : ");
		request = reader.next();
		reader.close();
        
		UDPclient client = new UDPclient();
		client.createSocket();						
		client.sendRequest(request, serverName, serverPort);
		response = client.receiveResponse();
		UDPclient.printResponse(response);
		client.closeSocket();
	}
}