package net.dongliu.byproxy.parser;

import lombok.AllArgsConstructor;
import lombok.Getter;
import net.dongliu.byproxy.store.BodyStore;

import java.io.Serializable;

@Getter
@AllArgsConstructor
public class HttpMessage implements Serializable {
    private static final long serialVersionUID = 5094098542868403395L;
    private Headers headers;
    private BodyStore body;
}
