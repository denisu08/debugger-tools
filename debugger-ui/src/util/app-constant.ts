export const COMMAND_TYPE = Object.freeze({
  SYNC: 'SYNC',
  CONNECT: 'CONNECT',
  DISCONNECT: 'DISCONNECT',
  RESUME: 'RESUME',
  NEXT: 'NEXT',
  MUTE: 'MUTE',
  SET_BREAKPOINT: 'SET_BREAKPOINT',
  ADD_VARIABLE: 'ADD_VARIABLE',
  REMOVE_VARIABLE: 'REMOVE_VARIABLE',
});

export const COMMAND_PARAM = Object.freeze({
  IP: 'ip',
  PORT: 'port',
  IS_CONNECT: 'connect',
  IS_MUTE: 'mute',
  CURRENT_LINE_BREAKPOINT: 'clb',
  CURRENT_POINTER_BREAKPOINT: 'cpb',
  CURRENT_BREAKPOINTS: 'currentBrColl',
  BREAKPOINTS: 'brColl',
  GET_SYSTEM_VARIABLES: 'sysVar',
  GET_CUSTOM_VARIABLES: 'custVar'
});
