/* For Copyright and License see LICENSE.txt and COPYING.txt in the root directory */
package com.nerdscentral.sython;

import java.io.Serializable;

/**
 * @author AlexTu
 * 
 */
public interface SFPL_Operator extends Serializable
{

    /**
     * <b>Gives the key word which the parser will use for this operator</b>
     * 
     * @return the key word
     */
    public String Word();

    /**
     * <b>Operate</b> What ever this operator does when SFPL is running is done by this method. The execution loop all this
     * method with the current execution context and the passed forward operand.
     * 
     * @param input
     *            the operand passed into this operator
     * @param context
     *            the current execution context
     * @return the operand passed forward from this operator
     * @throws SFPL_RuntimeException
     */
    public Object Interpret(Object input, SFPL_Context context) throws SFPL_RuntimeException;

}
