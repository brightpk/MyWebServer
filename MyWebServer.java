/*--------------------------------------------------------

1. Name / Date:
Siriporn Phakamad / 7 October 2018

2. Java version used, if not the official version for the class:
build 1.8.0_181

3. Precise command-line compilation examples / instructions:

> javac MyWebServer.java

4. Precise examples / instructions to run this program:

In separate shell windows:

> java MyWebServer

All acceptable commands are displayed on the various consoles.

This runs across machines, in which case you have to pass the IP address of
the server to the clients. For exmaple, if the server is running at
140.192.1.22 then you would type:

> java MyWebServer

5. List of files needed for running the program.
 a. checklist-mywebserver.html
 b. MyWebServer.java
 c. cat.html
 d. dog.txt
 e. addnums.html
 f. dynamic.html
 g. http-streams.txt
 h. serverlog.txt
 i. MyWebServer.class
 j. WeSerWorker.class

5. Notes:
My program is running fine. It returns what the browser requests.
It doesn't show MIME header on dynamic.html, addnums I believe.
I am not sure which ones don't know but I print all the MIME header
to MyServer to see the type and length.
----------------------------------------------------------*/

import java.io.*;  // Get the Input Output libraries
import java.net.*; // Get the Java networking libraries
import java.util.*; // Get all methods from java.util

public class MyWebServer {

  public static void main(String a[]) throws IOException {
    int maxQueue = 6; // maximun queue which is 6, won't take more than that.
    int PORT = 2540; // port number that we are listening and contacting with browser is 2540
    Socket sock;  // socket for beginning the connection between server and browser
    ServerSocket servsock = new ServerSocket(PORT, maxQueue);
    System.out.println("Siriporn Phakamad's WebServer is running at " + PORT + ".\n");
    while (true) {
      sock = servsock.accept();  // receive the signal from browser
      new WebSerWoker (sock).start(); // worker begins doing their jobs
    }
  }
}

/* This is class operate the fuctions that deal with http protocol
 * There're 3 functions in this class getMIMEType(), printFile(), getDirectory() and addNums()
 * The class basically connects to the browser and print out the outputs on the browser as well as on the MyWebServer
 * Some of the funstions, I got ideas from WebServerTips that Professor has posted on the instruction
 */
class WebSerWoker extends Thread {
  Socket WebServerSocket;              // socket that helps worker to listen to the web browser
  WebSerWoker (Socket s) { WebServerSocket = s; }

  /* The function run() starts when receive the connection from browser abd begins to work on
   * the request from broswer. First it reads the input from browser. In this case, server is localhost
   * The HTTP request in this assignemtn is GET method.
   * These function will prints out the outputs on the browser.
   * Also, we will capture the content-type and length for MIME and send in to browser
   */
  public void run() {
    PrintStream out = null;
    BufferedReader in = null;
    try {
      out = new PrintStream(WebServerSocket.getOutputStream());
      in = new BufferedReader(new InputStreamReader(WebServerSocket.getInputStream()));
      String messageFromBroswer;    // to store what browser requires
      String[] messageList = null;         // a list for getting each string in array
      String FileName = null;              // to store what file broswer is looking for
      String contentType = null;

      try {
        messageFromBroswer = in.readLine();     // read the request from the browser
        System.out.println(messageFromBroswer); // print what browser requests on MyWebServer

        /* if the request is not empty and also it starts with GET method
         * then we split the message by whitespace and store in array in order to get the name of the file
         * which is the second element (index 1)
         */
        if (!messageFromBroswer.isEmpty() && messageFromBroswer.startsWith("GET")) {
          messageList = messageFromBroswer.split(" "); // every time there's a space, and store in the array
          FileName = messageList[1]; // the second elememt is the name of the file
          if (FileName.equals("favicon.ico")) {
            System.out.println("No favicon.ico file");
          }
        }
        //
        // // print what file browser is looking for on MyWebServer
        // System.out.println("Searching for " + FileName.substring(1) + "...");
        System.out.flush(); // clear the output;

        /* call the getMIMEType() and inside this function
         * it'll also call another function depending on the type of content
         */
        getMIMEType(FileName, out);

      }

      // handle the error if it occurs
      catch (IOException err) {
        System.out.println("Server read error");
        err.printStackTrace();
      }
      WebServerSocket.close(); // close the connection after each request
    }

    // catch the bad error
    catch (IOException err) {
      System.out.println("Sorry, couldn't catch what your request is...");
    }
  }

  /* This function is for checking type so we know MIME content-type that broswer requestss
   * Got the idea from 150 line webserver that Professor posted on tips in MyWedserverTips
   * It returns the type of content that I will be using for the function called printFile
   * .txt, .java, is text/plain
   * .html, .fake-cgi and "/" is text/html
   * .jpg is image/jpeg
   */
  private void getMIMEType(String filename, PrintStream out) {
    String type = null;  // to store the type of content for MIME

    /* if GET .txt content type is context/plain and
     * then prints texts of .txt file into the browser
     */
    if (filename.endsWith(".txt")) {
      type = "text/plain";
      printFile(filename, out, type);

    /* if GET .java content type is also context/plain same as .txt
     * then print out the texts from files into the browser
     */
    } else if (filename.endsWith(".java")) {
      type = "text.plain";
      printFile(filename, out, type);

    /* if GET .html content type is context/html
     * then print out the html file into the browser
     */
    } else if (filename.endsWith(".html")) {
      type = "text/html";
      printFile(filename, out, type);

    /* if GET .fake-cgi content type is context/html
     * display html file which is the file call addnums.html into the browser
     */
    } else if (filename.contains(".fake-cgi")) {
      type = "text/html";
      addNums(filename, out, type);

    /* if GET .java content type is context/html
     * then get the directory for the browser
     */
    } else if (filename.endsWith("/")) {  // directory
      type = "text/html";
      getDirectory(filename, out, type);

    // if GET not of these file type, then treat it as a text/plain
    } else {
      type = "text.plain";
      printFile(filename, out, type);
    }
  }

  /* This function is called when browser requests to GET .txt, .java, .html
   * It basically get the file from the directory or subdirectory where the
   * MyWebserver is running from. If there's such a file, it will print out on the browser
   * If not, it'll catch the exception and it'll print cannot find a file that browser is
   * looking for.
   */
  static void printFile(String filename, PrintStream out, String contentType) {
    try {
      // Only need the name but not "/" for the filename
      if (filename.startsWith("/")) { filename = filename.substring(1); }

      /* These variables are for storing the file from directory
       * so it can be sent and be printed to the browser
       */
      InputStream file = new FileInputStream(filename); // read the file from the directory
      File thisFile = new File(filename); // get the filename from diretory

      /* send http header to browser after browser requests
       * these texts are printed on the browser to show MIME content
       * these can be found in the raw header on firefox
       * texts are content type, length date and current date
      */
      out.print("HTTP/1.1 200 OK\r\n");
      out.print("Content-Length: " + thisFile.length() + "\r\n");
      out.print("Content-Type: "+ contentType + "\r\n");
      out.print("\r\n\r\n");

      /* Make a buffer so the text in the file can be printed.
       * Should create a big buffer so it can store if a file happens to be big
       * store the file that browser looks for into buffer
       * and print text from file to the browser
       */

      byte[] buffer = new byte[5000];  // set size of buffer high because we don't know how big a file is
      while (file.available() > 0) {   // if there's a file
        out.write(buffer, 0, file.read(buffer)); // print whatever in the file on the browser
      }

      // print these on MyWebServer to see that the browser sees texts
      System.out.println("Opened file on the broswer: " + filename + "\n");
      System.out.println("HTTP/1.1 200 OK");
      System.out.println("Content-Length: " + thisFile.length());
      System.out.println("Content-Type: "+ contentType + "\n");


      out.flush();  // clear printstream
      file.close(); // close file so it won't be hanging around
    }
    catch (IOException err) {
      System.out.println("No file be found\n");  // print error message on MyWebServer
      out.print("Unable to find a file");      // print error text on browser when no suach a file
    }
  }

  /* Create a BufferedWriter so we can put all list of files or subdirectories that are under the directory
   * The directory in this case is where MyWebServer is running
   * This function is called when the browser ask to GET the directory or subdirectory
   * If browser asks for a directory, then it'll show the dynamic.html which contains all the files under the directory
   * I got some ideds from WebServerTips under the instruction page
   */
  static void getDirectory(String filename, PrintStream out, String contentType) {
    try {
      // These variables are for getting files name under the directory
      File dirFile = new File ("./" + filename + "/"); // create a new file that refer to  directory
      File[] listFiles = dirFile.listFiles(); // list of the files

      BufferedWriter dynamicBuffer = new BufferedWriter(new FileWriter("dynamic.html")); // create a new html that will be sent to the browser after modify it
      dynamicBuffer.write("<html><header><h1>Index of directory</h1></header><body><br>"); // header of this html file is index of directory
      dynamicBuffer.write("<a href=\"" + "http://localhost:2540" + "/\">" + "Parent Directory </a> <br>"); // first file is would be my localhost which is where I run MyWebServer

      String name; // variable to store the name of each file
      for (File f: listFiles) { // going through file in the array one by one
        name = f.getName();  // get the name of the file by calling getName()
        if (f.isFile()) {    // if it's a file such as .txt, .java, or .html
  	    	dynamicBuffer.write("<a href=" + name + ">" + name + "</a><br>"); // print a link by using its name on html
          System.out.println(name);
        } else if (f.isDirectory()) { // if it's a directory or subdirectory
          dynamicBuffer.write("<a href=" + name + "/>" + name + "</a><br>"); // print a link of that dir/sub. With this one we needs to add the "/" becasue it's dir/sub
          System.out.println(name);
        }
      }

      dynamicBuffer.write("</body></html>"); // close the body and html on html file
      dynamicBuffer.flush(); // clear out the buffer

      // These variables are for MIME header
      InputStream file = new FileInputStream("dynamic.html");  // create inputstream and store the dynamic.html file
      File filesDirFromhtml = new File("dynamic.html"); // create a file so we can print it later

      /* send http header to browser after browser requests
       * these texts are printed on the browser to show MIME content
       * these can be found in the raw header on firefox
       * texts are content type, length date and current date
      */
      out.print("HTTP/1.1 200 OK\r\n");
      out.print("Content-Length " + filesDirFromhtml.length() + "\r\n");
      out.print("Content-Type "+ contentType + "\r\n");
      out.print("\r\n\r\n");

      byte[] buffer = new byte[5000];  // set a larger size of buffer because we don't know how big a file is
      while (file.available() > 0) {
        out.write(buffer, 0, file.read(buffer)); // print whatever in the file on the browser
      }

      System.out.println("Sent directory to the browser: dynamic.html" + "\n");
      System.out.println("HTTP/1.1 200 OK");
      System.out.println("Content-Length: " + filesDirFromhtml.length());
      System.out.println("Content-Type: "+ contentType + "\n");
      out.flush(); // clear out the output for MIME
      file.close(); // close the file
      dynamicBuffer.close(); // close the buffer
    }

    // deal with the exception
    catch (IOException err) {
      System.out.println("Invalid openning a file/directory");  // print error message on MyWebServer
      out.print("Error looking up for directory");      // print error text on browser when no suach a file
    }
  }

  /* This function is called when the browser ask to GET fake-cgi
   * This will open the addnums.html page and it will calculate the sum of two numbers
   * It will also return .html file with the name of the person and th result
   */
  static void addNums(String filename, PrintStream out, String contentType) {
      Date currentDate = new Date();
      String personName = "";
      String temp1, temp2, page;
      String[] list;
      int firNum, secNum;
      int result;

      list = filename.split("\\?|&"); // split the string and put into an array
      page = list[0].substring(5);   // name of the file
      personName = list[1].substring(7); // name of the person
      temp1 = list[2].substring(5);
      temp2 = list[3].substring(5);
      firNum = Integer.parseInt(temp1); // number 1
      secNum = Integer.parseInt(temp2); // number 2

      result = firNum + secNum; // sum up two numbers

      out.print("HTTP/1.1 200 OK\r\n");
      out.print("Content-Length " + filename.length() + "\r\n");
      out.print("Content-Type "+ contentType + "\r\n");
      out.print("\r\n\r\n");

      out.print("<html><header><body><h1>Hello! <b>" + personName + "</h1></b><br>" + firNum + " + " + secNum + " = " + result + "</header>");
      out.print("</body></html>");



      // print this on the server
      System.out.println("Called on the browser: " + page + "\n");
      System.out.println("HTTP/1.1 200 OK");
      System.out.println("Content-Length: " + filename.length());
      System.out.println("Content-Type: "+ contentType + "\n");
      System.out.println("Hello! " + personName + "\n" + firNum + " + " + secNum + " = "+ result + "\n");
      out.flush();
  }

}
