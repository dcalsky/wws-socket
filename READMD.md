WWS-SOCKET (server side)
======

The Simplest TCP room IM system made by **Netty**. Inspired by [**@WWS**](https://github.com/Night12138).


## PackageT
**PackageT** is a TCP communication packet structure of WWS-SOCKET that includes two parts: **Header** and **Body**. 

Client use socket to create connection with **server** and then send packet based **PacketT** to it. Once server validates packet successfully, it will reply a packet back.

```
                              PackageT                                                                          
+---------------------------------------------------------------------+                                          
|       20 Bytes                          Unlimited                   |                                          
|+---------------------++--------------------------------------------+|                                          
||       Header        ||                    Body                    ||                                          
|+---------------------++--------------------------------------------+|                                          
+---------------------------------------------------------------------+
```

## Header
The header of packageT has fixed 20 bytes including 4 parts. `Message Type` has 2 bytes with uint16 (little-endian), `body Length` has 4 bytes with uint32 (little-endian), `Send Time` has 8 bytes with uint64 (little-endian) and `Package Hash` has 6 bytes. 

```
                                    PackageT Header                                               
+--------------------------------------------------------------------------------------+          
|    2 Bytes         4 Bytes                  8 Bytes                   6 Bytes        |          
| +------------++-----------------+ +-------------------------++---------------------+ |          
| |Message Type||   Body Length   | |        Send Time        ||     Package Hash    | |          
| +------------++-----------------+ +-------------------------++---------------------+ |          
+--------------------------------------------------------------------------------------+                                                                                   
```          

### Message Type

|Name|Uint16|Request|
|-----|-----|-----|
|PING_MSG|0|√|
|PONG_MSG|1|
|SET_NAME|2|√
|SET_NAME_SUCCESS|3|
|SET_NAME_FAILED|4|
|GET_ROOM_LIST|5|√
|ROOM_LIST|6|
|HOST_NEW_ROOM|7|√
|HOST_ROOM_SUCCESS|8|
|HOST_ROOM_FAILED|9|
|CONNECT_ROOM|10|√
|CONNECT_ROOM_SUCCESS|11|
|CONNECT_ROOM_FAILED|12|
|CHECK_ROOM_STATUS|13|√
|ROOM_STATUS|14|
|DISCONNECT_ROOM|15|√
|DISCONNECT_ROOM_SUCCESS|16|
|ROOM_MSG|17|
|SEND_MSG|18|√
|SEND_MSG_SUCCESS|19|
|SEND_MSG_FAILED|20|
|ROOM_CLEAN_OUT|21|          

### Body Length
The byte length of packet body, client/server should measure body before sending packet.

### Send Time
Unix millisecond timestamp when the packet is sent.

### Hash
Client generates a unique hash value before sending a packet and attach it to the header of packet. And then server should reply a packet with this hash in the header (except **ROOM_MSG** response type). 

## Body
The body structure of PackageT depends on *Message Type*, which means different message type has corresponding body.

## Ping
PING-PONG for connection test.

### Request
Message Type: **PING_MSG**

### Response
Message Type: **PONG_MSG**

|Body|Type|Description|
|-----|-----|-----|
|send time|Uint64|same as request packet|

## Set Name
Set name for current user

### Request
Message Type: **SET_NAME**

|Body|Type|Description|
|-----|-----|-----|
|name|Bytes|should be parsed to String|

### Response
Message Type: **SET_NAME_SUCCESS** or **SET_NAME_FAILED**

## Host a new room
Create a new room and set current user as host. User will join this room automatically once room created. 

### Request
Message Type: **HOST_NEW_ROOM**

### Response
Message Type: **HOST_ROOM_SUCCESS** or **HOST_ROOM_FAILED**

|Body|Type|Description|
|-----|-----|-----|
|room id|Uint32|Generated room id|

## Join a room
Let current user joins the specified room.

### Request
Message Type: **CONNECT_ROOM**

|Body|Type|Description|
|-----|-----|-----|
|room id|Uint32||

### Response
Message Type: **CONNECT_ROOM_SUCCESS** or **CONNECT_ROOM_FAILED**

## Leave a room
Let current user leaves the current room that user staying.

### Request
Message Type: **DISCONNECT_ROOM**

### Response
Message Type: **DISCONNECT_ROOM_SUCCESS**

## Get a room detail
Retrieve a room detail, including room id, room name and users information in the room.
 
### Request
Message Type: **CHECK_ROOM_STATUS**

|Body|Type|Description|
|-----|-----|-----|
|room id|Uint32||

### Response
Message Type: **ROOM_STATUS**

|Body|Type|Description|
|-----|-----|-----|
|room id|Uint32||
|number of online users|Uint32||
|room name|Bytes|length: 40 bytes|
|users info|Bytes||

#### User Info
|Fields|Type|Description|
|-----|-----|-----|
|joinTime|Uint64||
|user ip|Bytes|length: 20 bytes|
|user name|Bytes|length: 40 bytes|


## Get room list
List all available rooms information.

### Request
Message Type: **GET_ROOM_LIST**

### Response
Message Type: **ROOM_LIST**

|Body|Type|Description|
|-----|-----|-----|
|number of online rooms|Uint32||
|rooms info|Bytes||

#### Room Info
|Fields|Type|Description|
|-----|-----|-----|
|room id|Uint32||
|number of online users|Uint32||
|room name|Bytes|length: 40 bytes|

## Send Message
Send a message to current room of user. If user is host of this room, this message will be forwarded to all users in this room, while only host can receive this message.

### Request
Message Type: **SEND_MSG**

|Body|Type|Description|
|-----|-----|-----|
|message|Bytes||

### Response
Message Type: **SEND_MSG_SUCCESS** or **SEND_MSG_FAILED**

## Receiving room message
Client can receive room messages from the server passively.

### Response
Message Type: **ROOM_MSG**

|Body|Type|Description|
|-----|-----|-----|
|message length|Uint32||
|sender ip|Bytes|length: 20 bytes|
|sender name|Bytes|length: 40 bytes|
|message|Bytes|length: seeing message length|
