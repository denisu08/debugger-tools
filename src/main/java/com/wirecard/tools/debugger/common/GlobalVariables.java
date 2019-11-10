package com.wirecard.tools.debugger.common;

import com.wirecard.tools.debugger.model.DataDebug;
import org.jd.core.v1.service.converter.classfiletojavasyntax.ClassFileToJavaSyntaxProcessor;
import org.jd.core.v1.service.deserializer.classfile.DeserializeClassFileProcessor;
import org.jd.core.v1.service.fragmenter.javasyntaxtojavafragment.JavaSyntaxToJavaFragmentProcessor;
import org.jd.core.v1.service.layouter.LayoutFragmentProcessor;
import org.jd.core.v1.service.tokenizer.javafragmenttotoken.JavaFragmentToTokenProcessor;
import org.jd.core.v1.service.writer.WriteTokenProcessor;

import java.util.HashMap;
import java.util.Map;

public class GlobalVariables {
    public static Map<String, DataDebug> jdiContainer = new HashMap();
    public static Map<String, Map<String, Map<Integer, String>>> sourceMap = new HashMap<>();

    public static DeserializeClassFileProcessor deserializer = new DeserializeClassFileProcessor();
    public static ClassFileToJavaSyntaxProcessor converter = new ClassFileToJavaSyntaxProcessor();
    public static JavaSyntaxToJavaFragmentProcessor fragmenter = new JavaSyntaxToJavaFragmentProcessor();
    public static LayoutFragmentProcessor layouter = new LayoutFragmentProcessor();
    public static JavaFragmentToTokenProcessor tokenizer = new JavaFragmentToTokenProcessor();
    public static WriteTokenProcessor writer = new WriteTokenProcessor();

}
