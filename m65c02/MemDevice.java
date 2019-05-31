package m65c02;

public interface MemDevice
{
  public byte read(int address);
  public void write(int address, byte data);
  public int getSize();
  public void lock(boolean lock_flag);
}
