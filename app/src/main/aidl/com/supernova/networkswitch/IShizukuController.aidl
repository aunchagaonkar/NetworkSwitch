package com.supernova.networkswitch;

interface IShizukuController {
    boolean compatibilityCheck(int subId);
    boolean getFivegEnabled(int subId);
    void setFivegEnabled(int subId, boolean enabled);
    void destroy();
}
