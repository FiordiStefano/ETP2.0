/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package filedigestcompare;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.StandardOpenOption;
import java.util.zip.CRC32;
import java.util.zip.Checksum;

/**
 * Comparazione tra file ed aggiornamento attraverso confronto tra digest dei file
 * @author Stefano Fiordi
 */
public class FileDigestCompare {

    /**
     * Crea il digest CRC32 di un array binario
     * @param packet array binario
     * @return il digest
     * @throws NoSuchAlgorithmException
     */
    static long CRC32Hashing(byte[] packet) {
        Checksum checksum = new CRC32();
        checksum.update(packet, 0, packet.length);
        long digest = checksum.getValue();

        return digest;
    }

    /**
     * Crea il pacchetto array binario attraverso un buffer binario
     * @param buf buffer binario
     * @return il pacchetto array binario
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
        int NumberOfBytes = 10 * 1024 * 1024; // dimensione dei pacchetti = 10 MB
        long oldPackets, newPackets; // numero dei pacchetti della vecchia e nuova versione
        FileChannel fOld = new FileInputStream(oldVersion).getChannel(); // Canale di lettura del vecchio file
        FileChannel fNew = new FileInputStream(newVersion).getChannel(); // Canale di lettura del nuovo file
        FileChannel fOutOld = FileChannel.open(oldVersion.toPath(), StandardOpenOption.WRITE); // Canale di scrittura sul vecchio file in append

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

        ByteBuffer buf = ByteBuffer.allocate(NumberOfBytes); // Buffer di appoggio per i canali di lettura/scrittura
        long[] newDigests = new long[(int) newPackets]; // Vettore digest nuovo file
        int len = 0;
        System.out.println("Digests calculation...");
        for (int i = 0; (len = fNew.read(buf, (long) i * NumberOfBytes)) != -1; i++) {
            newDigests[i] = CRC32Hashing(createPacket(buf)); // calcolo dell'hashing cRC32
        }
        System.out.println("Digest calculation ended");

        System.out.println("Starting process...");
        long time = System.nanoTime();
        len = 0;
        int i;
        // troncamento del vecchio file in caso la dimensione sia suoeriore a quella del nuovo
        if (newVersion.length() < oldVersion.length()) {
            fOutOld.truncate(newVersion.length());
        }
        for (i = 0; (len = fOld.read(buf, (long) i * NumberOfBytes)) != -1; i++) {

            byte[] oldPacket = createPacket(buf);
            long oldDigest = CRC32Hashing(oldPacket); // calcolo del digest
            // se il digest del pacchetto del vecchio file è diverso da quello del nuovo viene sostituito con il pacchetto di quest'ultimo
            if (oldDigest != newDigests[i]) {
                ByteBuffer newBuf = ByteBuffer.allocate(NumberOfBytes);
                fNew.read(newBuf, i * NumberOfBytes);
                fOutOld.write(ByteBuffer.wrap(createPacket(newBuf)), i * NumberOfBytes); // sovrascrivo la parte del vecchio file con il pacchetto corrispondente del nuovo
                break;
            }
            
            float percent = 100f / newPackets * (i + 1); // progresso in percentuale
            System.out.println(percent + "%");
        }
        // aggiunta dei pacchetti in più del nuovo file rispetto al vecchio in caso questo sia più piccolo
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

        // chiusura canali
        fOutOld.close();
        fNew.close();
        fOld.close();
    }

}
