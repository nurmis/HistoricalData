package com.aware.simulator.semantization.config;


import android.util.Log;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.*;


public class JsonConfigReader {

    private static final String jsonConfigFileName = "json_configuration.json";

    public Configuration deserializeConfiguration() {
        try {
            ObjectMapper mapper = new ObjectMapper();
            String jsonConfiguration = readJsonFileFromPath();
            return mapper.readValue(jsonConfiguration, Configuration.class);
        } catch (Exception e) {
            System.out.print("ERROR:Syntax of a json file is not valid:");
            e.printStackTrace();
        }
        return null;
    }


    private String readJsonFileFromPath() throws IOException {
        String result = "";
        try {
            Log.d("Tester", "JSON33");
            File configFile = new File("C:\\Users\\tosh\\Documents\\Ania\\studia\\5 rok\\mobilne\\context-simulator2\\context-simulator2\\src\\resources\\json_configuration.json");
            InputStream stream= new FileInputStream(configFile);
            result = IOUtils.toString(stream);
            Log.d("Tester", "JSON37");
        } catch (IOException e) {
            e.printStackTrace();
            Log.d("Tester", "JSON40");
        }
        return result;
    }



}
