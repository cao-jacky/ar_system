#include "client.hpp"

#include <sys/socket.h>
#include <arpa/inet.h>

using namespace std;
using namespace cv;
using namespace std::chrono;

vector<uchar> intToBytes(int paramInt) {
    vector<uchar> arrayOfByte(4);
    for (int i = 0; i < 4; i++)
        arrayOfByte[3 - i] = (paramInt >> (i * 8));
    return arrayOfByte;
}

vector<uchar> double_to_byte(double param_double) {
    union {
        double double_value;
        uchar bytes[8];
    } dtbc;

    dtbc.double_value = param_double;
    vector<uchar> doubles_array_of_byte(8);
    int i;
    for (i=0; i<8; i++) 
        doubles_array_of_byte[7 - i] = dtbc.bytes[i];
    return doubles_array_of_byte;
}

vector <String> process_files(vector<String> filenames) {
    // struct sockaddr_in server_details;
    // int sock;

    // // defining server details
    // memset(&server_details, 0, sizeof(server_details));
    // server_details.sin_family = AF_INET;
    // server_details.sin_addr   = inet_addr("10.42.0.1");
    // server_details.sin_port   = htons(52727);

    // read through the folder of image files 
    glob("images/*.jpg", filenames);
    for (size_t i=0; i<filenames.size(); i++) {
        cout<<"Currently dealing with image which has path: "<<filenames[i]<<endl;
        // image is read by OpenCV
        Mat curr_image = imread(filenames[i], IMREAD_GRAYSCALE);

        // encode image into bytes and store into a buffer
        vector<uchar> image_buffer;
        imencode(".jpg", curr_image, image_buffer);
        int im_buff_size = image_buffer.size();

        // create byte array which will be sent as a packet
        vector<uchar> packet_content;
        packet_content.reserve(28+im_buff_size);

        // frame ID defined by the current image being processed
        int frame_id = i;
        vector<uchar> bytes_fid = intToBytes(frame_id);
        packet_content.insert(packet_content.end(), bytes_fid.begin(), 
            bytes_fid.end());

        // data type is set to 2 so server performs image recognition
        int image_detect = 2;
        vector<uchar> bytes_imdec = intToBytes(image_detect);
        packet_content.insert(packet_content.end(), bytes_imdec.begin(), 
            bytes_imdec.end());

        // time captured and time sent appear to be set to the same thing
        milliseconds curr_time = duration_cast< milliseconds >(
            system_clock::now().time_since_epoch());
        string curr_time_str = to_string(curr_time.count());

        double time_captured = stod(curr_time_str);
        vector<uchar> bytes_tc = double_to_byte(time_captured);
        packet_content.insert(packet_content.end(), bytes_tc.begin(), 
            bytes_tc.end());

        double time_sent = stod(curr_time_str);
        vector<uchar> bytes_ts = double_to_byte(time_sent);
        packet_content.insert(packet_content.end(), bytes_ts.begin(), 
            bytes_ts.end());      

        // size of frame when converted to bytes as an integer 
        int frame_size = im_buff_size;
        vector<uchar> bytes_fs = intToBytes(frame_size);
        packet_content.insert(packet_content.end(), bytes_fs.begin(), 
            bytes_fs.end());

        // append buffer of image to the end of packet_content
        packet_content.insert(packet_content.end(), image_buffer.begin(), 
            image_buffer.end());
        cout << packet_content.size() << endl;

        // I need to follow a similar structure as the original server sending code 

        // // send packet to the server process
        // sendto(sock, packet_content, sizeof(packet_content), 0, 
        //     (struct sockaddr *)&server_details, sizeof(server_details));

    }
    return filenames;
}

int main(int argc, char *argv[]) {
    cout << "Beginning processes to mock client application" << endl;

    vector<String> folder_file_names;
    process_files(folder_file_names);

    // int querysizefactor, nn_num, port;
    // if(argc < 4) {
    //     cout << "Usage: " << argv[0] << " size[s/m/l] NN#[1/2/3/4/5] port" << endl;
    //     return 1;
    // } else {
    //     if (argv[1][0] == 's') querysizefactor = 4;
    //     else if (argv[2][0] == 'm') querysizefactor = 2;
    //     else querysizefactor = 1;
    //     nn_num = argv[2][0] - '0';
    //     if (nn_num < 1 || nn_num > 5) nn_num = 5;
    //     port = strtol(argv[3], NULL, 10);
    // }
    // return 0;
}
