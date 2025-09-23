package com.supernova.networkswitch;

interface IRootController {
    boolean compatibilityCheck(int subId);
    boolean getNetworkState(int subId);
    void setNetworkState(int subId, boolean enabled);
}
