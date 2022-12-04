# Route Reply (RREP)

```
    0           1           2           3        
    0 1 2 3 4 5 0 1 2 3 4 5 0 1 2 3 4 5 0 1 2 3 4 5
   +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
   |Type       |           Lifetime                |
   +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
   |  Destination Address          | Dest Sequence |
   +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
   |  Originator  Address          |  Hop Count    |
   +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
```

| Field                       | Value                                                                                                    |
| --------------------------- | -------------------------------------------------------------------------------------------------------- |
| Type                        | 2                                                                                                        |
| Lifetime                    | The time in milliseconds for which nodes receiving the RREP consider the route to be valid.              |
| Destination Address         | The address of the destination for which a route is desired.                                             |
| Destination Sequence Number | The latest sequence number received in the past by the originator for any route towards the destination. |
| Originator Address          | The address of the node which originated the Route Request.                                              |
| Hop Count                   | The number of hops from the Originator Address to the node handling the request.                         |
