/* For Copyright and License see LICENSE.txt and COPYING.txt in the root directory */
package com.nerdscentral.sython;

public class ProgramOutLogger implements SFPL_Logger
{

    @Override
    public void println(String what)
    {
        System.out.println(what);
    }

}
