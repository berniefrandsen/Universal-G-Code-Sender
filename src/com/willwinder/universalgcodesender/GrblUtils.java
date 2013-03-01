/*
 * Collection of useful Grbl related utilities.
 */

/*
    Copywrite 2012-2013 Will Winder

    This file is part of Universal Gcode Sender (UGS).

    UGS is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    UGS is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with UGS.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.willwinder.universalgcodesender;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.vecmath.Point3d;

/**
 *
 * @author wwinder
 */
public class GrblUtils {
// Note: 5 characters of this buffer reserved for real time commands.
    public static final int GRBL_RX_BUFFER_SIZE= 123;
    
    /**
     * Real-time commands
     */
    public static final byte GRBL_PAUSE_COMMAND = '!';
    public static final byte GRBL_RESUME_COMMAND = '~';
    public static final byte GRBL_STATUS_COMMAND = '?';
    public static final byte GRBL_RESET_COMMAND = 0x18;
    
    /**
     * Gcode Commands
     */
    public static final String GCODE_RESET_COORDINATES_TO_ZERO = "G92 X0 Y0 Z0";
    public static final String GCODE_RETURN_TO_ZERO_LOCATION = "G0 X0 Y0 Z0";
    public static final String GCODE_PERFORM_HOMING_CYCLE = "G28 X0 Y0 Z0";
    
    public enum Capabilities {
        REAL_TIME, STATUS_C
    }
    /**
     * Removes any comments within parentheses or beginning with a semi-colon.
     */
    static protected String removeComment(String command) {
        String newCommand = command;

        // Remove any comments within ( parentheses ) with regex "\([^\(]*\)"
        newCommand = newCommand.replaceAll("\\([^\\(]*\\)", "");

        // Remove any comment beginning with ';' with regex "\;[^\\(]*"
        newCommand = newCommand.replaceAll("\\;[^\\\\(]*", "");

        return newCommand.trim();
    }
    
    /**
     * Searches for a comment in the input string and returns the first match.
     */
    static protected String parseComment(String command) {
        String comment = "";

        // REGEX: Find any comment, includes the comment characters:
        //              "(?<=\()[^\(\)]*|(?<=\;)[^;]*"
        //              "(?<=\\()[^\\(\\)]*|(?<=\\;)[^;]*"
        
        Pattern pattern = Pattern.compile("(?<=\\()[^\\(\\)]*|(?<=\\;)[^;]*");
        Matcher matcher = pattern.matcher(command);
        if (matcher.find()){
            comment = matcher.group(0);
        }

        return comment;
    }
    
    /** 
     * Checks if the string contains the GRBL version.
     */
    static Boolean isGrblVersionString(final String response) {
        return response.startsWith("Grbl ") && (getVersionDouble(response) != -1);
    }
    
    /** 
     * Parses the version double out of the version response string.
     */
    static protected double getVersionDouble(final String response) {
        double retValue = -1;
        final String VERSION_REGEX = "[0-9]*\\.[0-9]*";
        
        // Search for a version.
        Pattern pattern = Pattern.compile(VERSION_REGEX);
        Matcher matcher = pattern.matcher(response);
        if (matcher.find()) {
            retValue = Double.parseDouble(matcher.group(0));
        }
        
        return retValue;
    }
    
    static protected String getVersionLetter(final String response) {
        String retValue = null;
        final String VERSION_REGEX = "(?<=[0-9]\\.[0-9])[a-zA-Z]";
        
        // Search for a version.
        Pattern pattern = Pattern.compile(VERSION_REGEX);
        Matcher matcher = pattern.matcher(response);
        if (matcher.find()) {
            retValue = matcher.group(0);
            //retValue = Double.parseDouble(matcher.group(0));
        }
        
        return retValue;

    }

    /** 
     * Determines if the version of GRBL is capable of realtime commands.
     */
    static protected Boolean isRealTimeCapable(final double version) {
        return version > 0.7;
    }
    
    /**
     * Determines version of GRBL position capability.
     */
    static protected Capabilities getGrblStatusCapabilities(final double version, final String letter) {
        if (version >= 0.8) {
            if (version==0.8 && letter.equals("c")) {
                return Capabilities.STATUS_C;
            } else if (version >= 0.9) {
                return Capabilities.STATUS_C;
            }
        }
        return null;
    }
    
    /**
     * Check if a string contains a GRBL position string.
     */
    static protected Boolean isGrblStatusString(final String response) {
        double retValue = -1;
        final String REGEX = "\\<.*\\>";
        
        // Search for a version.
        Pattern pattern = Pattern.compile(REGEX);
        Matcher matcher = pattern.matcher(response);
        if (matcher.find()) {
            return true;
        }
        return false;
    }
    
    /**
     * Parse state out of position string.
     */
    static protected String getStateFromStatusString(final String status, final Capabilities version) {
        String retValue = null;
        String REGEX;
        
        if (version == Capabilities.STATUS_C) {
            REGEX = "(?<=\\<)[a-zA-z]*(?=[,])";
        } else {
            return null;
        }
        
        
        // Search for a version.
        Pattern pattern = Pattern.compile(REGEX);
        Matcher matcher = pattern.matcher(status);
        if (matcher.find()) {
            retValue = matcher.group(0);;
        }

        return retValue;
    }
    
    
    static protected Point3d getMachinePositionFromStatusString(final String status, final Capabilities version) {
        Point3d ret = null;
        String REGEX;
        
        if (version == Capabilities.STATUS_C) {
            REGEX = "(?<=MPos:)(-?\\d*\\..\\d*),(-?\\d*\\..\\d*),(-?\\d*\\..\\d*)(?=,WPos:)";
        } else {
            return null;
        }
        
        // Search for a version.
        return GrblUtils.getPositionFromStatusString(status, REGEX);
    }

    static protected Point3d getWorkPositionFromStatusString(final String status, final Capabilities version) {
        Point3d ret = null;
        String REGEX;

        if (version == Capabilities.STATUS_C) {
            REGEX = "(?<=WPos:)(\\-?\\d*\\..\\d*),(\\-?\\d*\\..\\d*),(\\-?\\d*\\..\\d*)";
        } else {
            return null;
        }
        
        // Search for a version.
        return GrblUtils.getPositionFromStatusString(status, REGEX);
    }
    
    static private Point3d getPositionFromStatusString(final String status, final String regex) {
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(status);
        if (matcher.find()) {
            return new Point3d( Double.parseDouble(matcher.group(1)),
                                Double.parseDouble(matcher.group(2)),
                                Double.parseDouble(matcher.group(3)));
        }
        
        return null;
    }
}