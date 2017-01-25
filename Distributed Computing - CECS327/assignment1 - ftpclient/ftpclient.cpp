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
#include <stdlib.h>
#include <string.h>    //strlen
#include <sys/socket.h>    //socket
#include <arpa/inet.h> //inet_addr
#include <netinet/in.h>
#include <sys/types.h>
#include <unistd.h>
#include <netdb.h>
#include <vector>

#define BUFFER_LENGTH 2048

//Helper function for sleeping
int ftpSleep() {
        //Sleep for 500 milliseconds (500000 microseconds)
        usleep(500000);
        return 1;
}

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

int request(int sock, std::string message)
{
        return send(sock, message.c_str(), message.size(), 0);
}

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

std::string requestReply(int s, std::string message)
{
        if (request(s, message) > 0)
        {
                return reply(s);
        }
        return "";
}

std::string passiveRequestReply(int socket, std::string message) {

        //First, set to passive mode
        std::string passiveReply = requestReply(socket, "PASV\r\n");
        ftpSleep();

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
        printf("Connecting to %s:%d\n", connectionHost.c_str(), connectionPort);
        int newConnection = createConnection(connectionHost, connectionPort);
        ftpSleep();

        //Ask the pi socket for a response
        requestReply(socket, message);
        ftpSleep();

        //Pass to the request reply function with the message and new connection
        std::string fileList = reply(newConnection);
        ftpSleep();

        //Close the Connection to the new conneciton
        close(newConnection);

        return fileList;
}


int main(int argc, char *argv[])
{
        int sockpi;
        std::string strReply;

        // TODO  arg[1] can be a dns or an IP address.
        if (argc > 2)
                sockpi = createConnection(argv[1], atoi(argv[2]));
        if (argc == 2)
                sockpi = createConnection(argv[1], 21);
        else
                sockpi = createConnection("130.179.16.134", 21);
        strReply = reply(sockpi);
        printf("%s\n", strReply.c_str());

        printf("***Logging into server***\n");
        strReply = requestReply(sockpi, "USER anonymous\r\n");
        // TODO parse srtReply to obtain the status.
        // Let the system act according to the status and display
        // friendly message to the user
        // You can see the ouput using std::cout << strReply  << std::endl;
        printf("%s\n", strReply.c_str());
        strReply = requestReply(sockpi, "PASS asa@asas.com\r\n");
        printf("%s\n", strReply.c_str());
        ftpSleep();

        // TODO implement PASV, LIST, RETR.
        // Hint: implement a function that set the SP in passive mode and accept commands.
        printf("***Listing Files, this may take a while...***\n");
        strReply = passiveRequestReply(sockpi, "LIST\r\n");
        printf("%s\n", strReply.c_str());
        ftpSleep();

        printf("***Retrieving welcome.msg, this may take a while...***\n");
        strReply = passiveRequestReply(sockpi, "RETR welcome.msg\r\n");
        printf("%s\n", strReply.c_str());
        ftpSleep();

        printf("***Goodbye! Have a nice day!***\n");
        return 0;
        exit(0);
}
