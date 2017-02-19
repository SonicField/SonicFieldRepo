/* For Copyright and License see LICENSE.txt and COPYING.txt in the root directory */
package com.nerdscentral.sython;

public class ProgramErrorLogger implements SFPL_Logger
{

    @Override
    public void println(String what)
    {
        System.err.println(what);
    }

}
