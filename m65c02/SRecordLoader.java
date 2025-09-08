package m65c02;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

//-----------------------------------------------------------------------------
public class SRecordLoader
{
  private AddressDecoder decoder;
  private int            checksum;
  static final private boolean debug = false;
  //---------------------------------------------------------------------------
  public SRecordLoader(AddressDecoder d)
  {
    decoder = d;
    checksum = 0;
  }
  //---------------------------------------------------------------------------
  public int loadFile(String filename)
  {
    try
    {
      File file = new File(filename);
      @SuppressWarnings("resource")
      BufferedReader br = new BufferedReader(new FileReader(file));
      String str; 
      while ((str = br.readLine()) != null)
      {
        //System.out.println(str);
        parseRecord(str);
      }
    } catch (IOException e)
    {
      // TODO Auto-generated catch block
      e.printStackTrace();
      return -1;
    } 
    return 0;
  }
  //---------------------------------------------------------------------------
  // parses a single S record
  private byte[] parseRecord(String record)
  {
    // make sure we're dealing with an S1 record for now
    char[] chars = record.toCharArray();
    if (chars[0] != 'S' && chars[1] != '1')
      return null;
    // init checksum
    checksum = 0;
    // read the number of bytes in the record (includes the address word and checksum byte)
    byte b = readByte(chars[2],chars[3]);
    // account for the checksum byte at the end
    // this will be the number of bytes used to verify the checksum
    int num_bytes = b - 1;
    if (debug) System.out.println("num_bytes in record = "+num_bytes);
    byte[] byte_array = new byte[num_bytes-2];
    int j = 0;
    // read the address word (16 bit address)
    // upper byte of address
    int upper = (int)readByte(chars[4+j],chars[5+j]) << 8;
    upper &= 0xff00;
    if (debug) System.out.println("Upper byte of address = "+String.format("%04X", upper));
    j += 2;
    // lower 8 bits of address
    int lower = (int)readByte(chars[4+j],chars[5+j]);
    lower &= 0x00ff;
    int address = upper | lower;
    address &= 0xffff;
    j += 2;
    if (debug) System.out.println("Address in S-Record = "+String.format("%04X", address));
    if (debug) System.out.print("bytes: ");
    for (int i = 0; i < num_bytes-2; i++, j += 2)
    {
      b = readByte(chars[4+j],chars[5+j]);
      byte_array[i] = b;
      if (debug) System.out.print(String.format("%02X ",byte_array[i]));
    }
    if (debug) System.out.println();
    int record_checksum = readByteNoCheck(chars[4+j],chars[5+j]);
    if (debug)
      {
      System.out.println("Calc checksum = "+String.format("%2X",checksum)+
        ", record checksum = "+String.format("%2X",record_checksum));
      }
    checksum += record_checksum + 1;
    checksum &= 0xff;
    if (checksum != 0)
      System.out.println("ERROR - S record has bad checksum!");
    else
    {
      // now that we know the checksum is good we can load the record into memory
      //System.out.println("Loading "+byte_array.length+" bytes at address "+String.format("%04X", address));
      decoder.unlock(address); // unlock it first
      int byte_address = address;
      for (int i = 0; i < byte_array.length; i++)
      {
        decoder.write(byte_address++, byte_array[i]);
      }
      decoder.lock(address); // lock it once complete
    }
    return byte_array;
  }
  //---------------------------------------------------------------------------
  // readByte method
  private byte readByte(char c1, char c2)
  {
    byte b = readByteNoCheck(c1,c2);
    checksum += b;
    checksum &= 0xff;
    return b;
  }
  //---------------------------------------------------------------------------
  // readByte method without adding it to the checksum
  private byte readByteNoCheck(char c1, char c2)
  {
    int hex_tens = Integer.parseInt(new String(c1+""),16) * 16;
    int hex_ones = Integer.parseInt(new String(c2+""),16);
    byte b = (byte)((hex_tens | hex_ones) & 0xff);
    return b;
  }
}
