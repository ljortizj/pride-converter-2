package uk.ac.ebi.pride.tools.converter.utils;

import org.apache.log4j.Logger;

import java.io.*;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.zip.GZIPInputStream;

/**
 * Created by IntelliJ IDEA.
 * User: rcote
 * Date: 24-Aug-2010
 * Time: 17:09:47
 */
public class FileUtils {

    public final static String fasta = ".fasta";
    public final static String dat = ".dat";
    public final static String txt = ".txt";
    public final static String pkl = ".pkl";
    public final static String dta = ".dta";
    public final static String mgf = ".mgf";
    public final static String xml = ".xml";
    public final static String mzML = ".mzml";
    public final static String mzXML = ".mzxml";
    public final static String mzData = ".mzdata";
    public final static String mzIdentML = ".mzid";
    public final static String ms2 = ".ms2";
    public final static String gz = ".gz";
    public static final String FILE_SEPARATOR = System.getProperty("file.separator");
    public static final String TEMP_DIR = System.getProperty("java.io.tmpdir");

    private static final Logger logger = Logger.getLogger(FileUtils.class);

    /**
     * Will move file from one path to another and returns the absolute path of the newly moved file
     *
     * @param sourcePath  - the full path to the file
     * @param destDirPath - the destination directory of the file
     * @throws IOException - will throw an error if the the file is not successfully moved
     */
    public static String moveFile(String sourcePath, String destDirPath) throws IOException {

        //copy to new location
        File currentFile = new File(sourcePath);
        if (currentFile.exists()) {

            File destDir = new File(destDirPath);
            if (destDir.exists() && destDir.canWrite()) {

                File destFile = new File(destDir, currentFile.getName());
                boolean success = currentFile.renameTo(destFile);

                if (success) {
                    logger.info(currentFile.getName() + " file moved to " + destDirPath);
                    return destFile.getAbsolutePath();
                } else {
                    throw new IOException("Could not copy file from " + sourcePath + " to " + destFile.getAbsolutePath());
                }

            } else {
                throw new IOException(destDirPath + " not found or not writeable!");
            }

        } else {
            throw new FileNotFoundException(currentFile.getAbsolutePath() + " not found!");
        }

    }

    public static String MD5Hash(String filePath) {
        return makeHash("MD5", filePath);
    }

    public static String SHA1Hash(String filePath) {
        return makeHash("SHA-1", filePath);
    }

    private static String makeHash(String hashMethod, String filePath) {

        // initialize the hash algorithm
        MessageDigest hash;
        try {
            hash = MessageDigest.getInstance(hashMethod);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(hashMethod + " not recognized as Secure Hash Algorithm.", e);
        }

        // create the input stream that will calculate the checksum
        FileInputStream fis;
        try {
            fis = new FileInputStream(filePath);
        } catch (FileNotFoundException e) {
            throw new IllegalStateException("File " + filePath + " could not be found!", e);
        }
        BufferedInputStream bis = new BufferedInputStream(fis);
        DigestInputStream dis = new DigestInputStream(bis, hash);

        // prepare for input stream processing
        byte[] b = new byte[1024]; // we only read one byte at a time
        try {
            while (dis.read(b) >= 0) {
            }
            dis.close();
        } catch (IOException e) {
            throw new IllegalStateException("Could not read from file '" + filePath +
                    "' while trying ot calculate hash.", e);
        }

        byte[] bytesDigest = dis.getMessageDigest().digest();

        return asHex(bytesDigest);

    }

    private static final char[] HEX_CHARS = "0123456789abcdef".toCharArray();

    private static String asHex(byte[] bytesDigest) {
        // from: http://forums.xkcd.com/viewtopic.php?f=11&t=16666&p=553936
        char[] chars = new char[2 * bytesDigest.length];
        for (int i = 0; i < bytesDigest.length; ++i) {
            chars[2 * i] = HEX_CHARS[(bytesDigest[i] & 0xF0) >>> 4];
            chars[2 * i + 1] = HEX_CHARS[bytesDigest[i] & 0x0F];
        }
        return new String(chars);
    }

    /*
     * Get the extension of a file.
     */
    public static String getExtension(File f) {
        String ext = null;
        String s = f.getName();
        int i = s.lastIndexOf('.');

        if (i > 0 && i < s.length() - 1) {
            ext = s.substring(i).toLowerCase();
        }
        return ext;
    }

    /**
     * Copies one file or directory(recursively) to another location. If target not exists creates it.
     *
     * @param sourceLocation
     * @param targetLocation
     * @throws IOException
     */
    public static void copyDirectory(File sourceLocation, File targetLocation) throws IOException {

        if (sourceLocation.isDirectory()) {
            if (!targetLocation.exists()) {
                targetLocation.mkdir();
            }

            String[] children = sourceLocation.list();
            for (int i = 0; i < children.length; i++) {
                copyDirectory(new File(sourceLocation, children[i]),
                        new File(targetLocation, children[i]));
            }
        } else {
            InputStream in = new FileInputStream(sourceLocation);
            OutputStream out = new FileOutputStream(targetLocation);

            // Copy the bits from instream to outstream
            byte[] buf = new byte[1024];
            int len;
            while ((len = in.read(buf)) > 0) {
                out.write(buf, 0, len);
            }
            in.close();
            out.close();
        }
    }

    public static void main(String[] args) {

        try {
            File inFile = new File("/home/rcote/Desktop/nursery_survey.doc.gz");
            File outFile = extractTempFile(inFile);
            System.out.println(outFile.getAbsolutePath());
        } catch (IOException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }

    }

    /**
     * Extract a gzip file to a temporary location and return a File object to this file.
     * The file is a temp file and will be deleted on JVM exit
     *
     * @param file - the file to extract
     * @return File object to the extracted file. Will be deleted on JVM exit.
     */
    public static File extractTempFile(File file) throws IOException {

        if (file == null || !file.exists()) {
            throw new IllegalArgumentException("File to unzip must be valid gzip file");
        }

        GZIPInputStream gzipInputStream = null;
        OutputStream outputStream = null;

        File outFile;
        try {
            String tempFileName = "tmp.file";
            int ndx = file.getName().lastIndexOf(FileUtils.gz);
            if (ndx > 0) {
                tempFileName = file.getName().substring(0, ndx);
            }
            gzipInputStream = new GZIPInputStream(new FileInputStream(file));

            String outFileName = TEMP_DIR + FILE_SEPARATOR + tempFileName;
            outFile = new File(outFileName);
            outFile.deleteOnExit();

            outputStream = new FileOutputStream(outFile);
            byte[] buf = new byte[4096];
            int len;
            while ((len = gzipInputStream.read(buf)) > 0) {
                outputStream.write(buf, 0, len);
            }
        } finally {
            if (gzipInputStream != null) {
                gzipInputStream.close();
            }
            if (outputStream != null) {
                outputStream.close();
            }
        }

        return outFile;
    }

}
