public class IntegerRegFile {
    int[] registerFile; //the actual register file

    public IntegerRegFile()
    {
        //REGISTER FILE INITIALIZATION (with dummy values)
        registerFile = new int[32]; //create 32 integer registers
        for (int i = 0; i < registerFile.length; i++) {
            registerFile[i] = i + (i/2);
        }
    }

    public Integer readFromRegFile(int regNr)
    //method to be used when issuing the instruction to fetch the value or the address base
    {
        try
        {
            return registerFile[regNr];
        }
        catch(Exception e)
        {
            throw new ArrayIndexOutOfBoundsException("Register does not exist!!");
        }
    }

    public void WriteToRegFile(String tag, int val)
    //method to be used in write back stage to write value in register file
    {
        try
        {
            int reg = Integer.parseInt(tag.substring(1));

            registerFile[reg] = val;
        }
        catch(Exception e)
        {
            throw new ArrayIndexOutOfBoundsException("Register does not exist!!");
        }
    }

    public String toString()
    //display register file content
    {
        String output="Integer register File: " + '\n';
        for (int i = 0; i < registerFile.length; i++) {

            output +=  "Register Name=R" + i + ", " + "Val=" + registerFile[i] +'\n';
        }
        return  output;
    }

    //STOLEN FROM ALI'S TESTING REGISTER FILE
    public static void main(String[] args) {
        IntegerRegFile reg = new IntegerRegFile();
        reg.WriteToRegFile("R1", 1000);
        reg.WriteToRegFile("R2", 1000);
        System.out.println(reg);
    }
}
