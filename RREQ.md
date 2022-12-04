# Route Request (RREQ)

```
    0           1           2           3        
    0 1 2 3 4 5 0 1 2 3 4 5 0 1 2 3 4 5 0 1 2 3 4 5
   +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
   |Type       | Flags     |Hop Count  | Request ID|
   +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
   |  Destination Address          | Dest Sequence |
   +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
   |  Originator  Address          |  Ori Sequence |
   +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
```

| Field                       | Value                                                                                                                     |
| --------------------------- | ------------------------------------------------------------------------------------------------------------------------- |
| Type                        | 1                                                                                                                         |
| Flags                       | Reserved for flags.                                                                                                       |
| Hop Count                   | The number of hops from the Originator Address to the node handling the request.                                          |
| RREQ ID                     | A sequence number uniquely identifying the particular RREQ when taken in conjunction with the originating node's address. |
| Destination Address         | The address of the destination for which a route is desired.                                                              |
| Destination Sequence Number | The latest sequence number received in the past by the originator for any route towards the destination.                  |
| Originator Address          | The address of the node which originated the Route Request.                                                               |
| Originator Sequence Number  | The current sequence number to be used in the route entry pointing towards the originator of the route request.           |
