package com.wirecard.tools.debugger.common;

import com.sun.jdi.*;
import com.wirecard.tools.debugger.loader.ZipLoader;
import org.jd.core.v1.model.message.Message;
import org.jd.core.v1.service.converter.classfiletojavasyntax.ClassFileToJavaSyntaxProcessor;
import org.jd.core.v1.service.deserializer.classfile.DeserializeClassFileProcessor;
import org.jd.core.v1.service.fragmenter.javasyntaxtojavafragment.JavaSyntaxToJavaFragmentProcessor;
import org.jd.core.v1.service.layouter.LayoutFragmentProcessor;
import org.jd.core.v1.service.tokenizer.javafragmenttotoken.JavaFragmentToTokenProcessor;
import org.jd.core.v1.service.writer.WriteTokenProcessor;
import printer.PlainTextPrinter;

import javax.validation.constraints.NotNull;
import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class DebuggerUtils {

    private static class CounterPrinter extends PlainTextPrinter {
        public long classCounter = 0;
        public long methodCounter = 0;
        public long errorInMethodCounter = 0;
        public long accessCounter = 0;

        public void printText(String text) {
            if (text != null) {
                if ("// Byte code:".equals(text) || text.startsWith("/* monitor enter ") || text.startsWith("/* monitor exit ")) {
                    errorInMethodCounter++;
                }
            }
            super.printText(text);
        }

        public void printDeclaration(int type, String internalTypeName, String name, String descriptor) {
            if (type == TYPE) classCounter++;
            if ((type == METHOD) || (type == CONSTRUCTOR)) methodCounter++;
            super.printDeclaration(type, internalTypeName, name, descriptor);
        }

        public void printReference(int type, String internalTypeName, String name, String descriptor, String ownerInternalName) {
            if ((name != null) && name.startsWith("access$")) {
                accessCounter++;
            }
            super.printReference(type, internalTypeName, name, descriptor, ownerInternalName);
        }
    }

    public static void removeSourceMap(String serviceId) {
        GlobalVariables.sourceMap.remove(serviceId);
    }

    public static Map<String, Map<Integer, String>> getSourceMap(String serviceId) throws Exception {
        Map<String, Map<Integer, String>> sourceDecompilerMap = GlobalVariables.sourceMap.get(serviceId);

        if (sourceDecompilerMap == null) {
            sourceDecompilerMap = new HashMap<>();
            // TODO: adapt to config later
            Path filejarPath = Paths.get(String.format("./testBundle/%s-1.0-SNAPSHOT.tar", serviceId.toLowerCase()));
            System.out.println("start - compile source: " + filejarPath.toString());
            FileInputStream inputStream = new FileInputStream(filejarPath.toFile());

            try (InputStream is = inputStream) {
                ZipLoader loader = new ZipLoader(is);
                CounterPrinter printer = new CounterPrinter();
                HashMap<String, Integer> statistics = new HashMap<>();
                HashMap<String, Object> configuration = new HashMap<>();

                configuration.put("realignLineNumbers", Boolean.TRUE);

                Message message = new Message();
                message.setHeader("loader", loader);
                message.setHeader("printer", printer);
                message.setHeader("configuration", configuration);

                for (String path : loader.getMap().keySet()) {
                    if (path.endsWith(".class") && (path.indexOf('$') == -1)) {
                        String internalTypeName = path.substring(0, path.length() - 6); // 6 = ".class".length()

                        message.setHeader("mainInternalTypeName", internalTypeName);
                        printer.init();

                        try {
                            // Decompile class
                            GlobalVariables.deserializer.process(message);
                            GlobalVariables.converter.process(message);
                            GlobalVariables.fragmenter.process(message);
                            GlobalVariables.layouter.process(message);
                            GlobalVariables.tokenizer.process(message);
                            GlobalVariables.writer.process(message);
                        } catch (AssertionError e) {
                            String msg = (e.getMessage() == null) ? "<?>" : e.getMessage();
                            Integer counter = statistics.get(msg);
                            statistics.put(msg, (counter == null) ? 1 : counter + 1);
                        } catch (Throwable t) {
                            String msg = t.getMessage() == null ? t.getClass().toString() : t.getMessage();
                            Integer counter = statistics.get(msg);
                            statistics.put(msg, (counter == null) ? 1 : counter + 1);
                        }

                        // Recompile source
                        String source = printer.toString();
                        String newKeyPath = path.replaceAll("/", "\\\\").substring(0, path.lastIndexOf("class")) + "java";
                        int indexBootInf = newKeyPath.indexOf(DebuggerConstant.KEY_BOOT_INF);
                        if (indexBootInf == 0) newKeyPath = newKeyPath.substring(DebuggerConstant.KEY_BOOT_INF.length());
                        BufferedReader br = new BufferedReader(new StringReader(source));
                        String sourceLine = "";
                        int lineNumber = 1;
                        Map<Integer, String> sourceLineCodeMap = new HashMap<>();
                        while ((sourceLine = br.readLine()) != null) {
                            sourceLineCodeMap.put(lineNumber++, sourceLine);
                        }
                        sourceDecompilerMap.put(newKeyPath, sourceLineCodeMap);
                    }
                }
            }
            System.out.println("done - compile source: " + filejarPath.toString());
            GlobalVariables.sourceMap.put(serviceId, sourceDecompilerMap);
        }

        return sourceDecompilerMap;
    }

    @NotNull
    public static Object getJavaValue(@NotNull Value jdiValue, @NotNull ThreadReference thread) {
        if (jdiValue instanceof StringReference) {
            return ((StringReference) jdiValue).value();
        }
        if (jdiValue instanceof PrimitiveValue) {
            PrimitiveValue primitiveValue = (PrimitiveValue) jdiValue;
            if (primitiveValue instanceof BooleanValue) {
                return primitiveValue.booleanValue();
            }
            if (primitiveValue instanceof ShortValue) {
                return primitiveValue.shortValue();
            }
            if (primitiveValue instanceof ByteValue) {
                return primitiveValue.byteValue();
            }
            if (primitiveValue instanceof CharValue) {
                return primitiveValue.charValue();
            }
            if (primitiveValue instanceof DoubleValue) {
                return primitiveValue.doubleValue();
            }
            if (primitiveValue instanceof FloatValue) {
                return primitiveValue.floatValue();
            }
            if (primitiveValue instanceof IntegerValue) {
                return primitiveValue.intValue();
            }
            if (primitiveValue instanceof LongValue) {
                return primitiveValue.longValue();
            }
        } else if (jdiValue instanceof ObjectReference) {
            ObjectReference oRef = (ObjectReference) jdiValue;
            ReferenceType refType = oRef.referenceType();
            String typename = refType.name();
            if (typename.equals(Boolean.class.getName()) || typename.equals(Short.class.getName()) || typename.equals(Byte.class.getName()) || typename.equals(Character.class.getName()) || typename.equals(Double.class.getName()) || typename.equals(Float.class.getName()) || typename.equals(Integer.class.getName()) || typename.equals(Long.class.getName())) {
                Field f = ((ObjectReference) jdiValue).referenceType().fieldByName("value");
                Value result = ((ObjectReference) jdiValue).getValue(f);
                return getJavaValue(result, thread);
            } else {
                try {
                    Method toString = refType.methodsByName("toString").stream().filter(e -> e.argumentTypeNames().size() == 0).collect(Collectors.toList()).get(0);
                    Value displayValue = oRef.invokeMethod(thread, toString, Collections.emptyList(), 0);
                    return displayValue == null ? "null" : displayValue.toString();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        return jdiValue.toString();
    }
}
