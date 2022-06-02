package com.dercio.database_proxy.proxy;

public enum ProxyAction {
    OPEN,
    CLOSE;

    public ProxyAction on(ProxyAction proxyAction, Runnable execute) {
        if (this.equals(proxyAction)) {
            execute.run();
        }
        return this;
    }
}
