package dev.masterflomaster1.sjc.crypto;

import javax.crypto.*;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.channels.Channels;
import java.nio.channels.CompletionHandler;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.security.*;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public class BlockCipherImpl {

    private static final SecureRandom random = new SecureRandom();

    public static byte[] encrypt(String algorithm, Mode mode, Padding padding, byte[] iv, byte[] inputData, byte[] key) {
        try {
            SecretKey secretKey = new SecretKeySpec(key, algorithm);
            Cipher cipher = Cipher.getInstance(algorithm+"/%s/%s".formatted(mode.mode, padding.padding), "BC");

            if (mode == Mode.ECB) {
                cipher.init(Cipher.ENCRYPT_MODE, secretKey);
            } else {
                IvParameterSpec ivParameterSpec = new IvParameterSpec(iv);
                cipher.init(Cipher.ENCRYPT_MODE, secretKey, ivParameterSpec);
            }

            return cipher.doFinal(inputData);
        } catch (NoSuchAlgorithmException | NoSuchProviderException | NoSuchPaddingException | InvalidKeyException |
                 IllegalBlockSizeException | BadPaddingException | InvalidAlgorithmParameterException e) {
            throw new RuntimeException(e);
        }
    }

    public static byte[] decrypt(String algorithm, Mode mode, Padding padding, byte[] iv, byte[] inputData, byte[] key) {
        try {
            SecretKey secretKey = new SecretKeySpec(key, algorithm);
            Cipher cipher = Cipher.getInstance(algorithm+"/%s/%s".formatted(mode.mode, padding), "BC");

            if (mode == Mode.ECB) {
                cipher.init(Cipher.DECRYPT_MODE, secretKey);
            } else  {
                IvParameterSpec ivParameterSpec = new IvParameterSpec(iv);
                cipher.init(Cipher.DECRYPT_MODE, secretKey, ivParameterSpec);
            }

            return cipher.doFinal(inputData);
        } catch (NoSuchAlgorithmException | NoSuchProviderException | NoSuchPaddingException | InvalidKeyException |
                 IllegalBlockSizeException | BadPaddingException | InvalidAlgorithmParameterException e) {
            throw new RuntimeException(e);
        }
    }

    public static CompletableFuture<Void> asyncEncrypt(String targetFilePath,
                                                       String destinationFilePath,
                                                       String algorithm, Mode mode,
                                                       Padding padding,
                                                       byte[] iv,
                                                       byte[] key) {

        return asyncOperation(
                targetFilePath,
                destinationFilePath,
                algorithm,
                mode,
                padding,
                iv,
                key,
                Cipher.ENCRYPT_MODE
        );
    }

    public static CompletableFuture<Void> asyncDecrypt(String targetFilePath,
                                                       String destinationFilePath,
                                                       String algorithm,
                                                       Mode mode,
                                                       Padding padding,
                                                       byte[] iv,
                                                       byte[] key) {

        return asyncOperation(
                targetFilePath,
                destinationFilePath,
                algorithm,
                mode,
                padding,
                iv,
                key,
                Cipher.DECRYPT_MODE
        );
    }

    private static CompletableFuture<Void> asyncOperation(String inputFilePath,
                                                          String outputFilePath,
                                                          String algorithm,
                                                          Mode mode,
                                                          Padding padding,
                                                          byte[] iv,
                                                          byte[] key,
                                                          int cryptMode) {

        return CompletableFuture.runAsync(() -> {
            try {
                Path inputPath = Paths.get(inputFilePath);
                Path outputPath = Paths.get(outputFilePath);

                SecretKey secretKey = new SecretKeySpec(key, algorithm);
                Cipher cipher = Cipher.getInstance(algorithm + "/" + mode.getMode() + "/" + padding.getPadding(), "BC");

                if (mode == Mode.ECB) {
                    cipher.init(cryptMode, secretKey);
                } else {
                    IvParameterSpec ivParameterSpec = new IvParameterSpec(iv);
                    cipher.init(cryptMode, secretKey, ivParameterSpec);
                }

                try (FileChannel inputChannel = new FileInputStream(inputFilePath).getChannel();
                     FileChannel outputChannel = new FileOutputStream(outputFilePath).getChannel()) {

                    ByteBuffer inputBuffer = ByteBuffer.allocate(1024);
                    ByteBuffer outputBuffer = ByteBuffer.allocate(1024 * 2);

                    while (inputChannel.read(inputBuffer) > 0) {
                        inputBuffer.flip();

                        cipher.doFinal(inputBuffer, outputBuffer);
                        outputBuffer.flip();

                        outputChannel.write(outputBuffer);

                        inputBuffer.clear();
                        outputBuffer.clear();
                    }

                } catch (Exception e) {
                    throw new RuntimeException(e);
                }

            } catch (InvalidAlgorithmParameterException | NoSuchPaddingException |
                     InvalidKeyException | NoSuchProviderException | NoSuchAlgorithmException e) {
                throw new RuntimeException(e);
            }
        });
    }

    private static void closeChannels(AsynchronousFileChannel... channels) {
        for (AsynchronousFileChannel channel : channels) {
            try {
                if (channel != null) {
                    channel.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Temporarily removed {@code DSTU7624 512} to fix key generation exception
     */
    public static List<Integer> getAvailableKeyLengths(String algorithm) {
        return switch (algorithm) {
            case "AES", "CAMELLIA", "RC6", "RIJNDAEL", "Serpent", "Tnepres", "Twofish" -> List.of(128, 192, 256);
            case "BLOWFISH", "DSTU7624" -> List.of(128, 256);
            case "CAST5", "IDEA", "NOEKEON", "RC2", "RC5", "SEED", "SM4", "TEA", "XTEA" -> List.of(128);
            case "CAST6" -> List.of(128, 160, 192, 224, 256);
            case "DES" -> List.of(64);
            case "DESEDE" -> List.of(128, 192);
            case "GOST28147", "GOST3412-2015", "Threefish-256" -> List.of(256);
            case "SHACAL-2" -> List.of(128, 192, 256, 512);
            case "SKIPJACK" -> List.of(80);
            case "Threefish-1024" -> List.of(1024);
            case "Threefish-512" -> List.of(512);
            default -> Collections.emptyList();
        };
    }

    private static int getBlockLengthBits(String algorithm) {
        return switch (algorithm) {
            case "DES", "DESEDE", "BLOWFISH", "CAST5", "GOST28147", "IDEA", "RC2", "RC5", "SKIPJACK", "TEA", "XTEA" -> 64;
            case "SHACAL-2", "Threefish-256" -> 256;
            case "Threefish-512" -> 512;
            case "Threefish-1024" -> 1024;
            default -> 128;
        };
    }

    public static byte[] generateKey(String algorithm, int keySize) {
        try {
            KeyGenerator keyGen = KeyGenerator.getInstance(algorithm, "BC");
            keyGen.init(keySize);
            return keyGen.generateKey().getEncoded();
        } catch (NoSuchAlgorithmException | NoSuchProviderException e) {
            throw new RuntimeException(e);
        }
    }

    public static byte[] generateIV(String algorithm) {
        byte[] iv = new byte[getBlockLengthBits(algorithm) / 8];
        random.nextBytes(iv);
        return iv;
    }

    public static byte[] generatePasswordBasedKey(char[] password, int keySize) {
        var salt = Base64.getDecoder().decode("4WHuOVNv8nIwjrPhLpyPwA==");
        var f = PbeImpl.asyncHash("PBKDF2", password, salt, 10000, keySize);

        try {
            return f.get();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    public enum Mode {
        ECB("ECB"),
        CBC("CBC"),
        CFB("CFB"),
        OFB("OFB");
//        CTR("CTR");

        private final String mode;

        Mode(String mode) {
            this.mode = mode;
        }

        public String getMode() {
            return mode;
        }

        @SuppressWarnings("unused")
        public static Mode fromString(String value) {
            for (Mode m : Mode.values()) {
                if (m.getMode().equalsIgnoreCase(value))
                    return m;
            }

            throw new IllegalArgumentException(value);
        }
    }

    public enum Padding {
        PKCS5Padding("PKCS5Padding"),
        PKCS7Padding("PKCS7Padding"),
        ISO10126Padding("ISO10126Padding"),
        ZeroBytePadding("ZeroBytePadding");

        private final String padding;

        Padding(String padding) {
            this.padding = padding;
        }

        public String getPadding() {
            return padding;
        }

        @SuppressWarnings("unused")
        public static Padding fromString(String value) {
            for (Padding p : Padding.values()) {
                if (p.padding.equalsIgnoreCase(value)) {
                    return p;
                }
            }

            throw new IllegalArgumentException(value);
        }
    }

}
