/*
    CsoundFFM.java:

    Copyright (C) 2024 Steven Yi 

    This file is part of CsoundFFM.

    The CsoundFFM Library is free software; you can redistribute it
    and/or modify it under the terms of the GNU Lesser General Public
    License as published by the Free Software Foundation; either
    version 2.1 of the License, or (at your option) any later version.

    CsoundFFM is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Lesser General Public License for more details.

    You should have received a copy of the GNU Lesser General Public
    License along with CsoundJNI; if not, write to the Free Software
    Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
    02110-1301 USA
 */
package com.kunstmusik.csoundffm;

/**
 *
 * @author stevenyi
 */
public class CsoundFFM {

    public static void main(String[] args) {
        System.out.println(String.format("Csound Version: %d", Csound.getVersion()));
        Csound csound = new Csound();

        csound.setOption("-odac");
        csound.setOption("--ksmps=64");
        csound.setOption("--sample-rate=48000");
        csound.setOption("--nchnls=2");
        csound.setOption("--nchnls_i=1");
        csound.setOption("--0dbfs=1");
        // csound.setOption("--daemon");

        System.out.printf("Sample Rate: %g\n", csound.getSr());
        System.out.printf("Control Rate: %g\n", csound.getKr());
        System.out.printf("ksmps: %d\n", csound.getKsmps());
        System.out.printf("0dbfs: %g\n", csound.get0dBFS());

        // double v = csound.evalCode("return 0.5");
        // System.out.printf("Evaluated Code: %g\n", v);


        csound.compileCsdText("""
           <CsoundSynthesizer> 
           <CsInstruments>
            instr 1
                a1 = oscil(0.5, 440)
                outs a1, a1
            endin
            
            </CsInstruments>
            <CsScore>
            i1 0 2
            </CsScore>
            </CsoundSynthesizer>
        """);

        csound.compileOrc("""
            
            instr 2
                a1 = oscil(0.25, 660)
                outs a1, a1
            endin
            
            schedule(2, 0, 4)
        """);

        csound.start();

        while(csound.performKsmps() == 0);

        csound.stop();
    }
}
