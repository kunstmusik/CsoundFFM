package com.kunstmusik.csoundffm;

/** Control Channel Types */
public class ControlChannelType {
  public static final int CSOUND_CONTROL_CHANNEL = 1;
  public static final int CSOUND_AUDIO_CHANNEL = 2;
  public static final int CSOUND_STRING_CHANNEL = 3;
  public static final int CSOUND_PVS_CHANNEL = 4;
  public static final int CSOUND_VAR_CHANNEL = 5;

  public static final int CSOUND_CHANNEL_TYPE_MASK = 15;

  public static final int CSOUND_INPUT_CHANNEL = 16;
  public static final int CSOUND_OUTPUT_CHANNEL = 32;
};
