import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Scanner;

/*  Summary:    Configures pi's in a network topology as specified by the user. 

    Steps:      1. Reads user's input from networkInput.txt file, where the first 
                    line of the file is the number of nodes. Lines 2 onwards 
                    is a list of adjacent nodes.
                2. Performs BFS to find the shortest path each PI can take to get 
                    to all other PI's in the network. Ex. Shortest path PI1 uses to
                    get to PI2, PI3, PI4, etc. Shortest path PI2 uses to get to PI1,
                    PI3, PI4, etc.
                3. Saves these paths into config files. The config files are sent to 
                    the appropriate PI.  Ex. All of PI1's shortest paths go into a 
                    file PI1config.txt. This file is sent to PI1, so that PI1 can 
                    know all of the paths it can take.
                4. Calls for a script to be executed on the PI's. This script will
                    read from the config file and convert these paths into routes
                    in the PI's routing table.  */

  public class Config{


    //Driver program 
    public static void main(String args[]) throws Exception
    {

        
        //Store user input file in textFile variable
        File textFile = new File("networkInput.txt");


        /*Logic to parse file and insert data into graph function in new.gv file. Then executes command 
        to open png file. The png image will show a picture of the desired network. These functions, 
        however, are not essential to the program and may be commented out. (writeToGVFile() and openPngFile())
        
        If you choose to use these functions, then be sure to first review important instructions located in the openPngFile() function.   */
        //writeToGVFile(textFile);
        //openPngFile();


        
        //Default ping count will be 10, unless otherwise specified by user in command-line argument
        int ping_count_custom=10;
       for(int i = args.length-1; i>=0; i--){
            //Customize ping count
            if(args[i].equals("-pc")){
                try {
                    ping_count_custom = Integer.parseInt(args[i+1]);
                } catch (Exception e) {
                    System.out.println(e);
                    System.out.println("Invalid use of -pc. You must provide an integer greater than 0 after the -pc argument.");
                    System.exit(1);
                }
            }
           //Execute configuration in NDN mode
            if(args[i].equals("-n")){
               switchToNDN();
           }  
       }

       /*IP Addressing Mode
       Logic to find shortest path*/
       startShortestPathSearch(textFile, ping_count_custom);   
    }                  




     /* -----------------------------------------------------------------
      ------------------       FUNCTIONS    ----------------------------
      -----------------------------------------------------------------*/

    public static void startShortestPathSearch(File textFile, int ping_count_custom) throws Exception{
          //Scan textfile 
          Scanner scan = new Scanner(textFile);
          //Number of nodes is the first integer in file
          int nodes = scan.nextInt();
          
          // Number of vertices is nodes + 1
          int v = nodes + 1; //6
  
   
          // Adjacency list for storing which vertices are connected
          //i.e. stores all of pi 1's connections, pi 2's connections...etc.
          /** Ex.  [ [x,x,x,...], [x,x,x,...], [x,x,x,...], ...] */
          ArrayList<ArrayList<Integer>> adj = new ArrayList<ArrayList<Integer>>(v);
          //add empty array list in each arraylist index
          // Ex. [ [], [], [], ...]
          for (int i = 0; i < v; i++) {
              adj.add(new ArrayList<Integer>());
          }
   
          
          //Scan textFile until there are no more integers to read
          while(scan.hasNextInt()){
              // Creating graph given in the diagram.
              // add_edge function takes adjacency list, source
              // and destination vertex as argument and forms
              // an edge between them.
                          //source         //destination
              addEdge(adj, scan.nextInt(), scan.nextInt());
          }
         
          int source, dest;
          
          
  
          //find each possible path to any given PI
          System.out.println("\n\n...Finding shortest route paths...");
          //Nested for loops collects all of Pi 1's connection to every other pi, then Pi 2's connections ...etc.
          for(int i = 0; i < v-1; i++ ) {
               //Number of starting PI
               source = i+1; //i+1 because we do not have a PI with the number 0, all PI's are labeled 1-n
                /*Create the config file for source node. When we enter the next for loop, we are
                    evaluating the source's path to every other pi in the network*/
               String piConfigFile = "pi" + source + "config.txt";
               runProcess("touch " + piConfigFile);
               runProcess("chmod 777 ./" + piConfigFile);

               FileWriter piConfig_writer = new FileWriter(piConfigFile);

               //Write the custom ping count here
                piConfig_writer.write(ping_count_custom + "\n");
    
                //Find all of curr_pi's connections to every other pi 
              for(int j = 0; j < v; j++){
                  //Number of destination PI
                  dest = j+1; //j+1 because we do not have a PI with the number 0, all PI's are labeled 1-n
                  //Will find shortest distance between source and destination pi and write to config file
                  printShortestDistance(adj, source, dest, v, piConfigFile, piConfig_writer); //write commands to add new route entry
              }
              piConfig_writer.close();
              //After we have written all possible paths PI i can take, send config file off
              sendConfigFilesToPI((i+1), piConfigFile);
          }
          scan.close();
          
          runPingAndSCPTest(nodes);
    }

    //Function to send config files to pis
    public static void sendConfigFilesToPI(int current_node, String piConfigFile) throws Exception{
        runProcess("scp " + piConfigFile + " umslpi" + current_node+ "@10.0.0." + current_node + ":~/RequiredForPICONFIG");
        runProcess("ssh umslpi" + current_node + "@10.0.0." + current_node + " ./RequiredForPICONFIG/configRoutes.sh");
        runProcess("rm " + piConfigFile);
        
    }

    //Function to start ping test in each pi
    public static void runPingAndSCPTest(int nodes) throws Exception{
        System.out.println("\nRunning Ping and SCP Tests");
        //Create script to ssh into each pi and execute their pingScript
        runProcess("touch sshingIntoPi.sh");
        runProcess("chmod 777 ./sshingIntoPi.sh");
        FileWriter sshingFileWriter = new FileWriter("sshingIntoPi.sh");

        sshingFileWriter.write("#!/bin/bash");

        for(int i = 0; i <nodes; i++){
            sshingFileWriter.write("\nssh umslpi" + (i+1) + "@10.0.0." + (i+1) + " ./RequiredForPICONFIG/pingTest.sh");
            sshingFileWriter.write("\nssh umslpi" + (i+1) + "@10.0.0." + (i+1) + " ./RequiredForPICONFIG/SCPTest.sh");
        }
        sshingFileWriter.close();
        runProcess("./sshingIntoPi.sh");
        System.out.println("\nData has been collected. Find test results in /RequiredForPICONFIG/Plots directory of each pi");
        runProcess("rm sshingIntoPi.sh");
    }

     // a modified version of BFS that stores predecessor
    // of each vertex in array pred
    // and its distance from source in array dist
    private static boolean BFS(ArrayList<ArrayList<Integer>> adj, int src,
                                  int dest, int v, int pred[], int dist[])
    {
        // a queue to maintain queue of vertices whose
        // adjacency list is to be scanned as per normal
        // BFS algorithm using LinkedList of Integer type
        LinkedList<Integer> queue = new LinkedList<Integer>();
 
        // boolean array visited[] which stores the
        // information whether ith vertex is reached
        // at least once in the Breadth first search
        boolean visited[] = new boolean[v];
 
        // initially all vertices are unvisited
        // so v[i] for all i is false
        // and as no path is yet constructed
        // dist[i] for all i set to infinity
        for (int i = 0; i < v; i++) {
            visited[i] = false;
            dist[i] = Integer.MAX_VALUE;
            pred[i] = -1;
        }
 
        // now source is first to be visited and
        // distance from source to itself should be 0
        visited[src] = true;
        dist[src] = 0;
        queue.add(src);
 
        // bfs Algorithm
        while (!queue.isEmpty()) {
            int u = queue.remove();
            for (int i = 0; i < adj.get(u).size(); i++) {
                if (visited[adj.get(u).get(i)] == false) {
                    visited[adj.get(u).get(i)] = true;
                    dist[adj.get(u).get(i)] = dist[u] + 1;
                    pred[adj.get(u).get(i)] = u;
                    queue.add(adj.get(u).get(i));
 
                    // stopping condition (when we find
                    // our destination)
                    if (adj.get(u).get(i) == dest)
                        return true;
                }
            }
        }
        return false;
    }

    // function to print the shortest distance and path
    // between source vertex and destination vertex
    private static void printShortestDistance(
                     ArrayList<ArrayList<Integer>> adj,
                             int s, int dest, int v, String piConfigFile, FileWriter piConfig_writer) throws Exception
    {
        // predecessor[i] array stores predecessor of
        // i and distance array stores distance of i
        // from s
        int pred[] = new int[v];
        int dist[] = new int[v];
  
        //If BFS is false, then source and destination are NOT connected in network
        if (BFS(adj, s, dest, v, pred, dist) == false) {
            return;
        }
 
        // LinkedList to store path
        LinkedList<Integer> path = new LinkedList<Integer>();
        int crawl = dest;
        //Each node we visit to get to destination is added to path
        path.add(crawl);
        //-1 means we have not visited node, while any other integer means we have
        while (pred[crawl] != -1) {
            //Add node to path if we have visited
            path.add(pred[crawl]);
            crawl = pred[crawl];
        }
 
       //Save path from source node to dest node into a vector to be used for configuration
        for (int i = path.size() - 1; i >= 0; i--) {
            piConfig_writer.write(path.get(i) + " ");
        }
        piConfig_writer.write("\n");
    }

     // function to form edge between two vertices
    // source and dest
    private static void addEdge(ArrayList<ArrayList<Integer>> adj, int i, int j)
    {
        //Source pi stores edge to destination pi
        adj.get(i).add(j);
        //Destination pi stores edge to source pi
        adj.get(j).add(i);
    }

     //function to switch to NDN mode 
     public static void switchToNDN(){
        System.out.println("In NDN mode");
        System.exit(0);
    }

     //function to run terminal command to greate a graphviz image from new.gv file. Then, opens the file
     public static void openPngFile() throws Exception{
        runProcess("neato -Tpng new.gv -o new.png");
        Thread.sleep(1000);
        runProcess("open new.png");
        runProcess("rm new.gv");
    }

    //Function that takes terminal commands as a String and runs them in terminal
    public static void runProcess(String command) throws Exception {
        //process waits for each command to be executed and stores any outputs we get from terminal
        Process pro = Runtime.getRuntime().exec(command);
        //print any errors
        try{
            printLinesFromTerminalCommands( "stdout:", pro.getInputStream());
            printLinesFromTerminalCommands(" stderr:", pro.getErrorStream());
        } catch(Exception e){
            e.printStackTrace();
        }
        pro.waitFor(); //wait for current command to finish
        //print exitValue() after commmand has run for troubleshooting hints
        System.out.println(command + " exitValue() " + pro.exitValue());
    }

    //Function to create new.gv file, write to file in the necessary "a--b" format that graphviz is able to read
    public static void writeToGVFile(File textFile) throws Exception{
        //Scanner object to read from textFile
        Scanner graph_file_scanner = new Scanner(textFile);
        //Create new.gv file
        runProcess("touch new.gv");
        //File writer to new.gv file
        FileWriter graph_writer = new FileWriter("new.gv");

        //Write first line of new.gv 
        graph_writer.write("graph foo {");

        //Scan first int which is the # of nodes. We don't need to do anything with this
        graph_file_scanner.nextInt();

        //Start scanning remainder of text file
        while(graph_file_scanner.hasNextInt()){
            graph_writer.write("\n");
            graph_writer.write("\t" + graph_file_scanner.nextInt() + " -- " + graph_file_scanner.nextInt() + ";");
        }
        graph_writer.write("\n}");
        
        graph_file_scanner.close();
        graph_writer.close();
    }

    //Function that prints output from termanal after we've run a command
                                                                    //needs to know what process
                                                                    //we want to print from, hence
                                                                    //InputStream ins
   private static void printLinesFromTerminalCommands(String name, InputStream ins) throws Exception {
    String line = null;
    BufferedReader in = new BufferedReader(new InputStreamReader(ins));
    while ((line = in.readLine()) != null) {
        System.out.println(name + " " + line);
    }
   }

}


