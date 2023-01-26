#include <enet/enet.h>

#include<cstdlib>
#include<cstdio>
#include<cstring>

#define SERVERADDRESS "172.21.209.103"

using namespace std;


int main(int argc, char ** argv)
{

	if (enet_initialize() != 0)
	{
		printf("An error occurred while initializing ENet.\n");
		exit(EXIT_FAILURE);
	}

	for (int i; i < 10; i++) {

		// ENetHost* client = createClient();

		ENetHost * client;
    	client = enet_host_create(NULL, 1, 2, 0, 0);

		ENetAddress address;
		ENetEvent event;
		ENetPeer *peer;

		/* Connect to some.server.net:1234. */
		enet_address_set_host(&address, SERVERADDRESS);
		address.port = 51005;

		/* Initiate the connection, allocating the two channels 0 and 1. */
		peer = enet_host_connect(client, &address, 2, 0);

		if (peer == NULL)
		{
			printf("No available peers for initiating an ENet connection.\n");
			system("pause");
			exit(EXIT_FAILURE);
		}
		/* Wait up to 5 seconds for the connection attempt to succeed. */
		if (enet_host_service(client, &event, 500) > 0 && event.type == ENET_EVENT_TYPE_CONNECT)
		{
			printf("Connection to %s:1234 succeeded.\n", SERVERADDRESS);
		}
		else
		{
			/* Either the 5 seconds are up or a disconnect event was */
			/* received. Reset the peer in the event the 5 seconds   */
			/* had run out without any significant event.            */
			enet_peer_reset(peer);
			puts("Connection to some.server.net:1234 failed.");
		}

		/* Create a reliable packet of size 7 containing "packet\0" */

		char buffer[200000];
		memset(buffer, 0, sizeof(buffer));

		char tmp_string[] = "asj";
		memcpy(&(buffer[199997]), tmp_string, 3);

		ENetPacket *packet = enet_packet_create(buffer, 200000+1, ENET_PACKET_FLAG_UNRELIABLE_FRAGMENT);
		
		/* Send the packet to the peer over channel id 0. */
		/* One could also broadcast the packet by         */
		/* enet_host_broadcast (host, 0, packet);         */
		enet_peer_send(peer, 0, packet);

		printf("sent data\n");
		
		// /* Send the packet to the peer over channel id 0. */
		// /* One could also broadcast the packet by         */
		// /* enet_host_broadcast (host, 0, packet);         */
		// enet_peer_send(peer, 0, packet);

		/* One could just use enet_host_service() instead. */
		enet_host_flush(client);

		// enet_peer_disconnect(peer, 0);
		///* Allow up to 3 seconds for the disconnect to succeed
		//* and drop any packets received packets.
		//*/
		while (enet_host_service(client, &event, 1) > 0)
		{
			switch (event.type)
			{
			case ENET_EVENT_TYPE_RECEIVE:
				enet_packet_destroy(event.packet);
				break;
			case ENET_EVENT_TYPE_DISCONNECT:
				puts("Disconnection succeeded.");
				break;
			}
		}
		///* We've arrived here, so the disconnect attempt didn't */
		///* succeed yet.  Force the connection down.             */
		enet_peer_reset(peer);

		// enet_host_destroy(client);
	}

	atexit(enet_deinitialize);
	system("pause");
    
}