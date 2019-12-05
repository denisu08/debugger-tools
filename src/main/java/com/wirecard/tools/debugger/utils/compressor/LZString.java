package com.wirecard.tools.debugger.utils.compressor;

import java.util.*;

public class LZString {

    private static char[] keyStrBase64 = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/=".toCharArray();
    private static char[] keyStrUriSafe = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+-$".toCharArray();
    private static Map<char[], Map<Character, Integer>> baseReverseDic = new HashMap<char[], Map<Character, Integer>>();

    private static char getBaseValue(char[] alphabet, Character character) {
        Map<Character, Integer> map = baseReverseDic.get(alphabet);
        if (map == null) {
            map = new HashMap<Character, Integer>();
            baseReverseDic.put(alphabet, map);
            for (int i = 0; i < alphabet.length; i++) {
                map.put(alphabet[i], i);
            }
        }
        return (char) map.get(character).intValue();
    }

    public static String compressToBase64(String input) {
        if (input == null)
            return "";
        String res = LZString._compress(input, 6, new CompressFunctionWrapper() {
            @Override
            public char doFunc(int a) {
                return keyStrBase64[a];
            }
        });
        switch (res.length() % 4) { // To produce valid Base64
            default: // When could this happen ?
            case 0:
                return res;
            case 1:
                return res + "===";
            case 2:
                return res + "==";
            case 3:
                return res + "=";
        }
    }

    public static String decompressFromBase64(final String inputStr) {
        if (inputStr == null)
            return "";
        if (inputStr.equals(""))
            return null;
        return LZString._decompress(inputStr.length(), 32, new DecompressFunctionWrapper() {
            @Override
            public char doFunc(int index) {
                return getBaseValue(keyStrBase64, inputStr.charAt(index));
            }
        });
    }

    public static String compressToUTF16(String input) {
        if (input == null)
            return "";
        return LZString._compress(input, 15, new CompressFunctionWrapper() {
            @Override
            public char doFunc(int a) {
                return fc(a + 32);
            }
        }) + " ";
    }

    public static String decompressFromUTF16(final String compressedStr) {
        if (compressedStr == null)
            return "";
        if (compressedStr.isEmpty())
            return null;
        return LZString._decompress(compressedStr.length(), 16384, new DecompressFunctionWrapper() {
            @Override
            public char doFunc(int index) {
                return (char) (compressedStr.charAt(index) - 32);
            }
        });
    }

    //TODO: java has no Uint8Array type, what can we do?

    public static String compressToEncodedURIComponent(String input) {
        if (input == null)
            return "";
        return LZString._compress(input, 6, new CompressFunctionWrapper() {
            @Override
            public char doFunc(int a) {
                return keyStrUriSafe[a];
            }
        });
    }

    public static String decompressFromEncodedURIComponent(String inputStr) {
        if (inputStr == null) return "";
        if (inputStr.isEmpty()) return null;
        final String urlEncodedInputStr = inputStr.replace(' ', '+');
        return LZString._decompress(urlEncodedInputStr.length(), 32, new DecompressFunctionWrapper() {
            @Override
            public char doFunc(int index) {
                return getBaseValue(keyStrUriSafe, urlEncodedInputStr.charAt(index));
            }
        });
    }

    private static abstract class CompressFunctionWrapper {
        public abstract char doFunc(int i);
    }

    public static String compress(String uncompressed) {
        return LZString._compress(uncompressed, 16, new CompressFunctionWrapper() {
            @Override
            public char doFunc(int a) {
                return fc(a);
            }
        });
    }
    private static String _compress(String uncompressedStr, int bitsPerChar, CompressFunctionWrapper getCharFromInt) {
        if (uncompressedStr == null) return "";
        int i, value;
        Map<String, Integer> context_dictionary = new HashMap<String, Integer>();
        Set<String> context_dictionaryToCreate = new HashSet<String>();
        String context_c = "";
        String context_wc = "";
        String context_w = "";
        int context_enlargeIn = 2; // Compensate for the first entry which should not count
        int context_dictSize = 3;
        int context_numBits = 2;
        StringBuilder context_data = new StringBuilder(uncompressedStr.length() / 3);
        int context_data_val = 0;
        int context_data_position = 0;
        int ii;

        for (ii = 0; ii < uncompressedStr.length(); ii += 1) {
            context_c = String.valueOf(uncompressedStr.charAt(ii));
            if (!context_dictionary.containsKey(context_c)) {
                context_dictionary.put(context_c, context_dictSize++);
                context_dictionaryToCreate.add(context_c);
            }

            context_wc = context_w + context_c;
            if (context_dictionary.containsKey(context_wc)) {
                context_w = context_wc;
            } else {
                if (context_dictionaryToCreate.contains(context_w)) {
                    if (context_w.charAt(0) < 256) {
                        for (i = 0; i < context_numBits; i++) {
                            context_data_val = (context_data_val << 1);
                            if (context_data_position == bitsPerChar - 1) {
                                context_data_position = 0;
                                context_data.append(getCharFromInt.doFunc(context_data_val));
                                context_data_val = 0;
                            } else {
                                context_data_position++;
                            }
                        }
                        value = context_w.charAt(0);
                        for (i = 0; i < 8; i++) {
                            context_data_val = (context_data_val << 1) | (value & 1);
                            if (context_data_position == bitsPerChar - 1) {
                                context_data_position = 0;
                                context_data.append(getCharFromInt.doFunc(context_data_val));
                                context_data_val = 0;
                            } else {
                                context_data_position++;
                            }
                            value = value >> 1;
                        }
                    } else {
                        value = 1;
                        for (i = 0; i < context_numBits; i++) {
                            context_data_val = (context_data_val << 1) | value;
                            if (context_data_position == bitsPerChar - 1) {
                                context_data_position = 0;
                                context_data.append(getCharFromInt.doFunc(context_data_val));
                                context_data_val = 0;
                            } else {
                                context_data_position++;
                            }
                            value = 0;
                        }
                        value = context_w.charAt(0);
                        for (i = 0; i < 16; i++) {
                            context_data_val = (context_data_val << 1) | (value & 1);
                            if (context_data_position == bitsPerChar - 1) {
                                context_data_position = 0;
                                context_data.append(getCharFromInt.doFunc(context_data_val));
                                context_data_val = 0;
                            } else {
                                context_data_position++;
                            }
                            value = value >> 1;
                        }
                    }
                    context_enlargeIn--;
                    if (context_enlargeIn == 0) {
                        context_enlargeIn = powerOf2(context_numBits);
                        context_numBits++;
                    }
                    context_dictionaryToCreate.remove(context_w);
                } else {
                    value = context_dictionary.get(context_w);
                    for (i = 0; i < context_numBits; i++) {
                        context_data_val = (context_data_val << 1) | (value & 1);
                        if (context_data_position == bitsPerChar - 1) {
                            context_data_position = 0;
                            context_data.append(getCharFromInt.doFunc(context_data_val));
                            context_data_val = 0;
                        } else {
                            context_data_position++;
                        }
                        value = value >> 1;
                    }

                }
                context_enlargeIn--;
                if (context_enlargeIn == 0) {
                    context_enlargeIn = powerOf2(context_numBits);
                    context_numBits++;
                }
                // Add wc to the dictionary.
                context_dictionary.put(context_wc, context_dictSize++);
                context_w = context_c;
            }
        }

        // Output the code for w.
        if (!context_w.isEmpty()) {
            if (context_dictionaryToCreate.contains(context_w)) {
                if (context_w.charAt(0) < 256) {
                    for (i = 0; i < context_numBits; i++) {
                        context_data_val = (context_data_val << 1);
                        if (context_data_position == bitsPerChar - 1) {
                            context_data_position = 0;
                            context_data.append(getCharFromInt.doFunc(context_data_val));
                            context_data_val = 0;
                        } else {
                            context_data_position++;
                        }
                    }
                    value = context_w.charAt(0);
                    for (i = 0; i < 8; i++) {
                        context_data_val = (context_data_val << 1) | (value & 1);
                        if (context_data_position == bitsPerChar - 1) {
                            context_data_position = 0;
                            context_data.append(getCharFromInt.doFunc(context_data_val));
                            context_data_val = 0;
                        } else {
                            context_data_position++;
                        }
                        value = value >> 1;
                    }
                } else {
                    value = 1;
                    for (i = 0; i < context_numBits; i++) {
                        context_data_val = (context_data_val << 1) | value;
                        if (context_data_position == bitsPerChar - 1) {
                            context_data_position = 0;
                            context_data.append(getCharFromInt.doFunc(context_data_val));
                            context_data_val = 0;
                        } else {
                            context_data_position++;
                        }
                        value = 0;
                    }
                    value = context_w.charAt(0);
                    for (i = 0; i < 16; i++) {
                        context_data_val = (context_data_val << 1) | (value & 1);
                        if (context_data_position == bitsPerChar - 1) {
                            context_data_position = 0;
                            context_data.append(getCharFromInt.doFunc(context_data_val));
                            context_data_val = 0;
                        } else {
                            context_data_position++;
                        }
                        value = value >> 1;
                    }
                }
                context_enlargeIn--;
                if (context_enlargeIn == 0) {
                    context_enlargeIn = powerOf2(context_numBits);
                    context_numBits++;
                }
                context_dictionaryToCreate.remove(context_w);
            } else {
                value = context_dictionary.get(context_w);
                for (i = 0; i < context_numBits; i++) {
                    context_data_val = (context_data_val << 1) | (value & 1);
                    if (context_data_position == bitsPerChar - 1) {
                        context_data_position = 0;
                        context_data.append(getCharFromInt.doFunc(context_data_val));
                        context_data_val = 0;
                    } else {
                        context_data_position++;
                    }
                    value = value >> 1;
                }

            }
            context_enlargeIn--;
            if (context_enlargeIn == 0) {
                context_enlargeIn = powerOf2(context_numBits);
                context_numBits++;
            }
        }

        // Mark the end of the stream
        value = 2;
        for (i = 0; i < context_numBits; i++) {
            context_data_val = (context_data_val << 1) | (value & 1);
            if (context_data_position == bitsPerChar - 1) {
                context_data_position = 0;
                context_data.append(getCharFromInt.doFunc(context_data_val));
                context_data_val = 0;
            } else {
                context_data_position++;
            }
            value = value >> 1;
        }

        // Flush the last char
        while (true) {
            context_data_val = (context_data_val << 1);
            if (context_data_position == bitsPerChar - 1) {
                context_data.append(getCharFromInt.doFunc(context_data_val));
                break;
            }
            else
                context_data_position++;
        }
        return context_data.toString();
    }

    private static abstract class DecompressFunctionWrapper {
        public abstract char doFunc(int i);
    }
    protected static class DecData {
        public char val;
        public int position;
        public int index;
    }

    public static String f(int i) {
        return String.valueOf((char) i);
    }
    public static char fc(int i) {
        return (char) i;
    }

    public static String decompress(final String compressed) {
        if (compressed == null)
            return "";
        if (compressed.isEmpty())
            return null;
        return LZString._decompress(compressed.length(), 32768, new DecompressFunctionWrapper() {
            @Override
            public char doFunc(int i) {
                return compressed.charAt(i);
            }
        });
    }
    private static String _decompress(int length, int resetValue, DecompressFunctionWrapper getNextValue) {
        List<String> dictionary = new ArrayList<String>();
        // TODO: is next an unused variable in original lz-string?
        @SuppressWarnings("unused")
        int next;
        int enlargeIn = 4;
        int dictSize = 4;
        int numBits = 3;
        String entry = "";
        StringBuilder result = new StringBuilder();
        String w;
        int bits, resb; int maxpower, power;
        String c = null;
        DecData data = new DecData();
        data.val = getNextValue.doFunc(0);
        data.position = resetValue;
        data.index = 1;

        for (int i = 0; i < 3; i += 1) {
            dictionary.add(i, f(i));
        }

        bits = 0;
        maxpower = (int) powerOf2(2);
        power = 1;
        while (power != maxpower) {
            resb = data.val & data.position;
            data.position >>= 1;
            if (data.position == 0) {
                data.position = resetValue;
                data.val = getNextValue.doFunc(data.index++);
            }
            bits |= (resb > 0 ? 1 : 0) * power;
            power <<= 1;
        }

        switch (next = bits) {
            case 0:
                bits = 0;
                maxpower = (int) powerOf2(8);
                power=1;
                while (power != maxpower) {
                    resb = data.val & data.position;
                    data.position >>= 1;
                    if (data.position == 0) {
                        data.position = resetValue;
                        data.val = getNextValue.doFunc(data.index++);
                    }
                    bits |= (resb>0 ? 1 : 0) * power;
                    power <<= 1;
                }
                c = f(bits);
                break;
            case 1:
                bits = 0;
                maxpower = powerOf2(16);
                power=1;
                while (power!=maxpower) {
                    resb = data.val & data.position;
                    data.position >>= 1;
                    if (data.position == 0) {
                        data.position = resetValue;
                        data.val = getNextValue.doFunc(data.index++);
                    }
                    bits |= (resb>0 ? 1 : 0) * power;
                    power <<= 1;
                }
                c = f(bits);
                break;
            case 2:
                return "";
        }
        dictionary.add(3, c);
        w = c;
        result.append(w);
        while (true) {
            if (data.index > length) {
                return "";
            }

            bits = 0;
            maxpower = powerOf2(numBits);
            power=1;
            while (power!=maxpower) {
                resb = data.val & data.position;
                data.position >>= 1;
                if (data.position == 0) {
                    data.position = resetValue;
                    data.val = getNextValue.doFunc(data.index++);
                }
                bits |= (resb>0 ? 1 : 0) * power;
                power <<= 1;
            }
            // TODO: very strange here, c above is as char/string, here further is a int, rename "c" in the switch as "cc"
            int cc;
            switch (cc = bits) {
                case 0:
                    bits = 0;
                    maxpower = powerOf2(8);
                    power=1;
                    while (power!=maxpower) {
                        resb = data.val & data.position;
                        data.position >>= 1;
                        if (data.position == 0) {
                            data.position = resetValue;
                            data.val = getNextValue.doFunc(data.index++);
                        }
                        bits |= (resb>0 ? 1 : 0) * power;
                        power <<= 1;
                    }

                    dictionary.add(dictSize++, f(bits));
                    cc = dictSize-1;
                    enlargeIn--;
                    break;
                case 1:
                    bits = 0;
                    maxpower = powerOf2(16);
                    power=1;
                    while (power!=maxpower) {
                        resb = data.val & data.position;
                        data.position >>= 1;
                        if (data.position == 0) {
                            data.position = resetValue;
                            data.val = getNextValue.doFunc(data.index++);
                        }
                        bits |= (resb>0 ? 1 : 0) * power;
                        power <<= 1;
                    }
                    dictionary.add(dictSize++, f(bits));
                    cc = dictSize-1;
                    enlargeIn--;
                    break;
                case 2:
                    return result.toString();
            }

            if (enlargeIn == 0) {
                enlargeIn = powerOf2(numBits);
                numBits++;
            }

            if (cc < dictionary.size() && dictionary.get(cc) != null) {
                entry = dictionary.get(cc);
            } else {
                if (cc == dictSize) {
                    entry = w + w.charAt(0);
                } else {
                    return null;
                }
            }
            result.append(entry);

            // Add w+entry[0] to the dictionary.
            dictionary.add(dictSize++, w + entry.charAt(0));
            enlargeIn--;

            w = entry;

            if (enlargeIn == 0) {
                enlargeIn = powerOf2(numBits);
                numBits++;
            }

        }

    }

    private static int powerOf2(int power) {
        return 1 << power;
    }

//    public static void main(String args[]) throws Exception {
//        // Normal Compression and Decompression
//        String test = "Lets see how much we can compress this string!";
//
//        String output = LZString.compress(test);
//
//        System.out.println("Compressed: " + output);
//
//        String decompressed = LZString.decompress(output);
//
//        System.out.println("Decompressed: " + decompressed);
//
//        //UTF-16 Compression and Decompression
//        String testUTF16 = "Lets see how much we can compress this string!";
//
//        String outputUTF16 = LZString.compressToUTF16(testUTF16);
//
//        System.out.println("Compressed: " + outputUTF16);
//
//        String decompressedUTF16 = LZString.decompressFromUTF16(outputUTF16);
//
//        System.out.println("Decompressed: " + decompressedUTF16);
//
//        String testBase64 = "Lets see how much we can compress this string!";
//
//        String outputBase64 = LZString.compressToBase64(testBase64);
//
//        System.out.println("Compressed: " + outputBase64);

//        String decompressedBase64 = LZString.decompressFromBase64(outputBase64);
//        String decompressedBase64 = LZString.decompressFromBase64("CLA0BcCcFcFNQLIE1QHEEBUDUAORBDAG3wE8BnAS31ADMizYBYAKBAhngEkA5AQTUxYA7KE4A7ACYB7MbErUocFkAAA=");
//
//        System.out.println("Decompressed: " + decompressedBase64);

//        String decompressedBase64 = LZString.decompressFromBase64("EoUQig7GAMCMAqB5AXiA7o2AhAFgQwFsAxAJwEkisw8BhKgTQA0A5HAIzoHFhoAtACQAiYAA4hgggC6D4ANQCyEefBAQAqpyIBHegHUAzgHM1lAPYVcAYwBMRfWUFkM8ANbMaL0ImjNma5PoAHvIAVpJgeiL4AJ5UGobGmsh6aIahIACsiDAA0qCQMAgoIMiYuISkFtR0ESzsNACC0aEuGcJiEtJyisoN6po6BsZmFjg2dmQgAsAulu6eIN6+/kHK4ZExcZwJGkTJuqmhDVm5+VBwSKjRZfjE5JTVDHUcTcqt7eJSMgpK8Iq7gyMJiw5koY1s9kc11c8y8Pj8AWCUgiuiieFiYHiiT2KUMMwAJtw0NA8uBzkVUIEbhV7lRaE9WC9moJ3qJPl0fsoyP1tHogSMweNIWQqTCPHDloj5Mh1qjNpjttj9ocQmQTiSzoVLuhqXcqvTaoyuDwBB9Ot8eioeYDhiDRkKHE5EGKFksEasZSi0RisbtlWlVerSQULsUMNhbpUHgamEasNw+EI2ebur9VAC+bbQVYIY7nG5xYt4StgmEvfLfUlcekg5rQ6hShGafqarH6q8QqyOl9U71rZngdnwRMpvwZnNC26S2ty+itjsqwcA8dsiSgA");
//        System.out.println("Decompressed: " + new String(Base64.getDecoder().decode(decompressedBase64)));

//        byte[] fileContent = Files.readAllBytes(Paths.get("D:/test.csv"));
//
//         // System.out.println("Decompressed: " + new String(Base64.getDecoder().decode("REQ7Q01TOzEwO1BhamFrIFBQaCBQYXNhbCBGR0ZHDQpERDtDTVM7MTE7UGFqYWsgUFBoIFBhc2FsIDIwOTkNCkREO0NNUzsxMjtQYWphayBQUGggUGFzYWwgMjE5OQ0KREQ7Q01TOzEzO1BhamFrIFBQaCBQYXNhbCAyMjk5DQpERDtDTVM7MTA7UGFqYWsgUFBoIFBhc2FsIEZHRkcNCkREO0NNUzsxMTtQYWphayBQUGggUGFzYWwgMjA5OQ0KREQ7Q01TOzEyO1BhamFrIFBQaCBQYXNhbCAyMTk5DQpERDtDTVM7MTM7UGFqYWsgUFBoIFBhc2FsIDIyOTkNCkREO0NNUzsxMDtQYWphayBQUGggUGFzYWwgRkdGRw0KREQ7Q01TOzExO1BhamFrIFBQaCBQYXNhbCAyMDk5DQpERDtDTVM7MTI7UGFqYWsgUFBoIFBhc2FsIDIxOTkNCkREO0NNUzsxMztQYWphayBQUGggUGFzYWwgMjI5OQ0KREQ7Q01TOzEwO1BhamFrIFBQaCBQYXNhbCBGR0ZHDQpERDtDTVM7MTE7UGFqYWsgUFBoIFBhc2FsIDIwOTkNCkREO0NNUzsxMztQYWphayBQUGggUGFzYWwgMjI5OQ0KREQ7Q01TOzEwO1BhamFrIFBQaCBQYXNhbCBGR0ZHDQpERDtDTVM7MTE7UGFqYWsgUFBoIFBhc2FsIDIwOTkNCkREO0NNUzsxMjtQYWphayBQUGggUGFzYWwgMjE5OQ0KREQ7Q01TOzEzO1BhamFrIFBQaCBQYXNhbCAyMjk5DQpERDtDTVM7MTA7UGFqYWsgUFBoIFBhc2FsIEZHRkcNCkREO0NNUzsxMTtQYWphayBQUGggUGFzYWwgMjA5OQ0K")));
//        String encoder = new String(Base64.getEncoder().encode(fileContent));
//        System.out.println("Encoder: " + encoder);
//        System.out.println("Compress64: " + new String(LZString.compressToBase64(encoder)));


//         System.out.println("Encode: " + new String(LZString.compressToBase64("REQ7Q01TOzEwO1BhamFrIFBQaCBQYXNhbCBGR0ZHDQpERDtDTVM7MTE7UGFqYWsgUFBoIFBhc2FsIDIwOTkNCkREO0NNUzsxMjtQYWphayBQUGggUGFzYWwgMjE5OQ0KREQ7Q01TOzEzO1BhamFrIFBQaCBQYXNhbCAyMjk5DQpERDtDTVM7MTA7UGFqYWsgUFBoIFBhc2FsIEZHRkcNCkREO0NNUzsxMTtQYWphayBQUGggUGFzYWwgMjA5OQ0KREQ7Q01TOzEyO1BhamFrIFBQaCBQYXNhbCAyMTk5DQpERDtDTVM7MTM7UGFqYWsgUFBoIFBhc2FsIDIyOTkNCkREO0NNUzsxMDtQYWphayBQUGggUGFzYWwgRkdGRw0KREQ7Q01TOzExO1BhamFrIFBQaCBQYXNhbCAyMDk5DQpERDtDTVM7MTI7UGFqYWsgUFBoIFBhc2FsIDIxOTkNCkREO0NNUzsxMztQYWphayBQUGggUGFzYWwgMjI5OQ0KREQ7Q01TOzEwO1BhamFrIFBQaCBQYXNhbCBGR0ZHDQpERDtDTVM7MTE7UGFqYWsgUFBoIFBhc2FsIDIwOTkNCkREO0NNUzsxMztQYWphayBQUGggUGFzYWwgMjI5OQ0KREQ7Q01TOzEwO1BhamFrIFBQaCBQYXNhbCBGR0ZHDQpERDtDTVM7MTE7UGFqYWsgUFBoIFBhc2FsIDIwOTkNCkREO0NNUzsxMjtQYWphayBQUGggUGFzYWwgMjE5OQ0KREQ7Q01TOzEzO1BhamFrIFBQaCBQYXNhbCAyMjk5DQpERDtDTVM7MTA7UGFqYWsgUFBoIFBhc2FsIEZHRkcNCkREO0NNUzsxMTtQYWphayBQUGggUGFzYWwgMjA5OQ0K")));

    // MoUQ5gzgKg6gnASwIYwB4BsCSIBSALAIwFsAXAB2IGMBeIA=
    // MoUQ5gzgKg6gnASwIYwB4BsCSIBSALAIwFsAXAB2IGMBeIA=
//        String originalString = "DD;CMS;10;Pajak PPh Pasal FGFG\n" +
//                "DD;CMS;11;Pajak PPh Pasal 2099\n" +
//                "DD;CMS;12;Pajak PPh Pasal 2199\n" +
//                "DD;CMS;13;Pajak PPh Pasal 2299\n" +
//                "DD;CMS;10;Pajak PPh Pasal FGFG\n" +
//                "DD;CMS;11;Pajak PPh Pasal 2099\n" +
//                "DD;CMS;12;Pajak PPh Pasal 2199\n" +
//                "DD;CMS;13;Pajak PPh Pasal 2299\n" +
//                "DD;CMS;10;Pajak PPh Pasal FGFG\n" +
//                "DD;CMS;11;Pajak PPh Pasal 2099\n" +
//                "DD;CMS;12;Pajak PPh Pasal 2199\n" +
//                "DD;CMS;13;Pajak PPh Pasal 2299\n" +
//                "DD;CMS;10;Pajak PPh Pasal FGFG\n" +
//                "DD;CMS;11;Pajak PPh Pasal 2099\n" +
//                "DD;CMS;13;Pajak PPh Pasal 2299\n" +
//                "DD;CMS;10;Pajak PPh Pasal FGFG\n" +
//                "DD;CMS;11;Pajak PPh Pasal 2099\n" +
//                "DD;CMS;12;Pajak PPh Pasal 2199\n" +
//                "DD;CMS;13;Pajak PPh Pasal 2299\n" +
//                "DD;CMS;10;Pajak PPh Pasal FGFG\n" +
//                "DD;CMS;11;Pajak PPh Pasal 2099\n";
//        String decodeString = new String(Base64.getEncoder().encode(originalString.getBytes()));
//        System.out.println("decodeString:\n" + decodeString);
//        String compressed64 = LZString.compressToBase64(decodeString);
//        System.out.println("compressed64:\n" + compressed64);
//
//        String decompressString = LZString.decompressFromBase64(compressed64);
//        System.out.println("decodeString: " + decompressString);
//        System.out.println("Decompressed: " + new String(Base64.getDecoder().decode(decompressString)));

//        String decompressString = LZString.decompressFromBase64("EoUQig7GAMCMAqB5AXiA7o2AhAFgQwFsAxAJwEkisw8BhKgTQA0A5HAIzoHFhoAtACQAiYAA4hgggC6D4ANQCyEefBAQAqpyIBHegHUAzgHM1lAPYVcAYwBMRfWUFkM8ANbMaL0ImjNma5PoAHvIAVpJgeiL4AJ5UGobGmsh6aIahIACsiDAA0qCQMAgoIMiYuISkFtR0ESzsNACC0aEuGcJiEtJyisoN6po6BsZmFjg2dmQgAsAulu6eIN6+/kHK4ZExcZwJGkTJuqmhDVm5+VBwSKjRZfjE5JTVDHUcTcqt7eJSMgpK8Iq7gyMJiw5koY1s9kc11c8y8Pj8AWCUgiuiieFiYHiiT2KUMMwAJtw0NA8uBzkVUIEbhV7lRaE9WC9moJ3qJPl0fsoyP1tHogSMweNIWQqTCPHDloj5Mh1qjNpjttj9ocQmQTiSzoVLuhqXcqvTaoyuDwBB9Ot8eioeYDhiDRkKHE5EGKFksEasZSi0RisbtlWlVerSQULsUMNhbpUHgamEasNw+EI2ebur9VAC+bbQVYIY7nG5xYt4StgmEvfLfUlcekg5rQ6hShGafqarH6q8QqyOl9U71rZngdnwRMpvwZnNC26S2ty+itjsqwcA8dsiSgA");
//        System.out.println("decodeString: " + decompressString);
//        System.out.println("Decompressed: " + new String(Base64.getDecoder().decode(decompressString)));
//    }
}
