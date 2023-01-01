# LoRa-AODV

- [LoRa-AODV](#lora-aodv)
- [User Data (UD)](#user-data-ud)
- [Route Request (RREQ)](#route-request-rreq)
	- [Flags](#flags)
- [Route Reply (RREP)](#route-reply-rrep)
- [Algorithm](#algorithm)
	- [Create RREQ](#create-rreq)
	- [Processing and Forwarding RREQs](#processing-and-forwarding-rreqs)
	- [Generating RREPs](#generating-rreps)
	- [Processing RREPs](#processing-rreps)
	- [Create or update Routes](#create-or-update-routes)
	- [Using a Route](#using-a-route)
	- [Constants](#constants)

# User Data (UD)
```
 0           1           2           3
 0 1 2 3 4 5 0 1 2 3 4 5 0 1 2 3 4 5 0 1 2 3 4 5
+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
|    Type   |    Destination Adress         | UD|
+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
|                    User data                  |
+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
```
| Field | Value |
| --------------------------- | ------------------------------------------------------------------------------------------------------------------------- |
| Type | 0 |
| Destination Adress |The address of the destination for which user data is desired. |
| User data (UD) | Userdata, length 0-228|

# Route Request (RREQ)
```
 0           1           2           3
 0 1 2 3 4 5 0 1 2 3 4 5 0 1 2 3 4 5 0 1 2 3 4 5
+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
|    Type   |   Flags   |Hop Count  | Request ID|
+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
|      Destination Address      | Dest Sequence |
+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
|      Originator Address       | Ori Sequence  |
+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
```
| Field | Value |
| --------------------------- | ------------------------------------------------------------------------------------------------------------------------- |
| Type | 1 |
| Flags | Reserved for flags. |
| Hop Count | The number of hops from the Originator Address to the node handling the request. |
| RREQ ID | A sequence number uniquely identifying the particular RREQ when taken in conjunction with the originating node's address. |
| Destination Address | The address of the destination for which a route is desired. |
| Destination Sequence Number | The latest sequence number received in the past by the originator for any route towards the destination. |
| Originator Address | The address of the node which originated the Route Request. |
| Originator Sequence Number | The current sequence number to be used in the route entry pointing towards the originator of the route request. |

## Flags

- 1st bit: Unknown Sequence Number (U)
- remaining bits: Reserved (5 bits)

# Route Reply (RREP)  
```
  0           1           2           3
 0 1 2 3 4 5 0 1 2 3 4 5 0 1 2 3 4 5 0 1 2 3 4 5
+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
|Type       |              Lifetime             |
+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
|        Destination Address    | Dest Sequence |
+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
|        Originator Address     |   Hop Count   |
+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
```
| Field | Value |
| --------------------------- | -------------------------------------------------------------------------------------------------------- |
| Type | 2 |
| Lifetime | The time in milliseconds for which nodes receiving the RREP consider the route to be valid. |
| Destination Address | The address of the destination for which a route is desired. |
| Destination Sequence Number | The latest sequence number received in the past by the originator for any route towards the destination. |
| Originator Address | The address of the node which originated the Route Request. |
| Hop Count | The number of hops from the Originator Address to the node handling the request. |
# Algorithm
## Create RREQ 
In the case when no (valid) route is found:
1. Set the RREQ `Destination Sequence Number` to the most up-to-date value. Else set the `Unknown Sequence Number`-flag, if none is available.
2. Set the RREQ `Originator Sequence Number` to the own sequence number, after it has been incremented for this step.
3. Set the RREQ `Request ID` to increment of the last used `Request ID`.
4. Set the `Hop Count` value to 0.
5. Buffer the (`Request ID`, `Originator Address`)-pair for `PATH_DISCOVERY_TIME` milliseconds .
6. After sending, wait for a reply for `NET_TRAVERSAL_TIME` milliseconds.
	- If no answer arrives by this time, send another `RREQ`. Repeat `RREQ_RETRIES` times.
	- Each new retry has its own `Request ID`.
	- On each retry the waiting time backs off exponentially (`2^(#retries) * NET_TRAVERSAL_TIME`)
7. Data packages waiting for a route will be FIFO-buffered.
	- If the RREQ receives no answer after so many retries, the corresponding data package will be discarded.
	- A "Destination unreachable"-message should be delivered to the application.
## Processing and Forwarding RREQs
1. Create or update route to the previous hop without a valid `Sequence Number`. (see Create or update Routes)
2. Check if we have seen the RREQ before (compare `RREQ_ID` and `Originator Address`).
	1. Discard, if we have processed it before.
3. Increment the `Hop Count` on the RREQ.
4. Search for reverse route with matching `Originator Address`.
	1. If none exists, create a new one or update the current (see Create or update Routes).
5. Update the reverse route table entry (see Create or update Routes)
6. If we are the `Destination Address ` or do have a valid route, generate a RREP (see 6.4) else broadcast the RREQ.

## Generating RREPs
1. Use the `Originator Sequence Number` and `Destination Address` from RREQ in the new RREP.
2. Depending on:
	- If we are the Destination:
		1. Set `Hop Count` to 0.
		2. Set `Lifetime` to the default `MY_ROUTE_TIMEOUT`. (TODO see 10.)
		3. If the our own incremented sequence number (Sequence Number + 1) matches the `Destination Sequence Number`, persist the incremented value. Otherwise don't change it.
		4. Set the `Destination Sequence Number` in the RREP to own sequence number.
	- If we are sending route from routing table:
		1. Set `Destination Sequence Number` to value of sequence number from the forward route.
		2. Add the RREQ's sender to the `Precursor`-list of the forward route.
		3. Add `Next Hop` from the forward route to the `Precursor`-list of the route to the `Originator Adress` of the RREQ (reverse route).
		4. Set `Hop Count` in RREP to the value in the route to the `Destination Adress` of the RREP (forward route).
		5. Set `Lifetime` in RREP to the difference between (`forward Route Lifetime - Current Timestamp`).
3. Send the RREP to the `Originator Address` (using the reverse route).

## Processing RREPs
1. Search for a forward route to the previous hop (Sender of RREP).
	1. If none exists, create a new one, without `Valid Sequence Number`-flag.
2. Increment `Hop Count` in RREP.
3. Search for a forward route to the `Destination`.
	1. If none exists, create a new one or update the current (see Create or update Routes).
4. Send the RREP to the `Originator Address` using the reverse route.
5. Add the `Next Hop` node (target of our RREP) to the `Precursor`-list for the `Destination Address`.
6. Update the `Lifetime` for the reverse route to the max of (`CurrentLifetime, CURRENT_TIMESTAMP + ACTIVE_ROUTE_TIMEOUT`).
7. Add the `Originator Adress` of the RREP to the `Precursor`-list of the next hop towards the `Destination Adress` of the RREP.
## Create or update Routes
1. Look for existing table entry. If none exists, create a new one.
2. Update the Route in the following cases:
	- If the forward route's Sequence Number is marked invalid.
	- If the `Sequence Number` in the control package is larger than the forward route's value.
	- If the Sequence Numbers match, but the package's `Hop Count` is smaller than the forward route value.
3. If the Update/Create is triggered by an RREQ
	1. Set the `Sequence Number` to the max of (`Destination Sequence Number` of the route, `Originator Sequence Number` of the RREQ)
	2. Set the `Valid Sequence Number`-flag to tru or if none exists, set the `Valid Sequence Number`-flag to false.
	3. Set the `Next Hop` to the Sender of the RREQ.
	4. Set the `Hop Count` from the RREQ's `Hop Count`.
	5. Update the `Lifetime` by max of (`ExistingLifetime`, `MinimalLifetime`)
		- `MinimalLifetime = (current time + 2*NET_TRAVERSAL_TIME - 2*HopCount*NODE_TRAVERSAL_TIME)`
		- `ExistingLifetime` is the existing `Lifetime`-value in table.
4. If the Update/Create is triggered by an RREP
	1. Mark the route route as `Active`.
	2. Set the `Valid Sequence Number`-flag to tru or if none exists, set the `Valid Sequence Number`-flag to false.
	3. Set the `Next Hop` to the Sender of the RREP.
	4. Set the route `Hop Count` to the RREP value.
	5. Set the route `Lifetime` to `CURRENT_TIMESTAMP` + `Lifetime` of RREP, if not `Lifetime` is given, use `ACTIVE_ROUTE_TIMEOUT`
	6. Set the route `Destination Sequence Number` from RREP value or max of (`Destination Sequence Number` of the route,` Originator Sequence Number` of the RREP).
## Using a Route 
- Each time the route is used:
	- Search the forward route for the following three nodes: Source, Destination, Next Hop
	- Update the `Lifetime` values to be the value of max(`CurrentLifetime`, `CURRENT_TIMESTAMP + ACTIVE_ROUTE_TIMEOUT`), to update them.
	- Search the reverse route for the following three nodes: Source, Destination, Previous Hop
	- (repeat the previous update step)
## Constants
| Parameter Name       | Value                                                  |
| -------------------- | ------------------------------------------------------ |
| ACTIVE_ROUTE_TIMEOUT | 3,000 Milliseconds                                     |
| MY_ROUTE_TIMEOUT     | 2 \* ACTIVE_ROUTE_TIMEOUT                               |
| NET_TRAVERSAL_TIME   | 2 \* NODE_TRAVERSAL_TIME * NET_DIAMETER                 |
| NODE_TRAVERSAL_TIME  | 40 milliseconds                                        |
| PATH_DISCOVERY_TIME  | 2 \* NET_TRAVERSAL_TIME                                 |
| RREQ_RETRIES         | 2                                                      |
| NET_DIAMETER         | 35                                                      |
