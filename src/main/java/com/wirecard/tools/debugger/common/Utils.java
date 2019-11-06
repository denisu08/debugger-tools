package com.wirecard.tools.debugger.common;

import com.sun.jdi.*;

import javax.validation.constraints.NotNull;

public class Utils {
    @NotNull
    public static Object getJavaValue(@NotNull Value jdiValue) {
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
            String typename = ((ObjectReference) jdiValue).referenceType().name();
            if (typename.equals(Boolean.class.getName()) || typename.equals(Short.class.getName()) || typename.equals(Byte.class.getName()) || typename.equals(Character.class.getName()) || typename.equals(Double.class.getName()) || typename.equals(Float.class.getName()) || typename.equals(Integer.class.getName()) || typename.equals(Long.class.getName())) {
                Field f = ((ObjectReference) jdiValue).referenceType().fieldByName("value");
                Value result = ((ObjectReference) jdiValue).getValue(f);
                return getJavaValue(result);
            }
        }
        throw new UnsupportedOperationException();
    }
}
