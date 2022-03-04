import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Random;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/* 
  Name: Andreas Hinsch 
  Course: CNT 4714 Spring 2022 
  Assignment title: Project 2 – Multi-threaded programming in Java 
  Date:  February 13, 2022 
 
  Class:  CNT4714 
*/

public class Main {

    public static void main(String[] args) {

        int MAX = 10;

        try {
            //!ATTENTION
            //!
            //Change File name to desired file before running
            //!
            //!ATTENTION
            File file = new File("C:\\Users\\an478360\\Downloads\\config.txt");

            //Print stream to file
            System.setOut(new PrintStream(new FileOutputStream("C:\Users\an478360\Downloads\\output.txt")));

            //BEGIN
            System.out.println("PACKAGE MANAGEMENT SYSTEM BEGINS");

            Scanner reads = new Scanner(file);

            //config list
            ArrayList < Integer > list = new ArrayList < Integer > ();

            //Gather config into an Array
            while (reads.hasNext()) {
                list.add(reads.nextInt());
            }
            //Close file pointer
            reads.close();

            //Create Max pool of threads of size 10
            ExecutorService Pool = Executors.newFixedThreadPool(MAX);

            //Create both station and Conveyor list
            Conveyor[] ConveyorArray = new Conveyor[list.get(0)];
            Station[] StationArray = new Station[list.get(0)];

            //Size is the number of Conveyors and Stations that we need to create
            int size = list.get(0);

            //Fill conveyors
            for (int x = 0; x < size; x++) {

                //Create a new Conveyor Object and insert it into our array
                Conveyor Conveyor = new Conveyor(x);
                ConveyorArray[x] = Conveyor;

            }

            //Fill Stations
            for (int x = 0; x < size - 1; x++) {
                //Create a new Station Object and insert it into our array
                //int StationNumber,      Conveyor IN,       Conveyor OUT,    int Workload
                Station Station = new Station(x, ConveyorArray[x], ConveyorArray[x + 1], list.get(x + 1));
                StationArray[x] = Station;

            }

            //Fill last station
            Station Station = new Station(size - 1, ConveyorArray[size - 1], ConveyorArray[0], list.get(size));
            StationArray[size - 1] = Station;

            //Run threads for the Stations
            for (int x = 0; x < list.get(0); x++) {
                Pool.execute(StationArray[x]);
            }
            //Close Thread pool
            Pool.shutdown();

        } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }

}

class Station implements Runnable {

    //Station Number
    int StationNumber;

    //The Outflow and Inflow Conveyors 
    Conveyor IN;
    Conveyor OUT;

    //amount of time it takes the station to complete one workload
    Random Rand = new Random();
    int Worktime;

    //Workload
    int Workload;

    //Constructor
    public Station(int StationNumber, Conveyor IN, Conveyor OUT, int Workload) {

        //Generates a random amount of time the work takes to simulate 
        //Random amount of time it takes the station to complete one workload with an arbitrary max
        this.Worktime = Rand.nextInt(500);

        //Station Number
        this.StationNumber = StationNumber;

        //Input and output Conveyors
        this.IN = IN;
        this.OUT = OUT;

        //Workload
        this.Workload = Workload;
    }
    @Override
    public void run() {

        //Print Conveyor setup 
        System.out.println("Routing Station " + StationNumber + " : Workload set. Station " + StationNumber + " has a total of " + Workload + " package groups to move");
        System.out.println("Routing Station " + StationNumber + " : Online");
        System.out.println("Routing Station " + StationNumber + " : Input conveyor set to conveyor number " + IN.ConveyorNumber);
        System.out.println("Routing Station " + StationNumber + " : Output conveyor set to conveyor number " + OUT.ConveyorNumber);

        //Until we run out of work keep trying to work
        while (Workload > 0) {
            //If IN is open
            if (IN.CheckLocks()) {

                //Status report
                System.out.println("Routing Station " + StationNumber + " : holds lock on input conveyor " + IN.ConveyorNumber);

                //If IN is open but OUT is closed
                if (OUT.CheckLocks()) {
                    System.out.println("Routing Station " + StationNumber + " : holds lock on output conveyor " + OUT.ConveyorNumber);
                    //If we successfully open both locks we are free to work
                    try {
                        //sleep through arbitrary worktime
                        System.out.println("Routing Station " + StationNumber + " : CURRENTLY HARD AT WORK MOVING PACKAGES.");
                        Thread.sleep(Worktime);

                        //Release IN
                        IN.Unlocks();
                        System.out.println("Routing Station " + StationNumber + " : unlocks/releases input conveyor " + IN.ConveyorNumber);

                        //Release OUT
                        OUT.Unlocks();
                        System.out.println("Routing Station " + StationNumber + " : unlocks/releases output conveyor " + OUT.ConveyorNumber);

                        //Workload is completed
                        Workload = Workload - 1;

                        //Status report
                        System.out.println("Routing Station " + StationNumber + " : has " + Workload + " package groups left to move");

                    } catch (InterruptedException e) {

                        e.printStackTrace();
                    }

                } else {
                    //Status report
                    IN.Unlocks();
                    System.out.println("Routing Station " + StationNumber + " : unable to lock output conveyor " + OUT.ConveyorNumber + ", unlocks input conveyor " + IN.ConveyorNumber);

                    try {
                        //Wait
                        Thread.sleep(Rand.nextInt(500));
                    } catch (InterruptedException e) {

                        e.printStackTrace();
                    }
                }

            }

            //If Input conveyor cannot be unlocked
            else {
                //Status report
                IN.Unlocks();
                System.out.println("Routing Station " + StationNumber + " : unable to lock input conveyor " + IN.ConveyorNumber + ", unlocks input conveyor " + IN.ConveyorNumber);
                try {
                    //Wait
                    Thread.sleep(Rand.nextInt(500));
                } catch (InterruptedException e) {

                    e.printStackTrace();
                }
            }

        }

        //FINAL Status report
        System.out.println("*  *  Station  " + StationNumber + ":  Workload  successfully  completed.  *  *  Station  " + StationNumber + " releasing locks and going offline * *");
    }

}

class Conveyor {

    //ConveyorNumber
    int ConveyorNumber;

    //Lock
    private Lock lock = new ReentrantLock();

    //Constructor
    public Conveyor(int ConveyorNumber) {

        this.ConveyorNumber = ConveyorNumber;

    }

    public void Unlocks() {
        //!ATTENTION
        //!
        //Instead of requiring both locks to be open we can access the lock outside of its owner but we need to protect the integrity of the lock.  By doing this we bypass the need to have both locks open and can simplify the code. However we will generate Many more fail locks. 
        //!
        //!ATTENTION
        if (((ReentrantLock) lock).isHeldByCurrentThread()) {
            //UNLOCK
            lock.unlock();
        }
    }
    //Checks to see if a lock can be locked and locks it
    public boolean CheckLocks() {
        return lock.tryLock();
    }

}