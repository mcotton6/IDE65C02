package m65c02;
//---------------------------------------------------------------------------
public class AddressDecoder
{
  private final static int NUM_ADDRESSES = 16;
  private int address_base[];
  private int address_end[];
  private MemDevice device[];
  private int num_addresses = 0;
  private boolean debug;
  static private AddressDecoder instance = null;
  //---------------------------------------------------------------------------
  private AddressDecoder()
  {
    address_base  = new int[NUM_ADDRESSES];
    address_end   = new int[NUM_ADDRESSES];
    device        = new MemDevice[NUM_ADDRESSES];
    num_addresses = 0;
    debug         = false;
  }
  //---------------------------------------------------------------------------
  public void setDebug(boolean enable)
  {
    debug = enable;
  }
  //---------------------------------------------------------------------------
  public static AddressDecoder getInstance()
  {
    if (instance == null)
      instance = new AddressDecoder();
    return instance;
  }
  //---------------------------------------------------------------------------
  public void setAddressRange(MemDevice d, int base, int size)
  {
    device[num_addresses]       = d;
    address_base[num_addresses] = base;
    address_end[num_addresses]  = base + size - 1;
    num_addresses++;
  }
  //---------------------------------------------------------------------------
  public byte read(int address)
  {
    byte data = (byte) 0xff;
    // search through the stored addresses and their respective ranges to see
    // if this passed address matches a valid address
    for (int i = 0; i < num_addresses; i++)
    {
      if (address >= address_base[i] && address <= address_end[i])
      {
        int phys_addr = address - address_base[i];
        if (debug)
          System.out.println("Decoder: Address: "+String.format("%04X", address)+
            " PhysAddr: "+ String.format("%04X", phys_addr));
        data = device[i].read(phys_addr);
        break;
      }
    }
    return data;
  }
  //---------------------------------------------------------------------------
  public void write(int address, byte data)
  {
    // search through the stored addresses and their respective ranges to see
    // if this passed address matches a valid address
    for (int i = 0; i < num_addresses; i++)
    {
      if (address >= address_base[i] && address <= address_end[i])
      {
        // convert to the physical address within this memory space
        int phys_addr = address - address_base[i];
        // writing to the address provided
        //System.out.println("Writing data to address "+String.format("%04X", address));
        device[i].write(phys_addr, data);
        break;
      }
    }
  }
}
