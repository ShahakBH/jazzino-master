package com.yazino.platform.plugin;

import java.io.IOException;

public interface PluginManager {

    String getPluginLocation();

    void syncPlugins() throws IOException;

    void syncPlugin(String filename) throws IOException;

    byte[] serialise(Object object);

    Object deserialise(byte[] serialisedObject);

}
