package com.supernova.networkswitch;

interface IShizukuController {
    boolean compatibilityCheck(int subId);
    int getCurrentNetworkMode(int subId);
    void setNetworkMode(int subId, int networkMode);
    void destroy();
}
