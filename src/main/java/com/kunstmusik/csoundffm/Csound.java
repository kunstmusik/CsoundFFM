/*
    Csound.java:

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

import java.io.File;
import java.lang.foreign.Arena;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.Linker;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SymbolLookup;
import java.lang.foreign.ValueLayout;
import static java.lang.foreign.ValueLayout.ADDRESS;
import java.lang.invoke.MethodHandle;
import java.nio.file.Path;

/**
 *
 * @author stevenyi
 */
public class Csound {

    private static MethodHandle csoundInitialize = null;
    private static MethodHandle csoundCreate = null;
    
    private MemorySegment csoundInstance;

    private static void initialize() {
        var os = System.getProperty("os.name").toLowerCase();
        var isMac = ((os.indexOf("mac") >= 0) || (os.indexOf("darwin") >= 0));

        String libraryPath = "csound64";

        if (isMac) {
            var home = System.getProperty("user.home");
            var libRoot = "/Library/Frameworks/CsoundLib64.framework/CsoundLib64";
            var userFramework = Path.of(home, libRoot).toString();
            var sysFramework = libRoot;

            if (new File(userFramework).exists()) {
                libraryPath = userFramework;
            } else if (new File(sysFramework).exists()) {
                libraryPath = sysFramework;
            }
        }

        Arena arena = Arena.global();

        try {
            var linker = Linker.nativeLinker();

            SymbolLookup mylib = SymbolLookup.libraryLookup(libraryPath, arena);

            csoundInitialize = linker.downcallHandle(
                    mylib.find("csoundInitialize").orElseThrow(), FunctionDescriptor.ofVoid(ADDRESS));

            try (Arena confined = Arena.ofConfined()) {
                var intSegment = arena.allocate(ValueLayout.JAVA_INT, 3);
                csoundInitialize.invoke(intSegment);
            }

            csoundCreate = linker.downcallHandle(mylib.find("csoundCreate").orElseThrow(),
                    FunctionDescriptor.of(ADDRESS, ADDRESS));

        } catch (Throwable t) {
            t.printStackTrace();
        }

    }
    
    static {
        initialize();
    }

    public Csound() {
        try (Arena arena = Arena.ofConfined()) {
            var arg = arena.allocate(ADDRESS);
            csoundInstance = (MemorySegment) csoundCreate.invokeExact(arg);
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }
}
