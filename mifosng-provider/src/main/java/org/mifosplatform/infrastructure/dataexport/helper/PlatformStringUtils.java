/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.mifosplatform.infrastructure.dataexport.helper;

import java.util.ArrayList;
import java.util.List;

/** 
 * Utility class containing methods that manipulate strings 
 **/
public class PlatformStringUtils {
    
    /** 
     * create an array of strings from the specified strings
     * 
     * @param first -- the first string to be added to the array
     * @param more -- additional strings to be added to the array
     * @return array of strings
     **/
    public static String[] toArray(final String first, final String ... more) {
        List<String> arrayList = new ArrayList<String>();
        
        // add the first string to the array
        arrayList.add(first);
        
        // make sure there are more strings to be added
        if (more != null && more.length > 0) {
            // iterate over the list of strings
            for (String string : more) {
                // add additional string to the array of strings
                arrayList.add(string);
            }
        }
        
        // convert to array containing all of the elements in this list 
        // in proper sequence (from first to last element)
        return arrayList.toArray(new String[arrayList.size()]);
    }
}
