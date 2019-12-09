#include "server.hpp"
#include <stdlib.h>
#include <sys/socket.h>
#include <sys/time.h>
#include <netinet/in.h>
#include <arpa/inet.h>
#include <unistd.h>
#include <pthread.h>
#include <chrono>
#include <thread>
#include <iomanip>
#include <queue>
#include <fstream>
#include <map>
#include <iterator>
//curl files
#include <iostream>
#include <string>
#include <curl/curl.h>

extern "C" {
#include "detector.h"
}
#define MESSAGE_ECHO 0
#define EDGE 1
#define IMAGE_DETECT 2
#define BOUNDARY 3
#define PORT 52727
#define PACKET_SIZE 80000
#define RES_SIZE 512
#define TRAIN

using namespace std;
using namespace cv;

struct sockaddr_in localAddr;
struct sockaddr_in remoteAddr;
struct sockaddr_in remoteAddr1;
struct sockaddr_in frontAddr;
socklen_t addrlen = sizeof(remoteAddr);

queue<frameBuffer> frames;
queue<resBuffer> resultss;
int recognizedMarkerID;

map<string, int> mapOfDevices;

double wallclock (void)
{
  struct timeval tv;
  gettimeofday(&tv, NULL);
  return (double)tv.tv_sec*1000 + (double)tv.tv_usec * 0.001;
}
double what_time_is_it_now()
{
    struct timeval time;
    if (gettimeofday(&time,NULL)){
        return 0;
    }
    return (double)time.tv_sec*1000 + (double)time.tv_usec * 0.001;
}

static size_t WriteCallback(void *contents, size_t size, size_t nmemb, void *userp)
{
    ((string*)userp)->append((char*)contents, size * nmemb);
    return size * nmemb;
}

int registerToEdge(int header_index, int device_index, const char* url, const char* data, int variable, int optional = 0){
//void registerToEdge(){
// header_index = 0; #POST

    CURL *curl;
    CURLcode res;
    string readBuffer;
    curl = curl_easy_init();
    if(curl) {
        curl_easy_setopt(curl, CURLOPT_URL, url);
        char buffertmp[500];
        if(optional==0) sprintf(buffertmp, data, device_index, variable);
        else sprintf(buffertmp, data, device_index, variable, optional);
        //cout << buffertmp << endl;
        struct curl_slist *headers=NULL;
        if (header_index == 0) {
            headers = curl_slist_append(headers, "Content-Type: application/json");
            //headers = curl_slist_append(headers, "charsets: utf-8"); 
            curl_easy_setopt(curl, CURLOPT_CUSTOMREQUEST, "POST");}
        curl_easy_setopt(curl, CURLOPT_HTTPHEADER, headers);
        curl_easy_setopt(curl, CURLOPT_COPYPOSTFIELDS, buffertmp);
        //curl_easy_setopt(curl, CURLOPT_POSTFIELDSIZE, data.length());
        curl_easy_setopt(curl, CURLOPT_POST, 1L);
        curl_easy_setopt(curl, CURLOPT_WRITEFUNCTION, WriteCallback);
        curl_easy_setopt(curl, CURLOPT_WRITEDATA, &readBuffer);
        res = curl_easy_perform(curl);
        curl_slist_free_all(headers);
        headers = NULL;
        curl_easy_cleanup(curl);
        //cout << readBuffer << endl;
        //memset(buffertmp,1,sizeof(buffertmp));
        for(int i = 0; i < sizeof(buffertmp); i++)
        {
            buffertmp[i] = rand();
        }
        string().swap(readBuffer);
        return 0;

  }

}


int getFromEdge(const char* url){

    CURL *curl;
    CURLcode res;
    string readBuffer;
    curl = curl_easy_init();
    if(curl) {
        curl_easy_setopt(curl, CURLOPT_URL, url);
        struct curl_slist *headers=NULL;
        headers = curl_slist_append(headers, "Content-Type: application/json");
        //curl_easy_setopt(curl, CURLOPT_CUSTOMREQUEST, "GET");
        curl_easy_setopt(curl, CURLOPT_HTTPHEADER, headers);
        curl_easy_setopt(curl, CURLOPT_FOLLOWLOCATION, 1L);
        curl_easy_setopt(curl, CURLOPT_WRITEFUNCTION, WriteCallback);
        curl_easy_setopt(curl, CURLOPT_WRITEDATA, &readBuffer);
        res = curl_easy_perform(curl);
        curl_slist_free_all(headers);
        //headers = NULL;
        curl_easy_cleanup(curl);
        cout << readBuffer << endl;
    
        string().swap(readBuffer);
        return 0;

  }

}

int fileToEdge(const char* url, ifstream& device_file){

    CURL *curl;
    CURLcode res;
    string readBuffer;
    string contents;
    if (device_file)
    {
	device_file.seekg(0, std::ios::end);
	contents.resize(device_file.tellg());
	device_file.seekg(0, std::ios::beg);
	device_file.read(&contents[0], contents.size());
	device_file.close();
    }

    struct curl_httppost *formpost = NULL;
    struct curl_httppost *lastptr = NULL;
    struct curl_slist *headerlist = NULL;
    static const char buf[] =  "Expect:";
    curl_global_init(CURL_GLOBAL_ALL);
    // set up the header
    curl_formadd(&formpost, &lastptr,
        CURLFORM_COPYNAME, "cache-control:",
        CURLFORM_COPYCONTENTS, "no-cache",
        CURLFORM_END);

    curl_formadd(&formpost, &lastptr,
        CURLFORM_COPYNAME, "content-type:",
        CURLFORM_COPYCONTENTS, "multipart/form-data",
        CURLFORM_END);

    curl_formadd(&formpost, &lastptr,
        CURLFORM_COPYNAME, "file", 
        CURLFORM_BUFFER, "data",
        CURLFORM_BUFFERPTR, contents.data(),
        CURLFORM_BUFFERLENGTH, contents.size(),
        CURLFORM_END);
    curl = curl_easy_init();
    headerlist = curl_slist_append(headerlist, buf);
    if(curl) {
        curl_easy_setopt(curl, CURLOPT_URL, url);
        curl_easy_setopt(curl, CURLOPT_HTTPPOST, formpost);
        //curl_easy_setopt(curl, CURLOPT_UPLOAD, 1L);
        //curl_easy_setopt(curl, CURLOPT_READDATA, &device_file);
        curl_easy_setopt(curl, CURLOPT_WRITEFUNCTION, WriteCallback);
        curl_easy_setopt(curl, CURLOPT_WRITEDATA, &readBuffer);
        res = curl_easy_perform(curl);
        curl_easy_cleanup(curl);
        curl_formfree(formpost);
        curl_slist_free_all(headerlist);
        //cout << readBuffer << endl;
        return 0;

  }

}

void *ThreadReceiverFunction(void *socket) {
    cout<<"Receiver Thread Created!"<<endl;
    char tmp[4];
    char Tmp[8];
    char buffer[PACKET_SIZE];
    int sock = *((int*)socket);
    cout << " sock is  " << sock << "\n" ;
    int device_ind = 1; 
    int len =20;
    char str[len];
    char str_front[len];
    double time_register_start;
    double time_to_register;
    double time_receivepic;
    double imageDelay;

    ofstream output_receive ("test_receive.txt");
    ofstream output_delay ("test_imagedelay.txt");
    while (1) {
        memset(buffer, 0, sizeof(buffer));
        recvfrom(sock, buffer, PACKET_SIZE, 0, (struct sockaddr *)&frontAddr, &addrlen);
        time_receivepic = what_time_is_it_now();
        frameBuffer curFrame;    
        memcpy(tmp, buffer, 4);
        curFrame.frmID = *(int*)tmp;        
        memcpy(tmp, &(buffer[4]), 4);
        curFrame.dataType = *(int*)tmp;
        FILE *fd;
        if(curFrame.dataType == MESSAGE_ECHO) {
            cout<<"echo message!"<<endl;
            charint echoID;
            echoID.i = curFrame.frmID;
            char echo[4];
            memcpy(echo, echoID.b, 4);

            inet_ntop(AF_INET, &(frontAddr.sin_addr), str_front, len);
            if (mapOfDevices.find(string(str_front)) != mapOfDevices.end()) {
                output_receive<<"receiving from an old " << (device_ind-1) << " device, whose ip is " << str_front << endl;
                cout<<"receiving from an old  " << (device_ind-1) << " device, whose ip is " << str_front << endl;
                continue;}
            cout<<"receiving from the " << device_ind << " device, whose ip is " << str_front << endl;
            //pair<map<int, string>::iterator,bool> ret;
            mapOfDevices.insert(pair<string, int>(string(str_front), device_ind));
            device_ind += 1; 
            printf("device_ind now has increased to %d\n", device_ind); 
            map<string, int>::iterator it_device = mapOfDevices.begin();
            while(it_device != mapOfDevices.end()){
                output_receive << it_device->first << " "  << it_device->second << "\n" ;
                cout << it_device->first << " "  << it_device->second << "\n" ;
                it_device ++;}
            continue;

        }
        //memcpy(Tmp, &(buffer[8]), 8);
        //curFrame.timeCaptured = *(double*)Tmp;
        //memcpy(Tmp, &(buffer[16]), 8);
        //curFrame.timeSend = *(double*)Tmp;
        memcpy(tmp, &(buffer[8]), 4);
        curFrame.bufferSize = *(int*)tmp;
        if (curFrame.bufferSize==0) { continue;}
        //imageDelay = time_receivepic - curFrame.timeCaptured;
         
        //output_receive << "receive frameID : " << curFrame.frmID << ", at time : " <<  time_receivepic << ", sent out from vehicle at time: " << curFrame.timeCaptured <<  ", has size: "<< curFrame.bufferSize << ", transmission delay: '" << time_receivepic - curFrame.timeCaptured << "' milliseconds" << endl;
        //output_delay << imageDelay << endl;
        //cout<<"frame "<<curFrame.frmID<<" received, filesize: "<<curFrame.bufferSize << endl;
        curFrame.buffer = new char[curFrame.bufferSize];
        memset(curFrame.buffer, 0, curFrame.bufferSize);
        memcpy(curFrame.buffer, &(buffer[12]), curFrame.bufferSize);

        frames.push(curFrame);
        //delete curFrame.buffer;
        //}
    }
    output_receive.close();
    output_delay.close();

}

void *ThreadSenderFunction(void *socket) {
    cout << "Sender Thread Created!" << endl;
    char buffer[RES_SIZE];
    int sock = *((int*)socket);
    int len =20;
    char str_buffer[len];
    ofstream output_send ("test_send.txt");

    while (1) {
        if(resultss.empty()) {
            this_thread::sleep_for(chrono::milliseconds(1));
            continue;
        }

        resBuffer curRes = resultss.front();
        resultss.pop();
    
        memset(buffer, 0, sizeof(buffer));
        memcpy(buffer, curRes.resID.b, 4);
        memcpy(&(buffer[4]), curRes.resType.b, 4);
        //memcpy(&(buffer[8]), curRes.resLatitude.b, 8);
        // pengzhou:currently, use longtitue to transfer the timestamp of result sent out by ES.
        // it shoud be written as timeSend etc., however keeping current state to save efforts... 
        //curRes.resLongtitude.d = what_time_is_it_now();
        //memcpy(&(buffer[16]), curRes.resLongtitude.b, 8);
        memcpy(&(buffer[8]), curRes.markerNum.b, 4);
        if(curRes.markerNum.i != 0)
            memcpy(&(buffer[12]), curRes.buffer, 100 * curRes.markerNum.i);
        map<string, int>::iterator it_device = mapOfDevices.begin();
        while(it_device != mapOfDevices.end()){
            memset((char*)&remoteAddr, 0, sizeof(remoteAddr));
            remoteAddr.sin_family = AF_INET;
            remoteAddr.sin_addr.s_addr = inet_addr((it_device->first).c_str());
            remoteAddr.sin_port = htons(51919);
            //output_send << "sending to the " << it_device->second<< " device, whose ip is "<< it_device->first << endl ;
            //cout << "sending to the " << it_device->second<< " device, whose ip is "<< it_device->first << endl ;
            sendto(sock, buffer, sizeof(buffer), 0, (struct sockaddr *)&frontAddr, addrlen);
            //output_send << "send_result of frameID of: " << curRes.resID.i << " sent by observer at time: " << std::fixed << std::setprecision(15) << curRes.resLongtitude.d << " whose size is: " << sizeof(buffer) << endl;
            //cout << "send_result of frameID of: " << curRes.resID.i << " sent by observer at time: " << curRes.resLongtitude.d << " whose size is: " << sizeof(buffer) << endl;
            it_device++;} 
            //cout<<"frame "<<curRes.resID.i<<" res sent, "<<"marker#: "<<curRes.markerNum.i;
            //cout<<" at "<<setprecision(15)<<wallclock()<<endl<<endl;
        //memset(curRes.buffer,1,sizeof(curRes.buffer)); 
        //memset(buffer,1,sizeof(buffer)); 
        //memset(str_buffer,1,sizeof(str_buffer)); 
        /*for(int i = 0; i < sizeof(curRes.buffer); i++)
        {
            curRes.buffer[i] = rand();
        }
        for(int i = 0; i < sizeof(buffer); i++)
        {
            buffer[i] = rand();
        }
        for(int i = 0; i < sizeof(str_buffer); i++)
        {
            str_buffer[i] = rand();
        }*/
    }    
    output_send.close();
}

void *ThreadProcessFunction(void *param) {
    cout<<"Process Thread Created!"<<endl;
    recognizedMarker marker;
    bool objectDetected = false;
    result* res;
    double time_process_start;
    double time_process;

    ofstream output_process("test_process.txt");
    ofstream output_process_delay("test_processdelay.txt");
    load_params();

    while (1) {
        if(frames.empty()) {
            this_thread::sleep_for(chrono::milliseconds(1));
            continue;
        }

        frameBuffer curFrame = frames.front();
        frames.pop();

        int frmID = curFrame.frmID;
        int frmDataType = curFrame.dataType;
        // pengzhou:currently, device send the timestamp of image instead of geolocation, therefore we use 0 to temporarily for location.
        //double latitude = curFrame.latitude;
        //double longtitude = curFrame.longtitude;
        //double latitude = 0;
        //double longtitude = 0;
        int frmSize = curFrame.bufferSize;
        char* frmdata = curFrame.buffer;
        
        if(frmDataType == IMAGE_DETECT) {
            // last change
            ofstream file("received.jpg", ios::out | ios::binary);
            //char picname[20];
            //sprintf(picname, "%d_received.jpg", frmID);
            //ofstream file(picname, ios::out | ios::binary);
            if(file.is_open()) {
                file.write(frmdata, frmSize);
                file.close();

                time_process_start = what_time_is_it_now();
                //res = detect(frmID);
                res = detect();
                output_process << "time_process_pic of frameid of: " << frmID << " takes: '" <<  what_time_is_it_now() - time_process_start<< "' milliseconds" << endl;
                //cout << "time_process_pic of frameid of: " << frmID << " takes: " <<  what_time_is_it_now() - time_process_start << " milliseconds" << endl;
                objectDetected = true;
                //output_process << "resultss: " << res->num << endl;
                //cout << "resultss: " << res->num << endl;
            } 
        } else if(frmDataType == EDGE) {
             cout << frmdata << endl;
             continue;
        }
        for(int i = 0; i < sizeof(curFrame.buffer); i++)
        {
            curFrame.buffer[i] = rand();
        }

        for(int i = 0; i < sizeof(frmdata); i++)
        {
            frmdata[i] = rand();
        }
        //memset(curFrame.buffer,1,sizeof(curFrame.buffer));
        //memset(frmdata,1,sizeof(frmdata));
        int personNum = 0;
        int carNum = 0;

        resBuffer curRes;
        if(objectDetected) {
            charfloat p;
            charint ci;
            curRes.resID.i = frmID;
            //curRes.resLatitude.d = latitude;
            //curRes.resLongtitude.d = curFrame.timeSend;
            curRes.resType.i = BOUNDARY;
            if(res->num <= 5)
                curRes.markerNum.i = res->num;
            else
                curRes.markerNum.i = 5;
            curRes.buffer = new char[100 * curRes.markerNum.i];

            for(int i = 0; i < curRes.markerNum.i; i++) {
                int pointer = 100 * i;
                struct object *cur = &(res->objects[i]); 

                p.f = cur->prob;
                memcpy(&(curRes.buffer[pointer]), p.b, 4);
                pointer += 4;
                ci.i = cur->left;
                memcpy(&(curRes.buffer[pointer]), ci.b, 4);
                pointer += 4;
                ci.i = cur->right;
                memcpy(&(curRes.buffer[pointer]), ci.b, 4);
                pointer += 4;
                ci.i = cur->top;
                memcpy(&(curRes.buffer[pointer]), ci.b, 4);
                pointer += 4;
                ci.i = cur->bot;
                memcpy(&(curRes.buffer[pointer]), ci.b, 4);
                pointer += 4;

                memcpy(&(curRes.buffer[pointer]), cur->name, strlen(cur->name));
                pointer += strlen(cur->name);
                curRes.buffer[pointer] = '.';
            }
        }
        else {
            curRes.resID.i = frmID;
            curRes.markerNum.i = 0;
        }
        //free(res->objects);
        delete res->objects;
        res->objects = NULL;

        resultss.push(curRes);
    }
    output_process.close();
    output_process_delay.close();
}

int main(int argc, char *argv[])
{
    pthread_t senderThread, receiverThread, processThread, annotationThread;
    int ret1, ret2, ret3, ret4;
    //char buffer[PACKET_SIZE];
    char fileid[4];
    int status = 0;
    int sockTCP, sockUDP;
   

    memset((char*)&localAddr, 0, sizeof(localAddr));
    localAddr.sin_family = AF_INET;
    localAddr.sin_addr.s_addr = htonl(INADDR_ANY);
    localAddr.sin_port = htons(PORT);

    memset((char*)&remoteAddr, 0, sizeof(remoteAddr));
    remoteAddr.sin_family = AF_INET;
    //remoteAddr.sin_addr.s_addr = inet_addr("192.168.12.42");
    remoteAddr.sin_addr.s_addr = inet_addr("INADDR_ANY");
    remoteAddr.sin_port = htons(51919);

    if((sockUDP = socket(AF_INET, SOCK_DGRAM, 0)) < 0) {
        cout<<"ERROR opening udp socket"<<endl;
        exit(1);
    }
    if(bind(sockUDP, (struct sockaddr *)&localAddr, sizeof(localAddr)) < 0) {
        cout<<"ERROR on udp binding"<<endl;
        exit(1);
    }
    cout << endl << "========server started, waiting for clients==========" << endl;

    //ret4 = pthread_create(&receiverThread_echo, NULL, ThreadReceiverFunction_echo, (void *)&sockUDP);
    ret1 = pthread_create(&receiverThread, NULL, ThreadReceiverFunction, (void *)&sockUDP);
    ret2 = pthread_create(&processThread, NULL, ThreadProcessFunction, NULL);
    ret3 = pthread_create(&senderThread, NULL, ThreadSenderFunction, (void *)&sockUDP);

    //pthread_join(receiverThread_echo, NULL);
    pthread_join(receiverThread, NULL);
    pthread_join(processThread, NULL);
    pthread_join(senderThread, NULL);

    return 0;
}
