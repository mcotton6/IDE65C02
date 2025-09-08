package idemain;

import m65c02.AddressDecoder;
import m65c02.M65C02;
import m65c02.RAM;
import m65c02.ROM;
import m65c02.SRecordLoader;

public class IDEMain
{
  public IDEMain()
  {
    // get an instance of the address space decoder
    AddressDecoder decoder = AddressDecoder.getInstance();
    // create some memory
    RAM ram = new RAM();
    System.out.println("RAM size = " + ram.getSize() + " bytes");
    decoder.setAddressRange(ram, 0x0000, ram.getSize());
    ROM rom = new ROM();
    System.out.println("ROM size = " + rom.getSize() + " bytes");
    decoder.setAddressRange(rom, 0x8000, rom.getSize());
    // pass the address space to the processor
    M65C02 m65c02 = new M65C02(decoder);
    // load in the s-record file
    SRecordLoader loader = new SRecordLoader(decoder);
    if (loader.loadFile("C:\\cygwin64\\home\\mcott\\test65\\test.asm.s9") < 0)
      {
      System.out.println("Error loading s9 file");
      System.exit(-1);
      }
    // issue reset signal and start running
    System.out.println("rom[]:");
    for (int i = 0; i < 64; i++)
    {
      System.out.print(String.format("%04X: ", 0x8000 + i*16));
      for (int j = 0; j < 16; j++)
        System.out.print(String.format("%02X", rom.read(i*16+j)) + " ");
      System.out.println();
    }
    m65c02.rst();
  }
  public static void main(String[] args)
  {
    // TODO Auto-generated method stub
    @SuppressWarnings("unused")
    IDEMain my_main = new IDEMain();
  }
}
