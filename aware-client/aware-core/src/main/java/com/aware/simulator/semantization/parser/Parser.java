package com.aware.simulator.semantization.parser;

import com.aware.simulator.AbstractEvent;

import java.util.Map;


public interface Parser {

    Map<String, String> parseEvent(AbstractEvent event);

}
