package com.dercio.database_proxy.proxy;

import com.dercio.database_proxy.common.codec.Codec;
import lombok.Data;

@Codec
@Data
public class ProxyRequest {
    ProxyAction action;
}
