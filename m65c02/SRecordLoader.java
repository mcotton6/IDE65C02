package m65c02;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

public class SRecordLoader
{
  private AddressDecoder decoder;
  //---------------------------------------------------------------------------
  public SRecordLoader(AddressDecoder d)
  {
    decoder = d;
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
        System.out.println(str);
        parseRecord(str);
      }
    } catch (IOException e)
    {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } 
    return 0;
  }
  //---------------------------------------------------------------------------
  // parses a single S record
  private byte[] parseRecord(String record)
  {
    char[] chars = record.toCharArray();
    if (chars[0] != 'S' && chars[1] != '1')
      return null;
    int checksum = 0;
    int hex_tens = Integer.parseInt(new String(chars[2]+""),16) * 16;
    int hex_ones = Integer.parseInt(new String(chars[3]+""),16);
    int num_bytes = (hex_tens + hex_ones) - 1; // account for the checksum byte at the end
    System.out.println("num_bytes in record = "+num_bytes);
    byte[] byte_array = new byte[num_bytes];
    int j = 0;
    hex_tens = Integer.parseInt(new String(chars[4+j]+""),16) * 16;
    hex_ones = Integer.parseInt(new String(chars[5+j]+""),16);
    int address = (byte)((hex_tens + hex_ones) & 0x000000ff);
    j += 2;
    for (int i = 0; i < num_bytes; i++, j += 2)
    {
      hex_tens = Integer.parseInt(new String(chars[4+j]+""),16) * 16;
      hex_ones = Integer.parseInt(new String(chars[5+j]+""),16);
      byte_array[i] = (byte)((hex_tens + hex_ones) & 0x000000ff);
      System.out.println("byte = "+String.format("%02X",byte_array[i]));
      checksum += byte_array[i];
    }
    hex_tens = Integer.parseInt(new String(chars[6+num_bytes]+""),16) * 16;
    hex_ones = Integer.parseInt(new String(chars[7+num_bytes]+""),16);
    int record_checksum = (byte)((hex_tens + hex_ones) & 0x000000ff);
    System.out.println("Calc checksum = "+String.format("%2X",checksum)+
        ", record checksum = "+String.format("%2X",record_checksum));
    return byte_array;
  }
}
