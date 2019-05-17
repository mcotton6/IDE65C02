package m65c02;

public class RAM implements MemDevice
{
  private byte [] ram;
  private final static int RAM_SIZE = 32768;
  public RAM()
  {
    ram = new byte[RAM_SIZE];
  }
  public byte read(int address)
  {
    return ram[address];
  }
  public void write(int address, byte data)
  {
    ram[address] = data;
  }
  public int getSize()
  {
    return ram.length;
  }
}
