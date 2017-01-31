/**
    C++ client example using sockets.
    This programs can be compiled in linux and with minor modification in
     mac (mainly on the name of the headers)
    Windows requires extra lines of code and different headers
 #define WIN32_LEAN_AND_MEAN

 #include <windows.h>
 #include <winsock2.h>
 #include <ws2tcpip.h>

   // Need to link with Ws2_32.lib, Mswsock.lib, and Advapi32.lib
 #pragma comment(lib, "Ws2_32.lib")
   ...
   WSADATA wsaData;
   iResult = WSAStartup(MAKEWORD(2,2), &wsaData);
   ...
 */
#include <iostream>    //cout
#include <string>
#include <stdio.h> //printf
#include <stdlib.h> //atoi
#include <string.h>    //strlen
#include <sstream>
#include <sys/socket.h>    //socket
#include <arpa/inet.h> //inet_addr
#include <netinet/in.h>
#include <sys/types.h>
#include <unistd.h>
#include <netdb.h>
#include <vector>

#define BUFFER_LENGTH 2048

//Define some nice colors for our UI
#define NORMAL  "\x1B[0m"
#define RED  "\x1B[31m"
#define GREEN  "\x1B[32m"

//Helper function for sleeping
void ftpSleep() {
        //Sleep for 200 milliseconds (200000 microseconds)
        usleep(200000);
}

//Helper functions for displaying messages
void displayMessage(std::string message) {
        printf("%s%s%s\n", NORMAL, message.c_str(), NORMAL);
}

//Display Message with nice stars and green color
void displayUserMessage(std::string message) {
        printf("%s*********************************\n", GREEN);
        printf("%s\n", message.c_str());
        printf("*********************************%s\n", NORMAL);
}

//Display Message with nice stars and red color
void displayUserError(std::string message) {
        printf("\n\n\n");
        printf("%s*********************************\n", RED);
        printf("%s\n", message.c_str());
        printf("*********************************%s\n", NORMAL);
}

//Create a connection with the specified host
int createConnection(std::string host, int port)
{
        int s;
        struct sockaddr_in sockaddr;

        memset(&sockaddr,0, sizeof(sockaddr));
        s = socket(AF_INET,SOCK_STREAM,0);
        sockaddr.sin_family=AF_INET;
        sockaddr.sin_port= htons(port);

        int a1,a2,a3,a4;
        if (sscanf(host.c_str(), "%d.%d.%d.%d", &a1, &a2, &a3, &a4 ) == 4)
        {
                std::cout << "by ip";
                sockaddr.sin_addr.s_addr =  inet_addr(host.c_str());
        }
        else {
                std::cout << "by name";
                hostent *record = gethostbyname(host.c_str());
                in_addr *addressptr = (in_addr *)record->h_addr;
                sockaddr.sin_addr = *addressptr;
        }
        if(connect(s,(struct sockaddr *)&sockaddr,sizeof(struct sockaddr))==-1)
        {
                perror("connection fail");
                exit(1);
                return -1;
        }
        return s;
}

// Send a request to our connected server
int request(int sock, std::string message)
{
        return send(sock, message.c_str(), message.size(), 0);
}

//Get the reply from a server after a request
std::string reply(int s)
{
        std::string strReply;
        int count;
        char buffer[BUFFER_LENGTH+1];

        ftpSleep();
        do {
                count = recv(s, buffer, BUFFER_LENGTH, 0);
                buffer[count] = '\0';
                strReply += buffer;
        } while (count == BUFFER_LENGTH);
        return strReply;
}

//request from the server, and then handle the reply
//Utilizeses the helper functions, request(), and reply()
std::string requestReply(int s, std::string message)
{
        if (request(s, message) > 0)
        {
                return reply(s);
        }
        return "";
}

//Check reply codes for errors
bool isReplyCodeValid(std::string replyCode, int expectedCode) {
        // Parse the code from the string
        int serverCode = atoi(replyCode.c_str());

        //Check if the two codes are the same
        if(serverCode == expectedCode || serverCode == 0) return true;
        std::stringstream unexpectedError;
        unexpectedError << "Unexpected Code: " << serverCode << "\nExpected: " << expectedCode << "\n";
        displayUserError(unexpectedError.str());
        return false;
}

//Send a request and reply to the server. Wrap the request around
//PASV, in order to allow for listing and retreiving files.
std::string passiveRequestReply(int socket, std::string message) {

        //First, set to passive mode
        std::string passiveReply = requestReply(socket, "PASV\r\n");
        ftpSleep();
        if(!isReplyCodeValid(passiveReply, 227)) exit(0);

        //Parse our ip and port
        //Grab the sub string of ip and port
        std::string ipPort = passiveReply
                             .substr(passiveReply.find("(") + 1,
                                     passiveReply.find(")") - passiveReply.find("(") - 1);

        //Build our connection string with our un-parse ip and port
        std::vector<std::string> responseHost;
        size_t index = 0;
        std::string delimiter = ",";
        //Parse by the , and create our array with each part of the host
        while((index = ipPort.find(delimiter)) != std::string::npos) {
                std::string foundItem = ipPort.substr(0, index);
                responseHost.push_back(foundItem);
                ipPort.erase(0, index + delimiter.length());
        }
        //Set the last item to the remainder of the ipPort
        responseHost.push_back(ipPort);
        //Bit shift to get the port
        int connectionPort = (atoi(responseHost[4].c_str()) << 8 | atoi(responseHost[5].c_str()));
        //Build the final connection host string
        std::string connectionHost = responseHost[0] + "." + responseHost[1] +
                                     "." + responseHost[2] + "." + responseHost[3];


        //Create our new connection
        std::stringstream connectionMessage;
        connectionMessage << "Connecting to " << connectionHost << ":" << connectionPort;
        displayMessage(connectionMessage.str());
        int newConnection = createConnection(connectionHost, connectionPort);
        ftpSleep();

        //Ask the pi socket for a response
        std::string piSocketReply = requestReply(socket, message);
        ftpSleep();
        if(!isReplyCodeValid(piSocketReply, 150)) exit(0);

        //Pass to the request reply function with the message and new connection
        std::string dtpResponse = reply(newConnection);
        ftpSleep();

        //Close the Connection to the new conneciton
        close(newConnection);

        return dtpResponse;
}

//List Files From the Server
void listFiles(int socket) {
        displayUserMessage("Listing Files from the server root directory");
        std::string fileListing = passiveRequestReply(socket, "LIST\r\n");
        displayMessage(fileListing);
        std::string serverReply = reply(socket);
        if(!isReplyCodeValid(serverReply, 226)) exit(0);
        displayMessage(serverReply);
        ftpSleep();
}

//Get a file from the server
void getFile(int socket, std::string fileName) {
        //Format/Display the user message
        std::stringstream formattedMessage;
        formattedMessage << "Retrieving the file, " << fileName.c_str();
        displayUserMessage(formattedMessage.str());

        //Format the filename with the return and newline
        std::stringstream formattedFileName;
        formattedFileName << "RETR " << fileName.c_str() << "\r\n";
        std::string fileReply = passiveRequestReply(socket, formattedFileName.str());

        //Print the responses
        displayMessage(fileReply);
        std::string socketReply = reply(socket);
        if(!isReplyCodeValid(socketReply, 226)) exit(0);
        displayMessage(socketReply);
        ftpSleep();
}

//Send QUIT to the server
void quitConnection(int socket) {
        displayUserMessage("Sending QUIT to the server, and closing the connection...");
        std::string strReply = requestReply(socket, "QUIT\r\n");
        displayMessage(strReply);
        close(socket);
        ftpSleep();
}


int main(int argc, char *argv[])
{
        //Initialize some variables
        int sockpi;
        std::string strReply;
        std::string piReply;

        displayUserMessage("Welcome to the CECS478 FTP Client");

        // Create a connection to the passed host and port, or the default server
        if (argc > 2)
                sockpi = createConnection(argv[1], atoi(argv[2]));
        if (argc == 2)
                sockpi = createConnection(argv[1], 21);
        else
                sockpi = createConnection("130.179.16.134", 21);
        strReply = reply(sockpi);
        if(!isReplyCodeValid(strReply, 220)) exit(0);
        printf("%s\n", strReply.c_str());

        displayUserMessage("Logging into server");
        strReply = requestReply(sockpi, "USER anonymous\r\n");
        // parse srtReply to obtain the status.
        // Let the system act according to the status and display
        printf("%s\n", strReply.c_str());
        if(!isReplyCodeValid(strReply, 331)) exit(0);
        strReply = requestReply(sockpi, "PASS asa@asas.com\r\n");
        if(!isReplyCodeValid(strReply, 230)) exit(0);
        printf("%s\n", strReply.c_str());
        ftpSleep();

        //Create an interface to use the client
        bool ftpActive = true;
        std::string userInput = "";
        while(ftpActive) {
                //Display the Available commands
                displayUserMessage("Please enter a command:");
                displayMessage("LIST - Show the files in the root directory");
                displayMessage("RETR - Retrieve a file from the root directory");
                displayMessage("QUIT - Leave the ftpclient");
                displayMessage("");

                //Get the user input
                std::getline (std::cin, userInput);

                //Find the input command
                if(strcmp(userInput.c_str(), "LIST") == 0) {
                        // LIST files in the server
                        listFiles(sockpi);
                } else if(strcmp(userInput.c_str(), "RETR") == 0) {
                        //GET the file

                        displayUserMessage("Enter the name of the file(case sensitive):");
                        //Get the user input
                        std::getline (std::cin, userInput);

                        // RETR a file from a server
                        getFile(sockpi, userInput);
                } else if(strcmp(userInput.c_str(), "QUIT") == 0) {
                        // QUIT the connection and leave the server
                        quitConnection(sockpi);
                        ftpActive = false;
                } else {
                        displayMessage("Unrecognized Command");
                }
        }

        //Prompt the user of a nice goodbye
        displayUserMessage("Goodbye! Have a nice day!");
        exit(0);
}
