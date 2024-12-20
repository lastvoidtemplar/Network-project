# Network programming project

## Sumary

The project is simple client-server application that parallel sorts an array of 64-bits interger.
The client and the server are written in Java(openjdk-23) and they are tested on Linux(Debian 12).

## Protocol

The protocol is simple binary protocol with request-response.

### Resquest

- first 4 bytes are for 32-bit signed integer that says how many threads the server must use.
- next 4 bytes are for 32-bit signed integer that specifies the lenght of the input array.
- every next 8 bytes are for 64-bit signed integer which correspondence to a number in the input array.

### Response

- first 1 bytes is for a status code of the response.
  - 0 - Ok
  - 1 - Invalid threads number
  - 2 - Invalid lenght of the array
  - 3 - Server error
- if the status code is different from Ok, next bytes are an error message.
- if the status code is Ok, next 4 bytes are for 32-bit signed integer that specifies teh lenght of the sorted output array.
- if the status code is Ok, every next 8 bytes after the lenght are for 64-bits signed integer which correspondence to a interger in the sorted output array.

## Server

### Input/Output

The Server use the TCP protocol for communication. It implements the selector pattern to handle multiple clients on single thread.

### Sorting algoritm

The sorting algoritm which the server uses is known as *Parallel Quick Merge Sort*. It divide the array into pieces, until it hits the threshold where it uses quick sort + inserttion sort, and then merges the pieces. It uses worker pool managed by blocking queue to distribute evenly the workload.

## Client

The client is simple CLI application which has 4 options.

- **Option 1** is running single connection. It takes the request parameters from a text file(example *request.txt*) and save the response in another text file(example *response.txt*).
- **Option 2** is running multiple connection in parallel. The connections are using the same request parameters(the same as option 1) and displays the min/average/max times.
- **Options 3** is running multiple connections in parallel. Every connection takes the reqest parameters from different text files(example *request.txt*, *request1.txt* ...) and save the response in different text files(example *response.txt*, *response1.txt*, ...).
- **Option 4** is running multiple connections sequentially to avoid the problem with the *noisy neighbours*. An random array is generated and is useds as input array for every connection. The only difference between the request is the number of threads. After all request are completed, the client displays the time for each request.

## Examples