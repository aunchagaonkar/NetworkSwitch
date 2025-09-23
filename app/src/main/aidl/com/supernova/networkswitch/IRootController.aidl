package com.supernova.networkswitch;

interface IRootController {
    boolean compatibilityCheck(int subId);
    boolean getFivegEnabled(int subId);
    void setFivegEnabled(int subId, boolean enabled);
}
