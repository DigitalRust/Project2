/**
 *	@file 	UDPserver.c
 *	@brief 	Makes a server on the local machine that processes 
 *		requests with the appropriate markup tags.
 *	@author Bryce McAnally
 *	@author Eugene Neff
 *	@bug 	No known bugs.
 */

#include <stdio.h>
#include <stdlib.h>
#include <sys/types.h>
#include <sys/socket.h>
#include <netinet/in.h>
#include <netdb.h>
#include <errno.h>
#include <signal.h>
#include <unistd.h>
#include <string.h>
#include <sys/wait.h>
#include <stdbool.h>
#include <arpa/inet.h>
#include <sys/ioctl.h>
#include <net/if.h>
#include <pthread.h>

#define SERVER_NAME_LENGTH 100
#define MAX_NUM_LISTENER_ALLOWED 20
#define MAX_MESSAGE_LENGTH 256
#define PORT 0 // Set to 0 for dynamic port binding
#define START_ECHO_LENGTH 6
#define END_ECHO_LENGTH 7

/**
 *	@brief 	Fills in the server address structure with 
 *		the appropriate address family, ip address,
 *		and port number.
 *
 *	@param 	serverSocket The socket that will be used
 *		for listening to requests.
 *	@return The filled in server address structure.
 */
struct sockaddr_in fillInServerAddress(int serverSocket);

/**
 *	@brief 	Returns the ip address associated with eth0.
 *
 *	@param 	serverSocket The socket that will be used
 *		for listening to requests.
 *	@return The ip address structure.
 */
struct in_addr getIPAddress(int serverSocket);

/**
 *	@brief 	Prints server info including the host name,
 *		ip address, and port number.
 *
 *	@param 	serverAddress The server address structure.
 *	@return Void.
 */
void printServerInfo(struct sockaddr_in *serverAddress);

/**
 *	@brief 	Creates a child thread to handle an incoming
 *		request.
 *
 *	@param 	connectionSocket The socket used to handle 
 *		the request.
 *	@return Void.
 */
void handleDatagram(int serverSocket);

/**
 *	@brief 	The thread function that handles an incoming
 *		request. This function receives the incoming
 *		message, processes it, and returns an 
 *		appropriate message.
 *
 *	@param 	connectionSocket The socket used to handle 
 *		the request.
 *	@return Void.
 */
void *threadHandleMessage(void *incomingMessage);

/**
 *	@brief 	Prepares the incoming message for processing.
 *
 *	@param 	message The incoming message.
 *	@param  length	The number of bytes in the incoming
 *		message.
 *	@return Void.
 */
int prepareMessage(char incomingMessage[], int bytes);

/**
 *	@brief 	Parses and analyzes the incoming message.
 *		Creates the response message and stores
 *		it back in the array used for the original
 *		message.
 *
 *	@param 	message The incoming message.
 *	@param  length	The length of the incoming message.
 *	@return Void.
 */
int processMessage(char message[], int length);

/**
 *	@brief 	Checks for errors with functions that utilize
 *		errno. Prints out a descriptive error message.
 *
 *	@param 	error Negative if an error has occured.
 *	@param  function The function call that caused the error.
 *	@return Void.
 */
void checkError(int error, const char *function);

typedef struct DatagramInfo{
	struct sockaddr_in from;
	char buffer[MAX_MESSAGE_LENGTH];
	int length, serverSocket;
} DatagramInfoT, *DatagramInfoP;

bool serverOn = true;

int main()
{
	// Creating TCP socket	
	int serverSocket;
	checkError(serverSocket = socket(AF_INET, SOCK_DGRAM, 0), "socket");
    
	// Filling in destination address structure
	struct sockaddr_in serverAddress = fillInServerAddress(serverSocket);
	
	// Binds the socket
	socklen_t length = sizeof(serverAddress);	
	checkError(bind(serverSocket, (struct sockaddr*)&serverAddress, length), "bind");				
	checkError(getsockname(serverSocket, (struct sockaddr *)&serverAddress, &length), "getsockname");
	
	// Print server information
	printServerInfo(&serverAddress);

	while(serverOn)
		handleDatagram(serverSocket);
	close(serverSocket);
	return 0;
}

struct sockaddr_in fillInServerAddress(int serverSocket)
{
	struct sockaddr_in serverAddress;
	
	// Clear data in serverAddress struct
	memset((void *)&serverAddress, 0, (size_t)sizeof(serverAddress));
	
	serverAddress.sin_family = (short)(AF_INET);
	serverAddress.sin_addr = getIPAddress(serverSocket);
	serverAddress.sin_port = htons((u_long)PORT);

    	return serverAddress;
}

struct in_addr getIPAddress(int serverSocket)
{
	struct ifreq interfaceRequest;	
	
	// Utilizes the network interface to get the global IP address of the local machine. 
	// Using gethostbyname returns the loopback address.
	interfaceRequest.ifr_addr.sa_family = AF_INET;
	strncpy(interfaceRequest.ifr_name, "eth0", IFNAMSIZ - 1);
	checkError(ioctl(serverSocket, SIOCGIFADDR, &interfaceRequest), "ioctl");

	return ((struct sockaddr_in *)&interfaceRequest.ifr_addr)->sin_addr;
}

void printServerInfo(struct sockaddr_in *serverAddress)
{
	char hostName[SERVER_NAME_LENGTH];
	checkError(gethostname(hostName, SERVER_NAME_LENGTH), "gethostname");

	printf("\nHost Name : %s\n", hostName);
	printf("Address : %s\n", inet_ntoa(serverAddress->sin_addr));
	printf("Port Number : %d\n\n", ntohs(serverAddress->sin_port));
}

void handleDatagram(int serverSocket)
{
	DatagramInfoP incomingMessage = (DatagramInfoP)malloc(sizeof(DatagramInfoT));	
	socklen_t length = sizeof(incomingMessage->from);	
	pthread_t threadID;   
	
	int bytes = recvfrom(serverSocket, incomingMessage->buffer, (size_t)sizeof(incomingMessage->buffer), 0, (struct sockaddr *)&incomingMessage->from, &length);
	incomingMessage->length = bytes;
	incomingMessage->serverSocket = serverSocket;
	prepareMessage(incomingMessage->buffer, bytes);	
	
	pthread_create(&threadID, NULL, threadHandleMessage, (void *)incomingMessage);
	pthread_detach(threadID);
}

void *threadHandleMessage(void *incomingMessage)
{
	DatagramInfoP message = (DatagramInfoP)incomingMessage;

	message->length = processMessage(message->buffer, prepareMessage(message->buffer, message->length));		
	sendto(message->serverSocket, message->buffer, (size_t)message->length, 0, (struct sockaddr *)&message->from, (socklen_t)sizeof(message->from));	

	// Frees the dynamically allocated struct.
	free(message);
	pthread_exit(0);
}

int prepareMessage(char message[], int bytes){	
	// For java client, adds null terminator to the end of the message.	
	if(message[bytes - 1] != '\0')
	{	
		message[bytes] = '\0';
		return bytes;
	}
	// For c client, replaces newline character with null terminator.	
	else
	{ 
		message[bytes - 2] = '\0'; 
		return bytes - 2;
	}
}

int processMessage(char message[], int length)
{
	char messageBody[length];	
	
	fprintf(stderr, "Incoming message : %s\n", message);
	// Handles <echo>message</echo> format.	
	if(!strncmp(message, "<echo>", START_ECHO_LENGTH) && !strncmp(&message[length - END_ECHO_LENGTH], "</echo>", END_ECHO_LENGTH))
	{
		strncpy(messageBody, &message[START_ECHO_LENGTH], length - (START_ECHO_LENGTH + END_ECHO_LENGTH));
		sprintf(message, "<reply>%s</reply>", messageBody);		
	}
	// Handles <loadavg/> format.
	else if(!strcmp(message, "<loadavg/>"))
	{
		double loadAvg[3];
		getloadavg(loadAvg, 3);
		sprintf(message, "<replyLoadAvg>%lf:%lf:%lf</replyLoadAvg>", loadAvg[0], loadAvg[1], loadAvg[2]);	
	}
	else if(!strcmp(message, "<shutdown/>"))
	{
		serverOn = false;
	}
	// Handles incorrect format.
	else
	{
		strcpy(messageBody, message);
		sprintf(message, "<error>%s</error>", messageBody);
	}
	fprintf(stderr, "Outgoing message : %s\n", message);
	return strlen(message);
}

void checkError(int error, const char *function)
{
	if(error < 0)
	{
		perror(function);
		exit(1);
	}
}