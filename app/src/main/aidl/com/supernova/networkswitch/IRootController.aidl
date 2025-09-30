package com.supernova.networkswitch;

interface IRootController {
    boolean compatibilityCheck(int subId);
    int getCurrentNetworkMode(int subId);
    void setNetworkMode(int subId, int networkMode);
}
