package com.wirecard.tools.debugger.loader;

import org.jd.core.v1.api.loader.Loader;
import org.jd.core.v1.api.loader.LoaderException;

public class NopLoader implements Loader {
    @Override
    public byte[] load(String internalName) throws LoaderException {
        return null;
    }

    @Override
    public boolean canLoad(String internalName) {
        return false;
    }
}
