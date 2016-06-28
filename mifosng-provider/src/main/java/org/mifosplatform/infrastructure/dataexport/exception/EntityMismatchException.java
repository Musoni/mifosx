/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.mifosplatform.infrastructure.dataexport.exception;


import java.util.InputMismatchException;

public class EntityMismatchException extends InputMismatchException{

    public EntityMismatchException(String entity1, String entity2){
        super("Error occurred: " + entity1 + " was the required entity, but " + entity2 + " was provided.");
    }
}
