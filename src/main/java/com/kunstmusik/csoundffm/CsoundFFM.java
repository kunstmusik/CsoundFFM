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

import static java.lang.foreign.ValueLayout.JAVA_DOUBLE;

import java.lang.foreign.MemorySegment;

/**
 * Class for testing CsoundFFM (useful for developers of CsoundFFM)
 * @author stevenyi
 */
public class CsoundFFM {

    private static void testCompile() {
        Csound csound = new Csound();
        csound.compile(new String[] { "csound", "-odac", "-d", "trapped.csd" });
        csound.perform();
        csound.reset();
    }

    private static void test1() {
        System.out.printf("Csound Version: %d%n", Csound.getVersion());
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

        csound.compileCSD("""
                   <CsoundSynthesizer>
                   <CsInstruments>
                    instr 1
                        a1 = oscil(0.25, 440)
                        outs a1, a1
                    endin

                    </CsInstruments>
                    <CsScore>
                    i1 0 2
                    </CsScore>
                    </CsoundSynthesizer>
                """, 1);

        csound.compileOrc("""

                    instr 2
                        a1 = oscil(0.25, 660)
                        outs a1, a1
                    endin

                    schedule(2, 0, 4)
                """, 0);

        csound.start();

        while (csound.performKsmps() == 0)
            ;

        csound.reset();
    }

    private static void testChannels() {
        Csound csound = new Csound();

        MessageCallback cb = (cs, attr, msg) -> {
            var msgText = msg.reinterpret(Integer.MAX_VALUE).getString(0);
            System.out.print(msgText);
        };
        csound.setMessageCallback(cb);

        csound.setOption("-odac");
        csound.setOption("--ksmps=64");
        csound.setOption("--sample-rate=48000");
        csound.setOption("--nchnls=2");
        csound.setOption("--nchnls_i=1");
        csound.setOption("--0dbfs=1");

        csound.compileOrc("""
                    instr 1
                        kfreq = chnget:k("freq")
                        kfreqBase = chnget:k("freqBase")
                        Sval = chnget:S("strChannel")

                        kfreq += kfreqBase

                        printks("String Channel: %s\\n", 0.25, Sval)
                        printk(0.5, kfreq)
                        printk(0.5, kfreqBase)
                        a1 = oscil(0.25, kfreq)
                        outs a1, a1
                    endin

                    schedule(1, 0, 2)
                    event_i("e", 0, 2)
                """, 0);

        csound.start();

        int counter = 0;
        int freqMultiplier = 0;
        String[] stringVals = new String[] { "a", "b" };

        MemorySegment channelPtr = csound.getControlChannelPtr("freqBase");

        do {
            if (counter % 32 == 0) {
                counter = 0;
                freqMultiplier = (freqMultiplier + 1) % 16;
                csound.setStringChannel("strChannel", stringVals[freqMultiplier % 2]);
                var v = ((freqMultiplier % 12) + 1) * 100.0;
                channelPtr.set(JAVA_DOUBLE, 0, v);
            }
            csound.setChannel("freq", 60 * (freqMultiplier + 1));
            counter++;
        } while (csound.performKsmps() == 0);

        csound.reset();
        csound.setMessageCallback(null);
    }

    private static void testSpinSpout() {
        Csound csound = new Csound();

        csound.setOption("-n");
        csound.setOption("--ksmps=16");
        csound.setOption("--sample-rate=48000");
        csound.setOption("--nchnls=1");
        csound.setOption("--nchnls_i=1");
        csound.setOption("--0dbfs=1");

        csound.compileOrc("""
                    instr 1
                        print 5
                        a1 = inch(1)
                        out a1
                    endin
                    schedule(1, 0, -1)
                """, 0);

        csound.start();

        int ksmps = csound.getKsmps();
        MemorySegment spin = csound.getSpin();
        MemorySegment spout = csound.getSpout();

        for(int i = 0; i < ksmps; i++) {
            System.out.println (Math.cos(i / 300.0));
            spin.setAtIndex(JAVA_DOUBLE, i, Math.cos(i / 300.0));
            // spin.set(JAVA_DOUBLE, i, Math.sin(i / 300));
            // System.out.println( Math.sin(i / 300.0));
        }

        csound.performKsmps();

        for(int i = 0; i < ksmps; i++) {
            double original = spin.getAtIndex(JAVA_DOUBLE, i);
            double returned = spout.getAtIndex(JAVA_DOUBLE, i);
            System.out.printf("Match: %b [ %g | %g ]\n", original == returned, original, returned);
        }
        csound.reset();
    }

    public static void main(String[] args) {
        // testCompile();
        // test1();
        testChannels();
        // testSpinSpout();
    }
}
