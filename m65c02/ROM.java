package m65c02;

//-----------------------------------------------------------------------------
public class ROM implements MemDevice
{
  private byte [] rom;
  private final static int ROM_SIZE = 32768;
  private boolean locked;
  //---------------------------------------------------------------------------
  public ROM()
  {
    rom = new byte[ROM_SIZE];
    locked = true; // ROM must be unlocked to write to it
  }
  //---------------------------------------------------------------------------
  public byte read(int address)
  {
    return rom[address];
  }
  //---------------------------------------------------------------------------
  public void write(int address, byte data)
  {
    // only allow a write to the ROM area if it is unlocked
    if (locked)
      System.out.println("ERROR - ROM is locked!  Unable to write "+
          String.format("%02X",data)+" to address "+String.format("%04X", address));
    else
      rom[address] = data;
  }
  //---------------------------------------------------------------------------
  // allows the ROM area to be locked or unlocked
  public void lock(boolean value)
  {
    locked = value;
  }
  //---------------------------------------------------------------------------
  public int getSize()
  {
    return rom.length;
  }
  //---------------------------------------------------------------------------
  private void testProgram()
  {
    // add some simple instructions for now
    int i = 0;
    rom[i++] = (byte) 0x78; // 8000 SEI
    rom[i++] = (byte) 0xA9; // 8001 LDA #55
    rom[i++] = (byte) 0x55;
    rom[i++] = (byte) 0x4c; // 8003 JMP 8006
    rom[i++] = (byte) 0x06;
    rom[i++] = (byte) 0x80;
    rom[i++] = (byte) 0xa8; // 8006 TAY
    rom[i++] = (byte) 0x80; // 8007 BRA 8000
    rom[i++] = (byte) 0xf7;
    for (int j = 0; j < i; j++)
      System.out.println("PHYADD("+String.format("%04X", j)+"): "+String.format("%02X", rom[j]));
    // reset vector 0x8000
    rom[0x7ffc] = (byte) 0x00;
    rom[0x7ffd] = (byte) 0x80;
  }
}
