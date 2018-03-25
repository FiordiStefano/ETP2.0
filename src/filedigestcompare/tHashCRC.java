/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package filedigestcompare;

import java.nio.ByteBuffer;
import java.util.zip.CRC32;
import java.util.zip.Checksum;

/**
 *
 * @author Stefano Fiordi
 */
public class tHashCRC implements Runnable {
    
    ByteBuffer buf; 
    int index;
    long[] newDigests;

    public tHashCRC(ByteBuffer buf, int index, long[] newDigests) {
        this.buf = ByteBuffer.wrap(buf.array());
        this.index = index;
        this.newDigests = newDigests;
    }
    
    @Override
    public void run() {
        newDigests[index] = CRC32Hashing(createPacket(buf)); // calcolo dell'hashing CRC32
    }
    
    /**
     * Crea il digest CRC32 di un array binario
     * @param packet array binario
     * @return il digest
     * @throws NoSuchAlgorithmException
     */
    static long CRC32Hashing(byte[] packet) {
        Checksum checksum = new CRC32();
        checksum.update(packet, 0, packet.length);

        return checksum.getValue();
    }
    
    /**
     * Crea il pacchetto array binario attraverso un buffer binario
     * @param buf buffer binario
     * @return il pacchetto array binario
     */
    static byte[] createPacket(ByteBuffer buf) {
        //buf.flip() 
        byte[] packet = new byte[buf.remaining()];
        buf.get(packet);
        //buf.clear();

        return packet;
    }
}
