
public class DataMemory {
    static double[] memory; // each location is 2 words (64 bits)
    static String mutexOwner; //if the memory is currently in use


    //initializes the memory with the given memory size
    public static void configure(int size){
        memory = new double[size];
    }

    //read from memory location index and return the result
    public static Double readLocation(int index){
        try{
          return memory[index];
        }
        catch(Exception e) {
            throw new ArrayIndexOutOfBoundsException("Register does not exist!!");
        }
    }

    //write to memory location index and return success or failure
    public static boolean writeLocation(int index, double data){
        try{
            memory[index] = data;
            return true;
        }
        catch(Exception e) {
            throw new ArrayIndexOutOfBoundsException("Register does not exist!!");
        }
    }

    //will using the memory now result in a structural hazard? true/false
    public static boolean inUse(){
        return mutexOwner != null;
    }

    //will using the memory now result in a structural hazard? true/false
    public static String getOwner(){
        String owner = mutexOwner;
        if( owner == null){
            owner = "";
        }
        return owner;
    }

    //mark the memory as busy before using it reservation success results in a true, failure false
    public static boolean reserve(String tag){
        if (mutexOwner == null) {
            mutexOwner = tag;
            return true;
        }
        return false;
    }

    //free the memory success results in a true, failure false
    public static boolean release(String tag){
        if (mutexOwner != null && mutexOwner.equals(tag)) {
            mutexOwner = null;
            return true;
        }
        return false;
    }
}
