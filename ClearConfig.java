import java.io.File;
import java.util.Scanner;

public class ClearConfig {

    public static void main(String[] args) throws Exception{

        //Store user input file in textFile variable
        File textFile = new File("networkInput.txt");

        //Scan textfile 
        Scanner scan = new Scanner(textFile);
        //Number of nodes is the first integer in file
        int nodes = scan.nextInt();

         //A command line argument was detected
         if(args.length >0){
            //Logic to run ClearConfig with the option to clear all route AND all Plots from /RequiredForPICONFIG/Plots directory
            if(args[0].charAt(0) == '-' && Character.toUpperCase(args[0].charAt(1)) == 'P'){
                //For each pi in network, ssh into it and run the removeRoutes.sh script that is on each of them
                //Also remove all plots, per the command line argument option
                for(int i = 0; i < nodes; i++){
                    Config.runProcess("ssh epharra" + (i+1) + "@10.0.0." + (i+1) + " ./RequiredForPICONFIG/removeRoutes.sh");
                    Config.runProcess("ssh epharra" + (i+1) + "@10.0.0." + (i+1) + " ./RequiredForPICONFIG/removePlots.sh");
                 }
            }
            //Else invalid argument
            else{
                System.out.println("\n\nInvalid command line argument. Enter -p or -P to run ClearConfig program with the option to delete all plots in addition to deleting all routes");
                System.out.println("Otherwise, run program without any command line arguments. Default will only delete all routes and nothing else");
                System.exit(0);
            }

            //Default clearConfig is to remove only the routes from routing table
            for(int i = 0; i < nodes; i++){
                Config.runProcess("ssh epharra" + (i+1) + "@10.0.0." + (i+1) + " ./RequiredForPICONFIG/removeRoutes.sh");
             }
        }
        
        
        scan.close();
    }

    
}