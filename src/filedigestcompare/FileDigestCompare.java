/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package filedigestcompare;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.StandardOpenOption;
import java.util.zip.CRC32;
import java.util.zip.Checksum;

/**
 *
 * @author Stefano Fiordi
 */
public class FileDigestCompare {

    /**
     *
     * @param packet
     * @return
     * @throws NoSuchAlgorithmException
     */
    static long CRC32Hashing(byte[] packet) {
        Checksum checksum = new CRC32();
        checksum.update(packet, 0, packet.length);
        long digest = checksum.getValue();

        return digest;
    }

    /**
     *
     * @param buf
     * @return
     */
    static byte[] createPacket(ByteBuffer buf) {
        buf.flip();
        byte[] packet = new byte[buf.remaining()];
        buf.get(packet, 0, buf.remaining());
        buf.clear();

        return packet;
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws IOException {
        File oldVersion = new File("E:/vdis/FSV.vdi"); // vecchia versione del file
        File newVersion = new File("E:/vdis/FSV 2.vdi"); // nuova versione del file
        //File downloaded = new File("E:/VMs/FSV2.vdi"); // nuovo file per scaricare la nuova versione
        int NumberOfBytes = 10 * 1024 * 1024; // dimensione dei pacchetti = 10 MB
        long oldPackets, newPackets; // numero dei pacchetti della vecchia e nuova versione
        FileChannel fOld = new FileInputStream(oldVersion).getChannel();
        FileChannel fNew = new FileInputStream(newVersion).getChannel();
        FileChannel fOutOld = FileChannel.open(oldVersion.toPath(), StandardOpenOption.WRITE);

        // calcolo del numero di pacchetti
        if (oldVersion.length() % NumberOfBytes == 0) {
            oldPackets = oldVersion.length() / NumberOfBytes;
        } else {
            oldPackets = oldVersion.length() / NumberOfBytes + 1;
        }
        if (newVersion.length() % NumberOfBytes == 0) {
            newPackets = newVersion.length() / NumberOfBytes;
        } else {
            newPackets = newVersion.length() / NumberOfBytes + 1;
        }

        ByteBuffer buf = ByteBuffer.allocate(NumberOfBytes);
        long[] newDigests = new long[(int) newPackets]; // Vettore dei digest vecchia versione
        int len = 0;
        System.out.println("Digests calculation...");
        for (int i = 0; (len = fNew.read(buf, (long) i * NumberOfBytes)) != -1; i++) {
            newDigests[i] = CRC32Hashing(createPacket(buf));
        }
        System.out.println("Digest calculation ended");

        System.out.println("Starting process...");
        long time = System.nanoTime();
        len = 0;
        int i;
        if (newVersion.length() < oldVersion.length()) {
            fOutOld.truncate(newVersion.length());
        }
        for (i = 0; (len = fOld.read(buf, (long) i * NumberOfBytes)) != -1; i++) {

            byte[] oldPacket = createPacket(buf);
            long oldDigest = CRC32Hashing(oldPacket); // calcolo del digest

            if (oldDigest != newDigests[i]) {
                ByteBuffer newBuf = ByteBuffer.allocate(NumberOfBytes);
                fNew.read(newBuf, i * NumberOfBytes);
                fOutOld.write(ByteBuffer.wrap(createPacket(newBuf)), i * NumberOfBytes);
                break;
            }
            
            float percent = 100f / newPackets * (i + 1);
            System.out.println(percent + "%");
        }
        if (newVersion.length() > oldVersion.length()) {
            for (; (len = fNew.read(buf, (long) i * NumberOfBytes)) != -1; i++) {
                byte[] newPacket = createPacket(buf);
                fOutOld.write(ByteBuffer.wrap(newPacket));
                
                float percent = 100f / newPackets * (i + 1);
                System.out.println(percent + "%");
            }
        }

        time = System.nanoTime() - time;
        System.out.println("Process ended");
        System.out.printf("Took %.3f seconds%n", time / 1e9);

        fOutOld.close();
        fNew.close();
        fOld.close();
    }

}
