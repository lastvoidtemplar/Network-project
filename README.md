# Network programming project

## Summary

The project is a simple client-server application that parallel sorts an array of 64-bits interger.
The client and the server are written in Java(openjdk-23) and they are tested on Linux(Debian 12).
Github repository [here](https://github.com/lastvoidtemplar/Network-project).
## Protocol

The protocol is a simple binary protocol with request-response.

### Request

- the first 4 bytes are for a 32-bit signed integer that says how many threads the server must use.
- the next 4 bytes are for a 32-bit signed integer that specifies the length of the input array.
- every next 8 bytes are for a 64-bit signed integer which corresponds to a number in the input array.

### Response

- first 1 byte is for a status code of the response.
  - 0 - Ok
  - 1 - Invalid threads number
  - 2 - Invalid length of the array
  - 3 - Server error
- if the status code is different from Ok, the next bytes are an error message.
- if the status code is Ok, the next 4 bytes are for a 32-bit signed integer that specifies the length of the sorted output array.
- if the status code is Ok, every next 8 bytes after the length are for a 64-bit signed integer which corresponds to a interger in the sorted output array.

## Server

### Input/Output

The server uses the TCP protocol for communication. It implements the selector pattern to handle multiple clients on a single thread.

### Sorting algoritm

The sorting algoritm which the server uses is known as *Parallel Quick Merge Sort*. It divides the array into pieces, until it hits the threshold where it uses quick sort + insertion sort, and then merges the pieces. It uses a worker pool managed by blocking queue to distribute the workload evenly.

## Client

The client is a simple CLI application which has 4 options.

- **Option 1** is running on single connection. It takes the request parameters from a text file(example *request.txt*) and saves the response in another text file(example *response.txt*).
- **Option 2** is running on multiple connections in parallel. The connections are using the same request parameters(the same as option 1) and the client displays the min/average/max times.
- **Option 3** is running on multiple connections in parallel. Every connection takes the request parameters from different text file(example *request.txt*, *request1.txt* ...) and saves the response in a different text file(example *response.txt*, *response1.txt*, ...).
- **Option 4** is running on multiple connections sequentially to avoid the problem with the *noisy neighbours*. A random array is generated and is used as an input array for every connection. The only difference between the requests is the number of threads. After all request are completed, the client displays the time for each request.

## Examples

### Client examples

![option 1 example](https://raw.githubusercontent.com/lastvoidtemplar/Network-project/refs/heads/main/examples/option1.png)

![option 2 example](https://raw.githubusercontent.com/lastvoidtemplar/Network-project/refs/heads/main/examples/option2.png)

![option 4 example](https://raw.githubusercontent.com/lastvoidtemplar/Network-project/refs/heads/main/examples/option4.png)

### Server logs

![server log 1 example](https://raw.githubusercontent.com/lastvoidtemplar/Network-project/refs/heads/main/examples/server%20log1.png)

![server log 2 example](https://raw.githubusercontent.com/lastvoidtemplar/Network-project/refs/heads/main/examples/server%20log2.png)

![server log 3 example](https://raw.githubusercontent.com/lastvoidtemplar/Network-project/refs/heads/main/examples/server%20log3.png)