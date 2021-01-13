import java.util.Hashtable;

public class LOAD_BUFFER {
    Hashtable<String, Object>[] buffer; //the actual reservation station
    int no_unused_rs; //this variable is to be used to know if there are any unused reservations stations
    int latency; //the latency of the add and sub instructions

    public LOAD_BUFFER(int stationNum, int latency){
        buffer = new Hashtable[stationNum];
        no_unused_rs = stationNum;
        this.latency = latency;

        for (int i = 0; i < buffer.length; i++) {
            buffer[i] = new Hashtable<String, Object>();
            buffer[i].put("RowNr","");// row number in display table
            buffer[i].put("Tag", "L"+(i+1));
            buffer[i].put("Time", 0);//This field is used to keep track of when the instruction finishes execution
            buffer[i].put("Busy", 0);
            buffer[i].put("Address", 0);
        }
    }

    public boolean freeRS()
    //check if there is a free reservation station
    {
        return no_unused_rs > 0;
    }

    public String issueToReservationStation(int address, String rowNr)
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
            if(buffer[i].get("Busy").equals(1) && !buffer[i].get("Time").equals(0)){
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


    public Hashtable getFinishedInstruction(int cycle)
    {
        for (int i = 0; i < buffer.length; i++)
        {
            if(buffer[i].get("Busy").equals(1) && buffer[i].get("Time").equals(0) && !Processor.DisplayTable
                    .get(Integer.parseInt((String) buffer[i].get("RowNr"))).get("End of Execution").equals(cycle+""))
            {
                //this instruction has finished and would like to write back so return its station tag and the value
                Hashtable tagAndVal= new Hashtable<String, String>();

                tagAndVal.put("Tag", buffer[i].get("Tag"));

                //retrieve the load result from memory
                 double result= DataMemory.readLocation((int) buffer[i].get("Address"));

                tagAndVal.put("Val", result);
                tagAndVal.put("RowNr", buffer[i].get("RowNr"));
                return tagAndVal;
            }
        }
        return null;
    }

    public void eraseInstruction(String tag)
    //method used to erase instruction from reservation station
    {
        int i = Integer.parseInt(tag.substring(1))-1;
        buffer[i].put("Tag", "L"+(i+1));
        buffer[i].put("Time", 0);//This field is used to keep track of when the instruction finishes execution
        buffer[i].put("Busy", 0);
        buffer[i].put("Address", 0);
        no_unused_rs++;
    }

    public boolean isEmpty()
    {
        return no_unused_rs == buffer.length;
    }

    public String toString()
    //display reservation station content
    {
        StringBuilder output = new StringBuilder("Load buffer: " + '\n');
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
                    .append("Address=")
                    .append(ht.get("Address"))
                    .append('\n');
        }
        return output.toString();
    }

}
