package idemain;

import m65c02.AddressDecoder;
import m65c02.M65C02;
import m65c02.RAM;
import m65c02.ROM;

public class IDEMain
{
  public IDEMain()
  {
    // get an instance of the address space decoder
    AddressDecoder decoder = AddressDecoder.getInstance();
    // create some memory
    RAM ram = new RAM();
    decoder.setAddressRange(ram, 0x0000, ram.getSize());
    ROM rom = new ROM();
    decoder.setAddressRange(rom, 0x8000, ram.getSize());
    // pass the address space to the processor
    M65C02 m65c02 = new M65C02(decoder);
    // issue reset signal and start running
    m65c02.rst();
  }
  public static void main(String[] args)
  {
    // TODO Auto-generated method stub
    @SuppressWarnings("unused")
    IDEMain my_main = new IDEMain();
  }
}
