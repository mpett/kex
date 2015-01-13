import gnu.io.CommPort;
import gnu.io.CommPortIdentifier;
import gnu.io.SerialPort;
import gnu.io.SerialPortEvent;
import gnu.io.SerialPortEventListener;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;

/**
 * A modified version of the TwoWaySerialComm example that makes use of the 
 * SerialPortEventListener to avoid polling.
 *
 * Added calculation of the median of measured values during a 2,5 s interval.
 * Compares if there's been an increase or decrease and
 * communicates this through public increasedEDA()
 */
public class PortReader
{
    private static final int freq = 81;		//2,5 s at 32 hz
    private static final int mid = 40;		//used for extracting median
    private int linec = 0;					//read line counter
	private ArrayList<Float> m = new ArrayList<Float>(freq);
	
	private boolean ready = false;
	private boolean increased = false;
	
	private float lastMedian = 0;

    public PortReader()
    {
        super();
    }
    
    void connect ( String portName ) throws Exception
    {
        CommPortIdentifier portIdentifier = CommPortIdentifier.getPortIdentifier(portName);
        if ( portIdentifier.isCurrentlyOwned() )
        {
            System.out.println("Error: Port is currently in use");
        }
        else
        {
            CommPort commPort = portIdentifier.open(this.getClass().getName(),2000);
            
            if ( commPort instanceof SerialPort )
            {
                SerialPort serialPort = (SerialPort) commPort;
                serialPort.setSerialPortParams(115200,SerialPort.DATABITS_8,SerialPort.STOPBITS_1,SerialPort.PARITY_NONE);
                
                InputStream in = serialPort.getInputStream();
                
                serialPort.addEventListener(new SerialReader(in));
                serialPort.notifyOnDataAvailable(true);

            }
            else
            {
                System.out.println("Error: Only serial ports are handled by this example.");
            }
        }     
    }
    
    /**
     * Handles the input coming from the serial port. A new line character
     * is treated as the end of a block in this example. 
     */
    public class SerialReader implements SerialPortEventListener 
    {
        private InputStream in;
        private byte[] buffer = new byte[1024];
        
        public SerialReader ( InputStream in )
        {
            this.in = in;
        }
        
        public void serialEvent(SerialPortEvent arg0) {
            int data;
          
            try
            {
                int len = 0;
                while ( ( data = in.read()) > -1 )
                {
                    if ( data == '\n' ) {
                        break;
                    }
                    buffer[len++] = (byte) data;
                }
                
                
                String line = new String(buffer,0,len);		//convert byte data to String
                String[] cutline = new String[7];

                if(linec < freq) {	//if still in current interval
          		cutline = line.split(",");		//split on ,
          		
          		m.add(Float.parseFloat(cutline[6]));	//extract EDA value and add to the ArrayList
      			linec++;
                
                } else {
        			System.out.println("-- START --");
        			float res = median(m);
        			if(lastMedian <= res)
        				increased = true;
        			else
        				increased = false;

        			System.out.println("LastMedian: " + lastMedian);
        			System.out.println("Median: " + res);
        			System.out.println("Increased: " + increased);
        			System.out.println("--- END ---");
        			
        			lastMedian = res;
        			ready = true;		//set ready flag
        			//reset line count and ArrayList
        			linec = 0;
        			m.clear();

        		}
            }
            catch ( IOException e )
            {
                e.printStackTrace();
                System.exit(-1);
            }             
        }

    }
    
    private float median(ArrayList<Float> m){
    	Collections.sort(m);
    	return m.get(mid);
    }

    /*
     * Flags that there's new interval data to be read
     */
    public boolean isReady() {
    	if(ready) {
    		ready = false;	//reset flag
    		return true;
    	}
    	return false;
    }
    
    public boolean increasedEDA() {
    	return increased;
    }
}
