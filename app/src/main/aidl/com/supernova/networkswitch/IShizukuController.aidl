package com.supernova.networkswitch;

interface IShizukuController {
    boolean compatibilityCheck(int subId);
    boolean getNetworkState(int subId);
    void setNetworkState(int subId, boolean enabled);
    void destroy();
}
