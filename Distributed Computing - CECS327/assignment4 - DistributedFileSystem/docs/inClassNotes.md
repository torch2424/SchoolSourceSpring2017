# API Methods

read(filename)
write(filename, stream)

# Notes

* Replica one of each file. The file system will be done as a chord:
````
  // i is the identifier of the file
  ri = md5(filename + "i");
````

* If Client One Reads, and client two reads from the server. If client two writes, and then client one writes without reading again, there will be a conflict.

* All Clients holding replicas must agree upon a commit, or else the commit should be aborted.

* In the above example, once the client one writes without reading. The server should return a simple error saying there was a conflict. Please fix.

* Due Date: Last Thursday of class. (~ May 11th).
