package m65c02;

public class Disassembler
{
  private int instr_num_bytes;
  private int address;
  //---------------------------------------------------------------------------
  public Disassembler()
  {
    instr_num_bytes = 0;
    address = 0;
  }
  //---------------------------------------------------------------------------
  public String disassemble(int addr, byte byte0, byte byte1, byte byte2)
  {
    instr_num_bytes = 1; // always at least a 1 byte instruction
    address = addr;      // save the start address for the instruction
    String str = new String("");
    str = str.concat(String.format("%04X", addr));
    str = str.concat(" ");
    str = str.concat(String.format("%02X", byte0));
    str = str.concat(" ");
    str = str.concat(getMnemonic(byte0));
    str = str.concat(" ");
    // now figure out the addressing mode
    // aaabbbcc - aaa---cc determine the opcode and ---bbb-- determines the addressing mode
    //    bbb addressing mode
    //    000 (zero page,X)
    //    001 zero page
    //    010 #immediate
    //    011 absolute
    //    100 (zero page),Y
    //    101 zero page,X
    //    110 absolute,Y
    //    111 absolute,X
    int mode = (byte0 & 0x1c) >> 2;
    if (byte0 == (byte)0x20) mode = 3; // jsr
    else if (byte0 == (byte)0x78) mode = 8; // sei
    else if (byte0 == (byte)0x60) mode = 8; // rts
    else if (byte0 == (byte)0xd0) mode = 1; // bne
    //System.out.println("byte0 = "+String.format("%02X",byte0)+" mode = "+mode);
    switch (mode)
    {
    case 0:
      str = str.concat("("+String.format("%02X",byte1)+",X)");
      instr_num_bytes = 2;
      break;
    case 1:
      str = str.concat(String.format("%02X",byte1));
      instr_num_bytes = 2;
      break;
    case 2:
      str = str.concat("#"+String.format("%02X",byte1));
      instr_num_bytes = 2;
      break;
    case 3:
      str = str.concat(String.format("%02X",byte2)+String.format("%02X",byte1));
      instr_num_bytes = 3;
      break;
    case 4:
      str = str.concat("("+String.format("%02X",byte1)+"),Y");
      instr_num_bytes = 2;
      break;
    case 5:
      str = str.concat(String.format("%02X",byte1)+",X");
      instr_num_bytes = 2;
      break;
    case 6:
      str = str.concat(String.format("%02X",byte2)+String.format("%02X",byte1)+",Y");
      instr_num_bytes = 3;
      break;
    case 7:
      str = str.concat(String.format("%02X",byte2)+String.format("%02X",byte1)+",X");
      instr_num_bytes = 3;
      break;
    case 8:
      break;
    }
    // increment the address by the number of bytes in the instruction
    address += instr_num_bytes;
    System.out.println(str);
    return str;
  }
  //---------------------------------------------------------------------------
  public int getCurrentAddress()
  {
    return address;
  }
  //---------------------------------------------------------------------------
  private String getMnemonic(byte opcode)
  {
    return mnemonics[(int)(opcode & 0xff)];
  }
  //---------------------------------------------------------------------------
  // OPCODE text string table
  private static final String mnemonics[] =
      {
          //x0    x1     x2     x3     x4     x5     x6     x7     x8     x9     xA     xB     xC     xD     xE     xF
          "BRK", "ORA", "cop", "ora", "Tsb", "ORA", "ASL", "ora", "PHP", "ORA", "ASL", "phd", "Tsb", "ORA", "ASL", "ora", // 0x
          "BPL", "ORA", "Ora", "ora", "Trb", "ORA", "ASL", "ora", "CLC", "ORA", "Inc", "tcs", "Trb", "ORA", "ASL", "ora", // 1x
          "JSR", "AND", "jsl", "and", "BIT", "AND", "ROL", "and", "PLP", "AND", "ROL", "pld", "BIT", "AND", "ROL", "and", // 2x
          "BMI", "AND", "And", "and", "Bit", "AND", "ROL", "and", "SEC", "AND", "Dec", "tsc", "Bit", "AND", "ROL", "and", // 3x
          "RTI", "EOR", "wdm", "eor", "mvp", "EOR", "LSR", "eor", "PHA", "EOR", "LSR", "phk", "JMP", "EOR", "LSR", "eor", // 4x
          "BVC", "EOR", "Eor", "eor", "mvn", "EOR", "LSR", "eor", "CLI", "EOR", "Phy", "tcd", "jmp", "EOR", "LSR", "eor", // 5x
          "RTS", "ADC", "per", "adc", "Stz", "ADC", "ROR", "adc", "PLA", "ADC", "ROR", "rtl", "JMP", "ADC", "ROR", "adc", // 6x
          "BVS", "ADC", "Adc", "adc", "Stz", "ADC", "ROR", "adc", "SEI", "ADC", "Ply", "tdc", "Jmp", "ADC", "ROR", "adc", // 7x
          "Bra", "STA", "brl", "sta", "STY", "STA", "STX", "sta", "DEY", "Bit", "TXA", "phb", "STY", "STA", "STX", "sta", // 8x
          "BCC", "STA", "Sta", "sta", "STY", "STA", "STX", "sta", "TYA", "STA", "TXS", "txy", "Stz", "STA", "Stz", "sta", // 9x
          "LDY", "LDA", "LDX", "lda", "LDY", "LDA", "LDX", "lda", "TAY", "LDA", "TAX", "plb", "LDY", "LDA", "LDX", "lda", // ax
          "BCS", "LDA", "Lda", "lda", "LDY", "LDA", "LDX", "lda", "CLV", "LDA", "TSX", "tyx", "LDY", "LDA", "LDX", "lda", // bx
          "CPY", "CMP", "rep", "cmp", "CPY", "CMP", "DEC", "cmp", "INY", "CMP", "DEX", "wai", "CPY", "CMP", "DEC", "cmp", // cx
          "BNE", "CMP", "Cmp", "cmp", "pei", "CMP", "DEC", "cmp", "CLD", "CMP", "Phx", "stp", "jml", "CMP", "DEC", "cmp", // dx
          "CPX", "SBC", "sep", "sbc", "CPX", "SBC", "INC", "sbc", "INX", "SBC", "NOP", "xba", "CPX", "SBC", "INC", "sbc", // ex
          "BEQ", "SBC", "Sbc", "sbc", "pea", "SBC", "INC", "sbc", "SED", "SBC", "Plx", "xce", "jsr", "SBC", "INC", "sbc"  // fx
      };
}
