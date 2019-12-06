package com.wirecard.tools.debugger.loader;

import com.wirecard.tools.debugger.common.DebuggerConstant;
import org.jd.core.v1.api.loader.Loader;
import org.jd.core.v1.api.loader.LoaderException;

import java.io.*;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class ZipLoader implements Loader {
    protected ConcurrentHashMap<String, byte[]> map = new ConcurrentHashMap();

    public ZipLoader(InputStream is, String parentName) throws LoaderException {
        this.loadLibs(is, parentName);
    }

    private void loadLibs(InputStream is, String parentName) {
        byte[] buffer = new byte[1024 * 2];

        try (ZipInputStream zis = new ZipInputStream(is)) {
            ZipEntry ze = zis.getNextEntry();

            while (ze != null) {
                if (ze.isDirectory() == false) {
                    ByteArrayOutputStream out = new ByteArrayOutputStream();
                    int read = zis.read(buffer);

                    while (read > 0) {
                        out.write(buffer, 0, read);
                        read = zis.read(buffer);
                    }

                    if (ze.getName().endsWith(".jar")) {
                        this.loadLibs(new ByteArrayInputStream(out.toByteArray()), ze.getName());
                    } else {
                        if(ze.getName().contains(DebuggerConstant.FILTER_CLASS_DECOMPILE)) {
                            map.put(ze.getName(), out.toByteArray());
                        } else if(ze.getName().contains(DebuggerConstant.FILTER_APPLICATION_CONTEXT)) {
                            map.put(String.format("%s-%s", parentName, ze.getName()), out.toByteArray());
                        }
                    }
                }

                ze = zis.getNextEntry();
            }

            zis.closeEntry();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public ConcurrentHashMap<String, byte[]> getMap() {
        return map;
    }

    @Override
    public byte[] load(String internalName) throws LoaderException {
        return map.get(internalName + ".class");
    }

    @Override
    public boolean canLoad(String internalName) {
        return map.containsKey(internalName + ".class");
    }
}
