package m65c02;
//-----------------------------------------------------------------------------
// simulated 65C02 class
public class M65C02
{
  long clk;
  AddressDecoder decoder;
  private byte a;
  private byte x;
  private byte y;
  private int pc;
  private byte sp;
  private byte sr;
  private static final byte N_FLAG = (byte) 0x80; // negative flag
  private static final byte V_FLAG = (byte) 0x40; // overflow flag
  private static final byte B_FLAG = (byte) 0x10; // break flag
  private static final byte D_FLAG = (byte) 0x08; // decimal mode flag
  private static final byte I_FLAG = (byte) 0x04; // interrupt disable flag
  private static final byte Z_FLAG = (byte) 0x02; // zero flag
  private static final byte C_FLAG = (byte) 0x01; // carry flag
  private static final int STACKBASE = 0x100;
  private String instruct_str;
  private String address_str;
  //---------------------------------------------------------------------------
  // constructor
  public M65C02 (AddressDecoder d)
  {
    clk = 0;
    decoder = d;
    instruct_str = null;
    address_str = null;
  }
  public void rst()
  {
    System.out.println("RST signal applied");
    pc = ((int)decoder.read(0xfffd) << 8) + (int)decoder.read(0xfffc);
    pc &= 0xffff;
    sp = (byte)0xff;
    a  = (byte)0;
    x  = (byte)0;
    y  = (byte)0;
    sr = I_FLAG;
    printCPUregs();
    execute();
  }
  //---------------------------------------------------------------------------
  // interrupt request handler
  public void irq()
  {
    // if interrupts are enabled
    if ((sr & I_FLAG) == 0)
    {
      pushword(pc);
      pc = ((int)decoder.read(0xfffe) << 8) + (int)decoder.read(0xffff);
    }
  }
  //---------------------------------------------------------------------------
  // non-maskable interrupt request handler
  public void nmi()
  {
    pushword(pc);
    pc = ((int)decoder.read(0xfffa) << 8) + (int)decoder.read(0xfffb);
  }
  //---------------------------------------------------------------------------
  // push a word value onto the stack (just used for pushing PC)
  public void pushword(int value)
  {
    push((byte)(value & 0xff));
    push((byte)(value >> 8));
  }
  //---------------------------------------------------------------------------
  // push a byte value onto the stack
  public void push(byte value)
  {
    int addr = STACKBASE+(int)sp;
    decoder.write(addr,value);
    sp--;
  }
  //---------------------------------------------------------------------------
  // pull a word value from the stack (just used for pulling PC)
  public int pullword()
  {
    int temp = pull() << 8;
    temp += pull();
    return temp;
  }
  //---------------------------------------------------------------------------
  // pull a byte value onto the stack
  public byte pull()
  {
    sp++;
    int addr = STACKBASE+(int)sp;
    return decoder.read(addr);
  }
  //---------------------------------------------------------------------------
  // execute instructions
  private void execute()
  {
    for (int i = 0; i < 1024; i++)
    {
      clk++;
      // disassemble the instruction
      byte byte0 = decoder.read(pc);
      byte byte1 = decoder.read(pc+1);
      byte byte2 = decoder.read(pc+2);
      String str = Disassembler.disassemble(pc, byte0, byte1, byte2);
      byte opcode = decoder.read(pc++);
      //System.out.println("opcode = "+String.format("%02X",opcode));
      decode(opcode);
      printCPUregs();
    }
  }
  //---------------------------------------------------------------------------
  // instruction decode logic
  private void decode(byte opcode)
  {
    if (checkSingleByteInstructions(opcode) == 0) // returns '0' if was executed
      return;
    int cc = (int)opcode & 0x03;  // part of the instruction
    switch (cc)
    {
    case 0x00:
      decodeCC00(opcode);
      break;
    case 0x01:
      decodeCC01(opcode);
      break;
    case 0x02:
      decodeCC02(opcode);
      break;
    default:
      // No instructions have the form aaabbb11. CC=11 in this case
      System.out.println("Error - unimplemented CC03 instruction!");
      System.exit(-1);
      break;
    }
  }
  //---------------------------------------------------------------------------
  // checks for the single byte instructions
  private int checkSingleByteInstructions(byte opbyte)
  {
    //System.out.println("Single byte instruction check. opcode = "+String.format("%02X", opbyte));
    /*
    The remaining instructions are probably best considered simply by listing them.
    Here are the interrupt and subroutine instructions:
      BRK JSR abs RTI RTS
      00  20  40  60
      (JSR is the only absolute-addressing instruction that doesn't fit the aaabbbcc pattern.)
      Other single-byte instructions:
      PHP   PLP   PHA   PLA   DEY   TAY   INY   INX
      08    28    48    68    88    A8    C8    E8
      CLC   SEC   CLI   SEI   TYA   CLV   CLD   SED
      18    38    58    78    98    B8    D8    F8
      TXA   TXS   TAX   TSX   DEX   NOP
      8A    9A    AA    BA    CA    EA
     */
    int opcode = opbyte & 0x000000ff;
    if      (opcode == 0x08) { php(); return 0; }
    else if (opcode == 0x28) { plp(); return 0; }
    else if (opcode == 0x48) { pha(); return 0; }
    else if (opcode == 0x68) { pla(); return 0; }
    else if (opcode == 0x88) { dey(); return 0; }
    else if (opcode == 0xa8) { tay(); return 0; }
    else if (opcode == 0xc8) { iny(); return 0; }
    else if (opcode == 0xe8) { inx(); return 0; }
    else if (opcode == 0x18) { clc(); return 0; }
    else if (opcode == 0x38) { sec(); return 0; }
    else if (opcode == 0x58) { cli(); return 0; }
    else if (opcode == 0x78) { sei(); return 0; }
    else if (opcode == 0x98) { tya(); return 0; }
    else if (opcode == 0xb8) { clv(); return 0; }
    else if (opcode == 0xd8) { cld(); return 0; }
    else if (opcode == 0xf8) { sed(); return 0; }
    else if (opcode == 0x8a) { txa(); return 0; }
    else if (opcode == 0x9a) { txs(); return 0; }
    else if (opcode == 0xaa) { tax(); return 0; }
    else if (opcode == 0xba) { tsx(); return 0; }
    else if (opcode == 0xca) { dex(); return 0; }
    else if (opcode == 0xea) { nop(); return 0; }
    else if (opcode == 0x80) { bra(); return 0; }
    return -1; // not executed, keep decoding
  }
  //---------------------------------------------------------------------------
  // instruction decode logic - CC = 01
  private void decodeCC01(byte opcode)
  {
    int aaa = ((int)opcode & 0xe0) >> 5;  // instruction
    int bbb = ((int)opcode & 0x1c) >> 2;  // addressing mode
    /*
    aaa  opcode
    000 ORA
    001 AND
    010 EOR
    011 ADC
    100 STA
    101 LDA
    110 CMP
    111 SBC
     */
    switch (aaa)
    {
    case 0:
      ora(bbb);
      break;
    case 1:
      and(bbb);
      break;
    case 2:
      eor(bbb);
      break;
    case 3:
      adc(bbb);
      break;
    case 4:
      sta(bbb);
      break;
    case 5:
      lda(bbb);
      break;
    case 6:
      cmp(bbb);
      break;
    case 7:
      sbc(bbb);
      break;
    default:
      System.out.println("Error - bad CC01 instruction decode");
      System.exit(-1);
    }
  }
  //---------------------------------------------------------------------------
  // instruction decode logic - CC = 02
  private void decodeCC02(byte opcode)
  {
    int aaa = ((int)opcode & 0xe0) >> 5;  // instruction
      int bbb = ((int)opcode & 0x1c) >> 2;  // addressing mode
    /*
    aaa opcode
    000 ASL
    001 ROL
    010 LSR
    011 ROR
    100 STX
    101 LDX
    110 DEC
    111 INC
     */
    switch (aaa)
    {
    case 0:
      asl(bbb);
      break;
    case 1:
      rol(bbb);
      break;
    case 2:
      lsr(bbb);
      break;
    case 3:
      ror(bbb);
      break;
    case 4:
      stx(bbb);
      break;
    case 5:
      ldx(bbb);
      break;
    case 6:
      dec(bbb);
      break;
    case 7:
      inc(bbb);
      break;
    default:
      System.out.println("Error - CC02 instruction decode");
      System.exit(-1);
    }
  }
  //---------------------------------------------------------------------------
  // instruction decode logic - CC = 00
  private void decodeCC00(byte opcode)
  {
    int aaa = ((int)opcode & 0xe0) >> 5;  // instruction
    int bbb = ((int)opcode & 0x1c) >> 2;  // addressing mode
    // The conditional branch instructions all have the form xxy10000. (aaa;bbb=4;cc=00)
    if (bbb == 4)
    {
      branchInstructions(opcode);
      return;
    }
    /*
    aaa opcode
    001 BIT
    010 JMP
    011 JMP (abs)
    100 STY
    101 LDY
    110 CPY
    111 CPX
     */
    switch (aaa)
    {
    case 0:
      brk();
      break;
    case 1:
      bit(bbb);
      break;
    case 2:
      jmp(bbb);
      break;
    case 3:
      jmpAbs(bbb);
      break;
    case 4:
      sty(bbb);
      break;
    case 5:
      ldy(bbb);
      break;
    case 6:
      cpy(bbb);
      break;
    case 7:
      cpx(bbb);
      break;
    default:
      System.out.println("Error - bad CC00 instruction decode");
      System.exit(-1);
    }
  }
  //---------------------------------------------------------------------------
  // Conditional branch instructions
  private void branchInstructions(byte opcode)
  {
    boolean branch_taken = false;
    System.out.println("branch check! sr = "+String.format("%02X", sr));
    /* aaabbbcc - aaa = xxy; bbb = 100; cc = 00
    The flag indicated by xx is compared with y, and the branch is taken if they are equal.
    xx  flag
    00  negative
    01  overflow
    10  carry
    11  zero
    This gives the following branches:
    BPL BMI BVC BVS BCC BCS BNE BEQ
    10  30  50  70  90  B0  D0  F0
     */
    address_str = new String(" "+String.format("%02X", decoder.read(pc)));
    if (opcode == (byte)0x10 && (sr & N_FLAG) == 0) branch_taken = takeBranch();           // BPL - branch of plus
    else if (opcode == (byte)0x30 && (sr & N_FLAG) == N_FLAG) branch_taken = takeBranch(); // BMI - branch if minus
    else if (opcode == (byte)0x50 && (sr & V_FLAG) == 0) branch_taken = takeBranch();      // BVC - branch if no overflow
    else if (opcode == (byte)0x70 && (sr & V_FLAG) == V_FLAG) branch_taken = takeBranch(); // BVS - branch if overflow
    else if (opcode == (byte)0x90 && (sr & C_FLAG) == 0) branch_taken = takeBranch();      // BCC - branch if no carry
    else if (opcode == (byte)0xb0 && (sr & C_FLAG) == C_FLAG) branch_taken = takeBranch(); // BCS - branch if carry
    else if (opcode == (byte)0xd0 && (sr & Z_FLAG) == 0) branch_taken = takeBranch();      // BNE - branch if not equal
    else if (opcode == (byte)0xf0 && (sr & Z_FLAG) == Z_FLAG) branch_taken = takeBranch(); // BEQ - branch if equal
    if (branch_taken == false)
      pc++;
  }
  //---------------------------------------------------------------------------
  // takes a branch - returns true to show that a branch was taken
  private boolean takeBranch()
  {
    int branch = decoder.read(pc++);
    //System.out.println("takeBranch - PC before = "+String.format("%04X", pc));
    pc += branch; // after reading the branch value the PC should be pointing to the next normal instruction
    //System.out.println("takeBranch - PC after = "+String.format("%04X", pc));
    // the PC is then added to the value read, which may be negative and result in a subtraction
    return true; // branch was taken
  }
  //---------------------------------------------------------------------------
  // decode address mode and read value from memory
  private byte readFromBBB(int bbb)
  {
    /*
    And the addressing mode (bbb) bits:
      bbb addressing mode
      000 (zero page,X)
      001 zero page
      010 #immediate
      011 absolute
      100 (zero page),Y
      101 zero page,X
      110 absolute,Y
      111 absolute,X
     */
    byte value = 0;
    if (bbb == 0) // (zpg,X)
    {
      System.out.println("(ZPAGE,X)!");
      int zpg = decoder.read(pc++);
      zpg = (zpg + x) & 0xff;
      int addr = (int)decoder.read(zpg) + (int)((int)decoder.read(zpg+1)<<8);
      value = decoder.read(addr);
    }
    else if (bbb == 1) // zero page
    {
      int zpg = decoder.read(pc++);
      System.out.println("ZPAGE! ("+String.format("%02X", zpg)+")");
      value = decoder.read(zpg);
    }
    else if (bbb == 2) // immediate
    {
      System.out.println("IMMEDIATE!");
      byte imm_value = decoder.read(pc++);
      value = imm_value;
    }
    else if (bbb == 3) // absolute address
    {
      System.out.println("ABSOLUTE!");
      int addr = (int)decoder.read(pc++) + (int)((int)decoder.read(pc++)<<8);
      value = decoder.read(addr);
    }
    else if (bbb == 4) // (zpg),Y
    {
      int zpg = decoder.read(pc++);
      int addr = (int)decoder.read(zpg) + (int)((int)decoder.read(zpg+1)<<8);
      addr = (addr + y) & 0xffff;
      value = decoder.read(addr);
    }
    else if (bbb == 5) // zpg,X
    {
      int zpg = decoder.read(pc++);
      zpg = (zpg + x) & 0xff;
      value = decoder.read(zpg);
    }
    else if (bbb == 6) // absolute,Y
    {
      int addr = (int)decoder.read(pc++) + (int)((int)decoder.read(pc++)<<8);
      addr = (addr + y) & 0xffff;
      value = decoder.read(addr);
    }
    else if (bbb == 7) // absolute,X
    {
      int addr = (int)decoder.read(pc++) + (int)((int)decoder.read(pc++)<<8);
      addr = (addr + x) & 0xffff;
      value = decoder.read(addr);
    }
    else
      System.exit(-1);
    return value;
  }
  //---------------------------------------------------------------------------
  // decode address mode and read value from memory (for READ-MODIFY-WRITE instructions)
  private byte readRmwFromBBB(int bbb)
  {
    /*
    And the addressing mode (bbb) bits:
      bbb addressing mode
      000 (zero page,X)
      001 zero page
      010 #immediate
      011 absolute
      100 (zero page),Y
      101 zero page,X
      110 absolute,Y
      111 absolute,X
     */
    byte value = 0;
    if (bbb == 0) // (zpg,X)
    {
      System.out.println("(ZPAGE,X)!");
      int zpg = decoder.read(pc);
      zpg = (zpg + x) & 0xff;
      int addr = (int)decoder.read(zpg) + (int)((int)decoder.read(zpg+1)<<8);
      value = decoder.read(addr);
    }
    else if (bbb == 1) // zero page
    {
      int zpg = decoder.read(pc);
      System.out.println("ZPAGE! ("+String.format("%02X", zpg)+")");
      value = decoder.read(zpg);
    }
    else if (bbb == 2) // immediate
    {
      System.out.println("IMMEDIATE!");
      byte imm_value = decoder.read(pc);
      value = imm_value;
    }
    else if (bbb == 3) // absolute address
    {
      System.out.println("ABSOLUTE!");
      int addr = (int)decoder.read(pc) + (int)((int)decoder.read(pc+1)<<8);
      value = decoder.read(addr);
    }
    else if (bbb == 4) // (zpg),Y
    {
      int zpg = decoder.read(pc);
      int addr = (int)decoder.read(zpg) + (int)((int)decoder.read(zpg+1)<<8);
      addr = (addr + y) & 0xffff;
      value = decoder.read(addr);
    }
    else if (bbb == 5) // zpg,X
    {
      int zpg = decoder.read(pc);
      zpg = (zpg + x) & 0xff;
      value = decoder.read(zpg);
    }
    else if (bbb == 6) // absolute,Y
    {
      int addr = (int)decoder.read(pc) + (int)((int)decoder.read(pc+1)<<8);
      addr = (addr + y) & 0xffff;
      value = decoder.read(addr);
    }
    else if (bbb == 7) // absolute,X
    {
      int addr = (int)decoder.read(pc) + (int)((int)decoder.read(pc+1)<<8);
      addr = (addr + x) & 0xffff;
      value = decoder.read(addr);
    }
    else
      System.exit(-1);
    return value;
  }
  //---------------------------------------------------------------------------
  // decode address mode and write value to memory
  private void writeToBBB(int bbb, byte value)
  {
    /*
    And the addressing mode (bbb) bits:
      bbb addressing mode
      000 (zero page,X)
      001 zero page
      010 #immediate
      011 absolute
      100 (zero page),Y
      101 zero page,X
      110 absolute,Y
      111 absolute,X
     */
    if (bbb == 0) // (zpg,X)
    {
      int zpg = decoder.read(pc++);
      zpg = (zpg + x) & 0xff;
      int addr = (int)decoder.read(zpg) + (int)((int)decoder.read(zpg+1)<<8);
      decoder.write(addr,value);
    }
    else if (bbb == 1) // zero page
    {
      int zpg = decoder.read(pc++);
      decoder.write(zpg,value);
    }
    else if (bbb == 3) // absolute address
    {
      int addr = (int)decoder.read(pc++) + (int)((int)decoder.read(pc++)<<8);
      decoder.write(addr,value);
    }
    else if (bbb == 4) // (zpg),Y
    {
      int zpg = decoder.read(pc++);
      int addr = (int)decoder.read(zpg) + (int)((int)decoder.read(zpg+1)<<8);
      addr = (addr + y) & 0xffff;
      decoder.write(addr,value);
    }
    else if (bbb == 5) // zpg,X
    {
      int zpg = decoder.read(pc++);
      zpg = (zpg + x) & 0xff;
      decoder.write(zpg,value);
    }
    else if (bbb == 6) // absolute,Y
    {
      int addr = (int)decoder.read(pc++) + (int)((int)decoder.read(pc++)<<8);
      addr = (addr + y) & 0xffff;
      decoder.write(addr,value);
    }
    else if (bbb == 7) // absolute,X
    {
      int addr = (int)decoder.read(pc++) + (int)((int)decoder.read(pc++)<<8);
      addr = (addr + x) & 0xffff;
      decoder.write(addr,value);
    }
    else
      System.exit(-1);
    return;
  }
  //---------------------------------------------------------------------------
  // sets or clears the Z and N flags
  private void setFlagsZN(byte v)
  {
    if (v == 0x00)  sr |= Z_FLAG;
    else sr &= ~Z_FLAG;
    if ((v & 0x80) != 0) sr |= N_FLAG;
    else sr &= ~N_FLAG;
  }
  //---------------------------------------------------------------------------
  // sets or clears the C and V flags
  private void setFlagsCV(int v)
  {
    if (v >= 0x100) sr |= C_FLAG;
    if (v < 0x100)  sr &= ~C_FLAG;
    if (v < 0x200)  sr &= ~V_FLAG;
    if (v >= 0x200) sr |= V_FLAG;
  }
  //---------------------------------------------------------------------------
  // sets or clears the C flag
  private void setFlagsC(int v)
  {
    if (v >= 0x100) sr |= C_FLAG;
    if (v < 0x100)  sr &= ~C_FLAG;
  }
  //---------------------------------------------------------------------------
  // ORA instruction
  private void ora(int bbb)
  {
    a |= readFromBBB(bbb);
    setFlagsZN(a);
  }
  //---------------------------------------------------------------------------
  // AND instruction
  private void and(int bbb)
  {
    a &= readFromBBB(bbb);
    setFlagsZN(a);
  }
  //---------------------------------------------------------------------------
  // EOR instruction
  private void eor(int bbb)
  {
    a ^= readFromBBB(bbb);
    setFlagsZN(a);
  }
  //---------------------------------------------------------------------------
  // ADC instruction
  private void adc(int bbb)
  {
    int temp = a;
    temp += readFromBBB(bbb);
    if ((sr & C_FLAG) == C_FLAG)
      temp += 1;
    setFlagsZN((byte)temp);
    if (temp >= 0x100) sr |= C_FLAG;
    if (temp >= 0x200) sr |= V_FLAG;
    temp &= 0xff;
    a = (byte)temp;
  }
  //---------------------------------------------------------------------------
  // STA instruction
  private void sta(int bbb)
  {
    writeToBBB(bbb,a);
  }
  //---------------------------------------------------------------------------
  // LDA instruction
  private void lda(int bbb)
  {
    a = readFromBBB(bbb);
    setFlagsZN(a);
  }
  //---------------------------------------------------------------------------
  // CMP instruction
  private void cmp(int bbb)
  {
    compareBBB(bbb,a);
  }
  //---------------------------------------------------------------------------
  // SBC instruction
  private void sbc(int bbb)
  {
    int temp = (int)a;
    if ((sr & C_FLAG) == C_FLAG) // add the borrow flag
      temp += 0x100;
    temp -= readFromBBB(bbb);
    setFlagsZN((byte)temp);
    setFlagsCV(temp);
    a = (byte)temp;
  }
  //---------------------------------------------------------------------------
  // ASL instruction
  private void asl(int bbb)
  {
    int temp = readFromBBB(bbb);
    temp <<= 1;
    setFlagsZN((byte)temp);
    setFlagsC(temp);
    temp &= 0xff;
    writeToBBB(bbb,(byte)temp);
  }
  //---------------------------------------------------------------------------
  // ROL instruction
  private void rol(int bbb)
  {
    int temp = readFromBBB(bbb);
    int carry = 0;
    if ((sr & C_FLAG) == C_FLAG) carry = 1;
    temp <<= 1;
    temp |= carry;
    if (temp >= 0x100)
      sr |= C_FLAG;
    else
      sr &= ~C_FLAG;
    if ((temp & 0x80) != 0) sr |= N_FLAG;
    else sr &= ~N_FLAG;
    if (temp == 0) sr |= Z_FLAG;
    else sr &= ~Z_FLAG;
    temp &= 0xff;
    writeToBBB(bbb,(byte)temp);
  }
  //---------------------------------------------------------------------------
  // LSR instruction
  private void lsr(int bbb)
  {
    int temp = readFromBBB(bbb);
    if ((temp & 0x01) != 0) sr |= C_FLAG;
    else sr &= ~C_FLAG;
    temp >>= 1;
    sr &= ~N_FLAG; // always set sign to zero
    if (temp == 0) sr |= Z_FLAG;
    else sr &= ~Z_FLAG;
    temp &= 0xff;
    writeToBBB(bbb,(byte)temp);
  }
  //---------------------------------------------------------------------------
  // ROR instruction
  private void ror(int bbb)
  {
    int temp = readFromBBB(bbb);
    int carry = 0;
    if ((temp & 0x01) != 0) carry = 1;
    temp >>= 1;
      if (carry == 1)
        temp |= 0x80;;
        if ((temp & 0x80) != 0) sr |= N_FLAG;
        else sr &= ~N_FLAG;
        if (temp == 0) sr |= Z_FLAG;
        else sr &= ~Z_FLAG;
        temp &= 0xff;
        writeToBBB(bbb,(byte)temp);
  }
  //---------------------------------------------------------------------------
  // STX instruction
  private void stx(int bbb)
  {
    writeToBBB(bbb,x);
  }
  //---------------------------------------------------------------------------
  // LDX instruction
  private void ldx(int bbb)
  {
    x = readFromBBB(bbb);
    if (x == 0x00)  sr |= Z_FLAG;
    else sr &= ~Z_FLAG;
    if ((x & 0x80) != 0) sr |= N_FLAG;
    else sr &= ~N_FLAG;
  }
  //---------------------------------------------------------------------------
  // DEC instruction
  private void dec(int bbb)
  {
    int temp = readRmwFromBBB(bbb);
    temp--;
    temp &= 0xff;
    if (temp == 0x00)  sr |= Z_FLAG;
    else sr &= ~Z_FLAG;
    if ((temp & 0x80) != 0) sr |= N_FLAG;
    writeToBBB(bbb,(byte)temp);
  }
  //---------------------------------------------------------------------------
  // INC instruction
  private void inc(int bbb)
  {
    int temp = readRmwFromBBB(bbb);
    temp++;
    temp &= 0xff;
    if (temp == 0x00)  sr |= Z_FLAG;
    else sr &= ~Z_FLAG;
    if ((temp & 0x80) != 0) sr |= N_FLAG;
    else sr &= ~N_FLAG;
    writeToBBB(bbb,(byte)temp);
    System.out.println("INC - byte = "+String.format("%02X", (byte)temp));
  }
  //---------------------------------------------------------------------------
  // BIT instruction
  private void bit(int bbb)
  {
    //    OP LEN CYC MODE  FLAGS    SYNTAX
    //    -- --- --- ----  ------   ------
    //    89 2   2   imm   ......Z. BIT #$12
    //    34 2   4   zp,X  NV....Z. BIT $34,X
    //    3C 3   4 a abs,X NV....Z. BIT $5678,X
    int temp = readFromBBB(bbb) & a;
    sr &= ~(N_FLAG | V_FLAG | Z_FLAG); // turn off status bits
    if (temp == 0)
      sr |= Z_FLAG;
    if (bbb != 2) // modes other than immediate set more flags
    {
      if ((temp & N_FLAG) != 0)
        sr |= N_FLAG;
      if ((temp & V_FLAG) != 0)
        sr |= V_FLAG;
    }
  }
  //---------------------------------------------------------------------------
  // JMP instruction
  private void jmp(int bbb)
  {
    int addr = decoder.read(pc++);
    addr += decoder.read(pc++)<<8;
    addr &= 0xffff;
    System.out.println("JMP "+String.format("%04X", addr));
    pc = addr;
  }
  //---------------------------------------------------------------------------
  // JMP (abs) instruction
  private void jmpAbs(int bbb)
  {
    int addr = decoder.read(pc++);
    addr += decoder.read(pc++)<<8;
    addr &= 0xffff;
    pc = decoder.read(addr) + decoder.read(addr+1);
  }
  //---------------------------------------------------------------------------
  // STY instruction
  private void sty(int bbb)
  {
    writeToBBB(bbb,y);
  }
  //---------------------------------------------------------------------------
  // LDY instruction
  private void ldy(int bbb)
  {
    y = readFromBBB(bbb);
    if (y == 0x00)  sr |= Z_FLAG;
    else sr &= ~Z_FLAG;
    if ((y & 0x80) != 0) sr |= N_FLAG;
    else sr &= ~N_FLAG;
  }
  //---------------------------------------------------------------------------
  // CPY instruction
  private void cpy(int bbb)
  {
    compareBBB(bbb,y);
  }
  //---------------------------------------------------------------------------
  // CPX instruction
  private void cpx(int bbb)
  {
    compareBBB(bbb,x);
  }
  //---------------------------------------------------------------------------
  // compares a register to the memory pointed to by BBB
  private void compareBBB(int bbb, byte reg)
  {
    int temp_reg = (int)reg;
    if ((sr & C_FLAG) == C_FLAG) // add carry if carry is set
      temp_reg += 0x100;
    // clear all statuses
    sr &= ~(N_FLAG | V_FLAG | Z_FLAG | C_FLAG);
    byte temp_mem = readFromBBB(bbb);
    temp_reg -= temp_mem; // non-destructive subtraction REG - MEM
    if (temp_reg == 0x00)
      sr |= Z_FLAG;
    if (temp_reg >= 0x80)
      sr |= N_FLAG;
    if (temp_reg >= 0x100) // set carry if no borrow occurred
      sr |= C_FLAG;
  }
  //---------------------------------------------------------------------------
  // BRK instruction
  private void brk()
  {
    push(sr |= B_FLAG); // set break condition flag
    sr |= I_FLAG;       // disable interrupts
    pushword(pc++);
    irq();              // handle software interrupt
  }
  //---------------------------------------------------------------------------
  // PHP instruction
  private void php()
  {
    push(sr);
  }
  //---------------------------------------------------------------------------
  // PLP instruction
  private void plp()
  {
    sr = pull();
  }
  //---------------------------------------------------------------------------
  // PHA instruction
  private void pha()
  {
    push(a);
  }
  //---------------------------------------------------------------------------
  // PLA instruction
  private void pla()
  {
    a = pull();
    if (a == 0x00)  sr |= Z_FLAG;
    else sr &= ~Z_FLAG;
    if ((a & 0x80) != 0) sr |= N_FLAG;
    else sr &= ~N_FLAG;
  }
  //---------------------------------------------------------------------------
  // DEY instruction
  private void dey()
  {
    y--;
    y &= 0xff;
    if (y == 0x00)  sr |= Z_FLAG;
    else sr &= ~Z_FLAG;
    if ((y & 0x80) != 0) sr |= N_FLAG;
    else sr &= ~N_FLAG;
  }
  //---------------------------------------------------------------------------
  // TAY instruction
  private void tay()
  {
    y = a;
    if (y == 0x00)  sr |= Z_FLAG;
    else sr &= ~Z_FLAG;
    if ((y & 0x80) != 0) sr |= N_FLAG;
    else sr &= ~N_FLAG;
  }
  //---------------------------------------------------------------------------
  // INY instruction
  private void iny()
  {
    y++;
    y &= 0xff;
    if (y == 0x00)  sr |= Z_FLAG;
    else sr &= ~Z_FLAG;
    if ((y & 0x80) != 0) sr |= N_FLAG;
    else sr &= ~N_FLAG;
  }
  //---------------------------------------------------------------------------
  // INX instruction
  private void inx()
  {
    x++;
    x &= 0xff;
    if (x == 0x00)  sr |= Z_FLAG;
    else sr &= ~Z_FLAG;
    if ((x & 0x80) != 0) sr |= N_FLAG;
    else sr &= ~N_FLAG;
  }
  //---------------------------------------------------------------------------
  // CLC instruction
  private void clc()
  {
    sr &= ~C_FLAG;
  }
  //---------------------------------------------------------------------------
  // SEC instruction
  private void sec()
  {
    sr |= C_FLAG;
  }
  //---------------------------------------------------------------------------
  // CLI instruction
  private void cli()
  {
    sr &= ~I_FLAG;
  }
  //---------------------------------------------------------------------------
  // SEI instruction
  private void sei()
  {
    sr |= I_FLAG;
  }
  //---------------------------------------------------------------------------
  // TYA instruction
  private void tya()
  {
    a = y;
    if (a == 0x00)  sr |= Z_FLAG;
    else sr &= ~Z_FLAG;
    if ((a & 0x80) != 0) sr |= N_FLAG;
    else sr &= ~N_FLAG;
  }
  //---------------------------------------------------------------------------
  // CLV instruction
  private void clv()
  {
    sr &= ~V_FLAG;
  }
  //---------------------------------------------------------------------------
  // CLD instruction
  private void cld()
  {
    sr &= ~D_FLAG;
  }
  //---------------------------------------------------------------------------
  // SED instruction
  private void sed()
  {
    sr |= D_FLAG;
  }
  //---------------------------------------------------------------------------
  // TXA instruction
  private void txa()
  {
    a = x;
    if (a == 0x00)  sr |= Z_FLAG;
    else sr &= ~Z_FLAG;
    if ((a & 0x80) != 0) sr |= N_FLAG;
    else sr &= ~N_FLAG;
  }
  //---------------------------------------------------------------------------
  // TXS instruction
  private void txs()
  {
    sp = x;
  }
  //---------------------------------------------------------------------------
  // TAX instruction
  private void tax()
  {
    x = a;
    if (x == 0x00)  sr |= Z_FLAG;
    else sr &= ~Z_FLAG;
    if ((x & 0x80) != 0) sr |= N_FLAG;
    else sr &= ~N_FLAG;
  }
  //---------------------------------------------------------------------------
  // TSX instruction
  private void tsx()
  {
    x = sp;
    if (x == 0x00)  sr |= Z_FLAG;
    else sr &= ~Z_FLAG;
    if ((x & 0x80) != 0) sr |= N_FLAG;
    else sr &= ~N_FLAG;
  }
  //---------------------------------------------------------------------------
  // DEX instruction
  private void dex()
  {
    x--;
    x &= 0xff;
    if (x == 0x00)  sr |= Z_FLAG;
    else sr &= ~Z_FLAG;
    if ((x & 0x80) != 0) sr |= N_FLAG;
    else sr &= ~N_FLAG;
  }
  //---------------------------------------------------------------------------
  // NOP instruction
  private void nop()
  {
  }
  //---------------------------------------------------------------------------
  // BRA instruction
  private void bra()
  {
    int operand = decoder.read(pc);
    System.out.println("BRA - operand = "+String.format("%02X", operand));
    takeBranch();
  }


  @SuppressWarnings("unused")
  private void Test(byte opcode)
  {
    if (opcode == 0)
      System.out.println("M65C02.execute - opcode = "+String.format("%02X", opcode));
    if (opcode == (byte)0x72)
    {
      sr |= I_FLAG;
      System.out.println("  instruction = SEI, PC = "+String.format("%04X", pc)+
          " SR = "+String.format("%02X", sr));
    }
    else if (opcode == (byte)0xA9)
    {
      byte operand = decoder.read(pc++);
      a = operand;
      System.out.println("  instruction = LDA #"+String.format("%02X", operand)+
          ", PC = "+String.format("%04X", pc)+
          " SR = "+String.format("%02X", sr));
    }
    else if (opcode == (byte)0x4c)
    {
      int addr = decoder.read(pc++);
      addr += decoder.read(pc++) << 8;
      addr &= 0xffff;
      pc = addr;
      System.out.println("  instruction = JMP "+String.format("%04X", addr)+
          ", PC = "+String.format("%04X", pc)+
          " SR = "+String.format("%02X", sr));
    }
    else
    {
      System.exit(-1);
    }
  }
  //---------------------------------------------------------------------------
  // prints the CPU registers
  private void printCPUregs()
  {
    System.out.print("CPU REGS: ");
    System.out.print("PC: "+String.format("%04X", pc)+
        " SP: "+String.format("%02X", sp)+
        " A: "+String.format("%02X", a)+
        " X: "+String.format("%02X", x)+
        " Y: "+String.format("%02X", y)
        );
    System.out.print(" SR(NV-BDIZC): ");
    if ((sr & N_FLAG) == N_FLAG) System.out.print("N"); else System.out.print("-");
    if ((sr & V_FLAG) == V_FLAG) System.out.print("V"); else System.out.print("-");
    System.out.print("-"); // no flag here in this bit position
    if ((sr & B_FLAG) == V_FLAG) System.out.print("B"); else System.out.print("-");
    if ((sr & D_FLAG) == D_FLAG) System.out.print("D"); else System.out.print("-");
    if ((sr & I_FLAG) == I_FLAG) System.out.print("I"); else System.out.print("-");
    if ((sr & Z_FLAG) == Z_FLAG) System.out.print("Z"); else System.out.print("-");
    if ((sr & C_FLAG) == C_FLAG) System.out.print("C"); else System.out.print("-");
    System.out.println("");
  }
}
