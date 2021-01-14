import java.util.ArrayList;
import java.util.Hashtable;

public class STORE_BUFFER {
    Hashtable<String, Object>[] buffer; //the actual reservation station
    int no_unused_rs; //this variable is to be used to know if there are any unused reservations stations
    int latency; //the latency of the add and sub instructions

    public STORE_BUFFER(int stationNum, int latency){
        buffer = new Hashtable[stationNum];
        no_unused_rs = stationNum;
        this.latency = latency;

        for (int i = 0; i < buffer.length; i++) {
            buffer[i] = new Hashtable<String, Object>();
            buffer[i].put("RowNr","");// row number in display table
            buffer[i].put("Tag", "S"+(i+1));
            buffer[i].put("Time", 0);//This field is used to keep track of when the instruction finishes execution
            buffer[i].put("Busy", 0); //available for use
            buffer[i].put("Q", 0);// tag of RS which holds the value (0 means value is available)
            buffer[i].put("V", 0);//RT register value
            buffer[i].put("Address", 0);
        }
    }

    public boolean freeRS()
    //check if there is a free reservation station
    {
        return no_unused_rs > 0;
    }

    public String issueToReservationStation(Hashtable operand, int address, String rowNr)
    //method used to place instruction in reservation station
    {
        for (int i = 0; i < buffer.length; i++) {
            if(buffer[i].get("Busy").equals(0))
            {
                //This reservation station is free so add instruction to it

                no_unused_rs--;//decrement number of unused reservation stations by 1

                buffer[i].put("Busy", 1); //setting the busy to 1

                //set the instruction to start next cycle
                buffer[i].put("Time", latency+1);

                //setting the values of Q and V
                if(operand.get("Qi").equals(0))
                {
                    buffer[i].put("V", (Double) operand.get("Val"));
                    buffer[i].put("Q", operand.get("Qi"));
                }
                else
                {
                    buffer[i].put("V", 0);
                    buffer[i].put("Q", operand.get("Qi"));
                }

                //put the resolved address in the buffer
                buffer[i].put("Address", address);

                //add row number
                buffer[i].put("RowNr", rowNr);

                return (String) buffer[i].get("Tag");
            }
        }
        return null;
    }

    public Object[] getCandidate(){
        Hashtable candidate = null;
        int candidateIssue = -1;
        for (int i = 0; i < buffer.length; i++)
        {
             if(buffer[i].get("Busy").equals(1) && !buffer[i].get("Time").equals(0)
                    && buffer[i].get("Q").equals(0)){

                if(!buffer[i].get("RowNr").equals("") && buffer[i].get("Time").equals(latency + 1)) {
                    int issueCycle = Integer.parseInt(Processor.DisplayTable
                            .get(Integer.parseInt((String) buffer[i].get("RowNr"))).get("Issue"));

                    if(candidateIssue == -1 || issueCycle < candidateIssue){
                        candidate = buffer[i];
                        candidateIssue  = issueCycle;
                    }
                }
            }
        }
        return new Object[]{candidateIssue, candidate};
    }

    public void executeInstructions(int cycle)
    {
        for (int i = 0; i < buffer.length; i++)
        {
            if(buffer[i].get("Busy").equals(1) && !buffer[i].get("Time").equals(0)
                    && DataMemory.getOwner().equals(buffer[i].get("Tag")))
            {
                if(buffer[i].get("Time").equals(latency))
                {
                    Processor.DisplayTable.get(Integer.parseInt((String) buffer[i].get("RowNr")))
                            .put("Start of Execution", cycle+""); //update start of execution field in table
                }

                //this reservation station contains an instruction in execution since busy=1
                //and the time is not equal to zero meaning it has not finished execution
                buffer[i].put("Time",(Integer)buffer[i].get("Time")-1); //it will execute in this cycle meaning the number of cycles left to finish will decrease by one

                if(buffer[i].get("Time").equals(0))
                {
                    //release the memory for functions to use in the next cycle
                    DataMemory.release((String) buffer[i].get("Tag"));

                    Processor.DisplayTable.get(Integer.parseInt((String) buffer[i].get("RowNr")))
                            .put("End of Execution", cycle+""); //update end of execution field in table
                }
            }
        }
    }

    public void recieveTagFromBus(String tag, double val)
    //method used to update the Qj and Qk when an instruction writes back
    {
        for (int i = 0; i < buffer.length; i++) {
            if(buffer[i].get("Q").equals(tag))
            {
                buffer[i].put("V", val);
                buffer[i].put("Q", 0);

                if(buffer[i].get("Q").equals(0)) //check if operand is now ready and if it is, set the time field of station
                {
                    buffer[i].put("Time", latency);
                }
            }
        }
    }
    

    public ArrayList<String> eraseFinishedInstructions(int cycle)
    //method used to erase instruction from reservation station
    {
        ArrayList<String> rows = new ArrayList<>();
        for (int i = 0; i < buffer.length; i++) {

            if(buffer[i].get("Busy").equals(1) && buffer[i].get("Time").equals(0) && !Processor.DisplayTable
                    .get(Integer.parseInt((String) buffer[i].get("RowNr"))).get("End of Execution").equals(cycle+"")) {

                DataMemory.writeLocation((int) buffer[i].get("Address"), (double) buffer[i].get("V"));
                rows.add((String) buffer[i].get("RowNr"));

                buffer[i].put("RowNr", "");// row number in display table
                buffer[i].put("Tag", "S" + (i + 1));
                buffer[i].put("Time", 0);//This field is used to keep track of when the instruction finishes execution
                buffer[i].put("Busy", 0); //available for use
                buffer[i].put("Q", 0);// tag of RS which holds the value (0 means value is available)
                buffer[i].put("V", 0);//RT register value
                buffer[i].put("Address", 0);
                no_unused_rs++;
            }
        }
        return rows;
    }

    public boolean isEmpty()
    {
        return no_unused_rs == buffer.length;
    }

    public String toString()
    //display reservation station content
    {
        StringBuilder output = new StringBuilder("Store buffer: " + '\n');
        for(Hashtable<String, Object> ht: buffer)
        {
            output.append("Time=")
                    .append(ht.get("Time"))
                    .append(", ")
                    .append("Tag=")
                    .append(ht.get("Tag"))
                    .append(", ")
                    .append("Busy=")
                    .append(ht.get("Busy"))
                    .append(", ")
                    .append("Q=")
                    .append(ht.get("Q"))
                    .append(", ")
                    .append("V=")
                    .append(ht.get("V"))
                    .append(", ")
                    .append("Address=")
                    .append(ht.get("Address"))
                    .append('\n');
        }
        return output.toString();
    }

}
