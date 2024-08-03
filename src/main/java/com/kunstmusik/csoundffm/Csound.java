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

import static com.kunstmusik.csoundffm.ControlChannelType.CSOUND_AUDIO_CHANNEL;
import static com.kunstmusik.csoundffm.ControlChannelType.CSOUND_CONTROL_CHANNEL;
import static com.kunstmusik.csoundffm.ControlChannelType.CSOUND_INPUT_CHANNEL;
import static com.kunstmusik.csoundffm.ControlChannelType.CSOUND_OUTPUT_CHANNEL;
import static java.lang.foreign.ValueLayout.ADDRESS;
import static java.lang.foreign.ValueLayout.JAVA_DOUBLE;
import static java.lang.foreign.ValueLayout.JAVA_INT;

import java.io.File;
import java.lang.foreign.Arena;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.Linker;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SymbolLookup;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.ref.Cleaner;
import java.nio.file.Path;

/**
 *
 * Csound engine instance that wraps Csound's C API into an object and
 * simplifies usage of the API.
 *
 * Comments for methods based on documentation from csound.h.
 *
 * @author Steven Yi
 */
public class Csound {

    private static MethodHandle csoundInitialize = null;
    private static MethodHandle csoundCreate = null;
    private static MethodHandle csoundGetVersion = null;
    private static MethodHandle csoundSetOption = null;
    private static MethodHandle csoundEvalCode = null;
    private static MethodHandle csoundCompile = null;
    private static MethodHandle csoundCompileOrc = null;
    private static MethodHandle csoundCompileOrcAsync = null;
    private static MethodHandle csoundCompileCsdText = null;
    private static MethodHandle csoundGetScoreTime = null;
    private static MethodHandle csoundInputMessage = null;
    private static MethodHandle csoundInputMessageAsync = null;
    private static MethodHandle csoundReadScore = null;
    private static MethodHandle csoundReadScoreAsync = null;
    private static MethodHandle csoundPerformKsmps = null;

    private static MethodHandle csoundStart = null;
    private static MethodHandle csoundStop = null;
    private static MethodHandle csoundCleanup = null;
    private static MethodHandle csoundReset = null;
    private static MethodHandle csoundDestroy = null;

    private static MethodHandle csoundGetSr;
    private static MethodHandle csoundGetKr;
    private static MethodHandle csoundGetKsmps;
    private static MethodHandle csoundGetNchnls;
    private static MethodHandle csoundGetNchnlsInput;
    private static MethodHandle csoundGet0dBFS;

    private static MethodHandle csoundGetSpin;
    private static MethodHandle csoundGetSpout;
    private static MethodHandle csoundSetControlChannel;
    private static MethodHandle csoundSetStringChannel;
    private static MethodHandle csoundGetChannelPtr;
    private static MethodHandle csoundSetMessageStringCallback = null;

    private static final Cleaner cleaner = Cleaner.create();

    // MEMBER VARIABLES
    @SuppressWarnings("unused")
    private final Cleaner.Cleanable cleanable;

    private MemorySegment csoundInstance;

    private static String getLibraryPath() {
        var os = System.getProperty("os.name").toLowerCase();
        var isMac = ((os.indexOf("mac") >= 0) || (os.indexOf("darwin") >= 0));
        var isLinux = os.toLowerCase().contains("linux");

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
        } else if (isLinux) {
            var usrLocalLibPath = "/usr/local/lib/libcsound64.so";
            if (new File(usrLocalLibPath).exists()) {
                return usrLocalLibPath;
            }

        }

        return libraryPath;
    }

    private static void initialize() {

        String libraryPath = getLibraryPath();

        Arena arena = Arena.global();

        try {
            var linker = Linker.nativeLinker();

            SymbolLookup mylib = SymbolLookup.libraryLookup(libraryPath, arena);

            csoundInitialize = linker.downcallHandle(
                    mylib.find("csoundInitialize").orElseThrow(), FunctionDescriptor.ofVoid(JAVA_INT));

            try (Arena confined = Arena.ofConfined()) {
                csoundInitialize.invoke(3);
            }

            csoundCreate = linker.downcallHandle(mylib.find("csoundCreate").orElseThrow(),
                    FunctionDescriptor.of(ADDRESS, ADDRESS));
            csoundGetVersion = linker.downcallHandle(mylib.find("csoundGetVersion").orElseThrow(),
                    FunctionDescriptor.of(JAVA_INT));
            csoundSetOption = linker.downcallHandle(mylib.find("csoundSetOption").orElseThrow(),
                    FunctionDescriptor.of(JAVA_INT, ADDRESS, ADDRESS));
            csoundEvalCode = linker.downcallHandle(mylib.find("csoundEvalCode").orElseThrow(),
                    FunctionDescriptor.of(JAVA_DOUBLE, ADDRESS, ADDRESS));
            csoundCompile = linker.downcallHandle(mylib.find("csoundCompile").orElseThrow(),
                    FunctionDescriptor.of(JAVA_INT, ADDRESS, JAVA_INT, ADDRESS));
            csoundCompileOrc = linker.downcallHandle(mylib.find("csoundCompileOrc").orElseThrow(),
                    FunctionDescriptor.of(JAVA_INT, ADDRESS, ADDRESS));
            csoundCompileOrcAsync = linker.downcallHandle(mylib.find("csoundCompileOrcAsync").orElseThrow(),
                    FunctionDescriptor.of(JAVA_INT, ADDRESS, ADDRESS));
            csoundCompileCsdText = linker.downcallHandle(mylib.find("csoundCompileCsdText").orElseThrow(),
                    FunctionDescriptor.of(JAVA_INT, ADDRESS, ADDRESS));
            csoundGetScoreTime = linker.downcallHandle(mylib.find("csoundGetScoreTime").orElseThrow(),
                    FunctionDescriptor.of(JAVA_DOUBLE, ADDRESS));
            csoundInputMessage = linker.downcallHandle(mylib.find("csoundInputMessage").orElseThrow(),
                    FunctionDescriptor.ofVoid(ADDRESS, ADDRESS));
            csoundInputMessageAsync = linker.downcallHandle(mylib.find("csoundInputMessageAsync").orElseThrow(),
                    FunctionDescriptor.ofVoid(ADDRESS, ADDRESS));
            csoundReadScore = linker.downcallHandle(mylib.find("csoundReadScore").orElseThrow(),
                    FunctionDescriptor.ofVoid(ADDRESS, ADDRESS));
            csoundReadScoreAsync = linker.downcallHandle(mylib.find("csoundReadScoreAsync").orElseThrow(),
                    FunctionDescriptor.ofVoid(ADDRESS, ADDRESS));
            csoundPerformKsmps = linker.downcallHandle(mylib.find("csoundPerformKsmps").orElseThrow(),
                    FunctionDescriptor.of(JAVA_INT, ADDRESS));

            csoundStart = linker.downcallHandle(mylib.find("csoundStart").orElseThrow(),
                    FunctionDescriptor.of(JAVA_INT, ADDRESS));
            csoundStop = linker.downcallHandle(mylib.find("csoundStop").orElseThrow(),
                    FunctionDescriptor.ofVoid(ADDRESS));
            csoundCleanup = linker.downcallHandle(mylib.find("csoundCleanup").orElseThrow(),
                    FunctionDescriptor.of(JAVA_INT, ADDRESS));
            csoundReset = linker.downcallHandle(mylib.find("csoundReset").orElseThrow(),
                    FunctionDescriptor.ofVoid(ADDRESS));
            csoundDestroy = linker.downcallHandle(mylib.find("csoundDestroy").orElseThrow(),
                    FunctionDescriptor.ofVoid(ADDRESS));

            csoundGetSr = linker.downcallHandle(mylib.find("csoundGetSr").orElseThrow(),
                    FunctionDescriptor.of(JAVA_DOUBLE, ADDRESS));
            csoundGetKr = linker.downcallHandle(mylib.find("csoundGetKr").orElseThrow(),
                    FunctionDescriptor.of(JAVA_DOUBLE, ADDRESS));
            csoundGetKsmps = linker.downcallHandle(mylib.find("csoundGetKsmps").orElseThrow(),
                    FunctionDescriptor.of(JAVA_INT, ADDRESS));
            csoundGetNchnls = linker.downcallHandle(mylib.find("csoundGetNchnls").orElseThrow(),
                    FunctionDescriptor.of(JAVA_INT, ADDRESS));
            csoundGetNchnlsInput = linker.downcallHandle(mylib.find("csoundGetNchnlsInput").orElseThrow(),
                    FunctionDescriptor.of(JAVA_INT, ADDRESS));
            csoundGet0dBFS = linker.downcallHandle(mylib.find("csoundGet0dBFS").orElseThrow(),
                    FunctionDescriptor.of(JAVA_DOUBLE, ADDRESS));

            csoundGetSpin = linker.downcallHandle(mylib.find("csoundGetSpin").orElseThrow(),
                    FunctionDescriptor.of(ADDRESS, ADDRESS));
            csoundGetSpout = linker.downcallHandle(mylib.find("csoundGetSpout").orElseThrow(),
                    FunctionDescriptor.of(ADDRESS, ADDRESS));
            csoundSetControlChannel = linker.downcallHandle(mylib.find("csoundSetControlChannel").orElseThrow(),
                    FunctionDescriptor.ofVoid(ADDRESS, ADDRESS, JAVA_DOUBLE));
            csoundSetStringChannel = linker.downcallHandle(mylib.find("csoundSetStringChannel").orElseThrow(),
                    FunctionDescriptor.ofVoid(ADDRESS, ADDRESS, ADDRESS));
            csoundGetChannelPtr = linker.downcallHandle(mylib.find("csoundGetChannelPtr").orElseThrow(),
                    FunctionDescriptor.of(JAVA_INT, ADDRESS, ADDRESS, ADDRESS, JAVA_INT));

            csoundSetMessageStringCallback = linker.downcallHandle(
                    mylib.find("csoundSetMessageStringCallback").orElseThrow(),
                    FunctionDescriptor.ofVoid(ADDRESS, ADDRESS));

        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    static {
        initialize();
    }

    /**
     * Returns the version number times 1000 (5.00.0 = 5000).
     *
     * @return version number
     */
    public static int getVersion() {
        try {
            int version = (int) csoundGetVersion.invoke();
            return version;
        } catch (Throwable t) {
            t.printStackTrace();
            return -1;
        }
    }

    /**
     * Constructor for Csound object. Registers to call csoundDelete for user when
     * there are no more references to the Csound object.
     */
    public Csound() {
        try (Arena arena = Arena.ofConfined()) {
            var arg = arena.allocate(ADDRESS);
            csoundInstance = (MemorySegment) csoundCreate.invokeExact(arg);
            cleanable = cleaner.register(this, new CsoundCleanup(csoundInstance));
        } catch (Throwable e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    /**
     * Set a single Csound option (flag). NB: blank spaces are not allowed
     *
     * @param option Single Csound option
     * @return Returns a non-zero error code on failure.
     */
    public int setOption(String option) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment optionSegment = arena.allocateFrom(option);

            return (int) csoundSetOption.invoke(csoundInstance, optionSegment);
        } catch (Throwable t) {
            t.printStackTrace();
            return -1;
        }
    }

    /**
     * Parse and compile an orchestra given on an string, evaluating any global
     * space code (i-time only).On SUCCESS it returns a value passed to the
     * 'return' opcode in global space
     *
     * <pre>
     * String code = "i1 = 2 + 2 \n return i1 \n";
     * double retval = csound.evalCode(code);
     * </pre>
     *
     * @param orcCode Csound orchestra code to evaluate
     * @return result of value passed to 'return' opcode in global space
     */
    public double evalCode(String orcCode) {

        try (Arena arena = Arena.ofConfined()) {
            MemorySegment orcCodeSegment = arena.allocateFrom(orcCode);

            return (double) csoundEvalCode.invoke(csoundInstance, orcCodeSegment);
        } catch (Throwable t) {
            t.printStackTrace();
            return -1;
        }
    }

    /**
     * Compiles Csound input files (such as an orchestra and score, or CSD) as
     * directed by the supplied command-line arguments, but does not perform
     * them. Returns a non-zero error code on failure.This function cannot be
     * called during performance, and before a repeated call, reset() needs to
     * be called. In this (host-driven) mode, the sequence of calls should be as
     * follows:
     *
     * <pre>
     * csound.compile(args);
     * while (csound.performKsmps() == 0)
     *     ;
     * csound.cleanup();
     * csound.reset();
     * </pre>
     *
     * Calls csoundStart() internally. Can only be called again after reset (see
     * csoundReset())
     *
     * @param args String arguments as if using command-line Csound. First
     *             argument should be the name of the command (e.g., "csound").
     * @return Returns a non-zero error code on failure.
     */
    public int compile(String[] args) {
        try (Arena arena = Arena.ofConfined()) {
            // Allocate memory for the array of pointers
            MemorySegment argsArray = arena.allocate(ADDRESS, args.length);

            // Allocate memory for each string and set the pointers
            for (int i = 0; i < args.length; i++) {
                MemorySegment argSegment = arena.allocateFrom(args[i]);
                argsArray.setAtIndex(ADDRESS, i, argSegment);
            }

            // Invoke the native function
            return (int) csoundCompile.invoke(csoundInstance, args.length, argsArray);
        } catch (Throwable t) {
            t.printStackTrace();
            return -1;
        }
    }

    /**
     * Parse, and compile the given orchestra from a String, also evaluating any
     * global space code (i-time only) this can be called during performance to
     * compile a new orchestra.
     *
     * <pre>
     * String orc = "instr 1 \n a1 rand 0dbfs/4 \n out a1 \n";
     * csound.compileOrc(csound, orc);
     * </pre>
     *
     * @param orcCode Csound orchestra code
     * @return Returns a non-zero error code on failure.
     */
    public int compileOrc(String orcCode) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment orcCodeSegment = arena.allocateFrom(orcCode);

            return (int) csoundCompileOrc.invoke(csoundInstance, orcCodeSegment);
        } catch (Throwable t) {
            t.printStackTrace();
            return -1;
        }
    }

    /**
     * Async version of compileOrc().The code is parsed and compiled, then
     * placed on a queue for asynchronous merge into the running engine, and
     * evaluation.The function returns following parsing and compilation.
     *
     * @param orcCode Csound orchestra code
     * @return Returns a non-zero error code on failure.
     */
    public int compileOrcAsync(String orcCode) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment orcCodeSegment = arena.allocateFrom(orcCode);

            return (int) csoundCompileOrcAsync.invoke(csoundInstance, orcCodeSegment);
        } catch (Throwable t) {
            t.printStackTrace();
            return -1;
        }
    }

    /**
     * Compiles Csound CSD input text, but does not perform it.Returns a
     * non-zero error code on failure.If start() is called before
     * compileCsdText(), the &lt;CsOptions&gt; element is ignored (but
     * csoundSetOption can be called any number of times), the &lt;CsScore&gt;
     * element is not pre-processed, but dispatched as real-time events; and
     * performance continues indefinitely, or until ended by calling csoundStop
     * or some other logic. In this "real-time" mode, the sequence of calls
     * should be:
     *
     * <pre>
     *
     * csound.setOption("-an_option");
     * csound.setOption("-another_option");
     * csound.start(csound);
     * csound.sompileCsdText(csound, csdText);
     * while (1) {
     *     csound.performKsmps(csound);
     *     // Something to break out of the loop
     *     // when finished here...
     * }
     * csound.cleanup(csound);
     * csound.reset(csound);
     *
     * </pre>
     *
     * NB: this function can be called repeatedly during performance to replace
     * or add new instruments and events.
     *
     * But if csoundCompileCsd is called before csoundStart, the
     * &lt;CsOptions&gt; element is used, the &lt;CsScore&gt; section is
     * pre-processed and dispatched normally, and performance terminates when
     * the score terminates, or stop() is called. In this "non-real-time" mode
     * (which can still output real-time audio and handle real-time events), the
     * sequence of calls should be:
     *
     * <pre>
     *
     * csound.compileCsdText(csound, csdText);
     * csound.start();
     * while (1) {
     *     int finished = csound.performKsmps(csound);
     *     if (finished)
     *         break;
     * }
     * csound.cleanup();
     * csound.reset();
     *
     * </pre>
     *
     * @param csdText Csound CSD text
     * @return Returns a non-zero error code on failure.
     */
    public int compileCsdText(String csdText) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment csdTextSegment = arena.allocateFrom(csdText);

            return (int) csoundCompileCsdText.invoke(csoundInstance, csdTextSegment);
        } catch (Throwable t) {
            t.printStackTrace();
            return -1;
        }
    }

    /**
     * Returns the current score time in seconds since the beginning of
     * performance.
     *
     * @return Current score time in seconds since the beginning of performance.
     */
    public double getScoreTime() {
        try {
            return (double) csoundGetScoreTime.invokeExact(csoundInstance);
        } catch (Throwable t) {
            t.printStackTrace();
            return -1;
        }
    }

    /**
     * Input a String (as if from a console), used for line events.
     *
     * @param scoreText Csound score text.
     */
    public void inputMessage(String scoreText) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment scoreTextSegment = arena.allocateFrom(scoreText);

            csoundInputMessage.invoke(csoundInstance, scoreTextSegment);
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    /**
     * Asynchronous version of inputMessage().
     *
     * @param scoreText Csound score text.
     */
    public void inputMessageAsync(String scoreText) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment scoreTextSegment = arena.allocateFrom(scoreText);

            csoundInputMessageAsync.invoke(csoundInstance, scoreTextSegment);
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    /**
     * Read, preprocess, and load a score from a String.It can be called
     * repeatedly, with the new score events being added to the currently
     * scheduled ones.
     *
     * @param scoreText Csound score text.
     * @return Returns a non-zero error code on failure.
     *
     */
    public int readScore(String scoreText) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment scoreTextSegment = arena.allocateFrom(scoreText);

            return (int) csoundReadScore.invoke(csoundInstance, scoreTextSegment);
        } catch (Throwable t) {
            t.printStackTrace();
            return -1;
        }
    }

    /**
     * Asynchronous version of readScore().
     *
     * @param scoreText Csound score text.
     */
    public void readScoreAsync(String scoreText) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment scoreTextSegment = arena.allocateFrom(scoreText);

            csoundReadScoreAsync.invoke(csoundInstance, scoreTextSegment);
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    /**
     * Prepares Csound for performance. Normally called after compiling a csd
     * file or an orc file, in which case score preprocessing is performed and
     * performance terminates when the score terminates.
     *
     * However, if called before compiling a csd file or an orc file, score
     * preprocessing is not performed and "i" statements are dispatched as
     * real-time events, the &lt;CsOptions&gt; tag is ignored, and performance
     * continues indefinitely or until ended using the API.
     * 
     * @return Returns a non-zero error code on failure.
     */
    public int start() {
        try {
            return (int) csoundStart.invokeExact(csoundInstance);
        } catch (Throwable t) {
            t.printStackTrace();
            return -1;
        }
    }

    /**
     * Stops a perform() running (may be in another thread). Note that it is not
     * guaranteed that perform() has already stopped when this function returns.
     */
    public void stop() {
        try {
            csoundStop.invokeExact(csoundInstance);
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    /**
     * Senses input events, and performs one control sample worth (ksmps) of
     * audio output. Note that compile() or compileOrc(), readScore(), start()
     * must be called first. Returns 0 during performance, and 1 when
     * performance is finished. If called until it returns 1, will perform an
     * entire score. Enables external software to control the execution of
     * Csound, and to synchronize performance with audio input and output.
     *
     * @return 1 if performance is done, 0 if still more to perform.
     */
    public int performKsmps() {
        try {
            return (int) csoundPerformKsmps.invokeExact(csoundInstance);
        } catch (Throwable t) {
            t.printStackTrace();
            return -1;
        }
    }

    /**
     * Senses input events and performs audio output until the end of score is
     * reached (positive return value), an error occurs (negative return value),
     * or performance is stopped by calling csoundStop() from another thread
     * (zero return value). Note that csoundCompile() or csoundCompileOrc(),
     * csoundReadScore(), csoundStart() must be called first. In the case of
     * zero return value, csoundPerform() can be called again to continue the
     * stopped performance. Otherwise, csoundReset() should be called to clean
     * up after the finished or failed performance.
     * 
     * @return Returns a non-zero error code on failure.
     */
    public int perform() {
        int res;
        do {
            res = performKsmps();
        } while (res == 0);

        return res;
    }

    /**
     * Prints information about the end of a performance, and closes audio and
     * MIDI devices.Note: after calling cleanup(), the operation of the perform
     * functions is undefined.
     *
     * @return Returns a non-zero error code on failure.
     *
     */
    public int cleanup() {
        try {
            return (int) csoundCleanup.invokeExact(csoundInstance);
        } catch (Throwable t) {
            t.printStackTrace();
            return -1;
        }
    }

    /**
     * Resets all internal memory and state in preparation for a new
     * performance. Enables external software to run successive Csound
     * performances without reloading Csound. Implies cleanup(), unless already
     * called.
     */
    public void reset() {
        try {
            csoundReset.invokeExact(csoundInstance);
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    /**
     * Returns the number of audio sample frames per second.
     *
     * @return The number of audio sample frames per second.
     */
    public double getSr() {
        try {
            return (double) csoundGetSr.invokeExact(csoundInstance);
        } catch (Throwable t) {
            t.printStackTrace();
            return -1;
        }

    }

    /**
     * Returns the number of control samples per second.
     *
     * @return The number of control samples per second.
     */
    public double getKr() {
        try {
            return (double) csoundGetKr.invokeExact(csoundInstance);
        } catch (Throwable t) {
            t.printStackTrace();
            return -1;
        }
    }

    /**
     * Returns the number of audio sample frames per control sample.
     *
     * @return The number of audio sample frames per control sample.
     */
    public int getKsmps() {
        try {
            return (int) csoundGetKsmps.invokeExact(csoundInstance);
        } catch (Throwable t) {
            t.printStackTrace();
            return -1;
        }
    }

    /**
     * Returns the number of audio output channels. Set through the nchnls
     * header variable in the csd file.
     *
     * @return The number of audio output channels
     */
    public int getNchnls() {
        try {
            return (int) csoundGetNchnls.invokeExact(csoundInstance);
        } catch (Throwable t) {
            t.printStackTrace();
            return -1;
        }
    }

    /**
     * Returns the number of audio input channels. Set through the nchnls_i
     * header variable in the csd file. If this variable is not set, the value
     * is taken from nchnls.
     *
     * @return The number of audio input channels
     */
    public int getNchnlsInput() {
        try {
            return (int) csoundGetNchnlsInput.invokeExact(csoundInstance);
        } catch (Throwable t) {
            t.printStackTrace();
            return -1;
        }
    }

    /**
     * Returns the 0dBFS level of the spin/spout buffers.
     *
     * @return The 0dBFS level of the spin/spout buffers.
     */
    public double get0dBFS() {
        try {
            return (double) csoundGet0dBFS.invokeExact(csoundInstance);
        } catch (Throwable t) {
            t.printStackTrace();
            return -1;
        }
    }

    /**
     * Returns a MemorySegment of the Csound audio input working buffer
     * (spin). Enables external software to write audio into Csound before
     * calling csoundPerformKsmps. The length of the MemorySegment is set to
     * ksmps * nchnls_i.
     *
     * @return MemorySegment of the Csound audio input working buffer
     *         (spin).
     */
    public MemorySegment getSpin() {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment spin = (MemorySegment) csoundGetSpin.invoke(csoundInstance);
            spin = spin.reinterpret(JAVA_DOUBLE.byteSize() * getKsmps());

            return spin;
        } catch (Throwable t) {
            t.printStackTrace();
            return null;
        }
    }

    /**
     * Returns a MemorySegment of the Csound audio output working buffer
     * (spout). Enables external software to read audio from Csound after
     * calling csoundPerformKsmps. The length of the MemorySegment is set to
     * ksmps * nchnls.
     *
     * @return MemorySegment of the Csound audio output working buffer (spout).
     */
    public MemorySegment getSpout() {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment spout = (MemorySegment) csoundGetSpout.invoke(csoundInstance);
            spout = spout.reinterpret(JAVA_DOUBLE.byteSize() * getKsmps());

            return spout;
        } catch (Throwable t) {
            t.printStackTrace();
            return null;
        }
    }

    /**
     * Sets the value of control channel identified by name.
     *
     *
     * @param channelName Name of channel.
     * @param value       Value to set.
     */
    public void setChannel(String channelName, double value) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment nameSegment = arena.allocateFrom(channelName);

            csoundSetControlChannel.invoke(csoundInstance, nameSegment, value);
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    /**
     * Sets the string channel identified by channelName with channelValue.
     *
     * @param channelName  Name of channel.
     * @param channelValue Value to set.
     */
    public void setStringChannel(String channelName, String channelValue) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment nameSegment = arena.allocateFrom(channelName);
            MemorySegment valueSegment = arena.allocateFrom(channelValue);

            csoundSetStringChannel.invoke(csoundInstance, nameSegment, valueSegment);
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    /**
     * Sets a MessageCallback to be called by Csound to print an informational
     * message. This callback is never called in --realtime mode. (Uses
     * csoundSetMessageStringCallback.)
     *
     * @param msgCallback Callback to execute to print messages.
     */
    public void setMessageCallback(MessageCallback msgCallback) {
        try (Arena arena = Arena.ofConfined()) {
            MethodHandle callbackHandle = MethodHandles.lookup().findVirtual(
                    MessageCallback.class, "callback",
                    MethodType.methodType(void.class, MemorySegment.class, int.class, MemorySegment.class));

            MemorySegment callbackSegment = Linker.nativeLinker().upcallStub(
                    callbackHandle.bindTo(msgCallback),
                    FunctionDescriptor.ofVoid(ADDRESS, JAVA_INT, ADDRESS),
                    Arena.global());

            csoundSetMessageStringCallback.invoke(csoundInstance, callbackSegment);
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    /**
     * Returns a MemorySegment for a control channel. Allows efficient reading
     * and writing of the channel as it does not have to look up the channel
     * each time as it does with getChannel().
     *
     * @param channelName Name of control channel
     * @return MemorySegment for control channel data pointer.
     */
    public MemorySegment getControlChannelPtr(String channelName) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment nameSegment = arena.allocateFrom(channelName);
            MemorySegment channelPtrPtr = arena.allocate(ADDRESS);

            int retVal = (int) csoundGetChannelPtr.invoke(csoundInstance, channelPtrPtr, nameSegment,
                    CSOUND_CONTROL_CHANNEL | CSOUND_INPUT_CHANNEL | CSOUND_OUTPUT_CHANNEL);

            // TODO - check retval, see ctcsound.py for example
            MemorySegment channelPtr = channelPtrPtr.get(ADDRESS, 0);
            channelPtr = channelPtr.reinterpret(JAVA_DOUBLE.byteSize());

            return channelPtr;
        } catch (Throwable t) {
            t.printStackTrace();
            return null;
        }
    }

    /**
     * Returns a MemorySegment for an audio channel. Allows efficient reading
     * and writing of the channel as it does not have to look up the channel
     * each time as it does with getChannel(). MemorySegment size is set to 
     * ksmps length.
     *
     * @param channelName Name of audio channel
     * @return MemorySegment for audio channel data pointer.
     */
    public MemorySegment getAudioChannelPtr(String channelName) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment nameSegment = arena.allocateFrom(channelName);
            MemorySegment channelPtrPtr = arena.allocate(ADDRESS);

            int retVal = (int) csoundGetChannelPtr.invoke(csoundInstance, channelPtrPtr, nameSegment,
                    CSOUND_AUDIO_CHANNEL | CSOUND_INPUT_CHANNEL | CSOUND_OUTPUT_CHANNEL);

            // TODO - check retval, see ctcsound.py for example
            MemorySegment channelPtr = channelPtrPtr.get(ADDRESS, 0);
            channelPtr = channelPtr.reinterpret(JAVA_DOUBLE.byteSize() * getKsmps());

            return channelPtr;
        } catch (Throwable t) {
            t.printStackTrace();
            return null;
        }
    }

    private static class CsoundCleanup implements Runnable {

        private final MemorySegment csoundInstance;

        CsoundCleanup(MemorySegment csoundInstance) {
            this.csoundInstance = csoundInstance;
        }

        @Override
        public void run() {
            try {
                csoundDestroy.invokeExact(csoundInstance);
            } catch (Throwable e) {
                e.printStackTrace();
            }
        }
    }
}
